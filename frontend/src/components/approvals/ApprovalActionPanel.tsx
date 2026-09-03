import { useState, type FormEvent } from 'react';
import {
  approveInboxItem,
  canRejectEntityType,
  rejectInboxItem,
} from '../../services/approvalsService';
import type { ApprovalEntityType, ApprovalInboxItem } from '../../types/api';

interface ApprovalActionPanelProps {
  item: ApprovalInboxItem;
  onCompleted: () => void;
}

export function ApprovalActionPanel({ item, onCompleted }: ApprovalActionPanelProps) {
  const [notes, setNotes] = useState('');
  const [showRejectForm, setShowRejectForm] = useState(false);
  const [acting, setActing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const entityType = item.entityType as ApprovalEntityType;
  const supportsReject = canRejectEntityType(entityType);

  async function handleApprove() {
    setActing(true);
    setError(null);
    setMessage(null);

    try {
      await approveInboxItem(entityType, item.entityId);
      setMessage(
        item.totalSteps > 1 && item.currentStepOrder < item.totalSteps
          ? 'Step approved — waiting for next approver'
          : 'Approved',
      );
      onCompleted();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Approve failed');
    } finally {
      setActing(false);
    }
  }

  async function handleReject(event: FormEvent) {
    event.preventDefault();
    if (!supportsReject) {
      return;
    }

    const trimmedNotes = notes.trim();
    if (entityType === 'STOCK_TRANSFER' && !trimmedNotes) {
      setError('A rejection reason is required for transfers');
      return;
    }

    setActing(true);
    setError(null);
    setMessage(null);

    try {
      await rejectInboxItem(entityType, item.entityId, trimmedNotes);
      setMessage('Rejected');
      onCompleted();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Reject failed');
    } finally {
      setActing(false);
    }
  }

  if (!item.canAct) {
    return null;
  }

  return (
    <div className="approval-actions">
      {!showRejectForm ? (
        <div className="approval-actions__buttons">
          <button
            type="button"
            className="btn btn--primary"
            disabled={acting}
            onClick={handleApprove}
          >
            {acting ? 'Working…' : 'Approve'}
          </button>
          {supportsReject && (
            <button
              type="button"
              className="btn btn--danger"
              disabled={acting}
              onClick={() => setShowRejectForm(true)}
            >
              Reject
            </button>
          )}
        </div>
      ) : (
        <form className="approval-actions__form" onSubmit={handleReject}>
          <label className="form__field">
            <span>
              {entityType === 'STOCK_TRANSFER' ? 'Rejection reason' : 'Review notes (optional)'}
            </span>
            <textarea
              className="textarea"
              rows={3}
              value={notes}
              onChange={(event) => setNotes(event.target.value)}
              required={entityType === 'STOCK_TRANSFER'}
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
                setNotes('');
                setError(null);
              }}
            >
              Cancel
            </button>
          </div>
        </form>
      )}

      {error && <p className="form__error">{error}</p>}
      {message && <p className="approval-actions__success">{message}</p>}
    </div>
  );
}
