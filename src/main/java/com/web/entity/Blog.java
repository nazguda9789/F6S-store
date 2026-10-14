package com.web.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "blog")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @CreatedDate
    @Column(updatable = false)
    @JsonFormat(pattern = "HH:mm dd/MM/yy")
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @JsonFormat(pattern = "HH:mm dd/MM/yy")
    private LocalDateTime updateDate;

    private String title;

    @Column(length = 1000)
    private String description;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    private String imageBanner;

    private Integer numView = 0;

    private String slug;

    @Column(length = 1000)
    private String metaDescription;

    private String seoTitle;

    @ManyToOne
    private User user;

}

