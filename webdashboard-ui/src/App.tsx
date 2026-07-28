import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { AuthProvider, useAuth } from './lib/auth';
import { ToastProvider } from './lib/toast';
import Login from './pages/Login';
import Overview from './pages/Overview';
import Players from './pages/Players';
import Economy from './pages/Economy';
import Warps from './pages/Warps';
import Kits from './pages/Kits';
import Holograms from './pages/Holograms';
import Discord from './pages/Discord';
import Users from './pages/Users';
import Backups from './pages/Backups';
import Commands from './pages/Commands';
import Logs from './pages/Logs';
import Permissions from './pages/Permissions';
import Settings from './pages/Settings';
import PublicLookup from './pages/PublicLookup';

function RequireAuth({ children }: { children: React.ReactNode }) {
  const { token, loading } = useAuth();
  const location = useLocation();

  if (loading) return null;
  if (!token) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  return <>{children}</>;
}

export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route
            path="/"
            element={
              <RequireAuth>
                <Overview />
              </RequireAuth>
            }
          />
          <Route
            path="/players"
            element={
              <RequireAuth>
                <Players />
              </RequireAuth>
            }
          />
          <Route
            path="/economy"
            element={
              <RequireAuth>
                <Economy />
              </RequireAuth>
            }
          />
          <Route
            path="/warps"
            element={
              <RequireAuth>
                <Warps />
              </RequireAuth>
            }
          />
          <Route
            path="/kits"
            element={
              <RequireAuth>
                <Kits />
              </RequireAuth>
            }
          />
          <Route
            path="/holograms"
            element={
              <RequireAuth>
                <Holograms />
              </RequireAuth>
            }
          />
          <Route
            path="/discord"
            element={
              <RequireAuth>
                <Discord />
              </RequireAuth>
            }
          />
          <Route
            path="/users"
            element={
              <RequireAuth>
                <Users />
              </RequireAuth>
            }
          />
          <Route
            path="/backups"
            element={
              <RequireAuth>
                <Backups />
              </RequireAuth>
            }
          />
          <Route
            path="/commands"
            element={
              <RequireAuth>
                <Commands />
              </RequireAuth>
            }
          />
          <Route
            path="/logs"
            element={
              <RequireAuth>
                <Logs />
              </RequireAuth>
            }
          />
          <Route
            path="/permissions"
            element={
              <RequireAuth>
                <Permissions />
              </RequireAuth>
            }
          />
          <Route
            path="/settings"
            element={
              <RequireAuth>
                <Settings />
              </RequireAuth>
            }
          />
          {/* No auth guard — mirrors the mod's own PUBLIC /api/public/moderation/* routes. */}
          <Route path="/lookup" element={<PublicLookup />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </ToastProvider>
    </AuthProvider>
  );
}
