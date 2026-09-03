import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { fetchAttentionDashboard } from '../services/alertsService';
import { fetchInventorySummary } from '../services/inventoryService';
import type { InventorySummary, OwnerAttentionReport } from '../types/api';

export function DashboardPage() {
  const { user, hasPermission } = useAuth();
  const [attention, setAttention] = useState<OwnerAttentionReport | null>(null);
  const [inventory, setInventory] = useState<InventorySummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const canViewAlerts = hasPermission('alert:view');
  const canViewInventory = hasPermission('inventory:view');

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);

      try {
        const tasks: Promise<void>[] = [];

        if (canViewAlerts) {
          tasks.push(
            fetchAttentionDashboard().then((data) => {
              if (!cancelled) {
                setAttention(data);
              }
            }),
          );
        }

        if (canViewInventory) {
          tasks.push(
            fetchInventorySummary().then((data) => {
              if (!cancelled) {
                setInventory(data);
              }
            }),
          );
        }

        await Promise.all(tasks);
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Failed to load dashboard');
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
  }, [canViewAlerts, canViewInventory]);

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="eyebrow">Overview</p>
          <h1>Dashboard</h1>
          <p className="subtitle">
            {user?.businessName} · {user?.currencyCode}
          </p>
        </div>
      </header>

      {loading && <p className="muted">Loading dashboard…</p>}
      {error && <p className="form__error">{error}</p>}

      {!loading && !error && (
        <div className="grid grid--dashboard">
          {canViewInventory && inventory && (
            <section className="panel">
              <div className="panel__header">
                <h2>Inventory</h2>
                <Link to="/inventory" className="panel__link">
                  View balances
                </Link>
              </div>
              <dl className="stat-grid">
                <div>
                  <dt>Balance rows</dt>
                  <dd>{inventory.balanceRowCount}</dd>
                </div>
                <div>
                  <dt>Low stock</dt>
                  <dd className={inventory.lowStockCount > 0 ? 'warn' : ''}>
                    {inventory.lowStockCount}
                  </dd>
                </div>
                <div>
                  <dt>Pending adjustments</dt>
                  <dd>{inventory.pendingAdjustmentRequests}</dd>
                </div>
                <div>
                  <dt>Active reservations</dt>
                  <dd>{inventory.activeReservations}</dd>
                </div>
              </dl>
            </section>
          )}

          {canViewAlerts && attention && (
            <section className="panel">
              <div className="panel__header">
                <h2>Attention center</h2>
              </div>
              <dl className="stat-grid">
                <div>
                  <dt>Open alerts</dt>
                  <dd>{attention.totalOpenAlerts}</dd>
                </div>
                <div>
                  <dt>Critical</dt>
                  <dd className={attention.criticalCount > 0 ? 'critical' : ''}>
                    {attention.criticalCount}
                  </dd>
                </div>
                <div>
                  <dt>Warnings</dt>
                  <dd className={attention.warningCount > 0 ? 'warn' : ''}>
                    {attention.warningCount}
                  </dd>
                </div>
              </dl>

              {attention.categories.length > 0 && (
                <ul className="list">
                  {attention.categories.map((category) => (
                    <li key={category.code} className="list__item">
                      <div>
                        <strong>{category.title}</strong>
                        <p className="muted">{category.summary}</p>
                      </div>
                      <span className={`pill pill--${category.severity.toLowerCase()}`}>
                        {category.count}
                      </span>
                    </li>
                  ))}
                </ul>
              )}

              {attention.recentAlerts.length > 0 && (
                <>
                  <h3 className="panel__subheading">Recent alerts</h3>
                  <ul className="list">
                    {attention.recentAlerts.slice(0, 5).map((alert) => (
                      <li key={alert.id} className="list__item">
                        <div>
                          <strong>{alert.title}</strong>
                          <p className="muted">{alert.summary}</p>
                        </div>
                        <span className={`pill pill--${alert.severity.toLowerCase()}`}>
                          {alert.severity}
                        </span>
                      </li>
                    ))}
                  </ul>
                </>
              )}
            </section>
          )}

          {!canViewAlerts && !canViewInventory && (
            <section className="panel">
              <p className="muted">No dashboard modules available for your role.</p>
            </section>
          )}
        </div>
      )}
    </div>
  );
}
