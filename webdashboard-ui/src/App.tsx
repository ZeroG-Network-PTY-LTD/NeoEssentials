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
          {/* Every other page (Permissions, Backups, Commands, Logs, PublicLookup) lands here
              until a later pass ports it — matches the approved plan's MVP-first scope, not a
              bug. */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </ToastProvider>
    </AuthProvider>
  );
}
