package com.example.sneaker.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import lombok.*;

@Entity
@Table(name = "orders")
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

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
  @OrderBy("id ASC")
  private List<OrderItem> items = new ArrayList<>();
}
