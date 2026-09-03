package com.mdl.platform.products.repository;

import com.mdl.platform.products.entity.ProductBarcode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductBarcodeRepository extends JpaRepository<ProductBarcode, Long> {

    List<ProductBarcode> findByProductIdOrderByPrimaryBarcodeDescBarcodeAsc(Long productId);

    List<ProductBarcode> findByProductIdIn(List<Long> productIds);

    Optional<ProductBarcode> findByIdAndBusinessId(Long id, Long businessId);

    Optional<ProductBarcode> findByBusinessIdAndBarcode(Long businessId, String barcode);

    boolean existsByBusinessIdAndBarcode(Long businessId, String barcode);
}
