package com.example.demodatn.repository.sanpham;
import com.example.demodatn.entity.sanpham.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage,Long> {
    boolean existsByPath(String path);
    List<ProductImage> findByProduct_IdOrderByPositionAscIdAsc(Integer productId);
}
