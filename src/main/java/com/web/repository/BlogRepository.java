package com.web.repository;

import com.web.entity.Blog;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BlogRepository extends JpaRepository<Blog,Long>, JpaSpecificationExecutor<Blog> {

    @Modifying
    @Transactional
    @Query("update Blog b set b.numView = (b.numView + 1)")
    int updateView();

    @Query(value = "select * from blog where archived = false order by id desc limit 1 offset 0", nativeQuery = true)
    Blog lastBlog();

    @Query(value = "select * from blog where archived = false order by id desc limit 5 offset 1", nativeQuery = true)
    List<Blog> listBlogIndex();

    @Query("select b from Blog b where b.slug = ?1 and b.archived = false")
    Optional<Blog> findBySlug(String slug);

    org.springframework.data.domain.Page<Blog> findByArchivedFalse(org.springframework.data.domain.Pageable pageable);
}
