package com.example.sneaker.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id")
  private CustomerOrder order;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "variant_id")
  private ProductVariant variant;

  @Column(nullable = false, length = 180)
  private String productName;

  @Column(nullable = false, length = 60)
  private String sku;

  @Column(nullable = false, length = 60)
  private String color;

  @Column(nullable = false, length = 10)
  private String size;

  @Column(nullable = false)
  private int quantity;

  @Column(nullable = false, precision = 19, scale = 0)
  private BigDecimal unitPrice;
}
