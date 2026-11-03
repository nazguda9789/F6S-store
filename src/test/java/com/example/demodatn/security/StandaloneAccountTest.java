package com.example.demodatn.security;
import com.example.demodatn.entity.nhanvien.Employee;
import com.example.demodatn.entity.nhanvien.Role;
import com.example.demodatn.entity.nhanvien.User;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.repository.nhanvien.EmployeeRepository;
import com.example.demodatn.repository.nhanvien.UserRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StandaloneAccountTest {
    @Test void standaloneAdminRemainsEnabledButInactiveLinkedEmployeeIsRejected() {
        var users=mock(UserRepository.class);var employees=mock(EmployeeRepository.class);var customers=mock(CustomerRepository.class);
        var user=new User();user.setId(3);user.setUsername("standalone");var role=new Role();role.setName("ROLE_ADMIN");user.setRole(role);
        when(users.findByUsername("standalone")).thenReturn(Optional.of(user));
        when(customers.findByUser_Id(3)).thenReturn(Optional.empty());when(employees.findByUser_Id(3)).thenReturn(Optional.empty());
        var service=new UserDetailsServiceImpl(users,customers,employees);assertTrue(service.loadUserByUsername("standalone").isEnabled());
        var employee=new Employee();employee.setStatus(false);when(employees.findByUser_Id(3)).thenReturn(Optional.of(employee));
        assertFalse(service.loadUserByUsername("standalone").isEnabled());
    }
}
