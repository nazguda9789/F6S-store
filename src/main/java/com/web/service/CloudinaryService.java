package com.web.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.web.entity.CloudKey;
import com.web.repository.CloudKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class CloudinaryService {

    @Autowired
    private CloudKeyRepository cloudKeyRepository;

    private Cloudinary getCloudNow(){
        CloudKey cloudKey = cloudKeyRepository.findCloudPrimary();
        Cloudinary cloudinary = null;
        Map config = new HashMap();
        config.put("cloud_name", cloudKey.getCloudName());
        config.put("api_key", cloudKey.getApiKey());
        config.put("api_secret", cloudKey.getApiSecret());
        cloudinary = new Cloudinary(config);
        return cloudinary;
    }

    public String uploadFile(MultipartFile file) {
        try {
            File uploadedFile = convertMultiPartToFile(file);
            Map uploadResult = getCloudNow().uploader().upload(uploadedFile, ObjectUtils.emptyMap());
            uploadedFile.delete();
            return uploadResult.get("url").toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public File convertMultiPartToFile(MultipartFile file) throws IOException {
        File convFile = new File(file.getOriginalFilename());
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());
        fos.close();
        return convFile;
    }

    public List<String> uploadMultiFile(List<MultipartFile> file){
        List<String> list = new ArrayList<>();
        ExecutorService es = Executors.newCachedThreadPool();
        for(int i=0; i<file.size(); i++) {
            Integer x=i;
            es.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        String image = uploadFile(file.get(x));
                        list.add(image);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        es.shutdown();
        try {
            boolean finished = es.awaitTermination(100000, TimeUnit.MINUTES);
            if (finished) {
                return list;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return list;
    }
}
