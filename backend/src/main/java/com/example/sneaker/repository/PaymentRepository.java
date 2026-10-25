package com.example.sneaker.repository;

import com.example.sneaker.model.PaymentAttempt;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface PaymentRepository extends JpaRepository<PaymentAttempt, String> {
  Optional<PaymentAttempt> findByOrderId(Long id);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from PaymentAttempt p where p.id=:id")
  Optional<PaymentAttempt> lockId(@org.springframework.data.repository.query.Param("id") String id);
}
