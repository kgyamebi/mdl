package com.mdl.platform.products.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "product_search_events")
public class ProductSearchEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "shop_id")
    private Long shopId;

    @Column(name = "query_text", nullable = false, length = 255)
    private String queryText;

    @Column(name = "result_count", nullable = false)
    private int resultCount;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "selected_product_id")
    private Long selectedProductId;

    @Column(name = "selection_latency_ms")
    private Integer selectionLatencyMs;

    @Column(nullable = false)
    private boolean failed;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public int getResultCount() {
        return resultCount;
    }

    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }

    public Long getSelectedProductId() {
        return selectedProductId;
    }

    public void setSelectedProductId(Long selectedProductId) {
        this.selectedProductId = selectedProductId;
    }

    public Integer getSelectionLatencyMs() {
        return selectionLatencyMs;
    }

    public void setSelectionLatencyMs(Integer selectionLatencyMs) {
        this.selectionLatencyMs = selectionLatencyMs;
    }

    public boolean isFailed() {
        return failed;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
