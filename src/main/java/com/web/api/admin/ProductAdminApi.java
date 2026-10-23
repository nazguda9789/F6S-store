package com.web.api.admin;

import com.web.dto.*;
import com.web.entity.Product;
import com.web.entity.ProductDetail;
import com.web.entity.ProductImage;
import com.web.repository.ProductDetailRepository;
import com.web.repository.ProductImageRepository;
import com.web.repository.ProductRepository;
import com.web.service.ProductColorImageService;
import com.web.service.ProductService;
import com.web.utils.TypeSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(TypeSystem.ADMIN_URL+ "/api/product")
public class ProductAdminApi {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> invalidAttribute(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(java.util.Map.of("message", exception.getMessage() == null ? "Dữ liệu không hợp lệ" : exception.getMessage()));
    }

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ProductDetailRepository productDetailRepository;

    @Autowired
    private ProductColorImageService productColorImageService;

    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody ProductCreateRequest productCreateRequest){
        Product result = productService.createProduct(productCreateRequest);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/update-image-color/{id}")
    public ResponseEntity<?> updateImageColor(@PathVariable Long id,@RequestParam("file") MultipartFile file){
        String result = productColorImageService.changeImage(id, file);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/update-image-color-by-url/{id}")
    public ResponseEntity<?> updateImageColorByUrl(@PathVariable Long id,@RequestBody String image){
        productColorImageService.changeImageByUrl(id, image);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/update")
    public ResponseEntity<?> update(@RequestBody ProductCreateRequest productCreateRequest, @RequestParam Long id){
        Product result = productService.updateProduct(productCreateRequest, id);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @GetMapping("/find-by-id")
    public ResponseEntity<?> findById(@RequestParam Long id){
        Product result = productService.findById(id);
        List<ProductImage> productImages = productImageRepository.findByProduct(id);
        List<ProductDetail> productDetails = productDetailRepository.findByProductId(id);
        ProductDetailAdmin productDetailAdmin = new ProductDetailAdmin(result,productImages, productDetails);
        return new ResponseEntity<>(productDetailAdmin, HttpStatus.CREATED);
    }


    @PostMapping("/filter")
    public ResponseEntity<?> filter(@RequestBody ProductSearchAdmin productSearchAdmin, Pageable pageable){
        Page<Product> result = productService.allProductByAdmin(productSearchAdmin, pageable);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/filter-get")
    public ResponseEntity<?> filter( Pageable pageable){
        Page<Product> result = productRepository.findAll(pageable);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @DeleteMapping("/delete-image")
    public ResponseEntity<?> deleteImage(@RequestParam Long id){
        productImageRepository.deleteById(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/update-detail")
    public ResponseEntity<?> updateDetail(@RequestBody ProductDetail productDetail){
        ProductDetail result = productService.updateDetail(productDetail);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @DeleteMapping("/delete-detail")
    public ResponseEntity<?> deleteDetail(@RequestParam Long id){
        String result = productService.deleteDetail(id);
        return new ResponseEntity<>(result,HttpStatus.OK);
    }

    @DeleteMapping("/delete-product")
    public ResponseEntity<?> deleteProduct(@RequestParam Long id){
        productService.deleteProduct(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/product-admin")
    public ResponseEntity<?> productAdmin(@RequestParam(defaultValue = "") String search, Pageable pageable){
        Page<ProductResponseAdmin> result = productService.getProductAdmin(pageable, search);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/all-product-admin")
    public ResponseEntity<?> productAdminByCategory(@RequestBody List<Long> categories){
        List<ProductView> result = productRepository.findByCategoryIds(categories);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }


}
