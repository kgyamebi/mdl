import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { useAuth } from '../auth/AuthContext';
import { SaleActionPanel } from '../components/sales/SaleActionPanel';
import { fetchShops } from '../services/locationsService';
import { fetchProducts } from '../services/productsService';
import { createSale, fetchSale, fetchSales } from '../services/salesService';
import type { PaymentMethod, Product, Sale, Shop } from '../types/api';

const STATUS_FILTERS: Array<{ value: string; label: string }> = [
  { value: '', label: 'All statuses' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'CANCELLED', label: 'Cancelled' },
  { value: 'REFUNDED', label: 'Refunded' },
];

const PAYMENT_METHODS: Array<{ value: PaymentMethod; label: string }> = [
  { value: 'CASH', label: 'Cash' },
  { value: 'MOBILE_MONEY', label: 'Mobile money' },
  { value: 'CARD', label: 'Card' },
  { value: 'BANK_TRANSFER', label: 'Bank transfer' },
];

function formatMoney(value: number, currencyCode: string): string {
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: currencyCode,
    maximumFractionDigits: 2,
  }).format(value);
}

function formatQty(value: number): string {
  return new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 }).format(value);
}

function statusPillClass(status: string): string {
  switch (status) {
    case 'COMPLETED':
      return 'pill--ok';
    case 'CANCELLED':
    case 'REFUNDED':
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

function formatPaymentMethod(method: string): string {
  return method
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

export function SalesPage() {
  const { hasPermission, user } = useAuth();
  const canCreate = hasPermission('sale:create');
  const currencyCode = user?.currencyCode ?? 'GHS';

  const [items, setItems] = useState<Sale[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [selectedSale, setSelectedSale] = useState<Sale | null>(null);
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [showPosForm, setShowPosForm] = useState(false);
  const [shops, setShops] = useState<Shop[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [shopId, setShopId] = useState('');
  const [customerName, setCustomerName] = useState('');
  const [productId, setProductId] = useState('');
  const [quantity, setQuantity] = useState('1');
  const [unitPrice, setUnitPrice] = useState('');
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CASH');
  const [paymentReference, setPaymentReference] = useState('');
  const [notes, setNotes] = useState('');
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  const lineTotal = useMemo(() => {
    const qty = Number(quantity);
    const price = Number(unitPrice);
    if (!Number.isFinite(qty) || !Number.isFinite(price)) {
      return 0;
    }
    return qty * price;
  }, [quantity, unitPrice]);

  const loadSales = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetchSales({
        status: statusFilter || undefined,
        page,
        size: 20,
      });
      setItems(response.items);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load sales');
    } finally {
      setLoading(false);
    }
  }, [page, statusFilter]);

  useEffect(() => {
    loadSales();
  }, [loadSales]);

  useEffect(() => {
    if (selectedId == null) {
      setSelectedSale(null);
      return;
    }

    let cancelled = false;
    setDetailLoading(true);

    fetchSale(selectedId)
      .then((sale) => {
        if (!cancelled) {
          setSelectedSale(sale);
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
    if (!canCreate || !showPosForm) {
      return;
    }

    Promise.all([fetchShops(), fetchProducts({ size: 100 })])
      .then(([shopList, productPage]) => {
        setShops(shopList);
        setProducts(productPage.items);
      })
      .catch(() => {
        setCreateError('Failed to load shops or products');
      });
  }, [canCreate, showPosForm]);

  useEffect(() => {
    if (!productId) {
      return;
    }
    const product = products.find((entry) => entry.id === Number(productId));
    if (product) {
      setUnitPrice(String(product.sellingPrice));
    }
  }, [productId, products]);

  function handleSaleUpdated(sale: Sale) {
    setSelectedSale(sale);
    setItems((current) =>
      current.map((item) => (item.id === sale.id ? { ...item, status: sale.status } : item)),
    );
    loadSales();
  }

  async function handleCreate(event: FormEvent) {
    event.preventDefault();
    setCreateError(null);
    setCreating(true);

    try {
      const created = await createSale({
        shopId: Number(shopId),
        customerName: customerName.trim() || undefined,
        notes: notes.trim() || undefined,
        items: [
          {
            productId: Number(productId),
            quantity: Number(quantity),
            unitPrice: Number(unitPrice),
          },
        ],
        payments: [
          {
            paymentMethod,
            amount: lineTotal,
            reference: paymentReference.trim() || undefined,
          },
        ],
      });
      setShowPosForm(false);
      setShopId('');
      setCustomerName('');
      setProductId('');
      setQuantity('1');
      setUnitPrice('');
      setPaymentMethod('CASH');
      setPaymentReference('');
      setNotes('');
      setSelectedId(created.id);
      await loadSales();
    } catch (err) {
      setCreateError(err instanceof Error ? err.message : 'Failed to complete sale');
    } finally {
      setCreating(false);
    }
  }

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="eyebrow">Point of sale</p>
          <h1>Sales</h1>
          <p className="subtitle">{totalElements} sale(s)</p>
        </div>
        <div className="page__header-actions">
          {canCreate && (
            <button
              type="button"
              className="btn btn--primary"
              onClick={() => setShowPosForm((current) => !current)}
            >
              {showPosForm ? 'Close POS' : 'New sale'}
            </button>
          )}
          <button type="button" className="btn btn--ghost" onClick={loadSales} disabled={loading}>
            Refresh
          </button>
        </div>
      </header>

      {showPosForm && canCreate && (
        <section className="panel pos-panel">
          <h2>Quick sale</h2>
          <form className="form form--grid form--touch-friendly pos-panel" onSubmit={handleCreate}>
            <label className="form__field">
              <span>Shop</span>
              <select
                className="input"
                value={shopId}
                onChange={(event) => setShopId(event.target.value)}
                required
              >
                <option value="">Select shop…</option>
                {shops.map((shop) => (
                  <option key={shop.id} value={shop.id}>
                    {shop.code} — {shop.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="form__field">
              <span>Customer name</span>
              <input
                type="text"
                className="input"
                value={customerName}
                onChange={(event) => setCustomerName(event.target.value)}
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
            <label className="form__field">
              <span>Unit price ({currencyCode})</span>
              <input
                type="number"
                min="0.01"
                step="any"
                className="input"
                value={unitPrice}
                onChange={(event) => setUnitPrice(event.target.value)}
                required
              />
            </label>
            <label className="form__field">
              <span>Payment method</span>
              <select
                className="input"
                value={paymentMethod}
                onChange={(event) => setPaymentMethod(event.target.value as PaymentMethod)}
                required
              >
                {PAYMENT_METHODS.map((method) => (
                  <option key={method.value} value={method.value}>
                    {method.label}
                  </option>
                ))}
              </select>
            </label>
            <label className="form__field">
              <span>Payment reference</span>
              <input
                type="text"
                className="input"
                value={paymentReference}
                onChange={(event) => setPaymentReference(event.target.value)}
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
            <div className="form__field form__field--wide pos-total">
              <strong>Total due: {formatMoney(lineTotal, currencyCode)}</strong>
            </div>
            {createError && <p className="form__error form__field--wide">{createError}</p>}
            <div className="form__field form__field--wide">
              <button type="submit" className="btn btn--primary" disabled={creating || lineTotal <= 0}>
                {creating ? 'Processing…' : 'Complete sale'}
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

      {loading && <p className="muted">Loading sales…</p>}
      {error && <p className="form__error">{error}</p>}

      {!loading && !error && (
        <>
          <div className="table-wrap table-wrap--stacked">
            <table className="table table--stacked">
              <thead>
                <tr>
                  <th>Sale</th>
                  <th>Shop</th>
                  <th>Customer</th>
                  <th>Total</th>
                  <th>Status</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                {items.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="muted">
                      No sales match your filters.
                    </td>
                  </tr>
                ) : (
                  items.map((sale) => (
                    <tr
                      key={sale.id}
                      className={`table__row--clickable${
                        selectedId === sale.id ? ' table__row--selected' : ''
                      }`}
                      onClick={() => setSelectedId(sale.id)}
                    >
                      <td>
                        <strong>{sale.saleNumber}</strong>
                      </td>
                      <td>{sale.shopCode}</td>
                      <td>{sale.customerName ?? '—'}</td>
                      <td>{formatMoney(sale.totalAmount, sale.currencyCode || currencyCode)}</td>
                      <td>
                        <span className={`pill ${statusPillClass(sale.status)}`}>
                          {formatStatus(sale.status)}
                        </span>
                      </td>
                      <td>{new Date(sale.createdAt).toLocaleString()}</td>
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
              {detailLoading && <p className="muted">Loading sale details…</p>}
              {selectedSale && (
                <>
                  <div className="panel__header">
                    <div>
                      <h2>{selectedSale.saleNumber}</h2>
                      <p className="muted">
                        {selectedSale.shopName}
                        {selectedSale.customerName ? ` · ${selectedSale.customerName}` : ''}
                      </p>
                    </div>
                    <span className={`pill ${statusPillClass(selectedSale.status)}`}>
                      {formatStatus(selectedSale.status)}
                    </span>
                  </div>

                  {selectedSale.notes && <p>{selectedSale.notes}</p>}
                  {selectedSale.cancelReason && (
                    <p className="form__error">Cancelled: {selectedSale.cancelReason}</p>
                  )}
                  {selectedSale.refundReason && (
                    <p className="form__error">Refunded: {selectedSale.refundReason}</p>
                  )}

                  <dl className="stat-grid stat-grid--inline">
                    <div>
                      <dt>Subtotal</dt>
                      <dd>{formatMoney(selectedSale.subtotal, selectedSale.currencyCode)}</dd>
                    </div>
                    <div>
                      <dt>Total</dt>
                      <dd>{formatMoney(selectedSale.totalAmount, selectedSale.currencyCode)}</dd>
                    </div>
                    <div>
                      <dt>Returned</dt>
                      <dd>{formatMoney(selectedSale.returnedAmount, selectedSale.currencyCode)}</dd>
                    </div>
                  </dl>

                  <div className="table-wrap">
                    <table className="table">
                      <thead>
                        <tr>
                          <th>Product</th>
                          <th>Qty</th>
                          <th>Unit price</th>
                          <th>Line total</th>
                        </tr>
                      </thead>
                      <tbody>
                        {selectedSale.items.map((item) => (
                          <tr key={item.id}>
                            <td>
                              <strong>{item.productSku}</strong>
                              <div className="muted">{item.productName}</div>
                            </td>
                            <td>
                              {formatQty(item.quantity)} {item.unitOfMeasure}
                            </td>
                            <td>{formatMoney(item.unitPrice, selectedSale.currencyCode)}</td>
                            <td>{formatMoney(item.lineTotal, selectedSale.currencyCode)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>

                  <h3 className="panel__subheading">Payments</h3>
                  <ul className="list">
                    {selectedSale.payments.map((payment) => (
                      <li key={payment.id} className="list__item">
                        <div>
                          <strong>{formatPaymentMethod(payment.paymentMethod)}</strong>
                          {payment.reference && <p className="muted">{payment.reference}</p>}
                        </div>
                        <span>{formatMoney(payment.amount, selectedSale.currencyCode)}</span>
                      </li>
                    ))}
                  </ul>

                  <SaleActionPanel sale={selectedSale} onUpdated={handleSaleUpdated} />
                </>
              )}
            </section>
          )}
        </>
      )}
    </div>
  );
}
