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
    public Category saveOrUpdate(Category category){
        if (category.getIsParent() != null && category.getIsParent()) {
            category.setCategory(null); // Là cha, không có cha khác
        } else {
            if (category.getParentId() != null) {
                Category parent = categoryRepository.findById(category.getParentId())
                        .orElseThrow(() -> new MessageException("Danh mục cha không tồn tại"));
                category.setCategory(parent);
            } else {
                category.setCategory(null);
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
    public void delete(Long id){
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
