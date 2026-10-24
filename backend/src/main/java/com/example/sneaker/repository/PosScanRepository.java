package com.example.sneaker.repository;

import com.example.sneaker.model.PosScanEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PosScanRepository extends JpaRepository<PosScanEvent, String> {}
