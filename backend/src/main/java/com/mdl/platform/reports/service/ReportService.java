package com.mdl.platform.reports.service;

import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.authorization.repository.TemporaryPermissionRepository;
import com.mdl.platform.businesses.repository.BusinessRepository;
import com.mdl.platform.inventory.repository.InventoryBalanceRepository;
import com.mdl.platform.locations.entity.Location;
import com.mdl.platform.authorization.LocationAccessService;
import com.mdl.platform.common.exception.NotFoundException;
import com.mdl.platform.reports.dto.BusinessOverviewReport;
import com.mdl.platform.reports.dto.InventoryValuationReport;
import com.mdl.platform.reports.dto.SalesByProductReport;
import com.mdl.platform.reports.dto.SalesSummaryReport;
import com.mdl.platform.reports.dto.TransferActivityReport;
import com.mdl.platform.reports.repository.ExtendedReportRepository;
import com.mdl.platform.reports.repository.SalesReportRepository;
import com.mdl.platform.security.UserContext;
import com.mdl.platform.transfers.repository.StockTransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final AuthorizationService authorizationService;
    private final LocationAccessService locationAccessService;
    private final BusinessRepository businessRepository;
    private final SalesReportRepository salesReportRepository;
    private final ExtendedReportRepository extendedReportRepository;
    private final InventoryBalanceRepository balanceRepository;
    private final StockTransferRepository stockTransferRepository;
    private final TemporaryPermissionRepository temporaryPermissionRepository;

    public ReportService(
            AuthorizationService authorizationService,
            LocationAccessService locationAccessService,
            BusinessRepository businessRepository,
            SalesReportRepository salesReportRepository,
            ExtendedReportRepository extendedReportRepository,
            InventoryBalanceRepository balanceRepository,
            StockTransferRepository stockTransferRepository,
            TemporaryPermissionRepository temporaryPermissionRepository) {
        this.authorizationService = authorizationService;
        this.locationAccessService = locationAccessService;
        this.businessRepository = businessRepository;
        this.salesReportRepository = salesReportRepository;
        this.extendedReportRepository = extendedReportRepository;
        this.balanceRepository = balanceRepository;
        this.stockTransferRepository = stockTransferRepository;
        this.temporaryPermissionRepository = temporaryPermissionRepository;
    }

    public SalesSummaryReport salesSummary(Long shopId, Instant from, Instant to) {
        authorizationService.requirePermission("report:view");
        UserContext context = authorizationService.requireAuthenticated();

        String currencyCode = businessRepository.findByIdWithCurrency(context.businessId())
                .orElseThrow(() -> new NotFoundException("Business not found"))
                .getCurrencyCode();

        long completed = salesReportRepository.countByStatus(
                context.businessId(), "COMPLETED", shopId, from, to);
        long cancelled = salesReportRepository.countByStatus(
                context.businessId(), "CANCELLED", shopId, from, to);
        long refunded = salesReportRepository.countByStatus(
                context.businessId(), "REFUNDED", shopId, from, to);

        BigDecimal gross = salesReportRepository.sumTotalAmountByStatus(
                context.businessId(), "COMPLETED", shopId, from, to);
        BigDecimal cancelledAmount = salesReportRepository.sumTotalAmountByStatus(
                context.businessId(), "CANCELLED", shopId, from, to);
        BigDecimal refundedAmount = salesReportRepository.sumTotalAmountByStatus(
                context.businessId(), "REFUNDED", shopId, from, to);
        BigDecimal itemsSold = salesReportRepository.sumCompletedItemsSold(
                context.businessId(), shopId, from, to);

        return new SalesSummaryReport(
                currencyCode,
                from,
                to,
                shopId,
                completed,
                cancelled,
                refunded,
                gross,
                cancelledAmount,
                refundedAmount,
                gross.subtract(refundedAmount),
                itemsSold);
    }

    public BusinessOverviewReport businessOverview() {
        authorizationService.requirePermission("report:view");
        UserContext context = authorizationService.requireAuthenticated();

        String currencyCode = businessRepository.findByIdWithCurrency(context.businessId())
                .orElseThrow(() -> new NotFoundException("Business not found"))
                .getCurrencyCode();

        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfDay = LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusMillis(1);

        long salesToday = salesReportRepository.countByStatus(
                context.businessId(), "COMPLETED", null, startOfDay, endOfDay);
        BigDecimal salesAmountToday = salesReportRepository.sumTotalAmountByStatus(
                context.businessId(), "COMPLETED", null, startOfDay, endOfDay);

        List<Long> locationIds = locationAccessService.getAccessibleLocations(context).stream()
                .map(Location::getId)
                .toList();
        long lowStock = locationIds.isEmpty()
                ? 0
                : balanceRepository.countLowStock(context.businessId(), locationIds);

        long pendingTransfers = stockTransferRepository.search(
                context.businessId(),
                locationIds.isEmpty() ? List.of(-1L) : locationIds,
                locationAccessService.canViewAllLocations(context),
                "REQUESTED",
                org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();

        long activeTempPermissions = temporaryPermissionRepository.search(
                context.businessId(),
                null,
                "ACTIVE",
                org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();

        return new BusinessOverviewReport(
                currencyCode,
                salesToday,
                salesAmountToday,
                lowStock,
                pendingTransfers,
                activeTempPermissions);
    }

    public SalesByProductReport salesByProduct(Long shopId, Instant from, Instant to) {
        authorizationService.requirePermission("report:view");
        UserContext context = authorizationService.requireAuthenticated();

        String currencyCode = businessRepository.findByIdWithCurrency(context.businessId())
                .orElseThrow(() -> new NotFoundException("Business not found"))
                .getCurrencyCode();

        List<SalesByProductReport.SalesByProductRow> items = extendedReportRepository
                .salesByProduct(context.businessId(), shopId, from, to)
                .stream()
                .map(row -> new SalesByProductReport.SalesByProductRow(
                        (Long) row[0],
                        (String) row[1],
                        (String) row[2],
                        (BigDecimal) row[3],
                        (BigDecimal) row[4]))
                .toList();

        return new SalesByProductReport(currencyCode, from, to, shopId, items);
    }

    public InventoryValuationReport inventoryValuation(Long locationId) {
        authorizationService.requirePermission("report:view");
        UserContext context = authorizationService.requireAuthenticated();

        String currencyCode = businessRepository.findByIdWithCurrency(context.businessId())
                .orElseThrow(() -> new NotFoundException("Business not found"))
                .getCurrencyCode();

        List<Long> locationIds = locationAccessService.getAccessibleLocations(context).stream()
                .map(Location::getId)
                .toList();
        if (locationIds.isEmpty()) {
            return emptyValuation(currencyCode);
        }
        if (locationId != null) {
            if (!locationIds.contains(locationId)) {
                throw new NotFoundException("Location not accessible");
            }
            locationIds = List.of(locationId);
        }

        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalRetail = BigDecimal.ZERO;
        List<InventoryValuationReport.InventoryValuationRow> items = new ArrayList<>();

        for (Object[] row : extendedReportRepository.inventoryValuationRows(context.businessId(), locationIds)) {
            BigDecimal onHand = (BigDecimal) row[6];
            BigDecimal costPrice = row[7] != null ? (BigDecimal) row[7] : BigDecimal.ZERO;
            BigDecimal sellingPrice = (BigDecimal) row[8];
            BigDecimal costValue = onHand.multiply(costPrice);
            BigDecimal retailValue = onHand.multiply(sellingPrice);
            totalCost = totalCost.add(costValue);
            totalRetail = totalRetail.add(retailValue);
            items.add(new InventoryValuationReport.InventoryValuationRow(
                    (Long) row[0],
                    (String) row[1],
                    (String) row[2],
                    (Long) row[3],
                    (String) row[4],
                    (String) row[5],
                    onHand,
                    costPrice,
                    sellingPrice,
                    costValue,
                    retailValue));
        }

        return new InventoryValuationReport(currencyCode, totalCost, totalRetail, items.size(), items);
    }

    public TransferActivityReport transferActivity(Instant from, Instant to) {
        authorizationService.requirePermission("report:view");
        UserContext context = authorizationService.requireAuthenticated();

        boolean viewAll = locationAccessService.canViewAllLocations(context);
        List<Long> scopedLocationIds = locationAccessService.getAccessibleLocations(context).stream()
                .map(Location::getId)
                .toList();
        List<Long> queryLocationIds = scopedLocationIds.isEmpty() ? List.of(-1L) : scopedLocationIds;

        long total = extendedReportRepository.countTransfers(
                context.businessId(), queryLocationIds, viewAll, from, to);

        List<TransferActivityReport.TransferStatusCount> statusCounts = extendedReportRepository
                .transferStatusCounts(context.businessId(), queryLocationIds, viewAll, from, to)
                .stream()
                .map(row -> new TransferActivityReport.TransferStatusCount(
                        (String) row[0],
                        (Long) row[1]))
                .toList();

        return new TransferActivityReport(from, to, total, statusCounts);
    }

    private InventoryValuationReport emptyValuation(String currencyCode) {
        return new InventoryValuationReport(
                currencyCode,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                List.of());
    }
}
