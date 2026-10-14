package com.web.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.web.enums.Role;
import com.web.enums.UserType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private Boolean actived;

    private Boolean deleted = false;

    private String rememberKey;

    private LocalDateTime createdDate;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String phone;

    private String fullName;

    private String address;

    private String avatar;

    private Boolean isGoogleUser = false;

    @Enumerated(EnumType.STRING)
    private UserType userType;

    private Boolean activationStatus = false;

    private int failedAttempts = 0;

    private boolean accountNonLocked = true;

    private LocalDateTime lockTime;
}
