import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { useAuth } from '../auth/AuthContext';
import { DetailCloseButton } from '../components/layout/DetailCloseButton';
import { fetchLocations } from '../services/locationsService';
import {
  cancelStocktake,
  createStocktake,
  fetchStocktake,
  fetchStocktakes,
  submitStocktake,
  upsertStocktakeLine,
} from '../services/stocktakeService';
import type { LocationSummary, Stocktake } from '../types/api';

export function StocktakesPage() {
  const { hasPermission } = useAuth();
  const canCount = hasPermission('stock:count');

  const [items, setItems] = useState<Stocktake[]>([]);
  const [locations, setLocations] = useState<LocationSummary[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [selected, setSelected] = useState<Stocktake | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [locationId, setLocationId] = useState('');
  const [preload, setPreload] = useState(true);
  const [countInputs, setCountInputs] = useState<Record<number, string>>({});

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetchStocktakes('', 0, 50);
      setItems(response.items);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load stocktakes');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (canCount) {
      load();
      fetchLocations().then(setLocations).catch(() => {});
    }
  }, [canCount, load]);

  useEffect(() => {
    if (selectedId == null) {
      setSelected(null);
      return;
    }
    fetchStocktake(selectedId).then((data) => {
      setSelected(data);
      const inputs: Record<number, string> = {};
      data.lines.forEach((line) => {
        if (line.countedQuantity != null) {
          inputs[line.productId] = String(line.countedQuantity);
        }
      });
      setCountInputs(inputs);
    }).catch((err) => setError(err instanceof Error ? err.message : 'Failed to load stocktake'));
  }, [selectedId]);

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

  async function saveLine(productId: number) {
    if (!selected) {
      return;
    }
    const qty = Number(countInputs[productId]);
    if (!Number.isFinite(qty)) {
      return;
    }
    try {
      const updated = await upsertStocktakeLine(selected.id, { productId, countedQuantity: qty });
      setSelected(updated);
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save count');
    }
  }

  async function handleSubmit() {
    if (!selected) {
      return;
    }
    try {
      const updated = await submitStocktake(selected.id);
      setSelected(updated);
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to submit stocktake');
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
        <section className="panel"><p className="muted">You need the stock:count permission.</p></section>
      </div>
    );
  }

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="eyebrow">Operations</p>
          <h1>Stocktakes</h1>
          <p className="subtitle">Physical inventory counts</p>
        </div>
        <div className="page__header-actions">
          <button type="button" className="btn btn--primary" onClick={() => setShowCreate((v) => !v)}>
            {showCreate ? 'Cancel' : 'New count'}
          </button>
        </div>
      </header>

      {showCreate && (
        <section className="panel">
          <form className="form form--touch-friendly" onSubmit={handleCreate}>
            <label className="form__field">
              <span>Location</span>
              <select className="input" required value={locationId} onChange={(e) => setLocationId(e.target.value)}>
                <option value="">Select location</option>
                {locations.map((loc) => (
                  <option key={loc.id} value={loc.id}>{loc.code} — {loc.name}</option>
                ))}
              </select>
            </label>
            <label className="checkbox">
              <input type="checkbox" checked={preload} onChange={(e) => setPreload(e.target.checked)} />
              Preload expected balances
            </label>
            <button type="submit" className="btn btn--primary">Start stocktake</button>
          </form>
        </section>
      )}

      {loading && <p className="muted">Loading…</p>}
      {error && <p className="form__error">{error}</p>}

      {!loading && (
        <div className={`workspace-split${selectedId != null ? ' workspace-split--open' : ''}`}>
          <div className="workspace-split__list">
            <div className="table-wrap table-wrap--stacked table-wrap--scroll-hint">
              <table className="table table--stacked">
                <thead><tr><th>Count</th><th>Location</th><th>Status</th><th>Lines</th></tr></thead>
                <tbody>
                  {items.map((item) => (
                    <tr key={item.id} className={`table__row--clickable${selectedId === item.id ? ' table__row--selected' : ''}`} onClick={() => setSelectedId(item.id)} tabIndex={0} role="button">
                      <td data-label="Count"><strong>{item.stocktakeNumber}</strong></td>
                      <td data-label="Location">{item.locationCode}</td>
                      <td data-label="Status">{item.status}</td>
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
                  <p className="muted">{selected.locationName} · {selected.status}</p>
                </div>
                {selected.status === 'IN_PROGRESS' && (
                  <div className="page__header-actions">
                    <button type="button" className="btn btn--primary" onClick={handleSubmit}>Submit</button>
                    <button type="button" className="btn btn--ghost" onClick={handleCancel}>Cancel</button>
                  </div>
                )}
              </div>
              <div className="table-wrap">
                <table className="table">
                  <thead><tr><th>Product</th><th>Expected</th><th>Counted</th><th></th></tr></thead>
                  <tbody>
                    {selected.lines.map((line) => (
                      <tr key={line.id}>
                        <td>{line.productSku} — {line.productName}</td>
                        <td>{line.expectedQuantity}</td>
                        <td>
                          {selected.status === 'IN_PROGRESS' ? (
                            <input
                              className="input input--compact"
                              type="number"
                              min="0"
                              step="any"
                              value={countInputs[line.productId] ?? ''}
                              onChange={(e) => setCountInputs({ ...countInputs, [line.productId]: e.target.value })}
                            />
                          ) : (
                            line.countedQuantity ?? '—'
                          )}
                        </td>
                        <td>
                          {selected.status === 'IN_PROGRESS' && (
                            <button type="button" className="btn btn--ghost" onClick={() => saveLine(line.productId)}>Save</button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </aside>
          )}
        </div>
      )}
    </div>
  );
}
