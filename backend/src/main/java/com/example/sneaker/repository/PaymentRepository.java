package com.example.sneaker.repository;

import com.example.sneaker.model.PaymentAttempt;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface PaymentRepository extends JpaRepository<PaymentAttempt, String> {
  Optional<PaymentAttempt> findByOrderId(Long id);

  @Query(
      "select p from PaymentAttempt p where p.expiresAt<:cutoff and p.order.status in"
          + " ('PENDING','CONFIRMED','PACKING') and p.order.paymentStatus in ('UNPAID','FAILED')")
  org.springframework.data.domain.Page<PaymentAttempt> expired(
      @org.springframework.data.repository.query.Param("cutoff") java.time.Instant cutoff,
      org.springframework.data.domain.Pageable pageable);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from PaymentAttempt p where p.id=:id")
  Optional<PaymentAttempt> lockId(@org.springframework.data.repository.query.Param("id") String id);
}
