package com.web.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "shopee_key")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class ShopeeKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String shopName;

    private String keyToken;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime localDateTime;

    @UpdateTimestamp
    private LocalDateTime updatedDate;
}
