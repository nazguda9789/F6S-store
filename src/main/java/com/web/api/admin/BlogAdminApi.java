package com.web.api.admin;

import com.web.dto.BlogSearchAdmin;
import com.web.entity.Blog;
import com.web.service.BlogService;
import com.web.utils.TypeSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(TypeSystem.ADMIN_URL+ "/api/v1/blog")
public class BlogAdminApi {

    @Autowired
    private BlogService blogService;

    @PostMapping("/get-all-by-admin")
    public ResponseEntity<?> filter(@RequestBody BlogSearchAdmin blogSearchAdmin, Pageable pageable){
        Page<Blog> result = blogService.allBlogByAdmin(blogSearchAdmin, pageable);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteProduct(@RequestParam Long id){
        blogService.delete(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
