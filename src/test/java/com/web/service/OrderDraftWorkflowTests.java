package com.web.service;

import com.web.dto.Result;
import com.web.entity.*;
import com.web.repository.*;
import com.web.utils.UserUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderDraftWorkflowTests {
    @Mock OrderDraftRepository orderDraftRepository;
    @Mock OrderDetailDraftRepository orderDetailDraftRepository;
    @Mock ProductDetailRepository productDetailRepository;
    @Mock UserUtils userUtils;
    @Mock UserRepository userRepository;
    @Mock ProductRepository productRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock InvoiceDetailRepository invoiceDetailRepository;
    @Mock com.web.mapper.InvoiceMapper invoiceMapper;
    @Mock com.web.utils.EmailInvoice emailInvoice;
    @Mock HistoryInvoiceStatusRepository historyInvoiceStatusRepository;
    @Mock com.web.bot.TeleSend teleSend;
    @Mock PosPricingService posPricing;
    @InjectMocks OrderDraftService service;
    @BeforeEach void init() { MockitoAnnotations.openMocks(this); }

    @Test void newDraftDoesNotTrustClientTotalOrCreator() {
        User staff = new User(); staff.setId(2L);
        when(userUtils.getUserWithAuthority()).thenReturn(staff);
        OrderDraft input = new OrderDraft(); input.setTotalAmount(999999D); input.setCreatedBy(new User());
        Result<?> result = service.create(input);
        assertTrue(result.isSuccess());
        OrderDraft saved = (OrderDraft) result.getData();
        assertEquals(0D, saved.getTotalAmount()); assertEquals("Khách lẻ", saved.getFullName()); assertSame(staff, saved.getCreatedBy());
    }
    @Test void updateKeepsCreatorAndUsesPersistedLines() {
        OrderDraft saved = new OrderDraft(); saved.setId(5L); User staff = new User(); saved.setCreatedBy(staff);
        when(orderDraftRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(saved));
        when(orderDetailDraftRepository.totalByOrderId(5L)).thenReturn(15000D);
        OrderDraft input = new OrderDraft(); input.setId(5L); input.setFullName("  Customer  "); input.setTotalAmount(1D);
        assertTrue(service.create(input).isSuccess());
        assertSame(staff, saved.getCreatedBy()); assertEquals(15000D, saved.getTotalAmount()); assertEquals("Customer", saved.getFullName());
    }
    @Test void updateCannotCreateMissingDraft() {
        OrderDraft input = new OrderDraft(); input.setId(77L);
        assertFalse(service.create(input).isSuccess()); verify(orderDraftRepository, never()).save(any());
    }
    @Test void emptyDraftCanSaveCustomerBeforeAddingProducts() {
        OrderDraft saved = new OrderDraft(); saved.setId(5L);
        when(orderDraftRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(saved));
        OrderDraft input = new OrderDraft(); input.setId(5L); input.setEmail("customer@example.test");
        assertTrue(service.create(input).isSuccess()); assertEquals(0D, saved.getTotalAmount());
    }
    @Test void rejectsInvalidMoney() {
        OrderDraft input = new OrderDraft(); input.setDiscount(-1D); assertFalse(service.create(input).isSuccess());
        input.setDiscount(Double.NaN); assertFalse(service.create(input).isSuccess());
        input.setDiscount(0D); input.setShipCost(Double.POSITIVE_INFINITY); assertFalse(service.create(input).isSuccess());
        verify(orderDraftRepository, never()).save(any());
    }
    @Test void rejectsZeroAndNegativeQuantities() {
        assertFalse(service.changeQuantity(1L,0).isSuccess()); assertFalse(service.changeQuantity(1L,-3).isSuccess());
        verify(orderDetailDraftRepository, never()).save(any());
    }
    @Test void rejectsQuantityAboveStock() {
        OrderDetailDraft line = new OrderDetailDraft(); line.setIdProductDetail(4L);
        OrderDraft draft = new OrderDraft(); draft.setId(7L); line.setOrderDraft(draft);
        when(orderDraftRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(draft));
        ProductDetail variant = new ProductDetail(); variant.setActive(true); variant.setStock(2);
        when(orderDetailDraftRepository.findById(1L)).thenReturn(Optional.of(line));
        when(productDetailRepository.findById(4L)).thenReturn(Optional.of(variant));
        assertFalse(service.changeQuantity(1L,3).isSuccess()); verify(orderDetailDraftRepository, never()).save(any());
    }
    @Test void rejectsAddingToMissingDraft() { assertFalse(service.addToOrder(1L,7L).isSuccess()); }
    @Test void rejectsAddingOutOfStockVariant() {
        when(orderDraftRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(new OrderDraft()));
        ProductDetail variant = new ProductDetail(); variant.setActive(true); variant.setStock(0);
        when(productDetailRepository.findById(1L)).thenReturn(Optional.of(variant));
        assertFalse(service.addToOrder(1L,7L).isSuccess()); verify(orderDetailDraftRepository, never()).save(any());
    }
    @Test void persistsVerifiedCustomerSeparatelyFromStaff() {
        User customer = new User(); customer.setId(10L); customer.setRole(com.web.enums.Role.ROLE_USER);
        customer.setActived(true); customer.setActivationStatus(true);
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        OrderDraft input = new OrderDraft(); input.setCustomerId(10L);
        Result<?> result = service.create(input);
        assertTrue(result.isSuccess()); assertEquals(10L, ((OrderDraft) result.getData()).getCustomerId());
    }
    @Test void cannotAttachStaffAsCustomer() {
        User staff = new User(); staff.setRole(com.web.enums.Role.ROLE_ADMIN);
        when(userRepository.findById(10L)).thenReturn(Optional.of(staff));
        OrderDraft input = new OrderDraft(); input.setCustomerId(10L);
        assertFalse(service.create(input).isSuccess()); verify(orderDraftRepository,never()).save(any());
    }
    @Test void consumedDraftCannotBeConvertedAgain() {
        assertFalse(service.changeToInvoice(7L, com.web.enums.InvoiceType.TAI_QUAY, com.web.enums.InvoiceStatus.HOAN_THANH).isSuccess());
        verify(orderDraftRepository).findByIdForUpdate(7L); verifyNoInteractions(productDetailRepository);
    }
    @Test void conversionConsumesDraftAndKeepsCustomerWithoutReplacingCreator() {
        OrderDraft draft = new OrderDraft(); draft.setId(7L); draft.setCustomerId(10L);
        when(orderDraftRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(draft), Optional.empty());
        OrderDetailDraft line = new OrderDetailDraft(); line.setIdProductDetail(4L); line.setQuantity(2); line.setPrice(100D);
        when(orderDetailDraftRepository.findByOrder(7L)).thenReturn(java.util.List.of(line));
        Invoice invoice = new Invoice(); invoice.setId(20L); invoice.setUserId(2L); invoice.setTotalAmount(200D);
        when(invoiceMapper.orderToInvoice(draft)).thenReturn(invoice);
        InvoiceDetail detail = new InvoiceDetail(); detail.setIdProductDetail(4L); detail.setQuantity(2);
        when(invoiceMapper.orderDetailToInvoiceDetail(any())).thenReturn(java.util.List.of(detail));
        ProductDetail variant = new ProductDetail(); variant.setProductId(3L); variant.setStock(5); variant.setActive(true);
        Product parent = new Product(); parent.setActive(true);
        when(productDetailRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(variant));
        when(productRepository.findById(3L)).thenReturn(Optional.of(parent));
        assertTrue(service.changeToInvoice(7L, com.web.enums.InvoiceType.TAI_QUAY, com.web.enums.InvoiceStatus.HOAN_THANH,new java.math.BigDecimal("300")).isSuccess());
        assertEquals(3,variant.getStock()); assertEquals(10L,invoice.getCustomerId()); assertEquals(2L,invoice.getUserId());
        assertNotNull(invoice.getCompletedDate());
        assertEquals(new java.math.BigDecimal("300"),invoice.getCashReceived());assertEquals(new java.math.BigDecimal("100"),invoice.getCashChange());
        verify(productRepository).incrementSold(3L,2);
        verify(orderDetailDraftRepository).deleteAll(java.util.List.of(line)); verify(orderDraftRepository).delete(draft);
        assertFalse(service.changeToInvoice(7L, com.web.enums.InvoiceType.TAI_QUAY, com.web.enums.InvoiceStatus.HOAN_THANH).isSuccess());
        verify(productDetailRepository,times(1)).save(variant); verify(invoiceDetailRepository,times(1)).saveAll(any());
    }
    @Test void rejectsCancelledInitialSaleWithoutTouchingStock() {
        assertFalse(service.changeToInvoice(7L, com.web.enums.InvoiceType.TAI_QUAY, com.web.enums.InvoiceStatus.DA_HUY).isSuccess());
        verifyNoInteractions(orderDraftRepository, productDetailRepository);
    }

    @Test void invalidVoucherDoesNotPartiallyUpdateManagedDraft(){
        var draft=new OrderDraft();draft.setId(7L);draft.setFullName("Before");when(orderDraftRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(draft));when(orderDetailDraftRepository.totalByOrderId(7L)).thenReturn(200D);
        when(posPricing.price(anyDouble(),any(),any(),eq("BAD"))).thenThrow(new IllegalArgumentException("invalid"));
        var input=new OrderDraft();input.setId(7L);input.setFullName("Changed");input.setVoucherCode("BAD");assertFalse(service.create(input).isSuccess());assertEquals("Before",draft.getFullName());verify(orderDraftRepository,never()).save(any());
    }
    @Test void cashShortageDoesNotCreateInvoiceOrTouchStock(){
        var draft=new OrderDraft();draft.setId(7L);when(orderDraftRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(draft));
        var line=new OrderDetailDraft();line.setQuantity(1);line.setPrice(200D);when(orderDetailDraftRepository.findByOrder(7L)).thenReturn(java.util.List.of(line));
        assertFalse(service.changeToInvoice(7L,com.web.enums.InvoiceType.TAI_QUAY,com.web.enums.InvoiceStatus.HOAN_THANH,new java.math.BigDecimal("199")).isSuccess());verifyNoInteractions(invoiceRepository,invoiceMapper,productDetailRepository);
    }
    @Test void unknownSkuCannotChangeDraft(){when(orderDraftRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(new OrderDraft()));assertFalse(service.addBySku(7L,"UNKNOWN").isSuccess());verify(orderDetailDraftRepository,never()).save(any());}
    @Test void rejectsEmptyAndOversizeSku(){assertFalse(service.addBySku(7L," ").isSuccess());assertFalse(service.addBySku(7L,"X".repeat(65)).isSuccess());verifyNoInteractions(productDetailRepository);}
}
