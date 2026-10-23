package com.web.repository;

import com.web.entity.ShoeAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ShoeAttributeRepository extends JpaRepository<ShoeAttribute, Long> {
    List<ShoeAttribute> findByKindOrderByNameAsc(ShoeAttribute.Kind kind);
    boolean existsByKindAndNameIgnoreCaseAndIdNot(ShoeAttribute.Kind kind, String name, Long id);
}
