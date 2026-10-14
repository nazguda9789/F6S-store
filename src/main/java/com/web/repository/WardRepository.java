package com.web.repository;

import com.web.entity.Wards;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WardRepository extends JpaRepository<Wards, String> {
}
