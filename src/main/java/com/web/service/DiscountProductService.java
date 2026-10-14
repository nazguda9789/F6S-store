package com.web.service;

import com.web.dto.ActiveDiscountResponse;
import com.web.dto.DiscountProductRequest;
import com.web.dto.UpdateDiscountProductRequest;
import com.web.entity.DiscountProduct;
import com.web.entity.Product;
import com.web.entity.ProductDetail;
import com.web.entity.User;
import com.web.enums.DiscountProductType;
import com.web.repository.DiscountProductRepository;
import com.web.repository.ProductRepository;
import com.web.utils.UserUtils;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class DiscountProductService {

    @Autowired
    private DiscountProductRepository discountProductRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserUtils userUtils;

    public Page<DiscountProduct> getDiscounts(
            Long productId,
            DiscountProductType type,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable) {

        return discountProductRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Lọc theo Product ID
            if (productId != null) {
                predicates.add(cb.equal(root.get("product").get("id"), productId));
            }

            // 2. Lọc theo Loại giảm giá (Enum)
            if (type != null) {
                predicates.add(cb.equal(root.get("discountProductType"), type));
            }

            // 3. Lọc theo khoảng thời gian bắt đầu (startDate)
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), fromDate));
            }

            // 4. Lọc theo khoảng thời gian kết thúc (endDate)
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), toDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    public DiscountProduct findById(Long id){
        return discountProductRepository.findById(id).orElse(null);
    }

    public Map<String, Object> save(List<DiscountProductRequest> list){

        Map<String, Object> map = new HashMap<>();

        int success = 0;
        int fail = 0;

        List<String> productName = new ArrayList<>();

        User user = userUtils.getUserWithAuthority();

        for(DiscountProductRequest d : list){

            // kiểm tra trùng thời gian giảm giá
            if(discountProductRepository.existsDiscountInTime(
                    d.getProductId(),
                    d.getStartDate(),
                    d.getEndDate()
            )){
                ++fail;

                String name = productRepository.getNameById(d.getProductId());
                productName.add(name);

                continue;
            }

            try{

                Product product = productRepository.findById(d.getProductId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

                DiscountProduct discount = new DiscountProduct();

                discount.setProduct(product);
                discount.setUser(user);
                discount.setStartDate(d.getStartDate());
                discount.setEndDate(d.getEndDate());
                discount.setDiscountProductType(d.getType());
                discount.setDiscountValue(d.getDiscountValue());
                discount.setMaxDiscountValue(d.getMaxDiscountValue());

                discountProductRepository.save(discount);

                ++success;

            }catch (Exception e){
                ++fail;

                String name = productRepository.getNameById(d.getProductId());
                productName.add(name);
            }
        }

        map.put("success", success);
        map.put("fail", fail);
        map.put("failProducts", productName);

        return map;
    }

    public void delete(Long id){
        discountProductRepository.deleteById(id);
    }

    public Double calculatePrice(ProductDetail productDetail){

        Double price = productDetail.getPrice();

        DiscountProduct discount = discountProductRepository
                .findActiveDiscount(productDetail.getProductId(), LocalDateTime.now());

        if(discount == null){
            return price;
        }

        if(discount.getDiscountProductType() == DiscountProductType.PERCENT){

            double discountAmount = price * discount.getDiscountValue() / 100;

            if(discount.getMaxDiscountValue() != null){
                discountAmount = Math.min(discountAmount, discount.getMaxDiscountValue());
            }

            return price - discountAmount;
        }

        if(discount.getDiscountProductType() == DiscountProductType.FIXED){
            return price - discount.getDiscountValue();
        }

        return price;
    }

    public Double calculatePrice(ProductDetail productDetail,DiscountProduct discount){

        Double price = productDetail.getPrice();

        if(discount.getDiscountProductType() == DiscountProductType.PERCENT){

            double discountAmount = price * discount.getDiscountValue() / 100;

            if(discount.getMaxDiscountValue() != null){
                discountAmount = Math.min(discountAmount, discount.getMaxDiscountValue());
            }

            return price - discountAmount;
        }

        if(discount.getDiscountProductType() == DiscountProductType.FIXED){
            return price - discount.getDiscountValue();
        }

        return price;
    }


    @Transactional
    public void update(Long id, UpdateDiscountProductRequest request){

        DiscountProduct discount = discountProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khuyến mại"));

        // validate ngày
        if(request.getStartDate().isAfter(request.getEndDate())){
            throw new RuntimeException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        // kiểm tra trùng thời gian
        boolean existed = discountProductRepository.existsDiscountInTimeUpdate(
                id,
                discount.getProduct().getId(),
                request.getStartDate(),
                request.getEndDate()
        );

        if(existed){
            throw new RuntimeException("Sản phẩm đã có khuyến mại trong khoảng thời gian này");
        }

        // cập nhật dữ liệu
        discount.setDiscountProductType(request.getDiscountProductType());
        discount.setDiscountValue(request.getDiscountValue());
        discount.setStartDate(request.getStartDate());
        discount.setEndDate(request.getEndDate());
        discount.setMaxDiscountValue(request.getMaxDiscountValue());
//        if(request.getDiscountProductType().name().equals("PERCENT")){
//            discount.setMaxDiscountValue(request.getMaxDiscountValue());
//        }
//        else{
//            discount.setMaxDiscountValue(null);
//        }

        discountProductRepository.save(discount);
    }


    public ActiveDiscountResponse getActiveDiscount(Long productId, Double price){

        Optional<ActiveDiscountResponse> optional =
                discountProductRepository.findActiveDiscountDto(productId, LocalDateTime.now());

        if(optional.isEmpty()){
            return null;
        }

        ActiveDiscountResponse discount = optional.get();

        double discountAmount = 0;

        if(discount.getType() == DiscountProductType.PERCENT){

            discountAmount = price * discount.getDiscountValue() / 100;

            if(discount.getMaxDiscountValue() != null){
                discountAmount = Math.min(discountAmount, discount.getMaxDiscountValue());
            }
        }

        if(discount.getType() == DiscountProductType.FIXED){
            discountAmount = discount.getDiscountValue();
        }

        double finalPrice = price - discountAmount;

        discount.setDiscountAmount(discountAmount);
        discount.setFinalPrice(finalPrice);

        return discount;
    }
}
