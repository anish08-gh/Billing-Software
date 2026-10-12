package com.shreedatta.BillingSoftware.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "bill_items")
public class BillItem {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "bill_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_bill_items_bill")
    )
    private Bill bill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "item_id",
            foreignKey = @ForeignKey(name = "fk_bill_items_item")
    )
    private Item item;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "unit", nullable = false, length = 30)
    private String unit;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected BillItem() {
    }

    public BillItem(
            Bill bill,
            Item item,
            String itemName,
            String unit,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal totalAmount,
            Integer lineNumber
    ) {
        this.id = UUID.randomUUID();
        this.bill = bill;
        this.item = item;
        this.itemName = itemName;
        this.unit = unit;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalAmount = totalAmount;
        this.lineNumber = lineNumber;
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public Bill getBill() {
        return bill;
    }

    public Item getItem() {
        return item;
    }

    public String getItemName() {
        return itemName;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void updateDetails(
            Item item,
            String itemName,
            String unit,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal totalAmount,
            Integer lineNumber
    ) {
        this.item = item;
        this.itemName = itemName;
        this.unit = unit;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalAmount = totalAmount;
        this.lineNumber = lineNumber;
    }
}