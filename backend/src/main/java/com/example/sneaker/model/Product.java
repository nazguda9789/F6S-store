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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "brand_attribute_id")
  private ShoeAttribute brandAttribute;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sole_attribute_id")
  private ShoeAttribute soleAttribute;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "material_attribute_id")
  private ShoeAttribute materialAttribute;

  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("displayOrder ASC,id ASC")
  private List<ProductColorImage> colorImages = new ArrayList<>();

  private java.time.Instant createdAt;
  private java.time.Instant updatedAt;

  @Column(length = 100)
  private String createdBy;

  @Column(length = 100)
  private String updatedBy;

  private String actor() {
    var auth =
        org.springframework.security.core.context.SecurityContextHolder.getContext()
            .getAuthentication();
    return auth == null ? "system" : auth.getName();
  }

  @PrePersist
  private void created() {
    createdAt = java.time.Instant.now();
    updatedAt = createdAt;
    createdBy = actor();
    updatedBy = createdBy;
  }

  @PreUpdate
  private void updated() {
    updatedAt = java.time.Instant.now();
    updatedBy = actor();
  }

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
