package com.example.sneaker.model;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "staff_accounts")
@Getter
@Setter
@NoArgsConstructor
public class StaffAccount {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 30)
  private String code;

  @Column(nullable = false, unique = true, length = 50)
  private String username;

  @Column(nullable = false, length = 100)
  private String passwordHash;

  @Column(nullable = false, length = 120)
  private String fullName;

  @Column(nullable = false, unique = true, length = 254)
  private String email;

  @Column(nullable = false, length = 15)
  private String phone;

  @Column(length = 10)
  private String gender;

  private LocalDate birthDate;

  @Column(length = 500)
  private String address;

  @Column(nullable = false)
  private boolean active = true;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();
}
