
package com.shreedatta.BillingSoftware.Service;

import com.shreedatta.BillingSoftware.Entity.Bill;
import com.shreedatta.BillingSoftware.Entity.BillItem;
import com.shreedatta.BillingSoftware.Entity.BillVersion;
import com.shreedatta.BillingSoftware.Entity.Item;
import com.shreedatta.BillingSoftware.Entity.Payment;
import com.shreedatta.BillingSoftware.Entity.Shop;
import com.shreedatta.BillingSoftware.Entity.User;
import com.shreedatta.BillingSoftware.Mapper.bill.PaymentMapper;
import com.shreedatta.BillingSoftware.dto.bill.*;
import com.shreedatta.BillingSoftware.Mapper.bill.BillMapper;
import com.shreedatta.BillingSoftware.Repository.BillItemRepository;
import com.shreedatta.BillingSoftware.Repository.BillRepository;
import com.shreedatta.BillingSoftware.Repository.BillVersionRepository;
import com.shreedatta.BillingSoftware.Repository.ItemRepository;
import com.shreedatta.BillingSoftware.Repository.PaymentRepository;
import com.shreedatta.BillingSoftware.Repository.ShopRepository;
import com.shreedatta.BillingSoftware.Repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.shreedatta.BillingSoftware.dto.bill.BillUpdateRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class BillService {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    private final BillRepository billRepository;
    private final BillItemRepository billItemRepository;
    private final PaymentRepository paymentRepository;
    private final BillVersionRepository billVersionRepository;
    private final ItemRepository itemRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final BillMapper billMapper;
    private final PaymentMapper paymentMapper;

    public BillService(
            BillRepository billRepository,
            BillItemRepository billItemRepository,
            PaymentRepository paymentRepository,
            BillVersionRepository billVersionRepository,
            ItemRepository itemRepository,
            ShopRepository shopRepository,
            UserRepository userRepository,
            BillMapper billMapper, PaymentMapper paymentMapper
    ) {
        this.billRepository = billRepository;
        this.billItemRepository = billItemRepository;
        this.paymentRepository = paymentRepository;
        this.billVersionRepository = billVersionRepository;
        this.itemRepository = itemRepository;
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.billMapper = billMapper;
        this.paymentMapper = paymentMapper;
    }

    @Transactional
    public BillResponse createBill(
            UUID shopId,
            UUID userId,
            BillCreateRequest request
    ) {
        Shop shop = shopRepository.findByIdForUpdate(shopId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Shop not found"
                ));

        if (!shop.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "This shop is inactive"
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"
                ));

        if (!user.isActive()
                || user.getShop() == null
                || !shopId.equals(user.getShop().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to create bills for this shop"
            );
        }

        // Validate all items and calculate amounts on the server.
        List<Item> selectedItems = new ArrayList<>();
        List<BigDecimal> quantities = new ArrayList<>();
        List<BigDecimal> unitPrices = new ArrayList<>();
        List<BigDecimal> lineTotals = new ArrayList<>();

        BigDecimal subtotal = BigDecimal.ZERO.setScale(MONEY_SCALE);

        for (BillItemRequest itemRequest : request.items()) {
            Item item = itemRepository
                    .findByIdAndShopId(itemRequest.itemId(), shopId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "An item was not found in your shop"
                    ));

            if (!item.isActive()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Item '" + item.getName() + "' is inactive"
                );
            }

            BigDecimal quantity = itemRequest.quantity();

            BigDecimal unitPrice = item.getPrice()
                    .setScale(MONEY_SCALE, MONEY_ROUNDING);

            BigDecimal lineTotal = quantity
                    .multiply(unitPrice)
                    .setScale(MONEY_SCALE, MONEY_ROUNDING);

            selectedItems.add(item);
            quantities.add(quantity);
            unitPrices.add(unitPrice);
            lineTotals.add(lineTotal);

            subtotal = subtotal.add(lineTotal);
        }

        subtotal = subtotal.setScale(MONEY_SCALE, MONEY_ROUNDING);
        BigDecimal totalAmount = subtotal;

        // Validate an optional initial payment before persisting the bill.
        InitialPaymentRequest initialPayment = request.initialPayment();

        BigDecimal amountPaid = BigDecimal.ZERO.setScale(MONEY_SCALE);

        if (initialPayment != null) {
            amountPaid = initialPayment.amount()
                    .setScale(MONEY_SCALE, MONEY_ROUNDING);

            if (amountPaid.compareTo(totalAmount) > 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Initial payment cannot exceed the bill total"
                );
            }
        }

        // The shop row lock protects bill sequence generation.
        long sequence = shop.incrementBillSequence();

        String billNumber = String.format(
                Locale.ROOT, "BILL-%06d", sequence
        );

        shopRepository.save(shop);

        Bill bill = new Bill(
                shop,
                billNumber,
                request.customerName().trim(),
                request.customerPhone().trim(),
                subtotal,
                totalAmount
        );

        bill = billRepository.save(bill);

        // Create each line using the persisted parent bill.
        List<BillItem> billItems = new ArrayList<>();

        for (int i = 0; i < selectedItems.size(); i++) {
            Item item = selectedItems.get(i);

            BillItem billItem = new BillItem(
                    bill,
                    item,
                    item.getName(),
                    item.getUnit(),
                    quantities.get(i),
                    unitPrices.get(i),
                    lineTotals.get(i),
                    i + 1
            );

            billItems.add(billItem);
        }

        billItems = billItemRepository.saveAll(billItems);

        // Record the initial payment as a separate immutable record.
        if (initialPayment != null) {
            Payment payment = new Payment(
                    bill,
                    amountPaid,
                    initialPayment.paymentMethod(),
                    initialPayment.paymentReference(),
                    initialPayment.notes(),
                    OffsetDateTime.now()
            );

            paymentRepository.save(payment);
        }

        Bill.PaymentStatus paymentStatus;

        if (amountPaid.compareTo(BigDecimal.ZERO) == 0) {
            paymentStatus = Bill.PaymentStatus.PENDING;
        } else if (amountPaid.compareTo(totalAmount) == 0) {
            paymentStatus = Bill.PaymentStatus.PAID;
        } else {
            paymentStatus = Bill.PaymentStatus.PARTIALLY_PAID;
        }

        bill.updatePaymentStatus(paymentStatus);
        bill = billRepository.save(bill);

        Map<String, Object> snapshot = createAuditSnapshot(
                bill, billItems, amountPaid, 1L
        );

        BillVersion version = new BillVersion(
                bill,
                1L,
                user,
                BillVersion.Action.CREATE,
                snapshot
        );

        billVersionRepository.save(version);

        return billMapper.toBillResponse(
                bill, billItems, amountPaid
        );
    }

    @Transactional(readOnly = true)
    public BillResponse getBillById(UUID shopId, UUID billId) {

        Bill bill = billRepository.findByIdAndShopId(billId, shopId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bill not found"
                ));

        List<BillItem> billItems =
                billItemRepository.findByBillIdOrderByLineNumberAsc(
                        bill.getId()
                );

        BigDecimal amountPaid =
                paymentRepository.getTotalPaidByBillId(bill.getId());

        return billMapper.toBillResponse(
                bill,
                billItems,
                amountPaid
        );
    }

    @Transactional
    public BillResponse recordAdditionalPayment(
            UUID shopId,
            UUID userId,
            UUID billId,
            AdditionalPaymentRequest request
    ) {
        // Lock the specific bill and verify shop ownership.
        Bill bill = billRepository
                .findByIdAndShopIdForUpdate(billId, shopId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bill not found"
                ));

        if (bill.getBillStatus() != Bill.BillStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot record a payment against a cancelled bill"
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        if (!user.isActive()
                || user.getShop() == null
                || !shopId.equals(user.getShop().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to record payments for this shop"
            );
        }

        // Calculate the current balance from immutable payment records.
        BigDecimal amountPaidBefore =
                paymentRepository.getTotalPaidByBillId(billId);

        if (amountPaidBefore == null) {
            amountPaidBefore = BigDecimal.ZERO;
        }

        BigDecimal pendingBefore =
                bill.getTotalAmount().subtract(amountPaidBefore);

        BigDecimal paymentAmount = request.amount()
                .setScale(MONEY_SCALE, MONEY_ROUNDING);

        if (paymentAmount.compareTo(pendingBefore) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment cannot exceed the pending balance of "
                            + pendingBefore.toPlainString()
            );
        }

        // Append a new payment record. Never overwrite previous payments.
        Payment payment = new Payment(
                bill,
                paymentAmount,
                request.paymentMethod(),
                request.paymentReference(),
                request.notes(),
                OffsetDateTime.now()
        );

        paymentRepository.saveAndFlush(payment);

        BigDecimal amountPaidAfter =
                paymentRepository.getTotalPaidByBillId(billId);

        BigDecimal pendingAfter =
                bill.getTotalAmount().subtract(amountPaidAfter);

        Bill.PaymentStatus paymentStatus;

        if (pendingAfter.compareTo(BigDecimal.ZERO) == 0) {
            paymentStatus = Bill.PaymentStatus.PAID;
        } else if (amountPaidAfter.compareTo(BigDecimal.ZERO) == 0) {
            paymentStatus = Bill.PaymentStatus.PENDING;
        } else {
            paymentStatus = Bill.PaymentStatus.PARTIALLY_PAID;
        }

        bill.updatePaymentStatus(paymentStatus);
        bill = billRepository.save(bill);

        // Create a new business audit version.
        List<BillVersion> previousVersions =
                billVersionRepository
                        .findByBillIdOrderByVersionNumberDesc(billId);

        long auditVersionNumber = previousVersions.isEmpty()
                ? 1L
                : previousVersions.get(0).getVersionNumber() + 1L;

        List<BillItem> billItems =
                billItemRepository.findByBillIdOrderByLineNumberAsc(billId);

        Map<String, Object> snapshot = createAuditSnapshot(
                bill,
                billItems,
                amountPaidAfter,
                auditVersionNumber
        );

        snapshot.put("paymentEvent", Map.of(
                "paymentId", payment.getId().toString(),
                "amount", paymentAmount,
                "paymentMethod", payment.getPaymentMethod().name(),
                "pendingAmountAfterPayment", pendingAfter
        ));

        BillVersion version = new BillVersion(
                bill,
                auditVersionNumber,
                user,
                BillVersion.Action.UPDATE,
                snapshot
        );

        billVersionRepository.save(version);

        return billMapper.toBillResponse(
                bill,
                billItems,
                amountPaidAfter
        );
    }

    @Transactional(readOnly = true)
    public Page<BillResponse> getBills(
            UUID shopId,
            int page,
            int size
    ) {
        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page number cannot be negative"
            );
        }

        if (size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page size must be between 1 and 100"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Bill> bills = billRepository
                .findByShopIdOrderByCreatedAtDesc(shopId, pageable);

        return bills.map(bill -> {
            List<BillItem> billItems =
                    billItemRepository.findByBillIdOrderByLineNumberAsc(
                            bill.getId()
                    );

            BigDecimal amountPaid =
                    paymentRepository.getTotalPaidByBillId(bill.getId());

            return billMapper.toBillResponse(
                    bill,
                    billItems,
                    amountPaid
            );
        });
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(UUID shopId) {

        Object[] stats = billRepository.getDashboardBillStats(
                shopId,
                Bill.BillStatus.ACTIVE
        );

        Object[] values = stats;

        if (stats.length == 1 && stats[0] instanceof Object[] nestedValues) {
            values = nestedValues;
        }

        long totalBills = ((Number) values[0]).longValue();
        BigDecimal totalRevenue = new BigDecimal(values[1].toString());

        BigDecimal totalCollected =
                paymentRepository.getTotalCollectedByShopId(
                        shopId,
                        Bill.BillStatus.ACTIVE
                );

        BigDecimal pendingPayments =
                totalRevenue.subtract(totalCollected);

        long outstandingBills =
                billRepository.countByShopIdAndBillStatusAndPaymentStatusNot(
                        shopId,
                        Bill.BillStatus.ACTIVE,
                        Bill.PaymentStatus.PAID
                );

        return new DashboardStatsResponse(
                totalBills,
                totalRevenue,
                totalCollected,
                pendingPayments,
                outstandingBills
        );
    }

    @Transactional
    public BillResponse updateBill(
            UUID shopId,
            UUID userId,
            UUID billId,
            BillUpdateRequest request
    ) {
        Bill bill = billRepository
                .findByIdAndShopIdForUpdate(billId, shopId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bill not found"
                ));

        if (bill.getBillStatus() != Bill.BillStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot edit a cancelled bill"
            );
        }

        if (request.expectedVersion() == null
                || request.expectedVersion() != bill.getVersion()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This bill has changed. Refresh it and try again."
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        if (!user.isActive()
                || user.getShop() == null
                || !shopId.equals(user.getShop().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to edit bills for this shop"
            );
        }

        List<BillItem> existingItems =
                billItemRepository.findByBillIdOrderByLineNumberAsc(billId);

        BigDecimal amountPaid =
                paymentRepository.getTotalPaidByBillId(billId);

        if (amountPaid == null) {
            amountPaid = BigDecimal.ZERO;
        }

        // Preserve the complete pre-edit state in the audit history.
        // Prepare the next business audit version and capture the original state.
        List<BillVersion> previousVersions =
                billVersionRepository
                        .findByBillIdOrderByVersionNumberDesc(billId);

        long auditVersionNumber = previousVersions.isEmpty()
                ? 1L
                : previousVersions.get(0).getVersionNumber() + 1L;

        Map<String, Object> beforeSnapshot = createAuditSnapshot(
                bill,
                existingItems,
                amountPaid,
                auditVersionNumber
        );


        BigDecimal subtotal = BigDecimal.ZERO;
        List<BillItem> updatedItems = new ArrayList<>();

        for (int i = 0; i < request.items().size(); i++) {
            BillItemRequest itemRequest = request.items().get(i);

            Item item = itemRepository
                    .findByIdAndShopId(itemRequest.itemId(), shopId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "One or more items do not belong to this shop"
                    ));

            if (!item.isActive()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cannot add an inactive item to a bill"
                );
            }

            BigDecimal quantity = itemRequest.quantity()
                    .setScale(3, RoundingMode.UNNECESSARY);

            BigDecimal unitPrice = item.getPrice()
                    .setScale(MONEY_SCALE, MONEY_ROUNDING);

            BigDecimal lineTotal = quantity
                    .multiply(unitPrice)
                    .setScale(MONEY_SCALE, MONEY_ROUNDING);

            subtotal = subtotal.add(lineTotal);

            updatedItems.add(new BillItem(
                    bill,
                    item,
                    item.getName(),
                    item.getUnit(),
                    quantity,
                    unitPrice,
                    lineTotal,
                    i + 1
            ));
        }

        subtotal = subtotal.setScale(MONEY_SCALE, MONEY_ROUNDING);
        BigDecimal totalAmount = subtotal;

        if (totalAmount.compareTo(amountPaid) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The updated bill total cannot be less than the amount already paid"
            );
        }

        // Replace the item rows only after validating the complete new bill.
        billItemRepository.deleteAll(existingItems);
        billItemRepository.flush();

        billItemRepository.saveAll(updatedItems);

        bill.updateCustomerDetails(
                request.customerName().trim(),
                request.customerPhone().trim()
        );

        bill.updateAmounts(subtotal, totalAmount);

        BigDecimal pendingAmount = totalAmount.subtract(amountPaid);

        Bill.PaymentStatus paymentStatus;

        if (pendingAmount.compareTo(BigDecimal.ZERO) == 0) {
            paymentStatus = Bill.PaymentStatus.PAID;
        } else if (amountPaid.compareTo(BigDecimal.ZERO) == 0) {
            paymentStatus = Bill.PaymentStatus.PENDING;
        } else {
            paymentStatus = Bill.PaymentStatus.PARTIALLY_PAID;
        }

        bill.updatePaymentStatus(paymentStatus);
        bill = billRepository.saveAndFlush(bill);

        Map<String, Object> afterSnapshot = createAuditSnapshot(
                bill,
                updatedItems,
                amountPaid,
                auditVersionNumber
        );

        Map<String, Object> auditSnapshot = new LinkedHashMap<>();
        auditSnapshot.put("versionNumber", auditVersionNumber);
        auditSnapshot.put("changeType", "BILL_EDIT");
        auditSnapshot.put("before", beforeSnapshot);
        auditSnapshot.put("after", afterSnapshot);

        billVersionRepository.save(new BillVersion(
                bill,
                auditVersionNumber,
                user,
                BillVersion.Action.UPDATE,
                auditSnapshot
        ));

        return billMapper.toBillResponse(
                bill,
                updatedItems,
                amountPaid
        );
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getBillPaymentHistory(UUID shopId, UUID billId) {
        billRepository.findByIdAndShopId(billId, shopId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bill not found"
                ));

        return paymentRepository.findByBillIdOrderByPaidAtDesc(billId)
                .stream()
                .map(paymentMapper::toPaymentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<BillResponse> searchBills(
            UUID shopId,
            String query,
            int page,
            int size
    ) {
        if (query == null || query.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Search query cannot be blank"
            );
        }

        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page number cannot be negative"
            );
        }

        if (size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page size must be between 1 and 100"
            );
        }

        String normalizedQuery = query.trim();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Bill> bills = billRepository.searchByCustomerNameOrPhone(
                shopId,
                normalizedQuery,
                pageable
        );

        return bills.map(bill -> {
            List<BillItem> billItems =
                    billItemRepository.findByBillIdOrderByLineNumberAsc(
                            bill.getId()
                    );

            BigDecimal amountPaid =
                    paymentRepository.getTotalPaidByBillId(bill.getId());

            return billMapper.toBillResponse(
                    bill,
                    billItems,
                    amountPaid
            );
        });
    }


    private Map<String, Object> createAuditSnapshot(
            Bill bill,
            List<BillItem> items,
            BigDecimal amountPaid,
            long versionNumber
    ) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        snapshot.put("billId", bill.getId().toString());
        snapshot.put("billNumber", bill.getBillNumber());
        snapshot.put("customerName", bill.getCustomerName());
        snapshot.put("customerPhone", bill.getCustomerPhone());
        snapshot.put("subtotal", bill.getSubtotal());
        snapshot.put("totalAmount", bill.getTotalAmount());
        snapshot.put("amountPaid", amountPaid);
        snapshot.put(
                "pendingAmount",
                bill.getTotalAmount().subtract(amountPaid)
        );
        snapshot.put("paymentStatus", bill.getPaymentStatus().name());
        snapshot.put("billStatus", bill.getBillStatus().name());
        snapshot.put("versionNumber", versionNumber);

        List<Map<String, Object>> itemSnapshots = new ArrayList<>();

        for (BillItem item : items) {
            Map<String, Object> itemSnapshot = new LinkedHashMap<>();

            itemSnapshot.put("billItemId", item.getId().toString());
            itemSnapshot.put(
                    "itemId",
                    item.getItem() == null
                            ? null
                            : item.getItem().getId().toString()
            );
            itemSnapshot.put("itemName", item.getItemName());
            itemSnapshot.put("unit", item.getUnit());
            itemSnapshot.put("quantity", item.getQuantity());
            itemSnapshot.put("unitPrice", item.getUnitPrice());
            itemSnapshot.put("totalAmount", item.getTotalAmount());
            itemSnapshot.put("lineNumber", item.getLineNumber());

            itemSnapshots.add(itemSnapshot);
        }

        snapshot.put("items", itemSnapshots);

        return snapshot;
    }
}