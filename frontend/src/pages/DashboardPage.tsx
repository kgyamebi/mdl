import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { useUnreadNotificationCount } from '../hooks/useUnreadNotificationCount';
import { fetchAttentionDashboard } from '../services/alertsService';
import { fetchApprovalInbox } from '../services/approvalsService';
import { fetchBusinessOverview } from '../services/dashboardService';
import { fetchInventorySummary } from '../services/inventoryService';
import type { AttentionCategory, BusinessOverviewReport, OwnerAttentionReport } from '../types/api';

function formatMoney(value: number, currencyCode: string): string {
  return new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: currencyCode,
    maximumFractionDigits: 0,
  }).format(value);
}

function categoryCount(categories: AttentionCategory[], code: string): number {
  return categories.find((category) => category.code === code)?.count ?? 0;
}

interface SummaryCardProps {
  icon: string;
  label: string;
  value: string;
  hint?: string;
  to: string;
  tone?: 'default' | 'warn' | 'ok';
}

function SummaryCard({ icon, label, value, hint, to, tone = 'default' }: SummaryCardProps) {
  return (
    <Link to={to} className={`dashboard-card dashboard-card--${tone}`}>
      <span className="dashboard-card__icon" aria-hidden="true">
        {icon}
      </span>
      <div className="dashboard-card__body">
        <span className="dashboard-card__label">{label}</span>
        <strong className="dashboard-card__value">{value}</strong>
        {hint && <span className="dashboard-card__hint">{hint}</span>}
      </div>
    </Link>
  );
}

export function DashboardPage() {
  const { user, hasPermission } = useAuth();
  const unreadCount = useUnreadNotificationCount('/dashboard');

  const [attention, setAttention] = useState<OwnerAttentionReport | null>(null);
  const [overview, setOverview] = useState<BusinessOverviewReport | null>(null);
  const [inventory, setInventory] = useState<{ lowStockCount: number } | null>(null);
  const [approvalTotal, setApprovalTotal] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const canViewAlerts = hasPermission('alert:view');
  const canViewInventory = hasPermission('inventory:view');
  const canViewReports = hasPermission('report:view');
  const canViewApprovals = hasPermission('approval:view');
  const canViewSales = hasPermission('sale:view');
  const canViewTransfers = hasPermission('transfer:view');
  const currencyCode = user?.currencyCode ?? 'GHS';

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

        if (canViewReports) {
          tasks.push(
            fetchBusinessOverview().then((data) => {
              if (!cancelled) {
                setOverview(data);
              }
            }),
          );
        } else if (canViewInventory) {
          tasks.push(
            fetchInventorySummary().then((data) => {
              if (!cancelled) {
                setInventory({ lowStockCount: data.lowStockCount });
              }
            }),
          );
        }

        if (canViewApprovals) {
          tasks.push(
            fetchApprovalInbox(0, 1).then((data) => {
              if (!cancelled) {
                setApprovalTotal(data.summary.totalCount);
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
  }, [canViewAlerts, canViewApprovals, canViewInventory, canViewReports]);

  const lowStockCount = overview?.lowStockBalanceCount
    ?? inventory?.lowStockCount
    ?? (attention ? categoryCount(attention.categories, 'LOW_STOCK') : null);

  const pendingTransfers = overview?.pendingTransferRequests
    ?? (attention ? categoryCount(attention.categories, 'PENDING_TRANSFERS') : null);

  const salesTodayValue = canViewReports && overview
    ? formatMoney(overview.salesAmountToday, overview.currencyCode || currencyCode)
    : canViewSales
      ? 'View sales'
      : null;

  const salesTodayHint = canViewReports && overview
    ? `${overview.completedSalesToday} completed today`
    : canViewSales
      ? 'Open sales module'
      : undefined;

  const showSummaryCards =
    canViewReports || canViewInventory || canViewApprovals || canViewSales || unreadCount >= 0;

  const firstName = user?.fullName?.split(' ')[0] ?? 'there';

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="eyebrow">Overview</p>
          <h1>Dashboard</h1>
          <p className="subtitle">{user?.businessName}</p>
        </div>
      </header>

      {loading && <p className="muted">Loading dashboard…</p>}
      {error && <p className="form__error">{error}</p>}

      {!loading && !error && (
        <>
          <section className="dashboard-welcome panel">
            <div className="dashboard-welcome__copy">
              <h2>Welcome back, {firstName}</h2>
              <p className="muted">Your business snapshot for today — jump in where you need to.</p>
            </div>
            <div className="dashboard-welcome__actions">
              {canViewSales && (
                <Link to="/sales" className="btn btn--ghost">
                  Sales
                </Link>
              )}
              {canViewInventory && (
                <Link to="/inventory" className="btn btn--ghost">
                  Inventory
                </Link>
              )}
              {canViewApprovals && (
                <Link to="/approvals" className="btn btn--ghost">
                  Approvals
                </Link>
              )}
              {hasPermission('copilot:use') && (
                <Link to="/copilot" className="btn btn--primary">
                  Ask Copilot
                </Link>
              )}
            </div>
          </section>

          {showSummaryCards && (
            <section className="dashboard-summary" aria-label="Business snapshot">
              {(canViewReports || canViewSales) && salesTodayValue && (
                <SummaryCard
                  icon="💰"
                  label="Today's sales"
                  value={salesTodayValue}
                  hint={salesTodayHint}
                  to="/sales"
                  tone={overview && overview.completedSalesToday > 0 ? 'ok' : 'default'}
                />
              )}

              {canViewInventory && lowStockCount !== null && (
                <SummaryCard
                  icon="📦"
                  label="Low stock"
                  value={String(lowStockCount)}
                  hint={lowStockCount > 0 ? 'Needs replenishment' : 'Stock levels OK'}
                  to="/inventory"
                  tone={lowStockCount > 0 ? 'warn' : 'ok'}
                />
              )}

              {(canViewReports || canViewAlerts || canViewTransfers) && pendingTransfers !== null && (
                <SummaryCard
                  icon="🚚"
                  label="Pending transfers"
                  value={String(pendingTransfers)}
                  hint={pendingTransfers > 0 ? 'Awaiting approval' : 'No pending transfers'}
                  to="/transfers"
                  tone={pendingTransfers > 0 ? 'warn' : 'default'}
                />
              )}

              {canViewApprovals && approvalTotal !== null && (
                <SummaryCard
                  icon="✅"
                  label="Pending approvals"
                  value={String(approvalTotal)}
                  hint={approvalTotal > 0 ? 'Review inbox' : 'Inbox clear'}
                  to="/approvals"
                  tone={approvalTotal > 0 ? 'warn' : 'ok'}
                />
              )}

              <SummaryCard
                icon="🔔"
                label="Notifications"
                value={String(unreadCount)}
                hint={unreadCount > 0 ? 'Unread messages' : "You're all caught up"}
                to="/notifications"
                tone={unreadCount > 0 ? 'warn' : 'ok'}
              />
            </section>
          )}

          {canViewAlerts && attention && (
            <section className="panel">
              <div className="panel__header">
                <h2>Needs attention</h2>
              </div>
              {attention.categories.filter((category) => category.code !== 'ALL_CLEAR').length > 0 ? (
                <ul className="list">
                  {attention.categories
                    .filter((category) => category.code !== 'ALL_CLEAR')
                    .map((category) => (
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
              ) : (
                <p className="empty-state">
                  <span className="empty-state__icon" aria-hidden="true">
                    ✅
                  </span>
                  All clear — nothing needs immediate attention.
                </p>
              )}
            </section>
          )}

          {!canViewAlerts && !canViewInventory && !canViewReports && !canViewSales && (
            <section className="panel">
              <p className="muted">No dashboard modules available for your role.</p>
            </section>
          )}
        </>
      )}
    </div>
  );
}
