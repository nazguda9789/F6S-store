package com.web.repository;

import com.web.entity.Banner;
import com.web.enums.BannerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {


    List<Banner> findAllByActiveTrueOrderByDisplayOrderAsc();

    @Query("select b from Banner b where b.pageName like ?1 and b.bannerType = ?2 order by b.displayOrder")
    List<Banner> findByPageName(String pageName, BannerType bannerType);
}
