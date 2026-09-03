import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { useAuth } from '../auth/AuthContext';
import { TransferActionPanel } from '../components/transfers/TransferActionPanel';
import { fetchWarehouses } from '../services/locationsService';
import { fetchProducts } from '../services/productsService';
import { createTransfer, fetchTransfer, fetchTransfers } from '../services/transfersService';
import type { Product, StockTransfer, Warehouse } from '../types/api';

const STATUS_FILTERS: Array<{ value: string; label: string }> = [
  { value: '', label: 'All statuses' },
  { value: 'REQUESTED', label: 'Requested' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'DISPATCHED', label: 'Dispatched' },
  { value: 'PARTIALLY_RECEIVED', label: 'Partially received' },
  { value: 'RECEIVED', label: 'Received' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'CANCELLED', label: 'Cancelled' },
];

function formatQty(value: number): string {
  return new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 }).format(value);
}

function statusPillClass(status: string): string {
  switch (status) {
    case 'REQUESTED':
      return 'pill--warning';
    case 'APPROVED':
    case 'DISPATCHED':
      return 'pill--info';
    case 'RECEIVED':
      return 'pill--ok';
    case 'PARTIALLY_RECEIVED':
      return 'pill--warning';
    case 'REJECTED':
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

export function TransfersPage() {
  const { hasAnyPermission } = useAuth();
  const canCreate = hasAnyPermission('transfer:create', 'stock:request');

  const [items, setItems] = useState<StockTransfer[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [selectedTransfer, setSelectedTransfer] = useState<StockTransfer | null>(null);
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [showCreateForm, setShowCreateForm] = useState(false);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [fromWarehouseId, setFromWarehouseId] = useState('');
  const [toWarehouseId, setToWarehouseId] = useState('');
  const [productId, setProductId] = useState('');
  const [quantity, setQuantity] = useState('1');
  const [notes, setNotes] = useState('');
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  const loadTransfers = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetchTransfers({
        status: statusFilter || undefined,
        page,
        size: 20,
      });
      setItems(response.items);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load transfers');
    } finally {
      setLoading(false);
    }
  }, [page, statusFilter]);

  useEffect(() => {
    loadTransfers();
  }, [loadTransfers]);

  useEffect(() => {
    if (selectedId == null) {
      setSelectedTransfer(null);
      return;
    }

    let cancelled = false;
    setDetailLoading(true);

    fetchTransfer(selectedId)
      .then((transfer) => {
        if (!cancelled) {
          setSelectedTransfer(transfer);
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

    Promise.all([fetchWarehouses(), fetchProducts({ size: 100 })])
      .then(([warehouseList, productPage]) => {
        setWarehouses(warehouseList);
        setProducts(productPage.items);
      })
      .catch(() => {
        setCreateError('Failed to load warehouses or products');
      });
  }, [canCreate, showCreateForm]);

  function handleTransferUpdated(transfer: StockTransfer) {
    setSelectedTransfer(transfer);
    setItems((current) =>
      current.map((item) => (item.id === transfer.id ? { ...item, status: transfer.status } : item)),
    );
    loadTransfers();
  }

  async function handleCreate(event: FormEvent) {
    event.preventDefault();
    setCreateError(null);
    setCreating(true);

    try {
      const created = await createTransfer({
        fromWarehouseId: Number(fromWarehouseId),
        toWarehouseId: Number(toWarehouseId),
        notes: notes.trim() || undefined,
        items: [{ productId: Number(productId), quantity: Number(quantity) }],
      });
      setShowCreateForm(false);
      setFromWarehouseId('');
      setToWarehouseId('');
      setProductId('');
      setQuantity('1');
      setNotes('');
      setSelectedId(created.id);
      await loadTransfers();
    } catch (err) {
      setCreateError(err instanceof Error ? err.message : 'Failed to create transfer');
    } finally {
      setCreating(false);
    }
  }

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="eyebrow">Distribution</p>
          <h1>Stock transfers</h1>
          <p className="subtitle">{totalElements} transfer(s)</p>
        </div>
        <div className="page__header-actions">
          {canCreate && (
            <button
              type="button"
              className="btn btn--primary"
              onClick={() => setShowCreateForm((current) => !current)}
            >
              {showCreateForm ? 'Close form' : 'New request'}
            </button>
          )}
          <button type="button" className="btn btn--ghost" onClick={loadTransfers} disabled={loading}>
            Refresh
          </button>
        </div>
      </header>

      {showCreateForm && canCreate && (
        <section className="panel">
          <h2>New stock transfer</h2>
          <form className="form form--grid" onSubmit={handleCreate}>
            <label className="form__field">
              <span>From warehouse</span>
              <select
                className="input"
                value={fromWarehouseId}
                onChange={(event) => setFromWarehouseId(event.target.value)}
                required
              >
                <option value="">Select source…</option>
                {warehouses.map((warehouse) => (
                  <option key={warehouse.id} value={warehouse.id}>
                    {warehouse.code} — {warehouse.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="form__field">
              <span>To warehouse</span>
              <select
                className="input"
                value={toWarehouseId}
                onChange={(event) => setToWarehouseId(event.target.value)}
                required
              >
                <option value="">Select destination…</option>
                {warehouses.map((warehouse) => (
                  <option key={warehouse.id} value={warehouse.id}>
                    {warehouse.code} — {warehouse.name}
                  </option>
                ))}
              </select>
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
              <span>Quantity</span>
              <input
                type="number"
                min="0.01"
                step="any"
                className="input"
                value={quantity}
                onChange={(event) => setQuantity(event.target.value)}
                required
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
                {creating ? 'Submitting…' : 'Submit request'}
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

      {loading && <p className="muted">Loading transfers…</p>}
      {error && <p className="form__error">{error}</p>}

      {!loading && !error && (
        <>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Transfer</th>
                  <th>Route</th>
                  <th>Status</th>
                  <th>Items</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                {items.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="muted">
                      No transfers match your filters.
                    </td>
                  </tr>
                ) : (
                  items.map((transfer) => (
                    <tr
                      key={transfer.id}
                      className={`table__row--clickable${
                        selectedId === transfer.id ? ' table__row--selected' : ''
                      }`}
                      onClick={() => setSelectedId(transfer.id)}
                    >
                      <td>
                        <strong>{transfer.transferNumber}</strong>
                      </td>
                      <td>
                        {transfer.fromWarehouseCode} → {transfer.toWarehouseCode}
                      </td>
                      <td>
                        <span className={`pill ${statusPillClass(transfer.status)}`}>
                          {formatStatus(transfer.status)}
                        </span>
                      </td>
                      <td>{transfer.items.length}</td>
                      <td>{new Date(transfer.createdAt).toLocaleString()}</td>
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
              {detailLoading && <p className="muted">Loading transfer details…</p>}
              {selectedTransfer && (
                <>
                  <div className="panel__header">
                    <div>
                      <h2>{selectedTransfer.transferNumber}</h2>
                      <p className="muted">
                        {selectedTransfer.fromWarehouseName} → {selectedTransfer.toWarehouseName}
                      </p>
                    </div>
                    <span className={`pill ${statusPillClass(selectedTransfer.status)}`}>
                      {formatStatus(selectedTransfer.status)}
                    </span>
                  </div>

                  {selectedTransfer.notes && <p>{selectedTransfer.notes}</p>}
                  {selectedTransfer.rejectReason && (
                    <p className="form__error">Rejected: {selectedTransfer.rejectReason}</p>
                  )}

                  <div className="table-wrap">
                    <table className="table">
                      <thead>
                        <tr>
                          <th>Product</th>
                          <th>Requested</th>
                          <th>Dispatched</th>
                          <th>Received</th>
                          <th>Remaining</th>
                        </tr>
                      </thead>
                      <tbody>
                        {selectedTransfer.items.map((item) => (
                          <tr key={item.id}>
                            <td>
                              <strong>{item.productSku}</strong>
                              <div className="muted">{item.productName}</div>
                            </td>
                            <td>
                              {formatQty(item.requestedQuantity)} {item.unitOfMeasure}
                            </td>
                            <td>
                              {formatQty(item.dispatchedQuantity)} {item.unitOfMeasure}
                            </td>
                            <td>
                              {formatQty(item.receivedQuantity)} {item.unitOfMeasure}
                            </td>
                            <td>
                              {formatQty(item.remainingToReceive)} {item.unitOfMeasure}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>

                  <TransferActionPanel
                    transfer={selectedTransfer}
                    onUpdated={handleTransferUpdated}
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
