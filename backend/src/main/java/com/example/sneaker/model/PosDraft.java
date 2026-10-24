package com.example.sneaker.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import lombok.*;

@Entity
@Table(name = "pos_drafts", indexes = @Index(columnList = "owner,status"))
@Getter
@Setter
@NoArgsConstructor
public class PosDraft {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String owner;

  @Column(nullable = false, unique = true, length = 36)
  private String requestKey;

  @Column(nullable = false, length = 20)
  private String status = "OPEN";

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  @OneToMany(mappedBy = "draft", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id ASC")
  private List<PosLine> lines = new ArrayList<>();

  // SQL Server UNIQUE nullable columns allow only one NULL. Keep the optional one-to-one
  // relation in a separate table so every OPEN draft can exist without an invoice.
  @OneToOne(fetch = FetchType.LAZY)
  @JoinTable(
      name = "pos_draft_invoices",
      joinColumns = @JoinColumn(name = "draft_id", nullable = false, unique = true),
      inverseJoinColumns = @JoinColumn(name = "invoice_id", nullable = false, unique = true))
  private CustomerOrder invoice;
}
