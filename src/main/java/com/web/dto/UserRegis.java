package com.web.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegis {

    private String fullName;

    private String phone;

    private String email;

    private String password;

    private String rePassword;
}
