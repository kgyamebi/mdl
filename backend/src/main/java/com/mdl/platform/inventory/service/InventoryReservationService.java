package com.mdl.platform.inventory.service;

import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.authorization.LocationAccessService;
import com.mdl.platform.common.dto.PageResponse;
import com.mdl.platform.common.exception.ConflictException;
import com.mdl.platform.common.exception.NotFoundException;
import com.mdl.platform.inventory.dto.CreateReservationRequest;
import com.mdl.platform.inventory.dto.ReservationResponse;
import com.mdl.platform.inventory.entity.InventoryBalance;
import com.mdl.platform.inventory.entity.InventoryReservation;
import com.mdl.platform.inventory.repository.InventoryBalanceRepository;
import com.mdl.platform.inventory.repository.InventoryReservationRepository;
import com.mdl.platform.locations.entity.Location;
import com.mdl.platform.products.entity.Product;
import com.mdl.platform.security.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class InventoryReservationService {

    private final AuthorizationService authorizationService;
    private final LocationAccessService locationAccessService;
    private final InventoryReservationRepository reservationRepository;
    private final InventoryBalanceRepository balanceRepository;
    private final InventoryLedgerService ledgerService;

    public InventoryReservationService(
            AuthorizationService authorizationService,
            LocationAccessService locationAccessService,
            InventoryReservationRepository reservationRepository,
            InventoryBalanceRepository balanceRepository,
            InventoryLedgerService ledgerService) {
        this.authorizationService = authorizationService;
        this.locationAccessService = locationAccessService;
        this.reservationRepository = reservationRepository;
        this.balanceRepository = balanceRepository;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request) {
        authorizationService.requirePermission("inventory:reserve");
        UserContext context = authorizationService.requireAuthenticated();

        Location location = locationAccessService.requireAccessibleLocation(context, request.locationId());
        Product product = ledgerService.requireTrackableProduct(context.businessId(), request.productId());

        InventoryBalance balance = balanceRepository
                .findForUpdate(context.businessId(), location.getId(), product.getId())
                .orElseThrow(() -> new ConflictException("No stock balance exists at this location for the product"));

        BigDecimal available = balance.getQuantityOnHand().subtract(balance.getQuantityReserved());
        if (available.compareTo(request.quantity()) < 0) {
            throw new ConflictException("Insufficient available stock to reserve");
        }

        InventoryReservation reservation = new InventoryReservation();
        reservation.setBusinessId(context.businessId());
        reservation.setLocationId(location.getId());
        reservation.setProductId(product.getId());
        reservation.setQuantity(request.quantity());
        reservation.setReferenceType(normalizeReferenceType(request.referenceType()));
        reservation.setReferenceId(request.referenceId());
        reservation.setNotes(trimToNull(request.notes()));
        reservation.setStatus("ACTIVE");
        reservation.setReservedBy(context.userId());
        reservation = reservationRepository.save(reservation);

        balance.setQuantityReserved(balance.getQuantityReserved().add(request.quantity()));
        balanceRepository.save(balance);

        return toResponse(reservation, location, product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> listReservations(String status, int page, int size) {
        authorizationService.requirePermission("inventory:view");
        UserContext context = authorizationService.requireAuthenticated();

        List<Long> locationIds = locationAccessService.getAccessibleLocations(context).stream()
                .map(Location::getId)
                .toList();

        if (locationIds.isEmpty()) {
            return new PageResponse<>(List.of(), page, size, 0, 0);
        }

        Page<InventoryReservation> result = reservationRepository.search(
                context.businessId(),
                locationIds,
                normalizeStatus(status),
                PageRequest.of(Math.max(page, 0), Math.max(size, 1)));

        Map<Long, Location> locations = ledgerService.loadLocations(
                context.businessId(), result.map(InventoryReservation::getLocationId).toList());
        Map<Long, Product> products = ledgerService.loadProducts(
                context.businessId(), result.map(InventoryReservation::getProductId).toList());

        List<ReservationResponse> items = result.getContent().stream()
                .map(row -> toResponse(
                        row,
                        locations.get(row.getLocationId()),
                        products.get(row.getProductId())))
                .toList();

        return new PageResponse<>(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional
    public ReservationResponse releaseReservation(Long reservationId) {
        authorizationService.requirePermission("inventory:reserve");
        UserContext context = authorizationService.requireAuthenticated();

        InventoryReservation reservation = requireActiveReservation(context, reservationId);
        Location location = locationAccessService.requireAccessibleLocation(context, reservation.getLocationId());
        Product product = ledgerService.requireTrackableProduct(context.businessId(), reservation.getProductId());

        InventoryBalance balance = balanceRepository
                .findForUpdate(context.businessId(), location.getId(), product.getId())
                .orElseThrow(() -> new NotFoundException("Inventory balance not found"));

        balance.setQuantityReserved(balance.getQuantityReserved().subtract(reservation.getQuantity()));
        balanceRepository.save(balance);

        reservation.setStatus("RELEASED");
        reservation.setReleasedBy(context.userId());
        reservation.setReleasedAt(Instant.now());
        reservation = reservationRepository.save(reservation);

        return toResponse(reservation, location, product);
    }

    @Transactional
    public void reserveForTransfer(
            UserContext context,
            Long locationId,
            Long productId,
            BigDecimal quantity,
            Long transferId,
            String notes) {

        Location location = locationAccessService.requireAccessibleLocation(context, locationId);
        Product product = ledgerService.requireTrackableProduct(context.businessId(), productId);

        InventoryBalance balance = balanceRepository
                .findForUpdate(context.businessId(), location.getId(), product.getId())
                .orElseThrow(() -> new ConflictException("No stock balance exists at source for the product"));

        BigDecimal available = balance.getQuantityOnHand().subtract(balance.getQuantityReserved());
        if (available.compareTo(quantity) < 0) {
            throw new ConflictException("Insufficient available stock at source to approve transfer");
        }

        InventoryReservation reservation = new InventoryReservation();
        reservation.setBusinessId(context.businessId());
        reservation.setLocationId(location.getId());
        reservation.setProductId(product.getId());
        reservation.setQuantity(quantity);
        reservation.setReferenceType("TRANSFER");
        reservation.setReferenceId(transferId);
        reservation.setNotes(trimToNull(notes));
        reservation.setStatus("ACTIVE");
        reservation.setReservedBy(context.userId());
        reservationRepository.save(reservation);

        balance.setQuantityReserved(balance.getQuantityReserved().add(quantity));
        balanceRepository.save(balance);
    }

    @Transactional
    public void consumeReservationsForTransfer(UserContext context, Long transferId) {
        List<InventoryReservation> reservations = reservationRepository
                .findByBusinessIdAndReferenceTypeAndReferenceIdAndStatus(
                        context.businessId(), "TRANSFER", transferId, "ACTIVE");

        Instant now = Instant.now();
        for (InventoryReservation reservation : reservations) {
            InventoryBalance balance = balanceRepository
                    .findForUpdate(context.businessId(), reservation.getLocationId(), reservation.getProductId())
                    .orElseThrow(() -> new NotFoundException("Inventory balance not found"));

            balance.setQuantityReserved(balance.getQuantityReserved().subtract(reservation.getQuantity()));
            balanceRepository.save(balance);

            reservation.setStatus("CONSUMED");
            reservation.setReleasedBy(context.userId());
            reservation.setReleasedAt(now);
            reservationRepository.save(reservation);
        }
    }

    @Transactional
    public void releaseReservationsForTransfer(UserContext context, Long transferId) {
        List<InventoryReservation> reservations = reservationRepository
                .findByBusinessIdAndReferenceTypeAndReferenceIdAndStatus(
                        context.businessId(), "TRANSFER", transferId, "ACTIVE");

        Instant now = Instant.now();
        for (InventoryReservation reservation : reservations) {
            InventoryBalance balance = balanceRepository
                    .findForUpdate(context.businessId(), reservation.getLocationId(), reservation.getProductId())
                    .orElseThrow(() -> new NotFoundException("Inventory balance not found"));

            balance.setQuantityReserved(balance.getQuantityReserved().subtract(reservation.getQuantity()));
            balanceRepository.save(balance);

            reservation.setStatus("RELEASED");
            reservation.setReleasedBy(context.userId());
            reservation.setReleasedAt(now);
            reservationRepository.save(reservation);
        }
    }

    private InventoryReservation requireActiveReservation(UserContext context, Long reservationId) {
        InventoryReservation reservation = reservationRepository.findByIdAndBusinessId(reservationId, context.businessId())
                .orElseThrow(() -> new NotFoundException("Reservation not found"));
        if (!"ACTIVE".equals(reservation.getStatus())) {
            throw new ConflictException("Reservation is not active");
        }
        return reservation;
    }

    private ReservationResponse toResponse(
            InventoryReservation reservation, Location location, Product product) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getLocationId(),
                location != null ? location.getCode() : null,
                location != null ? location.getName() : null,
                reservation.getProductId(),
                product != null ? product.getSku() : null,
                product != null ? product.getName() : null,
                reservation.getQuantity(),
                reservation.getReferenceType(),
                reservation.getReferenceId(),
                reservation.getStatus(),
                reservation.getNotes(),
                reservation.getReservedBy(),
                reservation.getReleasedBy(),
                reservation.getReleasedAt(),
                reservation.getCreatedAt());
    }

    private String normalizeReferenceType(String referenceType) {
        if (referenceType == null || referenceType.isBlank()) {
            return "MANUAL";
        }
        return referenceType.trim().toUpperCase(Locale.ROOT);
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
}
