import { useCallback, useEffect, useState } from 'react';
import {
  dismissNotification,
  fetchNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from '../services/notificationsService';
import type { NotificationItem } from '../types/api';

const STATUS_FILTERS = [
  { value: '', label: 'All statuses' },
  { value: 'UNREAD', label: 'Unread' },
  { value: 'READ', label: 'Read' },
  { value: 'DISMISSED', label: 'Dismissed' },
];

const CATEGORY_FILTERS = [
  { value: '', label: 'All categories' },
  { value: 'ALERT', label: 'Alerts' },
  { value: 'APPROVAL', label: 'Approvals' },
  { value: 'SECURITY', label: 'Security' },
  { value: 'SYSTEM', label: 'System' },
];

function categoryPillClass(category: string): string {
  switch (category) {
    case 'ALERT':
      return 'pill--warning';
    case 'SECURITY':
      return 'pill--critical';
    case 'APPROVAL':
      return 'pill--info';
    default:
      return '';
  }
}

function formatLabel(value: string): string {
  return value
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

export function NotificationsPage() {
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [statusFilter, setStatusFilter] = useState('UNREAD');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [actingId, setActingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const loadNotifications = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetchNotifications({
        status: statusFilter || undefined,
        category: categoryFilter || undefined,
        page,
        size: 20,
      });
      setItems(response.items);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load notifications');
    } finally {
      setLoading(false);
    }
  }, [categoryFilter, page, statusFilter]);

  useEffect(() => {
    loadNotifications();
  }, [loadNotifications]);

  async function handleMarkRead(id: number) {
    setActingId(id);
    try {
      await markNotificationRead(id);
      await loadNotifications();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to mark notification read');
    } finally {
      setActingId(null);
    }
  }

  async function handleDismiss(id: number) {
    setActingId(id);
    try {
      await dismissNotification(id);
      await loadNotifications();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to dismiss notification');
    } finally {
      setActingId(null);
    }
  }

  async function handleMarkAllRead() {
    setActingId(-1);
    try {
      await markAllNotificationsRead();
      await loadNotifications();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to mark all read');
    } finally {
      setActingId(null);
    }
  }

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="eyebrow">Inbox</p>
          <h1>Notifications</h1>
          <p className="subtitle">{totalElements} notification(s)</p>
        </div>
        <div className="page__header-actions">
          <button
            type="button"
            className="btn btn--ghost"
            onClick={handleMarkAllRead}
            disabled={loading || actingId !== null}
          >
            Mark all read
          </button>
          <button type="button" className="btn btn--ghost" onClick={loadNotifications} disabled={loading}>
            Refresh
          </button>
        </div>
      </header>

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
        <select
          className="input input--compact"
          value={categoryFilter}
          onChange={(event) => {
            setPage(0);
            setCategoryFilter(event.target.value);
          }}
        >
          {CATEGORY_FILTERS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>

      {loading && <p className="muted">Loading notifications…</p>}
      {error && <p className="form__error">{error}</p>}

      {!loading && !error && (
        <>
          {items.length === 0 ? (
            <section className="panel empty-state-panel">
              <p className="empty-state">
                <span className="empty-state__icon" aria-hidden="true">
                  🔔
                </span>
                <strong>No notifications</strong>
                <span className="muted">You're all caught up.</span>
              </p>
            </section>
          ) : (
            <ul className="list list--cards">
              {items.map((item) => (
                <li
                  key={item.id}
                  className={`approval-card${item.status === 'UNREAD' ? ' notification-card--unread' : ''}`}
                >
                  <div className="approval-card__header">
                    <div>
                      <span className={`pill ${categoryPillClass(item.category)}`}>
                        {formatLabel(item.category)}
                      </span>
                      {item.entityRef && <strong>{item.entityRef}</strong>}
                    </div>
                    <span className={`pill ${item.status === 'UNREAD' ? 'pill--ok' : ''}`}>
                      {formatLabel(item.status)}
                    </span>
                  </div>
                  <h2>{item.title}</h2>
                  <p className="muted">{item.message}</p>
                  <p className="muted notification-card__time">
                    {new Date(item.createdAt).toLocaleString()}
                  </p>
                  {item.status !== 'DISMISSED' && (
                    <div className="approval-actions__buttons">
                      {item.status === 'UNREAD' && (
                        <button
                          type="button"
                          className="btn btn--primary"
                          disabled={actingId === item.id}
                          onClick={() => handleMarkRead(item.id)}
                        >
                          {actingId === item.id ? 'Working…' : 'Mark read'}
                        </button>
                      )}
                      <button
                        type="button"
                        className="btn btn--ghost"
                        disabled={actingId === item.id}
                        onClick={() => handleDismiss(item.id)}
                      >
                        Dismiss
                      </button>
                    </div>
                  )}
                </li>
              ))}
            </ul>
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
    </div>
  );
}
