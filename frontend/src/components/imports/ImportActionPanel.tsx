import { useState, type FormEvent } from 'react';
import { useAuth } from '../../auth/AuthContext';
import {
  approveImport,
  cancelImport,
  receiveImport,
  submitImport,
  verifyImport,
} from '../../services/importsService';
import type { ImportOrder } from '../../types/api';

const RECEIVABLE_STATUSES = new Set(['APPROVED', 'RECEIVING', 'PARTIALLY_RECEIVED']);

interface ImportActionPanelProps {
  importOrder: ImportOrder;
  onUpdated: (importOrder: ImportOrder) => void;
}

function formatQty(value: number): string {
  return new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 }).format(value);
}

export function ImportActionPanel({ importOrder, onUpdated }: ImportActionPanelProps) {
  const { hasPermission, hasAnyPermission } = useAuth();
  const [receiveQuantities, setReceiveQuantities] = useState<Record<number, string>>(() =>
    Object.fromEntries(
      importOrder.items
        .filter((item) => item.remainingQuantity > 0)
        .map((item) => [item.id, String(item.remainingQuantity)]),
    ),
  );
  const [acting, setActing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const canSubmit = importOrder.status === 'DRAFT' && hasPermission('import:create');
  const canApprove = importOrder.status === 'PENDING_APPROVAL' && hasPermission('import:approve');
  const canReceive =
    RECEIVABLE_STATUSES.has(importOrder.status) &&
    hasAnyPermission('import:receive', 'import:receive:task');
  const canVerify = importOrder.status === 'RECEIVED' && hasPermission('import:verify');
  const canCancel =
    hasAnyPermission('import:create', 'import:approve') &&
    !['RECEIVED', 'VERIFIED', 'CANCELLED'].includes(importOrder.status);

  async function runAction(action: () => Promise<ImportOrder>) {
    setActing(true);
    setError(null);
    try {
      const updated = await action();
      onUpdated(updated);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Action failed');
    } finally {
      setActing(false);
    }
  }

  async function handleReceive(event: FormEvent) {
    event.preventDefault();
    const items = importOrder.items
      .filter((item) => item.remainingQuantity > 0)
      .map((item) => ({
        itemId: item.id,
        quantityReceived: Number(receiveQuantities[item.id] ?? 0),
      }))
      .filter((item) => item.quantityReceived > 0);

    if (items.length === 0) {
      setError('Enter at least one quantity to receive');
      return;
    }

    await runAction(() => receiveImport(importOrder.id, items));
  }

  const hasActions = canSubmit || canApprove || canReceive || canVerify || canCancel;
  if (!hasActions) {
    return null;
  }

  return (
    <div className="approval-actions">
      {canReceive && (
        <form className="approval-actions__form" onSubmit={handleReceive}>
          <p className="muted">Receive quantities</p>
          {importOrder.items
            .filter((item) => item.remainingQuantity > 0)
            .map((item) => (
              <label key={item.id} className="form__field">
                <span>
                  {item.productSku} — {item.productName} (remaining{' '}
                  {formatQty(item.remainingQuantity)} {item.unitOfMeasure})
                </span>
                <input
                  type="number"
                  min="0"
                  step="any"
                  className="input"
                  value={receiveQuantities[item.id] ?? ''}
                  onChange={(event) =>
                    setReceiveQuantities((current) => ({
                      ...current,
                      [item.id]: event.target.value,
                    }))
                  }
                />
              </label>
            ))}
          <div className="approval-actions__buttons">
            <button type="submit" className="btn btn--primary" disabled={acting}>
              {acting ? 'Working…' : 'Receive stock'}
            </button>
          </div>
        </form>
      )}

      {!canReceive && (
        <div className="approval-actions__buttons">
          {canSubmit && (
            <button
              type="button"
              className="btn btn--primary"
              disabled={acting}
              onClick={() => runAction(() => submitImport(importOrder.id))}
            >
              {acting ? 'Working…' : 'Submit for approval'}
            </button>
          )}
          {canApprove && (
            <button
              type="button"
              className="btn btn--primary"
              disabled={acting}
              onClick={() => runAction(() => approveImport(importOrder.id))}
            >
              {acting ? 'Working…' : 'Approve'}
            </button>
          )}
          {canVerify && (
            <button
              type="button"
              className="btn btn--primary"
              disabled={acting}
              onClick={() => runAction(() => verifyImport(importOrder.id))}
            >
              {acting ? 'Working…' : 'Verify receiving'}
            </button>
          )}
          {canCancel && (
            <button
              type="button"
              className="btn btn--ghost"
              disabled={acting}
              onClick={() => runAction(() => cancelImport(importOrder.id))}
            >
              Cancel
            </button>
          )}
        </div>
      )}

      {error && <p className="form__error">{error}</p>}
    </div>
  );
}
