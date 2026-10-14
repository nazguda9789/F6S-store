package com.web.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.web.enums.BannerType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "banner")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(updatable = false)
    @JsonFormat(pattern = "HH:mm dd/MM/yy")
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @JsonFormat(pattern = "HH:mm dd/MM/yy")
    private LocalDateTime updateDate;

    private String title;           // tiêu đề banner
    private String imageUrl;        // đường dẫn ảnh
    private String linkUrl;         // link khi click
    private Integer displayOrder;   // thứ tự hiển thị
    private Boolean active = true;  // trạng thái hiển thị

    @Enumerated(EnumType.STRING)
    private BannerType bannerType;

    private String pageName;        // tên trang hiển thị banner (home, product, blog...)
}

