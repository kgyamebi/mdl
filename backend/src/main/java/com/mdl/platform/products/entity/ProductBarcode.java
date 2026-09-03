package com.mdl.platform.products.entity;

import com.mdl.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "barcodes")
public class ProductBarcode extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false, length = 64)
    private String barcode;

    @Column(name = "barcode_type", nullable = false, length = 32)
    private String barcodeType = "EAN13";

    @Column(name = "is_primary", nullable = false)
    private boolean primaryBarcode;

    public Long getBusinessId() {
        return businessId;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getBarcodeType() {
        return barcodeType;
    }

    public void setBarcodeType(String barcodeType) {
        this.barcodeType = barcodeType;
    }

    public boolean isPrimaryBarcode() {
        return primaryBarcode;
    }

    public void setPrimaryBarcode(boolean primaryBarcode) {
        this.primaryBarcode = primaryBarcode;
    }
}
