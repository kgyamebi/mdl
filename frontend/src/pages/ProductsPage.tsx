import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { useAuth } from '../auth/AuthContext';
import {
  addProductBarcode,
  createProduct,
  fetchProduct,
  fetchProductCategories,
  fetchProducts,
  removeProductBarcode,
  updateProduct,
} from '../services/productsService';
import type { Product, ProductCategory } from '../types/api';

const UNIT_OPTIONS = ['PIECE', 'METRE', 'BOX', 'ROLL', 'PACK', 'SET'] as const;
const STATUS_OPTIONS = ['ACTIVE', 'DRAFT', 'DISCONTINUED'] as const;
const BARCODE_TYPES = ['EAN13', 'UPC', 'CODE128', 'INTERNAL', 'QR'] as const;

function formatMoney(value: number, currencyCode: string): string {
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: currencyCode,
    maximumFractionDigits: 2,
  }).format(value);
}

const emptyForm = {
  sku: '',
  name: '',
  description: '',
  brand: '',
  categoryId: '',
  unitOfMeasure: 'PIECE' as string,
  costPrice: '',
  sellingPrice: '',
  taxInclusive: true,
  trackInventory: true,
  reorderLevel: '',
  status: 'ACTIVE' as string,
};

export function ProductsPage() {
  const { hasPermission, user } = useAuth();
  const canManage = hasPermission('product:manage');
  const currencyCode = user?.currencyCode ?? 'GHS';

  const [items, setItems] = useState<Product[]>([]);
  const [categories, setCategories] = useState<ProductCategory[]>([]);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [form, setForm] = useState(emptyForm);
  const [showCreate, setShowCreate] = useState(false);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [newBarcode, setNewBarcode] = useState('');
  const [newBarcodeType, setNewBarcodeType] = useState<string>('EAN13');

  const loadProducts = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetchProducts({ search, page, size: 20 });
      setItems(response.items);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load products');
    } finally {
      setLoading(false);
    }
  }, [page, search]);

  useEffect(() => {
    loadProducts();
  }, [loadProducts]);

  useEffect(() => {
    if (canManage) {
      fetchProductCategories().then(setCategories).catch(() => {});
    }
  }, [canManage]);

  useEffect(() => {
    if (selectedId == null) {
      setSelectedProduct(null);
      return;
    }
    setDetailLoading(true);
    fetchProduct(selectedId)
      .then((product) => {
        setSelectedProduct(product);
        setForm({
          sku: product.sku,
          name: product.name,
          description: product.description ?? '',
          brand: product.brand ?? '',
          categoryId: product.categoryId != null ? String(product.categoryId) : '',
          unitOfMeasure: product.unitOfMeasure,
          costPrice: String(product.costPrice),
          sellingPrice: String(product.sellingPrice),
          taxInclusive: product.taxInclusive,
          trackInventory: product.trackInventory,
          reorderLevel: product.reorderLevel != null ? String(product.reorderLevel) : '',
          status: product.status,
        });
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load product'))
      .finally(() => setDetailLoading(false));
  }, [selectedId]);

  async function handleCreate(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const created = await createProduct({
        sku: form.sku.trim(),
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        brand: form.brand.trim() || undefined,
        categoryId: form.categoryId ? Number(form.categoryId) : null,
        unitOfMeasure: form.unitOfMeasure,
        costPrice: form.costPrice ? Number(form.costPrice) : undefined,
        sellingPrice: Number(form.sellingPrice),
        taxInclusive: form.taxInclusive,
        trackInventory: form.trackInventory,
        reorderLevel: form.reorderLevel ? Number(form.reorderLevel) : undefined,
        status: form.status,
      });
      setShowCreate(false);
      setForm(emptyForm);
      setSelectedId(created.id);
      loadProducts();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create product');
    } finally {
      setSaving(false);
    }
  }

  async function handleUpdate(event: FormEvent) {
    event.preventDefault();
    if (!selectedProduct) {
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const updated = await updateProduct(selectedProduct.id, {
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        brand: form.brand.trim() || undefined,
        categoryId: form.categoryId ? Number(form.categoryId) : null,
        unitOfMeasure: form.unitOfMeasure,
        costPrice: form.costPrice ? Number(form.costPrice) : undefined,
        sellingPrice: Number(form.sellingPrice),
        taxInclusive: form.taxInclusive,
        trackInventory: form.trackInventory,
        reorderLevel: form.reorderLevel ? Number(form.reorderLevel) : undefined,
        status: form.status,
      });
      setSelectedProduct(updated);
      loadProducts();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update product');
    } finally {
      setSaving(false);
    }
  }

  async function handleAddBarcode(event: FormEvent) {
    event.preventDefault();
    if (!selectedProduct || !newBarcode.trim()) {
      return;
    }
    try {
      const updated = await addProductBarcode(selectedProduct.id, {
        barcode: newBarcode.trim(),
        barcodeType: newBarcodeType,
        primary: (selectedProduct.barcodes?.length ?? 0) === 0,
      });
      setSelectedProduct(updated);
      setNewBarcode('');
      loadProducts();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to add barcode');
    }
  }

  async function handleRemoveBarcode(barcodeId: number) {
    if (!selectedProduct) {
      return;
    }
    try {
      await removeProductBarcode(selectedProduct.id, barcodeId);
      const updated = await fetchProduct(selectedProduct.id);
      setSelectedProduct(updated);
      loadProducts();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to remove barcode');
    }
  }

  function renderProductFields(editing: boolean) {
    return (
      <>
        {!editing && (
          <label className="form__field">
            <span>SKU</span>
            <input className="input" value={form.sku} onChange={(e) => setForm({ ...form, sku: e.target.value })} required />
          </label>
        )}
        {editing && (
          <label className="form__field">
            <span>SKU</span>
            <input className="input" value={form.sku} readOnly disabled />
          </label>
        )}
        <label className="form__field">
          <span>Name</span>
          <input className="input" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
        </label>
        <label className="form__field">
          <span>Brand</span>
          <input className="input" value={form.brand} onChange={(e) => setForm({ ...form, brand: e.target.value })} />
        </label>
        <label className="form__field">
          <span>Category</span>
          <select className="input" value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })}>
            <option value="">None</option>
            {categories.map((cat) => (
              <option key={cat.id} value={cat.id}>{cat.name}</option>
            ))}
          </select>
        </label>
        <label className="form__field">
          <span>Unit</span>
          <select className="input" value={form.unitOfMeasure} onChange={(e) => setForm({ ...form, unitOfMeasure: e.target.value })}>
            {UNIT_OPTIONS.map((unit) => (
              <option key={unit} value={unit}>{unit}</option>
            ))}
          </select>
        </label>
        <label className="form__field">
          <span>Cost price</span>
          <input type="number" min="0" step="any" className="input" value={form.costPrice} onChange={(e) => setForm({ ...form, costPrice: e.target.value })} />
        </label>
        <label className="form__field">
          <span>Selling price</span>
          <input type="number" min="0" step="any" className="input" value={form.sellingPrice} onChange={(e) => setForm({ ...form, sellingPrice: e.target.value })} required />
        </label>
        <label className="form__field">
          <span>Reorder level</span>
          <input type="number" min="0" step="1" className="input" value={form.reorderLevel} onChange={(e) => setForm({ ...form, reorderLevel: e.target.value })} />
        </label>
        <label className="form__field">
          <span>Status</span>
          <select className="input" value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
            {STATUS_OPTIONS.map((status) => (
              <option key={status} value={status}>{status}</option>
            ))}
          </select>
        </label>
        <label className="checkbox">
          <input type="checkbox" checked={form.taxInclusive} onChange={(e) => setForm({ ...form, taxInclusive: e.target.checked })} />
          Tax inclusive
        </label>
        <label className="checkbox">
          <input type="checkbox" checked={form.trackInventory} onChange={(e) => setForm({ ...form, trackInventory: e.target.checked })} />
          Track inventory
        </label>
        <label className="form__field form__field--wide">
          <span>Description</span>
          <input className="input" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
        </label>
      </>
    );
  }

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="eyebrow">Catalog</p>
          <h1>Products</h1>
          <p className="subtitle">{totalElements} product(s)</p>
        </div>
        {canManage && (
          <div className="page__header-actions">
            <button type="button" className="btn btn--primary" onClick={() => { setShowCreate((v) => !v); setSelectedId(null); setForm(emptyForm); }}>
              {showCreate ? 'Cancel' : 'New product'}
            </button>
          </div>
        )}
      </header>

      {showCreate && canManage && (
        <section className="panel">
          <h2>Create product</h2>
          <form className="form form--grid form--touch-friendly" onSubmit={handleCreate}>
            {renderProductFields(false)}
            <div className="form__field form__field--wide">
              <button type="submit" className="btn btn--primary" disabled={saving}>{saving ? 'Saving…' : 'Create product'}</button>
            </div>
          </form>
        </section>
      )}

      <div className="toolbar">
        <input type="search" className="input" placeholder="Search SKU, name, or brand…" value={search} onChange={(e) => { setPage(0); setSearch(e.target.value); }} />
      </div>

      {loading && <p className="muted">Loading products…</p>}
      {error && <p className="form__error">{error}</p>}

      {!loading && !error && (
        <div className={`workspace-split${selectedId != null ? ' workspace-split--open' : ''}`}>
          <div className="workspace-split__list">
            <div className="table-wrap table-wrap--stacked">
              <table className="table table--stacked">
                <thead>
                  <tr><th>SKU</th><th>Product</th><th>Category</th><th>Price</th><th>Status</th></tr>
                </thead>
                <tbody>
                  {items.length === 0 ? (
                    <tr><td colSpan={5} className="muted">No products match your search.</td></tr>
                  ) : (
                    items.map((product) => (
                      <tr
                        key={product.id}
                        className={`table__row--clickable${selectedId === product.id ? ' table__row--selected' : ''}`}
                        onClick={() => { setShowCreate(false); setSelectedId(product.id); }}
                      >
                        <td data-label="SKU"><strong>{product.sku}</strong></td>
                        <td data-label="Product"><strong>{product.name}</strong>{product.brand && <div className="muted">{product.brand}</div>}</td>
                        <td data-label="Category">{product.categoryName ?? '—'}</td>
                        <td data-label="Price">{formatMoney(product.sellingPrice, product.currencyCode || currencyCode)}</td>
                        <td data-label="Status"><span className={`pill ${product.status === 'ACTIVE' ? 'pill--ok' : 'pill--warning'}`}>{product.status}</span></td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
            {totalPages > 1 && (
              <div className="pager">
                <button type="button" className="btn btn--ghost" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Previous</button>
                <span className="muted">Page {page + 1} of {totalPages}</span>
                <button type="button" className="btn btn--ghost" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>Next</button>
              </div>
            )}
          </div>

          {selectedId != null && (
            <aside className="workspace-split__detail panel">
              {detailLoading && <p className="muted">Loading product…</p>}
              {selectedProduct && canManage && (
                <>
                  <h2>{selectedProduct.name}</h2>
                  <form className="form form--touch-friendly" onSubmit={handleUpdate}>
                    {renderProductFields(true)}
                    <button type="submit" className="btn btn--primary" disabled={saving}>{saving ? 'Saving…' : 'Save changes'}</button>
                  </form>
                  <h3 className="panel__subheading">Barcodes</h3>
                  <ul className="list">
                    {(selectedProduct.barcodes ?? []).map((barcode) => (
                      <li key={barcode.id} className="list__item">
                        <div>
                          <strong>{barcode.barcode}</strong>
                          <p className="muted">{barcode.barcodeType}{barcode.primary ? ' · primary' : ''}</p>
                        </div>
                        <button type="button" className="btn btn--ghost" onClick={() => handleRemoveBarcode(barcode.id)}>Remove</button>
                      </li>
                    ))}
                  </ul>
                  <form className="pos-barcode" onSubmit={handleAddBarcode}>
                    <input className="input" placeholder="Barcode" value={newBarcode} onChange={(e) => setNewBarcode(e.target.value)} />
                    <select className="input" value={newBarcodeType} onChange={(e) => setNewBarcodeType(e.target.value)}>
                      {BARCODE_TYPES.map((type) => (
                        <option key={type} value={type}>{type}</option>
                      ))}
                    </select>
                    <button type="submit" className="btn btn--ghost">Add</button>
                  </form>
                </>
              )}
              {selectedProduct && !canManage && (
                <>
                  <h2>{selectedProduct.name}</h2>
                  <p className="muted">{selectedProduct.sku}</p>
                  <p>{selectedProduct.description}</p>
                  <p>{formatMoney(selectedProduct.sellingPrice, selectedProduct.currencyCode || currencyCode)}</p>
                </>
              )}
            </aside>
          )}
        </div>
      )}
    </div>
  );
}
