package com.mdl.platform.imports.repository;

import com.mdl.platform.imports.entity.ImportItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImportItemRepository extends JpaRepository<ImportItem, Long> {

    List<ImportItem> findByImportIdOrderByIdAsc(Long importId);

    Optional<ImportItem> findByIdAndImportIdAndBusinessId(Long id, Long importId, Long businessId);

    List<ImportItem> findByImportIdIn(List<Long> importIds);
}
