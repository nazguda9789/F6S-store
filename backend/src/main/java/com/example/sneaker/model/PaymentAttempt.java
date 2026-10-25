package com.example.sneaker.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "payment_attempts")
@Getter
@Setter
@NoArgsConstructor
public class PaymentAttempt {
  @Id
  @Column(length = 36)
  private String id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false, unique = true)
  private CustomerOrder order;

  @Column(nullable = false, precision = 19, scale = 0)
  private BigDecimal amount;

  @Column(nullable = false, length = 20)
  private String status = "PENDING";

  @Column(length = 40)
  private String providerTransaction;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  @Column(nullable = false)
  private Instant expiresAt;

  private Instant settledAt;
}
