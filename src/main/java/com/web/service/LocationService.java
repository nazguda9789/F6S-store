package com.web.service;
import com.web.dto.ProvinceDTO;
import com.web.entity.Province;
import com.web.entity.Wards;
import com.web.repository.ProvinceRepository;
import com.web.repository.WardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LocationService {

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private WardRepository wardRepository;

    @Cacheable("provincesWithWards")
    public List<ProvinceDTO> getAllProvincesWithWards() {
        List<Province> provinces = provinceRepository.findAll();
        List<Wards> wards = wardRepository.findAll();

        Map<String, List<Wards>> wardsByProvince = wards.stream()
                .collect(Collectors.groupingBy(Wards::getProvinceCode));

        return provinces.stream().map(p -> {
            ProvinceDTO dto = new ProvinceDTO();
            dto.setCode(p.getCode());
            dto.setName(p.getName());
            dto.setAdministrativeLevel(p.getAdministrativeLevel());

            List<ProvinceDTO.WardDTO> wardDTOs = wardsByProvince
                    .getOrDefault(p.getCode(), Collections.emptyList())
                    .stream()
                    .map(w -> {
                        ProvinceDTO.WardDTO wd = new ProvinceDTO.WardDTO();
                        wd.setCode(w.getCode());
                        wd.setName(w.getName());
                        wd.setAdministrativeLevel(w.getAdministrativeLevel());
                        return wd;
                    })
                    .collect(Collectors.toList());

            dto.setWards(wardDTOs);
            return dto;
        }).toList();
    }
}
