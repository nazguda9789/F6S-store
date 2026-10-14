package com.web.repository;

import com.web.entity.OrderDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface OrderDraftRepository extends JpaRepository<OrderDraft, Long> {

    @Modifying
    @Transactional
    @Query("update OrderDraft o set o.totalAmount = ?1 where o.id = ?2")
    int updateTotalAmount(Double total, Long orderId);

    @Query("select o.totalAmount from OrderDraft o where o.id = ?1")
    Double getTotalAmount(Long orderId);
}
