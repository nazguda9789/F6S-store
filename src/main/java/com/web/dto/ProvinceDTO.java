package com.web.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ProvinceDTO {

    private String code;
    private String name;
    private String administrativeLevel;
    private List<WardDTO> wards = new ArrayList<>();

    @Data
    public static class WardDTO  {
        private String code;
        private String name;
        private String administrativeLevel;
        private Double shipCost = 30000D;
    }
}
