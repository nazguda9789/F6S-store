package com.web.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "cloud_key")
@Getter
@Setter
public class CloudKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cloudName;

    private String apiKey;

    private String apiSecret;

    private LocalDateTime createdDate;

    private String gmail;

    private Boolean isPrimary = false;
}
