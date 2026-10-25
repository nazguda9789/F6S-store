package com.example.sneaker.repository;

import com.example.sneaker.model.StaffAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface StaffRepository
    extends JpaRepository<StaffAccount, Long>, JpaSpecificationExecutor<StaffAccount> {
  Optional<StaffAccount> findByUsername(String username);
}
