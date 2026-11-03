package com.example.demodatn.security;
import com.example.demodatn.entity.nhanvien.User;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.repository.nhanvien.EmployeeRepository;
import com.example.demodatn.repository.nhanvien.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final CustomerRepository customers;
    private final EmployeeRepository employees;
    @org.springframework.beans.factory.annotation.Value("${shop.employee-role:ROLE_ADMIN}") private String employeeRole;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        var employee=employees.findByUser_Id(user.getId());
        boolean employeeEnabled=employee.map(e->Boolean.TRUE.equals(e.getStatus()))
                .orElse(true); // An active standalone User can manage its account; POS still requires Employee.
        boolean customerEnabled=customers.findByUser_Id(user.getId())
                .map(c->Boolean.TRUE.equals(c.getStatus())).orElse(true);
        return new CustomUserDetails(user,employeeEnabled && customerEnabled);
    }
}
