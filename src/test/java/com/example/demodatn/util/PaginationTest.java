package com.example.demodatn.util;
import com.example.demodatn.controller.PaginationAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.data.domain.Sort;
import static org.junit.jupiter.api.Assertions.*;

class PaginationTest {
    @Test void clampsInvalidInputAndUsesConfiguredSize() {
        var pages=new Pagination(12);
        assertEquals(0,pages.index("abc"));assertEquals(0,pages.index("-1"));
        assertEquals(2,pages.request("99999",30,Sort.by("id")).getPageNumber());
        assertEquals(12,pages.getSize());
    }
    @Test void linksKeepAllValuesAndEncodePlusUnicodeAndSeparators() {
        var request=new MockHttpServletRequest("GET","/products");
        request.addParameter("keyword","Giày + dép & mới");request.addParameter("filter","a","b");request.addParameter("page","0");
        var links=new PaginationAdvice().links(request);String url=links.url("page",2);
        assertTrue(url.contains("%2B"));assertTrue(url.contains("%26"));assertTrue(url.contains("filter=a&filter=b"));assertTrue(url.endsWith("page=2"));
        assertEquals(java.util.List.of(0,-1,49,50,51,-1,99),links.numbers(50,100));
    }
}
