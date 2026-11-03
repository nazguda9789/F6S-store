package com.example.demodatn.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import java.nio.file.Path;

@Configuration
public class ProductUploadConfig implements WebMvcConfigurer {
    @org.springframework.context.annotation.Bean
    public jakarta.servlet.MultipartConfigElement productMultipartConfig() {
        // 8 ảnh × 5 MB và phần multipart. Service vẫn kiểm tra 5 MB/ảnh.
        org.springframework.boot.servlet.autoconfigure.MultipartProperties properties = new org.springframework.boot.servlet.autoconfigure.MultipartProperties();
        properties.setMaxFileSize(org.springframework.util.unit.DataSize.ofMegabytes(6));
        properties.setMaxRequestSize(org.springframework.util.unit.DataSize.ofMegabytes(42));
        return properties.createMultipartConfig();
    }
    @Value("${shop.upload-dir:uploads/products}") private String directory;
    @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(directory).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/products/**").addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
