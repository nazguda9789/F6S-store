package com.web.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "province")
@Getter
@Setter
public class Province {

    @Id
    private String code;

    private String name;

    private String administrativeLevel;

}
