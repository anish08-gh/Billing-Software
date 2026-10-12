package com.shreedatta.BillingSoftware.Controller;

import com.shreedatta.BillingSoftware.dto.bill.*;
import com.shreedatta.BillingSoftware.Service.BillService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bills")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @PostMapping
    public ResponseEntity<BillResponse> createBill(
            @Valid @RequestBody BillCreateRequest request,
            Authentication authentication
    ) {
        UUID shopId = getShopId(authentication);
        UUID userId = getUserId(authentication);

        BillResponse response = billService.createBill(
                shopId,
                userId,
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{billId}")
    public ResponseEntity<BillResponse> getBillById(
            @PathVariable UUID billId,
            Authentication authentication
    ) {
        UUID shopId = getShopId(authentication);

        BillResponse response = billService.getBillById(
                shopId,
                billId
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<BillResponse>> getBills(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        UUID shopId = getShopId(authentication);

        Page<BillResponse> response = billService.getBills(
                shopId,
                page,
                size
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<BillResponse>> searchBills(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        UUID shopId = getShopId(authentication);

        Page<BillResponse> response = billService.searchBills(
                shopId,
                q,
                page,
                size
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard-stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats(
            Authentication authentication
    ) {
        UUID shopId = getShopId(authentication);

        DashboardStatsResponse response =
                billService.getDashboardStats(shopId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{billId}/payments")
    public ResponseEntity<List<PaymentResponse>> getBillPaymentHistory(
            @PathVariable UUID billId,
            Authentication authentication
    ) {
        UUID shopId = getShopId(authentication);

        return ResponseEntity.ok(
                billService.getBillPaymentHistory(shopId, billId)
        );
    }

    @PutMapping("/{billId}")
    public ResponseEntity<BillResponse> updateBill(
            @PathVariable UUID billId,
            @Valid @RequestBody BillUpdateRequest request,
            Authentication authentication
    ) {
        UUID shopId = getShopId(authentication);
        UUID userId = getUserId(authentication);

        return ResponseEntity.ok(
                billService.updateBill(
                        shopId,
                        userId,
                        billId,
                        request
                )
        );
    }

    @PostMapping("/{billId}/payments")
    public ResponseEntity<BillResponse> recordAdditionalPayment(
            @PathVariable UUID billId,
            @Valid @RequestBody AdditionalPaymentRequest request,
            Authentication authentication
    ) {
        UUID shopId = getShopId(authentication);
        UUID userId = getUserId(authentication);

        BillResponse response = billService.recordAdditionalPayment(
                shopId,
                userId,
                billId,
                request
        );

        return ResponseEntity.ok(response);
    }


    private UUID getShopId(Authentication authentication) {

        if (authentication == null ||
                authentication.getPrincipal() == null) {

            throw new IllegalStateException(
                    "Authenticated user is required"
            );
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof Jwt jwt)) {
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

    private UUID getUserId(Authentication authentication) {

        if (authentication == null ||
                authentication.getPrincipal() == null) {

            throw new IllegalStateException(
                    "Authenticated user is required"
            );
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof Jwt jwt)) {
            throw new IllegalStateException(
                    "Invalid authentication principal"
            );
        }

        String userId = jwt.getSubject();

        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException(
                    "JWT does not contain user information"
            );
        }

        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Invalid user information in JWT",
                    exception
            );
        }
    }
}
