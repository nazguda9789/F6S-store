package com.example.sneaker.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;
import lombok.*;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@org.hibernate.annotations.Check(constraints = "price > 0")
public class Product {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 40)
  private String code;

  @Column(nullable = false, length = 180)
  private String name;

  @Column(nullable = false, length = 100)
  private String brand;

  @Column(nullable = false, precision = 19, scale = 0)
  private BigDecimal price;

  @Column(nullable = false, length = 4000)
  private String description;

  @Column(nullable = false, length = 1000)
  private String imageUrl;

  @Column(length = 1000)
  private String hoverImageUrl;

  @Column(nullable = false)
  private boolean active = true;

  @Column(nullable = false)
  private boolean featured;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private Category category;

  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
  @OrderBy("id ASC")
  private List<ProductVariant> variants = new ArrayList<>();

  @Transient
  public int getStock() {
    return variants.stream()
        .filter(ProductVariant::isActive)
        .mapToInt(ProductVariant::getStock)
        .sum();
  }

  @Transient
  public List<String> getColorways() {
    return variants.stream()
        .filter(ProductVariant::isActive)
        .map(ProductVariant::getColor)
        .distinct()
        .toList();
  }
}
