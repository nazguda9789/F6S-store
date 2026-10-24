package com.web.service;

import com.web.entity.Voucher;
import com.web.repository.VoucherRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PosPricingService {
    public record Price(double merchandise, double discount, double shipping, double payable, String voucherCode) {}
    public record Suggestion(String code, String name, int discount, int minimum) {}
    public record Cash(BigDecimal received, BigDecimal change) {}
    private final VoucherRepository vouchers;
    public PosPricingService(VoucherRepository vouchers){this.vouchers=vouchers;}

    public Price price(double merchandise, Double manualDiscount, Double shipping, String code) {
        double discount=manualDiscount==null?0:manualDiscount, ship=shipping==null?0:shipping;
        if(!Double.isFinite(merchandise)||merchandise<0||!Double.isFinite(ship)||ship<0)
            throw new IllegalArgumentException("Tiền hàng hoặc phí vận chuyển không hợp lệ.");
        String normalized=code==null?"":code.strip();
        if(!normalized.isEmpty()){
            Voucher voucher=vouchers.findByCode(normalized).orElseThrow(()->new IllegalArgumentException("Phiếu giảm giá không tồn tại."));
            if(!Boolean.FALSE.equals(voucher.getBlock()) || voucher.getMinAmount()==null || voucher.getMinAmount()<0
                    || voucher.getDiscount()==null || voucher.getDiscount()<0 || voucher.getMinAmount()>merchandise)
                throw new IllegalArgumentException("Phiếu giảm giá bị khóa, sai cấu hình hoặc đơn chưa đủ điều kiện.");
            discount=voucher.getDiscount();
        }
        if(!Double.isFinite(discount)||discount<0||discount>merchandise || !Double.isFinite(merchandise+ship-discount))
            throw new IllegalArgumentException("Giảm giá không được âm hoặc vượt tiền hàng.");
        return new Price(merchandise,discount,ship,merchandise+ship-discount,normalized);
    }

    public List<Suggestion> suggestions(double merchandise){
        return vouchers.availableCheckout(merchandise).stream()
            .filter(v->Boolean.FALSE.equals(v.getBlock())&&v.getDiscount()!=null&&v.getDiscount()>=0&&v.getDiscount()<=merchandise
                &&v.getMinAmount()!=null&&v.getMinAmount()>=0&&v.getMinAmount()<=merchandise)
            .sorted(java.util.Comparator.comparing(Voucher::getDiscount).reversed().thenComparing(v->v.getCode()==null?"":v.getCode()))
            .filter(v->v.getCode()!=null&&!v.getCode().isBlank()).limit(10)
            .map(v->new Suggestion(v.getCode(),v.getName(),v.getDiscount(),v.getMinAmount())).toList();
    }

    public static Cash cash(double payable, BigDecimal received){
        if(!Double.isFinite(payable)||payable<0||received==null)throw new IllegalArgumentException("Nhập tiền khách đưa để hoàn thành bán tại quầy.");
        try{
            BigDecimal due=BigDecimal.valueOf(payable).setScale(0,RoundingMode.UNNECESSARY);
            BigDecimal paid=received.setScale(0,RoundingMode.UNNECESSARY);
            if(paid.signum()<0||paid.precision()>19||paid.compareTo(due)<0)
                throw new IllegalArgumentException("Tiền khách đưa chưa đủ hoặc vượt giới hạn 19 chữ số.");
            return new Cash(paid,paid.subtract(due));
        }catch(ArithmeticException e){throw new IllegalArgumentException("Tiền mặt VND phải là số nguyên; kiểm tra giá và phí trước khi chốt.");}
    }
}
