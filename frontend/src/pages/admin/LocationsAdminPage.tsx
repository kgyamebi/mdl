import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { useAuth } from '../../auth/AuthContext';
import {
  createMainWarehouse,
  createShop,
  createTransferRoute,
  deactivateShop,
  deactivateWarehouse,
  deleteTransferRoute,
  fetchBusinessStructure,
  fetchTransferRoutes,
  fetchWarehouses,
  updateTransferRoute,
} from '../../services/locationsService';
import type { BusinessStructure, TransferRoute, Warehouse } from '../../types/api';

export function LocationsAdminPage() {
  const { hasPermission } = useAuth();
  const canManage = hasPermission('business:manage');

  const [structure, setStructure] = useState<BusinessStructure | null>(null);
  const [routes, setRoutes] = useState<TransferRoute[]>([]);
  const [allWarehouses, setAllWarehouses] = useState<Warehouse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showAddShop, setShowAddShop] = useState(false);
  const [showAddWarehouse, setShowAddWarehouse] = useState(false);
  const [showAddRoute, setShowAddRoute] = useState(false);
  const [shopForm, setShopForm] = useState({ name: '', code: '' });
  const [warehouseForm, setWarehouseForm] = useState({ name: '', code: '', warehouseType: 'MAIN' as 'MAIN' | 'SHOP' });
  const [routeForm, setRouteForm] = useState({ fromWarehouseId: '', toWarehouseId: '' });

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [structureData, routeData, warehouseData] = await Promise.all([
        fetchBusinessStructure(),
        fetchTransferRoutes(),
        fetchWarehouses(),
      ]);
      setStructure(structureData);
      setRoutes(routeData);
      setAllWarehouses(warehouseData);
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

  async function handleAddShop(event: FormEvent) {
    event.preventDefault();
    if (!canManage) {
      return;
    }
    try {
      await createShop({
        name: shopForm.name.trim(),
        code: shopForm.code.trim() || undefined,
      });
      setShopForm({ name: '', code: '' });
      setShowAddShop(false);
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create shop');
    }
  }

  async function handleAddWarehouse(event: FormEvent) {
    event.preventDefault();
    if (!canManage) {
      return;
    }
    try {
      await createMainWarehouse({
        name: warehouseForm.name.trim(),
        code: warehouseForm.code.trim() || undefined,
        warehouseType: warehouseForm.warehouseType,
        restricted: warehouseForm.warehouseType === 'MAIN',
      });
      setWarehouseForm({ name: '', code: '', warehouseType: 'MAIN' });
      setShowAddWarehouse(false);
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create warehouse');
    }
  }

  async function handleAddRoute(event: FormEvent) {
    event.preventDefault();
    if (!canManage) {
      return;
    }
    try {
      await createTransferRoute({
        fromWarehouseId: Number(routeForm.fromWarehouseId),
        toWarehouseId: Number(routeForm.toWarehouseId),
      });
      setRouteForm({ fromWarehouseId: '', toWarehouseId: '' });
      setShowAddRoute(false);
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create route');
    }
  }

  async function removeShop(id: number, name: string) {
    if (!canManage || !window.confirm(`Deactivate shop "${name}"? Its linked warehouse will also be deactivated.`)) {
      return;
    }
    try {
      await deactivateShop(id);
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to deactivate shop');
    }
  }

  async function removeWarehouse(id: number, name: string) {
    if (!canManage || !window.confirm(`Deactivate warehouse "${name}"?`)) {
      return;
    }
    try {
      await deactivateWarehouse(id);
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to deactivate warehouse');
    }
  }

  async function removeRoute(id: number) {
    if (!canManage || !window.confirm('Delete this transfer route?')) {
      return;
    }
    try {
      await deleteTransferRoute(id);
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete route');
    }
  }

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="eyebrow">Administration</p>
          <h1>Locations & routes</h1>
          <p className="subtitle">Add or remove shops, warehouses, and transfer routes</p>
          {canManage && (
            <p className="muted">Only the business owner can change business structure. Managers handle stock through inventory and transfers.</p>
          )}
        </div>
        {canManage && (
          <div className="page__header-actions">
            <button type="button" className="btn btn--ghost" onClick={() => setShowAddShop((v) => !v)}>
              {showAddShop ? 'Cancel shop' : 'Add shop'}
            </button>
            <button type="button" className="btn btn--ghost" onClick={() => setShowAddWarehouse((v) => !v)}>
              {showAddWarehouse ? 'Cancel warehouse' : 'Add warehouse'}
            </button>
            <button type="button" className="btn btn--primary" onClick={() => setShowAddRoute((v) => !v)}>
              {showAddRoute ? 'Cancel route' : 'Add route'}
            </button>
          </div>
        )}
      </header>

      {loading && <p className="muted">Loading…</p>}
      {error && <p className="form__error">{error}</p>}

      {canManage && showAddShop && (
        <section className="panel">
          <h2>New shop</h2>
          <form className="form form--touch-friendly" onSubmit={handleAddShop}>
            <label className="form__field">
              <span>Shop name</span>
              <input className="input" required value={shopForm.name} onChange={(e) => setShopForm({ ...shopForm, name: e.target.value })} />
            </label>
            <label className="form__field">
              <span>Code (optional)</span>
              <input className="input" placeholder="Auto-generated from name" value={shopForm.code} onChange={(e) => setShopForm({ ...shopForm, code: e.target.value })} />
            </label>
            <button type="submit" className="btn btn--primary">Create shop</button>
          </form>
        </section>
      )}

      {canManage && showAddWarehouse && (
        <section className="panel">
          <h2>New warehouse</h2>
          <form className="form form--touch-friendly" onSubmit={handleAddWarehouse}>
            <label className="form__field">
              <span>Warehouse type</span>
              <select
                className="input"
                value={warehouseForm.warehouseType}
                onChange={(e) => setWarehouseForm({ ...warehouseForm, warehouseType: e.target.value as 'MAIN' | 'SHOP' })}
              >
                <option value="MAIN">Main warehouse (central hub)</option>
                <option value="SHOP">Shop / branch warehouse</option>
              </select>
            </label>
            <label className="form__field">
              <span>Warehouse name</span>
              <input className="input" required value={warehouseForm.name} onChange={(e) => setWarehouseForm({ ...warehouseForm, name: e.target.value })} />
            </label>
            <label className="form__field">
              <span>Code (optional)</span>
              <input className="input" placeholder="Auto-generated from name" value={warehouseForm.code} onChange={(e) => setWarehouseForm({ ...warehouseForm, code: e.target.value })} />
            </label>
            <button type="submit" className="btn btn--primary">Create warehouse</button>
          </form>
        </section>
      )}

      {canManage && showAddRoute && (
        <section className="panel">
          <h2>New transfer route</h2>
          <form className="form form--touch-friendly" onSubmit={handleAddRoute}>
            <label className="form__field">
              <span>From warehouse</span>
              <select className="input" required value={routeForm.fromWarehouseId} onChange={(e) => setRouteForm({ ...routeForm, fromWarehouseId: e.target.value })}>
                <option value="">Select…</option>
                {allWarehouses.map((w) => (
                  <option key={w.id} value={w.id}>{w.code} — {w.name}</option>
                ))}
              </select>
            </label>
            <label className="form__field">
              <span>To warehouse</span>
              <select className="input" required value={routeForm.toWarehouseId} onChange={(e) => setRouteForm({ ...routeForm, toWarehouseId: e.target.value })}>
                <option value="">Select…</option>
                {allWarehouses.map((w) => (
                  <option key={w.id} value={w.id}>{w.code} — {w.name}</option>
                ))}
              </select>
            </label>
            <button type="submit" className="btn btn--primary">Create route</button>
          </form>
        </section>
      )}

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
                    <li key={w.id} className="list__item list__item--actions">
                      <div>
                        <strong>{w.code}</strong>
                        <span className="muted">{w.name}</span>
                      </div>
                      {canManage && (
                        <button type="button" className="btn btn--ghost btn--sm" onClick={() => removeWarehouse(w.id, w.name)}>
                          Remove
                        </button>
                      )}
                    </li>
                  ))}
                </ul>
              </div>
              <div>
                <h3 className="panel__subheading">Shops</h3>
                <ul className="list">
                  {structure.shops.map((s) => (
                    <li key={s.id} className="list__item list__item--actions">
                      <div>
                        <strong>{s.code}</strong>
                        <span className="muted">{s.name}</span>
                      </div>
                      {canManage && (
                        <button type="button" className="btn btn--ghost btn--sm" onClick={() => removeShop(s.id, s.name)}>
                          Remove
                        </button>
                      )}
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          </section>

          <section className="panel">
            <h2>Transfer routes</h2>
            <div className="table-wrap table-wrap--stacked table-wrap--scroll-hint">
              <table className="table table--stacked">
                <thead>
                  <tr>
                    <th>From</th>
                    <th>To</th>
                    <th>Status</th>
                    {canManage && <th>Actions</th>}
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
                          <button type="button" className="btn btn--ghost btn--sm" onClick={() => toggleRoute(route)}>
                            {route.enabled ? 'Disable' : 'Enable'}
                          </button>
                          <button type="button" className="btn btn--ghost btn--sm" onClick={() => removeRoute(route.id)}>
                            Delete
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
