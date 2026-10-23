package com.web.service;

import com.web.entity.Category;
import com.web.exception.MessageException;
import com.web.repository.CategoryRepository;
import com.web.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    public List<Category> findAllList(){
        List<Category> categories = categoryRepository.findAll();
        return categories;
    }

    public List<Category> listParent(){
        List<Category> categories = categoryRepository.findParentCategory();
        return categories;
    }

    @Cacheable("categories")
    public List<Category> getParentCategoriesUser() {
        return categoryRepository.findParentCategoryUser();
    }

    @CacheEvict(value = "categories", allEntries = true)
    @org.springframework.transaction.annotation.Transactional
    public Category saveOrUpdate(Category category){
        String name=category.getName()==null ? "" : category.getName().strip().replaceAll("\\s+"," ");
        if(name.isEmpty() || name.length()>255) throw new MessageException("Tên danh mục phải từ 1 đến 255 ký tự.");
        if(category.getSlug()==null || !category.getSlug().matches("[a-z0-9]+(?:-[a-z0-9]+)*")) throw new MessageException("Đường dẫn danh mục chỉ dùng chữ thường không dấu, số và dấu gạch ngang.");
        if(category.getPosition()!=null && category.getPosition()<0) throw new MessageException("Vị trí hiển thị không được âm.");
        category.setName(name);
        if (category.getId()!=null) {
            Category previous=categoryRepository.findById(category.getId()).orElseThrow(()->new MessageException("Danh mục không tồn tại."));
            category.setCreatedDate(previous.getCreatedDate());
            if(category.getImage()==null) category.setImage(previous.getImage());
            if(!Boolean.TRUE.equals(category.getIsParent()) && previous.getChildren()!=null && !previous.getChildren().isEmpty())
                throw new MessageException("Chuyển các mục con trước khi đổi nhóm gốc thành mục con.");
        }
        if (category.getIsParent() != null && category.getIsParent()) {
            category.setCategory(null); // Là cha, không có cha khác
        } else {
            if (category.getParentId() != null) {
                Category parent = categoryRepository.findById(category.getParentId())
                        .orElseThrow(() -> new MessageException("Danh mục cha không tồn tại"));
                if (category.getParentId().equals(category.getId()) || !Boolean.TRUE.equals(parent.getIsParent()) || Boolean.TRUE.equals(parent.getDeleted()))
                    throw new MessageException("Danh mục cha phải là nhóm gốc đang dùng và không được là chính danh mục này.");
                category.setCategory(parent);
            } else {
                throw new MessageException("Chọn danh mục cha hoặc đánh dấu đây là danh mục gốc.");
            }
        }
        if(category.getId() == null){
            category.setCreatedDate(LocalDateTime.now());
            if(categoryRepository.findByName(category.getName()).isPresent()){
                throw new MessageException("Tên danh mục đã tồn tại");
            }
            if(categoryRepository.findBySlug(category.getSlug()).isPresent()){
                throw new MessageException("Slug đã tồn tại");
            }
        }
        else{
            if(categoryRepository.findByName(category.getName(), category.getId()).isPresent()){
                throw new MessageException("Tên danh mục đã tồn tại");
            }
            if(categoryRepository.findBySlug(category.getSlug(), category.getId()).isPresent()){
                throw new MessageException("Slug đã tồn tại");
            }
        }
        category.setDeleted(false);
        categoryRepository.save(category);
        return category;
    }

    public Category findById(Long id){
        Optional<Category> category = categoryRepository.findById(id);
        if(category.isEmpty()){
            throw new MessageException("Không tìm thấy danh mục");
        }
        return category.get();
    }

    @CacheEvict(value = "categories", allEntries = true)
    @org.springframework.transaction.annotation.Transactional
    public void delete(Long id){
        Category selected=categoryRepository.findById(id).orElseThrow(()->new MessageException("Không tìm thấy danh mục."));
        if(selected.getChildren()!=null && !selected.getChildren().isEmpty()) throw new MessageException("Danh mục còn mục con. Chuyển hoặc xử lý các mục con trước để tránh xóa dây chuyền.");
        Long idProduct = productRepository.checkProductExistByCategory(id);
        if(idProduct == null){
            categoryRepository.deleteById(id);
        }
        else{
            Category category = categoryRepository.findById(id).get();
            category.setDeleted(true);
            categoryRepository.save(category);
        }
    }

    public Category findBySlug(String slug) {
        return categoryRepository.findBySlug(slug).orElse(null);
    }
}
