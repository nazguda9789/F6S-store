package com.web.repository;

import com.web.entity.Color;
import com.web.entity.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SizeRepository extends JpaRepository<Size, Long> {

    @Query("select c from Size c where c.deleted <> true")
    public List<Size> findAll();

    @Query("select c from Size c where c.name = ?1 and c.deleted = false")
    public Size findByName(String name);

    @Query("select c from Size c where c.name = ?1 and c.id <> ?2 and c.deleted = false ")
    public Size findByName(String name, Long id);
}
