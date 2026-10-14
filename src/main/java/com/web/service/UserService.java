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

    public void registerUser(String email, String rawPassword, String fullName, String phone, String rePassword) {
        if(email == null){
            throw new MessageException("Email không được để trống");
        }
        if(rawPassword == null){
            throw new MessageException("Mật khẩu không được để trống");
        }
        if(fullName == null){
            throw new MessageException("Họ tên không được để trống");
        }
        if(userRepository.findByEmail(email).isPresent()) {
            throw new MessageException("Email đã tồn tại");
        }
        if(EmailValidator.isValidEmail(email) == false){
            throw new MessageException("Email không hợp lệ");
        }
        if(phone != null){
            if(PhoneValidator.isValidPhone(phone) == false){
                throw new MessageException("Số điện thoại không hợp lệ");
            }
        }
        if(rawPassword == null || rawPassword.length() < 6){
            throw new MessageException("Mật khẩu tối thiểu 6 ký tự");
        }
        if(!rawPassword.equals(rePassword)){
            throw new MessageException("Mật khẩu không hợp lệ");
        }
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setRole(Role.ROLE_USER);
        user.setActived(true);
        user.setDeleted(false);
        user.setUserType(UserType.NORMAL);
        user.setActivationStatus(false);
        user.setCreatedDate(LocalDateTime.now());
        userRepository.save(user);

        // Tạo OTP
        String otp = sendOtp.genOtp();
        OtpCode otpCode = new OtpCode();
        otpCode.setEmail(email);
        otpCode.setCode(otp);
        otpCode.setOtpType(OtpType.REGISTER_OTP);
        otpCode.setExpiryTime(LocalDateTime.now().plusMinutes(maxTimeVerify));
        otpCode.setUsed(false);
        otpCodeRepository.save(otpCode);

        // Gửi email OTP
        mailService.sendEmail(email, "Xác thực tài khoản","Mã xác thực: "+otpCode.getCode(), false, true);
    }

    public boolean verifyOtp(String email, String otp) {
        Optional<OtpCode> opt = otpCodeRepository.findByEmailAndCodeAndUsedFalse(email, otp);
        if(opt.isPresent()) {
            OtpCode otpCode = opt.get();
            if(otpCode.getExpiryTime().isAfter(LocalDateTime.now())) {
                otpCode.setUsed(true);
                otpCodeRepository.save(otpCode);
                // Kích hoạt user
                User user = userRepository.findByEmail(email).get();
                user.setActivationStatus(true);
                userRepository.save(user);
                return true;
            }
            else{
                throw new MessageException("Mã kích hoạt đã hết hạn");
            }
        }
        else{
            throw new MessageException("Mã kích hoạt không chính xác");
        }
    }

    public void sendNewOtp(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if(user.isEmpty()){
            throw new MessageException("Có lỗi xảy ra");
        }
        if(user.get().getActivationStatus()){
            throw new MessageException("Có lỗi xảy ra");
        }
        String otp = sendOtp.genOtp();
        OtpCode otpCode = new OtpCode();
        otpCode.setEmail(email);
        otpCode.setOtpType(OtpType.REGISTER_OTP);
        otpCode.setCode(otp);
        otpCode.setExpiryTime(LocalDateTime.now().plusMinutes(maxTimeVerify));
        otpCode.setUsed(false);
        otpCodeRepository.save(otpCode);
        sendOtp.sendOtp(email, otp);
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
        UserSpecification userSpecification = new UserSpecification(role, from, to, search);
        Page<User> page = userRepository.findAll(userSpecification, pageable);
        return page;
    }

    public Result<User> addUser(User user) {
        String msg = validateUser(user, true);
        if (msg != null) return Result.fail(msg);

        user.setCreatedDate(LocalDateTime.now());
        user.setActived(true);
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

        existing.setFullName(updatedUser.getFullName());
        existing.setPhone(updatedUser.getPhone());
        existing.setAddress(updatedUser.getAddress());
        existing.setRole(updatedUser.getRole());
        existing.setAvatar(updatedUser.getAvatar());
        existing.setAccountNonLocked(updatedUser.isAccountNonLocked());

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
