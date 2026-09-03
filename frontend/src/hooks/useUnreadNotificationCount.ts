import { useEffect, useState } from 'react';
import { getAccessToken } from '../auth/authStorage';
import { fetchUnreadNotificationCount } from '../services/notificationsService';
import type { NotificationItem } from '../types/api';

export const INBOX_UPDATE_EVENT = 'mdl:inbox-update';

function resolveWebSocketBase(): string {
  const apiBase = import.meta.env.VITE_API_BASE_URL ?? '';
  if (apiBase) {
    return apiBase.replace(/^http/i, 'ws');
  }
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}`;
}

export function useUnreadNotificationCount(refreshKey: string | number = 0) {
  const [count, setCount] = useState(0);

  useEffect(() => {
    let cancelled = false;

    fetchUnreadNotificationCount()
      .then((response) => {
        if (!cancelled) {
          setCount(response.unreadCount);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setCount(0);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [refreshKey]);

  useEffect(() => {
    const token = getAccessToken();
    if (!token) {
      return;
    }

    const ws = new WebSocket(
      `${resolveWebSocketBase()}/ws/notifications?token=${encodeURIComponent(token)}`,
    );

    ws.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data as string) as {
          type?: string;
          unreadCount?: number;
          notification?: NotificationItem;
        };
        if (payload.type === 'INBOX_UPDATE' && typeof payload.unreadCount === 'number') {
          setCount(payload.unreadCount);
          window.dispatchEvent(new CustomEvent(INBOX_UPDATE_EVENT, { detail: payload.notification }));
        }
      } catch {
        // ignore malformed frames
      }
    };

    return () => {
      ws.close();
    };
  }, []);

  return count;
}
