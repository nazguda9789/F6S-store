package com.example.sneaker.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "order_refunds")
@Getter
@Setter
@NoArgsConstructor
public class OrderRefund {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false, unique = true)
  private CustomerOrder order;

  @Column(nullable = false, length = 20)
  private String status = "REQUESTED";

  @Column(nullable = false, length = 500)
  private String reason;

  @Column(nullable = false, length = 100)
  private String requestedBy;

  @Column(length = 100)
  private String resolvedBy;

  @Column(length = 120)
  private String settlementReference;

  @Column(nullable = false, precision = 19, scale = 0)
  private BigDecimal amount;

  @Column(nullable = false)
  private boolean restocked;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  private Instant resolvedAt;
}
