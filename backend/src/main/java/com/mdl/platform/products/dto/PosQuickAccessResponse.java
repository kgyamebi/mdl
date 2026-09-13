package com.mdl.platform.products.dto;

import java.util.List;

public record PosQuickAccessResponse(
        List<PosProductHit> recent,
        List<PosProductHit> frequent,
        List<PosProductHit> favorites
) {
}
