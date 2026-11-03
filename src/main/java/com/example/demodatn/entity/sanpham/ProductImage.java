package com.example.demodatn.entity.sanpham;
import com.example.demodatn.util.ProductMedia;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="product_images")
@Getter @Setter
public class ProductImage {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="product_id", nullable=false)
    private Product product;
    @Column(name="duong_dan", nullable=false, length=2048, columnDefinition="nvarchar(2048)")
    @org.hibernate.annotations.Nationalized private String path;
    @Column(name="thu_tu", nullable=false)
    private int position;
    @Column(name="la_anh_chinh", nullable=false)
    private boolean main;
    public String getDisplayPath() { return ProductMedia.source(path); }
}
