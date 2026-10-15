package com.web.utils;

import com.web.entity.User;
import com.web.enums.Role;
import com.web.enums.UserType;
import com.web.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataInitializerTests {
    private final UserRepository repository = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final MockEnvironment environment = new MockEnvironment()
            .withProperty("F6S_ADMIN_EMAIL", "admin@example.test")
            .withProperty("F6S_ADMIN_PASSWORD", "test-password");

    private DataInitializer initializer() {
        DataInitializer runner = new DataInitializer();
        ReflectionTestUtils.setField(runner, "userRepository", repository);
        ReflectionTestUtils.setField(runner, "passwordEncoder", encoder);
        ReflectionTestUtils.setField(runner, "environment", environment);
        when(encoder.encode("test-password")).thenReturn("encoded-password");
        return runner;
    }

    @Test
    void existingAdminIsNotChangedWithoutResetFlag() throws Exception {
        User admin = new User();
        admin.setRole(Role.ROLE_ADMIN);
        when(repository.findByEmail("admin@example.test")).thenReturn(Optional.of(admin));
        initializer().run();
        verify(repository, never()).save(any());
        verify(encoder, never()).encode(any());
    }

    @Test
    void explicitResetEncodesPasswordAndUnlocksAdmin() throws Exception {
        environment.withProperty("F6S_ADMIN_RESET_PASSWORD", "true");
        User admin = new User();
        admin.setRole(Role.ROLE_ADMIN);
        admin.setAccountNonLocked(false);
        admin.setFailedAttempts(5);
        when(repository.findByEmail("admin@example.test")).thenReturn(Optional.of(admin));
        initializer().run();
        assertEquals("encoded-password", admin.getPassword());
        assertTrue(admin.isAccountNonLocked());
        assertTrue(admin.getActivationStatus());
        assertEquals(0, admin.getFailedAttempts());
        assertEquals(UserType.NORMAL, admin.getUserType());
        verify(repository).save(admin);
    }

    @Test
    void resetDoesNotPromoteEmployee() {
        environment.withProperty("F6S_ADMIN_RESET_PASSWORD", "true");
        User employee = new User();
        employee.setRole(Role.ROLE_EMPLOYEE);
        when(repository.findByEmail("admin@example.test")).thenReturn(Optional.of(employee));
        assertThrows(IllegalStateException.class, () -> initializer().run());
        verify(repository, never()).save(any());
    }

    @Test
    void newAdminCanUsePasswordLogin() throws Exception {
        when(repository.findByEmail("admin@example.test")).thenReturn(Optional.empty());
        initializer().run();
        verify(repository).save(argThat(user -> user.getRole() == Role.ROLE_ADMIN
                && user.getUserType() == UserType.NORMAL
                && Boolean.TRUE.equals(user.getActivationStatus())
                && "encoded-password".equals(user.getPassword())));
    }
}
