import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '../../auth/AuthContext';
import {
  fetchBusinessStructure,
  fetchTransferRoutes,
  updateTransferRoute,
} from '../../services/locationsService';
import type { BusinessStructure, TransferRoute } from '../../types/api';

export function LocationsAdminPage() {
  const { hasPermission } = useAuth();
  const canManage = hasPermission('business:manage');

  const [structure, setStructure] = useState<BusinessStructure | null>(null);
  const [routes, setRoutes] = useState<TransferRoute[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [structureData, routeData] = await Promise.all([
        fetchBusinessStructure(),
        fetchTransferRoutes(),
      ]);
      setStructure(structureData);
      setRoutes(routeData);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load locations');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function toggleRoute(route: TransferRoute) {
    if (!canManage) {
      return;
    }
    try {
      const updated = await updateTransferRoute(route.id, { enabled: !route.enabled, notes: route.notes ?? undefined });
      setRoutes((current) => current.map((r) => (r.id === updated.id ? updated : r)));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update route');
    }
  }

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="eyebrow">Administration</p>
          <h1>Locations & routes</h1>
          <p className="subtitle">Business structure and transfer routes</p>
        </div>
      </header>

      {loading && <p className="muted">Loading…</p>}
      {error && <p className="form__error">{error}</p>}

      {!loading && structure && (
        <>
          <section className="panel">
            <h2>Structure overview</h2>
            <p className="muted">
              {structure.business.name} · {structure.shops.length} shop(s) ·{' '}
              {structure.mainWarehouses.length + structure.shopWarehouses.length} warehouse(s) ·{' '}
              {structure.transferRouteCount} route(s)
            </p>
            <div className="grid grid--dashboard">
              <div>
                <h3 className="panel__subheading">Main warehouses</h3>
                <ul className="list">
                  {structure.mainWarehouses.map((w) => (
                    <li key={w.id} className="list__item">
                      <strong>{w.code}</strong>
                      <span className="muted">{w.name}</span>
                    </li>
                  ))}
                </ul>
              </div>
              <div>
                <h3 className="panel__subheading">Shops</h3>
                <ul className="list">
                  {structure.shops.map((s) => (
                    <li key={s.id} className="list__item">
                      <strong>{s.code}</strong>
                      <span className="muted">{s.name}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          </section>

          <section className="panel">
            <h2>Transfer routes</h2>
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>From</th>
                    <th>To</th>
                    <th>Status</th>
                    {canManage && <th>Action</th>}
                  </tr>
                </thead>
                <tbody>
                  {routes.map((route) => (
                    <tr key={route.id}>
                      <td>{route.fromWarehouseCode}</td>
                      <td>{route.toWarehouseCode}</td>
                      <td>{route.enabled ? 'Enabled' : 'Disabled'}</td>
                      {canManage && (
                        <td>
                          <button type="button" className="btn btn--ghost" onClick={() => toggleRoute(route)}>
                            {route.enabled ? 'Disable' : 'Enable'}
                          </button>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </>
      )}
    </div>
  );
}
