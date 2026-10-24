package com.web.dto;

import lombok.Getter;
import lombok.Setter;

import java.sql.Date;

@Getter
@Setter
public class BlogSearchAdmin {

    private Date from;
    private Date to;
    private String search;
    private Boolean archived = false;
}
