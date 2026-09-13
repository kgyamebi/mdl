package com.mdl.platform.products.service;

import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.authorization.LocationAccessService;
import com.mdl.platform.businesses.repository.BusinessRepository;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.common.exception.NotFoundException;
import com.mdl.platform.products.dto.PosProductHit;
import com.mdl.platform.products.dto.PosQuickAccessResponse;
import com.mdl.platform.products.dto.ProductSearchEventRequest;
import com.mdl.platform.products.entity.Product;
import com.mdl.platform.products.entity.ProductSearchEvent;
import com.mdl.platform.products.entity.UserProductFavorite;
import com.mdl.platform.products.entity.UserProductRecent;
import com.mdl.platform.products.repository.ProductRepository;
import com.mdl.platform.products.repository.ProductSearchEventRepository;
import com.mdl.platform.products.repository.UserProductFavoriteRepository;
import com.mdl.platform.products.repository.UserProductRecentRepository;
import com.mdl.platform.security.UserContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
@Service
public class ProductPosSearchService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int RECENT_LIMIT = 12;
    private static final int FREQUENT_LIMIT = 12;
    private static final int FAVORITE_LIMIT = 24;

    private final AuthorizationService authorizationService;
    private final LocationAccessService locationAccessService;
    private final ProductRepository productRepository;
    private final UserProductRecentRepository recentRepository;
    private final UserProductFavoriteRepository favoriteRepository;
    private final ProductSearchEventRepository searchEventRepository;
    private final BusinessRepository businessRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public ProductPosSearchService(
            AuthorizationService authorizationService,
            LocationAccessService locationAccessService,
            ProductRepository productRepository,
            UserProductRecentRepository recentRepository,
            UserProductFavoriteRepository favoriteRepository,
            ProductSearchEventRepository searchEventRepository,
            BusinessRepository businessRepository) {
        this.authorizationService = authorizationService;
        this.locationAccessService = locationAccessService;
        this.productRepository = productRepository;
        this.recentRepository = recentRepository;
        this.favoriteRepository = favoriteRepository;
        this.searchEventRepository = searchEventRepository;
        this.businessRepository = businessRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<PosProductHit> search(
            String search,
            Long categoryId,
            Long locationId,
            int page,
            int size) {
        authorizationService.requirePermission("product:view");
        UserContext context = authorizationService.requireAuthenticated();
        Long scopedLocationId = requireScopedLocationId(context, locationId);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String currencyCode = requireCurrencyCode(context.businessId());
        List<String> tokens = tokenize(search);

        StringBuilder from = new StringBuilder("""
                FROM products p
                LEFT JOIN product_categories c ON c.id = p.category_id
                LEFT JOIN inventory_balances ib
                       ON ib.product_id = p.id
                      AND ib.business_id = p.business_id
                      AND (:hasLocation = 1 AND ib.location_id = :locationId)
                LEFT JOIN user_product_recents upr
                       ON upr.product_id = p.id
                      AND upr.user_id = :userId
                LEFT JOIN user_product_favorites upf
                       ON upf.product_id = p.id
                      AND upf.user_id = :userId
                LEFT JOIN (
                    SELECT si.product_id AS product_id, COUNT(*) AS sale_count
                    FROM sale_items si
                    INNER JOIN sales s ON s.id = si.sale_id
                    WHERE s.business_id = :businessId
                      AND s.status = 'COMPLETED'
                      AND s.created_at >= (CURRENT_TIMESTAMP - INTERVAL 90 DAY)
                    GROUP BY si.product_id
                ) freq ON freq.product_id = p.id
                WHERE p.business_id = :businessId
                  AND p.status = 'ACTIVE'
                  AND (:hasCategory = 0 OR p.category_id = :categoryId)
                """);

        for (int i = 0; i < tokens.size(); i++) {
            from.append("""
                      AND (
                            LOWER(p.name) LIKE :token%1$d
                         OR LOWER(p.sku) LIKE :token%1$d
                         OR LOWER(COALESCE(p.brand, '')) LIKE :token%1$d
                         OR LOWER(COALESCE(c.name, '')) LIKE :token%1$d
                         OR LOWER(COALESCE(c.code, '')) LIKE :token%1$d
                         OR EXISTS (
                                SELECT 1 FROM barcodes b
                                WHERE b.product_id = p.id
                                  AND b.business_id = p.business_id
                                  AND LOWER(b.barcode) LIKE :token%1$d
                            )
                      )
                    """.formatted(i));
        }

        String orderBy = """
                ORDER BY
                  CASE
                    WHEN LOWER(p.name) = :exact OR LOWER(p.sku) = :exact THEN 0
                    WHEN LOWER(p.name) LIKE :starts OR LOWER(p.sku) LIKE :starts THEN 1
                    ELSE 2
                  END ASC,
                  COALESCE(upr.selected_at, TIMESTAMP('1970-01-01')) DESC,
                  COALESCE(freq.sale_count, 0) DESC,
                  p.name ASC
                """;

        String select = """
                SELECT
                  p.id,
                  p.sku,
                  p.name,
                  p.brand,
                  p.category_id,
                  c.name AS category_name,
                  p.unit_of_measure,
                  p.selling_price,
                  p.reorder_level,
                  CASE
                    WHEN :hasLocation = 0 THEN NULL
                    ELSE COALESCE(ib.quantity_on_hand - ib.quantity_reserved, 0)
                  END AS stock_available,
                  CASE WHEN upf.id IS NULL THEN 0 ELSE 1 END AS is_favorite,
                  COALESCE(freq.sale_count, 0) AS sale_count
                """;

        Query countQuery = entityManager.createNativeQuery("SELECT COUNT(*) " + from);
        bindFilterParams(countQuery, context, categoryId, scopedLocationId, tokens);
        Number totalNumber = (Number) countQuery.getSingleResult();
        long total = totalNumber == null ? 0L : totalNumber.longValue();

        Query dataQuery = entityManager.createNativeQuery(select + from + orderBy);
        bindFilterParams(dataQuery, context, categoryId, scopedLocationId, tokens);
        bindRankingParams(dataQuery, search);
        dataQuery.setFirstResult(safePage * safeSize);
        dataQuery.setMaxResults(safeSize);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();
        List<PosProductHit> items = rows.stream()
                .map(row -> toHit(row, currencyCode, scopedLocationId != null))
                .toList();

        int totalPages = safeSize == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return new PageResponse<>(items, safePage, safeSize, total, totalPages);
    }

    @Transactional(readOnly = true)
    public PosQuickAccessResponse quickAccess(Long locationId) {
        authorizationService.requirePermission("product:view");
        UserContext context = authorizationService.requireAuthenticated();
        Long scopedLocationId = requireScopedLocationId(context, locationId);
        String currencyCode = requireCurrencyCode(context.businessId());

        List<Long> recentIds = recentRepository.findTop20ByUserIdOrderBySelectedAtDesc(context.userId()).stream()
                .map(UserProductRecent::getProductId)
                .limit(RECENT_LIMIT)
                .toList();

        List<Long> favoriteIds = favoriteRepository.findByUserIdOrderByCreatedAtDesc(context.userId()).stream()
                .map(UserProductFavorite::getProductId)
                .limit(FAVORITE_LIMIT)
                .toList();

        List<Long> frequentIds = loadFrequentProductIds(context.businessId(), FREQUENT_LIMIT);

        Map<Long, PosProductHit> byId = loadHitsByIds(
                context.businessId(),
                context.userId(),
                currencyCode,
                scopedLocationId,
                mergeUnique(recentIds, frequentIds, favoriteIds));

        return new PosQuickAccessResponse(
                projectOrdered(recentIds, byId),
                projectOrdered(frequentIds, byId),
                projectOrdered(favoriteIds, byId));
    }

    @Transactional
    public void recordSelection(Long productId) {
        authorizationService.requirePermission("sale:create");
        UserContext context = authorizationService.requireAuthenticated();
        requireActiveProduct(context.businessId(), productId);

        UserProductRecent recent = recentRepository.findByUserIdAndProductId(context.userId(), productId)
                .orElseGet(UserProductRecent::new);
        recent.setBusinessId(context.businessId());
        recent.setUserId(context.userId());
        recent.setProductId(productId);
        recent.setSelectedAt(Instant.now());
        recentRepository.save(recent);
        recentRepository.trimOlderThan(context.userId());
    }

    @Transactional
    public boolean toggleFavorite(Long productId) {
        authorizationService.requirePermission("sale:create");
        UserContext context = authorizationService.requireAuthenticated();
        requireActiveProduct(context.businessId(), productId);

        var existing = favoriteRepository.findByUserIdAndProductId(context.userId(), productId);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return false;
        }

        UserProductFavorite favorite = new UserProductFavorite();
        favorite.setBusinessId(context.businessId());
        favorite.setUserId(context.userId());
        favorite.setProductId(productId);
        favoriteRepository.save(favorite);
        return true;
    }

    @Transactional
    public void recordSearchEvent(ProductSearchEventRequest request) {
        authorizationService.requirePermission("sale:create");
        UserContext context = authorizationService.requireAuthenticated();

        Long shopId = null;
        if (request.shopId() != null) {
            shopId = locationAccessService.requireAccessibleShop(context, request.shopId()).getId();
        }
        Long selectedProductId = null;
        if (request.selectedProductId() != null) {
            requireActiveProduct(context.businessId(), request.selectedProductId());
            selectedProductId = request.selectedProductId();
        }

        ProductSearchEvent event = new ProductSearchEvent();
        event.setBusinessId(context.businessId());
        event.setUserId(context.userId());
        event.setShopId(shopId);
        event.setQueryText(request.queryText().trim());
        event.setResultCount(request.resultCount() == null ? 0 : Math.max(request.resultCount(), 0));
        event.setLatencyMs(request.latencyMs());
        event.setSelectedProductId(selectedProductId);
        event.setSelectionLatencyMs(request.selectionLatencyMs());
        event.setFailed(Boolean.TRUE.equals(request.failed())
                || (request.resultCount() != null && request.resultCount() == 0 && request.queryText().trim().length() >= 2));
        searchEventRepository.save(event);
    }

    private void bindFilterParams(
            Query query,
            UserContext context,
            Long categoryId,
            Long locationId,
            List<String> tokens) {
        query.setParameter("businessId", context.businessId());
        query.setParameter("userId", context.userId());
        query.setParameter("hasCategory", categoryId == null ? 0 : 1);
        query.setParameter("categoryId", categoryId == null ? 0L : categoryId);
        query.setParameter("hasLocation", locationId == null ? 0 : 1);
        query.setParameter("locationId", locationId == null ? 0L : locationId);

        for (int i = 0; i < tokens.size(); i++) {
            query.setParameter("token" + i, "%" + tokens.get(i) + "%");
        }
    }

    private void bindRankingParams(Query query, String search) {
        String exact = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        query.setParameter("exact", exact);
        query.setParameter("starts", exact.isEmpty() ? "__no_prefix_match__%" : exact + "%");
    }

    private List<String> tokenize(String search) {
        if (search == null || search.isBlank()) {
            return List.of();
        }
        String[] parts = search.trim().toLowerCase(Locale.ROOT).split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String part : parts) {
            String cleaned = part.replace("%", "").replace("_", "").replace("\\", "");
            if (!cleaned.isBlank() && tokens.size() < 6) {
                tokens.add(cleaned);
            }
        }
        return tokens;
    }

    private PosProductHit toHit(Object[] row, String currencyCode, boolean stockScoped) {
        Long id = ((Number) row[0]).longValue();
        String sku = (String) row[1];
        String name = (String) row[2];
        String brand = (String) row[3];
        Long categoryId = row[4] == null ? null : ((Number) row[4]).longValue();
        String categoryName = (String) row[5];
        String unit = (String) row[6];
        BigDecimal price = toDecimal(row[7]);
        Integer reorder = row[8] == null ? null : ((Number) row[8]).intValue();
        BigDecimal stock = stockScoped ? toDecimal(row[9]) : null;
        boolean favorite = row[10] != null && ((Number) row[10]).intValue() == 1;
        long saleCount = row[11] == null ? 0L : ((Number) row[11]).longValue();

        return new PosProductHit(
                id,
                sku,
                name,
                brand,
                categoryId,
                categoryName,
                unit,
                price,
                currencyCode,
                reorder,
                stock,
                stockState(stock, reorder, stockScoped),
                favorite,
                saleCount);
    }

    private String stockState(BigDecimal stock, Integer reorderLevel, boolean stockScoped) {
        if (!stockScoped || stock == null) {
            return "UNKNOWN";
        }
        if (stock.compareTo(BigDecimal.ZERO) <= 0) {
            return "OUT";
        }
        if (reorderLevel != null && stock.compareTo(BigDecimal.valueOf(reorderLevel)) <= 0) {
            return "LOW";
        }
        return "IN";
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(value.toString());
    }

    @SuppressWarnings("unchecked")
    private List<Long> loadFrequentProductIds(Long businessId, int limit) {
        List<Object> rows = entityManager.createNativeQuery("""
                        SELECT si.product_id
                        FROM sale_items si
                        INNER JOIN sales s ON s.id = si.sale_id
                        INNER JOIN products p ON p.id = si.product_id
                        WHERE s.business_id = :businessId
                          AND s.status = 'COMPLETED'
                          AND p.status = 'ACTIVE'
                          AND s.created_at >= (CURRENT_TIMESTAMP - INTERVAL 90 DAY)
                        GROUP BY si.product_id
                        ORDER BY COUNT(*) DESC, MAX(s.created_at) DESC
                        """)
                .setParameter("businessId", businessId)
                .setMaxResults(limit)
                .getResultList();
        return rows.stream().map(id -> ((Number) id).longValue()).toList();
    }

    private Map<Long, PosProductHit> loadHitsByIds(
            Long businessId,
            Long userId,
            String currencyCode,
            Long locationId,
            List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }

        Query query = entityManager.createNativeQuery("""
                SELECT
                  p.id,
                  p.sku,
                  p.name,
                  p.brand,
                  p.category_id,
                  c.name AS category_name,
                  p.unit_of_measure,
                  p.selling_price,
                  p.reorder_level,
                  CASE
                    WHEN :hasLocation = 0 THEN NULL
                    ELSE COALESCE(ib.quantity_on_hand - ib.quantity_reserved, 0)
                  END AS stock_available,
                  CASE WHEN upf.id IS NULL THEN 0 ELSE 1 END AS is_favorite,
                  COALESCE(freq.sale_count, 0) AS sale_count
                FROM products p
                LEFT JOIN product_categories c ON c.id = p.category_id
                LEFT JOIN inventory_balances ib
                       ON ib.product_id = p.id
                      AND ib.business_id = p.business_id
                      AND (:hasLocation = 1 AND ib.location_id = :locationId)
                LEFT JOIN user_product_favorites upf
                       ON upf.product_id = p.id
                      AND upf.user_id = :userId
                LEFT JOIN (
                    SELECT si.product_id AS product_id, COUNT(*) AS sale_count
                    FROM sale_items si
                    INNER JOIN sales s ON s.id = si.sale_id
                    WHERE s.business_id = :businessId
                      AND s.status = 'COMPLETED'
                      AND s.created_at >= (CURRENT_TIMESTAMP - INTERVAL 90 DAY)
                    GROUP BY si.product_id
                ) freq ON freq.product_id = p.id
                WHERE p.business_id = :businessId
                  AND p.status = 'ACTIVE'
                  AND p.id IN (:productIds)
                """);
        query.setParameter("businessId", businessId);
        query.setParameter("userId", userId);
        query.setParameter("hasLocation", locationId == null ? 0 : 1);
        query.setParameter("locationId", locationId == null ? 0L : locationId);
        query.setParameter("productIds", productIds);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        Map<Long, PosProductHit> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            PosProductHit hit = toHit(row, currencyCode, locationId != null);
            map.put(hit.id(), hit);
        }
        return map;
    }

    private List<Long> mergeUnique(List<Long>... lists) {
        LinkedHashMap<Long, Boolean> seen = new LinkedHashMap<>();
        for (List<Long> list : lists) {
            for (Long id : list) {
                seen.putIfAbsent(id, Boolean.TRUE);
            }
        }
        return new ArrayList<>(seen.keySet());
    }

    private List<PosProductHit> projectOrdered(List<Long> ids, Map<Long, PosProductHit> byId) {
        return ids.stream().map(byId::get).filter(hit -> hit != null).toList();
    }

    private Long requireScopedLocationId(UserContext context, Long locationId) {
        if (locationId == null) {
            return null;
        }
        locationAccessService.requireViewableLocation(context, locationId);
        return locationId;
    }

    private void requireActiveProduct(Long businessId, Long productId) {
        Product product = productRepository.findByIdAndBusinessId(productId, businessId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));
        if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
            throw new NotFoundException("Product not found: " + productId);
        }
    }

    private String requireCurrencyCode(Long businessId) {
        return businessRepository.findByIdWithCurrency(businessId)
                .map(business -> business.getCurrency().getCode())
                .orElseThrow(() -> new NotFoundException("Business not found"));
    }
}
