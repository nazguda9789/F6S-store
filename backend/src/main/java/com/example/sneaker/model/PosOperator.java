package com.example.sneaker.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pos_operators")
@Getter
@Setter
@NoArgsConstructor
public class PosOperator {
  @Id
  @Column(length = 100)
  private String username;
}
