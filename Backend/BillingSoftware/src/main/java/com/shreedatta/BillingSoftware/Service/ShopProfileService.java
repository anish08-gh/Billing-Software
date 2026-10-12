package com.shreedatta.BillingSoftware.Service;

import com.shreedatta.BillingSoftware.dto.ShopProfileResponse;
import com.shreedatta.BillingSoftware.Entity.Shop;
import com.shreedatta.BillingSoftware.Repository.ShopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ShopProfileService {

    private final ShopRepository shopRepository;

    public ShopProfileService(ShopRepository shopRepository) {
        this.shopRepository = shopRepository;
    }

    @Transactional(readOnly = true)
    public ShopProfileResponse getShopProfile(UUID shopId) {
        Shop shop = shopRepository.findById(shopId)
                .filter(Shop::isActive)
                .orElseThrow(() ->
                        new IllegalStateException("Shop not found or inactive")
                );

        return new ShopProfileResponse(
                shop.getId(),
                shop.getName(),
                shop.getPhone()
        );
    }
}