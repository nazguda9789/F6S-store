package com.web.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

@Service
public class SmsService {

    @Value("${speedsms.api.key:}")
    private String apiKey;

    @Value("${speedsms.api.url:}")
    private String apiUrl;

    public String sendOtp(String phoneNumber, String otp) {
        if (!org.springframework.util.StringUtils.hasText(apiKey)
                || !org.springframework.util.StringUtils.hasText(apiUrl)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "SMS chưa được cấu hình.");
        }
        String message = "Mã OTP của bạn là: " + otp;
        String url = String.format("%s?to=%s&message=%s&apiKey=%s", apiUrl, phoneNumber, message, apiKey);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, String.class);
        return response.getBody();
    }
}

