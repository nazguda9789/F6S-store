package com.example.sneaker.repository;

import com.example.sneaker.model.CustomerAddress;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {
  List<CustomerAddress> findByCustomerIdAndActiveTrueOrderByDefaultAddressDescIdAsc(
      Long customerId);
}
