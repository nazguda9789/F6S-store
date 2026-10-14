package com.web.service;

import com.web.entity.CloudKey;
import com.web.exception.MessageException;
import com.web.repository.CloudKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CloudKeyService {

    @Autowired
    private CloudKeyRepository cloudKeyRepository;

    public List<CloudKey> findAllList(){
        List<CloudKey> list = cloudKeyRepository.findAll();
        return list;
    }

    public CloudKey saveOrUpdate(CloudKey cloudKey){
        if(cloudKey.getId() == null){
            cloudKey.setCreatedDate(LocalDateTime.now());
        }
        else{
            CloudKey c = cloudKeyRepository.findById(cloudKey.getId()).get();
            cloudKey.setCreatedDate(c.getCreatedDate());
        }
        if(cloudKey.getIsPrimary() == true){
            cloudKeyRepository.setFalse();
        }
        cloudKeyRepository.save(cloudKey);
        return cloudKey;
    }

    public void delete(Long id){
        CloudKey cloudKey = cloudKeyRepository.findById(id).get();
        if(cloudKey.getIsPrimary() == true){
            throw new MessageException("Không được xóa key chính");
        }
        cloudKeyRepository.deleteById(id);
    }

    public CloudKey findById(Long id) {
        return cloudKeyRepository.findById(id).get();
    }
}
