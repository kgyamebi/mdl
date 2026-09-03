import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { useAuth } from '../auth/AuthContext';
import { DetailCloseButton } from '../components/layout/DetailCloseButton';
import { SaleActionPanel } from '../components/sales/SaleActionPanel';
import { fetchInventoryBalances } from '../services/inventoryService';
import { fetchShops } from '../services/locationsService';
import { fetchProducts, lookupProductByBarcode } from '../services/productsService';
import { createSale, fetchSale, fetchSales } from '../services/salesService';
import type { InventoryBalance, PaymentMethod, Product, Sale, Shop } from '../types/api';
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
  const [warehouseStock, setWarehouseStock] = useState<Map<number, InventoryBalance>>(new Map());
  const [stockLoading, setStockLoading] = useState(false);

  const selectedShop = useMemo(
    () => shops.find((shop) => shop.id === Number(shopId)) ?? null,
    [shopId, shops],
  );

  const selectedManualProduct = useMemo(
    () => products.find((product) => product.id === Number(manualProductId)) ?? null,
    [manualProductId, products],
  );

  const stockByProductId = useMemo(() => {
    const map = new Map<number, number>();
    warehouseStock.forEach((balance) => {
      map.set(balance.productId, balance.quantityAvailable);
    });
    return map;
  }, [warehouseStock]);

  function getAvailableStock(productId: number): number {
    return stockByProductId.get(productId) ?? 0;
  }

  function getCartQuantityForProduct(productId: number): number {
    return cartLines.reduce(
      (sum, line) => (line.productId === productId ? sum + line.quantity : sum),
      0,
    );
  }

  function getRemainingStock(productId: number): number {
    return Math.max(0, getAvailableStock(productId) - getCartQuantityForProduct(productId));
  }

  function formatStockHint(productId: number): string {
    if (!shopId) {
      return '';
    }
    const balance = warehouseStock.get(productId);
    if (!balance) {
      return 'No stock at this shop';
    }
    const remaining = getRemainingStock(productId);
    const unit = formatUnitLabel(balance.unitOfMeasure);
    if (remaining <= 0) {
      return 'Out of stock at this shop';
    }
    return `${formatQty(remaining)} ${unit} available`;
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
        if (shopList.length === 1) {
          setShopId(String(shopList[0].id));
        }
      })
      .catch(() => {
        setCreateError('Failed to load shops or products');
      });
  }, [canCreate, showPosForm]);

  useEffect(() => {
    if (!showPosForm || !selectedShop?.warehouseLocationId) {
      setWarehouseStock(new Map());
      return;
    }

    let cancelled = false;
    setStockLoading(true);

    fetchInventoryBalances({
      locationId: selectedShop.warehouseLocationId,
      size: 200,
    })
      .then((response) => {
        if (cancelled) {
          return;
        }
        const next = new Map<number, InventoryBalance>();
        response.items.forEach((balance) => {
          next.set(balance.productId, balance);
        });
        setWarehouseStock(next);
      })
      .catch(() => {
        if (!cancelled) {
          setWarehouseStock(new Map());
        }
      })
      .finally(() => {
        if (!cancelled) {
          setStockLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [showPosForm, selectedShop?.warehouseLocationId]);

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
  }, [cartLines, shopId, stockByProductId]);

  const canSubmitSale =
    Boolean(shopId) &&
    cartLines.length > 0 &&
    cartTotal > 0 &&
    cartLines.every((line) => line.quantity > 0 && line.unitPrice > 0) &&
    stockIssues.length === 0 &&
    !stockLoading;

  function stockErrorForProduct(product: Product, requestedQty: number): string | null {
    const inCart = getCartQuantityForProduct(product.id);
    const available = getAvailableStock(product.id);
    const remaining = available - inCart;
    const unit = formatUnitLabel(
      warehouseStock.get(product.id)?.unitOfMeasure ?? product.unitOfMeasure,
    );

    if (requestedQty > remaining) {
      return remaining > 0
        ? `Only ${formatQty(remaining)} ${unit} of ${product.sku} available at this shop.`
        : `${product.sku} is out of stock at this shop.`;
    }
    return null;
  }

  function addProductToCart(product: Product, qty: number) {
    if (!shopId) {
      setCreateError('Select a shop before adding products.');
      return;
    }

    const stockError = stockErrorForProduct(product, qty);
    if (stockError) {
      setCreateError(stockError);
      return;
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
  }

  async function handleBarcodeAdd() {
    const trimmed = barcodeInput.trim();
    if (!trimmed) {
      return;
    }
    if (!shopId) {
      setBarcodeError('Select a shop before scanning products.');
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

  function handleManualAddClick() {
    const product = products.find((entry) => entry.id === Number(manualProductId));
    const qty = Number(manualQuantity);
    if (!product || !Number.isFinite(qty) || qty <= 0) {
      setCreateError('Select a product and enter a quantity greater than zero.');
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
              className="btn btn--primary btn--touch btn--block-mobile"
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

      {showPosForm && canCreate && (
        <section className="panel pos-panel">
          <h2>New sale</h2>
          <p className="hint pos-panel__hint">
            Required: shop, at least one product, and payment method. Everything else is optional.
          </p>
          <form className="form form--grid form--touch-friendly pos-form" onSubmit={handleCreate} noValidate>
            <label className="form__field">
              <span>Shop <em className="field-required">(required)</em></span>
              <select
                className="input"
                value={shopId}
                onChange={(e) => {
                  setShopId(e.target.value);
                  setCreateError(null);
                }}
              >
                <option value="">Select shop…</option>
                {shops.map((shop) => (
                  <option key={shop.id} value={shop.id}>{shop.code} — {shop.name}</option>
                ))}
              </select>
              {selectedShop && (
                <span className="hint">
                  Stock checked at {selectedShop.warehouseName}
                  {stockLoading ? ' · loading…' : ''}
                </span>
              )}
            </label>

            <div className="form__field form__field--wide pos-form__add-product">
              <span>Add product <em className="field-required">(required)</em></span>
              <div className="pos-add-product">
                <label className="pos-add-product__field">
                  <span className="pos-add-product__label">Product</span>
                  <select
                    className="input"
                    value={manualProductId}
                    onChange={(e) => setManualProductId(e.target.value)}
                  >
                    <option value="">Select product…</option>
                    {products.map((product) => {
                      const stockHint = shopId ? formatStockHint(product.id) : '';
                      return (
                        <option key={product.id} value={product.id}>
                          {product.sku} — {product.name}
                          {stockHint ? ` (${stockHint})` : ''}
                        </option>
                      );
                    })}
                  </select>
                </label>
                <label className="pos-add-product__field pos-add-product__field--qty">
                  <span className="pos-add-product__label">
                    Quantity to sell
                    {selectedManualProduct ? ` (${formatUnitLabel(selectedManualProduct.unitOfMeasure)})` : ''}
                  </span>
                  <input
                    type="number"
                    min="0.01"
                    step="any"
                    className="input"
                    value={manualQuantity}
                    onChange={(e) => setManualQuantity(e.target.value)}
                    placeholder="1"
                  />
                  {selectedManualProduct && shopId && (
                    <span className="hint">{formatStockHint(selectedManualProduct.id)}</span>
                  )}
                </label>
                <div className="pos-add-product__action">
                  <button type="button" className="btn btn--ghost btn--touch" onClick={handleManualAddClick}>
                    Add to sale
                  </button>
                </div>
              </div>
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
                <div className="form__field form__field--wide">
                  <span>Scan barcode</span>
                  <div className="pos-barcode">
                    <input
                      type="text"
                      className="input"
                      placeholder="Optional — scan or type barcode…"
                      value={barcodeInput}
                      onChange={(e) => setBarcodeInput(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') {
                          e.preventDefault();
                          void handleBarcodeAdd();
                        }
                      }}
                    />
                    <button type="button" className="btn btn--ghost btn--touch" onClick={() => void handleBarcodeAdd()}>
                      Add
                    </button>
                  </div>
                  {barcodeError && <p className="form__error">{barcodeError}</p>}
                </div>
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
                      : 'Select a shop and add at least one product to complete the sale.'}
                </p>
              )}
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
                      <td data-label="Total">{formatMoney(sale.totalAmount, sale.currencyCode || currencyCode)}</td>
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
