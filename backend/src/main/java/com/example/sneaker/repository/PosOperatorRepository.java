package com.example.sneaker.repository;

import com.example.sneaker.model.PosOperator;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PosOperatorRepository extends JpaRepository<PosOperator, String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from PosOperator o where o.username=:username")
  Optional<PosOperator> lock(@Param("username") String username);
}
