package com.mdl.platform.copilot.repository;

import com.mdl.platform.copilot.entity.CopilotUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CopilotUsageLogRepository extends JpaRepository<CopilotUsageLog, Long> {
}
