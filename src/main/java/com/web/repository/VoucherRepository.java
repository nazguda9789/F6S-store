package com.web.repository;

import com.web.entity.Voucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher,Long> {


    @Query("select v from Voucher v where v.code = ?1 ")
    public Optional<Voucher> findByCode(String code);

    @Query("select v from Voucher v where v.code = ?1 and v.id <> ?2")
    public Optional<Voucher> findByCode(String code, Long id);

    @Query("select v from Voucher v where v.block = false")
    List<Voucher> available();

    @Query(value = "select * from voucher v where v.block = false order by v.discount desc limit 3 ", nativeQuery = true)
    List<Voucher> availableLimit3();

    @Query(value = "select * from voucher v where v.block = false and v.min_amount <= ?1 order by v.discount desc ", nativeQuery = true)
    List<Voucher> availableCheckout(Double total);

    @Query(value = "select * from voucher v where v.block = false and v.min_amount > ?1 order by v.min_amount asc ", nativeQuery = true)
    List<Voucher> notAvailableCheckout(Double total);

    List<Voucher> findTop5ByBlockFalseOrderByCreatedDateDesc();
    Optional<Voucher> findByCodeAndBlockFalse(String code);
}
