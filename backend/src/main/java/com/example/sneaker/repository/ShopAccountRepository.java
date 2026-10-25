package com.example.sneaker.repository;

import com.example.sneaker.model.ShopAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopAccountRepository
    extends JpaRepository<ShopAccount, String>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<ShopAccount> {
  Optional<ShopAccount> findByEmail(String email);
}
