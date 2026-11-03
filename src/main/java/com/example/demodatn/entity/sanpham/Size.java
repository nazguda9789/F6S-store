package com.example.demodatn.entity.sanpham;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sizes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Size {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "size_value", nullable = false, unique = true)
    @org.hibernate.annotations.Nationalized private String sizeValue;
}
