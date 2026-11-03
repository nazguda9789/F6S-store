package com.example.demodatn.entity.donhang;
import com.example.demodatn.entity.sanpham.ProductVariant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="pos_held_items",uniqueConstraints=@UniqueConstraint(name="uq_pos_hold_variant",columnNames={"held_id","variant_id"}))
@Getter @Setter
public class PosHeldItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="held_id",nullable=false) private PosHeldOrder held;
    @ManyToOne(optional=false) @JoinColumn(name="variant_id",nullable=false) private ProductVariant variant;
    @Column(nullable=false) private Integer quantity;
}
