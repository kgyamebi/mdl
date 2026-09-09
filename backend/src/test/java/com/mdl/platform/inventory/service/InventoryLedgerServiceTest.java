package com.mdl.platform.inventory.service;

import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.authorization.LocationAccessService;
import com.mdl.platform.common.exception.ConflictException;
import com.mdl.platform.inventory.entity.InventoryBalance;
import com.mdl.platform.inventory.entity.InventoryTransaction;
import com.mdl.platform.inventory.repository.InventoryBalanceRepository;
import com.mdl.platform.inventory.repository.InventoryTransactionRepository;
import com.mdl.platform.locations.entity.Location;
import com.mdl.platform.locations.repository.LocationRepository;
import com.mdl.platform.notifications.service.OperationalNotificationService;
import com.mdl.platform.products.entity.Product;
import com.mdl.platform.products.repository.ProductRepository;
import com.mdl.platform.security.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventoryLedgerServiceTest {

    private InventoryBalanceRepository balanceRepository;
    private InventoryTransactionRepository transactionRepository;
    private InventoryLedgerService service;

    @BeforeEach
    void setUp() {
        balanceRepository = mock(InventoryBalanceRepository.class);
        transactionRepository = mock(InventoryTransactionRepository.class);
        service = new InventoryLedgerService(
                mock(AuthorizationService.class),
                mock(LocationAccessService.class),
                balanceRepository,
                transactionRepository,
                mock(ProductRepository.class),
                mock(LocationRepository.class),
                mock(OperationalNotificationService.class));
    }

    @Test
    void positiveReceiptCanPartiallyClearImportedDeficit() {
        InventoryBalance balance = negativeBalance("-10");
        when(balanceRepository.findForUpdate(1L, null, null)).thenReturn(Optional.of(balance));
        when(transactionRepository.save(any(InventoryTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(balanceRepository.save(any(InventoryBalance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.applyOnHandChange(
                context(), new Location(), new Product(), new BigDecimal("4"),
                "ADJUSTMENT", "WAREHOUSE_STOCK", null, "Partial restock");

        assertThat(result.balance().getQuantityOnHand()).isEqualByComparingTo("-6");
        assertThat(result.transaction().getQuantityAfter()).isEqualByComparingTo("-6");
    }

    @Test
    void deductionCannotDeepenImportedDeficit() {
        InventoryBalance balance = negativeBalance("-10");
        when(balanceRepository.findForUpdate(1L, null, null)).thenReturn(Optional.of(balance));

        assertThatThrownBy(() -> service.applyOnHandChange(
                context(), new Location(), new Product(), new BigDecimal("-1"),
                "SALE", "SALE", 1L, null))
                .isInstanceOf(ConflictException.class);
    }

    private InventoryBalance negativeBalance(String quantity) {
        InventoryBalance balance = new InventoryBalance();
        balance.setBusinessId(1L);
        balance.setQuantityOnHand(new BigDecimal(quantity));
        balance.setQuantityReserved(BigDecimal.ZERO);
        return balance;
    }

    private UserContext context() {
        return new UserContext(
                1L, "owner@mdl.local", "owner", 1L, "MDL", "GHS",
                Set.of("OWNER"), Set.of("inventory:adjust"), 1L);
    }
}
