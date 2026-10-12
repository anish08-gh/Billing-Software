package com.shreedatta.BillingSoftware.dto.auth;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String tokenType,
        UUID userId,
        UUID shopId,
        String name,
        String email,
        String role
) {
}