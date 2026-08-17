import { PropsWithChildren, useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { LayoutGrid, Users, Coins, MapPin, Package, Sparkles, MessageCircle, ShieldCheck, UserCog, DatabaseBackup, Terminal, ScrollText, Search, Radio, LogOut, UserRound, Menu, X, Settings, Flag } from 'lucide-react';
import { useAuth } from '../lib/auth';
import * as mcApi from '../lib/mcApi';

/**
 * Ported from the external Laravel dashboard's DashboardLayout.tsx — same visual shell, same
 * design tokens (theme.css), same nav-item pattern. What changed: `route()`/`<Link href>`
 * (Ziggy/Inertia) became react-router's `<Link to>`; `usePage().props.auth.user` became
 * `useAuth()`; the server-injected `apiReachable` Inertia prop became a local check against
 * the mod's own /api/server/status. Reverb/live-updates section dropped entirely — this pass
 * has no real-time wiring (see the plan's "not this session" follow-up list).
 *
 * Nav only lists the pages actually built so far (Overview, Players) — more are added as
 * later passes land, per the approved MVP-first plan.
 */
export default function DashboardLayout({ children }: PropsWithChildren) {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, isAdmin, logout } = useAuth();
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const [reachable, setReachable] = useState(true);

  useEffect(() => setMobileNavOpen(false), [location.pathname]);

  useEffect(() => {
    let cancelled = false;
    mcApi
      .status()
      .then(() => !cancelled && setReachable(true))
      .catch(() => !cancelled && setReachable(false));
    return () => {
      cancelled = true;
    };
  }, [location.pathname]);

  const nav = [
    { label: 'Overview', href: '/', icon: LayoutGrid },
    { label: 'Players', href: '/players', icon: Users },
    { label: 'Economy', href: '/economy', icon: Coins },
    { label: 'Warps', href: '/warps', icon: MapPin },
    { label: 'Kits', href: '/kits', icon: Package },
    { label: 'Holograms', href: '/holograms', icon: Sparkles },
    { label: 'Discord', href: '/discord', icon: MessageCircle },
    { label: 'Permissions', href: '/permissions', icon: ShieldCheck },
    { label: 'Backups', href: '/backups', icon: DatabaseBackup },
    { label: 'Commands', href: '/commands', icon: Terminal },
    { label: 'Logs', href: '/logs', icon: ScrollText },
    // Reviewing player reports is admin-only — the GET endpoints themselves are readable by
    // any logged-in dashboard account (see ModerationEndpoint's own doc comment), but review
    // actions require admin server-side, so there's nothing a non-admin could actually do here.
    ...(isAdmin ? [{ label: 'Reports', href: '/reports', icon: Flag }] : []),
    // Mod dashboard account management is admin-only, same gate as the external
    // dashboard's copy of this page.
    ...(isAdmin ? [{ label: 'Users', href: '/users', icon: UserCog }] : []),
  ];

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="min-h-screen flex bg-[var(--mc-bg-base)] text-[var(--mc-text-primary)]">
      {mobileNavOpen && (
        <div className="fixed inset-0 z-40 bg-black/60 lg:hidden" onClick={() => setMobileNavOpen(false)} />
      )}

      <aside
        className={`fixed inset-y-0 left-0 z-50 w-64 shrink-0 border-r border-[var(--mc-border)] bg-[var(--mc-bg-surface)] flex flex-col transition-transform duration-200 lg:static lg:translate-x-0 ${
          mobileNavOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="px-5 py-5 border-b border-[var(--mc-border)] flex items-center gap-2.5">
          <img src="/logo.png" alt="" className="h-8 w-8 shrink-0 object-contain" />
          <div className="min-w-0 flex-1">
            <div className="font-display text-[15px] font-semibold tracking-tight">NeoEssentials</div>
            <div className="text-[11px] text-[var(--mc-text-muted)] font-data mt-0.5">internal dashboard</div>
          </div>
          <button
            type="button"
            onClick={() => setMobileNavOpen(false)}
            className="p-1 rounded-[var(--radius)] text-[var(--mc-text-muted)] hover:text-[var(--mc-text-primary)] hover:bg-[var(--mc-bg-surface-raised)] lg:hidden"
          >
            <X size={18} />
          </button>
        </div>

        <nav className="flex-1 px-2.5 py-3 flex flex-col gap-0.5 overflow-y-auto">
          {nav.map(({ label, href, icon: Icon }) => {
            const active = location.pathname === href;
            return (
              <Link
                key={label}
                to={href}
                className={`group flex items-center gap-2.5 pl-3 pr-3 py-2 rounded-[8px] text-[13px] transition-all ${
                  active
                    ? 'bg-gradient-to-r from-[var(--mc-cyan-50)] to-transparent text-[var(--mc-cyan-400)] shadow-[inset_2px_0_0_0_var(--mc-cyan-500)]'
                    : 'text-[var(--mc-text-secondary)] hover:bg-[var(--mc-bg-surface-raised)] hover:text-[var(--mc-text-primary)]'
                }`}
              >
                <Icon size={16} strokeWidth={1.75} className={active ? '' : 'transition-transform group-hover:scale-110'} />
                {label}
              </Link>
            );
          })}
        </nav>

        <div className="px-2.5 py-2 border-t border-[var(--mc-border)] flex flex-col gap-0.5">
          <Link
            to="/lookup"
            className="flex items-center gap-2.5 pl-3 pr-3 py-2 rounded-[8px] text-[13px] text-[var(--mc-text-secondary)] hover:bg-[var(--mc-bg-surface-raised)] hover:text-[var(--mc-text-primary)] transition-colors"
          >
            <Search size={16} strokeWidth={1.75} />
            Player Lookup
          </Link>
        </div>

        <div className="px-4 py-3 border-t border-[var(--mc-border)]">
          <span
            className={`inline-flex items-center gap-2 rounded-full px-2.5 py-1 text-[12px] ${
              reachable ? 'bg-[var(--mc-moss-50)] text-[var(--mc-moss-500)]' : 'bg-[var(--mc-ember-50)] text-[var(--mc-ember-500)]'
            }`}
          >
            <Radio size={13} className="shrink-0" />
            {reachable ? 'API connected' : 'API unreachable'}
            <span className="pulse-dot relative ml-auto h-1.5 w-1.5 shrink-0 rounded-full bg-current" />
          </span>
        </div>

        <div className="border-t border-[var(--mc-border)] p-3 flex items-center gap-3">
          <Link to="/settings" className="group flex items-center gap-2.5 min-w-0 flex-1">
            {user?.mcUuid ? (
              <img
                src={`https://mc-heads.net/avatar/${user.mcUuid}/32`}
                alt=""
                className="h-8 w-8 rounded-[8px] shrink-0 [image-rendering:pixelated] border border-[var(--mc-border-strong)] transition-shadow group-hover:shadow-[0_0_0_2px_var(--mc-cyan-500)]"
              />
            ) : (
              <span className="h-8 w-8 rounded-[8px] shrink-0 bg-[var(--mc-bg-surface-raised)] border border-[var(--mc-border-strong)] flex items-center justify-center transition-shadow group-hover:shadow-[0_0_0_2px_var(--mc-cyan-500)]">
                <UserRound size={16} className="text-[var(--mc-text-muted)]" />
              </span>
            )}
            <span className="min-w-0">
              <span className="block text-[13px] font-medium truncate">{user?.username}</span>
              <span className="block text-[11px] text-[var(--mc-text-muted)] capitalize">{user?.role?.toLowerCase()}</span>
            </span>
          </Link>
          <Link
            to="/settings"
            title="Account settings"
            className="p-1.5 rounded-[var(--radius)] text-[var(--mc-text-muted)] hover:text-[var(--mc-text-primary)] hover:bg-[var(--mc-bg-surface-raised)] transition-colors"
          >
            <Settings size={16} strokeWidth={1.75} />
          </Link>
          <button
            type="button"
            title="Log out"
            onClick={handleLogout}
            className="p-1.5 rounded-[var(--radius)] text-[var(--mc-text-muted)] hover:text-[var(--mc-ember-500)] hover:bg-[var(--mc-bg-surface-raised)] transition-colors"
          >
            <LogOut size={16} strokeWidth={1.75} />
          </button>
        </div>
      </aside>

      <div className="flex-1 flex flex-col min-w-0">
        <div className="lg:hidden sticky top-0 z-30 flex items-center gap-3 px-4 py-3 border-b border-[var(--mc-border)] bg-[var(--mc-bg-surface)]">
          <button
            type="button"
            onClick={() => setMobileNavOpen(true)}
            className="p-1.5 rounded-[var(--radius)] text-[var(--mc-text-secondary)] hover:text-[var(--mc-text-primary)] hover:bg-[var(--mc-bg-surface-raised)]"
          >
            <Menu size={20} />
          </button>
          <span className="font-display text-[14px] font-semibold tracking-tight">NeoEssentials</span>
        </div>

        <main className="flex-1 px-4 py-5 sm:px-6 lg:px-8 lg:py-7 max-w-6xl w-full">{children}</main>
      </div>
    </div>
  );
}
