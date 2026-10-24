package com.example.sneaker.repository;

import com.example.sneaker.model.Promotion;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
  Optional<Promotion> findByCode(String code);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Promotion p where p.code=:code")
  Optional<Promotion> lockCode(@Param("code") String code);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Promotion p where p.id=:id")
  Optional<Promotion> lockId(@Param("id") Long id);
}
