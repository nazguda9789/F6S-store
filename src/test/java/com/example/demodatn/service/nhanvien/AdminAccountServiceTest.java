package com.example.demodatn.service.nhanvien;
import com.example.demodatn.dto.nhanvien.AccountPasswordForm;
import com.example.demodatn.dto.nhanvien.AccountProfileForm;
import com.example.demodatn.entity.nhanvien.User;
import com.example.demodatn.repository.nhanvien.EmployeeRepository;
import com.example.demodatn.repository.nhanvien.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.BeanPropertyBindingResult;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminAccountServiceTest {
    private final UserRepository users=mock(UserRepository.class);
    private final EmployeeRepository employees=mock(EmployeeRepository.class);
    private final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder();
    private final AdminAccountService service=new AdminAccountService(users,employees,encoder);
    private User user;
    @BeforeEach void setup(){user=new User();user.setId(1);user.setUsername("staff");user.setPassword(encoder.encode("OldPassword123"));when(users.lockByUsername("staff")).thenReturn(Optional.of(user));when(employees.findByUser_Id(1)).thenReturn(Optional.empty());}
    @Test void noEmployeeUsesUserProfile() {user.setFullName("User Only");assertEquals("User Only",service.profile(user).getFullName());}
    @Test void wrongCurrentPasswordDoesNotWrite() {
        var f=new AccountPasswordForm();f.setCurrentPassword("wrong");f.setNewPassword("NewPassword123");f.setConfirmPassword(f.getNewPassword());var e=new BeanPropertyBindingResult(f,"passwordForm");
        service.password("staff",f,e);assertTrue(e.hasFieldErrors("currentPassword"));verify(users,never()).save(any());
    }
    @Test void sameAndMismatchedPasswordAreRejected() {
        var f=new AccountPasswordForm();f.setCurrentPassword("OldPassword123");f.setNewPassword("OldPassword123");f.setConfirmPassword("different");var e=new BeanPropertyBindingResult(f,"passwordForm");
        service.password("staff",f,e);assertTrue(e.hasFieldErrors("newPassword"));assertTrue(e.hasFieldErrors("confirmPassword"));verify(users,never()).save(any());
    }
    @Test void newPasswordIsEncodedAndOldOneStopsMatching() {
        var f=new AccountPasswordForm();f.setCurrentPassword("OldPassword123");f.setNewPassword("NewPassword123");f.setConfirmPassword(f.getNewPassword());var e=new BeanPropertyBindingResult(f,"passwordForm");
        service.password("staff",f,e);assertFalse(e.hasErrors());assertTrue(encoder.matches("NewPassword123",user.getPassword()));assertFalse(encoder.matches("OldPassword123",user.getPassword()));
    }
    @Test void duplicateEmailDoesNotChangeProfile() {
        var f=new AccountProfileForm();f.setFullName("Changed");f.setEmail("other@shop.test");f.setPhone("0901234567");when(users.otherAccountsWithEmail(f.getEmail(),1)).thenReturn(1L);var e=new BeanPropertyBindingResult(f,"profileForm");
        service.update("staff",f,e);assertTrue(e.hasFieldErrors("email"));assertNull(user.getFullName());verify(users,never()).save(any());
    }
    @Test void unicodePasswordOverBcryptByteLimitIsRejectedWithoutException() {
        var f=new AccountPasswordForm();f.setCurrentPassword("OldPassword123");f.setNewPassword("ấ".repeat(30));f.setConfirmPassword(f.getNewPassword());var e=new BeanPropertyBindingResult(f,"passwordForm");
        service.password("staff",f,e);assertTrue(e.hasFieldErrors("newPassword"));verify(users,never()).save(any());
    }
}
