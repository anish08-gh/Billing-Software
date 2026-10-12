package com.shreedatta.BillingSoftware.Controller;

import com.shreedatta.BillingSoftware.dto.ShopProfileResponse;
import com.shreedatta.BillingSoftware.Service.ShopProfileService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/shop")
public class ShopProfileController {

    private final ShopProfileService shopProfileService;

    public ShopProfileController(ShopProfileService shopProfileService) {
        this.shopProfileService = shopProfileService;
    }

    @GetMapping("/profile")
    public ShopProfileResponse getShopProfile(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID shopId = UUID.fromString(
                jwt.getClaimAsString("shopId")
        );

        return shopProfileService.getShopProfile(shopId);
    }
}