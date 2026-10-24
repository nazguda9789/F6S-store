package com.example.sneaker.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pos_scan_events")
@Getter
@Setter
@NoArgsConstructor
public class PosScanEvent {
  @Id
  @Column(length = 36)
  private String eventKey;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "draft_id")
  private PosDraft draft;

  @Column(nullable = false, length = 60)
  private String sku;

  @Column(nullable = false)
  private int quantity;
}
