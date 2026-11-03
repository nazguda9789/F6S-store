package com.example.demodatn.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {
    @Test void appliesTheApprovedPolicyIncludingUnicodeAndBcryptByteLimit() {
        for (String weak : new String[]{null,"","123","abcdefgh","12345678","abc1234"})
            assertFalse(PasswordPolicy.isValid(weak));
        assertTrue(PasswordPolicy.isValid("abc12345"));
        assertTrue(PasswordPolicy.isValid("MậtKhẩu123"));
        assertTrue(PasswordPolicy.isValid("a1"+"x".repeat(70)));
        assertFalse(PasswordPolicy.isValid("a1"+"x".repeat(71)));
        assertFalse(PasswordPolicy.isValid("Đ".repeat(36)+"1"));
        assertEquals(PasswordPolicy.MESSAGE,assertThrows(IllegalArgumentException.class,
                () -> PasswordPolicy.requireValid("123")).getMessage());
    }
}
