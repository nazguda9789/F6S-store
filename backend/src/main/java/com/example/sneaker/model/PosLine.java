package com.example.sneaker.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "pos_lines",
    uniqueConstraints = @UniqueConstraint(columnNames = {"draft_id", "variant_id"}))
@Getter
@Setter
@NoArgsConstructor
public class PosLine {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "draft_id")
  private PosDraft draft;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "variant_id")
  private ProductVariant variant;

  @Column(nullable = false)
  private int quantity;
}
