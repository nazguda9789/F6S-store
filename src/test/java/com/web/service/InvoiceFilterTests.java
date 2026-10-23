package com.web.service;

import com.web.entity.Invoice;
import com.web.enums.InvoiceStatus;
import com.web.specification.InvoiceSpecification;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;
import java.sql.Date;
import java.time.LocalDateTime;
import static org.mockito.Mockito.*;

class InvoiceFilterTests {
    @Test void searchDoesNotBypassStatus() {
        Root<Invoice> root = mock(Root.class, RETURNS_DEEP_STUBS);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        InvoiceStatus status = InvoiceStatus.DANG_CHO_VAN_CHUYEN;
        new InvoiceSpecification(status, null, null, "test").toPredicate(root, query, cb);
        verify(cb).equal(root.get("invoiceStatus"), status);
    }
    @Test void acceptsFromDateWithoutToDate() {
        Root<Invoice> root = mock(Root.class, RETURNS_DEEP_STUBS);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        new InvoiceSpecification(null, Date.valueOf("2026-10-01"), null, "test").toPredicate(root, query, cb);
        verify(cb).greaterThanOrEqualTo(root.<LocalDateTime>get("createdDate"), LocalDateTime.of(2026,10,1,0,0));
    }
    @Test void toDateIncludesWholeDayUsingExclusiveNextDay() {
        Root<Invoice> root = mock(Root.class, RETURNS_DEEP_STUBS);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        new InvoiceSpecification(null, null, Date.valueOf("2026-10-23"), "test").toPredicate(root, query, cb);
        verify(cb).lessThan(root.<LocalDateTime>get("createdDate"), LocalDateTime.of(2026,10,24,0,0));
    }
}
