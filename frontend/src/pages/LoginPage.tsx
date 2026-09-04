import { FormEvent, useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { MdlLogo } from '../components/brand/MdlLogo';
import { useAuth } from '../auth/AuthContext';

export function LoginPage() {
  const { login, completeMfa, user, isLoading } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [loginId, setLoginId] = useState('');
  const [password, setPassword] = useState('');
  const [mfaToken, setMfaToken] = useState<string | null>(null);
  const [mfaCode, setMfaCode] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const redirectTo =
    (location.state as { from?: string } | null)?.from ?? '/dashboard';

  if (isLoading) {
    return (
      <div className="auth-page">
        <div className="auth-card">
          <p className="muted">Loading session…</p>
        </div>
      </div>
    );
  }

  if (user) {
    return <Navigate to={redirectTo} replace />;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);

    try {
      if (mfaToken) {
        await completeMfa(mfaToken, mfaCode.trim());
        navigate(redirectTo, { replace: true });
        return;
      }

      const result = await login(loginId.trim(), password);
      if (result.mfaRequired && result.mfaToken) {
        setMfaToken(result.mfaToken);
        setPassword('');
      } else {
        navigate(redirectTo, { replace: true });
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Sign in failed');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <header className="auth-card__header">
          <MdlLogo variant="auth" />
          <h1>{mfaToken ? 'Verify MFA' : 'Sign in'}</h1>
          <p className="subtitle">Business management platform</p>
        </header>

        <form className="form" onSubmit={handleSubmit} noValidate>
          {!mfaToken ? (
            <>
              <label className="form__field">
                <span>Email or username</span>
                <input
                  type="text"
                  autoComplete="username"
                  value={loginId}
                  onChange={(event) => setLoginId(event.target.value)}
                  required
                />
              </label>

              <label className="form__field">
                <span>Password</span>
                <input
                  type="password"
                  autoComplete="current-password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  required
                />
              </label>
            </>
          ) : (
            <label className="form__field">
              <span>Authentication code</span>
              <input
                type="text"
                inputMode="numeric"
                autoComplete="one-time-code"
                value={mfaCode}
                onChange={(event) => setMfaCode(event.target.value)}
                required
              />
            </label>
          )}

          {error && <p className="form__error">{error}</p>}

          <button type="submit" className="btn btn--primary btn--block" disabled={submitting}>
            {submitting ? 'Signing in…' : mfaToken ? 'Verify' : 'Sign in'}
          </button>
        </form>

        {!mfaToken && (
          <p className="hint auth-card__hint">
            Demo: owner@mdl.local / Owner@123! · manager: michael@mdl.local / Manager@123!
          </p>
        )}

        <Link to="/" className="auth-card__back">
          ← System status
        </Link>
      </div>
    </div>
  );
}
