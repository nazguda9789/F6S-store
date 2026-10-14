package com.web.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "wards")
@Getter
@Setter
public class Wards {

    @Id
    private String code;

    private String name;

    private String administrativeLevel;

    private String provinceCode;

    private String provinceName;
}
