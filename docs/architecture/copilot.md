# AI Copilot — Phase 35

## Purpose

Provide a **permission-aware business assistant** that answers operational questions about inventory, sales, transfers, imports, approvals, and notifications — scoped to each user's role and location access.

## API

| Method | Path | Permission | Description |
|--------|------|------------|-------------|
| POST | `/api/copilot/chat` | `copilot:use` | Send a message and receive a reply |
| GET | `/api/copilot/suggested-prompts` | `copilot:use` | Permission-filtered starter prompts |
| GET | `/api/copilot/conversations` | `copilot:use` | List user's conversations |
| GET | `/api/copilot/conversations/{id}` | `copilot:use` | Conversation with message history |

### Chat request

```json
{
  "message": "Which products are low on stock?",
  "conversationId": null
}
```

### Chat response

```json
{
  "conversationId": 12,
  "reply": "...",
  "provider": "data-grounded",
  "model": "data-grounded",
  "promptTokens": 42,
  "completionTokens": 88,
  "suggestedFollowUps": ["Summarize pending tasks."]
}
```

## Permission model

- **`copilot:use`** — required to access any copilot endpoint (seeded in V24 for all roles).
- **Data scope** — answers only include modules the user can access:
  - `inventory:view` — stock balances, low stock, location inventory
  - `sale:view` / `report:view` — sales counts; revenue only with `report:view`
  - `transfer:view` / `import:view` — operational status
  - `approval:view` — pending approvals
- **Location scoping** — workers see only assigned locations via `LocationAccessService`.

Financial metrics are **never** returned without `report:view`.

## AI provider architecture

```
CopilotChatService
  └── AiCompletionClient (interface)
        ├── DataGroundedAiCompletionClient (default)
        └── OpenAiCompletionClient (optional, app.copilot.provider=openai)
```

Default provider uses **data-grounded heuristics** against live business data — no external API key required.

Configure OpenAI (optional):

```yaml
app:
  copilot:
    provider: openai
    openai:
      api-key: ${OPENAI_API_KEY}
      model: gpt-4o-mini
```

Azure OpenAI settings are reserved under `app.copilot.azure.*` for future swap-in.

## Persistence & audit

| Table | Purpose |
|-------|---------|
| `copilot_conversations` | Per-user chat threads |
| `copilot_messages` | USER / ASSISTANT messages + token counts |
| `copilot_usage_logs` | Token usage audit trail |

Each chat also writes an audit log entry: action `COPILOT_CHAT`, module `COPILOT`.

## Migration

`V24__create_copilot.sql`

## Frontend

Route: `/copilot` — sidebar link (highlighted), mobile **More** menu entry, and a floating **Ask MDL ASSISTANT** button on all authenticated pages.

Service: `frontend/src/services/copilotService.ts`

## Related

- [Frontend UI](./frontend-ui.md)
- [Approvals](./approvals.md)
- Root `README.md`
