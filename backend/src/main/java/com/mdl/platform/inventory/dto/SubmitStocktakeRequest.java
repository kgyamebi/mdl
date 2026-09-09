package com.mdl.platform.inventory.dto;

public record SubmitStocktakeRequest(
        Boolean treatUncountedAsExpected
) {
}
