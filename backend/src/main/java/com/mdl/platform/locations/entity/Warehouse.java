package com.mdl.platform.locations.entity;

import com.mdl.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "warehouses")
public class Warehouse extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "warehouse_type", nullable = false, length = 32)
    private String warehouseType;

    @Column(name = "is_restricted", nullable = false)
    private boolean restricted;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    public Long getBusinessId() {
        return businessId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public String getWarehouseType() {
        return warehouseType;
    }

    public boolean isRestricted() {
        return restricted;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setWarehouseType(String warehouseType) {
        this.warehouseType = warehouseType;
    }

    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
