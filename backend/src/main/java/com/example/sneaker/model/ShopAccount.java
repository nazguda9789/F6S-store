package com.example.sneaker.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "shop_accounts")
@Getter
@Setter
@NoArgsConstructor
public class ShopAccount {
  @Id
  @Column(length = 36)
  private String id;

  @Column(nullable = false, unique = true, length = 254)
  private String email;

  @Column(nullable = false, length = 120)
  private String fullName = "";

  @Column(length = 15)
  private String phone;

  @Column(length = 500)
  private String address;

  @Column(nullable = false)
  private boolean active = true;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();
}
