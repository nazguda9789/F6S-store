package com.web.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "shoe_attribute", uniqueConstraints = @UniqueConstraint(columnNames = {"kind", "name"}))
@Getter @Setter
public class ShoeAttribute {
    public enum Kind { BRAND, SOLE, MATERIAL }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16)
    private Kind kind;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false)
    private boolean active = true;
}
