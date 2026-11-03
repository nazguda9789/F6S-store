package com.example.demodatn.service.khuyenmai;
import com.example.demodatn.dto.khuyenmai.PromotionDto;
import com.example.demodatn.dto.khuyenmai.VoucherDto;
import com.example.demodatn.entity.khuyenmai.Promotion;
import com.example.demodatn.entity.khuyenmai.PromotionDetail;
import com.example.demodatn.entity.khuyenmai.Voucher;
import com.example.demodatn.exception.DiscountValidationException;
import com.example.demodatn.repository.donhang.OrderRepository;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.repository.khuyenmai.PromotionDetailRepository;
import com.example.demodatn.repository.khuyenmai.PromotionRepository;
import com.example.demodatn.repository.khuyenmai.VoucherRepository;
import com.example.demodatn.repository.sanpham.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Service @RequiredArgsConstructor
public class DiscountAdminService {
    private final VoucherRepository vouchers;
    private final PromotionRepository promotions;
    private final PromotionDetailRepository details;
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final OrderRepository orders;
    public Voucher voucher(Integer id) { return vouchers.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)); }
    public Promotion promotion(Integer id) { return promotions.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)); }
    private void dates(LocalDateTime start,LocalDateTime end) {
        if(start==null || end==null || !end.isAfter(start)) throw new DiscountValidationException("endDate","Kết thúc phải sau thời điểm bắt đầu.");
    }
    @Transactional
    public Voucher saveVoucher(VoucherDto f) {
        dates(f.getStartDate(),f.getEndDate());
        String code=VoucherService.normalize(f.getCode());
        if(vouchers.existsByCodeIgnoreCaseAndIdNot(code,f.getId()==null ? -1 : f.getId())) throw new DiscountValidationException("code","Mã phiếu giảm giá đã tồn tại.");
        if(!Set.of("PERCENT","FIXED").contains(f.getDiscountType())) throw new DiscountValidationException("discountType","Loại phiếu giảm giá không hợp lệ.");
        if(f.getDiscountValue()==null || f.getDiscountValue().signum()<=0 || ("PERCENT".equals(f.getDiscountType()) && (f.getDiscountValue().compareTo(BigDecimal.ONE)<0 || f.getDiscountValue().compareTo(BigDecimal.valueOf(100))>0))) throw new DiscountValidationException("discountValue","Phần trăm từ 1–100; số tiền phải lớn hơn 0.");
        if(f.getMaxDiscount()!=null && f.getMaxDiscount().signum()<=0) throw new DiscountValidationException("maxDiscount","Giảm tối đa phải lớn hơn 0.");
        Voucher v=f.getId()==null ? new Voucher() : vouchers.findForUpdate(f.getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(f.getUsageLimit()!=null && f.getUsageLimit()<Math.max(1,v.getUsedCount()==null ? 0 : v.getUsedCount())) throw new DiscountValidationException("usageLimit","Giới hạn không được nhỏ hơn số lượt đã dùng và phải ít nhất 1.");
        v.setCode(code);v.setName(f.getName().trim());v.setDiscountType(f.getDiscountType());v.setDiscountValue(f.getDiscountValue());
        v.setMinOrderValue(f.getMinOrderValue()==null ? BigDecimal.ZERO : f.getMinOrderValue());
        v.setMaxDiscount("PERCENT".equals(f.getDiscountType()) ? f.getMaxDiscount() : null);
        v.setStartDate(f.getStartDate());v.setEndDate(f.getEndDate());v.setUsageLimit(f.getUsageLimit());v.setIsPublic(f.getIsPublic());v.setStatus(f.getStatus());
        if(Boolean.TRUE.equals(f.getIsPublic())) v.setCustomer(null);
        else {
            if(f.getCustomerId()==null) throw new DiscountValidationException("customerId","Phiếu giảm giá riêng tư bắt buộc chọn khách hàng.");
            v.setCustomer(customers.findById(f.getCustomerId()).orElseThrow(()->new DiscountValidationException("customerId","Khách hàng không tồn tại.")));
        }
        // usedCount is deliberately not bindable from the Admin form.
        return vouchers.saveAndFlush(v);
    }
    @Transactional
    public String deleteVoucher(Integer id) {
        Voucher v=vouchers.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(orders.existsByVoucher_Id(id) || (v.getUsedCount()!=null && v.getUsedCount()>0)) { v.setStatus(false);return "Phiếu giảm giá đã được sử dụng: đã khóa và giữ dữ liệu đơn cũ."; }
        vouchers.delete(v);return "Đã xóa phiếu giảm giá.";
    }
    @Transactional
    public Promotion savePromotion(PromotionDto f) {
        dates(f.getStartDate(),f.getEndDate());
        if(f.getDiscountValue()==null || f.getDiscountValue().compareTo(BigDecimal.ONE)<0 || f.getDiscountValue().compareTo(BigDecimal.valueOf(100))>0 || f.getDiscountValue().stripTrailingZeros().scale()>0) throw new DiscountValidationException("discountValue","Phần trăm nguyên từ 1 đến 100.");
        Promotion p=f.getId()==null ? new Promotion() : promotions.findForUpdate(f.getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        p.setName(f.getName().trim());p.setDiscountType("PERCENT");p.setDiscountValue(f.getDiscountValue());p.setStartDate(f.getStartDate());p.setEndDate(f.getEndDate());p.setStatus(f.getStatus());
        return promotions.saveAndFlush(p);
    }
    @Transactional
    public void selectProducts(Integer id,List<Integer> ids) {
        Promotion p=promotions.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        Set<Integer> unique=new HashSet<>(ids);
        var chosen=products.findAllById(unique);
        if(chosen.size()!=unique.size()) throw new IllegalArgumentException("Có sản phẩm không tồn tại; tải lại danh sách.");
        details.deleteByPromotion_Id(id);details.flush();
        for(var product:chosen) { var d=new PromotionDetail();d.setPromotion(p);d.setProduct(product);details.save(d); }
    }
    @Transactional
    public void selectProductPage(Integer id,List<Integer> visible,List<Integer> chosen) {
        Promotion promotion=promotions.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(!visible.containsAll(chosen))throw new IllegalArgumentException("Danh sách sản phẩm không hợp lệ.");
        var selected=products.findAllById(new HashSet<>(chosen));
        if(selected.size()!=new HashSet<>(chosen).size())throw new IllegalArgumentException("Có sản phẩm không tồn tại.");
        details.deleteAll(details.findByPromotion_IdAndProduct_IdIn(id,visible));details.flush();
        for(var product:selected){var row=new PromotionDetail();row.setPromotion(promotion);row.setProduct(product);details.save(row);}
    }
    @Transactional
    public void deletePromotion(Integer id) {
        Promotion p=promotions.findForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        details.deleteByPromotion_Id(id);details.flush();promotions.delete(p);
    }
}
