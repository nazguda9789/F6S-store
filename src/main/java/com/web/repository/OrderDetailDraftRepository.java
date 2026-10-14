package com.web.repository;

import com.web.entity.OrderDetailDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface OrderDetailDraftRepository extends JpaRepository<OrderDetailDraft, Long> {

    @Query("select o from OrderDetailDraft o where o.idProductDetail = ?1 and o.orderDraft.id = ?2")
    Optional<OrderDetailDraft> findByDetailAndOrderId(Long detailId, Long orderId);

    @Query("select o from OrderDetailDraft o where o.orderDraft.id = ?1")
    List<OrderDetailDraft> findByOrder(Long orderId);

    @Query("select cast(sum(o.quantity * o.price) as double )  from OrderDetailDraft o where o.orderDraft.id = ?1")
    Double totalByOrderId(Long orderId);

    @Modifying
    @Transactional
    @Query("delete from OrderDetailDraft p where p.orderDraft.id = ?1")
    int deleteByOrder(Long orderId);
}
