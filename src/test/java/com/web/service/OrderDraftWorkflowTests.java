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
        when(orderDraftRepository.findById(5L)).thenReturn(Optional.of(saved));
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
        when(orderDraftRepository.findById(5L)).thenReturn(Optional.of(saved));
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
        ProductDetail variant = new ProductDetail(); variant.setActive(true); variant.setStock(2);
        when(orderDetailDraftRepository.findById(1L)).thenReturn(Optional.of(line));
        when(productDetailRepository.findById(4L)).thenReturn(Optional.of(variant));
        assertFalse(service.changeQuantity(1L,3).isSuccess()); verify(orderDetailDraftRepository, never()).save(any());
    }
    @Test void rejectsAddingToMissingDraft() { assertFalse(service.addToOrder(1L,7L).isSuccess()); }
    @Test void rejectsAddingOutOfStockVariant() {
        when(orderDraftRepository.existsById(7L)).thenReturn(true);
        ProductDetail variant = new ProductDetail(); variant.setActive(true); variant.setStock(0);
        when(productDetailRepository.findById(1L)).thenReturn(Optional.of(variant));
        assertFalse(service.addToOrder(1L,7L).isSuccess()); verify(orderDetailDraftRepository, never()).save(any());
    }
}
