import { useEffect, useState } from 'react';
import { fetchUnreadNotificationCount } from '../services/notificationsService';

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

  return count;
}
