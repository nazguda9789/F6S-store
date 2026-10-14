package com.web.service;

import com.web.repository.ProductColorImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductColorImageService {

    @Autowired
    private ProductColorImageRepository productColorImageRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    public String changeImage(Long id, MultipartFile file){
        String image = cloudinaryService.uploadFile(file);
        productColorImageRepository.changeImage(image, id);
        return image;
    }

    public void changeImageByUrl(Long id, String image){
        productColorImageRepository.changeImage(image, id);
    }
}
