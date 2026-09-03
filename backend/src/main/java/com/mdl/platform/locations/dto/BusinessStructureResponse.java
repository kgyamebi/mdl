package com.mdl.platform.locations.dto;

import java.util.List;

public record BusinessStructureResponse(
        BusinessOverview business,
        List<WarehouseResponse> mainWarehouses,
        List<WarehouseResponse> shopWarehouses,
        List<ShopResponse> shops,
        int transferRouteCount
) {
    public record BusinessOverview(
            String code,
            String name,
            String currencyCode
    ) {
    }
}
