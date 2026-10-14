package com.web.repository;

import com.web.dto.ActiveDiscountResponse;
import com.web.entity.DiscountProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface DiscountProductRepository extends JpaRepository<DiscountProduct, Long>, JpaSpecificationExecutor<DiscountProduct> {

    @Query("""
    select count(d) > 0 
    from DiscountProduct d 
    where d.product.id = :productId
    and (
        (:startDate between d.startDate and d.endDate)
        or (:endDate between d.startDate and d.endDate)
        or (d.startDate between :startDate and :endDate)
    )
""")
    boolean existsDiscountInTime(
            Long productId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    @Query("""
    select d from DiscountProduct d
    where d.product.id = :productId
    and :now between d.startDate and d.endDate
    """)
    DiscountProduct findActiveDiscount(Long productId, LocalDateTime now);

    @Query("""
select count(d) > 0 
from DiscountProduct d
where d.product.id = :productId
and d.id <> :id
and (
    :startDate between d.startDate and d.endDate
    or :endDate between d.startDate and d.endDate
    or d.startDate between :startDate and :endDate
)
""")
    boolean existsDiscountInTimeUpdate(
            Long id,
            Long productId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );


    @Query("""
select new com.web.dto.ActiveDiscountResponse(
    d.id,
    d.discountProductType,
    d.discountValue,
    d.maxDiscountValue
)
from DiscountProduct d
where d.product.id = :productId
and :now between d.startDate and d.endDate
""")
    Optional<ActiveDiscountResponse> findActiveDiscountDto(
            Long productId,
            LocalDateTime now
    );

    @Modifying
    @Transactional
    @Query("delete from DiscountProduct c where c.product.id = ?1")
    int deleteByProductId(Long id);
}
