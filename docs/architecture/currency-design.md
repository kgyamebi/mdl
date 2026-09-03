# Currency Design — Configurable, Not Fixed

## Principle

**GHS is the default for MDL, but currency is a business setting — never hard-coded in application logic.**

This allows:
- MDL to operate in GHS today
- Another tenant to use USD, NGN, EUR, etc.
- A business to change display/reporting currency settings via admin (with audit trail in a later phase)

## Database

### `businesses.currency_code`

- ISO 4217 code (3 letters): `GHS`, `USD`, `EUR`, etc.
- Default for MDL seed: `GHS`
- FK conceptually references `supported_currencies.code`

### `supported_currencies`

Reference table of allowed currencies:

| code | name | symbol | decimal_places |
|------|------|--------|----------------|
| GHS | Ghana Cedi | GHS | 2 |
| USD | US Dollar | $ | 2 |
| EUR | Euro | € | 2 |
| ... | (expandable) | | |

Add new currencies by inserting rows — no code change required.

## Application rules

### Storage

- All monetary amounts stored as `DECIMAL(19,4)` in the database
- Amounts are stored in the **business's currency** unless multi-currency is explicitly added later
- Never use `float` or `double` for money

### Display

```typescript
// Frontend reads currency from business context (future API)
formatMoney(amount, business.currencyCode, business.locale)
// Example: 24500 → "GHS 24,500.00"
```

### API

Business context (including `currency_code`) loaded after login and attached to the session/JWT business claim.

Reports, POS receipts, and owner dashboard all use the business's configured currency.

## Future expansion (not Phase 1)

| Feature | Approach |
|---------|----------|
| Multi-currency imports | Store `original_currency` + `exchange_rate` on import |
| Display in secondary currency | Business setting + conversion table |
| Historical rate locking | Rate frozen at transaction time on `sales`, `imports` |

## What NOT to do

```java
// BAD — hard-coded currency
return "GHS " + amount;

// GOOD — use business context
return currencyFormatter.format(amount, business.getCurrencyCode());
```

## MDL default

The seed migration (`V1__create_businesses.sql`) creates:

- Business code: `MDL`
- Name: `Modern Dream Light`
- Currency: `GHS`
- Timezone: `Africa/Accra`

This can be changed by an authorized owner/admin through business settings (Phase 5+).
