import { useEffect, useState, type FormEvent } from 'react';
import { useAuth } from '../../auth/AuthContext';
import { mfaConfirmRequest, mfaSetupRequest } from '../../services/apiClient';
import { fetchBusiness, fetchCurrencies, updateBusiness } from '../../services/businessService';
import type { BusinessProfile, CurrencyOption, MfaSetupResponse } from '../../types/api';

export function BusinessSettingsPage() {
  const { hasPermission, user } = useAuth();
  const canManage = hasPermission('business:manage');

  const [business, setBusiness] = useState<BusinessProfile | null>(null);
  const [currencies, setCurrencies] = useState<CurrencyOption[]>([]);
  const [name, setName] = useState('');
  const [legalName, setLegalName] = useState('');
  const [currencyCode, setCurrencyCode] = useState('GHS');
  const [timezone, setTimezone] = useState('Africa/Accra');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [mfaSetup, setMfaSetup] = useState<MfaSetupResponse | null>(null);
  const [mfaCode, setMfaCode] = useState('');
  const [mfaBusy, setMfaBusy] = useState(false);

  useEffect(() => {
    Promise.all([fetchBusiness(), fetchCurrencies()])
      .then(([profile, currencyList]) => {
        setBusiness(profile);
        setCurrencies(currencyList);
        setName(profile.name);
        setLegalName(profile.legalName ?? '');
        setCurrencyCode(profile.currencyCode);
        setTimezone(profile.timezone);
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to load settings'))
      .finally(() => setLoading(false));
  }, []);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!canManage) {
      return;
    }
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      const updated = await updateBusiness({
        name: name.trim(),
        legalName: legalName.trim() || undefined,
        currencyCode,
        timezone,
      });
      setBusiness(updated);
      setSuccess('Business settings saved.');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save settings');
    } finally {
      setSaving(false);
    }
  }

  async function startMfaSetup() {
    setMfaBusy(true);
    setError(null);
    try {
      setMfaSetup(await mfaSetupRequest());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to start MFA setup');
    } finally {
      setMfaBusy(false);
    }
  }

  async function confirmMfaSetup(event: FormEvent) {
    event.preventDefault();
    setMfaBusy(true);
    setError(null);
    try {
      await mfaConfirmRequest(mfaCode.trim());
      setMfaSetup(null);
      setMfaCode('');
      setSuccess('Two-factor authentication is now enabled.');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Invalid verification code');
    } finally {
      setMfaBusy(false);
    }
  }

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="eyebrow">Administration</p>
          <h1>Business settings</h1>
          <p className="subtitle">{business?.code ?? 'Loading…'}</p>
        </div>
      </header>

      {loading && <p className="muted">Loading settings…</p>}
      {error && <p className="form__error">{error}</p>}
      {success && <p className="form__success">{success}</p>}

      {!loading && business && (
        <section className="panel">
          <form className="form form--touch-friendly" onSubmit={handleSubmit}>
            <label className="form__field">
              <span>Business name</span>
              <input className="input" value={name} onChange={(e) => setName(e.target.value)} required disabled={!canManage} />
            </label>
            <label className="form__field">
              <span>Legal name</span>
              <input className="input" value={legalName} onChange={(e) => setLegalName(e.target.value)} disabled={!canManage} />
            </label>
            <label className="form__field">
              <span>Currency</span>
              <select className="input" value={currencyCode} onChange={(e) => setCurrencyCode(e.target.value)} disabled={!canManage}>
                {currencies.map((c) => (
                  <option key={c.code} value={c.code}>
                    {c.code} — {c.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="form__field">
              <span>Timezone</span>
              <input className="input" value={timezone} onChange={(e) => setTimezone(e.target.value)} required disabled={!canManage} />
            </label>
            {canManage && (
              <div className="form__field">
                <button type="submit" className="btn btn--primary" disabled={saving}>
                  {saving ? 'Saving…' : 'Save settings'}
                </button>
              </div>
            )}
          </form>
        </section>
      )}

      <section className="panel">
        <h2>Two-factor authentication</h2>
        <p className="muted">
          {user?.mfaEnabled
            ? 'MFA is enabled on your account.'
            : 'Add an authenticator app for extra sign-in protection.'}
        </p>
        {!user?.mfaEnabled && !mfaSetup && (
          <button type="button" className="btn btn--primary" disabled={mfaBusy} onClick={startMfaSetup}>
            {mfaBusy ? 'Starting…' : 'Set up MFA'}
          </button>
        )}
        {mfaSetup && (
          <form className="form" onSubmit={confirmMfaSetup}>
            <p className="muted">Scan this URL in your authenticator app or enter the secret manually:</p>
            <code className="code-block">{mfaSetup.otpAuthUrl}</code>
            <p className="muted">Secret: {mfaSetup.secret}</p>
            <label className="form__field">
              <span>Verification code</span>
              <input
                className="input"
                inputMode="numeric"
                autoComplete="one-time-code"
                value={mfaCode}
                onChange={(e) => setMfaCode(e.target.value)}
                required
              />
            </label>
            <button type="submit" className="btn btn--primary" disabled={mfaBusy}>
              {mfaBusy ? 'Confirming…' : 'Enable MFA'}
            </button>
          </form>
        )}
      </section>
    </div>
  );
}
