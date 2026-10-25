package com.example.sneaker.repository;

import com.example.sneaker.model.ProductVariant;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VariantRepository extends JpaRepository<ProductVariant, Long> {
  boolean existsByColorIgnoreCase(String color);

  boolean existsBySize(String size);

  java.util.Optional<ProductVariant> findBySku(String sku);

  interface Identity {
    Long getId();

    Long getProductId();
  }

  @Query("select v.id as id, v.product.id as productId from ProductVariant v where v.id in :ids")
  List<Identity> identities(@Param("ids") Collection<Long> ids);
}
