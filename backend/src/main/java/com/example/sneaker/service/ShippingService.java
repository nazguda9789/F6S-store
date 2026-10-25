package com.example.sneaker.service;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ShippingService {
  private final BigDecimal fee, freeAbove;

  public ShippingService(
      @Value("${app.shipping-fee:0}") BigDecimal fee,
      @Value("${app.shipping-free-above:2000000}") BigDecimal freeAbove) {
    if (fee.signum() < 0 || fee.scale() > 0 || freeAbove.signum() < 0 || freeAbove.scale() > 0)
      throw new IllegalArgumentException("Phí vận chuyển phải là số nguyên VND không âm");
    this.fee = fee;
    this.freeAbove = freeAbove;
  }

  public BigDecimal quote(BigDecimal merchandise) {
    return merchandise.compareTo(freeAbove) >= 0 ? BigDecimal.ZERO : fee;
  }

  public record Policy(BigDecimal fee, BigDecimal freeAbove, String mode) {}

  public Policy policy() {
    return new Policy(fee, freeAbove, "MANUAL");
  }
}
