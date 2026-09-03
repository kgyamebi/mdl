package com.mdl.platform.copilot.repository;

import com.mdl.platform.copilot.entity.CopilotConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CopilotConversationRepository extends JpaRepository<CopilotConversation, Long> {

    Page<CopilotConversation> findByBusinessIdAndUserIdOrderByUpdatedAtDesc(
            Long businessId, Long userId, Pageable pageable);

    Optional<CopilotConversation> findByIdAndBusinessIdAndUserId(Long id, Long businessId, Long userId);
}
