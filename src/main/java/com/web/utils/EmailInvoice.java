package com.web.utils;

import com.web.entity.Invoice;
import com.web.entity.InvoiceDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class EmailInvoice {

    @Autowired
    private MailService mailService;

    public void buildOrderSuccessEmail(Invoice invoice, List<InvoiceDetail> details) {
        if(invoice.getEmail() == null){
            return;
        }
        NumberFormat vnFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

        // build rows sản phẩm
        StringBuilder productRows = new StringBuilder();
        for (InvoiceDetail d : details) {
            String productName = d.getProductName();
            String variant = d.getColorName() + " - " + d.getSizeName();
            String price = vnFormat.format(d.getPrice()) + " ₫";
            String totalRow = vnFormat.format(d.getPrice() * d.getQuantity()) + " ₫";

            productRows.append("<tr>"
                    + "<td>" + productName + "</td>"
                    + "<td>" + variant + "</td>"
                    + "<td>" + d.getQuantity() + "</td>"
                    + "<td>" + price + "</td>"
                    + "<td>" + totalRow + "</td>"
                    + "</tr>");
        }

        String discount = "";
        if (invoice.getDiscount() != null && invoice.getDiscount() > 0) {
            discount = "<p class=\"total\">Giảm giá (Voucher " + invoice.getVoucherCode() + "): -"
                    + vnFormat.format(invoice.getDiscount()) + " ₫</p>";
        }

        String shipCost = (invoice.getShipCost() != null && invoice.getShipCost() > 0)
                ? vnFormat.format(invoice.getShipCost()) + " ₫"
                : "Miễn phí";

        double total = invoice.getTotalAmount()
                - (invoice.getDiscount() == null ? 0 : invoice.getDiscount())
                + (invoice.getShipCost() == null ? 0 : invoice.getShipCost());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String formattedDate = invoice.getCreatedDate().format(formatter);

        String html =
                "<!DOCTYPE html>" +
                        "<html lang=\"vi\">" +
                        "<head>" +
                        "  <meta charset=\"UTF-8\">" +
                        "  <title>Xác nhận đơn hàng</title>" +
                        "  <style>" +
                        "    body {font-family: Arial, Helvetica, sans-serif; background:#f4f4f4; margin:0; padding:0; color:#333;}" +
                        "    .email-container {max-width:700px; margin:20px auto; background:#fff; border-radius:10px; overflow:hidden; box-shadow:0 4px 10px rgba(0,0,0,0.1);}" +
                        "    .header {background:#ff6600; color:#fff; padding:20px; text-align:center;}" +
                        "    .header h1 {margin:0; font-size:22px;}" +
                        "    .content {padding:20px;}" +
                        "    .section-title {font-size:16px; margin-bottom:8px; font-weight:bold; color:#ff6600;}" +
                        "    .info-box {border:1px solid #ddd; border-radius:6px; padding:12px 16px; margin-bottom:20px;}" +
                        "    .info-row {display:flex; justify-content:space-between; margin-bottom:6px; font-size:14px;}" +
                        "    table {width:100%; border-collapse:collapse; margin-top:10px;}" +
                        "    table th, table td {border:1px solid #ddd; padding:10px; text-align:left; font-size:14px;}" +
                        "    table th {background:#f7f7f7;}" +
                        "    .total {text-align:right; font-weight:bold; font-size:15px; padding-top:10px;}" +
                        "    .footer {background:#fafafa; padding:15px 20px; text-align:center; font-size:13px; color:#666; border-top:1px solid #eee;}" +
                        "    .btn {display:inline-block; padding:10px 18px; background:#ff6600; color:#fff; text-decoration:none; border-radius:6px; margin-top:15px; font-size:14px;}" +
                        "  </style>" +
                        "</head>" +
                        "<body>" +
                        "  <div class=\"email-container\">" +
                        "    <div class=\"header\">" +
                        "      <h1>🎉 Đặt hàng thành công!</h1>" +
                        "      <p>Cảm ơn bạn đã mua sắm tại <strong>Shop F6S</strong></p>" +
                        "    </div>" +
                        "    <div class=\"content\">" +

                        "      <div class=\"section-title\">Thông tin khách hàng</div>" +
                        "      <div class=\"info-box\">" +
                        "        <div class=\"info-row\"><span>👤 Họ tên:</span> <span>" + invoice.getFullName() + "</span></div>" +
                        "        <div class=\"info-row\"><span>📧 Email:</span> <span>" + invoice.getEmail() + "</span></div>" +
                        "        <div class=\"info-row\"><span>📱 SĐT:</span> <span>" + invoice.getPhone() + "</span></div>" +
                        "        <div class=\"info-row\"><span>📍 Địa chỉ:</span> <span>" + invoice.getAddressDetail() + ", " + invoice.getWardName() + ", " + invoice.getProvinceName() + "</span></div>" +
                        "      </div>" +

                        "      <div class=\"section-title\">Thông tin đơn hàng</div>" +
                        "      <div class=\"info-box\">" +
                        "        <div class=\"info-row\"><span>Mã đơn hàng:</span> <span>" + invoice.getCode() + "</span></div>" +
                        "        <div class=\"info-row\"><span>Ngày đặt:</span> <span>" + formattedDate + "</span></div>" +
                        "        <div class=\"info-row\"><span>Phương thức thanh toán:</span> <span>" + invoice.getPayType().name() + "</span></div>" +
                        "      </div>" +

                        "      <div class=\"section-title\">Chi tiết sản phẩm</div>" +
                        "      <table>" +
                        "        <thead>" +
                        "          <tr>" +
                        "            <th>Sản phẩm</th>" +
                        "            <th>Phân loại</th>" +
                        "            <th>SL</th>" +
                        "            <th>Đơn giá</th>" +
                        "            <th>Thành tiền</th>" +
                        "          </tr>" +
                        "        </thead>" +
                        "        <tbody>" + productRows + "</tbody>" +
                        "      </table>" +

                        "      <p class=\"total\">Tạm tính: " + vnFormat.format(invoice.getTotalAmount()) + " ₫</p>" +
                        discount +
                        "      <p class=\"total\">Phí vận chuyển: " + shipCost + "</p>" +
                        "      <p class=\"total\">Tổng thanh toán: <span style=\"color:#ff0000;\">" + vnFormat.format(total) + " ₫</span></p>" +

                        "      <div style=\"text-align:center;\">" +
                        "        <a href=\"https://shopabc.vn/orders/" + invoice.getId() + "\" class=\"btn\">Xem đơn hàng</a>" +
                        "      </div>" +

                        "    </div>" +
                        "    <div class=\"footer\">" +
                        "      <p>Shop F6S - Giay bong da chinh hang F6S</p>" +
                        "      <p>📍 Địa chỉ: 456 Lê Lợi, Quận 3, TP.HCM | 📞 0909 123 456</p>" +
                        "      <p>© 2025 Shop F6S. All rights reserved.</p>" +
                        "    </div>" +
                        "  </div>" +
                        "</body>" +
                        "</html>";

        mailService.sendEmail(invoice.getEmail(),"Đặt hàng thành công", html, false, true);
    }

}
