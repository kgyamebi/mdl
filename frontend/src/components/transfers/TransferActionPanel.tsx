import { useState, type FormEvent } from 'react';
import { useAuth } from '../../auth/AuthContext';
import {
  approveTransfer,
  cancelTransfer,
  dispatchTransfer,
  receiveTransfer,
  rejectTransfer,
} from '../../services/transfersService';
import type { StockTransfer } from '../../types/api';

interface TransferActionPanelProps {
  transfer: StockTransfer;
  onUpdated: (transfer: StockTransfer) => void;
}

function formatQty(value: number): string {
  return new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 }).format(value);
}

export function TransferActionPanel({ transfer, onUpdated }: TransferActionPanelProps) {
  const { hasPermission, hasAnyPermission } = useAuth();
  const [rejectReason, setRejectReason] = useState('');
  const [showRejectForm, setShowRejectForm] = useState(false);
  const [receiveQuantities, setReceiveQuantities] = useState<Record<number, string>>(() =>
    Object.fromEntries(
      transfer.items
        .filter((item) => item.remainingToReceive > 0)
        .map((item) => [item.id, String(item.remainingToReceive)]),
    ),
  );
  const [acting, setActing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const canApprove = transfer.status === 'REQUESTED' && hasPermission('transfer:approve');
  const canReject = transfer.status === 'REQUESTED' && hasPermission('transfer:approve');
  const canDispatch = transfer.status === 'APPROVED' && hasPermission('transfer:dispatch');
  const canReceive =
    (transfer.status === 'DISPATCHED' || transfer.status === 'PARTIALLY_RECEIVED') &&
    hasPermission('transfer:receive');
  const canCancel =
    (transfer.status === 'REQUESTED' || transfer.status === 'APPROVED') &&
    hasAnyPermission('transfer:create', 'transfer:approve', 'stock:request');

  async function runAction(action: () => Promise<StockTransfer>) {
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

  async function handleReject(event: FormEvent) {
    event.preventDefault();
    const reason = rejectReason.trim();
    if (!reason) {
      setError('A rejection reason is required');
      return;
    }
    await runAction(() => rejectTransfer(transfer.id, reason));
    setShowRejectForm(false);
    setRejectReason('');
  }

  async function handleReceive(event: FormEvent) {
    event.preventDefault();
    const items = transfer.items
      .filter((item) => item.remainingToReceive > 0)
      .map((item) => ({
        itemId: item.id,
        quantityReceived: Number(receiveQuantities[item.id] ?? 0),
      }))
      .filter((item) => item.quantityReceived > 0);

    if (items.length === 0) {
      setError('Enter at least one quantity to receive');
      return;
    }

    await runAction(() => receiveTransfer(transfer.id, items));
  }

  const hasActions = canApprove || canReject || canDispatch || canReceive || canCancel;
  if (!hasActions) {
    return null;
  }

  return (
    <div className="approval-actions">
      {!showRejectForm && canReceive && (
        <form className="approval-actions__form" onSubmit={handleReceive}>
          <p className="muted">Receive quantities</p>
          {transfer.items
            .filter((item) => item.remainingToReceive > 0)
            .map((item) => (
              <label key={item.id} className="form__field">
                <span>
                  {item.productSku} — {item.productName} (remaining{' '}
                  {formatQty(item.remainingToReceive)} {item.unitOfMeasure})
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

      {!showRejectForm && !canReceive && (
        <div className="approval-actions__buttons">
          {canApprove && (
            <button
              type="button"
              className="btn btn--primary"
              disabled={acting}
              onClick={() => runAction(() => approveTransfer(transfer.id))}
            >
              {acting ? 'Working…' : 'Approve'}
            </button>
          )}
          {canDispatch && (
            <button
              type="button"
              className="btn btn--primary"
              disabled={acting}
              onClick={() => runAction(() => dispatchTransfer(transfer.id))}
            >
              {acting ? 'Working…' : 'Dispatch'}
            </button>
          )}
          {canReject && (
            <button
              type="button"
              className="btn btn--danger"
              disabled={acting}
              onClick={() => setShowRejectForm(true)}
            >
              Reject
            </button>
          )}
          {canCancel && (
            <button
              type="button"
              className="btn btn--ghost"
              disabled={acting}
              onClick={() => runAction(() => cancelTransfer(transfer.id))}
            >
              Cancel
            </button>
          )}
        </div>
      )}

      {showRejectForm && (
        <form className="approval-actions__form" onSubmit={handleReject}>
          <label className="form__field">
            <span>Rejection reason</span>
            <textarea
              className="textarea"
              rows={3}
              value={rejectReason}
              onChange={(event) => setRejectReason(event.target.value)}
              required
            />
          </label>
          <div className="approval-actions__buttons">
            <button type="submit" className="btn btn--danger" disabled={acting}>
              {acting ? 'Working…' : 'Confirm reject'}
            </button>
            <button
              type="button"
              className="btn btn--ghost"
              disabled={acting}
              onClick={() => {
                setShowRejectForm(false);
                setRejectReason('');
                setError(null);
              }}
            >
              Back
            </button>
          </div>
        </form>
      )}

      {error && <p className="form__error">{error}</p>}
    </div>
  );
}
