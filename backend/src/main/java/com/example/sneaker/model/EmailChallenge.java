package com.example.sneaker.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "email_challenges")
@Getter
@Setter
@NoArgsConstructor
public class EmailChallenge {
  @Id
  @Column(length = 36)
  private String id;

  @Column(nullable = false, length = 254)
  private String email;

  @Column(nullable = false, length = 64)
  private String codeHash;

  @Column(length = 64)
  private String grantHash;

  @Column(nullable = false)
  private int attempts;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  @Column(nullable = false)
  private Instant expiresAt;

  private Instant verifiedAt;
  private Instant consumedAt;
}
