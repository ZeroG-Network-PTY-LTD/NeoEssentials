import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';

const TOKEN_KEY = 'ne_session_token';

/** One entry of a CUSTOM-role user's per-tab access, from /api/auth/validate's "tabs" field —
 *  see webDashboard.groupPermissions (config.json) / AuthenticationFilter.java (mod source) for
 *  where this is actually enforced server-side; this list exists purely so the UI doesn't have
 *  to guess, it mirrors exactly what the server will or won't allow. */
export interface AuthTabPermission {
  tab: string;
  canRead: boolean;
  canWrite: boolean;
}

export interface AuthUser {
  username: string;
  role: 'ADMIN' | 'OPERATOR' | 'MODERATOR' | 'VIEWER' | 'CUSTOM' | string;
  mcUuid?: string | null;
  mcUsername?: string | null;
}

interface AuthContextValue {
  token: string | null;
  user: AuthUser | null;
  isAdmin: boolean;
  loading: boolean;
  login: (username: string, password: string) => Promise<{ success: boolean; message?: string }>;
  logout: () => void;
  /**
   * Whether the current user can see (write=false) or act on (write=true) a given dashboard
   * tab id (see DashboardTab.java for the canonical list: economy, players, backups, etc).
   * ADMIN always true. The 4 other fixed roles (OPERATOR/MODERATOR/VIEWER, and the pre-CUSTOM-
   * feature default of just checking isAdmin) are left to each page's own existing checks —
   * this only adds real signal for CUSTOM-role accounts, which have no other way to know what
   * they're allowed to see. Returns false for every tab on a non-CUSTOM, non-ADMIN role, which
   * is intentionally conservative: those roles should keep using their existing isAdmin/role
   * checks instead of this function until/unless they're also given explicit per-tab data.
   */
  can: (tab: string, write?: boolean) => boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/**
 * The mod's general /api/* auth middleware only checks the Authorization: Bearer header —
 * the sessionId cookie login also sets is not enough on its own (see docs/API.md's auth
 * tiers and DashboardAPI.withAuth()). So the token has to be captured from the login
 * response body and attached to every subsequent call by hand, here.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => sessionStorage.getItem(TOKEN_KEY));
  const [user, setUser] = useState<AuthUser | null>(null);
  const [tabs, setTabs] = useState<AuthTabPermission[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!token) {
      setLoading(false);
      return;
    }
    fetch('/api/auth/validate', { headers: { Authorization: `Bearer ${token}` } })
      .then((res) => (res.ok ? res.json() : Promise.reject(res)))
      .then((data) => {
        setUser(data.user ?? { username: data.username, role: data.role });
        setTabs(Array.isArray(data.tabs) ? data.tabs : []);
      })
      .catch(() => {
        sessionStorage.removeItem(TOKEN_KEY);
        setToken(null);
      })
      .finally(() => setLoading(false));
    // Only re-validate when the token itself changes (login/logout), not on every render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  const login = async (username: string, password: string) => {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });
    const data = await res.json();
    if (!res.ok || !data.success) {
      return { success: false, message: data.message ?? data.error ?? 'Login failed.' };
    }
    sessionStorage.setItem(TOKEN_KEY, data.sessionId);
    setToken(data.sessionId);
    setUser(data.user ?? { username: data.username, role: data.role });
    // The login response doesn't include "tabs" yet (only /api/auth/validate does) — clear any
    // stale value from a previous session rather than show it momentarily; the token-change
    // effect above re-fetches validate right after this and fills it in correctly.
    setTabs(Array.isArray(data.tabs) ? data.tabs : []);
    return { success: true };
  };

  const logout = () => {
    if (token) {
      fetch('/api/auth/logout', { method: 'POST', headers: { Authorization: `Bearer ${token}` } }).catch(() => {});
    }
    sessionStorage.removeItem(TOKEN_KEY);
    setToken(null);
    setUser(null);
    setTabs([]);
  };

  const can = (tab: string, write = false) => {
    if (user?.role === 'ADMIN') return true;
    if (user?.role !== 'CUSTOM') return false;
    const entry = tabs.find((t) => t.tab === tab);
    if (!entry) return false;
    return write ? entry.canWrite : entry.canRead;
  };

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      user,
      isAdmin: user?.role === 'ADMIN',
      loading,
      login,
      logout,
      can,
    }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [token, user, tabs, loading],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth() must be used inside <AuthProvider>');
  return ctx;
}

/**
 * fetch() wrapper that attaches the Bearer token and, on a 401 (session expired/revoked —
 * sessions are a 30-minute sliding idle timeout, in-memory only on the mod side, so this is
 * routine, not exceptional), clears the stored token so the app shell redirects to /login.
 */
export async function mcFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const token = sessionStorage.getItem(TOKEN_KEY);
  const headers = new Headers(init.headers);
  if (token) headers.set('Authorization', `Bearer ${token}`);
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');

  const res = await fetch(path, { ...init, headers });

  if (res.status === 401) {
    sessionStorage.removeItem(TOKEN_KEY);
    // Full reload rather than a router redirect — simplest way to force AuthProvider to
    // re-read (the now-absent) token and land back on the login screen from anywhere.
    window.location.href = '/login';
  }

  return res;
}
