import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { useAuth } from '../auth/AuthContext';
import { fetchLocations, fetchShops } from '../services/locationsService';
import {
  exportInventoryBalancesCsv,
  exportLowStockCsv,
  exportSalesSummaryCsv,
  fetchReportExports,
} from '../services/reportsService';
import type { LocationSummary, ReportExport, Shop } from '../types/api';

const REPORT_TYPE_FILTERS = [
  { value: '', label: 'All report types' },
  { value: 'SALES_SUMMARY', label: 'Sales summary' },
  { value: 'INVENTORY_BALANCES', label: 'Inventory balances' },
  { value: 'LOW_STOCK', label: 'Low stock' },
];

function formatReportType(value: string): string {
  return value
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

function formatParameters(parameters: string): string {
  if (!parameters) {
    return '—';
  }

  try {
    const parsed = JSON.parse(parameters) as Record<string, unknown>;
    const parts = Object.entries(parsed)
      .filter(([, value]) => value !== null && value !== undefined && value !== '')
      .map(([key, value]) => `${key}: ${String(value)}`);
    return parts.length > 0 ? parts.join(', ') : '—';
  } catch {
    return parameters;
  }
}

export function ReportsPage() {
  const { hasPermission } = useAuth();
  const canExport = hasPermission('report:export');

  const [shops, setShops] = useState<Shop[]>([]);
  const [locations, setLocations] = useState<LocationSummary[]>([]);
  const [history, setHistory] = useState<ReportExport[]>([]);
  const [reportTypeFilter, setReportTypeFilter] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const [salesShopId, setSalesShopId] = useState('');
  const [salesFrom, setSalesFrom] = useState('');
  const [salesTo, setSalesTo] = useState('');

  const [inventoryLocationId, setInventoryLocationId] = useState('');
  const [inventoryLowStockOnly, setInventoryLowStockOnly] = useState(false);

  const [lowStockLocationId, setLowStockLocationId] = useState('');

  const loadHistory = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetchReportExports({
        reportType: reportTypeFilter || undefined,
        page,
        size: 20,
      });
      setHistory(response.items);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load export history');
    } finally {
      setLoading(false);
    }
  }, [page, reportTypeFilter]);

  useEffect(() => {
    if (!canExport) {
      setLoading(false);
      return;
    }

    let cancelled = false;

    Promise.all([fetchShops(), fetchLocations()])
      .then(([shopList, locationList]) => {
        if (!cancelled) {
          setShops(shopList);
          setLocations(locationList);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Failed to load filters');
        }
      });

    return () => {
      cancelled = true;
    };
  }, [canExport]);

  useEffect(() => {
    if (canExport) {
      loadHistory();
    }
  }, [canExport, loadHistory]);

  async function runExport(key: string, action: () => Promise<void>, label: string) {
    setExporting(key);
    setError(null);
    setSuccess(null);

    try {
      await action();
      setSuccess(`${label} downloaded.`);
      await loadHistory();
    } catch (err) {
      setError(err instanceof Error ? err.message : `Failed to export ${label.toLowerCase()}`);
    } finally {
      setExporting(null);
    }
  }

  function handleSalesExport(event: FormEvent) {
    event.preventDefault();
    runExport('sales', () =>
      exportSalesSummaryCsv({
        shopId: salesShopId ? Number(salesShopId) : undefined,
        from: salesFrom || undefined,
        to: salesTo || undefined,
      }),
    'Sales summary');
  }

  function handleInventoryExport(event: FormEvent) {
    event.preventDefault();
    runExport('inventory', () =>
      exportInventoryBalancesCsv({
        locationId: inventoryLocationId ? Number(inventoryLocationId) : undefined,
        lowStockOnly: inventoryLowStockOnly,
      }),
    'Inventory balances');
  }

  function handleLowStockExport(event: FormEvent) {
    event.preventDefault();
    runExport('low-stock', () =>
      exportLowStockCsv({
        locationId: lowStockLocationId ? Number(lowStockLocationId) : undefined,
      }),
    'Low stock');
  }

  if (!canExport) {
    return (
      <div className="page">
        <header className="page__header">
          <div>
            <p className="eyebrow">Reports</p>
            <h1>Export downloads</h1>
          </div>
        </header>
        <section className="panel">
          <p className="muted">You need the <code>report:export</code> permission to download reports.</p>
        </section>
      </div>
    );
  }

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="eyebrow">Reports</p>
          <h1>Export downloads</h1>
          <p className="subtitle">CSV exports are logged for audit</p>
        </div>
        <div className="page__header-actions">
          <button type="button" className="btn btn--ghost" onClick={loadHistory} disabled={loading}>
            Refresh history
          </button>
        </div>
      </header>

      {error && <p className="form__error">{error}</p>}
      {success && <p className="form__success">{success}</p>}

      <div className="report-grid">
        <section className="panel report-panel">
          <h2>Sales summary</h2>
          <p className="muted">Metric/value pairs for sales totals in a date range.</p>
          <form className="report-form" onSubmit={handleSalesExport}>
            <label className="form__field">
              <span>Shop</span>
              <select
                className="input"
                value={salesShopId}
                onChange={(event) => setSalesShopId(event.target.value)}
              >
                <option value="">All shops</option>
                {shops.map((shop) => (
                  <option key={shop.id} value={shop.id}>
                    {shop.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="form__field">
              <span>From</span>
              <input
                type="datetime-local"
                className="input"
                value={salesFrom}
                onChange={(event) => setSalesFrom(event.target.value)}
              />
            </label>
            <label className="form__field">
              <span>To</span>
              <input
                type="datetime-local"
                className="input"
                value={salesTo}
                onChange={(event) => setSalesTo(event.target.value)}
              />
            </label>
            <button type="submit" className="btn btn--primary" disabled={exporting !== null}>
              {exporting === 'sales' ? 'Exporting…' : 'Download CSV'}
            </button>
          </form>
        </section>

        <section className="panel report-panel">
          <h2>Inventory balances</h2>
          <p className="muted">On-hand, reserved, and available stock by location.</p>
          <form className="report-form" onSubmit={handleInventoryExport}>
            <label className="form__field">
              <span>Location</span>
              <select
                className="input"
                value={inventoryLocationId}
                onChange={(event) => setInventoryLocationId(event.target.value)}
              >
                <option value="">All accessible locations</option>
                {locations.map((location) => (
                  <option key={location.id} value={location.id}>
                    {location.name} ({location.locationType})
                  </option>
                ))}
              </select>
            </label>
            <label className="checkbox">
              <input
                type="checkbox"
                checked={inventoryLowStockOnly}
                onChange={(event) => setInventoryLowStockOnly(event.target.checked)}
              />
              Low stock only
            </label>
            <button type="submit" className="btn btn--primary" disabled={exporting !== null}>
              {exporting === 'inventory' ? 'Exporting…' : 'Download CSV'}
            </button>
          </form>
        </section>

        <section className="panel report-panel">
          <h2>Low stock</h2>
          <p className="muted">Items at or below reorder level.</p>
          <form className="report-form" onSubmit={handleLowStockExport}>
            <label className="form__field">
              <span>Location</span>
              <select
                className="input"
                value={lowStockLocationId}
                onChange={(event) => setLowStockLocationId(event.target.value)}
              >
                <option value="">All accessible locations</option>
                {locations.map((location) => (
                  <option key={location.id} value={location.id}>
                    {location.name} ({location.locationType})
                  </option>
                ))}
              </select>
            </label>
            <button type="submit" className="btn btn--primary" disabled={exporting !== null}>
              {exporting === 'low-stock' ? 'Exporting…' : 'Download CSV'}
            </button>
          </form>
        </section>
      </div>

      <section className="panel">
        <div className="panel__header">
          <div>
            <h2>Export history</h2>
            <p className="muted">{totalElements} export(s)</p>
          </div>
          <select
            className="input input--compact"
            value={reportTypeFilter}
            onChange={(event) => {
              setPage(0);
              setReportTypeFilter(event.target.value);
            }}
          >
            {REPORT_TYPE_FILTERS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>

        {loading && <p className="muted">Loading export history…</p>}

        {!loading && (
          <>
            {history.length === 0 ? (
              <p className="muted">No exports yet.</p>
            ) : (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>When</th>
                      <th>Report</th>
                      <th>File</th>
                      <th>Rows</th>
                      <th>Parameters</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {history.map((item) => (
                      <tr key={item.id}>
                        <td>{new Date(item.createdAt).toLocaleString()}</td>
                        <td>{formatReportType(item.reportType)}</td>
                        <td>{item.fileName}</td>
                        <td>{item.rowCount}</td>
                        <td className="report-params">{formatParameters(item.parameters)}</td>
                        <td>
                          <span className="pill pill--ok">{formatReportType(item.status)}</span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

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
      </section>
    </div>
  );
}
