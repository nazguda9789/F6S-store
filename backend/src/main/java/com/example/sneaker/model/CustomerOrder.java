package com.example.sneaker.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import lombok.*;

@Entity
@Table(
    name = "orders",
    indexes =
        @Index(name = "idx_orders_cancellation", columnList = "cancellation_status,created_at"))
@Getter
@Setter
@NoArgsConstructor
public class CustomerOrder {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 36)
  private String requestKey;

  @Column(nullable = false, unique = true, length = 40)
  private String code;

  @Column(nullable = false, length = 120)
  private String customerName;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id")
  private Customer customer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "shop_account_id")
  private ShopAccount shopAccount;

  @Column(nullable = false, length = 254)
  private String email;

  @Column(nullable = false, length = 15)
  private String phone;

  @Column(nullable = false, length = 500)
  private String address;

  @Column(nullable = false, length = 500)
  private String note;

  @Column(nullable = false, length = 30)
  private String status = "PENDING";

  @Column(nullable = false, length = 10)
  private String paymentMethod = "COD";

  @Column(nullable = false, precision = 19, scale = 0)
  private BigDecimal total;

  @Column(nullable = false, precision = 19, scale = 0)
  private BigDecimal subtotal = BigDecimal.ZERO;

  @Column(nullable = false, precision = 19, scale = 0)
  private BigDecimal discount = BigDecimal.ZERO;

  @Column(length = 30)
  private String promotionCode;

  @Column(nullable = false, length = 10)
  private String channel = "ONLINE";

  @Column(precision = 19, scale = 0)
  private BigDecimal cashReceived;

  @Column(precision = 19, scale = 0)
  private BigDecimal cashChange;

  private Instant completedAt;

  @Column(length = 20)
  private String paymentStatus;

  @Column(precision = 19, scale = 0)
  private BigDecimal shippingFee;

  @Column(length = 60)
  private String carrier;

  @Column(length = 100)
  private String trackingCode;

  private Instant shippedAt;

  @Column(precision = 19, scale = 0)
  private BigDecimal refundedAmount;

  private Instant stockReleasedAt;

  @Column(length = 20)
  private String cancellationStatus;

  @Column(length = 500)
  private String cancellationReason;

  private Instant cancellationRequestedAt;
  private Instant cancellationResolvedAt;

  @Column(length = 100)
  private String cancellationResolvedBy;

  @Column(length = 500)
  private String cancellationDecisionReason;

  @Column(length = 500)
  private String deliveryIssue;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
  @OrderBy("id ASC")
  private List<OrderItem> items = new ArrayList<>();
}
