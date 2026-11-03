package com.example.demodatn.entity.sanpham;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="inventory_logs")
@Getter @Setter @NoArgsConstructor
public class InventoryLog {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="variant_id",nullable=false) private ProductVariant variant;
    @Column(name="quantity_change",nullable=false) private Integer quantityChange;
    @Column(name="quantity_before",nullable=false) private Integer quantityBefore;
    @Column(name="quantity_after",nullable=false) private Integer quantityAfter;
    @Column(nullable=false,length=500,columnDefinition="nvarchar(500)") @org.hibernate.annotations.Nationalized private String reason;
    @Column(nullable=false,length=255) private String actor;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
}

