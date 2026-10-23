package com.web.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import java.time.LocalDateTime;

/** Read-only audit record written by the reviewed inventory migration. */
@Entity @Table(name="catalog_variant_merge") @Immutable @Getter
public class CatalogVariantMerge {
    @Id @Column(length=64) private String migrationKey;
    @Column(nullable=false) private Long sourceVariantId;
    @Column(nullable=false) private Long targetVariantId;
    @Column(nullable=false) private Integer sourceStockBefore;
    @Column(nullable=false) private Integer targetStockBefore;
    @Column(nullable=false) private LocalDateTime createdAt;
}
