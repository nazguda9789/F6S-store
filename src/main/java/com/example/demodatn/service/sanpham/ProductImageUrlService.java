package com.example.demodatn.service.sanpham;

import com.example.demodatn.util.ProductMedia;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.nio.file.Files;
import java.nio.file.Path;

@Service("productImageUrl")
public class ProductImageUrlService {
    private final Path uploads;

    public ProductImageUrlService(@Value("${shop.upload-dir:uploads/products}") String directory) {
        uploads = Path.of(directory).toAbsolutePath().normalize();
    }

    public String source(String path) {
        String safe = ProductMedia.source(path);
        if (safe.startsWith("/uploads/products/")) {
            Path file = uploads.resolve(safe.substring("/uploads/products/".length())).normalize();
            return file.startsWith(uploads) && Files.isRegularFile(file) ? safe : ProductGalleryService.PLACEHOLDER;
        }
        return new ClassPathResource("static" + safe).exists() ? safe : ProductGalleryService.PLACEHOLDER;
    }
}
