package com.example.sneaker.repository;

import com.example.sneaker.model.Product;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProductRepository
    extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Product p where p.id=:id")
  Optional<Product> lockById(@Param("id") Long id);

  boolean existsByCategoryIdAndActiveTrue(Long id);

  boolean existsByBrandIgnoreCase(String brand);

  boolean existsBySoleAttributeId(Long id);

  boolean existsByMaterialAttributeId(Long id);

  boolean existsByImageUrlOrHoverImageUrl(String url, String hoverUrl);
}
