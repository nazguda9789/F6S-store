package com.example.demodatn.util;
import com.example.demodatn.service.sanpham.ProductGalleryService;
public final class ProductMedia {
    private ProductMedia() { }
    public static String source(String path) {
        return path!=null && (path.startsWith("/uploads/products/") || path.startsWith("/images/") || path.startsWith("/anh/"))
            ? path : ProductGalleryService.PLACEHOLDER;
    }
}
