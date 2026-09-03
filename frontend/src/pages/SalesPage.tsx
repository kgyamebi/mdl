import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { useAuth } from '../auth/AuthContext';
import { SaleActionPanel } from '../components/sales/SaleActionPanel';
import { fetchShops } from '../services/locationsService';
import { fetchProducts, lookupProductByBarcode } from '../services/productsService';
import { createSale, fetchSale, fetchSales } from '../services/salesService';
import type { PaymentMethod, Product, Sale, Shop } from '../types/api';
import { printSaleReceipt } from '../utils/printReceipt';

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

interface CartLine {
  productId: number;
  sku: string;
  name: string;
  quantity: number;
  unitPrice: number;
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
  const [cartLines, setCartLines] = useState<CartLine[]>([]);
  const [barcodeInput, setBarcodeInput] = useState('');
  const [manualProductId, setManualProductId] = useState('');
  const [manualQuantity, setManualQuantity] = useState('1');
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CASH');
  const [paymentReference, setPaymentReference] = useState('');
  const [notes, setNotes] = useState('');
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [barcodeError, setBarcodeError] = useState<string | null>(null);

  const cartTotal = useMemo(
    () => cartLines.reduce((sum, line) => sum + line.quantity * line.unitPrice, 0),
    [cartLines],
  );

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

  function addProductToCart(product: Product, qty: number) {
    setCartLines((current) => {
      const existing = current.find((line) => line.productId === product.id);
      if (existing) {
        return current.map((line) =>
          line.productId === product.id
            ? { ...line, quantity: line.quantity + qty }
            : line,
        );
      }
      return [
        ...current,
        {
          productId: product.id,
          sku: product.sku,
          name: product.name,
          quantity: qty,
          unitPrice: product.sellingPrice,
        },
      ];
    });
  }

  async function handleBarcodeSubmit(event: FormEvent) {
    event.preventDefault();
    const trimmed = barcodeInput.trim();
    if (!trimmed) {
      return;
    }
    setBarcodeError(null);
    try {
      const product = await lookupProductByBarcode(trimmed);
      addProductToCart(product, 1);
      setBarcodeInput('');
    } catch (err) {
      setBarcodeError(err instanceof Error ? err.message : 'Product not found');
    }
  }

  function handleManualAdd(event: FormEvent) {
    event.preventDefault();
    const product = products.find((entry) => entry.id === Number(manualProductId));
    const qty = Number(manualQuantity);
    if (!product || !Number.isFinite(qty) || qty <= 0) {
      return;
    }
    addProductToCart(product, qty);
    setManualProductId('');
    setManualQuantity('1');
  }

  function updateCartLine(productId: number, patch: Partial<Pick<CartLine, 'quantity' | 'unitPrice'>>) {
    setCartLines((current) =>
      current.map((line) => (line.productId === productId ? { ...line, ...patch } : line)),
    );
  }

  function removeCartLine(productId: number) {
    setCartLines((current) => current.filter((line) => line.productId !== productId));
  }

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
        items: cartLines.map((line) => ({
          productId: line.productId,
          quantity: line.quantity,
          unitPrice: line.unitPrice,
        })),
        payments: [
          {
            paymentMethod,
            amount: cartTotal,
            reference: paymentReference.trim() || undefined,
          },
        ],
      });
      setShowPosForm(false);
      setShopId('');
      setCustomerName('');
      setCartLines([]);
      setBarcodeInput('');
      setManualProductId('');
      setManualQuantity('1');
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
          <h2>Point of sale</h2>
          <form className="form form--grid form--touch-friendly pos-panel" onSubmit={handleCreate}>
            <label className="form__field">
              <span>Shop</span>
              <select className="input" value={shopId} onChange={(e) => setShopId(e.target.value)} required>
                <option value="">Select shop…</option>
                {shops.map((shop) => (
                  <option key={shop.id} value={shop.id}>{shop.code} — {shop.name}</option>
                ))}
              </select>
            </label>
            <label className="form__field">
              <span>Customer name</span>
              <input type="text" className="input" value={customerName} onChange={(e) => setCustomerName(e.target.value)} />
            </label>
            <div className="form__field form__field--wide">
              <span>Scan barcode</span>
              <form className="pos-barcode" onSubmit={handleBarcodeSubmit}>
                <input
                  type="text"
                  className="input"
                  placeholder="Scan or type barcode…"
                  value={barcodeInput}
                  onChange={(e) => setBarcodeInput(e.target.value)}
                  autoFocus
                />
                <button type="submit" className="btn btn--ghost">Add</button>
              </form>
              {barcodeError && <p className="form__error">{barcodeError}</p>}
            </div>
            <div className="form__field form__field--wide">
              <span>Add manually</span>
              <form className="pos-barcode" onSubmit={handleManualAdd}>
                <select className="input" value={manualProductId} onChange={(e) => setManualProductId(e.target.value)}>
                  <option value="">Select product…</option>
                  {products.map((product) => (
                    <option key={product.id} value={product.id}>{product.sku} — {product.name}</option>
                  ))}
                </select>
                <input type="number" min="0.01" step="any" className="input" value={manualQuantity} onChange={(e) => setManualQuantity(e.target.value)} />
                <button type="submit" className="btn btn--ghost">Add</button>
              </form>
            </div>
            {cartLines.length > 0 && (
              <div className="form__field form__field--wide">
                <div className="table-wrap">
                  <table className="table">
                    <thead><tr><th>Product</th><th>Qty</th><th>Price</th><th>Total</th><th></th></tr></thead>
                    <tbody>
                      {cartLines.map((line) => (
                        <tr key={line.productId}>
                          <td>{line.sku} — {line.name}</td>
                          <td>
                            <input
                              type="number"
                              min="0.01"
                              step="any"
                              className="input input--compact"
                              value={line.quantity}
                              onChange={(e) => updateCartLine(line.productId, { quantity: Number(e.target.value) })}
                            />
                          </td>
                          <td>
                            <input
                              type="number"
                              min="0.01"
                              step="any"
                              className="input input--compact"
                              value={line.unitPrice}
                              onChange={(e) => updateCartLine(line.productId, { unitPrice: Number(e.target.value) })}
                            />
                          </td>
                          <td>{formatMoney(line.quantity * line.unitPrice, currencyCode)}</td>
                          <td>
                            <button type="button" className="btn btn--ghost" onClick={() => removeCartLine(line.productId)}>Remove</button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
            <label className="form__field">
              <span>Payment method</span>
              <select className="input" value={paymentMethod} onChange={(e) => setPaymentMethod(e.target.value as PaymentMethod)} required>
                {PAYMENT_METHODS.map((method) => (
                  <option key={method.value} value={method.value}>{method.label}</option>
                ))}
              </select>
            </label>
            <label className="form__field">
              <span>Payment reference</span>
              <input type="text" className="input" value={paymentReference} onChange={(e) => setPaymentReference(e.target.value)} />
            </label>
            <label className="form__field form__field--wide">
              <span>Notes</span>
              <input type="text" className="input" value={notes} onChange={(e) => setNotes(e.target.value)} />
            </label>
            <div className="form__field form__field--wide pos-total">
              <strong>Total due: {formatMoney(cartTotal, currencyCode)}</strong>
              <span className="muted"> · {cartLines.length} line(s)</span>
            </div>
            {createError && <p className="form__error form__field--wide">{createError}</p>}
            <div className="form__field form__field--wide">
              <button type="submit" className="btn btn--primary" disabled={creating || cartTotal <= 0 || cartLines.length === 0}>
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
          <div
            className={`workspace-split${selectedId != null ? ' workspace-split--open' : ''}`}
          >
            <div className="workspace-split__list">
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
            </div>

          {selectedId != null && (
            <aside className="workspace-split__detail panel transfer-detail">
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
                    <div className="page__header-actions">
                      {selectedSale.status === 'COMPLETED' && (
                        <button
                          type="button"
                          className="btn btn--ghost"
                          onClick={() => printSaleReceipt(selectedSale, user?.businessName ?? 'MDL')}
                        >
                          Print receipt
                        </button>
                      )}
                      <span className={`pill ${statusPillClass(selectedSale.status)}`}>
                        {formatStatus(selectedSale.status)}
                      </span>
                    </div>
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
            </aside>
          )}
          </div>
        </>
      )}
    </div>
  );
}
