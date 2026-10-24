package com.web.service;

import com.web.entity.Voucher;
import com.web.repository.VoucherRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class PosPricingTests {
    private final VoucherRepository repo=mock(VoucherRepository.class);
    private final PosPricingService service=new PosPricingService(repo);
    private Voucher voucher(String code,int discount,int minimum){var v=new Voucher();v.setCode(code);v.setDiscount(discount);v.setMinAmount(minimum);v.setBlock(false);return v;}
    @Test void computesManualDiscountAndShipping(){var p=service.price(100000,10000D,30000D,null);assertEquals(120000,p.payable());verifyNoInteractions(repo);}
    @Test void rejectsInvalidAmounts(){for(double d:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY}){assertThrows(IllegalArgumentException.class,()->service.price(d,0D,0D,null));assertThrows(IllegalArgumentException.class,()->service.price(10,d,0D,null));assertThrows(IllegalArgumentException.class,()->service.price(10,0D,d,null));}assertThrows(IllegalArgumentException.class,()->service.price(10,11D,0D,null));}
    @Test void voucherAmountComesFromDatabaseNotClient(){when(repo.findByCode("VC")).thenReturn(Optional.of(voucher("VC",20000,100000)));var p=service.price(100000,99999D,0D," VC ");assertEquals(20000,p.discount());assertEquals(80000,p.payable());}
    @Test void checksEligibilityAgainWhenBlocked(){var v=voucher("VC",20000,100000);v.setBlock(true);when(repo.findByCode("VC")).thenReturn(Optional.of(v));assertThrows(IllegalArgumentException.class,()->service.price(100000,0D,0D,"VC"));}
    @Test void rejectsMissingAndTooSmallVouchers(){assertThrows(IllegalArgumentException.class,()->service.price(100000,0D,0D,"MISSING"));when(repo.findByCode("VC")).thenReturn(Optional.of(voucher("VC",10000,200000)));assertThrows(IllegalArgumentException.class,()->service.price(100000,0D,0D,"VC"));}
    @Test void sortsValidSuggestionsBestFirst(){var blocked=voucher("BAD",90000,0);blocked.setBlock(true);when(repo.availableCheckout(100000D)).thenReturn(List.of(voucher("B",10000,50000),voucher("A",20000,100000),blocked));assertEquals(List.of("A","B"),service.suggestions(100000).stream().map(PosPricingService.Suggestion::code).toList());}
    @Test void recordsCashAndChangeExactly(){var c=PosPricingService.cash(85000,new BigDecimal("100000"));assertEquals(new BigDecimal("100000"),c.received());assertEquals(new BigDecimal("15000"),c.change());}
    @Test void rejectsMissingOrInsufficientCash(){assertThrows(IllegalArgumentException.class,()->PosPricingService.cash(100,null));assertThrows(IllegalArgumentException.class,()->PosPricingService.cash(100,new BigDecimal("99")));}
    @Test void rejectsFractionalOrOversizeCash(){assertThrows(IllegalArgumentException.class,()->PosPricingService.cash(10,new BigDecimal("10.5")));assertThrows(IllegalArgumentException.class,()->PosPricingService.cash(10.5,new BigDecimal("20")));assertThrows(IllegalArgumentException.class,()->PosPricingService.cash(10,new BigDecimal("10000000000000000000")));}
    @Test void supportsExactPaymentAndZeroDue(){assertEquals(BigDecimal.ZERO,PosPricingService.cash(0,BigDecimal.ZERO).change());assertEquals(BigDecimal.ZERO,PosPricingService.cash(100,new BigDecimal("100")).change());}
}
