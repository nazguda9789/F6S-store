package com.web.service;

import com.web.dto.ProductSpecification;
import com.web.dto.Result;
import com.web.dto.UserSpecification;
import com.web.entity.Category;
import com.web.entity.OtpCode;
import com.web.entity.Product;
import com.web.entity.User;
import com.web.enums.OtpType;
import com.web.enums.Role;
import com.web.enums.UserType;
import com.web.exception.MessageException;
import com.web.repository.CategoryRepository;
import com.web.repository.OtpCodeRepository;
import com.web.repository.PendingRegistrationRepository;
import com.web.repository.UserRepository;
import com.web.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OtpCodeRepository otpCodeRepository;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private MailService mailService;

    @Autowired
    private SendOtp sendOtp;

    @Autowired
    private UserUtils userUtils;

    private Integer maxTimeVerify = 10;
    private Integer maxTimeResetPass = 60;

    public List<User> getUser(){
        List<User> list = userRepository.findByRole(Role.ROLE_ADMIN);
        List<User> listEmp = userRepository.findByRole(Role.ROLE_EMPLOYEE);
        list.addAll(listEmp);
        return list;
    }

    @org.springframework.transaction.annotation.Transactional
    public void registerUser(String email, String rawPassword, String fullName, String phone, String rePassword) {
        email = normalizedEmail(email);
        if (!EmailValidator.isValidEmail(email)) throw new MessageException("Email không hợp lệ");
        if (!StringUtils.hasText(fullName)) throw new MessageException("Họ tên không được để trống");
        if (phone != null && !phone.isBlank() && !PhoneValidator.isValidPhone(phone))
            throw new MessageException("Số điện thoại không hợp lệ");
        if (rawPassword == null || rawPassword.length() < 6)
            throw new MessageException("Mật khẩu tối thiểu 6 ký tự");
        if (!rawPassword.equals(rePassword)) throw new MessageException("Mật khẩu nhập lại không khớp");
        if (userRepository.findByEmail(email).isPresent())
            throw new MessageException("Email đã tồn tại. Nếu chưa xác thực, mở bước xác thực để gửi lại mã.");
        var previous = pendingRegistrationRepository.findForUpdate(email);
        if (previous.isPresent() && previous.get().getExpiresAt().isAfter(LocalDateTime.now()))
            throw new MessageException("Email đang chờ xác thực. Hãy mở bước xác thực để gửi lại mã.");
        mailService.requireConfigured();
        var pending = new com.web.entity.PendingRegistration();
        pending.setEmail(email);
        pending.setPasswordHash(passwordEncoder.encode(rawPassword));
        pending.setFullName(fullName.trim());
        pending.setPhone(phone);
        pending.setCreatedDate(LocalDateTime.now());
        pending.setExpiresAt(LocalDateTime.now().plusMinutes(maxTimeVerify));
        pendingRegistrationRepository.save(pending);
        OtpCode code = newRegistrationCode(email);
        otpCodeRepository.save(code);
        sendOtp.sendOtp(email, code.getCode());
        otpCodeRepository.invalidatePreviousRegistrationCodes(email, code.getId());
        // No users row is created until the email owner verifies the code.
    }

    @org.springframework.transaction.annotation.Transactional
    public boolean verifyOtp(String email, String otp) {
        email = normalizedEmail(email);
        var pending = pendingRegistrationRepository.findForUpdate(email).orElse(null);
        OtpCode code = otpCodeRepository.findByEmailAndCodeAndUsedFalse(email, otp)
                .orElseThrow(() -> new MessageException("Mã kích hoạt không chính xác hoặc đã dùng"));
        if (!code.getExpiryTime().isAfter(LocalDateTime.now()))
            throw new MessageException("Mã kích hoạt đã hết hạn");
        User user = userRepository.findByEmail(email).orElse(null);
        if (pending != null) {
            if (!pending.getExpiresAt().isAfter(LocalDateTime.now()))
                throw new MessageException("Yêu cầu đăng ký đã hết hạn. Vui lòng gửi lại mã.");
            if (user != null) throw new MessageException("Tài khoản đã tồn tại. Vui lòng đăng nhập.");
            user = new User();
            user.setEmail(email); user.setPassword(pending.getPasswordHash());
            user.setFullName(pending.getFullName()); user.setPhone(pending.getPhone());
            user.setRole(Role.ROLE_USER); user.setUserType(UserType.NORMAL);
            user.setActived(true); user.setDeleted(false); user.setCreatedDate(LocalDateTime.now());
        } else if (!isLegacyPending(user)) {
            throw new MessageException("Không còn yêu cầu đăng ký cần xác thực. Vui lòng đăng nhập hoặc đăng ký lại.");
        }
        user.setActivationStatus(true);
        userRepository.save(user);
        code.setUsed(true);
        otpCodeRepository.save(code);
        otpCodeRepository.invalidatePreviousRegistrationCodes(email,code.getId());
        if (pending != null) pendingRegistrationRepository.delete(pending);
        return true;
    }

    @org.springframework.transaction.annotation.Transactional
    public void sendNewOtp(String email) {
        email = normalizedEmail(email);
        var pending = pendingRegistrationRepository.findForUpdate(email).orElse(null);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null && !isLegacyPending(user))
            throw new MessageException("Tài khoản không cần xác thực hoặc không khả dụng. Vui lòng đăng nhập.");
        if (pending == null && !isLegacyPending(user))
            throw new MessageException("Không tìm thấy yêu cầu đăng ký. Vui lòng đăng ký trước.");
        mailService.requireConfigured();
        otpCodeRepository.findTopByEmailAndOtpTypeOrderByLocalDateTimeDesc(email, OtpType.REGISTER_OTP)
                .filter(previous -> previous.getLocalDateTime() != null
                        && previous.getLocalDateTime().isAfter(LocalDateTime.now().minusSeconds(60)))
                .ifPresent(previous -> { throw new MessageException("Vui lòng chờ 60 giây trước khi gửi lại mã."); });
        OtpCode code = newRegistrationCode(email);
        otpCodeRepository.save(code);
        sendOtp.sendOtp(email, code.getCode());
        otpCodeRepository.invalidatePreviousRegistrationCodes(email, code.getId());
        if (pending != null) {
            pending.setExpiresAt(LocalDateTime.now().plusMinutes(maxTimeVerify));
            pendingRegistrationRepository.save(pending);
        }
    }

    private String normalizedEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private boolean isLegacyPending(User user) {
        return user != null && user.getRole() == Role.ROLE_USER && user.getUserType() == UserType.NORMAL
                && !Boolean.TRUE.equals(user.getActivationStatus()) && Boolean.TRUE.equals(user.getActived())
                && !Boolean.TRUE.equals(user.getDeleted()) && user.isAccountNonLocked();
    }

    private OtpCode newRegistrationCode(String email) {
        OtpCode code = new OtpCode();
        code.setEmail(email); code.setCode(sendOtp.genOtp()); code.setOtpType(OtpType.REGISTER_OTP);
        code.setExpiryTime(LocalDateTime.now().plusMinutes(maxTimeVerify));
        code.setUsed(false); code.setLocalDateTime(LocalDateTime.now());
        return code;
    }

    public Result<Void> sendMailForgotPass(String email){
        Optional<User> user = userRepository.findByEmail(email);
        if(user.isEmpty()){
            return new Result<>(false, null, "Email dùng không tồn tại");
        }
        if(user.get().getActivationStatus() != null && !user.get().getActivationStatus()){
            return new Result<>(false, null, "Tài khoản chưa được xác thực");
        }
        if(!user.get().getActived()){
            return new Result<>(false, null, "Tài khoản đã bị khóa do phát hiện nghi ngờ");
        }
        if(user.get().getIsGoogleUser() == true){
            return new Result<>(false, null, "Hãy đăng nhập bằng google");
        }
        String uuid = UUID.randomUUID().toString() +String.valueOf(System.currentTimeMillis())+ sendOtp.genOtp();;
        OtpCode otpCode = new OtpCode();
        otpCode.setEmail(email);
        otpCode.setOtpType(OtpType.RESET_PASS);
        otpCode.setCode(uuid);
        otpCode.setExpiryTime(LocalDateTime.now().plusMinutes(maxTimeResetPass));
        otpCode.setUsed(false);
        otpCodeRepository.save(otpCode);
        sendOtp.sendLinkResetPass(email, uuid);
        return new Result<>(true, null, "Đã gửi thông báo đến email của bạn");
    }

    public void resetPassword(String email, String key, String password, String repassword){
        Optional<User> userOptional = userRepository.findByEmail(email);
        if(userOptional.isEmpty()){
            throw new MessageException("Có lỗi xảy ra");
        }
        else if(password.length() < 6){
            throw new MessageException("Mật khẩu tối thiểu 6 ký tự");
        }
        else if(!password.equals(repassword)){
            throw new MessageException("Mật khẩu không trùng khớp");
        }
        Optional<OtpCode> opt = otpCodeRepository.findByEmailAndCodeResetPass(email, key);
        if(opt.isPresent()) {
            OtpCode otpCode = opt.get();
            if(otpCode.getExpiryTime().isAfter(LocalDateTime.now())) {
                otpCode.setUsed(true);
                otpCodeRepository.save(otpCode);
                // đặt lại mật khẩu
                userOptional.get().setPassword(passwordEncoder.encode(password));
                userRepository.save(userOptional.get());
            }
            else{
                throw new MessageException("Đã hết hạn đặt lại mật khẩu");
            }
        }
        else{
            throw new MessageException("Có lỗi xảy ra");
        }
    }

    public Boolean checkResetPass(String email, String key) {
        Optional<OtpCode> otp = otpCodeRepository.findByEmailAndCodeResetPass(email, key);
        if(otp.isEmpty()){
            return false;
        }
        return true;
    }

    public Result<?> changeInfor(String fullName, String phone) {
        if(fullName == null){
            return new Result<>(false, null, "Họ tên không được để trống");
        }
        if(phone != null){
            if(PhoneValidator.isValidPhone(phone) == false){
                return new Result<>(false, null, "Số điện thoại không hợp lệ");
            }
        }
        User user = userUtils.getUserWithAuthority();
        user.setFullName(fullName);
        user.setPhone(phone);
        userRepository.save(user);
        return new Result<>(true, user, "Cập nhật thông tin thành công");
    }

    public Result<?> changePassword(String oldPassword,
                                    String newPassword,
                                    String confirmPassword) {
        // kiểm tra điều kiện, tối thiểu 6 ký tự
        User user = userUtils.getUserWithAuthority();
        if(!user.getUserType().equals(UserType.NORMAL)){
            return new Result<>(false, null, "Tài khoản không thể thực hiện chức năng này");
        }
        // Check mật khẩu cũ có đúng không
        if(oldPassword == null || newPassword == null || confirmPassword == null){
            return new Result<>(false, null, "Mật khẩu không được để trống");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return new Result<>(false, null, "Mật khẩu hiện tại không đúng");
        }

        // Check mật khẩu mới tối thiểu 6 ký tự
        if (newPassword == null || newPassword.length() < 6) {
            return new Result<>(false, null, "Mật khẩu mới phải có ít nhất 6 ký tự");
        }

        // Check xác nhận mật khẩu
        if (!newPassword.equals(confirmPassword)) {
            return new Result<>(false, null, "Mật khẩu xác nhận không khớp");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return new Result<>(true, null, "Cập nhật mật khẩu thành công");
    }

    public Page<User> getAllUser(Role role, LocalDate from, LocalDate to, String search, Pageable pageable) {
        return getAllUser(role, from, to, search, null, pageable);
    }

    public Page<User> getAllUser(Role role, LocalDate from, LocalDate to, String search, Boolean active, Pageable pageable) {
        UserSpecification userSpecification = new UserSpecification(role, from, to, search);
        org.springframework.data.jpa.domain.Specification<User> spec = userSpecification;
        if (active != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("actived"), active));
        Page<User> page = userRepository.findAll(spec, pageable);
        return page;
    }

    public Result<User> addUser(User user) {
        String msg = validateUser(user, true);
        if (msg != null) return Result.fail(msg);

        user.setCreatedDate(LocalDateTime.now());
        user.setActived(user.getActived() == null || user.getActived());
        user.setDeleted(false);
        user.setActivationStatus(true);
        user.setIsGoogleUser(false);
        user.setUserType(UserType.NORMAL);
        if (user.getRole() == null) user.setRole(Role.ROLE_USER);

        // ✅ Mã hóa mật khẩu
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User saved = userRepository.save(user);
        return Result.ok(saved, "Thêm người dùng thành công");
    }

    public Result<User> updateUser(Long id, User updatedUser) {
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) {
            return Result.fail("Không tìm thấy người dùng với ID: " + id);
        }

        User existing = opt.get();
        boolean checkDuplicateEmail = !existing.getEmail().equalsIgnoreCase(updatedUser.getEmail());
        String msg = validateUser(updatedUser, checkDuplicateEmail);
        if (msg != null) return Result.fail(msg);

        if (updatedUser.getRole() == null) return Result.fail("Vui lòng chọn vai trò tài khoản.");
        if (existing.getRole() == Role.ROLE_ADMIN) {
            User actor = userUtils.getUserWithAuthority();
            if (actor != null && id.equals(actor.getId()) && (updatedUser.getRole() != Role.ROLE_ADMIN
                    || Boolean.FALSE.equals(updatedUser.getActived()) || !updatedUser.isAccountNonLocked()))
                return Result.fail("Không thể tự ngừng hoạt động, khóa hoặc bỏ quyền quản trị của tài khoản đang đăng nhập.");
        }

        existing.setFullName(updatedUser.getFullName());
        existing.setPhone(updatedUser.getPhone());
        existing.setAddress(updatedUser.getAddress());
        existing.setRole(updatedUser.getRole());
        if (updatedUser.getAvatar() != null) existing.setAvatar(updatedUser.getAvatar());
        existing.setAccountNonLocked(updatedUser.isAccountNonLocked());
        if (updatedUser.getActived() != null) existing.setActived(updatedUser.getActived());

        // ✅ Nếu người dùng nhập mật khẩu mới thì mã hóa và cập nhật
        if (StringUtils.hasText(updatedUser.getPassword())) {
            existing.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }

        User saved = userRepository.save(existing);
        return Result.ok(saved, "Cập nhật người dùng thành công");
    }

    private String validateUser(User user, boolean checkDuplicateEmail) {
        if (!StringUtils.hasText(user.getEmail()))
            return "Email không được để trống";

        if (!user.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$"))
            return "Email không hợp lệ";

        if (checkDuplicateEmail && userRepository.existsByEmail(user.getEmail()))
            return "Email đã tồn tại trong hệ thống";

        if (!StringUtils.hasText(user.getFullName()))
            return "Họ tên không được để trống";

        if (user.getPhone() != null && !user.getPhone().matches("^[0-9]{9,11}$"))
            return "Số điện thoại không hợp lệ";

        // Kiểm tra trùng số điện thoại
        Optional<User> userWithPhone = userRepository.findAll().stream()
                .filter(u -> user.getPhone() != null && user.getPhone().equals(u.getPhone()))
                .findFirst();
        if (userWithPhone.isPresent() && (user.getId() == null || !userWithPhone.get().getId().equals(user.getId())))
            return "Số điện thoại đã tồn tại.";

        // ✅ Kiểm tra mật khẩu (chỉ khi thêm mới)
        if (user.getId() == null) {
            if (!StringUtils.hasText(user.getPassword()))
                return "Mật khẩu không được để trống";

            if (user.getPassword().length() < 6)
                return "Mật khẩu phải có ít nhất 6 ký tự";
        }

        // Nếu cập nhật mà nhập mật khẩu thì cũng kiểm tra độ dài
        if (user.getId() != null && StringUtils.hasText(user.getPassword()) && user.getPassword().length() < 6)
            return "Mật khẩu mới phải có ít nhất 6 ký tự";

        return null;
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Result<Void> deleteUser(Long id) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isEmpty()) {
            return new Result<>(false, null, "Không tìm thấy người dùng để xóa.");
        }
        try{
            userRepository.deleteById(id);
        }
        catch (Exception e){
            User user = optionalUser.get();
            user.setDeleted(true);
            user.setActived(false);
            userRepository.save(user);
        }
        return new Result<>(true, null, "Xóa người dùng thành công!");
    }
}
