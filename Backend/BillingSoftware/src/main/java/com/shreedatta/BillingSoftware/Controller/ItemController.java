package com.shreedatta.BillingSoftware.Controller;

import com.shreedatta.BillingSoftware.dto.item.IncreaseItemPricesRequest;
import com.shreedatta.BillingSoftware.dto.item.ItemCreateRequest;
import com.shreedatta.BillingSoftware.dto.item.ItemResponse;
import com.shreedatta.BillingSoftware.dto.item.ItemUpdateRequest;
import com.shreedatta.BillingSoftware.Service.ItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    public ResponseEntity<ItemResponse> createItem(
            @Valid @RequestBody ItemCreateRequest request,
            Authentication authentication
    ) {

        UUID shopId = getShopId(authentication);

        ItemResponse response =
                itemService.createItem(shopId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<ItemResponse>> getAllItems(
            Authentication authentication
    ) {

        UUID shopId = getShopId(authentication);

        return ResponseEntity.ok(
                itemService.getAllItems(shopId)
        );
    }

    @GetMapping("/search")
    public ResponseEntity<List<ItemResponse>> searchItems(
            @RequestParam String q,
            Authentication authentication
    ) {

        UUID shopId = getShopId(authentication);

        return ResponseEntity.ok(
                itemService.searchItems(shopId, q)
        );
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<ItemResponse> getItem(
            @PathVariable UUID itemId,
            Authentication authentication
    ) {

        UUID shopId = getShopId(authentication);

        return ResponseEntity.ok(
                itemService.getItem(shopId, itemId)
        );
    }

    @PatchMapping("/prices/increase")
    public ResponseEntity<Map<String, Object>> increaseAllItemPrices(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody IncreaseItemPricesRequest request
    ) {
        UUID shopId = UUID.fromString(jwt.getClaimAsString("shopId"));

        int updatedCount = itemService.increaseAllItemPrices(
                shopId,
                request.percentage()
        );

        return ResponseEntity.ok(Map.of(
                "message", "Item prices increased successfully",
                "percentage", request.percentage(),
                "updatedItems", updatedCount
        ));
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<ItemResponse> updateItem(
            @PathVariable UUID itemId,
            @Valid @RequestBody ItemUpdateRequest request,
            Authentication authentication
    ) {

        UUID shopId = getShopId(authentication);

        return ResponseEntity.ok(
                itemService.updateItem(
                        shopId,
                        itemId,
                        request
                )
        );
    }

    @PatchMapping("/{itemId}/deactivate")
    public ResponseEntity<Void> deactivateItem(
            @PathVariable UUID itemId,
            Authentication authentication
    ) {

        UUID shopId = getShopId(authentication);

        itemService.deactivateItem(
                shopId,
                itemId
        );

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{itemId}/activate")
    public ResponseEntity<Void> activateItem(
            @PathVariable UUID itemId,
            Authentication authentication
    ) {

        UUID shopId = getShopId(authentication);

        itemService.activateItem(
                shopId,
                itemId
        );

        return ResponseEntity.noContent().build();
    }

    private UUID getShopId(Authentication authentication) {

        if (authentication == null ||
                authentication.getPrincipal() == null) {

            throw new IllegalStateException(
                    "Authenticated user is required"
            );
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof org.springframework.security.oauth2.jwt.Jwt jwt)) {
            throw new IllegalStateException(
                    "Invalid authentication principal"
            );
        }

        String shopId = jwt.getClaimAsString("shopId");

        if (shopId == null || shopId.isBlank()) {
            throw new IllegalStateException(
                    "JWT does not contain shop information"
            );
        }

        try {
            return UUID.fromString(shopId);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Invalid shop information in JWT",
                    exception
            );
        }
    }
}