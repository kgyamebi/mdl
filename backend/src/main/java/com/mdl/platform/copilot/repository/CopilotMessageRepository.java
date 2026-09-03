package com.mdl.platform.copilot.repository;

import com.mdl.platform.copilot.entity.CopilotMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CopilotMessageRepository extends JpaRepository<CopilotMessage, Long> {

    List<CopilotMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
