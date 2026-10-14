package com.web.service;

import com.web.entity.Color;
import com.web.entity.Size;
import com.web.exception.MessageException;
import com.web.repository.ColorRepository;
import com.web.repository.ProductDetailRepository;
import com.web.repository.SizeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SizeService {

    @Autowired
    private SizeRepository sizeRepository;

    @Autowired
    private ProductDetailRepository productDetailRepository;

    public List<Size> findAllList(){
        List<Size> sizes = sizeRepository.findAll();
        return sizes;
    }

    public Size saveOrUpdate(Size size){
        if(size.getId() == null){
            if(sizeRepository.findByName(size.getName()) != null){
                throw new MessageException("Tên size đã tồn tại");
            }
        }
        else{
            if(sizeRepository.findByName(size.getName(), size.getId()) != null){
                throw new MessageException("Tên size đã tồn tại");
            }
        }
        size.setDeleted(false);
        sizeRepository.save(size);
        return size;
    }

    public Size findById(Long id){
        Optional<Size> color = sizeRepository.findById(id);
        if(color.isEmpty()){
            throw new MessageException("Không tìm thấy size");
        }
        return color.get();
    }

    public void delete(Long id){
        Long idDetail = productDetailRepository.findBySize(id);
        if(idDetail == null){
            sizeRepository.deleteById(id);
        }
        else{
            Size size = sizeRepository.findById(id).get();
            size.setDeleted(true);
            sizeRepository.save(size);
        }
    }
}
