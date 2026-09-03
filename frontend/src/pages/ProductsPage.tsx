import { useEffect, useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { fetchProducts } from '../services/productsService';
import type { Product } from '../types/api';

function formatMoney(value: number, currencyCode: string): string {
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: currencyCode,
    maximumFractionDigits: 2,
  }).format(value);
}

export function ProductsPage() {
  const { user } = useAuth();
  const currencyCode = user?.currencyCode ?? 'GHS';
  const [items, setItems] = useState<Product[]>([]);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);

      try {
        const response = await fetchProducts({ search, page, size: 20 });
        if (!cancelled) {
          setItems(response.items);
          setTotalPages(response.totalPages);
          setTotalElements(response.totalElements);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Failed to load products');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [search, page]);

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="eyebrow">Catalog</p>
          <h1>Products</h1>
          <p className="subtitle">{totalElements} product(s)</p>
        </div>
      </header>

      <div className="toolbar">
        <input
          type="search"
          className="input"
          placeholder="Search SKU, name, or brand…"
          value={search}
          onChange={(event) => {
            setPage(0);
            setSearch(event.target.value);
          }}
        />
      </div>

      {loading && <p className="muted">Loading products…</p>}
      {error && <p className="form__error">{error}</p>}

      {!loading && !error && (
        <>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>SKU</th>
                  <th>Product</th>
                  <th>Category</th>
                  <th>Cost</th>
                  <th>Price</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {items.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="muted">
                      No products match your search.
                    </td>
                  </tr>
                ) : (
                  items.map((product) => (
                    <tr key={product.id}>
                      <td>
                        <strong>{product.sku}</strong>
                      </td>
                      <td>
                        <strong>{product.name}</strong>
                        {product.brand && <div className="muted">{product.brand}</div>}
                      </td>
                      <td>{product.categoryName ?? '—'}</td>
                      <td>{formatMoney(product.costPrice, product.currencyCode || currencyCode)}</td>
                      <td>{formatMoney(product.sellingPrice, product.currencyCode || currencyCode)}</td>
                      <td>
                        <span
                          className={`pill ${
                            product.status === 'ACTIVE' ? 'pill--ok' : 'pill--warning'
                          }`}
                        >
                          {product.status}
                        </span>
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
