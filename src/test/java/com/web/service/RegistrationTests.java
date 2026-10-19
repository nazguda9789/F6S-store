package com.web.service;

import com.web.entity.*;
import com.web.exception.MessageException;
import com.web.repository.*;
import com.web.utils.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistrationTests {
    UserService service;
    UserRepository users;
    OtpCodeRepository codes;
    SendOtp otp;
    MailService mail;
    PendingRegistrationRepository pending;
    @BeforeEach void setup() {
        service = new UserService(); users = mock(UserRepository.class); codes = mock(OtpCodeRepository.class);
        otp = mock(SendOtp.class); mail = mock(MailService.class);
        pending = mock(PendingRegistrationRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(any())).thenReturn("test-hash");
        when(otp.genOtp()).thenReturn("123456");
        when(codes.save(any())).thenAnswer(call -> {OtpCode code=call.getArgument(0);code.setId(9L);return code;});
        ReflectionTestUtils.setField(service,"userRepository",users);
        ReflectionTestUtils.setField(service,"otpCodeRepository",codes);
        ReflectionTestUtils.setField(service,"pendingRegistrationRepository",pending);
        ReflectionTestUtils.setField(service,"sendOtp",otp);
        ReflectionTestUtils.setField(service,"mailService",mail);
        ReflectionTestUtils.setField(service,"passwordEncoder",encoder);
    }
    private void register() {service.registerUser("  New@Example.test  ","secret123","Test Customer","0912345678","secret123");}
    private void pendingUser() {
        User user = new User(); user.setActivationStatus(false); user.setActived(true);
        user.setRole(com.web.enums.Role.ROLE_USER); user.setUserType(com.web.enums.UserType.NORMAL);
        when(users.findByEmail("pending@example.test")).thenReturn(Optional.of(user));
    }
    @Test void normalizesEmailAndUsesRequiredOtpDelivery() {
        register();
        verify(otp).sendOtp("new@example.test","123456");
        verify(users,never()).save(any());
        verify(pending).save(argThat(p->p.getEmail().equals("new@example.test")&&p.getPasswordHash().equals("test-hash")));
        verify(codes).save(argThat(c->c.getExpiryTime().isAfter(LocalDateTime.now().plusMinutes(9))));
    }
    @Test void missingSmtpCannotCreatePendingAccount() {
        doThrow(new MessageException("SMTP missing")).when(mail).requireConfigured();
        assertThrows(MessageException.class,this::register);
        verify(users,never()).save(any()); verify(codes,never()).save(any());
        verify(pending,never()).save(any());
    }
    @Test void deliveryFailurePropagatesForTransactionalRollback() {
        doThrow(new MessageException("SMTP failed")).when(otp).sendOtp(anyString(),anyString());
        assertThrows(MessageException.class,this::register);
        assertTrue(java.util.Arrays.stream(UserService.class.getMethods())
                .filter(m->m.getName().equals("registerUser")).allMatch(m->m.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class)));
    }
    @Test void resendIsLimitedToOncePerMinute() {
        pendingUser();
        OtpCode previous=new OtpCode();previous.setLocalDateTime(LocalDateTime.now());
        when(codes.findTopByEmailAndOtpTypeOrderByLocalDateTimeDesc(any(),any())).thenReturn(Optional.of(previous));
        assertThrows(MessageException.class,()->service.sendNewOtp("pending@example.test"));
        verify(otp,never()).sendOtp(anyString(),anyString()); verify(codes,never()).save(any());
    }
    @Test void resendInvalidatesOldCodesOnlyAfterSuccessfulDelivery() {
        pendingUser();service.sendNewOtp("pending@example.test");
        var ordered=inOrder(otp,codes);
        ordered.verify(otp).sendOtp("pending@example.test","123456");
        ordered.verify(codes).invalidatePreviousRegistrationCodes("pending@example.test",9L);
    }
    @Test void failedResendKeepsOldCodesAvailable() {
        pendingUser();doThrow(new MessageException("SMTP failed")).when(otp).sendOtp(anyString(),anyString());
        assertThrows(MessageException.class,()->service.sendNewOtp("pending@example.test"));
        verify(codes,never()).invalidatePreviousRegistrationCodes(any(),any());
    }
    @Test void expiredOtpCannotActivateAccount() {
        OtpCode code=new OtpCode();code.setExpiryTime(LocalDateTime.now().minusSeconds(1));
        when(codes.findByEmailAndCodeAndUsedFalse(any(),any())).thenReturn(Optional.of(code));
        assertThrows(MessageException.class,()->service.verifyOtp("pending@example.test","123456"));
        verify(users,never()).save(any());
    }
    @Test void correctCodeCreatesAccountAndRemovesPendingRequest() {
        PendingRegistration p=new PendingRegistration();p.setEmail("pending@example.test");p.setPasswordHash("verified-hash");
        p.setFullName("Customer");p.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(pending.findForUpdate(p.getEmail())).thenReturn(Optional.of(p));
        OtpCode code=new OtpCode();code.setId(7L);code.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        when(codes.findByEmailAndCodeAndUsedFalse(any(),any())).thenReturn(Optional.of(code));
        assertTrue(service.verifyOtp(p.getEmail(),"123456"));
        verify(users).save(argThat(u->Boolean.TRUE.equals(u.getActivationStatus())&&u.getPassword().equals("verified-hash")));
        verify(pending).delete(p);assertTrue(code.isUsed());
    }
    @Test void legacyPendingAccountCanStillVerify() {
        pendingUser();OtpCode code=new OtpCode();code.setId(7L);code.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        when(codes.findByEmailAndCodeAndUsedFalse(any(),any())).thenReturn(Optional.of(code));
        assertTrue(service.verifyOtp("pending@example.test","123456"));
        verify(users).save(argThat(u->Boolean.TRUE.equals(u.getActivationStatus())));
    }
    @Test void duplicatePendingRequestCannotReplacePasswordBeforeVerification() {
        PendingRegistration p=new PendingRegistration();p.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(pending.findForUpdate("new@example.test")).thenReturn(Optional.of(p));
        assertThrows(MessageException.class,this::register);
        verify(pending,never()).save(any());verify(otp,never()).sendOtp(anyString(),anyString());
    }
}
