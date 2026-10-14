package com.web.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProvinceResponse {
    private String requestId;
    private List<ProvinceDTO> provinces;

    @Getter
    @Setter
    public static class ProvinceDTO {
        private String code;
        private String name;
        private String administrativeLevel;
        private String decree;
    }
}
