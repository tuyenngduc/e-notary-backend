import { Navigate, Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from './components/ProtectedRoute';
import { useAuth } from './context/AuthContext';
import { LoginPage } from './features/auth/pages/LoginPage';
import { RegisterPage } from './features/auth/pages/RegisterPage';
import { getDefaultRouteByRole } from './lib/roleRedirect';
import { HomePage } from './pages/HomePage';
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage';
import { CustomerDashboardPage } from './pages/customer/CustomerDashboardPage';
import { CustomerRequestsPage } from './pages/customer/CustomerRequestsPage';
import { NewRequestPage } from './pages/customer/NewRequestPage';
import { CustomerRequestDetailPage } from './pages/customer/CustomerRequestDetailPage';
import { NotaryDashboardPage } from './pages/notary/NotaryDashboardPage';
import { NotaryRequestDetailPage } from './pages/notary/NotaryRequestDetailPage';
import { NotaryRequestsPage } from './pages/notary/NotaryRequestsPage';
import { NotaryAppointmentsPage } from './pages/notary/NotaryAppointmentsPage';
import { ProfilePage } from './pages/ProfilePage';

function GuestRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, session } = useAuth();

  if (isAuthenticated) {
    return <Navigate to={getDefaultRouteByRole(session?.role)} replace />;
  }

  return <>{children}</>;
}

function RoleHomeRedirect() {
  const { isAuthenticated, session } = useAuth();
  if (!isAuthenticated) {
    return <Navigate to="/" replace />;
  }
  return <Navigate to={getDefaultRouteByRole(session?.role)} replace />;
}

export function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route
        path="/login"
        element={
          <GuestRoute>
            <LoginPage />
          </GuestRoute>
        }
      />
      <Route
        path="/register"
        element={
          <GuestRoute>
            <RegisterPage />
          </GuestRoute>
        }
      />

      <Route
        path="/customer/dashboard"
        element={
          <ProtectedRoute allowedRoles={['CLIENT']}>
            <CustomerDashboardPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/customer/requests"
        element={
          <ProtectedRoute allowedRoles={['CLIENT']}>
            <CustomerRequestsPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/customer/new-request"
        element={
          <ProtectedRoute allowedRoles={['CLIENT']}>
            <NewRequestPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/customer/request/:id"
        element={
          <ProtectedRoute allowedRoles={['CLIENT']}>
            <CustomerRequestDetailPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/notary/dashboard"
        element={
          <ProtectedRoute allowedRoles={['NOTARY']}>
            <NotaryDashboardPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/notary/requests"
        element={
          <ProtectedRoute allowedRoles={['NOTARY']}>
            <NotaryRequestsPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/notary/appointments"
        element={
          <ProtectedRoute allowedRoles={['NOTARY']}>
            <NotaryAppointmentsPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/notary/request/:id"
        element={
          <ProtectedRoute allowedRoles={['NOTARY']}>
            <NotaryRequestDetailPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/admin/dashboard"
        element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <AdminDashboardPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/profile"
        element={
          <ProtectedRoute allowedRoles={['CLIENT', 'NOTARY', 'ADMIN']}>
            <ProfilePage />
          </ProtectedRoute>
        }
      />

      <Route path="/dashboard" element={<RoleHomeRedirect />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
