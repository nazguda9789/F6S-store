package com.example.sneaker.repository;

import com.example.sneaker.model.CustomerOrder;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
  Optional<CustomerOrder> findByRequestKey(String key);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from CustomerOrder o where o.id=:id")
  Optional<CustomerOrder> lockId(@Param("id") Long id);

  org.springframework.data.domain.Page<CustomerOrder> findAllByOrderByCreatedAtDesc(
      Pageable pageable);

  interface Revenue {
    BigDecimal getRevenue();

    Long getCompletedOrders();
  }

  @Query(
      "select coalesce(sum(o.total),0) as revenue,count(o) as completedOrders from CustomerOrder o"
          + " where o.status='COMPLETED' and o.completedAt>=:start and o.completedAt<:end")
  Revenue revenue(@Param("start") Instant start, @Param("end") Instant end);

  @Query("select count(o) from CustomerOrder o where o.createdAt>=:start and o.createdAt<:end")
  long total(@Param("start") Instant start, @Param("end") Instant end);

  @Query(
      "select count(o) from CustomerOrder o where o.status='COMPLETED' and o.createdAt>=:start and"
          + " o.createdAt<:end")
  long successful(@Param("start") Instant start, @Param("end") Instant end);

  interface Top {
    Long getProductId();

    String getCode();

    String getName();

    Long getQuantity();

    BigDecimal getGrossRevenue();
  }

  @Query(
      "select i.variant.product.id as productId,i.variant.product.code as"
          + " code,i.variant.product.name as name,sum(i.quantity) as"
          + " quantity,sum(i.unitPrice*i.quantity) as grossRevenue from OrderItem i where"
          + " i.order.status='COMPLETED' and i.order.completedAt>=:start and"
          + " i.order.completedAt<:end group by"
          + " i.variant.product.id,i.variant.product.code,i.variant.product.name order by"
          + " sum(i.quantity) desc,i.variant.product.id asc")
  List<Top> top(@Param("start") Instant start, @Param("end") Instant end, Pageable pageable);
}
