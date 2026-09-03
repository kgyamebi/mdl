package com.mdl.platform.sales.service;

import com.mdl.platform.audit.service.AuditService;
import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.authorization.LocationAccessService;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.common.exception.ConflictException;
import com.mdl.platform.common.exception.ForbiddenException;
import com.mdl.platform.common.exception.NotFoundException;
import com.mdl.platform.inventory.service.InventoryLedgerService;
import com.mdl.platform.locations.entity.Location;
import com.mdl.platform.locations.repository.LocationRepository;
import com.mdl.platform.products.entity.Product;
import com.mdl.platform.products.repository.ProductRepository;
import com.mdl.platform.security.UserContext;
import com.mdl.platform.sales.dto.CreateSaleReturnRequest;
import com.mdl.platform.sales.dto.SaleReturnItemResponse;
import com.mdl.platform.sales.dto.SaleReturnRefundResponse;
import com.mdl.platform.sales.dto.SaleReturnResponse;
import com.mdl.platform.sales.entity.Sale;
import com.mdl.platform.sales.entity.SaleItem;
import com.mdl.platform.sales.entity.SaleReturn;
import com.mdl.platform.sales.entity.SaleReturnItem;
import com.mdl.platform.sales.entity.SaleReturnRefund;
import com.mdl.platform.sales.repository.SaleItemRepository;
import com.mdl.platform.sales.repository.SaleRepository;
import com.mdl.platform.sales.repository.SaleReturnItemRepository;
import com.mdl.platform.sales.repository.SaleReturnRefundRepository;
import com.mdl.platform.sales.repository.SaleReturnRepository;
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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SaleReturnService {

    private static final Set<String> RETURNABLE_SALE_STATUSES = Set.of("COMPLETED", "PARTIALLY_RETURNED");
    private static final Set<String> RETURN_REASONS = Set.of(
            "DEFECTIVE", "WRONG_ITEM", "CUSTOMER_CHANGED_MIND", "OTHER");
    private static final Set<String> PAYMENT_METHODS = Set.of(
            "CASH", "MOBILE_MONEY", "CARD", "BANK_TRANSFER");

    private final AuthorizationService authorizationService;
    private final LocationAccessService locationAccessService;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final SaleReturnRepository saleReturnRepository;
    private final SaleReturnItemRepository saleReturnItemRepository;
    private final SaleReturnRefundRepository saleReturnRefundRepository;
    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final InventoryLedgerService ledgerService;
    private final AuditService auditService;

    public SaleReturnService(
            AuthorizationService authorizationService,
            LocationAccessService locationAccessService,
            SaleRepository saleRepository,
            SaleItemRepository saleItemRepository,
            SaleReturnRepository saleReturnRepository,
            SaleReturnItemRepository saleReturnItemRepository,
            SaleReturnRefundRepository saleReturnRefundRepository,
            LocationRepository locationRepository,
            ProductRepository productRepository,
            InventoryLedgerService ledgerService,
            AuditService auditService) {
        this.authorizationService = authorizationService;
        this.locationAccessService = locationAccessService;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.saleReturnRepository = saleReturnRepository;
        this.saleReturnItemRepository = saleReturnItemRepository;
        this.saleReturnRefundRepository = saleReturnRefundRepository;
        this.locationRepository = locationRepository;
        this.productRepository = productRepository;
        this.ledgerService = ledgerService;
        this.auditService = auditService;
    }

    @Transactional
    public SaleReturnResponse createReturn(Long saleId, CreateSaleReturnRequest request) {
        authorizationService.requirePermission("sale:return");
        UserContext context = authorizationService.requireAuthenticated();

        Sale sale = requireReturnableSale(context, saleId);
        String reason = normalizeReason(request.reason());
        validateUniqueSaleItems(request.items());

        Map<Long, SaleItem> saleItemsById = saleItemRepository.findBySaleIdOrderByIdAsc(sale.getId()).stream()
                .collect(Collectors.toMap(SaleItem::getId, Function.identity()));

        BigDecimal totalRefund = BigDecimal.ZERO;
        for (CreateSaleReturnRequest.CreateSaleReturnItemRequest itemRequest : request.items()) {
            SaleItem saleItem = saleItemsById.get(itemRequest.saleItemId());
            if (saleItem == null) {
                throw new NotFoundException("Sale item not found on this sale: " + itemRequest.saleItemId());
            }

            BigDecimal remaining = remainingQuantity(saleItem);
            if (itemRequest.quantity().compareTo(remaining) > 0) {
                throw new ConflictException(
                        "Return quantity exceeds remaining quantity for sale item " + saleItem.getId());
            }

            BigDecimal lineRefund = saleItem.getUnitPrice()
                    .multiply(itemRequest.quantity())
                    .setScale(4, RoundingMode.HALF_UP);
            totalRefund = totalRefund.add(lineRefund);
        }
        totalRefund = totalRefund.setScale(4, RoundingMode.HALF_UP);

        BigDecimal refundPaymentTotal = request.refunds().stream()
                .map(CreateSaleReturnRequest.CreateSaleReturnRefundRequest::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
        if (refundPaymentTotal.compareTo(totalRefund) != 0) {
            throw new ConflictException("Refund payment total must match return amount");
        }

        Location warehouseLocation = locationRepository.findByIdAndBusinessId(
                        sale.getWarehouseLocationId(), context.businessId())
                .orElseThrow(() -> new NotFoundException("Warehouse location not found"));
        locationAccessService.requireLocationAccess(context, warehouseLocation.getId());

        SaleReturn saleReturn = new SaleReturn();
        saleReturn.setBusinessId(context.businessId());
        saleReturn.setReturnNumber(generateReturnNumber(context.businessId()));
        saleReturn.setSaleId(sale.getId());
        saleReturn.setShopId(sale.getShopId());
        saleReturn.setWarehouseLocationId(sale.getWarehouseLocationId());
        saleReturn.setCurrencyCode(sale.getCurrencyCode());
        saleReturn.setStatus("COMPLETED");
        saleReturn.setTotalRefundAmount(totalRefund);
        saleReturn.setReason(reason);
        saleReturn.setNotes(trimToNull(request.notes()));
        saleReturn.setProcessedBy(context.userId());
        saleReturnRepository.save(saleReturn);

        for (CreateSaleReturnRequest.CreateSaleReturnItemRequest itemRequest : request.items()) {
            SaleItem saleItem = saleItemsById.get(itemRequest.saleItemId());
            Product product = ledgerService.requireTrackableProduct(context.businessId(), saleItem.getProductId());
            BigDecimal lineRefund = saleItem.getUnitPrice()
                    .multiply(itemRequest.quantity())
                    .setScale(4, RoundingMode.HALF_UP);

            SaleReturnItem returnItem = new SaleReturnItem();
            returnItem.setBusinessId(context.businessId());
            returnItem.setSaleReturnId(saleReturn.getId());
            returnItem.setSaleItemId(saleItem.getId());
            returnItem.setProductId(saleItem.getProductId());
            returnItem.setQuantity(itemRequest.quantity());
            returnItem.setUnitPrice(saleItem.getUnitPrice());
            returnItem.setLineRefund(lineRefund);
            saleReturnItemRepository.save(returnItem);

            saleItem.setQuantityReturned(
                    saleItem.getQuantityReturned().add(itemRequest.quantity()));
            saleItemRepository.save(saleItem);

            ledgerService.applyOnHandChange(
                    context,
                    warehouseLocation,
                    product,
                    itemRequest.quantity(),
                    "RETURN",
                    "SALE_RETURN",
                    saleReturn.getId(),
                    reason);
        }

        for (CreateSaleReturnRequest.CreateSaleReturnRefundRequest refundRequest : request.refunds()) {
            validatePaymentMethod(refundRequest.paymentMethod());

            SaleReturnRefund refund = new SaleReturnRefund();
            refund.setBusinessId(context.businessId());
            refund.setSaleReturnId(saleReturn.getId());
            refund.setPaymentMethod(refundRequest.paymentMethod().trim().toUpperCase(Locale.ROOT));
            refund.setAmount(refundRequest.amount());
            refund.setReference(trimToNull(refundRequest.reference()));
            refund.setProcessedBy(context.userId());
            saleReturnRefundRepository.save(refund);
        }

        sale.setReturnedAmount(sale.getReturnedAmount().add(totalRefund));
        updateSaleStatusAfterReturn(sale, saleItemsById.values());
        saleRepository.save(sale);

        auditService.record(context, new AuditService.AuditEvent(
                "SALE_RETURN_CREATED",
                "SALES",
                "SALE_RETURN",
                saleReturn.getId(),
                saleReturn.getReturnNumber(),
                "Processed return " + saleReturn.getReturnNumber() + " for sale " + sale.getSaleNumber(),
                Map.of(
                        "saleId", sale.getId(),
                        "saleNumber", sale.getSaleNumber(),
                        "reason", reason,
                        "totalRefundAmount", totalRefund)));

        return toResponse(sale, saleReturn);
    }

    @Transactional(readOnly = true)
    public List<SaleReturnResponse> listReturnsForSale(Long saleId) {
        authorizationService.requirePermission("sale:view");
        UserContext context = authorizationService.requireAuthenticated();

        Sale sale = requireVisibleSale(context, saleId);
        List<SaleReturn> returns = saleReturnRepository.findBySaleIdAndBusinessIdOrderByCreatedAtDesc(
                sale.getId(), context.businessId());

        return returns.stream()
                .map(saleReturn -> toResponse(sale, saleReturn))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<SaleReturnResponse> listReturns(int page, int size) {
        authorizationService.requirePermission("sale:view");
        UserContext context = authorizationService.requireAuthenticated();

        List<Long> locationIds = locationAccessService.getAccessibleLocations(context).stream()
                .map(Location::getId)
                .toList();
        boolean viewAll = locationAccessService.canViewAllLocations(context);

        Page<SaleReturn> result = saleReturnRepository.search(
                context.businessId(),
                locationIds.isEmpty() ? List.of(-1L) : locationIds,
                viewAll,
                PageRequest.of(Math.max(page, 0), Math.max(size, 1)));

        Map<Long, Sale> sales = saleRepository.findAllById(result.map(SaleReturn::getSaleId).toList()).stream()
                .filter(sale -> sale.getBusinessId().equals(context.businessId()))
                .collect(Collectors.toMap(Sale::getId, Function.identity()));

        List<SaleReturnResponse> items = result.getContent().stream()
                .map(saleReturn -> toResponse(sales.get(saleReturn.getSaleId()), saleReturn))
                .toList();

        return new PageResponse<>(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public SaleReturnResponse getReturn(Long returnId) {
        authorizationService.requirePermission("sale:view");
        UserContext context = authorizationService.requireAuthenticated();

        SaleReturn saleReturn = saleReturnRepository.findByIdAndBusinessId(returnId, context.businessId())
                .orElseThrow(() -> new NotFoundException("Sale return not found"));

        Sale sale = requireVisibleSale(context, saleReturn.getSaleId());
        return toResponse(sale, saleReturn);
    }

    private void updateSaleStatusAfterReturn(Sale sale, Iterable<SaleItem> saleItems) {
        boolean fullyReturned = true;
        for (SaleItem item : saleItems) {
            if (remainingQuantity(item).compareTo(BigDecimal.ZERO) > 0) {
                fullyReturned = false;
                break;
            }
        }

        if (fullyReturned) {
            sale.setStatus("REFUNDED");
            sale.setRefundedAt(Instant.now());
            if (sale.getRefundReason() == null) {
                sale.setRefundReason("Fully returned via customer return");
            }
        } else {
            sale.setStatus("PARTIALLY_RETURNED");
        }
    }

    private BigDecimal remainingQuantity(SaleItem saleItem) {
        return saleItem.getQuantity().subtract(saleItem.getQuantityReturned());
    }

    private Sale requireReturnableSale(UserContext context, Long saleId) {
        Sale sale = requireVisibleSale(context, saleId);
        if (!RETURNABLE_SALE_STATUSES.contains(sale.getStatus())) {
            throw new ConflictException("Sale cannot accept returns in status: " + sale.getStatus());
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

    private SaleReturnResponse toResponse(Sale sale, SaleReturn saleReturn) {
        List<SaleReturnItem> items = saleReturnItemRepository.findBySaleReturnIdOrderByIdAsc(saleReturn.getId());
        List<SaleReturnRefund> refunds = saleReturnRefundRepository.findBySaleReturnIdOrderByIdAsc(saleReturn.getId());
        Map<Long, Product> products = productRepository.findAllById(items.stream()
                        .map(SaleReturnItem::getProductId)
                        .toList()).stream()
                .filter(product -> product.getBusinessId().equals(saleReturn.getBusinessId()))
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        return new SaleReturnResponse(
                saleReturn.getId(),
                saleReturn.getReturnNumber(),
                saleReturn.getSaleId(),
                sale != null ? sale.getSaleNumber() : null,
                saleReturn.getShopId(),
                saleReturn.getCurrencyCode(),
                saleReturn.getStatus(),
                saleReturn.getTotalRefundAmount(),
                saleReturn.getReason(),
                saleReturn.getNotes(),
                saleReturn.getProcessedBy(),
                items.stream()
                        .map(item -> new SaleReturnItemResponse(
                                item.getId(),
                                item.getSaleItemId(),
                                item.getProductId(),
                                products.containsKey(item.getProductId())
                                        ? products.get(item.getProductId()).getSku()
                                        : null,
                                products.containsKey(item.getProductId())
                                        ? products.get(item.getProductId()).getName()
                                        : null,
                                item.getQuantity(),
                                item.getUnitPrice(),
                                item.getLineRefund()))
                        .toList(),
                refunds.stream()
                        .map(refund -> new SaleReturnRefundResponse(
                                refund.getId(),
                                refund.getPaymentMethod(),
                                refund.getAmount(),
                                refund.getReference(),
                                refund.getProcessedBy(),
                                refund.getCreatedAt()))
                        .toList(),
                saleReturn.getCreatedAt());
    }

    private String generateReturnNumber(Long businessId) {
        String prefix = "RET-" + Year.now().getValue() + "-";
        long count = saleReturnRepository.countByBusinessIdAndReturnNumberStartingWith(businessId, prefix);
        return prefix + String.format("%04d", count + 1);
    }

    private void validateUniqueSaleItems(List<CreateSaleReturnRequest.CreateSaleReturnItemRequest> items) {
        Set<Long> saleItemIds = new HashSet<>();
        for (CreateSaleReturnRequest.CreateSaleReturnItemRequest item : items) {
            if (!saleItemIds.add(item.saleItemId())) {
                throw new ConflictException("Duplicate sale item in return: " + item.saleItemId());
            }
        }
    }

    private void validatePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || !PAYMENT_METHODS.contains(paymentMethod.trim().toUpperCase(Locale.ROOT))) {
            throw new ConflictException("Invalid payment method: " + paymentMethod);
        }
    }

    private String normalizeReason(String reason) {
        String normalized = reason.trim().toUpperCase(Locale.ROOT);
        if (!RETURN_REASONS.contains(normalized)) {
            throw new ConflictException("Invalid return reason: " + reason);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
