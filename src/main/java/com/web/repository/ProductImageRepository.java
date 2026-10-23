package com.web.repository;

import com.web.entity.ProductImage;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    @Query("select i from ProductImage i join fetch i.product p where lower(p.name) like lower(concat('%', :search, '%'))")
    org.springframework.data.domain.Page<ProductImage> searchLibrary(@org.springframework.data.repository.query.Param("search") String search,
                                                                    org.springframework.data.domain.Pageable pageable);

    @Query("select p from ProductImage p where p.product.id = ?1")
    List<ProductImage> findByProduct(Long idProduct);

    @Modifying
    @Transactional
    @Query("delete from ProductImage p where p.product.id = ?1")
    void deleteByProduct(Long id);
}
