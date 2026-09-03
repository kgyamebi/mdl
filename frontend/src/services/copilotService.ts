import { apiRequest } from './apiClient';
import type { CopilotChatResponse, CopilotSuggestedPrompt } from '../types/api';

export function fetchCopilotSuggestedPrompts(): Promise<CopilotSuggestedPrompt[]> {
  return apiRequest<CopilotSuggestedPrompt[]>('/api/copilot/suggested-prompts');
}

export function sendCopilotMessage(payload: {
  message: string;
  conversationId?: number | null;
}): Promise<CopilotChatResponse> {
  return apiRequest<CopilotChatResponse>('/api/copilot/chat', {
    method: 'POST',
    body: {
      message: payload.message,
      conversationId: payload.conversationId ?? null,
    },
  });
}
