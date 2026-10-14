package com.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class Result <T>{

    private boolean success;

    private T data;

    private String message;

    public static <T> Result<T> ok(T data, String message) {
        return new Result<>(true, data, message);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(false, null, message);
    }
}
