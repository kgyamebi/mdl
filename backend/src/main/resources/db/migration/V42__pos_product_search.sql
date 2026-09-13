-- =============================================================================
-- V42: POS product search — indexes, recents, favorites, search analytics
-- =============================================================================

-- Faster catalog lookups for POS type-ahead (name / brand / status scoped).
CREATE INDEX idx_products_business_status_name ON products (business_id, status, name);
CREATE INDEX idx_products_business_brand ON products (business_id, brand);
CREATE INDEX idx_products_business_category_status ON products (business_id, category_id, status);

-- Per-user recent product selections (POS speed).
CREATE TABLE user_product_recents (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id     BIGINT UNSIGNED NOT NULL,
    user_id         BIGINT UNSIGNED NOT NULL,
    product_id      BIGINT UNSIGNED NOT NULL,
    selected_at     TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_user_product_recents (user_id, product_id),
    INDEX idx_user_product_recents_user_time (user_id, selected_at),
    INDEX idx_user_product_recents_business (business_id),
    CONSTRAINT fk_user_product_recents_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_user_product_recents_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_product_recents_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Per-user favorite products for one-tap POS access.
CREATE TABLE user_product_favorites (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id     BIGINT UNSIGNED NOT NULL,
    user_id         BIGINT UNSIGNED NOT NULL,
    product_id      BIGINT UNSIGNED NOT NULL,
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_user_product_favorites (user_id, product_id),
    INDEX idx_user_product_favorites_user (user_id, created_at),
    INDEX idx_user_product_favorites_business (business_id),
    CONSTRAINT fk_user_product_favorites_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_user_product_favorites_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_product_favorites_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Search / selection latency analytics for POS tuning.
CREATE TABLE product_search_events (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_id         BIGINT UNSIGNED NOT NULL,
    user_id             BIGINT UNSIGNED NULL,
    shop_id             BIGINT UNSIGNED NULL,
    query_text          VARCHAR(255)    NOT NULL,
    result_count        INT             NOT NULL DEFAULT 0,
    latency_ms          INT             NULL,
    selected_product_id BIGINT UNSIGNED NULL,
    selection_latency_ms INT            NULL,
    failed              BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_product_search_events_business_created (business_id, created_at),
    INDEX idx_product_search_events_query (business_id, query_text),
    INDEX idx_product_search_events_selected (selected_product_id),
    CONSTRAINT fk_product_search_events_business FOREIGN KEY (business_id) REFERENCES businesses (id),
    CONSTRAINT fk_product_search_events_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_product_search_events_shop FOREIGN KEY (shop_id) REFERENCES shops (id) ON DELETE SET NULL,
    CONSTRAINT fk_product_search_events_product FOREIGN KEY (selected_product_id) REFERENCES products (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
