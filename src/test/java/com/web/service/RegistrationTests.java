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
    @BeforeEach void setup() {
        service = new UserService(); users = mock(UserRepository.class); codes = mock(OtpCodeRepository.class);
        otp = mock(SendOtp.class); mail = mock(MailService.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(any())).thenReturn("test-hash");
        when(otp.genOtp()).thenReturn("123456");
        when(codes.save(any())).thenAnswer(call -> {OtpCode code=call.getArgument(0);code.setId(9L);return code;});
        ReflectionTestUtils.setField(service,"userRepository",users);
        ReflectionTestUtils.setField(service,"otpCodeRepository",codes);
        ReflectionTestUtils.setField(service,"sendOtp",otp);
        ReflectionTestUtils.setField(service,"mailService",mail);
        ReflectionTestUtils.setField(service,"passwordEncoder",encoder);
    }
    private void register() {service.registerUser("  New@Example.test  ","secret123","Test Customer","0912345678","secret123");}
    private void pendingUser() {
        User user = new User(); user.setActivationStatus(false);
        when(users.findByEmail("pending@example.test")).thenReturn(Optional.of(user));
    }
    @Test void normalizesEmailAndUsesRequiredOtpDelivery() {
        register();
        verify(otp).sendOtp("new@example.test","123456");
        verify(users).save(argThat(u->u.getEmail().equals("new@example.test")&&!u.getActivationStatus()));
        verify(codes).save(argThat(c->c.getExpiryTime().isAfter(LocalDateTime.now().plusMinutes(9))));
    }
    @Test void missingSmtpCannotCreatePendingAccount() {
        doThrow(new MessageException("SMTP missing")).when(mail).requireConfigured();
        assertThrows(MessageException.class,this::register);
        verify(users,never()).save(any()); verify(codes,never()).save(any());
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
}
