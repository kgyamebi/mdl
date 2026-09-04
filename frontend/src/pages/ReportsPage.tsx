import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { useAuth } from '../auth/AuthContext';
import { fetchLocations, fetchShops } from '../services/locationsService';
import {
  exportInventoryBalancesCsv,
  exportInventoryBalancesPdf,
  exportLowStockCsv,
  exportLowStockPdf,
  exportSalesSummaryCsv,
  exportSalesSummaryPdf,
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
  const { hasPermission, hasAnyPermission } = useAuth();
  const canView = hasAnyPermission('report:view', 'report:export');
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
    if (!canView) {
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
  }, [canView]);

  useEffect(() => {
    if (canView) {
      loadHistory();
    }
  }, [canView, loadHistory]);

  async function runExport(
    key: string,
    action: () => Promise<void>,
    label: string,
    format: 'CSV' | 'PDF',
  ) {
    setExporting(`${key}-${format}`);
    setError(null);
    setSuccess(null);

    try {
      await action();
      setSuccess(`${label} ${format} downloaded.`);
      await loadHistory();
    } catch (err) {
      setError(err instanceof Error ? err.message : `Failed to export ${label.toLowerCase()} ${format}`);
    } finally {
      setExporting(null);
    }
  }

  function handleSalesExport(event: FormEvent, format: 'CSV' | 'PDF') {
    event.preventDefault();
    const payload = {
      shopId: salesShopId ? Number(salesShopId) : undefined,
      from: salesFrom || undefined,
      to: salesTo || undefined,
    };
    runExport(
      'sales',
      () => (format === 'CSV' ? exportSalesSummaryCsv(payload) : exportSalesSummaryPdf(payload)),
      'Sales summary',
      format,
    );
  }

  function handleInventoryExport(event: FormEvent, format: 'CSV' | 'PDF') {
    event.preventDefault();
    const payload = {
      locationId: inventoryLocationId ? Number(inventoryLocationId) : undefined,
      lowStockOnly: inventoryLowStockOnly,
    };
    runExport(
      'inventory',
      () => (format === 'CSV' ? exportInventoryBalancesCsv(payload) : exportInventoryBalancesPdf(payload)),
      'Inventory balances',
      format,
    );
  }

  function handleLowStockExport(event: FormEvent, format: 'CSV' | 'PDF') {
    event.preventDefault();
    const payload = { locationId: lowStockLocationId ? Number(lowStockLocationId) : undefined };
    runExport(
      'low-stock',
      () => (format === 'CSV' ? exportLowStockCsv(payload) : exportLowStockPdf(payload)),
      'Low stock',
      format,
    );
  }

  if (!canView) {
    return (
      <div className="page">
        <header className="page__header">
          <div>
            <p className="eyebrow">Reports</p>
            <h1>Export downloads</h1>
          </div>
        </header>
        <section className="panel">
          <p className="muted">You need report access to view exports.</p>
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
          <p className="subtitle">
            {canExport
              ? 'Download CSV or branded PDF reports'
              : 'View export history. Download requires export permission.'}
          </p>
        </div>
        <div className="page__header-actions">
          <button type="button" className="btn btn--ghost" onClick={loadHistory} disabled={loading}>
            Refresh history
          </button>
        </div>
      </header>

      {error && <p className="form__error">{error}</p>}
      {success && <p className="form__success">{success}</p>}

      {canExport ? (
      <div className="report-grid">
        <section className="panel report-panel">
          <h2>Sales summary</h2>
          <p className="muted">Metric/value pairs for sales totals in a date range.</p>
          <form className="report-form form--touch-friendly" onSubmit={(e) => handleSalesExport(e, 'CSV')}>
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
            <div className="report-actions">
              <button type="submit" className="btn btn--primary" disabled={exporting !== null}>
                {exporting === 'sales-CSV' ? 'Exporting…' : 'Download CSV'}
              </button>
              <button
                type="button"
                className="btn btn--ghost"
                disabled={exporting !== null}
                onClick={(e) => handleSalesExport(e, 'PDF')}
              >
                {exporting === 'sales-PDF' ? 'Exporting…' : 'Download PDF'}
              </button>
            </div>
          </form>
        </section>

        <section className="panel report-panel">
          <h2>Inventory balances</h2>
          <p className="muted">On-hand, reserved, and available stock by location.</p>
          <form className="report-form form--touch-friendly" onSubmit={(e) => handleInventoryExport(e, 'CSV')}>
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
            <div className="report-actions">
              <button type="submit" className="btn btn--primary" disabled={exporting !== null}>
                {exporting === 'inventory-CSV' ? 'Exporting…' : 'Download CSV'}
              </button>
              <button
                type="button"
                className="btn btn--ghost"
                disabled={exporting !== null}
                onClick={(e) => handleInventoryExport(e, 'PDF')}
              >
                {exporting === 'inventory-PDF' ? 'Exporting…' : 'Download PDF'}
              </button>
            </div>
          </form>
        </section>

        <section className="panel report-panel">
          <h2>Low stock</h2>
          <p className="muted">Items at or below reorder level.</p>
          <form className="report-form form--touch-friendly" onSubmit={(e) => handleLowStockExport(e, 'CSV')}>
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
            <div className="report-actions">
              <button type="submit" className="btn btn--primary" disabled={exporting !== null}>
                {exporting === 'low-stock-CSV' ? 'Exporting…' : 'Download CSV'}
              </button>
              <button
                type="button"
                className="btn btn--ghost"
                disabled={exporting !== null}
                onClick={(e) => handleLowStockExport(e, 'PDF')}
              >
                {exporting === 'low-stock-PDF' ? 'Exporting…' : 'Download PDF'}
              </button>
            </div>
          </form>
        </section>
      </div>
      ) : (
        <section className="panel">
          <p className="muted">You can view export history below. Downloading new reports requires export permission.</p>
        </section>
      )}

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
              <div className="table-wrap table-wrap--stacked">
                <table className="table table--stacked">
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
