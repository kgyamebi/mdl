import { useEffect, useState } from 'react';
import { fetchInventoryBalances } from '../services/inventoryService';
import type { InventoryBalance } from '../types/api';

function formatQty(value: number): string {
  return new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 }).format(value);
}

export function InventoryPage() {
  const [items, setItems] = useState<InventoryBalance[]>([]);
  const [search, setSearch] = useState('');
  const [lowStockOnly, setLowStockOnly] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);

      try {
        const response = await fetchInventoryBalances({ search, lowStockOnly, page, size: 20 });
        if (!cancelled) {
          setItems(response.items);
          setTotalPages(response.totalPages);
          setTotalElements(response.totalElements);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Failed to load inventory');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [search, lowStockOnly, page]);

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="eyebrow">Stock</p>
          <h1>Inventory balances</h1>
          <p className="subtitle">{totalElements} balance row(s)</p>
        </div>
      </header>

      <div className="toolbar">
        <input
          type="search"
          className="input"
          placeholder="Search product or location…"
          value={search}
          onChange={(event) => {
            setPage(0);
            setSearch(event.target.value);
          }}
        />
        <label className="checkbox">
          <input
            type="checkbox"
            checked={lowStockOnly}
            onChange={(event) => {
              setPage(0);
              setLowStockOnly(event.target.checked);
            }}
          />
          Low stock only
        </label>
      </div>

      {loading && <p className="muted">Loading inventory…</p>}
      {error && <p className="form__error">{error}</p>}

      {!loading && !error && (
        <>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Product</th>
                  <th>Location</th>
                  <th>On hand</th>
                  <th>Reserved</th>
                  <th>Available</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {items.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="muted">
                      No balances match your filters.
                    </td>
                  </tr>
                ) : (
                  items.map((row) => (
                    <tr key={row.id} className={row.belowReorderLevel ? 'row--warn' : ''}>
                      <td>
                        <strong>{row.productSku}</strong>
                        <div className="muted">{row.productName}</div>
                      </td>
                      <td>
                        <strong>{row.locationCode}</strong>
                        <div className="muted">{row.locationName}</div>
                      </td>
                      <td>
                        {formatQty(row.quantityOnHand)} {row.unitOfMeasure}
                      </td>
                      <td>{formatQty(row.quantityReserved)}</td>
                      <td>{formatQty(row.quantityAvailable)}</td>
                      <td>
                        {row.belowReorderLevel ? (
                          <span className="pill pill--warning">Low stock</span>
                        ) : (
                          <span className="pill pill--ok">OK</span>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {totalPages > 1 && (
            <div className="pager">
              <button
                type="button"
                className="btn btn--ghost"
                disabled={page === 0}
                onClick={() => setPage((current) => current - 1)}
              >
                Previous
              </button>
              <span className="muted">
                Page {page + 1} of {totalPages}
              </span>
              <button
                type="button"
                className="btn btn--ghost"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((current) => current + 1)}
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
