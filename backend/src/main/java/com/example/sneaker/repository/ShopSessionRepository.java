package com.example.sneaker.repository;

import com.example.sneaker.model.ShopSession;
import java.time.Instant;
import org.springframework.data.jpa.repository.*;

public interface ShopSessionRepository extends JpaRepository<ShopSession, String> {
  void deleteByAccountId(String id);

  @Modifying
  @Query("delete from ShopSession s where s.expiresAt<:now")
  void deleteExpired(@org.springframework.data.repository.query.Param("now") Instant now);
}
