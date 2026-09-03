import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '../../auth/AuthContext';
import { fetchApprovalRule, fetchApprovalRules, updateApprovalRule } from '../../services/approvalsService';
import type { ApprovalRule } from '../../types/api';

export function ApprovalRulesPage() {
  const { hasPermission } = useAuth();
  const canManage = hasPermission('approval:manage');
  const [rules, setRules] = useState<ApprovalRule[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [selectedRule, setSelectedRule] = useState<ApprovalRule | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadRules = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchApprovalRules();
      setRules(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load rules');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadRules();
  }, [loadRules]);

  useEffect(() => {
    if (selectedId == null) {
      setSelectedRule(null);
      return;
    }
    fetchApprovalRule(selectedId)
      .then(setSelectedRule)
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load rule'));
  }, [selectedId]);

  async function toggleRule(rule: ApprovalRule) {
    if (!canManage) {
      return;
    }
    try {
      const updated = await updateApprovalRule(rule.id, { enabled: !rule.enabled });
      setRules((current) => current.map((r) => (r.id === updated.id ? updated : r)));
      if (selectedId === rule.id) {
        setSelectedRule(updated);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update rule');
    }
  }

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="eyebrow">Administration</p>
          <h1>Approval rules</h1>
          <p className="subtitle">Configure workflow rules and steps</p>
        </div>
      </header>

      {loading && <p className="muted">Loading rules…</p>}
      {error && <p className="form__error">{error}</p>}

      {!loading && (
        <div className={`workspace-split${selectedId != null ? ' workspace-split--open' : ''}`}>
          <div className="workspace-split__list">
            <div className="table-wrap table-wrap--stacked table-wrap--scroll-hint">
              <table className="table table--stacked">
                <thead>
                  <tr>
                    <th>Code</th>
                    <th>Name</th>
                    <th>Entity</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {rules.map((rule) => (
                    <tr
                      key={rule.id}
                      className={`table__row--clickable${selectedId === rule.id ? ' table__row--selected' : ''}`}
                      onClick={() => setSelectedId(rule.id)}
                    >
                      <td><strong>{rule.code}</strong></td>
                      <td>{rule.name}</td>
                      <td>{rule.entityType}</td>
                      <td>{rule.enabled ? 'Enabled' : 'Disabled'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {selectedRule && (
            <aside className="workspace-split__detail panel">
              <div className="panel__header">
                <div>
                  <h2>{selectedRule.name}</h2>
                  <p className="muted">{selectedRule.code}</p>
                </div>
                {canManage && (
                  <button type="button" className="btn btn--ghost" onClick={() => toggleRule(selectedRule)}>
                    {selectedRule.enabled ? 'Disable' : 'Enable'}
                  </button>
                )}
              </div>
              {selectedRule.description && <p>{selectedRule.description}</p>}
              <p className="muted">Permission: {selectedRule.requiredPermission}</p>
              {selectedRule.minAbsQuantity != null && (
                <p className="muted">Min quantity threshold: {selectedRule.minAbsQuantity}</p>
              )}
              <h3 className="panel__subheading">Steps</h3>
              <ol className="list">
                {selectedRule.steps.map((step) => (
                  <li key={step.id} className="list__item">
                    <div>
                      <strong>{step.stepOrder}. {step.name}</strong>
                      <p className="muted">{step.requiredPermission}</p>
                    </div>
                  </li>
                ))}
              </ol>
            </aside>
          )}
        </div>
      )}
    </div>
  );
}
