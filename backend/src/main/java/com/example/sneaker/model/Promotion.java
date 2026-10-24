package com.example.sneaker.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import lombok.*;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
public class Promotion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 30)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, length = 10)
  private String type;

  @Column(name = "discount_value", nullable = false, precision = 19, scale = 0)
  private BigDecimal value;

  @Column(nullable = false, precision = 19, scale = 0)
  private BigDecimal minSubtotal;

  @Column(precision = 19, scale = 0)
  private BigDecimal maxDiscount;

  @Column(nullable = false)
  private int maxUses;

  @Column(nullable = false)
  private int usedCount;

  @Column(nullable = false)
  private Instant startsAt;

  @Column(nullable = false)
  private Instant endsAt;

  @Column(nullable = false)
  private boolean active = true;

  @ManyToMany
  @JoinTable(
      name = "promotion_products",
      joinColumns = @JoinColumn(name = "promotion_id"),
      inverseJoinColumns = @JoinColumn(name = "product_id"))
  private Set<Product> products = new HashSet<>();
}
