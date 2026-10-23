package com.web.repository;

import com.web.dto.ProductStatisticProjection;
import com.web.dto.ProductView;
import com.web.dto.ProductWeight;
import com.web.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {


    @Query("select pc.id from ProductCategory pc where pc.category.id = ?1")
    Long checkProductExistByCategory(Long categoryId);


    @Query("""
    SELECT 
        pd.productId AS productId,
        COALESCE(SUM(pd.stock), 0) AS totalStock
    FROM ProductDetail pd
    WHERE pd.productId IN :productIds
    GROUP BY pd.productId
""")
    List<ProductStatisticProjection> getProductStatistics(@Param("productIds") List<Long> productIds);

//    @Query(value = "select p.id, p.name, p.image, p.slug, cast(p.price as double) as price, " +
//            "cast(p.ole_price as double) as olePrice from product p where p.active = true", nativeQuery = true)
//    Page<ProductView> getSanPhamBanChay(Pageable pageable);

    @Query(value = """
SELECT 
    p.id,
    p.name,
    p.image,
    p.slug,
    CAST(p.price AS DOUBLE) AS price,
    CAST(p.ole_price AS DOUBLE) AS olePrice,

    /* số tiền giảm */
    CAST(
        CASE 
            WHEN d.id IS NULL THEN 0
            WHEN d.discount_product_type = 'PERCENT' 
                THEN LEAST(p.price * d.discount_value / 100, IFNULL(d.max_discount_value, p.price))
            WHEN d.discount_product_type = 'FIXED'
                THEN d.discount_value
            ELSE 0
        END
    AS DOUBLE) AS discountAmount,

    /* giá sau giảm */
    CAST(
        CASE 
            WHEN d.id IS NULL THEN p.price
            WHEN d.discount_product_type = 'PERCENT' 
                THEN p.price - LEAST(p.price * d.discount_value / 100, IFNULL(d.max_discount_value, p.price))
            WHEN d.discount_product_type = 'FIXED'
                THEN p.price - d.discount_value
            ELSE p.price
        END
    AS DOUBLE) AS finalPrice,

    /* % giảm */
    CAST(
        CASE 
            WHEN d.id IS NULL THEN 0
            WHEN d.discount_product_type = 'PERCENT' 
                THEN d.discount_value
            WHEN d.discount_product_type = 'FIXED'
                THEN ROUND((d.discount_value / p.price) * 100)
            ELSE 0
        END
    AS SIGNED) AS discountPercent

FROM product p

LEFT JOIN discount_product d 
    ON d.product_id = p.id
    AND NOW() BETWEEN d.start_date AND d.end_date

WHERE p.active = true
""",
            countQuery = "SELECT COUNT(*) FROM product p WHERE p.active = true",
            nativeQuery = true)
    Page<ProductView> getSanPhamBanChay(Pageable pageable);

//    @Query(value = "select DISTINCT p.id, p.name as name, p.image, p.slug, \n" +
//            "       cast(p.price as double) as price,\n" +
//            "       cast(p.ole_price as double) as olePrice\n" +
//            "from product p\n" +
//            "join product_category pc on p.id = pc.product_id\n" +
//            "where p.active = true\n" +
//            "  and pc.category_id = ?1\n", nativeQuery = true)
//    Page<ProductView> getProductByCategory(Long categoryId,Pageable pageable);

    @Query(value = """
SELECT DISTINCT
    p.id,
    p.name,
    p.image,
    p.slug,
    CAST(p.price AS DOUBLE) AS price,
    CAST(p.ole_price AS DOUBLE) AS olePrice,

    /* số tiền giảm */
    CAST(
        CASE 
            WHEN d.id IS NULL THEN 0
            WHEN d.discount_product_type = 'PERCENT' 
                THEN LEAST(p.price * d.discount_value / 100, IFNULL(d.max_discount_value, p.price))
            WHEN d.discount_product_type = 'FIXED'
                THEN d.discount_value
            ELSE 0
        END
    AS DOUBLE) AS discountAmount,

    /* giá sau giảm */
    CAST(
        CASE 
            WHEN d.id IS NULL THEN p.price
            WHEN d.discount_product_type = 'PERCENT' 
                THEN p.price - LEAST(p.price * d.discount_value / 100, IFNULL(d.max_discount_value, p.price))
            WHEN d.discount_product_type = 'FIXED'
                THEN p.price - d.discount_value
            ELSE p.price
        END
    AS DOUBLE) AS finalPrice,

    /* % giảm */
    CAST(
        CASE 
            WHEN d.id IS NULL THEN 0
            WHEN d.discount_product_type = 'PERCENT' 
                THEN d.discount_value
            WHEN d.discount_product_type = 'FIXED'
                THEN ROUND((d.discount_value / p.price) * 100)
            ELSE 0
        END
    AS SIGNED) AS discountPercent

FROM product p

JOIN product_category pc 
    ON p.id = pc.product_id

LEFT JOIN discount_product d 
    ON d.product_id = p.id
    AND NOW() BETWEEN d.start_date AND d.end_date

WHERE p.active = true
AND pc.category_id = ?1
""",
            countQuery = """
SELECT COUNT(DISTINCT p.id)
FROM product p
JOIN product_category pc ON p.id = pc.product_id
WHERE p.active = true
AND pc.category_id = ?1
""",
            nativeQuery = true)
    Page<ProductView> getProductByCategory(Long categoryId, Pageable pageable);

//    @Query(value = "select DISTINCT p.id, p.name, p.image, p.slug, \n" +
//            "       cast(p.price as double) as price,\n" +
//            "       cast(p.ole_price as double) as olePrice\n" +
//            "from product p\n" +
//            "where p.active = true\n" +
//            "  and p.name like ?1\n", nativeQuery = true)
//    Page<ProductView> getProductByParam(String search,Pageable pageable);

    @Query(value = """
SELECT DISTINCT
    p.id,
    p.name,
    p.image,
    p.slug,
    CAST(p.price AS DOUBLE) AS price,
    CAST(p.ole_price AS DOUBLE) AS olePrice,

    CAST(
        CASE
            WHEN d.id IS NULL THEN 0
            WHEN d.discount_product_type = 'PERCENT'
                THEN LEAST(p.price * d.discount_value / 100, IFNULL(d.max_discount_value, p.price))
            WHEN d.discount_product_type = 'FIXED'
                THEN d.discount_value
            ELSE 0
        END
    AS DOUBLE) AS discountAmount,

    CAST(
        CASE
            WHEN d.id IS NULL THEN p.price
            WHEN d.discount_product_type = 'PERCENT'
                THEN p.price - LEAST(p.price * d.discount_value / 100, IFNULL(d.max_discount_value, p.price))
            WHEN d.discount_product_type = 'FIXED'
                THEN p.price - d.discount_value
            ELSE p.price
        END
    AS DOUBLE) AS finalPrice,

    CAST(
        CASE
            WHEN d.id IS NULL THEN 0
            WHEN d.discount_product_type = 'PERCENT'
                THEN d.discount_value
            WHEN d.discount_product_type = 'FIXED'
                THEN ROUND((d.discount_value / p.price) * 100)
            ELSE 0
        END
    AS SIGNED) AS discountPercent

FROM product p

LEFT JOIN discount_product d
    ON d.product_id = p.id
    AND NOW() BETWEEN d.start_date AND d.end_date

WHERE p.active = true
AND (p.name LIKE ?1 OR p.code LIKE ?1 OR EXISTS (SELECT 1 FROM product_detail pd WHERE pd.product_id=p.id AND pd.active=true AND (pd.sku LIKE ?1 OR pd.legacy_sku LIKE ?1)))
""",
            countQuery = """
SELECT COUNT(p.id)
FROM product p
WHERE p.active = true
AND (p.name LIKE ?1 OR p.code LIKE ?1 OR EXISTS (SELECT 1 FROM product_detail pd WHERE pd.product_id=p.id AND pd.active=true AND (pd.sku LIKE ?1 OR pd.legacy_sku LIKE ?1)))
""",
            nativeQuery = true)
    Page<ProductView> getProductByParam(String search, Pageable pageable);

    @Query("select p from Product p where p.slug = ?1 and p.active = true")
    Product findBySlug(String slug);

    @Query("select p.id from Product p where p.slug = ?1")
    Long findIdBySlug(String slug);

    @Query(value = "select DISTINCT p.id, p.name, p.image, p.slug, \n" +
            "       cast(p.price as double) as price,\n" +
            "       cast(p.ole_price as double) as olePrice\n" +
            "from product p\n" +
            "where p.active = true\n" +
            "  and p.price between ?1 and ?2 limit 8\n", nativeQuery = true)
    List<ProductView> samePrice(Double small, Double large);


//    @Query(value = "select DISTINCT p.id, p.name, p.image, p.slug, \n" +
//            "cast(p.price as double) as price,\n" +
//            "cast(p.ole_price as double) as olePrice\n" +
//            "from product_category pc\n" +
//            "inner join product p on p.id = pc.product_id\n" +
//            "where p.active = true and pc.category_id = ?1", nativeQuery = true)
//    Page<ProductView> findByCategory(Long categoryId, Pageable pageable);

    @Query(value = """
SELECT DISTINCT
    p.id,
    p.name,
    p.image,
    p.slug,
    CAST(p.price AS DOUBLE) AS price,
    CAST(p.ole_price AS DOUBLE) AS olePrice,

    /* số tiền giảm */
    CAST(
        CASE
            WHEN d.id IS NULL THEN 0
            WHEN d.discount_product_type = 'PERCENT'
                THEN LEAST(p.price * d.discount_value / 100, IFNULL(d.max_discount_value, p.price))
            WHEN d.discount_product_type = 'FIXED'
                THEN d.discount_value
            ELSE 0
        END
    AS DOUBLE) AS discountAmount,

    /* giá sau giảm */
    CAST(
        CASE
            WHEN d.id IS NULL THEN p.price
            WHEN d.discount_product_type = 'PERCENT'
                THEN p.price - LEAST(p.price * d.discount_value / 100, IFNULL(d.max_discount_value, p.price))
            WHEN d.discount_product_type = 'FIXED'
                THEN p.price - d.discount_value
            ELSE p.price
        END
    AS DOUBLE) AS finalPrice,

    /* % giảm */
    CAST(
        CASE
            WHEN d.id IS NULL THEN 0
            WHEN d.discount_product_type = 'PERCENT'
                THEN d.discount_value
            WHEN d.discount_product_type = 'FIXED'
                THEN ROUND((d.discount_value / p.price) * 100)
            ELSE 0
        END
    AS SIGNED) AS discountPercent

FROM product_category pc
INNER JOIN product p ON p.id = pc.product_id

LEFT JOIN discount_product d
    ON d.product_id = p.id
    AND NOW() BETWEEN d.start_date AND d.end_date

WHERE p.active = true
AND pc.category_id = ?1
""",
            countQuery = """
SELECT COUNT(DISTINCT p.id)
FROM product_category pc
INNER JOIN product p ON p.id = pc.product_id
WHERE p.active = true
AND pc.category_id = ?1
""",
            nativeQuery = true)
    Page<ProductView> findByCategory(Long categoryId, Pageable pageable);

//    @Query(value = "select DISTINCT p.id, p.name, p.image, p.slug, \n" +
//            "cast(p.price as double) as price,\n" +
//            "cast(p.ole_price as double) as olePrice\n" +
//            "from product_category pc\n" +
//            "inner join product p on p.id = pc.product_id\n" +
//            "where p.active = true and p.price between ?1 and ?2 and pc.category_id In ?3",
//
//            countQuery = "select COUNT(DISTINCT p.id)\n"+
//                    "from product_category pc\n" +
//                    "inner join product p on p.id = pc.product_id\n" +
//                    "where p.active = true and p.price between ?1 and ?2 and pc.category_id In ?3"
//            , nativeQuery = true)
//    Page<ProductView> searchFull(Double minPrice, Double maxPrice, List<Long> categoriesId, Pageable pageable);
//
//    @Query(value = "select p.id, p.name, p.image, p.slug, \n" +
//            "cast(p.price as double) as price,\n" +
//            "cast(p.ole_price as double) as olePrice\n" +
//            "from product p\n" +
//            "where p.active = true and p.price between ?1 and ?2", nativeQuery = true)
//    Page<ProductView> searchFull(Double minPrice, Double maxPrice, Pageable pageable);

    @Query(value = """
SELECT DISTINCT
    p.id,
    p.name,
    p.image,
    p.slug,
    CAST(p.price AS DOUBLE) AS price,
    CAST(p.ole_price AS DOUBLE) AS olePrice,

    CAST(
        CASE
            WHEN d.id IS NULL THEN 0
            WHEN d.discount_product_type = 'PERCENT'
                THEN LEAST(p.price * d.discount_value / 100, IFNULL(d.max_discount_value, p.price))
            WHEN d.discount_product_type = 'FIXED'
                THEN d.discount_value
            ELSE 0
        END
    AS DOUBLE) AS discountAmount,

    CAST(
        CASE
            WHEN d.id IS NULL THEN p.price
            WHEN d.discount_product_type = 'PERCENT'
                THEN p.price - LEAST(p.price * d.discount_value / 100, IFNULL(d.max_discount_value, p.price))
            WHEN d.discount_product_type = 'FIXED'
                THEN p.price - d.discount_value
            ELSE p.price
        END
    AS DOUBLE) AS finalPrice,

    CAST(
        CASE
            WHEN d.id IS NULL THEN 0
            WHEN d.discount_product_type = 'PERCENT'
                THEN d.discount_value
            WHEN d.discount_product_type = 'FIXED'
                THEN ROUND((d.discount_value / p.price) * 100)
            ELSE 0
        END
    AS SIGNED) AS discountPercent

FROM product_category pc
INNER JOIN product p ON p.id = pc.product_id

LEFT JOIN discount_product d
    ON d.product_id = p.id
    AND NOW() BETWEEN d.start_date AND d.end_date

WHERE p.active = true
AND p.price BETWEEN ?1 AND ?2
AND pc.category_id IN (?3)
""",
            countQuery = """
SELECT COUNT(DISTINCT p.id)
FROM product_category pc
INNER JOIN product p ON p.id = pc.product_id
WHERE p.active = true
AND p.price BETWEEN ?1 AND ?2
AND pc.category_id IN (?3)
""",
            nativeQuery = true)
    Page<ProductView> searchFull(Double minPrice, Double maxPrice, List<Long> categoriesId, Pageable pageable);

    @Query(value = """
SELECT
    p.id,
    p.name,
    p.image,
    p.slug,
    CAST(p.price AS DOUBLE) AS price,
    CAST(p.ole_price AS DOUBLE) AS olePrice,

    CAST(
        CASE
            WHEN d.id IS NULL THEN 0
            WHEN d.discount_product_type = 'PERCENT'
                THEN LEAST(p.price * d.discount_value / 100, IFNULL(d.max_discount_value, p.price))
            WHEN d.discount_product_type = 'FIXED'
                THEN d.discount_value
            ELSE 0
        END
    AS DOUBLE) AS discountAmount,

    CAST(
        CASE
            WHEN d.id IS NULL THEN p.price
            WHEN d.discount_product_type = 'PERCENT'
                THEN p.price - LEAST(p.price * d.discount_value / 100, IFNULL(d.max_discount_value, p.price))
            WHEN d.discount_product_type = 'FIXED'
                THEN p.price - d.discount_value
            ELSE p.price
        END
    AS DOUBLE) AS finalPrice,

    CAST(
        CASE
            WHEN d.id IS NULL THEN 0
            WHEN d.discount_product_type = 'PERCENT'
                THEN d.discount_value
            WHEN d.discount_product_type = 'FIXED'
                THEN ROUND((d.discount_value / p.price) * 100)
            ELSE 0
        END
    AS SIGNED) AS discountPercent

FROM product p

LEFT JOIN discount_product d
    ON d.product_id = p.id
    AND NOW() BETWEEN d.start_date AND d.end_date

WHERE p.active = true
AND p.price BETWEEN ?1 AND ?2
""",
            countQuery = """
SELECT COUNT(p.id)
FROM product p
WHERE p.active = true
AND p.price BETWEEN ?1 AND ?2
""",
            nativeQuery = true)
    Page<ProductView> searchFull(Double minPrice, Double maxPrice, Pageable pageable);

    @Query(value = "select p.id, p.name, p.image, p.slug, \n" +
            "cast(p.price as double) as price,\n" +
            "cast(p.ole_price as double) as olePrice\n" +
            "from product p\n" +
            "where p.active = true and p.id = ?1", nativeQuery = true)
    ProductView findViewById(Long productId);

    @Query(value = "select p.id, cast(p.weight as float) as weight from product p where p.id in ?1", nativeQuery = true)
    List<ProductWeight> sumWeightByIds(List<Long> ids);

    @Query(value = "select p.id, p.name, p.image, p.slug, \n" +
            "cast(p.price as double) as price,\n" +
            "cast(p.ole_price as double) as olePrice\n" +
            "from product p\n" +
            "where p.active = true and p.name like ?1 ", nativeQuery = true)
    Page<ProductView> findByParam(String search, Pageable pageable);

    @Query("select p.image from Product p where p.id = ?1")
    String getImageById(Long productId);

    @Query("select p.name from Product p where p.id = ?1")
    String getNameById(Long productId);

    @Query("select p.weight from Product p where p.id = ?1")
    Float getWeightById(Long productId);


    @Query("""
    select  distinct 
            p.id as id,
            p.name as name,
            p.image as image,
            p.slug as slug,
            p.price as price,
            p.olePrice as olePrice
        from Product p
        join p.productCategories pc
        where pc.category.id in :categoryIds
    """)
    List<ProductView> findByCategoryIds(@Param("categoryIds") List<Long> categoryIds);


    List<Product> findTop5ByOrderBySoldDesc();
    List<Product> findTop10ByOrderBySoldDesc();
    List<Product> findTop10ByPriceLessThanEqualOrderByPriceDesc(Double price);
    List<Product> findByIdIn(List<Long> ids);
}
