package com.example.demodatn.service.nhanvien;
import com.example.demodatn.util.PasswordPolicy;
import com.example.demodatn.dto.nhanvien.RegisterRequest;
import com.example.demodatn.entity.khachhang.Customer;
import com.example.demodatn.entity.nhanvien.Role;
import com.example.demodatn.entity.nhanvien.User;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.repository.nhanvien.RoleRepository;
import com.example.demodatn.repository.nhanvien.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(RegisterRequest request) {
        PasswordPolicy.requireValid(request.getPassword());
        request.setUsername(request.getUsername().trim().toLowerCase(java.util.Locale.ROOT));
        request.setEmail(com.example.demodatn.service.PeopleService.email(request.getEmail()));
        request.setPhone(com.example.demodatn.service.PeopleService.phone(request.getPhone()));
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        }
        if (customerRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Thư điện tử đã tồn tại");
        }
        if (customerRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new IllegalArgumentException("SĐT đã tồn tại");
        }

        Role userRole = roleRepository.findByName("ROLE_USER");
        if (userRole == null) {
            Role role = new Role();
            role.setName("ROLE_USER");
            userRole = roleRepository.save(role);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(userRole);
        user.setStatus(true);
        userRepository.save(user);

        Customer customer = new Customer();
        customer.setCustomerCode("KH" + System.currentTimeMillis());
        customer.setFullName(request.getFullName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setStatus(true);
        customer.setUser(user);
        customerRepository.save(customer);
    }
}
