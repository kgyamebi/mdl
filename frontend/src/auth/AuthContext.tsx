import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { clearSession, getAccessToken, getStoredUser, saveSession } from './authStorage';
import {
  bootstrapSession,
  fetchCurrentUser,
  loginRequest,
  logoutRequest,
  mfaChallengeRequest,
  setSessionExpiredHandler,
} from '../services/apiClient';
import type { AuthUser } from '../types/api';

interface AuthContextValue {
  user: AuthUser | null;
  isLoading: boolean;
  login: (login: string, password: string) => Promise<{ mfaRequired: boolean; mfaToken?: string }>;
  completeMfa: (mfaToken: string, code: string) => Promise<void>;
  logout: () => Promise<void>;
  hasPermission: (permission: string) => boolean;
  hasAnyPermission: (...permissions: string[]) => boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => getStoredUser());
  const [isLoading, setIsLoading] = useState(true);

  const handleSessionExpired = useCallback(() => {
    clearSession();
    setUser(null);
  }, []);

  useEffect(() => {
    setSessionExpiredHandler(handleSessionExpired);
  }, [handleSessionExpired]);

  useEffect(() => {
    let cancelled = false;

    async function bootstrap() {
      // Prefer refresh so role/permission changes in the database apply without forcing sign-out.
      try {
        const session = await bootstrapSession();
        if (session?.accessToken && session.user && !cancelled) {
          saveSession(session.accessToken, null, session.user);
          setUser(session.user);
          setIsLoading(false);
          return;
        }
      } catch {
        // Fall through to the stored access token below.
      }

      if (getAccessToken() && getStoredUser()) {
        try {
          const currentUser = await fetchCurrentUser();
          if (!cancelled) {
            setUser(currentUser);
          }
        } catch {
          if (!cancelled) {
            clearSession();
            setUser(null);
          }
        } finally {
          if (!cancelled) {
            setIsLoading(false);
          }
        }
        return;
      }

      if (!cancelled) {
        clearSession();
        setUser(null);
        setIsLoading(false);
      }
    }

    bootstrap();
    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (loginId: string, password: string) => {
    const response = await loginRequest(loginId, password);
    if (response.mfaRequired && response.mfaToken) {
      return { mfaRequired: true, mfaToken: response.mfaToken };
    }
    if (response.accessToken && response.user) {
      saveSession(response.accessToken, null, response.user);
      setUser(response.user);
    }
    return { mfaRequired: false };
  }, []);

  const completeMfa = useCallback(async (mfaToken: string, code: string) => {
    const response = await mfaChallengeRequest(mfaToken, code);
    if (response.accessToken && response.user) {
      saveSession(response.accessToken, null, response.user);
      setUser(response.user);
    }
  }, []);

  const logout = useCallback(async () => {
    await logoutRequest();
    setUser(null);
  }, []);

  const hasPermission = useCallback(
    (permission: string) => user?.permissions.includes(permission) ?? false,
    [user],
  );

  const hasAnyPermission = useCallback(
    (...permissions: string[]) =>
      permissions.some((permission) => user?.permissions.includes(permission) ?? false),
    [user],
  );

  const value = useMemo(
    () => ({ user, isLoading, login, completeMfa, logout, hasPermission, hasAnyPermission }),
    [user, isLoading, login, completeMfa, logout, hasPermission, hasAnyPermission],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}

