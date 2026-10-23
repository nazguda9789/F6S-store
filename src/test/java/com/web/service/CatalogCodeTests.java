package com.web.service;

import com.web.dto.ProductCreateRequest;
import com.web.entity.ProductDetail;
import com.web.repository.ProductDetailRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogCodeTests {
    CatalogCodeService codes = new CatalogCodeService();
    @Test void padsProductAndVariantCodes() {
        assertEquals("F6S-SP000031", codes.productCode(31L)); assertEquals("F6S-SKU000239", codes.variantCode(239L));
    }
    @Test void expandsBeyondSixDigitsWithoutTruncation() {
        assertEquals("F6S-SP1000000", codes.productCode(1000000L)); assertNotEquals(codes.variantCode(1L), codes.variantCode(1000001L));
    }
    @Test void rejectsMissingOrInvalidId() {
        assertThrows(IllegalArgumentException.class, () -> codes.productCode(null));
        assertThrows(IllegalArgumentException.class, () -> codes.variantCode(0L));
    }
    @Test void productAndVariantNamespacesCannotCollide() { assertNotEquals(codes.productCode(17L), codes.variantCode(17L)); }
    @Test void rejectsDuplicateNewCombinations() {
        ProductCreateRequest request = new ProductCreateRequest();
        ProductCreateRequest.ProductDetailRequest line = new ProductCreateRequest.ProductDetailRequest();
        line.setColorId(1L); line.setSizeId(2L); line.setStock(0); line.setPrice(100D);
        request.setDetails(List.of(line,line));
        assertThrows(IllegalArgumentException.class, () -> new ProductService().createProduct(request));
    }
    @Test void rejectsNegativeStockBeforeWritingProduct() {
        ProductCreateRequest request = new ProductCreateRequest();
        ProductCreateRequest.ProductDetailRequest line = new ProductCreateRequest.ProductDetailRequest();
        line.setColorId(1L); line.setSizeId(2L); line.setStock(-1); request.setDetails(List.of(line));
        assertThrows(IllegalArgumentException.class, () -> new ProductService().createProduct(request));
    }
    @Test void editingVariantKeepsParentAndCodeAndArchivesOldSku() {
        ProductService service = new ProductService(); ProductDetailRepository repository = mock(ProductDetailRepository.class);
        ReflectionTestUtils.setField(service,"productDetailRepository",repository); ReflectionTestUtils.setField(service,"catalogCodes",codes);
        ProductDetail saved = new ProductDetail(); saved.setId(17L); saved.setProductId(5L); saved.setSku("OLD-SKU"); saved.setColorId(1L); saved.setSizeId(2L);
        when(repository.findById(17L)).thenReturn(Optional.of(saved)); when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        ProductDetail input = new ProductDetail(); input.setId(17L); input.setProductId(999L); input.setSku("FORGED"); input.setColorId(1L); input.setSizeId(2L); input.setStock(4); input.setPrice(500D);
        ProductDetail result = service.updateDetail(input);
        assertEquals(5L,result.getProductId()); assertEquals("F6S-SKU000017",result.getSku()); assertEquals("OLD-SKU",result.getLegacySku());
    }
}
