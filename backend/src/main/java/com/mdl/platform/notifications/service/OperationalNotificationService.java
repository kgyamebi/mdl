package com.mdl.platform.notifications.service;

import com.mdl.platform.inventory.entity.InventoryTransaction;
import com.mdl.platform.locations.entity.Location;
import com.mdl.platform.products.entity.Product;
import com.mdl.platform.security.UserContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;

/**
 * Owner-facing operational alerts for inventory movements and related events.
 */
@Service
public class OperationalNotificationService {

    private static final Set<String> NOTIFY_TYPES = Set.of(
            "SALE", "SALE_CANCEL", "SALE_REFUND", "RETURN",
            "TRANSFER_OUT", "TRANSFER_IN",
            "ADJUSTMENT", "DAMAGE", "STOCKTAKE");

    private final NotificationPublisher notificationPublisher;

    public OperationalNotificationService(NotificationPublisher notificationPublisher) {
        this.notificationPublisher = notificationPublisher;
    }

    public void notifyInventoryMovement(
            UserContext context,
            Location location,
            Product product,
            BigDecimal quantityChange,
            InventoryTransaction transaction) {
        if (!NOTIFY_TYPES.contains(transaction.getTransactionType())) {
            return;
        }

        String direction = quantityChange.compareTo(BigDecimal.ZERO) < 0 ? "removed from" : "added to";
        String title = "Stock " + direction + " " + location.getCode();
        String message = product.getSku() + " — " + formatQty(quantityChange.abs())
                + " " + product.getUnitOfMeasure()
                + " (" + humanizeType(transaction.getTransactionType()) + ")"
                + (transaction.getNotes() != null && !transaction.getNotes().isBlank()
                ? " — " + transaction.getNotes()
                : "");

        notificationPublisher.notifyUsersWithPermission(
                context.businessId(),
                "alert:view",
                new NotificationEvent(
                        "INVENTORY_" + transaction.getTransactionType(),
                        "OPERATIONS",
                        title,
                        message,
                        "INVENTORY_TRANSACTION",
                        transaction.getId(),
                        product.getSku(),
                        transaction.getReferenceType(),
                        transaction.getReferenceId(),
                        "NOTIF:INV_TX:" + transaction.getId()));
    }

    private String formatQty(BigDecimal qty) {
        return qty.stripTrailingZeros().toPlainString();
    }

    private String humanizeType(String type) {
        return type.toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
