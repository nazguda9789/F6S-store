package com.web.repository;

import com.web.dto.ProductColorImageView;
import com.web.entity.ProductColorImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ProductColorImageRepository extends JpaRepository<ProductColorImage, Long> {
    @Transactional
    void deleteByProductId(Long productId);

    @Query(value = "select pc.id, pc.image, pc.product_id as productId, pc.color_id as colorId ,\n" +
            "c.code as code, c.name as colorName\n" +
            "from product_color_image pc\n" +
            "inner join color c on c.id = pc.color_id where pc.product_id = ?1 ", nativeQuery = true)
    List<ProductColorImageView> findByProduct(Long id);

    @Modifying
    @Transactional
    @Query("update ProductColorImage p set p.image = ?1 where p.id = ?2")
    int changeImage(String image, Long id);

    @Modifying
    @Transactional
    @Query("delete from ProductColorImage p where p.colorId = ?1 and p.productId = ?2")
    void deleteByColorAndProduct(Long colorId, Long productId);

    @Query("select p.id from ProductColorImage p where p.colorId = ?1 and p.productId = ?2")
    Long checkExist(Long colorId, Long id);

    @Query("select p.image from ProductColorImage p where p.productId = ?1 and p.colorId = ?2")
    String getImageByProductAndColor(Long productId, Long colorId);
}
