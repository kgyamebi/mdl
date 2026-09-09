import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { useAuth } from '../auth/AuthContext';
import { DetailCloseButton } from '../components/layout/DetailCloseButton';
import { ProductSearchSelect } from '../components/products/ProductSearchSelect';
import { fetchShops, fetchWarehouses } from '../services/locationsService';
import {
  approveStocktake,
  cancelStocktake,
  createStocktake,
  fetchStocktake,
  fetchStocktakes,
  submitStocktake,
  upsertStocktakeLine,
  upsertStocktakeLines,
} from '../services/stocktakeService';
import type { LocationSummary, Product, Stocktake, StocktakeLine } from '../types/api';
import { formatLocationLabel } from '../utils/formatLocationLabel';

const STATUS_FILTERS: Array<{ value: string; label: string }> = [
  { value: '', label: 'All statuses' },
  { value: 'IN_PROGRESS', label: 'In progress' },
  { value: 'SUBMITTED', label: 'Submitted' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'CANCELLED', label: 'Cancelled' },
];

function formatQty(value: number | null | undefined): string {
  if (value == null || !Number.isFinite(value)) {
    return '—';
  }
  return new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 }).format(value);
}

function statusPillClass(status: string): string {
  switch (status) {
    case 'IN_PROGRESS':
      return 'pill--info';
    case 'SUBMITTED':
      return 'pill--warning';
    case 'COMPLETED':
      return 'pill--ok';
    case 'CANCELLED':
      return 'pill--critical';
    default:
      return '';
  }
}

export function StocktakesPage() {
  const { hasPermission, user } = useAuth();
  const canCount = hasPermission('stock:count');
  const canApprove = hasPermission('stocktake:approve') || hasPermission('inventory:adjust');

  const [items, setItems] = useState<Stocktake[]>([]);
  const [countLocations, setCountLocations] = useState<LocationSummary[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [selected, setSelected] = useState<Stocktake | null>(null);
  const [statusFilter, setStatusFilter] = useState('');
  const [lineSearch, setLineSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [locationId, setLocationId] = useState('');
  const [preload, setPreload] = useState(true);
  const [countInputs, setCountInputs] = useState<Record<number, string>>({});
  const [addProduct, setAddProduct] = useState<Product | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetchStocktakes(statusFilter, 0, 50);
      setItems(response.items);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load stocktakes');
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => {
    if (!canCount) {
      return;
    }
    load();
    Promise.all([fetchShops(), fetchWarehouses()])
      .then(([shops, warehouses]) => {
        const byId = new Map<number, LocationSummary>();
        shops
          .filter((shop) => shop.canOperate !== false && shop.location)
          .forEach((shop) => {
            byId.set(shop.location!.id, shop.location!);
          });
        const canCountRestricted =
          user?.roles.includes('OWNER') || hasPermission('inventory:view:all');
        warehouses
          .filter((warehouse) => warehouse.status === 'ACTIVE' && warehouse.location)
          .filter((warehouse) => warehouse.warehouseType !== 'SHOP')
          .filter((warehouse) => !warehouse.restricted || canCountRestricted)
          .forEach((warehouse) => {
            byId.set(warehouse.location!.id, warehouse.location!);
          });
        setCountLocations([...byId.values()].sort((a, b) => a.name.localeCompare(b.name)));
      })
      .catch(() => {});
  }, [canCount, hasPermission, load, user?.roles]);

  useEffect(() => {
    if (selectedId == null) {
      setSelected(null);
      return;
    }
    fetchStocktake(selectedId)
      .then((data) => {
        setSelected(data);
        const inputs: Record<number, string> = {};
        data.lines.forEach((line) => {
          if (line.countedQuantity != null) {
            inputs[line.productId] = String(line.countedQuantity);
          }
        });
        setCountInputs(inputs);
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load stocktake'));
  }, [selectedId]);

  const visibleLines = useMemo(() => {
    if (!selected) {
      return [];
    }
    const term = lineSearch.trim().toLowerCase();
    if (!term) {
      return selected.lines;
    }
    return selected.lines.filter((line) =>
      `${line.productSku} ${line.productName}`.toLowerCase().includes(term),
    );
  }, [lineSearch, selected]);

  const countedLineCount = selected
    ? selected.lines.filter((line) => countInputs[line.productId]?.trim()).length
    : 0;

  async function handleCreate(event: FormEvent) {
    event.preventDefault();
    try {
      const created = await createStocktake({
        locationId: Number(locationId),
        preloadBalances: preload,
      });
      setShowCreate(false);
      setSelectedId(created.id);
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create stocktake');
    }
  }

  function applyUpdated(updated: Stocktake) {
    setSelected(updated);
    const inputs: Record<number, string> = { ...countInputs };
    updated.lines.forEach((line) => {
      if (line.countedQuantity != null) {
        inputs[line.productId] = String(line.countedQuantity);
      }
    });
    setCountInputs(inputs);
    load();
  }

  async function flushUnsavedCounts(): Promise<Stocktake | null> {
    if (!selected || selected.status !== 'IN_PROGRESS') {
      return selected;
    }
    const items = selected.lines.flatMap((line) => {
      const raw = countInputs[line.productId]?.trim();
      if (!raw) {
        return [];
      }
      const qty = Number(raw);
      if (!Number.isFinite(qty) || qty < 0) {
        return [];
      }
      if (line.countedQuantity != null && Number(line.countedQuantity) === qty) {
        return [];
      }
      return [{ productId: line.productId, countedQuantity: qty }];
    });
    if (items.length === 0) {
      return selected;
    }
    const updated = await upsertStocktakeLines(selected.id, items);
    applyUpdated(updated);
    return updated;
  }

  async function saveLine(line: StocktakeLine) {
    if (!selected) {
      return;
    }
    const qty = Number(countInputs[line.productId]);
    if (!Number.isFinite(qty) || qty < 0) {
      setError('Enter a valid counted quantity.');
      return;
    }
    setSaving(true);
    try {
      const updated = await upsertStocktakeLine(selected.id, { productId: line.productId, countedQuantity: qty });
      applyUpdated(updated);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save count');
    } finally {
      setSaving(false);
    }
  }

  async function addSelectedProduct() {
    if (!selected || !addProduct) {
      return;
    }
    setSaving(true);
    try {
      const updated = await upsertStocktakeLine(selected.id, {
        productId: addProduct.id,
        countedQuantity: 0,
      });
      applyUpdated(updated);
      setAddProduct(null);
      setCountInputs((current) => ({ ...current, [addProduct.id]: '0' }));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to add item');
    } finally {
      setSaving(false);
    }
  }

  async function handleSubmit() {
    if (!selected) {
      return;
    }
    const uncounted = selected.lines.filter((line) => !countInputs[line.productId]?.trim()).length;
    let treatUncounted = false;
    if (uncounted > 0) {
      treatUncounted = window.confirm(
        `${uncounted} item(s) have not been counted. Treat them as matching the system quantity and submit?`,
      );
      if (!treatUncounted) {
        return;
      }
    }
    setSaving(true);
    setError(null);
    try {
      await flushUnsavedCounts();
      const updated = await submitStocktake(selected.id, treatUncounted);
      applyUpdated(updated);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to submit stocktake');
    } finally {
      setSaving(false);
    }
  }

  async function handleApprove() {
    if (!selected) {
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const updated = await approveStocktake(selected.id);
      applyUpdated(updated);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to approve stocktake');
    } finally {
      setSaving(false);
    }
  }

  async function handleCancel() {
    if (!selected) {
      return;
    }
    const reason = window.prompt('Cancel reason?');
    if (!reason?.trim()) {
      return;
    }
    try {
      await cancelStocktake(selected.id, reason.trim());
      setSelectedId(null);
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to cancel stocktake');
    }
  }

  if (!canCount) {
    return (
      <div className="page">
        <section className="panel">
          <p className="muted">You do not have permission to run stocktakes.</p>
        </section>
      </div>
    );
  }

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="eyebrow">Operations</p>
          <h1>Stocktakes</h1>
          <p className="subtitle">Count stock at a shop or warehouse, then submit for approval</p>
        </div>
        <div className="page__header-actions">
          <button type="button" className="btn btn--primary" onClick={() => setShowCreate((value) => !value)}>
            {showCreate ? 'Close form' : 'New count'}
          </button>
        </div>
      </header>

      {showCreate && (
        <section className="panel">
          <h2>Start stocktake</h2>
          <p className="muted">
            Shop counts use the shop floor (the same stock used for sales). Main warehouses appear separately.
            Preload includes every item on hand at that location.
          </p>
          <form className="form form--touch-friendly" onSubmit={handleCreate}>
            <label className="form__field" htmlFor="stocktake-location">
              <span>Location</span>
              <select
                id="stocktake-location"
                className="input"
                required
                value={locationId}
                onChange={(event) => setLocationId(event.target.value)}
              >
                <option value="">Select location…</option>
                {countLocations.map((location) => (
                  <option key={location.id} value={location.id}>
                    {formatLocationLabel(location.name)} — {location.code}
                  </option>
                ))}
              </select>
            </label>
            <label className="checkbox">
              <input type="checkbox" checked={preload} onChange={(event) => setPreload(event.target.checked)} />
              Preload expected balances
            </label>
            <button type="submit" className="btn btn--primary">Start stocktake</button>
          </form>
        </section>
      )}

      <div className="toolbar">
        <select
          className="input input--compact"
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value)}
          aria-label="Stocktake status"
        >
          {STATUS_FILTERS.map((option) => (
            <option key={option.value} value={option.value}>{option.label}</option>
          ))}
        </select>
      </div>

      {loading && <p className="muted">Loading stocktakes…</p>}
      {error && <p className="form__error">{error}</p>}

      {!loading && items.length === 0 && (
        <p className="muted">No stocktakes yet. Start a count at your shop or warehouse.</p>
      )}

      {!loading && items.length > 0 && (
        <div className={`workspace-split${selectedId != null ? ' workspace-split--open' : ''}`}>
          <div className="workspace-split__list">
            <div className="table-wrap table-wrap--stacked table-wrap--scroll-hint">
              <table className="table table--stacked">
                <thead>
                  <tr>
                    <th>Count</th>
                    <th>Location</th>
                    <th>Status</th>
                    <th>Lines</th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((item) => (
                    <tr
                      key={item.id}
                      className={`table__row--clickable${selectedId === item.id ? ' table__row--selected' : ''}`}
                      onClick={() => setSelectedId(item.id)}
                    >
                      <td data-label="Count"><strong>{item.stocktakeNumber}</strong></td>
                      <td data-label="Location">{formatLocationLabel(item.locationName)}</td>
                      <td data-label="Status">
                        <span className={`pill ${statusPillClass(item.status)}`}>{item.status.replace(/_/g, ' ')}</span>
                      </td>
                      <td data-label="Lines">{item.lineCount}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {selected && (
            <aside className="workspace-split__detail panel">
              <DetailCloseButton onClose={() => setSelectedId(null)} />
              <div className="panel__header">
                <div>
                  <h2>{selected.stocktakeNumber}</h2>
                  <p className="muted">
                    {formatLocationLabel(selected.locationName)} · {selected.status.replace(/_/g, ' ')}
                    {selected.status === 'IN_PROGRESS' ? ` · ${countedLineCount} of ${selected.lines.length} counted` : ''}
                    {selected.status !== 'IN_PROGRESS' && selected.varianceLineCount != null
                      ? ` · ${selected.varianceLineCount} variance`
                      : ''}
                  </p>
                </div>
                <div className="page__header-actions">
                  {selected.status === 'IN_PROGRESS' && (
                    <>
                      <button type="button" className="btn btn--primary" onClick={handleSubmit} disabled={saving}>
                        Submit
                      </button>
                      <button type="button" className="btn btn--ghost" onClick={handleCancel}>Cancel</button>
                    </>
                  )}
                  {selected.status === 'SUBMITTED' && canApprove && (
                    <button type="button" className="btn btn--primary" onClick={handleApprove} disabled={saving}>
                      Approve and post
                    </button>
                  )}
                  {selected.status === 'SUBMITTED' && (
                    <button type="button" className="btn btn--ghost" onClick={handleCancel}>Cancel</button>
                  )}
                </div>
              </div>

              {selected.status === 'IN_PROGRESS' && (
                <div className="form form--touch-friendly">
                  <label className="form__field">
                    <span>Find a line</span>
                    <input
                      className="input"
                      type="search"
                      placeholder="Type item name or code…"
                      value={lineSearch}
                      onChange={(event) => setLineSearch(event.target.value)}
                    />
                  </label>
                  <div className="form__field">
                    <span>Add an item not on the list</span>
                    <ProductSearchSelect
                      value={addProduct}
                      onChange={setAddProduct}
                      placeholder="Type to add a product…"
                    />
                    <button
                      type="button"
                      className="btn btn--ghost"
                      disabled={!addProduct || saving}
                      onClick={addSelectedProduct}
                    >
                      Add to count
                    </button>
                  </div>
                </div>
              )}

              <div className="table-wrap table-wrap--scroll-hint">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Product</th>
                      <th>Expected</th>
                      <th>Counted</th>
                      <th>Variance</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    {visibleLines.map((line) => {
                      const counted = countInputs[line.productId];
                      const countedNumber = counted?.trim() ? Number(counted) : null;
                      const liveVariance =
                        countedNumber != null && Number.isFinite(countedNumber)
                          ? countedNumber - line.expectedQuantity
                          : line.variance;
                      return (
                        <tr key={line.id}>
                          <td>
                            <strong>{line.productSku}</strong>
                            <div className="muted">{line.productName}</div>
                          </td>
                          <td className="num">{formatQty(line.expectedQuantity)}</td>
                          <td>
                            {selected.status === 'IN_PROGRESS' ? (
                              <input
                                className="input input--compact"
                                type="number"
                                min="0"
                                step="any"
                                aria-label={`Counted quantity for ${line.productSku}`}
                                value={countInputs[line.productId] ?? ''}
                                onChange={(event) =>
                                  setCountInputs({ ...countInputs, [line.productId]: event.target.value })
                                }
                              />
                            ) : (
                              formatQty(line.countedQuantity)
                            )}
                          </td>
                          <td className="num">{formatQty(liveVariance)}</td>
                          <td>
                            {selected.status === 'IN_PROGRESS' && (
                              <button type="button" className="btn btn--ghost btn--sm" onClick={() => saveLine(line)} disabled={saving}>
                                Save
                              </button>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
              {visibleLines.length === 0 && (
                <p className="muted">No lines match that search.</p>
              )}
            </aside>
          )}
        </div>
      )}
    </div>
  );
}
