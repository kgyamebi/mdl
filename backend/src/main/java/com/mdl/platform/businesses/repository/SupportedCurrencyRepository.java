package com.mdl.platform.businesses.repository;

import com.mdl.platform.businesses.entity.SupportedCurrency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportedCurrencyRepository extends JpaRepository<SupportedCurrency, String> {
}
