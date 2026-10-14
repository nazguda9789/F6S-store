package com.web.api.user;

import com.web.dto.ProductDetailView;
import com.web.dto.ProductSearch;
import com.web.dto.ProductView;
import com.web.service.ProductService;
import com.web.utils.TypeSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductUserApi {

    @Autowired
    private ProductService productService;

    @GetMapping("/find-by-category/{category}")
    public ResponseEntity<?> findByCategory(@PathVariable Long category){
        Page<ProductView> page = productService.getProductByCategory(category);
        return new ResponseEntity<>(page.getContent(), HttpStatus.OK);
    }

    @GetMapping("/search-by-param")
    public ResponseEntity<?> searchByParam(@RequestParam String search, Pageable pageable){
        Page<ProductView> page = productService.getProductByParam(search, pageable);
        return new ResponseEntity<>(page, HttpStatus.OK);
    }

    @GetMapping("/variant/{slug}")
    public ResponseEntity<?> findVariantDetailByProduct(@PathVariable String slug){
        List<ProductDetailView> result = productService.getVariant(slug);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/filter")
    public ResponseEntity<?> searchProductFull(@RequestBody ProductSearch search, Pageable pageable){
        Page<ProductView> result = productService.searchFull(search, pageable);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

}
