package com.mdl.platform.businesses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateBusinessRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String legalName,
        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "Currency code must be 3 uppercase letters") String currencyCode,
        @NotBlank @Size(max = 64) String timezone
) {
}
