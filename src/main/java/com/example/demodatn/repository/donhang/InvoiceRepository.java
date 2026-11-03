package com.example.demodatn.repository.donhang;
import com.example.demodatn.entity.donhang.Invoice;
public interface InvoiceRepository extends org.springframework.data.jpa.repository.JpaRepository<Invoice,Integer>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Invoice> {
    java.util.Optional<Invoice> findByOrder_Id(Integer id);
}
