package com.web.repository;

import com.web.dto.CartResponse;
import com.web.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    List<Cart> findByUserId(Long userId);

    @Query("select count(c.id) from Cart c where c.userId = ?1")
    Long countByUser(Long userId);

    @Query(value = "SELECT COALESCE(\n" +
            "    CAST(SUM(\n" +
            "        c.quantity * \n" +
            "        CASE \n" +
            "            WHEN pd.price IS NULL THEN p.price \n" +
            "            ELSE pd.price \n" +
            "        END\n" +
            "    ) AS DOUBLE),\n" +
            "    0\n" +
            ") AS total_cart\n" +
            "FROM carts c\n" +
            "INNER JOIN product_detail pd ON pd.id = c.product_detail_id\n" +
            "INNER JOIN product p ON p.id = pd.product_id\n" +
            "WHERE c.user_id = ?1;\n", nativeQuery = true)
    Double totalCartByUser(Long userId);

    @Query("select c.id from Cart c where c.userId = ?1 and c.productDetail.id = ?2")
    Long findIdByUserAndProductDetail(Long userId, Long id);

    @Query(value = "select \n" +
            "    c.product_detail_id as dataId, \n" +
            "    p.name as productName, \n" +
            "    cl.name as colorName, \n" +
            "    s.name as sizeName, \n" +
            "    c.quantity as quantity, \n" +
            "    p.slug as slug, \n" +
            "    p.id as productId, \n" +
            "    cast(\n" +
            "        case \n" +
            "            when pd.price is null then p.price \n" +
            "            else pd.price \n" +
            "        end \n" +
            "        as double\n" +
            "    ) as price, \n" +
            "    coalesce(\n" +
            "        (\n" +
            "            select pci.image \n" +
            "            from product_color_image pci \n" +
            "            where pci.color_id = cl.id \n" +
            "              and pci.product_id = p.id \n" +
            "            limit 1\n" +
            "        ), \n" +
            "        p.image\n" +
            "    ) as image\n" +
            "from carts c \n" +
            "inner join product_detail pd on pd.id = c.product_detail_id \n" +
            "inner join color cl on cl.id = pd.color \n" +
            "inner join size s on s.id = pd.size \n" +
            "inner join product p on p.id = pd.product_id \n" +
            "where c.user_id = ?1;", nativeQuery = true)
    List<CartResponse.CartDetailInject> getAllDetailByUser(Long userId);

    @Modifying
    @Transactional
    @Query("delete from Cart c where c.userId = ?1 and c.productDetail.id = ?2")
    int deleteByUserAndDetailId(Long userId, Long detailId);

    @Query("select c.quantity from Cart c where c.productDetail.id = ?1 and c.userId = ?2")
    Integer getQuantityByDetailAndUser(Long productDetailId, Long userId);

    @Modifying
    @Transactional
    @Query("update Cart c set c.quantity = ?3 where c.userId = ?1 and c.productDetail.id = ?2")
    int updateQuantity(Long userId, Long detailId, Integer quantity);

    @Modifying
    @Transactional
    @Query("delete from Cart c where c.productDetail.productId = ?1")
    int deleteByProduct(Long productId);
}