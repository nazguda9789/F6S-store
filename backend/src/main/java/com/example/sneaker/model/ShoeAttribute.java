package com.example.sneaker.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "shoe_attributes",
    uniqueConstraints = @UniqueConstraint(columnNames = {"attribute_type", "name"}))
@Getter
@Setter
@NoArgsConstructor
public class ShoeAttribute {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 50)
  private String code;

  @Column(name = "attribute_type", nullable = false, length = 15)
  private String type;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false)
  private boolean active = true;
}
