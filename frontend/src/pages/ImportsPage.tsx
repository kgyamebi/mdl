import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { useAuth } from '../auth/AuthContext';
import { ImportActionPanel } from '../components/imports/ImportActionPanel';
import {
  createImport,
  fetchImport,
  fetchImports,
} from '../services/importsService';
import { fetchLocations } from '../services/locationsService';
import { fetchProducts } from '../services/productsService';
import type { ImportOrder, LocationSummary, Product } from '../types/api';

const STATUS_FILTERS: Array<{ value: string; label: string }> = [
  { value: '', label: 'All statuses' },
  { value: 'DRAFT', label: 'Draft' },
  { value: 'PENDING_APPROVAL', label: 'Pending approval' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'RECEIVING', label: 'Receiving' },
  { value: 'PARTIALLY_RECEIVED', label: 'Partially received' },
  { value: 'RECEIVED', label: 'Received' },
  { value: 'VERIFIED', label: 'Verified' },
  { value: 'CANCELLED', label: 'Cancelled' },
];

function formatQty(value: number): string {
  return new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 }).format(value);
}

function formatMoney(value: number, currencyCode: string): string {
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: currencyCode,
    maximumFractionDigits: 2,
  }).format(value);
}

function statusPillClass(status: string): string {
  switch (status) {
    case 'DRAFT':
      return '';
    case 'PENDING_APPROVAL':
      return 'pill--warning';
    case 'APPROVED':
    case 'RECEIVING':
      return 'pill--info';
    case 'PARTIALLY_RECEIVED':
      return 'pill--warning';
    case 'RECEIVED':
    case 'VERIFIED':
      return 'pill--ok';
    case 'CANCELLED':
      return 'pill--critical';
    default:
      return '';
  }
}

function formatStatus(status: string): string {
  return status
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

export function ImportsPage() {
  const { hasPermission, user } = useAuth();
  const canCreate = hasPermission('import:create');
  const currencyCode = user?.currencyCode ?? 'GHS';

  const [items, setItems] = useState<ImportOrder[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [selectedImport, setSelectedImport] = useState<ImportOrder | null>(null);
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [showCreateForm, setShowCreateForm] = useState(false);
  const [locations, setLocations] = useState<LocationSummary[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [supplierName, setSupplierName] = useState('');
  const [supplierReference, setSupplierReference] = useState('');
  const [destinationLocationId, setDestinationLocationId] = useState('');
  const [expectedArrivalDate, setExpectedArrivalDate] = useState('');
  const [productId, setProductId] = useState('');
  const [expectedQuantity, setExpectedQuantity] = useState('1');
  const [unitCost, setUnitCost] = useState('');
  const [notes, setNotes] = useState('');
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  const loadImports = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetchImports({
        status: statusFilter || undefined,
        page,
        size: 20,
      });
      setItems(response.items);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load imports');
    } finally {
      setLoading(false);
    }
  }, [page, statusFilter]);

  useEffect(() => {
    loadImports();
  }, [loadImports]);

  useEffect(() => {
    if (selectedId == null) {
      setSelectedImport(null);
      return;
    }

    let cancelled = false;
    setDetailLoading(true);

    fetchImport(selectedId)
      .then((importOrder) => {
        if (!cancelled) {
          setSelectedImport(importOrder);
        }
      })
      .catch((err: Error) => {
        if (!cancelled) {
          setError(err.message);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setDetailLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [selectedId]);

  useEffect(() => {
    if (!canCreate || !showCreateForm) {
      return;
    }

    Promise.all([fetchLocations(), fetchProducts({ size: 100 })])
      .then(([locationList, productPage]) => {
        setLocations(locationList);
        setProducts(productPage.items);
      })
      .catch(() => {
        setCreateError('Failed to load locations or products');
      });
  }, [canCreate, showCreateForm]);

  function handleImportUpdated(importOrder: ImportOrder) {
    setSelectedImport(importOrder);
    setItems((current) =>
      current.map((item) => (item.id === importOrder.id ? { ...item, status: importOrder.status } : item)),
    );
    loadImports();
  }

  async function handleCreate(event: FormEvent) {
    event.preventDefault();
    setCreateError(null);
    setCreating(true);

    try {
      const created = await createImport({
        supplierName: supplierName.trim(),
        supplierReference: supplierReference.trim() || undefined,
        destinationLocationId: Number(destinationLocationId),
        expectedArrivalDate: expectedArrivalDate || undefined,
        notes: notes.trim() || undefined,
        items: [
          {
            productId: Number(productId),
            expectedQuantity: Number(expectedQuantity),
            unitCost: unitCost ? Number(unitCost) : undefined,
          },
        ],
      });
      setShowCreateForm(false);
      setSupplierName('');
      setSupplierReference('');
      setDestinationLocationId('');
      setExpectedArrivalDate('');
      setProductId('');
      setExpectedQuantity('1');
      setUnitCost('');
      setNotes('');
      setSelectedId(created.id);
      await loadImports();
    } catch (err) {
      setCreateError(err instanceof Error ? err.message : 'Failed to create import');
    } finally {
      setCreating(false);
    }
  }

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="eyebrow">Receiving</p>
          <h1>Import orders</h1>
          <p className="subtitle">{totalElements} import(s)</p>
        </div>
        <div className="page__header-actions">
          {canCreate && (
            <button
              type="button"
              className="btn btn--primary"
              onClick={() => setShowCreateForm((current) => !current)}
            >
              {showCreateForm ? 'Close form' : 'New import'}
            </button>
          )}
          <button type="button" className="btn btn--ghost" onClick={loadImports} disabled={loading}>
            Refresh
          </button>
        </div>
      </header>

      {showCreateForm && canCreate && (
        <section className="panel">
          <h2>New import order</h2>
          <form className="form form--grid form--touch-friendly" onSubmit={handleCreate}>
            <label className="form__field">
              <span>Supplier name</span>
              <input
                type="text"
                className="input"
                value={supplierName}
                onChange={(event) => setSupplierName(event.target.value)}
                required
              />
            </label>
            <label className="form__field">
              <span>Supplier reference</span>
              <input
                type="text"
                className="input"
                value={supplierReference}
                onChange={(event) => setSupplierReference(event.target.value)}
              />
            </label>
            <label className="form__field">
              <span>Destination location</span>
              <select
                className="input"
                value={destinationLocationId}
                onChange={(event) => setDestinationLocationId(event.target.value)}
                required
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
              <span>Expected arrival</span>
              <input
                type="date"
                className="input"
                value={expectedArrivalDate}
                onChange={(event) => setExpectedArrivalDate(event.target.value)}
              />
            </label>
            <label className="form__field">
              <span>Product</span>
              <select
                className="input"
                value={productId}
                onChange={(event) => setProductId(event.target.value)}
                required
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
              <span>Expected quantity</span>
              <input
                type="number"
                min="0.01"
                step="any"
                className="input"
                value={expectedQuantity}
                onChange={(event) => setExpectedQuantity(event.target.value)}
                required
              />
            </label>
            <label className="form__field">
              <span>Unit cost ({currencyCode})</span>
              <input
                type="number"
                min="0"
                step="any"
                className="input"
                value={unitCost}
                onChange={(event) => setUnitCost(event.target.value)}
              />
            </label>
            <label className="form__field form__field--wide">
              <span>Notes</span>
              <input
                type="text"
                className="input"
                value={notes}
                onChange={(event) => setNotes(event.target.value)}
              />
            </label>
            {createError && <p className="form__error form__field--wide">{createError}</p>}
            <div className="form__field form__field--wide">
              <button type="submit" className="btn btn--primary" disabled={creating}>
                {creating ? 'Creating…' : 'Save draft'}
              </button>
            </div>
          </form>
        </section>
      )}

      <div className="toolbar">
        <select
          className="input input--compact"
          value={statusFilter}
          onChange={(event) => {
            setPage(0);
            setStatusFilter(event.target.value);
          }}
        >
          {STATUS_FILTERS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>

      {loading && <p className="muted">Loading imports…</p>}
      {error && <p className="form__error">{error}</p>}

      {!loading && !error && (
        <>
          <div className="table-wrap table-wrap--stacked">
            <table className="table table--stacked">
              <thead>
                <tr>
                  <th>Import</th>
                  <th>Supplier</th>
                  <th>Destination</th>
                  <th>Status</th>
                  <th>Items</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                {items.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="muted">
                      No imports match your filters.
                    </td>
                  </tr>
                ) : (
                  items.map((importOrder) => (
                    <tr
                      key={importOrder.id}
                      className={`table__row--clickable${
                        selectedId === importOrder.id ? ' table__row--selected' : ''
                      }`}
                      onClick={() => setSelectedId(importOrder.id)}
                    >
                      <td>
                        <strong>{importOrder.importNumber}</strong>
                      </td>
                      <td>{importOrder.supplierName}</td>
                      <td>{importOrder.destinationLocationCode}</td>
                      <td>
                        <span className={`pill ${statusPillClass(importOrder.status)}`}>
                          {formatStatus(importOrder.status)}
                        </span>
                      </td>
                      <td>{importOrder.items.length}</td>
                      <td>{new Date(importOrder.createdAt).toLocaleString()}</td>
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

          {selectedId != null && (
            <section className="panel transfer-detail">
              {detailLoading && <p className="muted">Loading import details…</p>}
              {selectedImport && (
                <>
                  <div className="panel__header">
                    <div>
                      <h2>{selectedImport.importNumber}</h2>
                      <p className="muted">
                        {selectedImport.supplierName} → {selectedImport.destinationLocationName}
                      </p>
                    </div>
                    <span className={`pill ${statusPillClass(selectedImport.status)}`}>
                      {formatStatus(selectedImport.status)}
                    </span>
                  </div>

                  {selectedImport.supplierReference && (
                    <p className="muted">Ref: {selectedImport.supplierReference}</p>
                  )}
                  {selectedImport.notes && <p>{selectedImport.notes}</p>}
                  {selectedImport.expectedArrivalDate && (
                    <p className="muted">
                      Expected arrival: {selectedImport.expectedArrivalDate}
                    </p>
                  )}

                  <div className="table-wrap">
                    <table className="table">
                      <thead>
                        <tr>
                          <th>Product</th>
                          <th>Expected</th>
                          <th>Received</th>
                          <th>Remaining</th>
                          <th>Unit cost</th>
                        </tr>
                      </thead>
                      <tbody>
                        {selectedImport.items.map((item) => (
                          <tr key={item.id}>
                            <td>
                              <strong>{item.productSku}</strong>
                              <div className="muted">{item.productName}</div>
                            </td>
                            <td>
                              {formatQty(item.expectedQuantity)} {item.unitOfMeasure}
                            </td>
                            <td>
                              {formatQty(item.receivedQuantity)} {item.unitOfMeasure}
                            </td>
                            <td>
                              {formatQty(item.remainingQuantity)} {item.unitOfMeasure}
                            </td>
                            <td>{formatMoney(item.unitCost, currencyCode)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>

                  <ImportActionPanel
                    importOrder={selectedImport}
                    onUpdated={handleImportUpdated}
                  />
                </>
              )}
            </section>
          )}
        </>
      )}
    </div>
  );
}
