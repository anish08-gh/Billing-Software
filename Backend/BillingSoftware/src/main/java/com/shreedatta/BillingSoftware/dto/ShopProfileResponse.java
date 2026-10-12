package com.shreedatta.BillingSoftware.dto;

import java.util.UUID;

public record ShopProfileResponse(
        UUID id,
        String name,
        String phone
) {
}