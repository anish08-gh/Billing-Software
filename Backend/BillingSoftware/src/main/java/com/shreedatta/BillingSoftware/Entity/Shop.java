package com.shreedatta.BillingSoftware.Entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "shops")
public class Shop {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;


    @Column(name = "bill_sequence", nullable = false)
    private long billSequence;

    protected Shop() {
        // Required by JPA
    }

    public Shop(
            String name,
            String phone,
            String email,
            String address,
            String logoUrl
    ) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.logoUrl = logoUrl;
        this.active = true;
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

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }


    public long getBillSequence() {
        return billSequence;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateDetails(
            String name,
            String phone,
            String email,
            String address,
            String logoUrl
    ) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.logoUrl = logoUrl;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }


    public long incrementBillSequence() {
        this.billSequence++;
        return this.billSequence;
    }
}