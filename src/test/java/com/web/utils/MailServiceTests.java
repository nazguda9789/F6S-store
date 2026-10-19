package com.web.utils;

import com.web.exception.MessageException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MailServiceTests {
    @Test void usesConfiguredSenderAndSendsSynchronously() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        MailService service = new MailService(sender);
        ReflectionTestUtils.setField(service,"username","store@example.test");
        service.sendEmailRequired("customer@example.test","OTP","Test message",false,true);
        verify(sender).send(message);
        assertEquals("store@example.test",message.getFrom()[0].toString());
    }
    @Test void missingConfigurationIsVisibleWithoutAttemptingDelivery() {
        JavaMailSender sender = mock(JavaMailSender.class);
        MailService service = new MailService(sender);
        MessageException error = assertThrows(MessageException.class,
                () -> service.sendEmailRequired("customer@example.test","OTP","Test",false,true));
        assertTrue(error.getDefaultMessage().contains("MAIL_USERNAME"));
        verifyNoInteractions(sender);
    }
    @Test void providerFailureIsNotSwallowedOrExposedToCustomer() {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("private-provider-detail")).when(sender).send(any(MimeMessage.class));
        MailService service = new MailService(sender);
        ReflectionTestUtils.setField(service,"username","store@example.test");
        MessageException error = assertThrows(MessageException.class,
                () -> service.sendEmailRequired("customer@example.test","OTP","Test",false,true));
        assertFalse(error.getDefaultMessage().contains("private-provider-detail"));
    }
    @Test void optionalOrderMailRemainsBestEffort() {
        MailService service = new MailService(mock(JavaMailSender.class));
        assertDoesNotThrow(() -> service.sendEmail("customer@example.test","Order","Test",false,true));
    }
}
