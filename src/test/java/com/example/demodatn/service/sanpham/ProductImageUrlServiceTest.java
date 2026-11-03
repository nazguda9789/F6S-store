package com.example.demodatn.service.sanpham;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ProductImageUrlServiceTest {
    @TempDir Path uploads;
    @Test void existingImagesKeepTheirUrlAndMissingOrUnsafeImagesUseTheExistingPlaceholder() throws Exception {
        var urls=new ProductImageUrlService(uploads.toString());
        Files.write(uploads.resolve("present.png"),new byte[]{1});
        assertEquals("/uploads/products/present.png",urls.source("/uploads/products/present.png"));
        assertEquals(ProductGalleryService.PLACEHOLDER,urls.source("/uploads/products/missing.png"));
        assertEquals(ProductGalleryService.PLACEHOLDER,urls.source("/images/placeholder.svg"));
        assertEquals(ProductGalleryService.PLACEHOLDER,urls.source("/uploads/products/../outside.png"));
        assertEquals(ProductGalleryService.PLACEHOLDER,urls.source("https://example.test/image.png"));
        assertEquals(ProductGalleryService.PLACEHOLDER,urls.source(null));
        assertEquals(ProductGalleryService.PLACEHOLDER,urls.source(ProductGalleryService.PLACEHOLDER));
    }
}
