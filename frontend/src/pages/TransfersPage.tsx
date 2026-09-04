import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { useAuth } from '../auth/AuthContext';
import { TransferActionPanel } from '../components/transfers/TransferActionPanel';
import { fetchProducts } from '../services/productsService';
import { createTransfer, fetchTransfer, fetchTransferFormOptions, fetchTransfers } from '../services/transfersService';
import type { Product, StockTransfer, TransferShopOption, TransferWarehouseOption } from '../types/api';
import {
  formatTransferEndpointLabel,
  formatTransferRouteLabel,
  groupWarehousesForTransfer,
  mapTransferFormOptions,
} from '../utils/transferEndpointLabel';

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

function WarehouseSelectOptions({
  warehouses,
  shops,
}: {
  warehouses: Array<TransferWarehouseOption | { id: number; code: string; name: string; warehouseType: string; linkedShopName?: string | null }>;
  shops: Array<Pick<TransferShopOption, 'warehouseId' | 'name'>>;
}) {
  const groups = groupWarehousesForTransfer(warehouses, shops);

  return (
    <>
      {groups.main.length > 0 && (
        <optgroup label="Main warehouses">
          {groups.main.map((warehouse) => (
            <option key={warehouse.id} value={warehouse.id}>
              {formatTransferEndpointLabel(warehouse, shops)}
            </option>
          ))}
        </optgroup>
      )}
      {groups.shopStock.length > 0 && (
        <optgroup label="Shop stock">
          {groups.shopStock.map((warehouse) => (
            <option key={warehouse.id} value={warehouse.id}>
              {formatTransferEndpointLabel(warehouse, shops)}
            </option>
          ))}
        </optgroup>
      )}
      {groups.other.length > 0 && (
        <optgroup label="Other warehouses">
          {groups.other.map((warehouse) => (
            <option key={warehouse.id} value={warehouse.id}>
              {formatTransferEndpointLabel(warehouse, shops)}
            </option>
          ))}
        </optgroup>
      )}
    </>
  );
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
  const [transferWarehouses, setTransferWarehouses] = useState<TransferWarehouseOption[]>([]);
  const [transferShops, setTransferShops] = useState<TransferShopOption[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [formOptionsLoading, setFormOptionsLoading] = useState(false);
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
    fetchTransferFormOptions()
      .then((formOptions) => {
        setTransferWarehouses(formOptions.warehouses);
        setTransferShops(formOptions.shops);
      })
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (!canCreate || !showCreateForm) {
      return;
    }

    let cancelled = false;
    setFormOptionsLoading(true);
    setCreateError(null);

    Promise.all([fetchTransferFormOptions(), fetchProducts({ size: 100 })])
      .then(([formOptions, productPage]) => {
        if (cancelled) {
          return;
        }
        setTransferWarehouses(formOptions.warehouses);
        setTransferShops(formOptions.shops);
        setProducts(productPage.items);
        if (formOptions.warehouses.length === 0) {
          setCreateError('No authorized transfer routes are available for your role yet. Ask the owner to configure routes.');
        } else if (productPage.items.length === 0) {
          setCreateError('No products are available to transfer.');
        }
      })
      .catch((err: Error) => {
        if (!cancelled) {
          setCreateError(err.message || 'Failed to load transfer form');
        }
      })
      .finally(() => {
        if (!cancelled) {
          setFormOptionsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [canCreate, showCreateForm]);

  const mappedFormOptions = useMemo(
    () => mapTransferFormOptions({ warehouses: transferWarehouses, shops: transferShops }),
    [transferWarehouses, transferShops],
  );

  const routeLabelsReady = mappedFormOptions.warehouses.length > 0;

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
          <p className="muted">
            Move stock between main warehouses, shop stock locations, and other warehouses.
            Shop destinations use each shop&apos;s stock location — the same inventory used for sales.
          </p>
          {formOptionsLoading && <p className="muted">Loading transfer options…</p>}
          <form className="form form--grid form--touch-friendly" onSubmit={handleCreate}>
            <label className="form__field">
              <span>From location</span>
              <select
                className="input"
                value={fromWarehouseId}
                onChange={(event) => setFromWarehouseId(event.target.value)}
                required
                disabled={formOptionsLoading || transferWarehouses.length === 0}
              >
                <option value="">Select source…</option>
                <WarehouseSelectOptions warehouses={transferWarehouses} shops={transferShops} />
              </select>
            </label>
            <label className="form__field">
              <span>To location</span>
              <select
                className="input"
                value={toWarehouseId}
                onChange={(event) => setToWarehouseId(event.target.value)}
                required
                disabled={formOptionsLoading || transferWarehouses.length === 0}
              >
                <option value="">Select destination…</option>
                <WarehouseSelectOptions warehouses={transferWarehouses} shops={transferShops} />
              </select>
            </label>
            <label className="form__field">
              <span>Product</span>
              <select
                className="input"
                value={productId}
                onChange={(event) => setProductId(event.target.value)}
                required
                disabled={formOptionsLoading || products.length === 0}
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
              <button
                type="submit"
                className="btn btn--primary"
                disabled={creating || formOptionsLoading || transferWarehouses.length === 0 || products.length === 0}
              >
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
          <div
            className={`workspace-split${selectedId != null ? ' workspace-split--open' : ''}`}
          >
            <div className="workspace-split__list">
              <div className="table-wrap table-wrap--stacked">
                <table className="table table--stacked">
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
                        {routeLabelsReady
                          ? formatTransferRouteLabel(
                              transfer.fromWarehouseId,
                              transfer.toWarehouseId,
                              mappedFormOptions.warehouses,
                              mappedFormOptions.shops,
                            )
                          : `${transfer.fromWarehouseCode} → ${transfer.toWarehouseCode}`}
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
            </div>

          {selectedId != null && (
            <aside className="workspace-split__detail panel transfer-detail">
              {detailLoading && <p className="muted">Loading transfer details…</p>}
              {selectedTransfer && (
                <>
                  <div className="panel__header">
                    <div>
                      <h2>{selectedTransfer.transferNumber}</h2>
                      <p className="muted">
                        {routeLabelsReady
                          ? formatTransferRouteLabel(
                              selectedTransfer.fromWarehouseId,
                              selectedTransfer.toWarehouseId,
                              mappedFormOptions.warehouses,
                              mappedFormOptions.shops,
                            )
                          : `${selectedTransfer.fromWarehouseName} → ${selectedTransfer.toWarehouseName}`}
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
            </aside>
          )}
          </div>
        </>
      )}
    </div>
  );
}
