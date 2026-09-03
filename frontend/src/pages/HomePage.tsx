import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { MdlLogo } from '../components/brand/MdlLogo';
import { getHealthStatus } from '../services/apiClient';
import type { HealthStatus } from '../types/api';

type LoadState = 'loading' | 'ok' | 'error' | 'offline';

export function HomePage() {
  const { user } = useAuth();
  const [loadState, setLoadState] = useState<LoadState>('loading');
  const [health, setHealth] = useState<HealthStatus | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getHealthStatus()
      .then((data) => {
        setHealth(data);
        setLoadState('ok');
      })
      .catch((err: Error) => {
        if (!navigator.onLine) {
          setLoadState('offline');
          return;
        }
        setError(err.message);
        setLoadState('error');
      });
  }, []);

  return (
    <div className="landing">
      <header className="header">
        <MdlLogo variant="full" />
        <p className="subtitle">Business management for inventory-based businesses</p>
      </header>

      <main className="card">
        {loadState === 'loading' && <p className="status loading">Checking system…</p>}

        {loadState === 'offline' && (
          <div className="status error">
            <strong>Offline mode</strong>
            <p>The app shell is available. Connect to load live system status.</p>
          </div>
        )}

        {loadState === 'error' && (
          <div className="status error">
            <strong>Cannot reach backend</strong>
            <p>{error}</p>
            <p className="hint">Start MariaDB and the Spring Boot backend, then refresh.</p>
          </div>
        )}

        {loadState === 'ok' && health && (
          <div className="status ok">
            <div className="badge">System OK</div>
            <dl className="details">
              <div>
                <dt>Application</dt>
                <dd>{health.application}</dd>
              </div>
              <div>
                <dt>Version</dt>
                <dd>{health.version}</dd>
              </div>
              <div>
                <dt>Database</dt>
                <dd className={health.database === 'UP' ? 'up' : 'down'}>{health.database}</dd>
              </div>
              <div>
                <dt>Checked at</dt>
                <dd>{new Date(health.timestamp).toLocaleString()}</dd>
              </div>
            </dl>
          </div>
        )}

        <div className="landing__actions">
          {user ? (
            <Link to="/dashboard" className="btn btn--primary">
              Open dashboard
            </Link>
          ) : (
            <Link to="/login" className="btn btn--primary">
              Sign in
            </Link>
          )}
        </div>
      </main>

      <footer className="footer">
        <p>Installable PWA — add to home screen on phone or tablet</p>
      </footer>
    </div>
  );
}
