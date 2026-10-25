package com.example.sneaker.repository;

import com.example.sneaker.model.EmailChallenge;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface EmailChallengeRepository extends JpaRepository<EmailChallenge, String> {
  long countByEmailAndCreatedAtAfter(String email, Instant after);

  long countByCreatedAtAfter(Instant after);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from EmailChallenge c where c.id=:id")
  Optional<EmailChallenge> lockId(@org.springframework.data.repository.query.Param("id") String id);
}
