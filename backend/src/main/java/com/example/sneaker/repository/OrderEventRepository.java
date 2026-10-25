package com.example.sneaker.repository;

import com.example.sneaker.model.OrderEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {
  List<OrderEvent> findByOrderIdOrderByIdAsc(Long id);
}
