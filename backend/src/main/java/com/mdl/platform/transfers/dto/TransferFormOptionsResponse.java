package com.mdl.platform.transfers.dto;

import java.util.List;

public record TransferFormOptionsResponse(
        List<TransferWarehouseOption> warehouses,
        List<TransferShopOption> shops
) {
    public record TransferWarehouseOption(
            Long id,
            String code,
            String name,
            String warehouseType,
            Long linkedShopId,
            String linkedShopName
    ) {
    }

    public record TransferShopOption(
            Long id,
            String code,
            String name,
            Long warehouseId
    ) {
    }
}
