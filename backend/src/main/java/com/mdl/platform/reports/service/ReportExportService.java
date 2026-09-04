package com.mdl.platform.reports.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdl.platform.audit.service.AuditRecorder;
import com.mdl.platform.audit.service.AuditService;
import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.authorization.LocationAccessService;
import com.mdl.platform.businesses.repository.BusinessRepository;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.common.exception.NotFoundException;
import com.mdl.platform.inventory.entity.InventoryBalance;
import com.mdl.platform.inventory.repository.InventoryBalanceRepository;
import com.mdl.platform.locations.entity.Location;
import com.mdl.platform.locations.repository.LocationRepository;
import com.mdl.platform.products.entity.Product;
import com.mdl.platform.products.repository.ProductRepository;
import com.mdl.platform.reports.dto.CsvExportResult;
import com.mdl.platform.reports.dto.ReportExportResponse;
import com.mdl.platform.reports.dto.SalesSummaryReport;
import com.mdl.platform.reports.entity.ReportExport;
import com.mdl.platform.reports.repository.ReportExportRepository;
import com.mdl.platform.security.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ReportExportService {

    private static final Logger log = LoggerFactory.getLogger(ReportExportService.class);
    private static final int MAX_INVENTORY_EXPORT_ROWS = 5000;
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AuthorizationService authorizationService;
    private final LocationAccessService locationAccessService;
    private final BusinessRepository businessRepository;
    private final ReportService reportService;
    private final InventoryBalanceRepository balanceRepository;
    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final ReportExportRepository exportRepository;
    private final AuditRecorder auditRecorder;
    private final ObjectMapper objectMapper;

    private final ReportPdfGenerator reportPdfGenerator;

    public ReportExportService(
            AuthorizationService authorizationService,
            LocationAccessService locationAccessService,
            BusinessRepository businessRepository,
            ReportService reportService,
            InventoryBalanceRepository balanceRepository,
            LocationRepository locationRepository,
            ProductRepository productRepository,
            ReportExportRepository exportRepository,
            AuditRecorder auditRecorder,
            ObjectMapper objectMapper,
            ReportPdfGenerator reportPdfGenerator) {
        this.authorizationService = authorizationService;
        this.locationAccessService = locationAccessService;
        this.businessRepository = businessRepository;
        this.reportService = reportService;
        this.balanceRepository = balanceRepository;
        this.locationRepository = locationRepository;
        this.productRepository = productRepository;
        this.exportRepository = exportRepository;
        this.auditRecorder = auditRecorder;
        this.objectMapper = objectMapper;
        this.reportPdfGenerator = reportPdfGenerator;
    }

    @Transactional
    public CsvExportResult exportSalesSummary(Long shopId, Instant from, Instant to) {
        authorizationService.requirePermission("report:export");
        UserContext context = authorizationService.requireAuthenticated();

        SalesSummaryReport report = reportService.salesSummary(shopId, from, to);
        String businessCode = businessRepository.findById(context.businessId())
                .map(b -> b.getCode())
                .orElse("BUSINESS");

        StringBuilder csv = new StringBuilder();
        csv.append("metric,value\n");
        appendMetric(csv, "currency_code", report.currencyCode());
        appendMetric(csv, "from", report.from());
        appendMetric(csv, "to", report.to());
        appendMetric(csv, "shop_id", report.shopId());
        appendMetric(csv, "completed_sales_count", report.completedSalesCount());
        appendMetric(csv, "cancelled_sales_count", report.cancelledSalesCount());
        appendMetric(csv, "refunded_sales_count", report.refundedSalesCount());
        appendMetric(csv, "gross_sales_amount", report.grossSalesAmount());
        appendMetric(csv, "cancelled_sales_amount", report.cancelledSalesAmount());
        appendMetric(csv, "refunded_sales_amount", report.refundedSalesAmount());
        appendMetric(csv, "net_sales_amount", report.netSalesAmount());
        appendMetric(csv, "items_sold", report.itemsSold());

        int rowCount = 11;
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("shopId", shopId);
        parameters.put("from", from);
        parameters.put("to", to);

        return finishExport(
                context,
                "SALES_SUMMARY",
                fileName("sales-summary", businessCode),
                csv.toString(),
                rowCount,
                parameters);
    }

    @Transactional
    public CsvExportResult exportInventoryBalances(Long locationId, boolean lowStockOnly) {
        authorizationService.requirePermission("report:export");
        UserContext context = authorizationService.requireAuthenticated();

        List<Long> locationIds = locationAccessService.getAccessibleLocations(context).stream()
                .map(Location::getId)
                .toList();
        if (locationIds.isEmpty()) {
            throw new NotFoundException("No accessible locations for export");
        }
        if (locationId != null && !locationIds.contains(locationId)) {
            throw new NotFoundException("Location not accessible");
        }

        String businessCode = businessRepository.findById(context.businessId())
                .map(b -> b.getCode())
                .orElse("BUSINESS");

        Page<InventoryBalance> balances = balanceRepository.search(
                context.businessId(),
                locationIds,
                locationId,
                null,
                null,
                lowStockOnly,
                PageRequest.of(0, MAX_INVENTORY_EXPORT_ROWS));

        Map<Long, Location> locations = loadLocations(context.businessId(), balances.getContent());
        Map<Long, Product> products = loadProducts(context.businessId(), balances.getContent());

        StringBuilder csv = new StringBuilder();
        csv.append("location_code,location_name,sku,product_name,unit_of_measure,quantity_on_hand,quantity_reserved,quantity_available,reorder_level\n");

        int rowCount = 0;
        for (InventoryBalance balance : balances.getContent()) {
            Location location = locations.get(balance.getLocationId());
            Product product = products.get(balance.getProductId());
            if (location == null || product == null) {
                continue;
            }

            BigDecimal available = balance.getQuantityOnHand().subtract(balance.getQuantityReserved());
            csv.append(escapeCsv(location.getCode())).append(',');
            csv.append(escapeCsv(location.getName())).append(',');
            csv.append(escapeCsv(product.getSku())).append(',');
            csv.append(escapeCsv(product.getName())).append(',');
            csv.append(escapeCsv(product.getUnitOfMeasure())).append(',');
            csv.append(balance.getQuantityOnHand().toPlainString()).append(',');
            csv.append(balance.getQuantityReserved().toPlainString()).append(',');
            csv.append(available.toPlainString()).append(',');
            csv.append(product.getReorderLevel() != null ? product.getReorderLevel().toString() : "").append('\n');
            rowCount++;
        }

        String reportType = lowStockOnly ? "LOW_STOCK" : "INVENTORY_BALANCES";
        String prefix = lowStockOnly ? "low-stock" : "inventory-balances";
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("locationId", locationId);
        parameters.put("lowStockOnly", lowStockOnly);

        return finishExport(
                context,
                reportType,
                fileName(prefix, businessCode),
                csv.toString(),
                rowCount,
                parameters);
    }

    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
    public CsvExportResult exportSalesSummaryPdf(Long shopId, Instant from, Instant to) {
        authorizationService.requirePermission("report:export");
        UserContext context = authorizationService.requireAuthenticated();

        SalesSummaryReport report = reportService.salesSummary(shopId, from, to);
        var business = businessRepository.findById(context.businessId()).orElseThrow();
        Map<String, String> metrics = new LinkedHashMap<>();
        metrics.put("currency_code", report.currencyCode());
        metrics.put("from", formatValue(report.from()));
        metrics.put("to", formatValue(report.to()));
        metrics.put("shop_id", formatValue(report.shopId()));
        metrics.put("completed_sales_count", formatValue(report.completedSalesCount()));
        metrics.put("cancelled_sales_count", formatValue(report.cancelledSalesCount()));
        metrics.put("refunded_sales_count", formatValue(report.refundedSalesCount()));
        metrics.put("gross_sales_amount", formatValue(report.grossSalesAmount()));
        metrics.put("cancelled_sales_amount", formatValue(report.cancelledSalesAmount()));
        metrics.put("refunded_sales_amount", formatValue(report.refundedSalesAmount()));
        metrics.put("net_sales_amount", formatValue(report.netSalesAmount()));
        metrics.put("items_sold", formatValue(report.itemsSold()));

        byte[] pdf = reportPdfGenerator.salesSummaryPdf(
                business.getName(),
                report.currencyCode(),
                metrics,
                Instant.now());

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("shopId", shopId);
        parameters.put("from", from);
        parameters.put("to", to);

        return finishBinaryExport(
                context,
                "SALES_SUMMARY",
                fileName("sales-summary", business.getCode(), "pdf"),
                pdf,
                "application/pdf",
                metrics.size(),
                parameters);
    }

    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
    public CsvExportResult exportInventoryBalancesPdf(Long locationId, boolean lowStockOnly) {
        authorizationService.requirePermission("report:export");
        UserContext context = authorizationService.requireAuthenticated();

        List<Long> locationIds = locationAccessService.getAccessibleLocations(context).stream()
                .map(Location::getId)
                .toList();
        if (locationIds.isEmpty()) {
            throw new NotFoundException("No accessible locations for export");
        }
        if (locationId != null && !locationIds.contains(locationId)) {
            throw new NotFoundException("Location not accessible");
        }

        var business = businessRepository.findById(context.businessId()).orElseThrow();
        Page<InventoryBalance> balances = balanceRepository.search(
                context.businessId(),
                locationIds,
                locationId,
                null,
                null,
                lowStockOnly,
                PageRequest.of(0, MAX_INVENTORY_EXPORT_ROWS));

        Map<Long, Location> locations = loadLocations(context.businessId(), balances.getContent());
        Map<Long, Product> products = loadProducts(context.businessId(), balances.getContent());

        List<List<String>> rows = balances.getContent().stream()
                .map(balance -> {
                    Location location = locations.get(balance.getLocationId());
                    Product product = products.get(balance.getProductId());
                    if (location == null || product == null) {
                        return null;
                    }
                    BigDecimal available = balance.getQuantityOnHand().subtract(balance.getQuantityReserved());
                    return List.of(
                            location.getCode(),
                            location.getName(),
                            product.getSku(),
                            product.getName(),
                            product.getUnitOfMeasure(),
                            balance.getQuantityOnHand().toPlainString(),
                            balance.getQuantityReserved().toPlainString(),
                            available.toPlainString(),
                            product.getReorderLevel() != null ? String.valueOf(product.getReorderLevel()) : "");
                })
                .filter(row -> row != null)
                .toList();

        String title = lowStockOnly ? "Low Stock Report" : "Inventory Balances Report";
        byte[] pdf = reportPdfGenerator.tabularPdf(
                business.getName(),
                title,
                lowStockOnly ? "Items at or below reorder level" : "On-hand stock by location",
                Instant.now(),
                List.of("Location", "Location name", "SKU", "Product", "UoM", "On hand", "Reserved", "Available", "Reorder"),
                rows);

        String reportType = lowStockOnly ? "LOW_STOCK" : "INVENTORY_BALANCES";
        String prefix = lowStockOnly ? "low-stock" : "inventory-balances";
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("locationId", locationId);
        parameters.put("lowStockOnly", lowStockOnly);

        return finishBinaryExport(
                context,
                reportType,
                fileName(prefix, business.getCode(), "pdf"),
                pdf,
                "application/pdf",
                rows.size(),
                parameters);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportExportResponse> listExports(String reportType, int page, int size) {
        authorizationService.requireAnyPermission("report:view", "report:export");
        UserContext context = authorizationService.requireAuthenticated();

        Page<ReportExport> results = exportRepository.search(
                context.businessId(),
                normalizeFilter(reportType),
                PageRequest.of(Math.max(page, 0), Math.max(size, 1)));

        List<ReportExportResponse> items = results.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(
                items,
                results.getNumber(),
                results.getSize(),
                results.getTotalElements(),
                results.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ReportExportResponse getExport(Long exportId) {
        authorizationService.requirePermission("report:export");
        UserContext context = authorizationService.requireAuthenticated();

        ReportExport export = exportRepository.findByIdAndBusinessId(exportId, context.businessId())
                .orElseThrow(() -> new NotFoundException("Report export not found"));
        return toResponse(export);
    }

    private Map<Long, Location> loadLocations(Long businessId, List<InventoryBalance> balances) {
        List<Long> ids = balances.stream().map(InventoryBalance::getLocationId).distinct().toList();
        return locationRepository.findAllById(ids).stream()
                .filter(location -> location.getBusinessId().equals(businessId))
                .collect(java.util.stream.Collectors.toMap(Location::getId, location -> location));
    }

    private Map<Long, Product> loadProducts(Long businessId, List<InventoryBalance> balances) {
        List<Long> ids = balances.stream().map(InventoryBalance::getProductId).distinct().toList();
        return productRepository.findAllById(ids).stream()
                .filter(product -> product.getBusinessId().equals(businessId))
                .collect(java.util.stream.Collectors.toMap(Product::getId, product -> product));
    }

    private CsvExportResult finishExport(
            UserContext context,
            String reportType,
            String fileName,
            String csvContent,
            int rowCount,
            Map<String, Object> parameters) {

        ReportExport export = new ReportExport();
        export.setBusinessId(context.businessId());
        export.setUserId(context.userId());
        export.setReportType(reportType);
        export.setExportFormat("CSV");
        export.setFileName(fileName);
        export.setRowCount(rowCount);
        export.setParameters(serializeParameters(parameters));
        export.setStatus("COMPLETED");
        exportRepository.save(export);

        auditRecorder.record(context, new AuditService.AuditEvent(
                "REPORT_EXPORTED",
                "REPORTS",
                "REPORT_EXPORT",
                export.getId(),
                fileName,
                "Exported " + reportType + " report (" + rowCount + " rows)",
                Map.of("reportType", reportType, "rowCount", rowCount, "fileName", fileName)));

        byte[] bytes = csvContent.getBytes(StandardCharsets.UTF_8);
        return new CsvExportResult(bytes, fileName, "text/csv; charset=UTF-8", rowCount, export.getId());
    }

    private CsvExportResult finishBinaryExport(
            UserContext context,
            String reportType,
            String fileName,
            byte[] content,
            String contentType,
            int rowCount,
            Map<String, Object> parameters) {

        ReportExport export = new ReportExport();
        export.setBusinessId(context.businessId());
        export.setUserId(context.userId());
        export.setReportType(reportType);
        export.setExportFormat(fileName.endsWith(".pdf") ? "PDF" : "CSV");
        export.setFileName(fileName);
        export.setRowCount(rowCount);
        export.setParameters(serializeParameters(parameters));
        export.setStatus("COMPLETED");

        Long exportId = persistExportRecord(context, export, reportType, fileName, rowCount);
        return new CsvExportResult(content, fileName, contentType, rowCount, exportId);
    }

    private Long persistExportRecord(
            UserContext context,
            ReportExport export,
            String reportType,
            String fileName,
            int rowCount) {
        try {
            exportRepository.save(export);
            auditRecorder.record(context, new AuditService.AuditEvent(
                    "REPORT_EXPORTED",
                    "REPORTS",
                    "REPORT_EXPORT",
                    export.getId(),
                    fileName,
                    "Exported " + reportType + " report (" + rowCount + " rows)",
                    Map.of("reportType", reportType, "rowCount", rowCount, "fileName", fileName)));
            return export.getId();
        } catch (DataIntegrityViolationException ex) {
            log.error(
                    "Could not persist {} export history for {} (export will still be delivered)",
                    export.getExportFormat(),
                    fileName,
                    ex);
            return null;
        }
    }

    private void appendMetric(StringBuilder csv, String metric, Object value) {
        csv.append(escapeCsv(metric)).append(',').append(escapeCsv(formatValue(value))).append('\n');
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Instant instant) {
            return instant.toString();
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        return String.valueOf(value);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String fileName(String prefix, String businessCode) {
        return fileName(prefix, businessCode, "csv");
    }

    private String fileName(String prefix, String businessCode, String extension) {
        String date = LocalDate.now(ZoneOffset.UTC).format(FILE_DATE);
        return prefix + "-" + businessCode + "-" + date + "." + extension;
    }

    private ReportExportResponse toResponse(ReportExport export) {
        return new ReportExportResponse(
                export.getId(),
                export.getReportType(),
                export.getExportFormat(),
                export.getFileName(),
                export.getRowCount(),
                export.getParameters(),
                export.getStatus(),
                export.getUserId(),
                export.getCreatedAt());
    }

    private String serializeParameters(Map<String, Object> parameters) {
        try {
            return objectMapper.writeValueAsString(parameters);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize export parameters", ex);
            return null;
        }
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
