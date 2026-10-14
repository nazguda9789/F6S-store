package com.web.repository;

import com.web.entity.Category;
import com.web.entity.Color;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ColorRepository extends JpaRepository<Color, Long> {

    @Query("select c from Color c where c.deleted <> true")
    public List<Color> findAll();

    @Query("select c from Color c where c.name = ?1 and c.deleted = false")
    public Color findByName(String name);

    @Query("select c from Color c where c.name = ?1 and c.id <> ?2 and c.deleted = false ")
    public Color findByName(String name, Long id);
}
