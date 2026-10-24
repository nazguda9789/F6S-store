package com.web.repository;

import com.web.entity.Product;
import com.web.entity.ProductDetail;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductDetailRepository extends JpaRepository<ProductDetail, Long> {
    Optional<ProductDetail> findBySkuIgnoreCase(String sku);
    boolean existsByProductIdAndColorIdAndSizeIdAndIdNot(Long productId, Long colorId, Long sizeId, Long id);

    @Query("select p.id from ProductDetail p where p.colorId = ?1")
    Long findByColor(Long colorId);

    @Query("select p.id from ProductDetail p where p.sizeId = ?1")
    Long findBySize(Long sizeId);

    @Query("select p from ProductDetail p where p.productId = ?1 order by p.colorId desc")
    List<ProductDetail> findByProductId(Long productId);

    @Query("select p from ProductDetail p where p.productId = ?1 and p.active = true order by p.colorId desc")
    List<ProductDetail> findByProductIdAndActive(Long productId);

    @Modifying
    @Transactional
    @Query("delete from ProductDetail p where p.productId = ?1")
    void deleteByProduct(Long id);

    @Query(value = "select DISTINCT p.color_id from product_detail p where p.product_id = ?1", nativeQuery = true)
    List<Long> findIdColorByProduct(Long id);

    @Query(value = "select d.id from product_detail d where d.color = ?1 and d.product_id = ?2 and d.id != ?3 limit 1", nativeQuery = true)
    Long findByColorAndProductId(Long colorId, Long productId, Long id);

    @Query("select p.id from ProductDetail p where p.color.id = ?1 and p.size.id = ?2 and p.productId = ?3")
    Long checkExist(Long colorId, Long sizeId, Long productId);

    @Query("select distinct(p.productId) from ProductDetail p where p.id in ?1")
    List<Long> getProductIds(List<Long> dataIds);

    @Query("select cast(p.stock as int) from ProductDetail p where p.id = ?1")
    Integer getQuantityById(Long id);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductDetail p where p.id = ?1")
    Optional<ProductDetail> findByIdForUpdate(Long id);

    @Query("select p from ProductDetail p where p.active = true and p.productId in ?1 order by p.color.id desc")
    List<ProductDetail> findAllByProductId(List<Long> listIdProduct);

    @Query("select cast(sum(p.stock) as int) from ProductDetail p")
    Integer totalProduct();

    List<ProductDetail> findTop5ByStockLessThanOrderByStockAsc(Integer stock);
    Optional<ProductDetail> findByProductIdAndColorIdAndSizeId(Long productId, Long colorId, Long sizeId);

    @Query(value = "select distinct p.* from product_detail pd inner join product p on p.id = pd.product_id where pd.id = ?1", nativeQuery = true)
    Product findProductById(Long idProductDetail);
}
