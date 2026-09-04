package com.mdl.platform.copilot.service;

import com.mdl.platform.approvals.dto.ApprovalInboxResponse;
import com.mdl.platform.approvals.service.ApprovalInboxService;
import com.mdl.platform.authorization.LocationAccessService;
import com.mdl.platform.businesses.repository.BusinessRepository;
import com.mdl.platform.common.exception.NotFoundException;
import com.mdl.platform.imports.entity.ImportOrder;
import com.mdl.platform.imports.repository.ImportOrderRepository;
import com.mdl.platform.inventory.entity.InventoryBalance;
import com.mdl.platform.inventory.repository.InventoryBalanceRepository;
import com.mdl.platform.locations.entity.Location;
import com.mdl.platform.notifications.repository.NotificationRepository;
import com.mdl.platform.products.entity.Product;
import com.mdl.platform.products.repository.ProductRepository;
import com.mdl.platform.reports.repository.ExtendedReportRepository;
import com.mdl.platform.reports.repository.SalesReportRepository;
import com.mdl.platform.security.UserContext;
import com.mdl.platform.transfers.entity.StockTransfer;
import com.mdl.platform.transfers.repository.StockTransferRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class CopilotDataService {

    private static final int MAX_LIST_ITEMS = 8;

    private final LocationAccessService locationAccessService;
    private final BusinessRepository businessRepository;
    private final InventoryBalanceRepository balanceRepository;
    private final ProductRepository productRepository;
    private final StockTransferRepository stockTransferRepository;
    private final ImportOrderRepository importOrderRepository;
    private final NotificationRepository notificationRepository;
    private final SalesReportRepository salesReportRepository;
    private final ExtendedReportRepository extendedReportRepository;
    private final ApprovalInboxService approvalInboxService;

    public CopilotDataService(
            LocationAccessService locationAccessService,
            BusinessRepository businessRepository,
            InventoryBalanceRepository balanceRepository,
            ProductRepository productRepository,
            StockTransferRepository stockTransferRepository,
            ImportOrderRepository importOrderRepository,
            NotificationRepository notificationRepository,
            SalesReportRepository salesReportRepository,
            ExtendedReportRepository extendedReportRepository,
            ApprovalInboxService approvalInboxService) {
        this.locationAccessService = locationAccessService;
        this.businessRepository = businessRepository;
        this.balanceRepository = balanceRepository;
        this.productRepository = productRepository;
        this.stockTransferRepository = stockTransferRepository;
        this.importOrderRepository = importOrderRepository;
        this.notificationRepository = notificationRepository;
        this.salesReportRepository = salesReportRepository;
        this.extendedReportRepository = extendedReportRepository;
        this.approvalInboxService = approvalInboxService;
    }

    public String answer(UserContext context, String message) {
        String normalized = message.toLowerCase(Locale.ROOT).trim();

        if (matchesAny(normalized, "low stock", "low on stock", "running low", "replenishment", "reorder")) {
            return lowStockAnswer(context);
        }
        if (matchesAny(normalized, "transfer") && matchesAny(normalized, "approval", "awaiting", "pending", "waiting")) {
            return pendingTransfersAnswer(context);
        }
        if (matchesAny(normalized, "import") && matchesAny(normalized, "pending", "approval", "awaiting")) {
            return pendingImportsAnswer(context);
        }
        if (matchesAny(normalized, "inventory", "stock") && containsLocationHint(normalized)) {
            return locationInventoryAnswer(context, normalized);
        }
        if (matchesAny(normalized, "today") && matchesAny(normalized, "sales", "sold", "revenue")) {
            return todaysSalesAnswer(context);
        }
        if (matchesAny(normalized, "selling fastest", "top product", "best seller", "fastest")) {
            return topProductsAnswer(context);
        }
        if (matchesAny(normalized, "stock summary", "inventory summary", "generate a stock")) {
            return stockSummaryAnswer(context);
        }
        if (matchesAny(normalized, "pending task", "summarize", "summary of pending", "what needs attention")) {
            return pendingTasksAnswer(context);
        }
        if (matchesAny(normalized, "approval", "approve")) {
            return approvalsAnswer(context);
        }
        if (matchesAny(normalized, "notification", "inbox", "unread")) {
            return notificationsAnswer(context);
        }
        if (matchesAny(normalized, "product", "catalog", "sku")) {
            return productsAnswer(context, normalized);
        }
        if (matchesAny(normalized, "performance", "overview", "business")) {
            return businessPerformanceAnswer(context);
        }

        return defaultHelp(context);
    }

    public String buildGroundedContext(UserContext context, String message) {
        return "User roles: " + String.join(", ", context.roles())
                + "\nUser permissions: " + String.join(", ", context.permissions())
                + "\nQuestion: " + message
                + "\n\n" + answer(context, message);
    }

    private String lowStockAnswer(UserContext context) {
        if (!hasPermission(context, "inventory:view")) {
            return deny("inventory balances");
        }

        List<Long> locationIds = accessibleLocationIds(context);
        if (locationIds.isEmpty()) {
            return "You do not have access to any locations. Contact your manager for location assignments.";
        }

        var page = balanceRepository.search(
                context.businessId(),
                locationIds,
                null,
                null,
                null,
                true,
                PageRequest.of(0, MAX_LIST_ITEMS));

        if (page.isEmpty()) {
            return "Good news — no low-stock items in your accessible locations right now.";
        }

        StringBuilder reply = new StringBuilder("Low-stock items in your locations:\n");
        for (InventoryBalance balance : page.getContent()) {
            Product product = productRepository.findById(balance.getProductId()).orElse(null);
            Location location = findLocation(context, balance.getLocationId());
            String productName = product != null ? product.getName() : "Product #" + balance.getProductId();
            String locationName = location != null ? location.getName() : "Location #" + balance.getLocationId();
            Integer reorderLevel = product != null ? product.getReorderLevel() : null;
            reply.append("- ")
                    .append(productName)
                    .append(" at ")
                    .append(locationName)
                    .append(": ")
                    .append(formatQty(availableQty(balance)))
                    .append(" available (reorder ")
                    .append(reorderLevel != null ? reorderLevel : "n/a")
                    .append(")\n");
        }
        if (page.getTotalElements() > MAX_LIST_ITEMS) {
            reply.append("…and ")
                    .append(page.getTotalElements() - MAX_LIST_ITEMS)
                    .append(" more. Check Inventory for the full list.");
        } else {
            reply.append("Action: review replenishment or create transfer/import requests.");
        }
        return reply.toString().trim();
    }

    private String pendingTransfersAnswer(UserContext context) {
        if (!hasPermission(context, "transfer:view")) {
            return deny("transfers");
        }
        if (!hasPermission(context, "approval:view")) {
            return "Transfers awaiting approval require approval access. You can view transfers you have access to in the Transfers module.";
        }

        var inbox = approvalInboxService.getInbox("STOCK_TRANSFER", 0, MAX_LIST_ITEMS);
        long count = inbox.summary().transferCount();
        if (count == 0) {
            return "No stock transfers are awaiting approval in your scope.";
        }

        StringBuilder reply = new StringBuilder("Transfers awaiting approval (")
                .append(count)
                .append("):\n");
        for (var item : inbox.items().items()) {
            reply.append("- ")
                    .append(item.reference())
                    .append(" — ")
                    .append(item.summary())
                    .append("\n");
        }
        reply.append("Action: open Approvals to approve or reject.");
        return reply.toString().trim();
    }

    private String pendingImportsAnswer(UserContext context) {
        if (!hasPermission(context, "import:view")) {
            return deny("imports");
        }

        List<Long> locationIds = accessibleLocationIds(context);
        boolean viewAll = locationAccessService.canViewAllLocations(context);
        var page = importOrderRepository.search(
                context.businessId(),
                locationIds.isEmpty() ? List.of(-1L) : locationIds,
                context.userId(),
                viewAll,
                "PENDING_APPROVAL",
                PageRequest.of(0, MAX_LIST_ITEMS));

        if (page.isEmpty()) {
            return "No import orders are pending approval in your scope.";
        }

        StringBuilder reply = new StringBuilder("Pending import orders (")
                .append(page.getTotalElements())
                .append("):\n");
        for (ImportOrder order : page.getContent()) {
            reply.append("- ")
                    .append(order.getImportNumber())
                    .append(" from ")
                    .append(order.getSupplierName())
                    .append(" (")
                    .append(order.getStatus())
                    .append(")\n");
        }
        reply.append("Action: review in Imports or Approvals.");
        return reply.toString().trim();
    }

    private String locationInventoryAnswer(UserContext context, String normalized) {
        if (!hasPermission(context, "inventory:view")) {
            return deny("inventory balances");
        }

        Location location = resolveLocationFromMessage(context, normalized);
        if (location == null) {
            return "Which location should I check? Try: \"Show me inventory in Main Warehouse\".";
        }

        var page = balanceRepository.search(
                context.businessId(),
                List.of(location.getId()),
                location.getId(),
                null,
                null,
                false,
                PageRequest.of(0, MAX_LIST_ITEMS));

        if (page.isEmpty()) {
            return "No inventory balances found at " + location.getName() + ".";
        }

        StringBuilder reply = new StringBuilder("Inventory at ")
                .append(location.getName())
                .append(" (top ")
                .append(page.getNumberOfElements())
                .append("):\n");
        for (InventoryBalance balance : page.getContent()) {
            Product product = productRepository.findById(balance.getProductId()).orElse(null);
            String productName = product != null ? product.getName() : "Product #" + balance.getProductId();
            reply.append("- ")
                    .append(productName)
                    .append(": ")
                    .append(formatQty(balance.getQuantityOnHand()))
                    .append(" on hand, ")
                    .append(formatQty(availableQty(balance)))
                    .append(" available\n");
        }
        return reply.toString().trim();
    }

    private String todaysSalesAnswer(UserContext context) {
        if (!hasPermission(context, "sale:view") && !hasPermission(context, "report:view")) {
            return deny("sales information");
        }

        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfDay = LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Long> shopScope = scopedShopIds(context);
        if (shopScope != null && shopScope.isEmpty()) {
            return "Today's sales: 0 completed transaction(s).";
        }

        long completed = salesReportRepository.countByStatus(
                context.businessId(), "COMPLETED", null, shopScope, startOfDay, endOfDay);

        StringBuilder reply = new StringBuilder("Today's sales: ")
                .append(completed)
                .append(" completed transaction(s).");

        if (hasPermission(context, "report:view")) {
            String currency = businessRepository.findByIdWithCurrency(context.businessId())
                    .orElseThrow(() -> new NotFoundException("Business not found"))
                    .getCurrencyCode();
            BigDecimal gross = salesReportRepository.sumTotalAmountByStatus(
                    context.businessId(), "COMPLETED", null, shopScope, startOfDay, endOfDay);
            reply.append(" Gross revenue: ")
                    .append(currency)
                    .append(" ")
                    .append(formatMoney(gross))
                    .append(".");
        } else {
            reply.append(" Revenue details require report access.");
        }

        reply.append(" Action: open Sales for transaction details.");
        return reply.toString();
    }

    private String topProductsAnswer(UserContext context) {
        if (!hasPermission(context, "report:view")) {
            return "Top-selling product analysis requires report access. You can browse products in the Products module.";
        }

        Instant from = LocalDate.now(ZoneOffset.UTC).minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = Instant.now();
        List<Long> shopScope = scopedShopIds(context);
        if (shopScope != null && shopScope.isEmpty()) {
            return "No completed sales in the last 30 days to rank products.";
        }
        var rows = extendedReportRepository.salesByProduct(context.businessId(), null, shopScope, from, to);

        if (rows.isEmpty()) {
            return "No completed sales in the last 30 days to rank products.";
        }

        String currency = businessRepository.findByIdWithCurrency(context.businessId())
                .orElseThrow(() -> new NotFoundException("Business not found"))
                .getCurrencyCode();

        StringBuilder reply = new StringBuilder("Top sellers (last 30 days):\n");
        rows.stream().limit(MAX_LIST_ITEMS).forEach(row -> {
            String name = (String) row[2];
            BigDecimal qty = (BigDecimal) row[3];
            BigDecimal amount = (BigDecimal) row[4];
            reply.append("- ")
                    .append(name)
                    .append(": ")
                    .append(formatQty(qty))
                    .append(" units, ")
                    .append(currency)
                    .append(" ")
                    .append(formatMoney(amount))
                    .append("\n");
        });
        reply.append("Action: ensure top sellers are in stock at shop locations.");
        return reply.toString().trim();
    }

    private String stockSummaryAnswer(UserContext context) {
        if (!hasPermission(context, "inventory:view")) {
            return deny("inventory summaries");
        }

        List<Long> locationIds = accessibleLocationIds(context);
        if (locationIds.isEmpty()) {
            return "You do not have access to any locations for a stock summary.";
        }

        long totalRows = balanceRepository.search(
                context.businessId(), locationIds, null, null, null, false, PageRequest.of(0, 1)).getTotalElements();
        long lowStock = balanceRepository.countLowStock(context.businessId(), locationIds);

        return "Stock summary for your locations: "
                + totalRows + " balance row(s), "
                + lowStock + " below reorder level. "
                + "Action: prioritize low-stock replenishment and review the Inventory page.";
    }

    private String pendingTasksAnswer(UserContext context) {
        List<String> lines = new ArrayList<>();

        if (hasPermission(context, "approval:view")) {
            var inbox = approvalInboxService.getInbox(null, 0, 1);
            long total = inbox.summary().totalCount();
            if (total > 0) {
                lines.add(total + " approval item(s) need attention");
            }
        }

        if (hasPermission(context, "import:view")) {
            List<Long> locationIds = accessibleLocationIds(context);
            long pendingImports = importOrderRepository.search(
                    context.businessId(),
                    locationIds.isEmpty() ? List.of(-1L) : locationIds,
                    context.userId(),
                    locationAccessService.canViewAllLocations(context),
                    "PENDING_APPROVAL",
                    PageRequest.of(0, 1)).getTotalElements();
            if (pendingImports > 0) {
                lines.add(pendingImports + " import(s) pending approval");
            }
        }

        if (hasPermission(context, "transfer:view") && hasPermission(context, "approval:view")) {
            long pendingTransfers = stockTransferRepository.search(
                    context.businessId(),
                    accessibleLocationIds(context).isEmpty() ? List.of(-1L) : accessibleLocationIds(context),
                    locationAccessService.canViewAllLocations(context),
                    "REQUESTED",
                    PageRequest.of(0, 1)).getTotalElements();
            if (pendingTransfers > 0) {
                lines.add(pendingTransfers + " transfer(s) awaiting approval");
            }
        }

        long unread = notificationRepository.countByBusinessIdAndUserIdAndStatus(
                context.businessId(), context.userId(), "UNREAD");
        if (unread > 0) {
            lines.add(unread + " unread notification(s)");
        }

        if (hasPermission(context, "inventory:view")) {
            List<Long> locationIds = accessibleLocationIds(context);
            if (!locationIds.isEmpty()) {
                long lowStock = balanceRepository.countLowStock(context.businessId(), locationIds);
                if (lowStock > 0) {
                    lines.add(lowStock + " low-stock item(s)");
                }
            }
        }

        if (lines.isEmpty()) {
            return "No pending tasks in your scope. You're caught up!";
        }

        return "Pending tasks summary:\n- "
                + String.join("\n- ", lines)
                + "\nAction: start with approvals and low-stock replenishment.";
    }

    private String approvalsAnswer(UserContext context) {
        if (!hasPermission(context, "approval:view")) {
            return deny("the approval inbox");
        }

        ApprovalInboxResponse inbox = approvalInboxService.getInbox(null, 0, MAX_LIST_ITEMS);
        var summary = inbox.summary();
        if (summary.totalCount() == 0) {
            return "Your approval inbox is clear — nothing waiting for action.";
        }

        return "Approval inbox: "
                + summary.adjustmentCount() + " adjustment(s), "
                + summary.transferCount() + " transfer(s), "
                + summary.importCount() + " import(s), "
                + summary.stocktakeCount() + " stocktake(s). "
                + "Open Approvals to action items you can approve.";
    }

    private String notificationsAnswer(UserContext context) {
        long unread = notificationRepository.countByBusinessIdAndUserIdAndStatus(
                context.businessId(), context.userId(), "UNREAD");
        if (unread == 0) {
            return "Your notification inbox is up to date — no unread messages.";
        }
        return "You have " + unread + " unread notification(s). Open Notifications to review alerts and approvals.";
    }

    private String productsAnswer(UserContext context, String normalized) {
        if (!hasPermission(context, "product:view")) {
            return deny("the product catalog");
        }

        String search = extractSearchTerm(normalized);
        var page = productRepository.search(
                context.businessId(),
                null,
                null,
                search,
                PageRequest.of(0, MAX_LIST_ITEMS));

        if (page.isEmpty()) {
            return search != null
                    ? "No products matched \"" + search + "\"."
                    : "No active products found in the catalog.";
        }

        StringBuilder reply = new StringBuilder("Products");
        if (search != null) {
            reply.append(" matching \"").append(search).append("\"");
        }
        reply.append(":\n");
        page.getContent().forEach(product -> reply.append("- ")
                .append(product.getSku())
                .append(" — ")
                .append(product.getName())
                .append("\n"));
        return reply.toString().trim();
    }

    private String businessPerformanceAnswer(UserContext context) {
        if (!hasPermission(context, "report:view") && !hasPermission(context, "alert:view")) {
            return deny("business performance metrics");
        }

        List<String> parts = new ArrayList<>();

        if (hasPermission(context, "report:view")) {
            Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant endOfDay = LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            List<Long> shopScope = scopedShopIds(context);
            long salesToday = shopScope != null && shopScope.isEmpty()
                    ? 0
                    : salesReportRepository.countByStatus(
                            context.businessId(), "COMPLETED", null, shopScope, startOfDay, endOfDay);
            parts.add(salesToday + " sale(s) completed today");
        }

        if (hasPermission(context, "inventory:view")) {
            List<Long> locationIds = accessibleLocationIds(context);
            if (!locationIds.isEmpty()) {
                parts.add(balanceRepository.countLowStock(context.businessId(), locationIds) + " low-stock SKU(s)");
            }
        }

        if (hasPermission(context, "approval:view")) {
            parts.add(approvalInboxService.getInbox(null, 0, 1).summary().totalCount() + " pending approval(s)");
        }

        if (parts.isEmpty()) {
            return "Limited performance data available with your current permissions.";
        }

        return "Business snapshot: " + String.join(", ", parts) + ". Ask a follow-up for details on any area.";
    }

    private String defaultHelp(UserContext context) {
        return """
                I can help with inventory, approvals, imports, transfers, sales, and notifications — \
                scoped to your role and locations.

                Try asking:
                - Which products are low on stock?
                - What imports are pending?
                - Summarize pending tasks.

                I only show financial metrics if you have report access.""";
    }

    private String deny(String topic) {
        return "You don't have permission to view " + topic + ". Contact your manager if you need access.";
    }

    private List<Long> accessibleLocationIds(UserContext context) {
        return locationAccessService.getAccessibleLocations(context).stream()
                .map(Location::getId)
                .toList();
    }

    /** Null means all shops (owner / view-all); empty list means no shop access. */
    private List<Long> scopedShopIds(UserContext context) {
        if (locationAccessService.canViewAllLocations(context)) {
            return null;
        }
        return locationAccessService.getAccessibleShopIds(context);
    }

    private Location findLocation(UserContext context, Long locationId) {
        return locationAccessService.getAccessibleLocations(context).stream()
                .filter(location -> location.getId().equals(locationId))
                .findFirst()
                .orElse(null);
    }

    private Location resolveLocationFromMessage(UserContext context, String normalized) {
        List<Location> locations = locationAccessService.getAccessibleLocations(context);
        for (Location location : locations) {
            if (normalized.contains(location.getName().toLowerCase(Locale.ROOT))) {
                return location;
            }
            if (location.getCode() != null
                    && normalized.contains(location.getCode().toLowerCase(Locale.ROOT))) {
                return location;
            }
        }
        if (normalized.contains("main warehouse") || normalized.contains("main wh")) {
            return locations.stream()
                    .filter(location -> location.getName().toLowerCase(Locale.ROOT).contains("main"))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private String extractSearchTerm(String normalized) {
        for (String prefix : List.of("product ", "products ", "sku ", "find ", "search ")) {
            if (normalized.startsWith(prefix)) {
                String term = normalized.substring(prefix.length()).trim();
                return term.isEmpty() ? null : term;
            }
        }
        return null;
    }

    private boolean hasPermission(UserContext context, String permission) {
        return context.permissions().contains(permission);
    }

    private boolean matchesAny(String text, String... phrases) {
        for (String phrase : phrases) {
            if (text.contains(phrase)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsLocationHint(String text) {
        return matchesAny(text, "warehouse", "shop", "location", "main warehouse", "inventory in", "stock in");
    }

    private BigDecimal availableQty(InventoryBalance balance) {
        BigDecimal onHand = balance.getQuantityOnHand() != null ? balance.getQuantityOnHand() : BigDecimal.ZERO;
        BigDecimal reserved = balance.getQuantityReserved() != null ? balance.getQuantityReserved() : BigDecimal.ZERO;
        return onHand.subtract(reserved);
    }

    private String formatQty(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
