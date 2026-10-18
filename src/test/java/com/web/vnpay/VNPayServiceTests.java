package com.web.vnpay;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import static org.junit.jupiter.api.Assertions.*;

class VNPayServiceTests {
    @Test
    void missingSecretIsRejected() {
        String original = VNPayConfig.vnp_HashSecret;
        try {
            VNPayConfig.vnp_HashSecret = "";
            assertThrows(ResponseStatusException.class,
                    () -> new VNPayService().createOrder(100000, "demo", "https://example.test/check-vnpay"));
        } finally { VNPayConfig.vnp_HashSecret = original; }
    }

    @Test
    void sandboxUrlHasValidSignatureAmountTimezoneAndReturnUrl() throws Exception {
        String originalCode = VNPayConfig.vnp_TmnCode;
        String originalSecret = VNPayConfig.vnp_HashSecret;
        try {
            VNPayConfig.vnp_TmnCode = "TESTCODE";
            VNPayConfig.vnp_HashSecret = "synthetic-test-key-not-a-merchant-secret";
            String url = new VNPayService().createOrder(30000000, "demo", "https://example.test/check-vnpay");
            URI uri = URI.create(url);
            assertEquals("sandbox.vnpayment.vn", uri.getHost());
            Map<String, String> encoded = new TreeMap<>();
            for (String field : uri.getRawQuery().split("&")) {
                String[] pair = field.split("=", 2);
                encoded.put(pair[0], pair[1]);
            }
            String signature = encoded.remove("vnp_SecureHash");
            String payload = encoded.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                    .collect(java.util.stream.Collectors.joining("&"));
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(VNPayConfig.vnp_HashSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            assertEquals(HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))), signature);
            assertEquals("3000000000", encoded.get("vnp_Amount"));
            assertEquals("TESTCODE", encoded.get("vnp_TmnCode"));
            assertEquals("https://example.test/check-vnpay", URLDecoder.decode(encoded.get("vnp_ReturnUrl"), StandardCharsets.UTF_8));
            DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            LocalDateTime created = LocalDateTime.parse(encoded.get("vnp_CreateDate"), format);
            assertTrue(java.time.Duration.between(created, LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))).abs().getSeconds() < 10);
            assertEquals(created.plusMinutes(15), LocalDateTime.parse(encoded.get("vnp_ExpireDate"), format));
            assertThrows(IllegalArgumentException.class,
                    () -> new VNPayService().createOrder(0, "demo", "https://example.test/check-vnpay"));
        } finally {
            VNPayConfig.vnp_TmnCode = originalCode;
            VNPayConfig.vnp_HashSecret = originalSecret;
        }
    }
}
