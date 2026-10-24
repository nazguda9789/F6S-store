package com.example.sneaker.repository;

import com.example.sneaker.model.CustomerOrder;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
  Optional<CustomerOrder> findByRequestKey(String key);
}
