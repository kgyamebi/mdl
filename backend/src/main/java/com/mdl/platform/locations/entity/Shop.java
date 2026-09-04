package com.mdl.platform.locations.entity;

import com.mdl.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "shops")
public class Shop extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    public Long getBusinessId() {
        return businessId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
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

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
