package com.web.service;

import com.web.entity.Category;
import com.web.entity.Color;
import com.web.exception.MessageException;
import com.web.repository.CategoryRepository;
import com.web.repository.ColorRepository;
import com.web.repository.ProductDetailRepository;
import com.web.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ColorService {

    @Autowired
    private ColorRepository colorRepository;

    @Autowired
    private ProductDetailRepository productDetailRepository;

    public List<Color> findAllList(){
        List<Color> colors = colorRepository.findAll();
        return colors;
    }

    public Color saveOrUpdate(Color color){
        String name = color.getName() == null ? "" : color.getName().strip().replaceAll("\\s+"," ");
        if(name.isEmpty() || name.length()>100) throw new MessageException("Tên màu phải từ 1 đến 100 ký tự.");
        if(color.getCode()==null || !color.getCode().matches("#[0-9a-fA-F]{6}")) throw new MessageException("Mã màu phải đúng định dạng #RRGGBB, ví dụ #111827.");
        color.setName(name); color.setCode(color.getCode().toUpperCase(java.util.Locale.ROOT));
        if(color.getId() == null){
            if(colorRepository.findByName(color.getName()) != null){
                throw new MessageException("Tên màu sắc đã tồn tại");
            }
        }
        else{
            if(colorRepository.findByName(color.getName(), color.getId()) != null){
                throw new MessageException("Tên màu sắc đã tồn tại");
            }
        }
        color.setDeleted(false);
        colorRepository.save(color);
        return color;
    }

    public Color findById(Long id){
        Optional<Color> color = colorRepository.findById(id);
        if(color.isEmpty()){
            throw new MessageException("Không tìm thấy màu sắc");
        }
        return color.get();
    }

    public void delete(Long id){
        Long idDetail = productDetailRepository.findByColor(id);
        if(idDetail == null){
            colorRepository.deleteById(id);
        }
        else{
            Color color = colorRepository.findById(id).get();
            color.setDeleted(true);
            colorRepository.save(color);
        }
    }
}
