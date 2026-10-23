package com.web.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "product", uniqueConstraints = @UniqueConstraint(name = "uk_f6s_product_code", columnNames = "code"))
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    @Column(length = 32)
    private String code;
    private Long brandId;
    @Transient
    private String brandName;
    private Long soleId;
    private Long materialId;

    private String slug;              // slug SEO (unique)

    private String image;

    private Double price;

    private Double olePrice;

    private Float weight;

    @Column(columnDefinition = "LONGTEXT")
    private String description;

    private String shortDescription;

    private String metaTitle;

    @Column(length = 2000)
    private String metaDescription;

    private Boolean active = true;

    @CreatedDate
    @Column(updatable = false)
    @JsonFormat(pattern = "HH:mm dd/MM/yy")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @JsonFormat(pattern = "HH:mm dd/MM/yy")
    private LocalDateTime updatedAt;

    @Transient
    private Long stock;

    private Long sold = 0L;

    @OneToMany(mappedBy = "product")
    private List<ProductCategory> productCategories;

    @Transient
    private List<Long> categoryIds;

    @Transient
    public List<Long> getCategoryIds() {
        if (productCategories == null) return List.of();
        return productCategories.stream()
                .map(pc -> pc.getCategory().getId())
                .toList();
    }
}
