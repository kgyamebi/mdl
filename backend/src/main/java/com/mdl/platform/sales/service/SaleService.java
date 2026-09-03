package com.mdl.platform.sales.service;

import com.mdl.platform.audit.service.AuditService;
import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.authorization.LocationAccessService;
import com.mdl.platform.businesses.repository.BusinessRepository;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.common.exception.ConflictException;
import com.mdl.platform.common.exception.ForbiddenException;
import com.mdl.platform.common.exception.NotFoundException;
import com.mdl.platform.inventory.service.InventoryLedgerService;
import com.mdl.platform.locations.entity.Location;
import com.mdl.platform.locations.entity.Shop;
import com.mdl.platform.locations.entity.Warehouse;
import com.mdl.platform.locations.repository.LocationRepository;
import com.mdl.platform.locations.repository.ShopRepository;
import com.mdl.platform.locations.repository.WarehouseRepository;
import com.mdl.platform.products.entity.Product;
import com.mdl.platform.products.repository.ProductRepository;
import com.mdl.platform.security.UserContext;
import com.mdl.platform.sales.dto.CancelSaleRequest;
import com.mdl.platform.sales.dto.CreateSaleRequest;
import com.mdl.platform.sales.dto.RefundSaleRequest;
import com.mdl.platform.sales.dto.SaleItemResponse;
import com.mdl.platform.sales.dto.SalePaymentResponse;
import com.mdl.platform.sales.dto.SaleResponse;
import com.mdl.platform.sales.entity.Sale;
import com.mdl.platform.sales.entity.SaleItem;
import com.mdl.platform.sales.entity.SalePayment;
import com.mdl.platform.sales.repository.SaleItemRepository;
import com.mdl.platform.sales.repository.SalePaymentRepository;
import com.mdl.platform.sales.repository.SaleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.Year;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SaleService {

    private final AuthorizationService authorizationService;
    private final LocationAccessService locationAccessService;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final SalePaymentRepository salePaymentRepository;
    private final ShopRepository shopRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final BusinessRepository businessRepository;
    private final InventoryLedgerService ledgerService;
    private final AuditService auditService;

    public SaleService(
            AuthorizationService authorizationService,
            LocationAccessService locationAccessService,
            SaleRepository saleRepository,
            SaleItemRepository saleItemRepository,
            SalePaymentRepository salePaymentRepository,
            ShopRepository shopRepository,
            WarehouseRepository warehouseRepository,
            LocationRepository locationRepository,
            ProductRepository productRepository,
            BusinessRepository businessRepository,
            InventoryLedgerService ledgerService,
            AuditService auditService) {
        this.authorizationService = authorizationService;
        this.locationAccessService = locationAccessService;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.salePaymentRepository = salePaymentRepository;
        this.shopRepository = shopRepository;
        this.warehouseRepository = warehouseRepository;
        this.locationRepository = locationRepository;
        this.productRepository = productRepository;
        this.businessRepository = businessRepository;
        this.ledgerService = ledgerService;
        this.auditService = auditService;
    }

    @Transactional
    public SaleResponse createSale(CreateSaleRequest request) {
        authorizationService.requirePermission("sale:create");
        UserContext context = authorizationService.requireAuthenticated();

        ShopContext shopContext = resolveShopContext(context, request.shopId());
        validateUniqueProducts(request.items());

        String currencyCode = businessRepository.findByIdWithCurrency(context.businessId())
                .orElseThrow(() -> new NotFoundException("Business not found"))
                .getCurrencyCode();

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CreateSaleRequest.CreateSaleItemRequest itemRequest : request.items()) {
            Product product = ledgerService.requireTrackableProduct(context.businessId(), itemRequest.productId());
            BigDecimal unitPrice = itemRequest.unitPrice() != null
                    ? itemRequest.unitPrice()
                    : product.getSellingPrice();
            subtotal = subtotal.add(unitPrice.multiply(itemRequest.quantity()));
        }
        subtotal = subtotal.setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal;

        BigDecimal paymentTotal = request.payments().stream()
                .map(CreateSaleRequest.CreateSalePaymentRequest::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
        if (paymentTotal.compareTo(totalAmount) != 0) {
            throw new ConflictException("Payment total must equal sale total");
        }

        Sale sale = new Sale();
        sale.setBusinessId(context.businessId());
        sale.setSaleNumber(generateSaleNumber(context.businessId()));
        sale.setShopId(shopContext.shop().getId());
        sale.setShopLocationId(shopContext.shop().getLocationId());
        sale.setWarehouseLocationId(shopContext.warehouseLocation().getId());
        sale.setCurrencyCode(currencyCode);
        sale.setStatus("COMPLETED");
        sale.setSubtotal(subtotal);
        sale.setTotalAmount(totalAmount);
        sale.setCustomerName(trimToNull(request.customerName()));
        sale.setNotes(trimToNull(request.notes()));
        sale.setSoldBy(context.userId());
        sale = saleRepository.save(sale);

        for (CreateSaleRequest.CreateSaleItemRequest itemRequest : request.items()) {
            Product product = ledgerService.requireTrackableProduct(context.businessId(), itemRequest.productId());
            BigDecimal unitPrice = itemRequest.unitPrice() != null
                    ? itemRequest.unitPrice()
                    : product.getSellingPrice();
            BigDecimal lineTotal = unitPrice.multiply(itemRequest.quantity()).setScale(4, RoundingMode.HALF_UP);

            SaleItem item = new SaleItem();
            item.setBusinessId(context.businessId());
            item.setSaleId(sale.getId());
            item.setProductId(product.getId());
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(unitPrice);
            item.setLineTotal(lineTotal);
            saleItemRepository.save(item);

            ledgerService.applyOnHandChange(
                    context,
                    shopContext.warehouseLocation(),
                    product,
                    itemRequest.quantity().negate(),
                    "SALE",
                    "SALE",
                    sale.getId(),
                    "Sale " + sale.getSaleNumber());
        }

        for (CreateSaleRequest.CreateSalePaymentRequest paymentRequest : request.payments()) {
            SalePayment payment = new SalePayment();
            payment.setBusinessId(context.businessId());
            payment.setSaleId(sale.getId());
            payment.setPaymentMethod(paymentRequest.paymentMethod().trim().toUpperCase(Locale.ROOT));
            payment.setAmount(paymentRequest.amount());
            payment.setReference(trimToNull(paymentRequest.reference()));
            payment.setReceivedBy(context.userId());
            salePaymentRepository.save(payment);
        }

        auditService.record(context, new AuditService.AuditEvent(
                "SALE_CREATED",
                "SALES",
                "SALE",
                sale.getId(),
                sale.getSaleNumber(),
                "Completed sale " + sale.getSaleNumber(),
                Map.of(
                        "shopId", sale.getShopId(),
                        "totalAmount", sale.getTotalAmount(),
                        "currencyCode", sale.getCurrencyCode())));

        return toResponse(context, sale);
    }

    @Transactional(readOnly = true)
    public PageResponse<SaleResponse> listSales(String status, int page, int size) {
        authorizationService.requirePermission("sale:view");
        UserContext context = authorizationService.requireAuthenticated();

        List<Long> locationIds = locationAccessService.getAccessibleLocations(context).stream()
                .map(Location::getId)
                .toList();
        boolean viewAll = locationAccessService.canViewAllLocations(context);

        Page<Sale> result = saleRepository.search(
                context.businessId(),
                locationIds.isEmpty() ? List.of(-1L) : locationIds,
                viewAll,
                normalizeStatus(status),
                PageRequest.of(Math.max(page, 0), Math.max(size, 1)));

        return toPageResponse(context, result);
    }

    @Transactional(readOnly = true)
    public SaleResponse getSale(Long saleId) {
        authorizationService.requirePermission("sale:view");
        UserContext context = authorizationService.requireAuthenticated();

        Sale sale = requireVisibleSale(context, saleId);
        return toResponse(context, sale);
    }

    @Transactional
    public SaleResponse cancelSale(Long saleId, CancelSaleRequest request) {
        authorizationService.requirePermission("sale:cancel");
        UserContext context = authorizationService.requireAuthenticated();

        Sale sale = requireTransferableSale(context, saleId);
        restoreInventory(context, sale, "SALE_CANCEL", request.reason());

        sale.setStatus("CANCELLED");
        sale.setCancelledBy(context.userId());
        sale.setCancelledAt(Instant.now());
        sale.setCancelReason(request.reason().trim());
        saleRepository.save(sale);

        auditService.record(context, new AuditService.AuditEvent(
                "SALE_CANCELLED",
                "SALES",
                "SALE",
                sale.getId(),
                sale.getSaleNumber(),
                "Cancelled sale " + sale.getSaleNumber(),
                Map.of("reason", request.reason().trim())));

        return toResponse(context, sale);
    }

    @Transactional
    public SaleResponse refundSale(Long saleId, RefundSaleRequest request) {
        authorizationService.requirePermission("sale:refund");
        UserContext context = authorizationService.requireAuthenticated();

        Sale sale = requireTransferableSale(context, saleId);
        restoreInventory(context, sale, "SALE_REFUND", request.reason());

        sale.setStatus("REFUNDED");
        sale.setRefundedBy(context.userId());
        sale.setRefundedAt(Instant.now());
        sale.setRefundReason(request.reason().trim());
        saleRepository.save(sale);

        auditService.record(context, new AuditService.AuditEvent(
                "SALE_REFUNDED",
                "SALES",
                "SALE",
                sale.getId(),
                sale.getSaleNumber(),
                "Refunded sale " + sale.getSaleNumber(),
                Map.of("reason", request.reason().trim())));

        return toResponse(context, sale);
    }

    private void restoreInventory(UserContext context, Sale sale, String transactionType, String reason) {
        Location warehouseLocation = locationRepository.findByIdAndBusinessId(
                        sale.getWarehouseLocationId(), context.businessId())
                .orElseThrow(() -> new NotFoundException("Warehouse location not found"));
        locationAccessService.requireLocationAccess(context, warehouseLocation.getId());

        List<SaleItem> items = saleItemRepository.findBySaleIdOrderByIdAsc(sale.getId());
        for (SaleItem item : items) {
            Product product = ledgerService.requireTrackableProduct(context.businessId(), item.getProductId());
            ledgerService.applyOnHandChange(
                    context,
                    warehouseLocation,
                    product,
                    item.getQuantity(),
                    transactionType,
                    "SALE",
                    sale.getId(),
                    reason);
        }
    }

    private Sale requireTransferableSale(UserContext context, Long saleId) {
        Sale sale = requireVisibleSale(context, saleId);
        if (!"COMPLETED".equals(sale.getStatus())) {
            throw new ConflictException("Only completed sales can be cancelled or fully refunded");
        }
        return sale;
    }

    private Sale requireVisibleSale(UserContext context, Long saleId) {
        Sale sale = saleRepository.findByIdAndBusinessId(saleId, context.businessId())
                .orElseThrow(() -> new NotFoundException("Sale not found"));

        if (locationAccessService.canViewAllLocations(context)) {
            return sale;
        }

        Set<Long> accessible = locationAccessService.getAccessibleLocations(context).stream()
                .map(Location::getId)
                .collect(Collectors.toSet());
        if (accessible.contains(sale.getShopLocationId()) || accessible.contains(sale.getWarehouseLocationId())) {
            return sale;
        }
        throw new ForbiddenException("You do not have access to this sale");
    }

    private ShopContext resolveShopContext(UserContext context, Long shopId) {
        Shop shop = shopRepository.findByIdAndBusinessId(shopId, context.businessId())
                .orElseThrow(() -> new NotFoundException("Shop not found"));
        if (!"ACTIVE".equals(shop.getStatus())) {
            throw new ConflictException("Shop is not active");
        }
        if (shop.getWarehouseId() == null) {
            throw new ConflictException("Shop has no linked warehouse for stock deduction");
        }

        locationAccessService.requireLocationAccess(context, shop.getLocationId());

        Warehouse warehouse = warehouseRepository.findByIdAndBusinessId(shop.getWarehouseId(), context.businessId())
                .orElseThrow(() -> new NotFoundException("Shop warehouse not found"));
        Location warehouseLocation = locationRepository.findByIdAndBusinessId(
                        warehouse.getLocationId(), context.businessId())
                .orElseThrow(() -> new NotFoundException("Warehouse location not found"));

        return new ShopContext(shop, warehouseLocation);
    }

    private String generateSaleNumber(Long businessId) {
        String prefix = "SALE-" + Year.now().getValue() + "-";
        long count = saleRepository.countByBusinessIdAndSaleNumberStartingWith(businessId, prefix);
        return prefix + String.format("%04d", count + 1);
    }

    private void validateUniqueProducts(List<CreateSaleRequest.CreateSaleItemRequest> items) {
        Set<Long> productIds = new HashSet<>();
        for (CreateSaleRequest.CreateSaleItemRequest item : items) {
            if (!productIds.add(item.productId())) {
                throw new ConflictException("Duplicate product in sale items: " + item.productId());
            }
        }
    }

    private PageResponse<SaleResponse> toPageResponse(UserContext context, Page<Sale> page) {
        Map<Long, Shop> shops = loadShops(context.businessId(), page.map(Sale::getShopId).toList());
        Map<Long, List<SaleItem>> itemsBySale = saleItemRepository
                .findBySaleIdIn(page.map(Sale::getId).toList()).stream()
                .collect(Collectors.groupingBy(SaleItem::getSaleId));
        Map<Long, List<SalePayment>> paymentsBySale = salePaymentRepository
                .findBySaleIdIn(page.map(Sale::getId).toList()).stream()
                .collect(Collectors.groupingBy(SalePayment::getSaleId));
        Map<Long, Product> products = loadProducts(context.businessId(), itemsBySale.values().stream()
                .flatMap(List::stream)
                .map(SaleItem::getProductId)
                .distinct()
                .toList());

        List<SaleResponse> items = page.getContent().stream()
                .map(sale -> toResponse(
                        sale,
                        shops.get(sale.getShopId()),
                        itemsBySale.getOrDefault(sale.getId(), List.of()),
                        paymentsBySale.getOrDefault(sale.getId(), List.of()),
                        products))
                .toList();

        return new PageResponse<>(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private SaleResponse toResponse(UserContext context, Sale sale) {
        Shop shop = shopRepository.findByIdAndBusinessId(sale.getShopId(), context.businessId()).orElse(null);
        List<SaleItem> items = saleItemRepository.findBySaleIdOrderByIdAsc(sale.getId());
        List<SalePayment> payments = salePaymentRepository.findBySaleIdOrderByIdAsc(sale.getId());
        Map<Long, Product> products = loadProducts(context.businessId(),
                items.stream().map(SaleItem::getProductId).toList());
        return toResponse(sale, shop, items, payments, products);
    }

    private SaleResponse toResponse(
            Sale sale,
            Shop shop,
            List<SaleItem> items,
            List<SalePayment> payments,
            Map<Long, Product> products) {
        List<SaleItemResponse> itemResponses = items.stream()
                .map(item -> toItemResponse(item, products.get(item.getProductId())))
                .toList();
        List<SalePaymentResponse> paymentResponses = payments.stream()
                .map(this::toPaymentResponse)
                .toList();

        return new SaleResponse(
                sale.getId(),
                sale.getSaleNumber(),
                sale.getShopId(),
                shop != null ? shop.getCode() : null,
                shop != null ? shop.getName() : null,
                sale.getShopLocationId(),
                sale.getWarehouseLocationId(),
                sale.getCurrencyCode(),
                sale.getStatus(),
                sale.getSubtotal(),
                sale.getTotalAmount(),
                sale.getReturnedAmount(),
                sale.getCustomerName(),
                sale.getNotes(),
                sale.getSoldBy(),
                sale.getCancelledBy(),
                sale.getCancelledAt(),
                sale.getCancelReason(),
                sale.getRefundedBy(),
                sale.getRefundedAt(),
                sale.getRefundReason(),
                itemResponses,
                paymentResponses,
                sale.getCreatedAt(),
                sale.getUpdatedAt());
    }

    private SaleItemResponse toItemResponse(SaleItem item, Product product) {
        return new SaleItemResponse(
                item.getId(),
                item.getProductId(),
                product != null ? product.getSku() : null,
                product != null ? product.getName() : null,
                product != null ? product.getUnitOfMeasure() : null,
                item.getQuantity(),
                item.getQuantityReturned(),
                item.getUnitPrice(),
                item.getLineTotal());
    }

    private SalePaymentResponse toPaymentResponse(SalePayment payment) {
        return new SalePaymentResponse(
                payment.getId(),
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getReference(),
                payment.getReceivedBy(),
                payment.getCreatedAt());
    }

    private Map<Long, Shop> loadShops(Long businessId, List<Long> shopIds) {
        if (shopIds.isEmpty()) {
            return Map.of();
        }
        return shopRepository.findAllById(shopIds).stream()
                .filter(shop -> shop.getBusinessId().equals(businessId))
                .collect(Collectors.toMap(Shop::getId, Function.identity()));
    }

    private Map<Long, Product> loadProducts(Long businessId, List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productRepository.findAllById(productIds).stream()
                .filter(product -> product.getBusinessId().equals(businessId))
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record ShopContext(Shop shop, Location warehouseLocation) {
    }
}
