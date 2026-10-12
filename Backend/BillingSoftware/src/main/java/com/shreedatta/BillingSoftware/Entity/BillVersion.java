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

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "bill_versions")
public class BillVersion {

    public enum Action {
        CREATE,
        UPDATE,
        CANCEL
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "bill_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_bill_versions_bill")
    )
    private Bill bill;

    @Column(name = "version_number", nullable = false)
    private long versionNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "changed_by_user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_bill_versions_user")
    )
    private User changedBy;

    @Column(name = "action", nullable = false, length = 30)
    private String action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> snapshot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected BillVersion() {
    }

    public BillVersion(
            Bill bill,
            long versionNumber,
            User changedBy,
            Action action,
            Map<String, Object> snapshot
    ) {
        this.id = UUID.randomUUID();
        this.bill = bill;
        this.versionNumber = versionNumber;
        this.changedBy = changedBy;
        this.action = action.name();
        this.snapshot = snapshot;
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

    public long getVersionNumber() {
        return versionNumber;
    }

    public User getChangedBy() {
        return changedBy;
    }

    public String getAction() {
        return action;
    }

    public Map<String, Object> getSnapshot() {
        return snapshot;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}