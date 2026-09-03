package com.mdl.platform.imports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddImportEvidenceRequest(
        @NotBlank @Pattern(regexp = "PHOTO|DOCUMENT|NOTE") String evidenceType,
        @NotBlank @Size(max = 1000) String description,
        @Size(max = 500) String referenceUri
) {
}
