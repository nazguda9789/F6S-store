package com.web.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "shoe_attribute", uniqueConstraints = {@UniqueConstraint(columnNames = {"kind", "name"}), @UniqueConstraint(name="uk_shoe_attribute_code",columnNames="code")})
@Getter @Setter
public class ShoeAttribute {
    public enum Kind { BRAND, SOLE, MATERIAL }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16)
    private Kind kind;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(length=32) private String code;
    @Column(length=500) private String description;
    @org.hibernate.annotations.CreationTimestamp @Column(nullable=false,updatable=false)
    private java.time.LocalDateTime createdAt;
    @org.hibernate.annotations.UpdateTimestamp @Column(nullable=false)
    private java.time.LocalDateTime updatedAt;
    @Column(nullable = false)
    private boolean active = true;
}
