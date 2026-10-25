package com.example.sneaker.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "order_events")
@Getter
@Setter
@NoArgsConstructor
public class OrderEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id")
  private CustomerOrder order;

  @Column(length = 30)
  private String fromStatus;

  @Column(nullable = false, length = 30)
  private String toStatus;

  @Column(nullable = false, length = 100)
  private String actor;

  @Column(nullable = false, length = 500)
  private String note;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();
}
