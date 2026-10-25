package com.example.sneaker.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "product_color_images",
    uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "color", "display_order"}))
@Getter
@Setter
@NoArgsConstructor
public class ProductColorImage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id")
  private Product product;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "media_id")
  private MediaAsset media;

  @Column(nullable = false, length = 60)
  private String color;

  @Column(nullable = false)
  private int displayOrder;
}
