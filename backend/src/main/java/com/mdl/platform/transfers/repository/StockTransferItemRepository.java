package com.mdl.platform.transfers.repository;

import com.mdl.platform.transfers.entity.StockTransferItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockTransferItemRepository extends JpaRepository<StockTransferItem, Long> {

    List<StockTransferItem> findByTransferIdOrderByIdAsc(Long transferId);

    List<StockTransferItem> findByTransferIdIn(List<Long> transferIds);
}
