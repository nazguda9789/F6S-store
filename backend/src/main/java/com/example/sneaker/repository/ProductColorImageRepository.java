package com.example.sneaker.repository;

import com.example.sneaker.model.ProductColorImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductColorImageRepository extends JpaRepository<ProductColorImage, Long> {
  boolean existsByMediaId(String mediaId);
}
