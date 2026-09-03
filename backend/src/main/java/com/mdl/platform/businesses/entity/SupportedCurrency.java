package com.mdl.platform.businesses.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * ISO 4217 currency reference — matches {@code supported_currencies} (CHAR(3) code).
 */
@Entity
@Table(name = "supported_currencies")
public class SupportedCurrency {

    @Id
    @Column(name = "code", nullable = false, length = 3, columnDefinition = "CHAR(3)")
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Column(name = "decimal_places", nullable = false)
    private byte decimalPlaces;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected SupportedCurrency() {
    }

    public SupportedCurrency(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    public byte getDecimalPlaces() {
        return decimalPlaces;
    }

    public boolean isActive() {
        return active;
    }
}
