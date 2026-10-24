package com.web.service;
import com.web.entity.Invoice;
import com.web.enums.InvoiceStatus;
import com.web.repository.*;
import com.web.utils.UserUtils;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class InvoiceStateTests {
    private final InvoiceRepository invoices=mock(InvoiceRepository.class);
    private final ProductRepository products=mock(ProductRepository.class);
    private final InvoiceService service=new InvoiceService(mock(SimpMessagingTemplate.class));
    InvoiceStateTests(){ReflectionTestUtils.setField(service,"invoiceRepository",invoices);ReflectionTestUtils.setField(service,"productRepository",products);ReflectionTestUtils.setField(service,"historyInvoiceStatusRepository",mock(HistoryInvoiceStatusRepository.class));ReflectionTestUtils.setField(service,"userUtils",mock(UserUtils.class));ReflectionTestUtils.setField(service,"messagingTemplate",mock(SimpMessagingTemplate.class));}
    @Test void missingInvoiceUsesLockAndDoesNotWrite(){assertFalse(service.changeStatus(5L,InvoiceStatus.HOAN_THANH).isSuccess());verify(invoices).findByIdForUpdate(5L);verify(invoices,never()).save(any());}
    @Test void rejectsDuplicateTerminalStatus(){var i=new Invoice();i.setInvoiceStatus(InvoiceStatus.DA_HUY);when(invoices.findByIdForUpdate(5L)).thenReturn(Optional.of(i));assertFalse(service.changeStatus(5L,InvoiceStatus.DA_HUY).isSuccess());verify(invoices,never()).save(any());}
    @Test void preservesFirstCompletionAfterExchangeRequest(){var i=new Invoice();i.setInvoiceStatus(InvoiceStatus.YEU_CAU_DOI_HANG);var date=LocalDateTime.of(2026,10,24,10,0);i.setCompletedDate(date);when(invoices.findByIdForUpdate(5L)).thenReturn(Optional.of(i));assertTrue(service.changeStatus(5L,InvoiceStatus.HOAN_THANH).isSuccess());assertEquals(date,i.getCompletedDate());verifyNoInteractions(products);}
}
