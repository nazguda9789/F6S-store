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
        return sizes.stream().sorted(java.util.Comparator.comparing((Size s)->s.getName()==null || !s.getName().matches("[0-9]+(\\.[0-9]+)?"))
                .thenComparingDouble(s->s.getName()!=null && s.getName().matches("[0-9]+(\\.[0-9]+)?") ? Double.parseDouble(s.getName()) : Double.MAX_VALUE)
                .thenComparing(s->s.getName()==null ? "" : s.getName())).toList();
    }

    public Size saveOrUpdate(Size size){
        String name = size.getName() == null ? "" : size.getName().strip();
        if(name.isEmpty() || name.length()>20) throw new MessageException("Kích cỡ phải từ 1 đến 20 ký tự, ví dụ 38, 39 hoặc 40.5.");
        size.setName(name);
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
