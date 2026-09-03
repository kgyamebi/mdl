package com.mdl.platform.imports.repository;

import com.mdl.platform.imports.entity.ImportEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportEvidenceRepository extends JpaRepository<ImportEvidence, Long> {

    List<ImportEvidence> findByImportIdOrderByCreatedAtDesc(Long importId);
}
