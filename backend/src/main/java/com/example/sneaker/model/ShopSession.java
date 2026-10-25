package com.example.sneaker.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "shop_sessions")
@Getter
@Setter
@NoArgsConstructor
public class ShopSession {
  @Id
  @Column(length = 64)
  private String tokenHash;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false)
  private ShopAccount account;

  @Column(nullable = false)
  private Instant expiresAt;
}
