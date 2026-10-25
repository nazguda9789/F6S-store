package com.example.sneaker.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "media_assets")
@Getter
@Setter
@NoArgsConstructor
public class MediaAsset {
  @Id
  @Column(length = 36)
  private String id;

  @Column(nullable = false, unique = true, length = 64)
  private String sha256;

  @Column(nullable = false, length = 80)
  private String storedName;

  @Column(nullable = false, length = 120)
  private String originalName;

  @Column(nullable = false, length = 20)
  private String contentType;

  @Column(nullable = false)
  private int width;

  @Column(nullable = false)
  private int height;

  @Column(nullable = false)
  private long byteSize;

  @Column(nullable = false, length = 100)
  private String uploadedBy;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  @Column(nullable = false)
  private boolean active = true;

  public String publicUrl() {
    return "/api/media/" + id;
  }
}
