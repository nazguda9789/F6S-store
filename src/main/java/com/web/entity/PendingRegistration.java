package com.web.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/** A verification request, not a usable account. Never store a raw password. */
@Entity
@Table(name = "pending_registration")
@Getter
@Setter
public class PendingRegistration {
    @Id private String email;
    @Column(nullable = false) private String passwordHash;
    private String fullName;
    private String phone;
    @Column(nullable = false) private LocalDateTime createdDate;
    @Column(nullable = false) private LocalDateTime expiresAt;
}
