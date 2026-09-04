import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { useAuth } from '../auth/AuthContext';
import { fetchInventoryBalances, recordWarehouseStock } from '../services/inventoryService';
import { fetchLocations } from '../services/locationsService';
import { fetchProducts } from '../services/productsService';
import type { InventoryBalance, LocationSummary, Product } from '../types/api';

function formatQty(value: number): string {
  return new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 }).format(value);
}

export function InventoryPage() {
  const { hasPermission } = useAuth();
  const canRecordStock = hasPermission('inventory:record:warehouse');

  const [items, setItems] = useState<InventoryBalance[]>([]);
  const [locations, setLocations] = useState<LocationSummary[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [search, setSearch] = useState('');
  const [lowStockOnly, setLowStockOnly] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [showMovement, setShowMovement] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [movementForm, setMovementForm] = useState({
    locationId: '',
    productId: '',
    direction: 'remove' as 'add' | 'remove',
    quantity: '1',
    reason: '',
  });

  const loadBalances = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetchInventoryBalances({ search, lowStockOnly, page, size: 20 });
      setItems(response.items);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load inventory');
    } finally {
      setLoading(false);
    }
  }, [search, lowStockOnly, page]);

  useEffect(() => {
    loadBalances();
  }, [loadBalances]);

  useEffect(() => {
    if (!canRecordStock) {
      return;
    }
    Promise.all([
      fetchLocations(),
      fetchProducts({ status: 'ACTIVE', size: 500, page: 0 }),
    ])
      .then(([locationList, productResponse]) => {
        setLocations(locationList.filter((l) => l.locationType === 'WAREHOUSE' || l.locationType === 'SHOP'));
        setProducts(productResponse.items);
      })
      .catch(() => {});
  }, [canRecordStock]);

  async function handleStockMovement(event: FormEvent) {
    event.preventDefault();
    if (!canRecordStock) {
      return;
    }

    const quantity = Number(movementForm.quantity);
    if (!Number.isFinite(quantity) || quantity <= 0) {
      setError('Enter a valid quantity greater than zero.');
      return;
    }

    const signedQuantity = movementForm.direction === 'remove' ? -quantity : quantity;

    setSubmitting(true);
    setError(null);
    setSuccess(null);
    try {
      await recordWarehouseStock({
        locationId: Number(movementForm.locationId),
        productId: Number(movementForm.productId),
        quantityChange: signedQuantity,
        reason: movementForm.reason.trim(),
      });
      setSuccess(
        movementForm.direction === 'remove'
          ? 'Stock removed and balances updated.'
          : 'Stock added and balances updated.',
      );
      setMovementForm({ locationId: '', productId: '', direction: 'remove', quantity: '1', reason: '' });
      setShowMovement(false);
      loadBalances();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update stock');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="eyebrow">Stock</p>
          <h1>Inventory balances</h1>
          <p className="subtitle">{totalElements} balance row(s)</p>
        </div>
        {canRecordStock && (
          <div className="page__header-actions">
            <button type="button" className="btn btn--primary" onClick={() => setShowMovement((v) => !v)}>
              {showMovement ? 'Cancel' : 'Record stock in/out'}
            </button>
          </div>
        )}
      </header>

      {canRecordStock && showMovement && (
        <section className="panel">
          <h2>Record warehouse stock</h2>
          <p className="muted">Log items added or removed at any shop or warehouse. Stock updates immediately.</p>
          <form className="stock-movement-form" onSubmit={handleStockMovement}>
            <label className="form__field">
              <span>Location</span>
              <select
                className="input"
                required
                value={movementForm.locationId}
                onChange={(e) => setMovementForm({ ...movementForm, locationId: e.target.value })}
              >
                <option value="">Select location…</option>
                {locations.map((location) => (
                  <option key={location.id} value={location.id}>
                    {location.code} — {location.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="form__field">
              <span>Product</span>
              <select
                className="input"
                required
                value={movementForm.productId}
                onChange={(e) => setMovementForm({ ...movementForm, productId: e.target.value })}
              >
                <option value="">Select product…</option>
                {products.map((product) => (
                  <option key={product.id} value={product.id}>
                    {product.sku} — {product.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="form__field">
              <span>Action</span>
              <select
                className="input"
                value={movementForm.direction}
                onChange={(e) => setMovementForm({ ...movementForm, direction: e.target.value as 'add' | 'remove' })}
              >
                <option value="remove">Remove from stock</option>
                <option value="add">Add to stock</option>
              </select>
            </label>
            <label className="form__field">
              <span>Quantity</span>
              <input
                className="input"
                type="number"
                min="0.01"
                step="any"
                required
                value={movementForm.quantity}
                onChange={(e) => setMovementForm({ ...movementForm, quantity: e.target.value })}
              />
            </label>
            <label className="form__field form__field--wide">
              <span>Reason / notes</span>
              <input
                className="input"
                required
                placeholder="e.g. Taken for Shop B delivery"
                value={movementForm.reason}
                onChange={(e) => setMovementForm({ ...movementForm, reason: e.target.value })}
              />
            </label>
            <div className="form__field form__field--wide">
              <button type="submit" className="btn btn--primary" disabled={submitting}>
                {submitting ? 'Saving…' : 'Update stock'}
              </button>
            </div>
          </form>
        </section>
      )}

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
      {success && <p className="form__success">{success}</p>}

      {!loading && !error && (
        <>
          <div className="table-wrap table-wrap--stacked">
            <table className="table table--stacked">
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
                      <td data-label="Product">
                        <strong>{row.productSku}</strong>
                        <div className="muted">{row.productName}</div>
                      </td>
                      <td data-label="Location">
                        <strong>{row.locationCode}</strong>
                        <div className="muted">{row.locationName}</div>
                      </td>
                      <td data-label="On hand">
                        {formatQty(row.quantityOnHand)} {row.unitOfMeasure}
                      </td>
                      <td data-label="Reserved">{formatQty(row.quantityReserved)}</td>
                      <td data-label="Available">{formatQty(row.quantityAvailable)}</td>
                      <td data-label="Status">
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
