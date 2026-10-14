package com.web.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.web.entity.Blog;
import com.web.entity.Product;
import com.web.service.BlogService;
import com.web.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Controller
public class BlogUserController {

    @Autowired
    private BlogService blogService;

    private Integer size = 9;

    @GetMapping({"/blog/{slug}"})
    public String productView(@PathVariable String slug, Model model, HttpServletRequest request){
        Blog blog = blogService.findBySlug(slug);
        if(blog == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Blog not found");
        }
        model.addAttribute("blog", blog);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonLd = mapper.createObjectNode();
        jsonLd.put("@context", "https://schema.org");
        jsonLd.put("@type", "BlogPosting");
        jsonLd.put("headline", blog.getSeoTitle());
        jsonLd.put("description", blog.getMetaDescription());
        jsonLd.put("image", blog.getImageBanner());

        // Author
        ObjectNode author = mapper.createObjectNode();
        author.put("@type", "Person");
        author.put("name", blog.getUser() != null ? blog.getUser().getFullName() : "Admin");
        jsonLd.set("author", author);

        // Date published
        if (blog.getCreatedDate() != null) {
            jsonLd.put("datePublished", blog.getCreatedDate().toLocalDate().toString());
        }

        // mainEntityOfPage
        ObjectNode mainEntity = mapper.createObjectNode();
        mainEntity.put("@type", "WebPage");

        String domain = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort());

        String blogUrl = domain + "/blog/" + blog.getSlug();
        mainEntity.put("@id", blogUrl);
        jsonLd.set("mainEntityOfPage", mainEntity);

        try {
            String jsonLdString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonLd);
            model.addAttribute("jsonLd", jsonLdString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "user/blogdetail";
    }

    @GetMapping("/blogs")
    public String blogList(Model model, Pageable pageable){
        pageable = PageRequest.of(pageable.getPageNumber(), size, Sort.by("id").descending());
        Page<Blog> page = blogService.findAllList(pageable);
        model.addAttribute("blogList", page.getContent());
        model.addAttribute("tongSoTrang", page.getTotalPages());
        model.addAttribute("pageable", pageable);
        model.addAttribute("trangHienTai", page.getNumber());
        return "user/bloglist.html";
    }
}
