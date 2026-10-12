package com.shreedatta.BillingSoftware.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bills")
public class Bill {

    public enum PaymentStatus {
        PENDING,
        PARTIALLY_PAID,
        PAID
    }

    public enum BillStatus {
        ACTIVE,
        CANCELLED
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "shop_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_bills_shop")
    )
    private Shop shop;

    @Column(name = "bill_number", nullable = false, length = 50)
    private String billNumber;

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 15)
    private String customerPhone;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "bill_status", nullable = false, length = 30)
    private BillStatus billStatus = BillStatus.ACTIVE;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Bill() {
    }

    public Bill(
            Shop shop,
            String billNumber,
            String customerName,
            String customerPhone,
            BigDecimal subtotal,
            BigDecimal totalAmount
    ) {
        this.id = UUID.randomUUID();
        this.shop = shop;
        this.billNumber = billNumber;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.subtotal = subtotal;
        this.totalAmount = totalAmount;
        this.paymentStatus = PaymentStatus.PENDING;
        this.billStatus = BillStatus.ACTIVE;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();

        if (id == null) {
            id = UUID.randomUUID();
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Shop getShop() {
        return shop;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public BillStatus getBillStatus() {
        return billStatus;
    }

    public long getVersion() {
        return version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateCustomerDetails(
            String customerName,
            String customerPhone
    ) {
        this.customerName = customerName;
        this.customerPhone = customerPhone;
    }

    public void updateAmounts(
            BigDecimal subtotal,
            BigDecimal totalAmount
    ) {
        this.subtotal = subtotal;
        this.totalAmount = totalAmount;
    }

    public void updatePaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public void cancel() {
        this.billStatus = BillStatus.CANCELLED;
    }
}
