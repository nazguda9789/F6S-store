package com.web.service;

import com.web.entity.User;
import com.web.enums.*;
import com.web.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.*;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OAuthAccountServiceTests {
    UserRepository users; PendingRegistrationRepository pending; OtpCodeRepository codes; OAuthAccountService service;
    @BeforeEach void setup() {
        users=mock(UserRepository.class);pending=mock(PendingRegistrationRepository.class);codes=mock(OtpCodeRepository.class);
        when(users.save(any())).thenAnswer(call->call.getArgument(0));
        service=new OAuthAccountService(users,pending,codes);
    }
    OAuth2User profile(boolean verified) {
        return new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("sub","provider-subject","email"," Customer@Example.test ","email_verified",verified,
                        "name","Google Name","picture","https://example.test/avatar","role","ROLE_ADMIN"),"sub");
    }
    User existing(boolean activated) {
        User u=new User();u.setId(42L);u.setEmail("customer@example.test");u.setRole(Role.ROLE_USER);
        u.setActived(true);u.setDeleted(false);u.setActivationStatus(activated);u.setUserType(UserType.NORMAL);
        when(users.findByEmail(u.getEmail())).thenReturn(Optional.of(u));return u;
    }
    @Test void newGoogleCustomerIsVerifiedAndCannotChooseAnAdminRole() {
        OAuth2User result=service.completeLogin("google",profile(true));
        assertEquals("customer@example.test",result.getName());
        assertEquals("ROLE_USER",result.getAuthorities().iterator().next().getAuthority());
        verify(users).save(argThat(u->u.getRole()==Role.ROLE_USER && u.getActivationStatus()
                && u.getUserType()==UserType.GOOGLE && u.getIsGoogleUser() && u.getPassword()==null));
        verify(pending).deleteById("customer@example.test");
    }
    @Test void unverifiedEmailCannotCreateOrLinkAnyAccount() {
        assertThrows(OAuth2AuthenticationException.class,()->service.completeLogin("google",profile(false)));
        verifyNoInteractions(users,pending,codes);
    }
    @Test void missingEmailIsRejectedBeforePersistence() {
        OAuth2User noEmail=new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")),Map.of("sub","id"),"sub");
        assertThrows(OAuth2AuthenticationException.class,()->service.completeLogin("google",noEmail));
        verifyNoInteractions(users,pending,codes);
    }
    @Test void activePasswordAccountKeepsItsCredentialAndStoredRole() {
        User u=existing(true);u.setPassword("existing-verified-hash");u.setRole(Role.ROLE_ADMIN);
        OAuth2User result=service.completeLogin("google",profile(true));
        assertEquals("ROLE_ADMIN",result.getAuthorities().iterator().next().getAuthority());
        assertEquals("existing-verified-hash",u.getPassword());assertEquals(UserType.NORMAL,u.getUserType());
        assertEquals(42L,u.getId());
    }
    @Test void legacyUnverifiedPasswordIsDiscardedWhenGoogleProvesEmailOwnership() {
        User u=existing(false);u.setPassword("unverified-attacker-chosen-hash");u.setPhone("unverified");
        service.completeLogin("google",profile(true));
        assertNull(u.getPassword());assertNull(u.getPhone());assertTrue(u.getActivationStatus());assertEquals(UserType.GOOGLE,u.getUserType());
        verify(codes).invalidatePreviousRegistrationCodes(u.getEmail(),-1L);
    }
    @Test void disabledAccountIsNeverAutomaticallyEnabled() {
        User u=existing(true);u.setActived(false);
        assertThrows(OAuth2AuthenticationException.class,()->service.completeLogin("google",profile(true)));
        verify(users,never()).save(any());assertFalse(u.getActived());
    }
    @Test void deletedAccountCannotUseGoogle() {
        User u=existing(true);u.setDeleted(true);
        assertThrows(OAuth2AuthenticationException.class,()->service.completeLogin("google",profile(true)));
        verify(users,never()).save(any());
    }
    @Test void manualLockCannotBeBypassedWithGoogle() {
        User u=existing(true);u.setAccountNonLocked(false);
        assertThrows(OAuth2AuthenticationException.class,()->service.completeLogin("google",profile(true)));
        verify(users,never()).save(any());
    }
    @Test void expiredTemporaryLockCanBeRecoveredAfterVerifiedGoogleLogin() {
        User u=existing(true);u.setAccountNonLocked(false);u.setLockTime(LocalDateTime.now().minusMinutes(3));
        service.completeLogin("google",profile(true));assertTrue(u.isAccountNonLocked());assertNull(u.getLockTime());
    }
    @Test void unverifiedPrivilegedAccountIsNotActivatedBySso() {
        User u=existing(false);u.setRole(Role.ROLE_EMPLOYEE);
        assertThrows(OAuth2AuthenticationException.class,()->service.completeLogin("google",profile(true)));
        verify(users,never()).save(any());
    }
    @Test void facebookDoesNotImplicitlyTakeOverAnExistingPasswordAccount() {
        existing(true);
        assertThrows(OAuth2AuthenticationException.class,()->service.completeLogin("facebook",profile(true)));
        verify(users,never()).save(any());
    }
}
