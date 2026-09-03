import { useCallback, useEffect, useState } from 'react';
import { ApprovalActionPanel } from '../components/approvals/ApprovalActionPanel';
import { fetchApprovalInbox } from '../services/approvalsService';
import type { ApprovalInboxItem, ApprovalInboxSummary } from '../types/api';

function formatEntityType(value: string): string {
  return value
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

export function ApprovalsPage() {
  const [summary, setSummary] = useState<ApprovalInboxSummary | null>(null);
  const [items, setItems] = useState<ApprovalInboxItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadInbox = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetchApprovalInbox();
      setSummary(response.summary);
      setItems(response.items.items);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load approvals inbox');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadInbox();
  }, [loadInbox]);

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="eyebrow">Workflows</p>
          <h1>Approvals inbox</h1>
          <p className="subtitle">Review and action pending items</p>
        </div>
        <button type="button" className="btn btn--ghost" onClick={loadInbox} disabled={loading}>
          Refresh
        </button>
      </header>

      {loading && <p className="muted">Loading inbox…</p>}
      {error && <p className="form__error">{error}</p>}

      {!loading && !error && summary && (
        <>
          <dl className="stat-grid stat-grid--inline">
            <div>
              <dt>Adjustments</dt>
              <dd>{summary.adjustmentCount}</dd>
            </div>
            <div>
              <dt>Transfers</dt>
              <dd>{summary.transferCount}</dd>
            </div>
            <div>
              <dt>Imports</dt>
              <dd>{summary.importCount}</dd>
            </div>
            <div>
              <dt>Stocktakes</dt>
              <dd>{summary.stocktakeCount}</dd>
            </div>
          </dl>

          {items.length === 0 ? (
            <section className="panel">
              <p className="muted">No pending approvals require your action.</p>
            </section>
          ) : (
            <ul className="list list--cards">
              {items.map((item) => (
                <li key={`${item.entityType}-${item.entityId}`} className="approval-card">
                  <div className="approval-card__header">
                    <div>
                      <span className="pill">{formatEntityType(item.entityType)}</span>
                      <strong>{item.reference}</strong>
                    </div>
                    {item.canAct && <span className="pill pill--ok">Action required</span>}
                  </div>
                  <h2>{item.title}</h2>
                  <p className="muted">{item.summary}</p>
                  <dl className="approval-card__meta">
                    <div>
                      <dt>Step</dt>
                      <dd>
                        {item.currentStepOrder} / {item.totalSteps} — {item.currentStepName}
                        {item.parallelStep && ' (any approver)'}
                      </dd>
                    </div>
                    <div>
                      <dt>Submitted</dt>
                      <dd>{new Date(item.submittedAt).toLocaleString()}</dd>
                    </div>
                  </dl>
                  <ApprovalActionPanel item={item} onCompleted={loadInbox} />
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </div>
  );
}
