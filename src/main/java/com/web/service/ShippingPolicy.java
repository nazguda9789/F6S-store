package com.web.service;

import com.web.ghn.GhnClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@Service
public class ShippingPolicy {
    @Value("${shipping.provider:local}") private String provider;
    @Value("${shipping.cost:30000}") private double localFee;
    @Value("${amount.freeShipping:500000}") private double freeThreshold;
    private final GhnClient ghn;
    public ShippingPolicy(GhnClient ghn) { this.ghn = ghn; }
    public boolean isLocal() { return "local".equalsIgnoreCase(provider); }
    public double fee(double total, int weight, HttpServletRequest request) {
        if (isLocal()) return total >= freeThreshold ? 0 : localFee;
        if (!"ghn".equalsIgnoreCase(provider)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Phương thức vận chuyển chưa hợp lệ.");
        }
        try {
            int district = Integer.parseInt(request.getParameter("districtId"));
            String ward = request.getParameter("ward");
            Map<?, ?> data = (Map<?, ?>) ghn.calculateShippingFee(weight, district, ward).get("data");
            Object quoted = data.get("total");
            if (!(quoted instanceof Number) || !Double.isFinite(((Number) quoted).doubleValue())
                    || ((Number) quoted).doubleValue() < 0) throw new IllegalArgumentException();
            return total >= freeThreshold ? 0 : ((Number) quoted).doubleValue();
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn đầy đủ địa chỉ giao hàng.");
        }
    }
}
