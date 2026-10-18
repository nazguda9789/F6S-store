package com.web.service;

import com.web.ghn.GhnClient;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShippingPolicyTests {
    private final GhnClient ghn = mock(GhnClient.class);
    private ShippingPolicy policy(String provider) {
        ShippingPolicy policy = new ShippingPolicy(ghn);
        ReflectionTestUtils.setField(policy, "provider", provider);
        ReflectionTestUtils.setField(policy, "localFee", 30000d);
        ReflectionTestUtils.setField(policy, "freeThreshold", 500000d);
        return policy;
    }
    @Test void localShippingNeedsNoGhnAndIgnoresBrowserFee() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("phiShip", "1");
        assertEquals(30000d, policy("local").fee(499000, 220, request));
        verifyNoInteractions(ghn);
    }
    @Test void localFreeShippingAtThreshold() {
        assertEquals(0d, policy("local").fee(500000, 220, new MockHttpServletRequest()));
        verifyNoInteractions(ghn);
    }
    @Test void ghnFeeComesFromProviderNotBrowser() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("districtId", "12"); request.setParameter("ward", "123");
        request.setParameter("phiShip", "1");
        when(ghn.calculateShippingFee(220,12,"123")).thenReturn(Map.of("data", Map.of("total",45000)));
        assertEquals(45000d, policy("ghn").fee(499000,220,request));
    }
    @Test void missingGhnAddressCannotBypassShippingValidation() {
        assertThrows(ResponseStatusException.class, () -> policy("ghn").fee(499000,220,new MockHttpServletRequest()));
    }
    @Test void invalidProviderDoesNotSilentlyEnableLocalShipping() {
        assertThrows(ResponseStatusException.class, () -> policy("invalid").fee(499000,220,new MockHttpServletRequest()));
    }
}
