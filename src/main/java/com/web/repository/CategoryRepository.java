package com.web.repository;

import com.web.entity.Category;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("select c from Category c where c.name = ?1 and c.deleted <> true")
    Optional<Category> findByName(String name);

    @Query("select c from Category c where c.name = ?1 and c.id <> ?2 and c.deleted <> true")
    Optional<Category> findByName(String name, Long id);

    @Query("select c from Category c where c.slug = ?1 and c.deleted <> true")
    Optional<Category> findBySlug(String slug);

    @Query("select c from Category c where c.slug = ?1 and c.id <> ?2 and c.deleted <> true")
    Optional<Category> findBySlug(String slug, Long id);

    @Query("select c from Category c where c.deleted <> true order by c.isParent desc, c.position asc")
    public List<Category> findAll();

    @Query("select c from Category c where c.id = ?1 and c.deleted <> true")
    public Optional<Category> findById(Long id);

    @Query("select c from Category c where c.deleted <> true and c.isParent = true")
    public List<Category> findParentCategory();


    @EntityGraph(attributePaths = "children")
    @Query("select c from Category c where c.deleted <> true and c.isParent = true order by c.position asc")
    public List<Category> findParentCategoryUser();

    List<Category> findByIsParentTrue();
    List<Category> findByIsParentFalse();
}
