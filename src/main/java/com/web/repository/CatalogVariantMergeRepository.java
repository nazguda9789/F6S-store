package com.web.repository;

import com.web.entity.CatalogVariantMerge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogVariantMergeRepository extends JpaRepository<CatalogVariantMerge,String> {
    boolean existsBySourceVariantId(Long sourceVariantId);
}
