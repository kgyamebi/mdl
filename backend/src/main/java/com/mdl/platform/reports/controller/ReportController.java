package com.mdl.platform.reports.controller;

import com.mdl.platform.common.dto.ApiResponse;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.reports.dto.BusinessOverviewReport;
import com.mdl.platform.reports.dto.CsvExportResult;
import com.mdl.platform.reports.dto.InventoryValuationReport;
import com.mdl.platform.reports.dto.ReportExportResponse;
import com.mdl.platform.reports.dto.SalesByProductReport;
import com.mdl.platform.reports.dto.SalesSummaryReport;
import com.mdl.platform.reports.dto.TransferActivityReport;
import com.mdl.platform.reports.service.ReportExportService;
import com.mdl.platform.reports.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final ReportExportService reportExportService;

    public ReportController(ReportService reportService, ReportExportService reportExportService) {
        this.reportService = reportService;
        this.reportExportService = reportExportService;
    }

    @GetMapping("/sales-summary")
    public ResponseEntity<ApiResponse<SalesSummaryReport>> salesSummary(
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.salesSummary(shopId, from, to)));
    }

    @GetMapping("/business-overview")
    public ResponseEntity<ApiResponse<BusinessOverviewReport>> businessOverview() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.businessOverview()));
    }

    @GetMapping("/sales-by-product")
    public ResponseEntity<ApiResponse<SalesByProductReport>> salesByProduct(
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.salesByProduct(shopId, from, to)));
    }

    @GetMapping("/inventory-valuation")
    public ResponseEntity<ApiResponse<InventoryValuationReport>> inventoryValuation(
            @RequestParam(required = false) Long locationId) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.inventoryValuation(locationId)));
    }

    @GetMapping("/transfer-activity")
    public ResponseEntity<ApiResponse<TransferActivityReport>> transferActivity(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.transferActivity(from, to)));
    }

    @GetMapping("/sales-summary/export")
    public ResponseEntity<byte[]> exportSalesSummary(
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return toCsvResponse(reportExportService.exportSalesSummary(shopId, from, to));
    }

    @GetMapping("/inventory-balances/export")
    public ResponseEntity<byte[]> exportInventoryBalances(
            @RequestParam(required = false) Long locationId,
            @RequestParam(defaultValue = "false") boolean lowStockOnly) {
        return toCsvResponse(reportExportService.exportInventoryBalances(locationId, lowStockOnly));
    }

    @GetMapping("/low-stock/export")
    public ResponseEntity<byte[]> exportLowStock(
            @RequestParam(required = false) Long locationId) {
        return toCsvResponse(reportExportService.exportInventoryBalances(locationId, true));
    }

    @GetMapping("/sales-summary/export/pdf")
    public ResponseEntity<byte[]> exportSalesSummaryPdf(
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return toFileResponse(reportExportService.exportSalesSummaryPdf(shopId, from, to));
    }

    @GetMapping("/inventory-balances/export/pdf")
    public ResponseEntity<byte[]> exportInventoryBalancesPdf(
            @RequestParam(required = false) Long locationId,
            @RequestParam(defaultValue = "false") boolean lowStockOnly) {
        return toFileResponse(reportExportService.exportInventoryBalancesPdf(locationId, lowStockOnly));
    }

    @GetMapping("/low-stock/export/pdf")
    public ResponseEntity<byte[]> exportLowStockPdf(
            @RequestParam(required = false) Long locationId) {
        return toFileResponse(reportExportService.exportInventoryBalancesPdf(locationId, true));
    }

    @GetMapping("/exports")
    public ResponseEntity<ApiResponse<PageResponse<ReportExportResponse>>> listExports(
            @RequestParam(required = false) String reportType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(reportExportService.listExports(reportType, page, size)));
    }

    @GetMapping("/exports/{id}")
    public ResponseEntity<ApiResponse<ReportExportResponse>> getExport(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(reportExportService.getExport(id)));
    }

    private ResponseEntity<byte[]> toCsvResponse(CsvExportResult result) {
        return toFileResponse(result);
    }

    private ResponseEntity<byte[]> toFileResponse(CsvExportResult result) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.fileName() + "\"")
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(result.content());
    }
}