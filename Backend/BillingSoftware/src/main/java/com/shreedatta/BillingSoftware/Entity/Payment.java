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
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    public enum PaymentMethod {
        CASH,
        UPI,
        CARD,
        BANK_TRANSFER,
        CHEQUE,
        OTHER
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "bill_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_payments_bill")
    )
    private Bill bill;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "paid_at", nullable = false)
    private OffsetDateTime paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Payment() {
    }

    public Payment(
            Bill bill,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            String paymentReference,
            String notes,
            OffsetDateTime paidAt
    ) {
        this.id = UUID.randomUUID();
        this.bill = bill;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentReference = paymentReference;
        this.notes = notes;
        this.paidAt = paidAt;
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        OffsetDateTime now = OffsetDateTime.now();

        if (paidAt == null) {
            paidAt = now;
        }

        if (createdAt == null) {
            createdAt = now;
        }
    }

    public UUID getId() {
        return id;
    }

    public Bill getBill() {
        return bill;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public String getNotes() {
        return notes;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}

