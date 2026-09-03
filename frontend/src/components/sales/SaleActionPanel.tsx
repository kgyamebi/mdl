import { useState, type FormEvent } from 'react';
import { useAuth } from '../../auth/AuthContext';
import { cancelSale, refundSale } from '../../services/salesService';
import type { Sale } from '../../types/api';

interface SaleActionPanelProps {
  sale: Sale;
  onUpdated: (sale: Sale) => void;
}

export function SaleActionPanel({ sale, onUpdated }: SaleActionPanelProps) {
  const { hasPermission } = useAuth();
  const [actionType, setActionType] = useState<'cancel' | 'refund' | null>(null);
  const [reason, setReason] = useState('');
  const [acting, setActing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const canCancel = sale.status === 'COMPLETED' && hasPermission('sale:cancel');
  const canRefund = sale.status === 'COMPLETED' && hasPermission('sale:refund');

  if (!canCancel && !canRefund) {
    return null;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const trimmedReason = reason.trim();
    if (!trimmedReason) {
      setError('A reason is required');
      return;
    }

    setActing(true);
    setError(null);

    try {
      const updated =
        actionType === 'refund'
          ? await refundSale(sale.id, trimmedReason)
          : await cancelSale(sale.id, trimmedReason);
      onUpdated(updated);
      setActionType(null);
      setReason('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Action failed');
    } finally {
      setActing(false);
    }
  }

  if (actionType) {
    return (
      <div className="approval-actions">
        <form className="approval-actions__form" onSubmit={handleSubmit}>
          <label className="form__field">
            <span>{actionType === 'refund' ? 'Refund reason' : 'Cancellation reason'}</span>
            <textarea
              className="textarea"
              rows={3}
              value={reason}
              onChange={(event) => setReason(event.target.value)}
              required
            />
          </label>
          <div className="approval-actions__buttons">
            <button type="submit" className="btn btn--danger" disabled={acting}>
              {acting ? 'Working…' : actionType === 'refund' ? 'Confirm refund' : 'Confirm cancel'}
            </button>
            <button
              type="button"
              className="btn btn--ghost"
              disabled={acting}
              onClick={() => {
                setActionType(null);
                setReason('');
                setError(null);
              }}
            >
              Back
            </button>
          </div>
          {error && <p className="form__error">{error}</p>}
        </form>
      </div>
    );
  }

  return (
    <div className="approval-actions">
      <div className="approval-actions__buttons">
        {canCancel && (
          <button
            type="button"
            className="btn btn--danger"
            disabled={acting}
            onClick={() => setActionType('cancel')}
          >
            Cancel sale
          </button>
        )}
        {canRefund && (
          <button
            type="button"
            className="btn btn--ghost"
            disabled={acting}
            onClick={() => setActionType('refund')}
          >
            Refund sale
          </button>
        )}
      </div>
    </div>
  );
}
