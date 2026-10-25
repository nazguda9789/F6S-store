package com.example.sneaker.repository;

import com.example.sneaker.model.Customer;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository
    extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Customer c where c.id=:id")
  Optional<Customer> lock(@Param("id") Long id);
}
