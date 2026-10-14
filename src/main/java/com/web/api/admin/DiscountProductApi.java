package com.web.api.admin;

import com.web.dto.BlogSearchAdmin;
import com.web.dto.DiscountProductRequest;
import com.web.dto.UpdateDiscountProductRequest;
import com.web.entity.Blog;
import com.web.entity.DiscountProduct;
import com.web.enums.DiscountProductType;
import com.web.service.BlogService;
import com.web.service.DiscountProductService;
import com.web.utils.TypeSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(TypeSystem.ADMIN_URL+ "/api/v1/discount-product")
public class DiscountProductApi {

    @Autowired
    private DiscountProductService discountProductService;

    @PostMapping("/admin/save")
    public ResponseEntity<?> filter(@RequestBody List<DiscountProductRequest> request){
        Map<String, Object> result = discountProductService.save(request);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }


    @PostMapping("/admin/get-all")
    public ResponseEntity<?> filter(@RequestParam(required = false) Long productId,
                                    @RequestParam(required = false) DiscountProductType type,
                                    @RequestParam(required = false) LocalDateTime fromDate,
                                    @RequestParam(required = false) LocalDateTime toDate,
                                    Pageable pageable){
        Page<DiscountProduct> result = discountProductService.getDiscounts(productId, type,fromDate, toDate, pageable);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/admin/{id}")
    public ResponseEntity<?> filter(@PathVariable("id") Long id){
        DiscountProduct result = discountProductService.findById(id);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PutMapping("/admin/update/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody UpdateDiscountProductRequest request){

        discountProductService.update(id, request);

        return ResponseEntity.ok("Cập nhật thành công");
    }


    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteProduct(@RequestParam Long id){
        discountProductService.delete(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
