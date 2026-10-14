package com.web.repository;

import com.web.entity.CloudKey;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CloudKeyRepository extends JpaRepository<CloudKey, Long> {

    @Modifying
    @Transactional
    @Query("update CloudKey c set c.isPrimary = false")
    int setFalse ();

    @Query("select c from CloudKey c where c.isPrimary = true")
    CloudKey findCloudPrimary();
}
