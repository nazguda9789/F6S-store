package com.example.sneaker.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customer_addresses")
@Getter
@Setter
@NoArgsConstructor
public class CustomerAddress {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id")
  private Customer customer;

  @Column(nullable = false, length = 120)
  private String recipientName;

  @Column(nullable = false, length = 15)
  private String phone;

  @Column(nullable = false, length = 100)
  private String province;

  @Column(nullable = false, length = 100)
  private String district;

  @Column(nullable = false, length = 100)
  private String ward;

  @Column(nullable = false, length = 250)
  private String detail;

  @Column(nullable = false)
  private boolean defaultAddress;

  @Column(nullable = false)
  private boolean active = true;

  public String fullAddress() {
    return detail + ", " + ward + ", " + district + ", " + province;
  }
}
