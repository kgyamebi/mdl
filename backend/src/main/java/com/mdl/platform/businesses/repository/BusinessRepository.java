package com.mdl.platform.businesses.repository;

import com.mdl.platform.businesses.entity.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BusinessRepository extends JpaRepository<Business, Long> {

    Optional<Business> findByCode(String code);

    @Query("SELECT b FROM Business b JOIN FETCH b.currency WHERE b.id = :id")
    Optional<Business> findByIdWithCurrency(@Param("id") Long id);
}
