package com.web.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "category")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String slug;

    private Boolean isParent = false;

    private Integer position;

    private String metaDescription;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updateDate;

    private Boolean deleted = false;

    private String image;

    @ManyToOne
    @JoinColumn(name = "category_parent")
    @JsonBackReference
    private Category category;

    @Transient
    private Long parentId;

    @OneToMany(mappedBy = "category", cascade = CascadeType.REMOVE)
    @JsonManagedReference
    private List<Category> children = new ArrayList<>();

}
