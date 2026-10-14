package com.web.repository;

import com.web.entity.Category;
import com.web.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory,Long> {

    @Modifying
    @Transactional
    @Query("delete from ProductCategory p where p.product.id = ?1")
    int deleteByProduct(Long productId);

    List<ProductCategory> findByProductId(Long productId);
    List<Long> findProductIdsByCategoryId(Long categoryId);

    @Query("SELECT DISTINCT pc.category.id FROM ProductCategory pc WHERE pc.product.id IN ?1")
    List<Long> findCategoryIdsByProductIds(List<Long> productIds);

    @Query("SELECT pc.category.id FROM ProductCategory pc WHERE pc.product.id = ?1")
    List<Long> findCategoryIdsByProductId(Long productId);
}

