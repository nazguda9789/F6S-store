package com.web.service;

import com.web.entity.Banner;
import com.web.enums.BannerType;
import com.web.repository.BannerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BannerService {

    @Autowired
    private BannerRepository bannerRepository;

    public List<Banner> findByPageNameAndType(String pageName, BannerType bannerType) {
        return bannerRepository.findByPageName("%"+pageName+"%", bannerType);
    }

    // Lấy tất cả banner (admin)
    public List<Banner> getAllBanners() {
        return bannerRepository.findAll();
    }

    // Lấy banner theo id
    public Optional<Banner> getBannerById(Long id) {
        return bannerRepository.findById(id);
    }

    // Lưu hoặc cập nhật banner
    public Banner saveBanner(Banner banner) {
        return bannerRepository.save(banner);
    }

    // Xóa banner
    public void deleteBanner(Long id) {
        bannerRepository.deleteById(id);
    }
}
