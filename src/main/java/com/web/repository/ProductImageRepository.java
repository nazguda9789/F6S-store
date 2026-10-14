package com.web.repository;

import com.web.entity.ProductImage;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    @Query("select p from ProductImage p where p.product.id = ?1")
    List<ProductImage> findByProduct(Long idProduct);

    @Modifying
    @Transactional
    @Query("delete from ProductImage p where p.product.id = ?1")
    void deleteByProduct(Long id);
}
