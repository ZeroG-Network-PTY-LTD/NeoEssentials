import { useState, type FormEvent } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../lib/auth';

/**
 * Not a port of the external dashboard's Auth/Login.tsx — that page is Laravel's own account
 * system (email/password, "remember me", Discord OAuth, password reset), none of which exists
 * on the mod's side. This calls the mod's own POST /api/auth/login directly; visual shell
 * (centered card, theme tokens) matches GuestLayout.tsx for consistency.
 */
export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    const result = await login(username, password);
    setSubmitting(false);
    if (!result.success) {
      setError(result.message ?? 'Login failed.');
      return;
    }
    const from = (location.state as { from?: string } | null)?.from ?? '/';
    navigate(from, { replace: true });
  };

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-[var(--mc-bg-base)] px-4 py-10">
      <div className="flex items-center gap-2 text-[var(--mc-text-primary)]">
        <svg className="h-8 w-8 text-[var(--mc-cyan-500)]" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M12 2 3 6.5v11L12 22l9-4.5v-11L12 2Z" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round" />
          <path d="M3 6.5 12 11l9-4.5M12 11v11" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round" />
        </svg>
        <span className="font-display text-lg font-semibold tracking-tight">NeoEssentials</span>
      </div>

      <div className="mt-8 w-full overflow-hidden rounded-[var(--radius-lg)] border border-[var(--mc-border)] bg-[var(--mc-bg-surface)] px-6 py-8 shadow-2xl shadow-black/40 sm:max-w-md">
        <h1 className="font-display text-xl font-semibold text-[var(--mc-text-primary)]">Welcome back</h1>
        <p className="mt-1 text-sm text-[var(--mc-text-secondary)]">Log in to manage your server.</p>

        {error && (
          <div className="mb-4 mt-4 rounded-[var(--radius)] border border-[var(--mc-ember-400)] bg-[var(--mc-ember-50)] px-3 py-2 text-sm font-medium text-[var(--mc-ember-500)]">
            {error}
          </div>
        )}

        <form onSubmit={submit} className="mt-6">
          <div>
            <label htmlFor="username" className="block text-sm font-medium text-[var(--mc-text-secondary)]">
              Username
            </label>
            <input
              id="username"
              type="text"
              autoFocus
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="mt-1 block w-full rounded-[var(--radius)] border border-[var(--mc-border-strong)] bg-[var(--mc-bg-surface-raised)] px-3 py-2 text-[13px] text-[var(--mc-text-primary)] outline-none focus:border-[var(--mc-cyan-400)]"
            />
          </div>

          <div className="mt-4">
            <label htmlFor="password" className="block text-sm font-medium text-[var(--mc-text-secondary)]">
              Password
            </label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="mt-1 block w-full rounded-[var(--radius)] border border-[var(--mc-border-strong)] bg-[var(--mc-bg-surface-raised)] px-3 py-2 text-[13px] text-[var(--mc-text-primary)] outline-none focus:border-[var(--mc-cyan-400)]"
            />
          </div>

          <div className="mt-6 flex items-center justify-end">
            <button
              type="submit"
              disabled={submitting}
              className="btn-pop rounded-[var(--radius)] bg-[var(--mc-cyan-500)] px-4 py-2 text-[13px] font-medium text-[#0a1620] hover:bg-[var(--mc-cyan-400)] disabled:opacity-50 transition-colors"
            >
              {submitting ? 'Logging in…' : 'Log in'}
            </button>
          </div>
        </form>
      </div>

      <p className="mt-6 text-[13px] text-[var(--mc-text-muted)]">Powered by NeoEssentials</p>
    </div>
  );
}
