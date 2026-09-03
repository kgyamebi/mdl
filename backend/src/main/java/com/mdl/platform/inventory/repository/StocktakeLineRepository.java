package com.mdl.platform.inventory.repository;

import com.mdl.platform.inventory.entity.StocktakeLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StocktakeLineRepository extends JpaRepository<StocktakeLine, Long> {

    List<StocktakeLine> findByStocktakeIdOrderByIdAsc(Long stocktakeId);

    Optional<StocktakeLine> findByStocktakeIdAndProductId(Long stocktakeId, Long productId);

    long countByStocktakeId(Long stocktakeId);
}
