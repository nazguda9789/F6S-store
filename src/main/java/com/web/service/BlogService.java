package com.web.service;

import com.web.dto.BlogSearchAdmin;
import com.web.dto.BlogSpecification;
import com.web.dto.ProductSpecification;
import com.web.entity.Blog;
import com.web.entity.Category;
import com.web.entity.Product;
import com.web.exception.MessageException;
import com.web.repository.BlogRepository;
import com.web.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BlogService {

    @Autowired
    private BlogRepository blogRepository;

    public Page<Blog> findAllList(Pageable pageable){
        Page<Blog> list = blogRepository.findByArchivedFalse(pageable);
        return list;
    }

    public Blog lastBlog(){
        Blog blog = blogRepository.lastBlog();
        return blog;
    }

    public List<Blog> listBlogIndex(){
        List<Blog> list = blogRepository.listBlogIndex();
        return list;
    }


    public Blog saveOrUpdate(Blog blog){
        blogRepository.save(blog);
        return blog;
    }

    public Blog findById(Long id){
        Optional<Blog> category = blogRepository.findById(id);
        if(category.isEmpty()){
            throw new MessageException("Không tìm thấy bài viết");
        }
        return category.get();
    }

    public void delete(Long id){
        blogRepository.deleteById(id);
    }

    public Long totalBlog(){
        return blogRepository.count();
    }

    public Page<Blog> allBlogByAdmin(BlogSearchAdmin blogSearchAdmin, Pageable pageable) {
        org.springframework.data.jpa.domain.Specification<Blog> spec = new BlogSpecification(blogSearchAdmin.getFrom(), blogSearchAdmin.getTo(), blogSearchAdmin.getSearch());
        if(blogSearchAdmin.getArchived()!=null)spec=spec.and((root,query,cb)->cb.equal(root.get("archived"),blogSearchAdmin.getArchived()));
        Page<Blog> page = blogRepository.findAll(spec, pageable);
        return page;
    }

    public Blog findBySlug(String slug) {
        Optional<Blog> optionalBlog = blogRepository.findBySlug(slug);
        return optionalBlog.orElse(null);
    }
}
