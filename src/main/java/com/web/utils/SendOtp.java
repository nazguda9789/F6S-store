package com.web.utils;

import com.web.entity.OtpCode;
import com.web.repository.OtpCodeRepository;
import com.web.utils.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class SendOtp {

    @Autowired
    private MailService mailService;

    @Value("${brand.name}")
    String brandName;

    @Value("${logo.url}")
    String logoUrl;

//    @Value("${brand.address}")
    String brandAddress = "Xóm 25, Thôn 10, Hòa Hậu, Lý Nhân, Hà Nam";

    @Value("${brand.phone}")
    String brandPhone;

    @Value("${brand.email}")
    String brandEmail;

    @Value("${support.url}")
    String supportUrl ;

    @Value("${datlaimatkhau.url}")
    String datLaiMatKhauUrl ;

    @Autowired
    private OtpCodeRepository otpRepo;
    private static final SecureRandom random = new SecureRandom();
    private static final int EXPIRE_MINUTES = 5;

    public void sendOtp(String email, String otp) {
        String html = "<!doctype html>" +
                "<html lang=\"vi\">" +
                "<head>" +
                "  <meta charset=\"utf-8\">" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "  <title>Xác thực tài khoản - " + brandName + "</title>" +
                "</head>" +
                "<body style=\"margin:0;padding:0;background:#f4f6f8;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial;\">" +
                "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">" +
                "<tr><td align=\"center\">" +
                "<table style=\"background:#ffffff;max-width:600px;margin:28px auto;border-radius:12px;overflow:hidden;box-shadow:0 8px 30px rgba(20,30,40,0.08);\">" +
                "<tr><td style=\"padding:20px;\">" +
                "<table width=\"100%\"><tr>" +
                "<td style=\"width:72px;\"><img src='" + logoUrl + "' alt='" + brandName + "' width=\"64\" height=\"64\" style=\"border-radius:12px;\"/></td>" +
                "<td style=\"padding-left:12px;\">" +
                "<div style=\"font-size:18px;font-weight:700;color:#0f1724;\">" + brandName + "</div>" +
                "<div style=\"font-size:13px;color:#64748b;margin-top:4px;\">Xin chào bạn — cảm ơn đã chọn cửa hàng chúng tôi</div>" +
                "</td></tr></table>" +
                "</td></tr>" +
                "<tr><td style=\"padding:28px 24px;text-align:center;\">" +
                "<div style=\"font-size:16px;color:#334155;margin-bottom:6px;\">Xin chào,</div>" +
                "<div style=\"font-size:14px;color:#475569;margin-bottom:20px;\">Bạn hoặc ai đó vừa yêu cầu mã OTP để xác thực tài khoản tại <strong>" + brandName + "</strong>. Nhập mã sau để hoàn tất xác thực.</div>" +
                "<div style=\"margin:0 auto;display:inline-block;\">" +
                "<div style=\"font-size:28px;letter-spacing:6px;background:#0f1724;color:#fff;padding:12px 20px;border-radius:8px;font-weight:700;\">" + otp + "</div>" +
                "</div>" +
                "<div style=\"margin-top:20px;\">" +
                "<a href='" + supportUrl + "' style=\"text-decoration:none;padding:12px 20px;border-radius:10px;background:linear-gradient(90deg,#06b6d4,#3b82f6);color:#fff;font-weight:600;\">Không nhận mã?</a>" +
                "</div>" +
                "<div style=\"margin-top:18px;font-size:13px;color:#94a3b8;\">Mã có hiệu lực trong <strong>10 phút</strong>. Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email hoặc liên hệ chúng tôi.</div>" +
                "</td></tr>" +
                "<tr><td style=\"padding:20px 24px;font-size:13px;color:#64748b;border-top:1px solid #eef2f7;\">" +
                "<div><strong>Địa chỉ:</strong> " + brandAddress + "</div>" +
                "<div><strong>Liên hệ:</strong> " + brandPhone + " • " + brandEmail + "</div>" +
                "<div><strong>Giờ hỗ trợ:</strong> Thứ 2 - Chủ nhật, 08:00 - 20:00</div>" +
                "</td></tr>" +
                "<tr><td style=\"padding:16px 24px;font-size:12px;color:#94a3b8;text-align:center;\">" +
                "<div style=\"margin-bottom:6px;\">Cảm ơn bạn đã sử dụng <strong>" + brandName + "</strong>!</div>" +
                "<div>Email này được gửi tự động, vui lòng không trả lời trực tiếp. Nếu cần hỗ trợ, truy cập " + supportUrl + " hoặc liên hệ " + brandPhone + ".</div>" +
                "</td></tr>" +
                "</table></td></tr></table>" +
                "</body></html>";

        mailService.sendEmailRequired(email, "Mã OTP xác thực - " + brandName, html, false, true);
    }


    public void sendLinkResetPass(String email, String otp) {
        String html = "<!doctype html>" +
                "<html lang=\"vi\">" +
                "<head>" +
                "  <meta charset=\"utf-8\">" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "  <title>Quên Mật Khẩu - " + brandName + "</title>" +
                "</head>" +
                "<body style=\"margin:0;padding:0;background:#f4f6f8;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial;\">" +
                "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">" +
                "<tr><td align=\"center\">" +
                "<table style=\"background:#ffffff;max-width:600px;margin:28px auto;border-radius:12px;overflow:hidden;box-shadow:0 8px 30px rgba(20,30,40,0.08);\">" +
                "<tr><td style=\"padding:20px;\">" +
                "<table width=\"100%\"><tr>" +
                "<td style=\"width:72px;\"><img src='" + logoUrl + "' alt='" + brandName + "' width=\"64\" height=\"64\" style=\"border-radius:12px;\"/></td>" +
                "<td style=\"padding-left:12px;\">" +
                "<div style=\"font-size:18px;font-weight:700;color:#0f1724;\">" + brandName + "</div>" +
                "<div style=\"font-size:13px;color:#64748b;margin-top:4px;\">Xin chào bạn — cảm ơn đã chọn cửa hàng chúng tôi</div>" +
                "</td></tr></table>" +
                "</td></tr>" +
                "<tr><td style=\"padding:28px 24px;text-align:center;\">" +
                "<div style=\"font-size:16px;color:#334155;margin-bottom:6px;\">Xin chào,</div>" +
                "<div style=\"font-size:14px;color:#475569;margin-bottom:20px;\">Bạn hoặc ai đó vừa gửi yêu cầu quên mật khẩu tại <strong>" + brandName + "</strong>. Click button bên dưới để đặt lại mật khẩu.</div>" +
                "<div style=\"margin:0 auto;display:inline-block;\">" +
                "</div>" +
                "<div style=\"margin-top:20px;\">" +
                "<a href='" + datLaiMatKhauUrl+"?key="+otp + "&email="+email + " ' target='_blank' style=\"text-decoration:none;padding:12px 20px;border-radius:10px;background:linear-gradient(90deg,#06b6d4,#3b82f6);color:#fff;font-weight:600;\">Đặt lại mật khẩu</a>" +
                "</div>" +
                "<div style=\"margin-top:18px;font-size:13px;color:#94a3b8;\">Mã có hiệu lực trong <strong>60 phút</strong>. Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email hoặc liên hệ chúng tôi.</div>" +
                "</td></tr>" +
                "<tr><td style=\"padding:20px 24px;font-size:13px;color:#64748b;border-top:1px solid #eef2f7;\">" +
                "<div><strong>Địa chỉ:</strong> " + brandAddress + "</div>" +
                "<div><strong>Liên hệ:</strong> " + brandPhone + " • " + brandEmail + "</div>" +
                "<div><strong>Giờ hỗ trợ:</strong> Thứ 2 - Chủ nhật, 08:00 - 20:00</div>" +
                "</td></tr>" +
                "<tr><td style=\"padding:16px 24px;font-size:12px;color:#94a3b8;text-align:center;\">" +
                "<div style=\"margin-bottom:6px;\">Cảm ơn bạn đã sử dụng <strong>" + brandName + "</strong>!</div>" +
                "<div>Email này được gửi tự động, vui lòng không trả lời trực tiếp. Nếu cần hỗ trợ, truy cập " + supportUrl + " hoặc liên hệ " + brandPhone + ".</div>" +
                "</td></tr>" +
                "</table></td></tr></table>" +
                "</body></html>";

        mailService.sendEmail(email, "Quên Mật Khẩu - " + brandName, html, false, true);
    }

    public String genOtp(){
        String otp = String.valueOf((int)(Math.random()*900000)+100000); // 6 chữ số
        return otp;
    }
}
