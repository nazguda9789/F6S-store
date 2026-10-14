package com.web.service;

import com.web.dto.Result;
import com.web.entity.ShopeeKey;
import com.web.repository.ShopeeKeyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShopeeKeyService {

    private final ShopeeKeyRepository shopeeKeyRepository;

    public ShopeeKeyService(ShopeeKeyRepository shopeeKeyRepository) {
        this.shopeeKeyRepository = shopeeKeyRepository;
    }

    public List<ShopeeKey> findAll() {
        return shopeeKeyRepository.findAll();
    }

    public Result<?> save(ShopeeKey shopeeKey) {
        shopeeKeyRepository.save(shopeeKey);
        Result<ShopeeKey> result = new Result<>(true, shopeeKey, "Thêm key thành công");
        return result;
    }

    public void delete(Long id) {
        shopeeKeyRepository.deleteById(id);
    }
}
