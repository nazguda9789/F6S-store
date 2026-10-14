package com.web.api.test;

import com.web.bot.TeleSend;
import com.web.service.SmsService;
import com.web.utils.SpeedSMSAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class SmsController {

    @Autowired
    private SpeedSMSAPI smsService;

    @Autowired
    private TeleSend teleSend;

    @GetMapping("/send-otp")
    public void sendOtp() throws IOException {
        teleSend.sendMessage("Test send API");
    }

    private String generateOtp() {
        // Logic để tạo mã OTP
        return "123456";
    }
}
