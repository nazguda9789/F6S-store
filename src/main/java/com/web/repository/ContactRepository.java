package com.web.repository;

import com.web.entity.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    @Query("select c from Contact c where c.email like ?1 or c.fullName like ?1 or c.phone like ?1")
    Page<Contact> findByParam(String search, Pageable pageable);

    @Query("select c from Contact c where (c.email like ?1 or c.fullName like ?1 or c.phone like ?1) and c.createdDate between ?2 and ?3")
    Page<Contact> findByParamAndDate(String search, LocalDateTime from, LocalDateTime to, Pageable pageable);
}
