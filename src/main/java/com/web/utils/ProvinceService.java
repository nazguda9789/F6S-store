package com.web.utils;

import com.web.dto.WardResponse;
import com.web.entity.Wards;
import com.web.repository.WardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.web.dto.ProvinceResponse;
import com.web.entity.Province;
import com.web.repository.ProvinceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProvinceService {

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private WardRepository wardRepository;

    public void syncProvincesFromApi() {
        String url = "https://production.cas.so/address-kit/2025-07-01/provinces";
        RestTemplate restTemplate = new RestTemplate();

        ProvinceResponse response = restTemplate.getForObject(url, ProvinceResponse.class);

        if (response != null && response.getProvinces() != null) {
            List<Province> provinces = response.getProvinces().stream().map(dto -> {
                Province p = new Province();
                p.setCode(dto.getCode());
                p.setName(dto.getName());
                p.setAdministrativeLevel(dto.getAdministrativeLevel());
                return p;
            }).collect(Collectors.toList());

            provinceRepository.saveAll(provinces); // lưu Province trước

            List<Wards> allWards = new ArrayList<>();

            for (Province p : provinces) {
                String urlWard = "https://production.cas.so/address-kit/2025-07-01/provinces/" + p.getCode() + "/communes";
                WardResponse wardResponse = restTemplate.getForObject(urlWard, WardResponse.class);

                if (wardResponse != null && wardResponse.getCommunes() != null) {
                    List<Wards> wards = wardResponse.getCommunes().stream().map(dto -> {
                        Wards w = new Wards();
                        w.setCode(dto.getCode());
                        w.setProvinceCode(dto.getProvinceCode());
                        w.setProvinceName(dto.getProvinceName());
                        w.setName(dto.getName());
                        w.setAdministrativeLevel(dto.getAdministrativeLevel());
                        return w;
                    }).collect(Collectors.toList());

                    allWards.addAll(wards);
                }
            }

            wardRepository.saveAll(allWards);
        }

    }
}
