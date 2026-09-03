import { useCallback, useEffect, useState } from 'react';
import { INBOX_UPDATE_EVENT } from '../hooks/useUnreadNotificationCount';
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
  { value: 'OPERATIONS', label: 'Operations' },
  { value: 'ALERT', label: 'Alerts' },
  { value: 'APPROVAL', label: 'Approvals' },
  { value: 'SECURITY', label: 'Security' },
  { value: 'SYSTEM', label: 'System' },
];

function categoryPillClass(category: string): string {
  switch (category) {
    case 'OPERATIONS':
      return 'pill--info';
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
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actingId, setActingId] = useState<number | null>(null);

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
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load notifications');
    } finally {
      setLoading(false);
    }
  }, [categoryFilter, page, statusFilter]);

  useEffect(() => {
    loadNotifications();
  }, [loadNotifications]);

  useEffect(() => {
    function onInboxUpdate() {
      loadNotifications();
    }
    window.addEventListener(INBOX_UPDATE_EVENT, onInboxUpdate);
    return () => window.removeEventListener(INBOX_UPDATE_EVENT, onInboxUpdate);
  }, [loadNotifications]);

  async function handleMarkRead(id: number) {
    setActingId(id);
    try {
      await markNotificationRead(id);
      loadNotifications();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to mark read');
    } finally {
      setActingId(null);
    }
  }

  async function handleDismiss(id: number) {
    setActingId(id);
    try {
      await dismissNotification(id);
      loadNotifications();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to dismiss');
    } finally {
      setActingId(null);
    }
  }

  async function handleMarkAllRead() {
    setLoading(true);
    try {
      await markAllNotificationsRead();
      loadNotifications();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to mark all read');
    }
  }

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="eyebrow">Inbox</p>
          <h1>Notifications</h1>
          <p className="subtitle">Live updates for inventory, approvals, and alerts</p>
        </div>
        <div className="page__header-actions">
          <button
            type="button"
            className="btn btn--ghost"
            disabled={loading || actingId !== null}
            onClick={handleMarkAllRead}
          >
            Mark all read
          </button>
          <button type="button" className="btn btn--ghost" onClick={loadNotifications} disabled={loading}>
            Refresh
          </button>
        </div>
      </header>

      <div className="page__filters">
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
          <div className="notification-list">
            {items.length === 0 ? (
              <p className="muted">No notifications match your filters.</p>
            ) : (
              items.map((item) => (
                <article
                  key={item.id}
                  className={`notification-card${item.status === 'UNREAD' ? ' notification-card--unread' : ''}`}
                >
                  <div className="notification-card__header">
                    <span className={`pill ${categoryPillClass(item.category)}`}>
                      {formatLabel(item.category)}
                    </span>
                    <time className="muted">{new Date(item.createdAt).toLocaleString()}</time>
                  </div>
                  <h2 className="notification-card__title">{item.title}</h2>
                  <p>{item.message}</p>
                  <div className="notification-card__actions">
                    {item.status === 'UNREAD' && (
                      <button
                        type="button"
                        className="btn btn--ghost"
                        disabled={actingId === item.id}
                        onClick={() => handleMarkRead(item.id)}
                      >
                        Mark read
                      </button>
                    )}
                    {item.status !== 'DISMISSED' && (
                      <button
                        type="button"
                        className="btn btn--ghost"
                        disabled={actingId === item.id}
                        onClick={() => handleDismiss(item.id)}
                      >
                        Dismiss
                      </button>
                    )}
                  </div>
                </article>
              ))
            )}
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
