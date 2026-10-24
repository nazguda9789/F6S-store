package com.example.sneaker.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "product_variants",
    uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "color", "shoe_size"}))
@Getter
@Setter
@NoArgsConstructor
@org.hibernate.annotations.Check(constraints = "stock >= 0 and price > 0")
public class ProductVariant {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id")
  private Product product;

  @Column(nullable = false, unique = true, length = 60)
  private String sku;

  @Column(nullable = false, length = 60)
  private String color;

  @Column(name = "shoe_size", nullable = false, length = 10)
  private String size;

  @Column(nullable = false)
  private int stock;

  @Column(nullable = false, precision = 19, scale = 0)
  private java.math.BigDecimal price;

  @Column(nullable = false)
  private boolean active = true;
}
