import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { AuthProvider, useAuth } from './lib/auth';
import { ToastProvider } from './lib/toast';
import Login from './pages/Login';
import Overview from './pages/Overview';
import Players from './pages/Players';

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
          {/* Every other page (Economy, Warps, Kits, Holograms, Discord, Permissions, Backups,
              Commands, Logs, Users, PublicLookup) lands here until a later pass ports it —
              matches the approved plan's MVP-first scope, not a bug. */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </ToastProvider>
    </AuthProvider>
  );
}
