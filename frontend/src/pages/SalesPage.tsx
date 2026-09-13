import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { useAuth } from '../auth/AuthContext';
import { DetailCloseButton } from '../components/layout/DetailCloseButton';
import { SaleActionPanel } from '../components/sales/SaleActionPanel';
import { PosProductPicker } from '../components/sales/PosProductPicker';
import { fetchInventoryBalances } from '../services/inventoryService';
import { fetchShops } from '../services/locationsService';
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

function formatUnitLabel(unitOfMeasure: string): string {
  switch (unitOfMeasure.toUpperCase()) {
    case 'PIECE':
      return 'pieces';
    case 'METRE':
      return 'metres';
    default:
      return unitOfMeasure.toLowerCase();
  }
}

interface CartLine {
  productId: number;
  sku: string;
  name: string;
  unitOfMeasure: string;
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
  const [listShopId, setListShopId] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [showPosForm, setShowPosForm] = useState(false);
  const [shops, setShops] = useState<Shop[]>([]);
  const [shopsLoaded, setShopsLoaded] = useState(false);
  const [shopId, setShopId] = useState('');
  const [customerName, setCustomerName] = useState('');
  const [cartLines, setCartLines] = useState<CartLine[]>([]);
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CASH');
  const [paymentReference, setPaymentReference] = useState('');
  const [notes, setNotes] = useState('');
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  // productId -> available quantity at the selected shop. A present key means the
  // balance has been checked, so a missing row can be reported as zero rather than
  // being confused with "not looked up yet".
  const [stockAvailable, setStockAvailable] = useState<Map<number, number>>(new Map());
  const [stockLoading, setStockLoading] = useState(false);

  const selectedShop = useMemo(
    () => shops.find((shop) => shop.id === Number(shopId)) ?? null,
    [shopId, shops],
  );

  const operableShops = useMemo(
    () => shops.filter((shop) => shop.canOperate !== false),
    [shops],
  );

  // Sales deduct from the shop floor. Imported stock lives there, not in the
  // linked shop warehouse (those start empty until a physical count).
  const shopStockLocationId = selectedShop?.location?.id ?? selectedShop?.warehouseLocationId;

  function isStockChecked(productId: number): boolean {
    return stockAvailable.has(productId);
  }

  function getAvailableStock(productId: number): number {
    return stockAvailable.get(productId) ?? 0;
  }

  function getCartQuantityForProduct(productId: number): number {
    return cartLines.reduce(
      (sum, line) => (line.productId === productId ? sum + line.quantity : sum),
      0,
    );
  }

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
        shopId: listShopId ? Number(listShopId) : undefined,
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
  }, [page, statusFilter, listShopId]);

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
    fetchShops()
      .then((shopList) => {
        const active = shopList.filter((shop) => shop.status === 'ACTIVE');
        setShops(active);
        const operable = active.filter((shop) => shop.canOperate !== false);
        if (canCreate && operable.length === 1) {
          setShopId(String(operable[0].id));
        }
      })
      .catch(() => {
        if (canCreate) {
          setCreateError('Failed to load shops');
        }
      })
      .finally(() => setShopsLoaded(true));
  }, [canCreate]);

  // Stock is looked up per item rather than preloaded, because a shop can hold
  // thousands of balance rows.
  useEffect(() => {
    setStockAvailable(new Map());
  }, [shopStockLocationId]);

  const loadStockFor = useCallback(
    async (productId: number): Promise<number | null> => {
      const locationId = shopStockLocationId;
      if (!locationId) {
        return null;
      }

      setStockLoading(true);
      try {
        const response = await fetchInventoryBalances({ locationId, productId, size: 1 });
        const available = response.items[0]?.quantityAvailable ?? 0;
        setStockAvailable((current) => new Map(current).set(productId, available));
        return available;
      } catch {
        return null;
      } finally {
        setStockLoading(false);
      }
    },
    [shopStockLocationId],
  );

  const stockIssues = useMemo(() => {
    if (!shopId || cartLines.length === 0) {
      return [];
    }

    const totalsByProduct = new Map<number, { sku: string; quantity: number; unit: string }>();
    cartLines.forEach((line) => {
      const current = totalsByProduct.get(line.productId);
      if (current) {
        current.quantity += line.quantity;
        return;
      }
      totalsByProduct.set(line.productId, {
        sku: line.sku,
        quantity: line.quantity,
        unit: formatUnitLabel(line.unitOfMeasure),
      });
    });

    const issues: string[] = [];
    totalsByProduct.forEach(({ sku, quantity, unit }, productId) => {
      const available = getAvailableStock(productId);
      if (quantity > available) {
        issues.push(`${sku}: only ${formatQty(available)} ${unit} at this shop`);
      }
    });
    return issues;
    // getAvailableStock closes over stockAvailable, already listed below.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cartLines, shopId, stockAvailable]);

  const canSubmitSale =
    Boolean(shopId) &&
    cartLines.length > 0 &&
    cartTotal > 0 &&
    cartLines.every((line) => line.quantity > 0 && line.unitPrice > 0) &&
    stockIssues.length === 0 &&
    !stockLoading;

  async function addProductToCart(
    product: Product,
    qty: number,
    knownStock?: number | null,
  ): Promise<boolean> {
    if (!shopId) {
      setCreateError('Select a shop before adding products.');
      return false;
    }

    let available: number;
    if (knownStock != null && Number.isFinite(knownStock)) {
      available = knownStock;
      setStockAvailable((current) => new Map(current).set(product.id, available));
    } else if (isStockChecked(product.id)) {
      available = getAvailableStock(product.id);
    } else {
      available = (await loadStockFor(product.id)) ?? 0;
    }

    const remaining = available - getCartQuantityForProduct(product.id);
    if (qty > remaining) {
      const unit = formatUnitLabel(product.unitOfMeasure);
      setCreateError(
        remaining > 0
          ? `Only ${formatQty(remaining)} ${unit} of ${product.sku} available at this shop.`
          : `${product.sku} is out of stock at this shop.`,
      );
      return false;
    }

    setCreateError(null);
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
          unitOfMeasure: product.unitOfMeasure,
          quantity: qty,
          unitPrice: product.sellingPrice,
        },
      ];
    });
    setStockAvailable((current) => new Map(current).set(product.id, available));
    return true;
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

    if (!shopId) {
      setCreateError('Select a shop to continue.');
      return;
    }
    if (cartLines.length === 0 || cartTotal <= 0) {
      setCreateError('Add at least one product to the sale.');
      return;
    }

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
              className="btn btn--primary btn--touch btn--block-mobile"
              disabled={shopsLoaded && operableShops.length === 0}
              onClick={() => setShowPosForm((current) => !current)}
            >
              {showPosForm ? 'Close POS' : 'New sale'}
            </button>
          )}
          <button type="button" className="btn btn--ghost btn--touch btn--block-mobile" onClick={loadSales} disabled={loading}>
            Refresh
          </button>
        </div>
      </header>

      {canCreate && shopsLoaded && operableShops.length === 0 && (
        <p className="form__error" role="status">
          You are not assigned to a shop, so you cannot make sales. Ask the owner to assign you a shop on the Users page.
        </p>
      )}

      {showPosForm && canCreate && operableShops.length > 0 && (
        <section className="panel pos-panel">
          <h2>New sale</h2>
          <p className="hint pos-panel__hint">
            Fast sale: type → Enter adds ×1 · tap Quick picks · # for quantity · barcode Enter auto-adds.
          </p>
          <form className="form form--grid form--touch-friendly pos-form" onSubmit={handleCreate} noValidate>
            <div className="form__field">
              <span>Shop <em className="field-required">(required)</em></span>
              <div className="shop-switcher" role="tablist" aria-label="Sale shop">
                {operableShops.map((shop) => (
                  <button
                    key={shop.id}
                    type="button"
                    role="tab"
                    aria-selected={shopId === String(shop.id)}
                    className={`shop-switcher__tab${shopId === String(shop.id) ? ' shop-switcher__tab--active' : ''}`}
                    onClick={() => {
                      setShopId(String(shop.id));
                      setCreateError(null);
                    }}
                  >
                    {shop.name}
                  </button>
                ))}
              </div>
              <select
                className="input visually-hidden"
                value={shopId}
                aria-label="Shop"
                onChange={(e) => {
                  setShopId(e.target.value);
                  setCreateError(null);
                }}
              >
                <option value="">Select shop…</option>
                {operableShops.map((shop) => (
                  <option key={shop.id} value={shop.id}>{shop.code} — {shop.name}</option>
                ))}
              </select>
              {selectedShop && (
                <span className="hint">
                  Stock checked at {selectedShop.name}
                  {stockLoading ? ' · loading…' : ''}
                </span>
              )}
            </div>

            <div className="form__field form__field--wide pos-form__add-product">
              <span>Add products <em className="field-required">(required)</em></span>
              <PosProductPicker
                inputId="pos-product-search"
                locationId={shopStockLocationId}
                shopId={shopId ? Number(shopId) : undefined}
                currencyCode={currencyCode}
                disabled={!shopId}
                autoFocus={showPosForm}
                onAdd={(product, quantity, knownStock) =>
                  addProductToCart(product, quantity, knownStock)
                }
              />
              {!shopId && (
                <span className="hint">Select a shop first so stock and selling location are correct.</span>
              )}
            </div>

            {cartLines.length > 0 && (
              <div className="form__field form__field--wide">
                <div className="table-wrap table-wrap--stacked table-wrap--scroll-hint">
                  <table className="table table--stacked">
                    <thead><tr><th>Product</th><th>Qty sold</th><th>Price</th><th>Total</th><th></th></tr></thead>
                    <tbody>
                      {cartLines.map((line) => {
                        const lineTotalQty = getCartQuantityForProduct(line.productId);
                        const available = getAvailableStock(line.productId);
                        const overStock = shopId && lineTotalQty > available;
                        return (
                        <tr key={line.productId}>
                          <td data-label="Product">{line.sku} — {line.name}</td>
                          <td data-label={`Qty sold (${formatUnitLabel(line.unitOfMeasure)})`}>
                            <input
                              type="number"
                              min="0.01"
                              step="any"
                              className="input input--compact"
                              value={line.quantity}
                              onChange={(e) => updateCartLine(line.productId, { quantity: Number(e.target.value) })}
                              aria-label={`Quantity to sell in ${formatUnitLabel(line.unitOfMeasure)}`}
                            />
                            {overStock && (
                              <span className="form__error form__error--inline">
                                Max {formatQty(available)} {formatUnitLabel(line.unitOfMeasure)} at shop
                              </span>
                            )}
                          </td>
                          <td data-label="Price">
                            <input
                              type="number"
                              min="0.01"
                              step="any"
                              className="input input--compact"
                              value={line.unitPrice}
                              onChange={(e) => updateCartLine(line.productId, { unitPrice: Number(e.target.value) })}
                            />
                          </td>
                          <td data-label="Total">{formatMoney(line.quantity * line.unitPrice, currencyCode)}</td>
                          <td data-label="">
                            <button type="button" className="btn btn--ghost btn--touch" onClick={() => removeCartLine(line.productId)}>Remove</button>
                          </td>
                        </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            <label className="form__field">
              <span>Payment method <em className="field-required">(required)</em></span>
              <select className="input" value={paymentMethod} onChange={(e) => setPaymentMethod(e.target.value as PaymentMethod)}>
                {PAYMENT_METHODS.map((method) => (
                  <option key={method.value} value={method.value}>{method.label}</option>
                ))}
              </select>
            </label>

            <details className="pos-form__optional form__field--wide">
              <summary className="pos-form__optional-summary">Additional details (optional)</summary>
              <div className="pos-form__optional-body form form--grid">
                <label className="form__field">
                  <span>Customer name</span>
                  <input type="text" className="input" value={customerName} onChange={(e) => setCustomerName(e.target.value)} placeholder="Walk-in customer" />
                </label>
                <label className="form__field">
                  <span>Payment reference</span>
                  <input type="text" className="input" value={paymentReference} onChange={(e) => setPaymentReference(e.target.value)} placeholder="Receipt or transaction ID" />
                </label>
                <label className="form__field form__field--wide">
                  <span>Notes</span>
                  <input type="text" className="input" value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Any extra notes" />
                </label>
              </div>
            </details>

            <div className="form__field form__field--wide pos-total">
              <strong>Total due: {formatMoney(cartTotal, currencyCode)}</strong>
              <span className="muted"> · {cartLines.length} line(s)</span>
            </div>

            {stockIssues.length > 0 && (
              <p className="form__error form__field--wide">
                {stockIssues.join(' · ')}
              </p>
            )}

            {createError && <p className="form__error form__field--wide">{createError}</p>}

            <div className="form__field form__field--wide pos-form__submit">
              <button type="submit" className="btn btn--primary btn--block btn--touch" disabled={creating || !canSubmitSale}>
                {creating ? 'Processing…' : 'Complete sale'}
              </button>
              {!canSubmitSale && !creating && (
                <p className="hint pos-form__submit-hint">
                  {stockIssues.length > 0
                    ? 'Reduce quantities to match stock at the selected shop.'
                    : stockLoading
                      ? 'Loading stock for the selected shop…'
                      : shopId
                        ? 'Add at least one product to complete the sale.'
                        : 'Select a shop and add at least one product to complete the sale.'}
                </p>
              )}
            </div>
          </form>
        </section>
      )}

      <div className="toolbar">
        <label className="filter-field">
          <span className="filter-field__label">Shop</span>
          <select
            className="input input--compact"
            value={listShopId}
            onChange={(event) => {
              setPage(0);
              setListShopId(event.target.value);
            }}
          >
            <option value="">All shops</option>
            {operableShops.map((shop) => (
              <option key={shop.id} value={shop.id}>
                {shop.name}
              </option>
            ))}
          </select>
        </label>
        <label className="filter-field">
          <span className="filter-field__label">Status</span>
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
        </label>
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
                  <th className="num">Total</th>
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
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault();
                          setSelectedId(sale.id);
                        }
                      }}
                      tabIndex={0}
                      role="button"
                      aria-label={`View sale ${sale.saleNumber}`}
                    >
                      <td data-label="Sale">
                        <strong>{sale.saleNumber}</strong>
                      </td>
                      <td data-label="Shop">{sale.shopCode}</td>
                      <td data-label="Customer">{sale.customerName ?? '—'}</td>
                      <td data-label="Total" className="num">{formatMoney(sale.totalAmount, sale.currencyCode || currencyCode)}</td>
                      <td data-label="Status">
                        <span className={`pill ${statusPillClass(sale.status)}`}>
                          {formatStatus(sale.status)}
                        </span>
                      </td>
                      <td data-label="Created">{new Date(sale.createdAt).toLocaleString()}</td>
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
              <DetailCloseButton onClose={() => setSelectedId(null)} />
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
                          onClick={() => printSaleReceipt(selectedSale, user?.businessName ?? 'modern DL')}
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

                  <div className="table-wrap table-wrap--stacked table-wrap--scroll-hint">
                    <table className="table table--stacked">
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
                            <td data-label="Product">
                              <strong>{item.productSku}</strong>
                              <div className="muted">{item.productName}</div>
                            </td>
                            <td data-label="Qty">
                              {formatQty(item.quantity)} {item.unitOfMeasure}
                            </td>
                            <td data-label="Unit price">{formatMoney(item.unitPrice, selectedSale.currencyCode)}</td>
                            <td data-label="Line total">{formatMoney(item.lineTotal, selectedSale.currencyCode)}</td>
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
