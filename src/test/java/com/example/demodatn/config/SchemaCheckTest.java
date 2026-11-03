package com.example.demodatn.config;

import com.example.demodatn.entity.khachhang.ShippingAddress;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import javax.sql.DataSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SchemaCheckTest {
    @Test void missingTableNamesItsMigrationAndMissingColumnsAreAllReported() {
        var required = SchemaCheck.requiredColumns(List.of(ShippingAddress.class));
        assertEquals(Set.of("id","customer_id","ten_nguoi_nhan","so_dien_thoai","dia_chi","la_mac_dinh","thu_tu","created_at"), required.get("dia_chi_nhan_hang"));
        assertEquals(List.of("Thiếu bảng dbo.dia_chi_nhan_hang — 009_dia_chi_nhan_hang.sql"), SchemaCheck.missing(required, Map.of()));
        var missing = SchemaCheck.missing(required, Map.of("dia_chi_nhan_hang", Set.of("id")));
        assertEquals(7, missing.size());
        assertTrue(missing.stream().allMatch(line -> line.contains("009_dia_chi_nhan_hang.sql")));
    }

    @Test void completeSchemaHasNoErrorsAndUnrelatedBeansDoNotOpenAConnection() {
        var required = SchemaCheck.requiredColumns(List.of(ShippingAddress.class));
        assertTrue(SchemaCheck.missing(required, required).isEmpty());
        var provider = new StaticListableBeanFactory().getBeanProvider(DataSource.class);
        var checker = new SchemaCheck(provider);
        Object bean = new Object();
        assertSame(bean, checker.postProcessBeforeInitialization(bean, "ordinaryBean"));
    }
}
