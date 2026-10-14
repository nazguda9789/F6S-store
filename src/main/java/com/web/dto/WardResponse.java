package com.web.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class WardResponse {
    private String requestId;

    private List<Commune> communes;

    @Getter
    @Setter
    public static class Commune {
        private String code;
        private String name;
        private String administrativeLevel;
        private String provinceCode;
        private String provinceName;
    }
}
