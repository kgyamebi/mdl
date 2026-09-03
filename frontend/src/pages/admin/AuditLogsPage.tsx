import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '../../auth/AuthContext';
import { fetchAuditLogs } from '../../services/auditService';
import type { AuditLog } from '../../types/api';

function formatDetails(details: string | null): string {
  if (!details) {
    return '';
  }
  try {
    return JSON.stringify(JSON.parse(details), null, 2);
  } catch {
    return details;
  }
}

export function AuditLogsPage() {
  const { hasPermission } = useAuth();
  const canView = hasPermission('audit:view');

  const [items, setItems] = useState<AuditLog[]>([]);
  const [moduleFilter, setModuleFilter] = useState('');
  const [actionFilter, setActionFilter] = useState('');
  const [entityTypeFilter, setEntityTypeFilter] = useState('');
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const selected = items.find((item) => item.id === selectedId) ?? null;

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetchAuditLogs({
        module: moduleFilter || undefined,
        action: actionFilter || undefined,
        entityType: entityTypeFilter || undefined,
        page,
        size: 25,
      });
      setItems(response.items);
      setTotalPages(response.totalPages);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load audit logs');
    } finally {
      setLoading(false);
    }
  }, [actionFilter, entityTypeFilter, moduleFilter, page]);

  useEffect(() => {
    if (canView) {
      load();
    }
  }, [canView, load]);

  if (!canView) {
    return (
      <div className="page">
        <section className="panel">
          <p className="muted">You need the audit:view permission to view the audit trail.</p>
        </section>
      </div>
    );
  }

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="eyebrow">Administration</p>
          <h1>Audit trail</h1>
          <p className="subtitle">Who changed what, and when</p>
        </div>
        <div className="page__header-actions">
          <button type="button" className="btn btn--ghost" onClick={load} disabled={loading}>
            Refresh
          </button>
        </div>
      </header>

      <div className="toolbar">
        <input
          type="search"
          className="input"
          placeholder="Module (e.g. AUTH, SALES)"
          value={moduleFilter}
          onChange={(e) => { setPage(0); setModuleFilter(e.target.value); }}
        />
        <input
          type="search"
          className="input"
          placeholder="Action (e.g. LOGIN_SUCCESS)"
          value={actionFilter}
          onChange={(e) => { setPage(0); setActionFilter(e.target.value); }}
        />
        <input
          type="search"
          className="input"
          placeholder="Entity type"
          value={entityTypeFilter}
          onChange={(e) => { setPage(0); setEntityTypeFilter(e.target.value); }}
        />
      </div>

      {loading && <p className="muted">Loading audit logs…</p>}
      {error && <p className="form__error">{error}</p>}

      {!loading && !error && (
        <div className={`workspace-split${selectedId != null ? ' workspace-split--open' : ''}`}>
          <div className="workspace-split__list">
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>When</th>
                    <th>Action</th>
                    <th>Module</th>
                    <th>Summary</th>
                  </tr>
                </thead>
                <tbody>
                  {items.length === 0 ? (
                    <tr>
                      <td colSpan={4} className="muted">No audit entries match your filters.</td>
                    </tr>
                  ) : (
                    items.map((entry) => (
                      <tr
                        key={entry.id}
                        className={`table__row--clickable${selectedId === entry.id ? ' table__row--selected' : ''}`}
                        onClick={() => setSelectedId(entry.id)}
                      >
                        <td>{new Date(entry.createdAt).toLocaleString()}</td>
                        <td><strong>{entry.action}</strong></td>
                        <td>{entry.module}</td>
                        <td>{entry.summary}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
            {totalPages > 1 && (
              <div className="pager">
                <button type="button" className="btn btn--ghost" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Previous</button>
                <span className="muted">Page {page + 1} of {totalPages}</span>
                <button type="button" className="btn btn--ghost" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>Next</button>
              </div>
            )}
          </div>

          {selected && (
            <aside className="workspace-split__detail panel">
              <h2>{selected.action}</h2>
              <p className="muted">{new Date(selected.createdAt).toLocaleString()}</p>
              <dl className="audit-detail">
                <dt>Module</dt><dd>{selected.module}</dd>
                <dt>User ID</dt><dd>{selected.userId ?? '—'}</dd>
                <dt>Entity</dt><dd>{selected.entityType ?? '—'} {selected.entityId != null ? `#${selected.entityId}` : ''}</dd>
                <dt>Reference</dt><dd>{selected.entityRef ?? '—'}</dd>
                <dt>IP address</dt><dd>{selected.ipAddress ?? '—'}</dd>
              </dl>
              <p>{selected.summary}</p>
              {selected.details && (
                <>
                  <h3 className="panel__subheading">Details</h3>
                  <pre className="audit-detail__json">{formatDetails(selected.details)}</pre>
                </>
              )}
            </aside>
          )}
        </div>
      )}
    </div>
  );
}
