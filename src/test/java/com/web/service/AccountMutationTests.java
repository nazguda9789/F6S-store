package com.web.service;

import com.web.entity.User;
import com.web.enums.Role;
import com.web.repository.UserRepository;
import com.web.utils.UserUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountMutationTests {
    UserService service; UserRepository users; UserUtils actors; User existing;
    @BeforeEach void setup(){
        service=new UserService(); users=mock(UserRepository.class); actors=mock(UserUtils.class);
        ReflectionTestUtils.setField(service,"userRepository",users); ReflectionTestUtils.setField(service,"userUtils",actors);
        existing=input();existing.setActived(true);existing.setPassword("existing-hash");existing.setAvatar("/fixture/avatar.png");
        when(users.findById(14L)).thenReturn(Optional.of(existing));when(users.save(any())).thenAnswer(i->i.getArgument(0));
    }
    User input(){User u=new User();u.setId(14L);u.setEmail("staff@example.test");u.setFullName("Staff fixture");u.setPhone("0900000001");u.setRole(Role.ROLE_EMPLOYEE);u.setAccountNonLocked(true);return u;}
    @Test void stoppingEmployeePersistsStatusWithoutChangingPasswordOrRole(){
        User request=input();request.setActived(false);
        assertTrue(service.updateUser(14L,request).isSuccess());assertFalse(existing.getActived());
        assertEquals("existing-hash",existing.getPassword());assertEquals(Role.ROLE_EMPLOYEE,existing.getRole());
        assertEquals("/fixture/avatar.png",existing.getAvatar());
    }
    @Test void omittedStatusDoesNotSilentlyDeactivateExistingAccount(){
        assertTrue(service.updateUser(14L,input()).isSuccess());assertTrue(existing.getActived());
    }
    @Test void currentAdminCannotDisableOwnAccount(){
        existing.setRole(Role.ROLE_ADMIN);when(actors.getUserWithAuthority()).thenReturn(existing);
        User request=input();request.setRole(Role.ROLE_ADMIN);request.setActived(false);
        assertFalse(service.updateUser(14L,request).isSuccess());assertTrue(existing.getActived());verify(users,never()).save(any());
    }
}
