package com.web.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.web.enums.ChattingType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "chatting")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Chatting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    private ChattingType type; // GUI hoặc NHAN

    @CreatedDate
    @Column(updatable = false)
    @JsonFormat(pattern = "HH:mm dd/MM/yyyy")
    private LocalDateTime createdDate;

    @ManyToOne
    private User user; // người dùng

}
