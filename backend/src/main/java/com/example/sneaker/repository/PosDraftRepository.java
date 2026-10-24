package com.example.sneaker.repository;

import com.example.sneaker.model.PosDraft;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PosDraftRepository extends JpaRepository<PosDraft, Long> {
  List<PosDraft> findByOwnerAndStatusOrderByIdAsc(String owner, String status);

  long countByOwnerAndStatus(String owner, String status);

  Optional<PosDraft> findByRequestKey(String key);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from PosDraft d where d.id=:id")
  Optional<PosDraft> lock(@Param("id") Long id);
}
