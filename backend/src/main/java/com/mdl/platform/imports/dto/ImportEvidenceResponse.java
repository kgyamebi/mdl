package com.mdl.platform.imports.dto;

import java.time.Instant;

public record ImportEvidenceResponse(
        Long id,
        String evidenceType,
        String description,
        String referenceUri,
        Long uploadedBy,
        Instant createdAt
) {
}
