package com.example.sneaker.model;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class Customer {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 30)
  private String code;

  @Column(nullable = false, length = 120)
  private String fullName;

  @Column(length = 254)
  private String email;

  @Column(nullable = false, unique = true, length = 15)
  private String phone;

  @Column(length = 10)
  private String gender;

  private LocalDate birthDate;

  @Column(nullable = false)
  private boolean active = true;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();
}
