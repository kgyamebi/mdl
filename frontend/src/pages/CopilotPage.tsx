import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
import { useAuth } from '../auth/AuthContext';
import { fetchCopilotSuggestedPrompts, sendCopilotMessage } from '../services/copilotService';
import type { CopilotSuggestedPrompt } from '../types/api';

interface ChatMessage {
  id: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
}

export function CopilotPage() {
  const { hasPermission } = useAuth();
  const canUseCopilot = hasPermission('copilot:use');

  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [conversationId, setConversationId] = useState<number | null>(null);
  const [input, setInput] = useState('');
  const [prompts, setPrompts] = useState<CopilotSuggestedPrompt[]>([]);
  const [loading, setLoading] = useState(false);
  const [loadingPrompts, setLoadingPrompts] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!canUseCopilot) {
      setLoadingPrompts(false);
      return;
    }

    let cancelled = false;
    fetchCopilotSuggestedPrompts()
      .then((response) => {
        if (!cancelled) {
          setPrompts(response);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Failed to load suggested prompts');
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoadingPrompts(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [canUseCopilot]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  const submitMessage = useCallback(
    async (text: string) => {
      const trimmed = text.trim();
      if (!trimmed || loading) {
        return;
      }

      setError(null);
      setLoading(true);
      setInput('');

      const userMessage: ChatMessage = {
        id: `user-${Date.now()}`,
        role: 'USER',
        content: trimmed,
      };
      setMessages((current) => [...current, userMessage]);

      try {
        const response = await sendCopilotMessage({
          message: trimmed,
          conversationId,
        });
        setConversationId(response.conversationId);
        setMessages((current) => [
          ...current,
          {
            id: `assistant-${Date.now()}`,
            role: 'ASSISTANT',
            content: response.reply,
          },
        ]);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Copilot request failed');
      } finally {
        setLoading(false);
      }
    },
    [conversationId, loading],
  );

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    submitMessage(input);
  }

  function startNewChat() {
    setMessages([]);
    setConversationId(null);
    setError(null);
    setInput('');
  }

  if (!canUseCopilot) {
    return (
      <div className="page">
        <header className="page__header">
          <div>
            <p className="eyebrow">Assistant</p>
            <h1>MDL Copilot</h1>
          </div>
        </header>
        <section className="panel">
          <p className="muted">You need the <code>copilot:use</code> permission to use the AI assistant.</p>
        </section>
      </div>
    );
  }

  return (
    <div className="page copilot-page">
      <header className="page__header">
        <div>
          <p className="eyebrow">Assistant</p>
          <h1>MDL Copilot</h1>
          <p className="subtitle">Inventory and business answers scoped to your role</p>
        </div>
        <div className="page__header-actions">
          <button type="button" className="btn btn--ghost" onClick={startNewChat} disabled={loading}>
            New chat
          </button>
        </div>
      </header>

      <section className="panel copilot-panel">
        <div className="copilot-messages" aria-live="polite">
          {messages.length === 0 && !loading && (
            <div className="copilot-empty">
              <p className="muted">Ask about inventory, sales, transfers, imports, approvals, or pending tasks.</p>
              {!loadingPrompts && prompts.length > 0 && (
                <div className="copilot-prompts">
                  {prompts.map((item) => (
                    <button
                      key={item.prompt}
                      type="button"
                      className="copilot-prompt"
                      onClick={() => submitMessage(item.prompt)}
                      disabled={loading}
                    >
                      {item.prompt}
                    </button>
                  ))}
                </div>
              )}
            </div>
          )}

          {messages.map((message) => (
            <div
              key={message.id}
              className={`copilot-message copilot-message--${message.role.toLowerCase()}`}
            >
              <span className="copilot-message__role">
                {message.role === 'USER' ? 'You' : 'Copilot'}
              </span>
              <p>{message.content}</p>
            </div>
          ))}

          {loading && (
            <div className="copilot-message copilot-message--assistant">
              <span className="copilot-message__role">Copilot</span>
              <p className="muted">Thinking…</p>
            </div>
          )}
          <div ref={messagesEndRef} />
        </div>

        {error && <p className="form__error">{error}</p>}

        <form className="copilot-composer" onSubmit={handleSubmit}>
          <textarea
            className="input copilot-composer__input"
            rows={2}
            placeholder="Ask about stock, sales, approvals…"
            value={input}
            onChange={(event) => setInput(event.target.value)}
            disabled={loading}
          />
          <button type="submit" className="btn btn--primary" disabled={loading || !input.trim()}>
            {loading ? 'Sending…' : 'Send'}
          </button>
        </form>
      </section>
    </div>
  );
}
