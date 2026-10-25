package com.example.sneaker.repository;

import com.example.sneaker.model.ShoeAttribute;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShoeAttributeRepository extends JpaRepository<ShoeAttribute, Long> {
  List<ShoeAttribute> findByTypeOrderByNameAsc(String type);
}
