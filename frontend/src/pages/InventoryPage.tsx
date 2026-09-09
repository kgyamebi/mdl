import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { useAuth } from '../auth/AuthContext';
import { ProductSearchSelect } from '../components/products/ProductSearchSelect';
import { fetchInventoryBalances, recordWarehouseStock } from '../services/inventoryService';
import { fetchLocations, fetchShops } from '../services/locationsService';
import type { InventoryBalance, LocationSummary, Product, Shop } from '../types/api';
import { formatLocationLabel } from '../utils/formatLocationLabel';

/** Quantity filter modes. Bounds are inclusive, which the labels state explicitly. */
type QuantityMode = 'any' | 'atMost' | 'atLeast' | 'between';

const QUANTITY_MODES: Array<{ value: QuantityMode; label: string }> = [
  { value: 'any', label: 'Any quantity' },
  { value: 'atMost', label: 'Quantity is at most…' },
  { value: 'atLeast', label: 'Quantity is at least…' },
  { value: 'between', label: 'Quantity is between…' },
];

const FILTER_DEBOUNCE_MS = 350;

function formatQty(value: number): string {
  return new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 }).format(value);
}

interface ShopGroup {
  shop: Shop;
  locationId: number;
  items: InventoryBalance[];
  total: number;
}

function parseBound(raw: string): number | undefined {
  const trimmed = raw.trim();
  if (trimmed === '') {
    return undefined;
  }
  const parsed = Number(trimmed);
  return Number.isFinite(parsed) ? parsed : undefined;
}

export function InventoryPage() {
  const { hasPermission } = useAuth();
  const canRecordStock = hasPermission('inventory:record:warehouse');

  const [items, setItems] = useState<InventoryBalance[]>([]);
  const [shopGroups, setShopGroups] = useState<ShopGroup[]>([]);
  const [locations, setLocations] = useState<LocationSummary[]>([]);
  const [shops, setShops] = useState<Shop[]>([]);
  const [locationsReady, setLocationsReady] = useState(false);
  const [locationCounts, setLocationCounts] = useState<Map<number, number>>(new Map());
  const [search, setSearch] = useState('');
  const [locationId, setLocationId] = useState('');
  const [lowStockOnly, setLowStockOnly] = useState(false);
  const [negativeStockOnly, setNegativeStockOnly] = useState(false);
  const [quantityMode, setQuantityMode] = useState<QuantityMode>('any');
  const [quantityLower, setQuantityLower] = useState('');
  const [quantityUpper, setQuantityUpper] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [showMovement, setShowMovement] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [movementProduct, setMovementProduct] = useState<Product | null>(null);
  const [movementForm, setMovementForm] = useState({
    locationId: '',
    direction: 'remove' as 'add' | 'remove',
    quantity: '1',
    reason: '',
  });

  // Typing a bound shouldn't fire a request per keystroke.
  const [debouncedBounds, setDebouncedBounds] = useState({ lower: '', upper: '' });

  useEffect(() => {
    const timer = setTimeout(
      () => setDebouncedBounds({ lower: quantityLower, upper: quantityUpper }),
      FILTER_DEBOUNCE_MS,
    );
    return () => clearTimeout(timer);
  }, [quantityLower, quantityUpper]);

  const quantityRange = useMemo(() => {
    const lower = parseBound(debouncedBounds.lower);
    const upper = parseBound(debouncedBounds.upper);
    switch (quantityMode) {
      case 'atMost':
        return { minQuantity: undefined, maxQuantity: lower };
      case 'atLeast':
        return { minQuantity: lower, maxQuantity: undefined };
      case 'between':
        return { minQuantity: lower, maxQuantity: upper };
      default:
        return { minQuantity: undefined, maxQuantity: undefined };
    }
  }, [quantityMode, debouncedBounds]);

  const balanceFilters = useMemo(
    () => ({
      search,
      lowStockOnly,
      negativeStockOnly,
      minQuantity: quantityRange.minQuantity,
      maxQuantity: quantityRange.maxQuantity,
    }),
    [search, lowStockOnly, negativeStockOnly, quantityRange],
  );

  const loadBalances = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      if (!locationId) {
        const groups = (
          await Promise.all(
            shops.map(async (shop) => {
              const shopLocationId = shop.location?.id;
              if (shopLocationId == null) {
                return null;
              }
              const response = await fetchInventoryBalances({
                ...balanceFilters,
                locationId: shopLocationId,
                page: 0,
                size: 4,
              });
              return {
                shop,
                locationId: shopLocationId,
                items: response.items,
                total: response.totalElements,
              } satisfies ShopGroup;
            }),
          )
        ).filter((group): group is ShopGroup => group != null);
        setShopGroups(groups);
        setItems([]);
        setTotalPages(0);
        setTotalElements(groups.reduce((sum, group) => sum + group.total, 0));
        return;
      }

      const response = await fetchInventoryBalances({
        ...balanceFilters,
        locationId: Number(locationId),
        page,
        size: 20,
      });
      setShopGroups([]);
      setItems(response.items);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load inventory');
    } finally {
      setLoading(false);
    }
  }, [balanceFilters, locationId, page, shops]);

  useEffect(() => {
    if (!locationId && shops.length === 0) {
      return;
    }
    void loadBalances();
  }, [loadBalances, locationId, shops.length]);

  const loadStockLocations = useCallback(async () => {
    try {
      const [shopList, locationList] = await Promise.all([fetchShops(), fetchLocations()]);
      const activeShops = shopList.filter((shop) => shop.status === 'ACTIVE');
      const stockLocations = locationList.filter(
        (location) => location.locationType === 'WAREHOUSE' || location.locationType === 'SHOP',
      );
      setShops(activeShops);
      setLocations(stockLocations);

      const countEntries = await Promise.all(
        stockLocations.map(async (location) => {
          const response = await fetchInventoryBalances({ locationId: location.id, size: 1 });
          return [location.id, response.totalElements] as const;
        }),
      );
      setLocationCounts(new Map(countEntries));
    } catch {
      // Keep any shops already loaded so a single count failure cannot hide Shop B.
    } finally {
      setLocationsReady(true);
    }
  }, []);

  useEffect(() => {
    void loadStockLocations();

    function reloadWhenVisible() {
      if (document.visibilityState === 'visible') {
        void loadStockLocations();
      }
    }
    document.addEventListener('visibilitychange', reloadWhenVisible);
    window.addEventListener('focus', reloadWhenVisible);
    return () => {
      document.removeEventListener('visibilitychange', reloadWhenVisible);
      window.removeEventListener('focus', reloadWhenVisible);
    };
  }, [loadStockLocations]);

  const selectedLocation = useMemo(
    () => locations.find((location) => String(location.id) === locationId) ?? null,
    [locations, locationId],
  );

  function selectLocation(nextId: string) {
    setPage(0);
    setLocationId(nextId);
  }

  function countFor(id: number | undefined): number | undefined {
    if (id == null) {
      return undefined;
    }
    return locationCounts.get(id);
  }

  function renderBalanceRows(rows: InventoryBalance[], options?: { hideLocation?: boolean }) {
    const hideLocation = options?.hideLocation ?? false;
    const colSpan = hideLocation ? 5 : 6;
    if (rows.length === 0) {
      return (
        <tr>
          <td colSpan={colSpan} className="muted">
            No balances match your filters.
          </td>
        </tr>
      );
    }
    return rows.map((row) => (
      <tr
        key={row.id}
        className={row.quantityOnHand < 0 ? 'row--critical' : row.belowReorderLevel ? 'row--warn' : ''}
      >
        <td data-label="Product">
          <strong>{row.productSku}</strong>
          <div className="muted">{row.productName}</div>
        </td>
        {!hideLocation && (
          <td data-label="Location">
            <strong>{row.locationCode}</strong>
            <div className="muted">{formatLocationLabel(row.locationName)}</div>
          </td>
        )}
        <td data-label="On hand" className="num">
          {formatQty(row.quantityOnHand)} {row.unitOfMeasure}
        </td>
        <td data-label="Reserved" className="num">{formatQty(row.quantityReserved)}</td>
        <td data-label="Available" className="num">{formatQty(row.quantityAvailable)}</td>
        <td data-label="Status">
          {row.quantityOnHand < 0 ? (
            <span className="pill pill--critical">Negative stock</span>
          ) : row.quantityOnHand === 0 ? (
            <span className="pill pill--warning">Out of stock</span>
          ) : row.belowReorderLevel ? (
            <span className="pill pill--warning">Low stock</span>
          ) : (
            <span className="pill pill--ok">OK</span>
          )}
        </td>
      </tr>
    ));
  }

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
    if (!movementProduct) {
      setError('Find the item you want to adjust.');
      return;
    }

    const signedQuantity = movementForm.direction === 'remove' ? -quantity : quantity;

    setSubmitting(true);
    setError(null);
    setSuccess(null);
    try {
      await recordWarehouseStock({
        locationId: Number(movementForm.locationId),
        productId: movementProduct.id,
        quantityChange: signedQuantity,
        reason: movementForm.reason.trim(),
      });
      setSuccess(
        movementForm.direction === 'remove'
          ? 'Stock removed and balances updated.'
          : 'Stock added and balances updated.',
      );
      setMovementForm({ locationId: '', direction: 'remove', quantity: '1', reason: '' });
      setMovementProduct(null);
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
          <p className="subtitle">
            {totalElements} balance row(s)
            {selectedLocation ? ` at ${selectedLocation.name}` : shops.length > 1 ? ` across ${shops.length} shops` : ''}
          </p>
        </div>
        {canRecordStock && (
          <div className="page__header-actions">
            <button type="button" className="btn btn--primary" onClick={() => setShowMovement((v) => !v)}>
              {showMovement ? 'Cancel' : 'Record stock in/out'}
            </button>
          </div>
        )}
      </header>

      <div className="shop-switcher" role="tablist" aria-label="Choose shop or location">
        <button
          type="button"
          role="tab"
          aria-selected={locationId === ''}
          className={`shop-switcher__tab${locationId === '' ? ' shop-switcher__tab--active' : ''}`}
          onClick={() => selectLocation('')}
        >
          All locations
        </button>
        {shops.map((shop) => {
          const shopLocationId = shop.location?.id;
          const selected = shopLocationId != null && locationId === String(shopLocationId);
          const count = countFor(shopLocationId);
          return (
            <button
              key={shop.id}
              type="button"
              role="tab"
              aria-selected={selected}
              className={`shop-switcher__tab${selected ? ' shop-switcher__tab--active' : ''}`}
              onClick={() => shopLocationId != null && selectLocation(String(shopLocationId))}
            >
              {shop.name}
              {count != null ? <span className="shop-switcher__count">{count}</span> : null}
            </button>
          );
        })}
        {locations.some((location) => location.locationType === 'WAREHOUSE') && (
          <div className="shop-switcher__warehouses">
            {locations
              .filter((location) => location.locationType === 'WAREHOUSE')
              .map((location) => {
                const selected = locationId === String(location.id);
                const count = countFor(location.id);
                return (
                  <button
                    key={location.id}
                    type="button"
                    role="tab"
                    aria-selected={selected}
                    className={`shop-switcher__tab shop-switcher__tab--warehouse${
                      selected ? ' shop-switcher__tab--active' : ''
                    }`}
                    onClick={() => selectLocation(String(location.id))}
                  >
                    {formatLocationLabel(location.name)}
                    {count != null ? <span className="shop-switcher__count">{count}</span> : null}
                  </button>
                );
              })}
          </div>
        )}
      </div>
      {locationsReady && shops.length === 0 && (
        <p className="hint">No shops are available yet. Refresh the page or add a shop in Locations.</p>
      )}

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
                    {location.code} — {formatLocationLabel(location.name)}
                  </option>
                ))}
              </select>
            </label>
            <div className="form__field">
              <label className="form__field-label" htmlFor="movement-product-search">
                Item
              </label>
              <ProductSearchSelect
                inputId="movement-product-search"
                value={movementProduct}
                onChange={setMovementProduct}
                placeholder="Type item name or code…"
              />
            </div>
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
        <label className="filter-field filter-field--grow">
          <span className="filter-field__label">Search</span>
          <input
            type="search"
            className="input"
            placeholder="Product name or code…"
            value={search}
            onChange={(event) => {
              setPage(0);
              setSearch(event.target.value);
            }}
          />
        </label>
        <label className="checkbox">
          <input
            type="checkbox"
            checked={lowStockOnly}
            onChange={(event) => {
              setPage(0);
              setLowStockOnly(event.target.checked);
              if (event.target.checked) {
                setNegativeStockOnly(false);
              }
            }}
          />
          Low stock only
        </label>
        <label className="checkbox">
          <input
            type="checkbox"
            checked={negativeStockOnly}
            onChange={(event) => {
              setPage(0);
              setNegativeStockOnly(event.target.checked);
              if (event.target.checked) {
                setLowStockOnly(false);
              }
            }}
          />
          Negative stock only
        </label>
      </div>

      <div className="toolbar toolbar--filters">
        <label className="filter-field">
          <span className="filter-field__label">Stock level</span>
          <select
            className="input input--compact"
            value={quantityMode}
            onChange={(event) => {
              setPage(0);
              setQuantityMode(event.target.value as QuantityMode);
              setQuantityLower('');
              setQuantityUpper('');
            }}
          >
            {QUANTITY_MODES.map((mode) => (
              <option key={mode.value} value={mode.value}>
                {mode.label}
              </option>
            ))}
          </select>
        </label>

        {quantityMode !== 'any' && (
          <label className="filter-field">
            <span className="filter-field__label">
              {quantityMode === 'between' ? 'From (inclusive)' : 'Quantity'}
            </span>
            <input
              type="number"
              step="any"
              className="input input--compact"
              placeholder="0"
              value={quantityLower}
              onChange={(event) => {
                setPage(0);
                setQuantityLower(event.target.value);
              }}
            />
          </label>
        )}

        {quantityMode === 'between' && (
          <label className="filter-field">
            <span className="filter-field__label">To (inclusive)</span>
            <input
              type="number"
              step="any"
              className="input input--compact"
              placeholder="100"
              value={quantityUpper}
              onChange={(event) => {
                setPage(0);
                setQuantityUpper(event.target.value);
              }}
            />
          </label>
        )}

        {quantityMode !== 'any' && (
          <button
            type="button"
            className="btn btn--ghost btn--compact"
            onClick={() => {
              setPage(0);
              setQuantityMode('any');
              setQuantityLower('');
              setQuantityUpper('');
            }}
          >
            Clear
          </button>
        )}
      </div>

      {loading && <p className="muted">Loading inventory…</p>}
      {error && <p className="form__error">{error}</p>}
      {success && <p className="form__success">{success}</p>}

      {!loading && !error && !locationId && shopGroups.length > 0 && (
        <div className="shop-groups">
          {shopGroups.map((group) => (
            <section key={group.shop.id} className="shop-group panel">
              <div className="shop-group__header">
                <div>
                  <h2>{group.shop.name}</h2>
                  <p className="muted">{group.total} item(s) at this shop</p>
                </div>
                <button
                  type="button"
                  className="btn btn--primary"
                  onClick={() => selectLocation(String(group.locationId))}
                  aria-label={`View all items at ${group.shop.name}`}
                >
                  View all
                </button>
              </div>
              <div className="table-wrap table-wrap--stacked table-wrap--flush">
                <table className="table table--stacked">
                  <thead>
                    <tr>
                      <th>Product</th>
                      <th className="num">On hand</th>
                      <th className="num">Reserved</th>
                      <th className="num">Available</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>{renderBalanceRows(group.items, { hideLocation: true })}</tbody>
                </table>
              </div>
            </section>
          ))}
        </div>
      )}

      {!loading && !error && locationId && (
        <>
          <div className="table-wrap table-wrap--stacked">
            <table className="table table--stacked">
              <thead>
                <tr>
                  <th>Product</th>
                  <th>Location</th>
                  <th className="num">On hand</th>
                  <th className="num">Reserved</th>
                  <th className="num">Available</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>{renderBalanceRows(items)}</tbody>
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
