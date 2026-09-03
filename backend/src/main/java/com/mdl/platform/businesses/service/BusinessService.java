package com.mdl.platform.businesses.service;

import com.mdl.platform.authorization.AuthorizationService;
import com.mdl.platform.businesses.dto.BusinessResponse;
import com.mdl.platform.businesses.dto.CurrencyResponse;
import com.mdl.platform.businesses.dto.UpdateBusinessRequest;
import com.mdl.platform.businesses.entity.Business;
import com.mdl.platform.businesses.entity.SupportedCurrency;
import com.mdl.platform.businesses.repository.BusinessRepository;
import com.mdl.platform.businesses.repository.SupportedCurrencyRepository;
import com.mdl.platform.common.exception.NotFoundException;
import com.mdl.platform.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BusinessService {

    private final AuthorizationService authorizationService;
    private final BusinessRepository businessRepository;
    private final SupportedCurrencyRepository supportedCurrencyRepository;

    public BusinessService(
            AuthorizationService authorizationService,
            BusinessRepository businessRepository,
            SupportedCurrencyRepository supportedCurrencyRepository) {
        this.authorizationService = authorizationService;
        this.businessRepository = businessRepository;
        this.supportedCurrencyRepository = supportedCurrencyRepository;
    }

    @Transactional(readOnly = true)
    public BusinessResponse getCurrentBusiness() {
        authorizationService.requirePermission("business:view");
        UserContext context = authorizationService.requireAuthenticated();
        return toResponse(requireBusiness(context.businessId()));
    }

    @Transactional
    public BusinessResponse updateCurrentBusiness(UpdateBusinessRequest request) {
        authorizationService.requirePermission("business:manage");
        UserContext context = authorizationService.requireAuthenticated();

        Business business = requireBusiness(context.businessId());
        SupportedCurrency currency = supportedCurrencyRepository.findById(request.currencyCode())
                .filter(SupportedCurrency::isActive)
                .orElseThrow(() -> new NotFoundException("Currency not supported: " + request.currencyCode()));

        business.setName(request.name().trim());
        business.setLegalName(request.legalName() != null ? request.legalName().trim() : null);
        business.setCurrency(currency);
        business.setTimezone(request.timezone().trim());

        return toResponse(businessRepository.save(business));
    }

    @Transactional(readOnly = true)
    public List<CurrencyResponse> listSupportedCurrencies() {
        authorizationService.requireAnyPermission("business:view", "business:manage");

        return supportedCurrencyRepository.findAll().stream()
                .filter(SupportedCurrency::isActive)
                .map(c -> new CurrencyResponse(c.getCode(), c.getName(), c.getSymbol(), c.getDecimalPlaces()))
                .toList();
    }

    private Business requireBusiness(Long businessId) {
        return businessRepository.findByIdWithCurrency(businessId)
                .orElseThrow(() -> new NotFoundException("Business not found"));
    }

    private BusinessResponse toResponse(Business business) {
        SupportedCurrency currency = business.getCurrency();
        return new BusinessResponse(
                business.getId(),
                business.getCode(),
                business.getName(),
                business.getLegalName(),
                currency.getCode(),
                currency.getName(),
                currency.getSymbol(),
                business.getTimezone(),
                business.getStatus(),
                business.getCreatedAt());
    }
}
