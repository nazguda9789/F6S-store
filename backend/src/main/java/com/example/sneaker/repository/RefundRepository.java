package com.example.sneaker.repository;

import com.example.sneaker.model.OrderRefund;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<OrderRefund, Long> {
  Optional<OrderRefund> findByOrderId(Long id);
}
