import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { AppShell } from './components/AppShell';
import { AppLayout } from './components/layout/AppLayout';
import { ApprovalsPage } from './pages/ApprovalsPage';
import { CopilotPage } from './pages/CopilotPage';
import { DashboardPage } from './pages/DashboardPage';
import { HomePage } from './pages/HomePage';
import { ImportsPage } from './pages/ImportsPage';
import { InventoryPage } from './pages/InventoryPage';
import { LoginPage } from './pages/LoginPage';
import { NotificationsPage } from './pages/NotificationsPage';
import { ProductsPage } from './pages/ProductsPage';
import { ReportsPage } from './pages/ReportsPage';
import { SalesPage } from './pages/SalesPage';
import { StocktakesPage } from './pages/StocktakesPage';
import { UsersPage } from './pages/admin/UsersPage';
import { BusinessSettingsPage } from './pages/admin/BusinessSettingsPage';
import { LocationsAdminPage } from './pages/admin/LocationsAdminPage';
import { ApprovalRulesPage } from './pages/admin/ApprovalRulesPage';
import { AuditLogsPage } from './pages/admin/AuditLogsPage';
import { TransfersPage } from './pages/TransfersPage';
import './styles/globals.css';

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppShell>
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route element={<ProtectedRoute />}>
              <Route element={<AppLayout />}>
                <Route path="/dashboard" element={<DashboardPage />} />
                <Route path="/inventory" element={<InventoryPage />} />
                <Route path="/products" element={<ProductsPage />} />
                <Route path="/transfers" element={<TransfersPage />} />
                <Route path="/imports" element={<ImportsPage />} />
                <Route path="/sales" element={<SalesPage />} />
                <Route path="/reports" element={<ReportsPage />} />
                <Route path="/approvals" element={<ApprovalsPage />} />
                <Route path="/notifications" element={<NotificationsPage />} />
                <Route path="/copilot" element={<CopilotPage />} />
                <Route path="/stocktakes" element={<StocktakesPage />} />
                <Route path="/admin/users" element={<UsersPage />} />
                <Route path="/admin/settings" element={<BusinessSettingsPage />} />
                <Route path="/admin/locations" element={<LocationsAdminPage />} />
                <Route path="/admin/approval-rules" element={<ApprovalRulesPage />} />
                <Route path="/admin/audit" element={<AuditLogsPage />} />
              </Route>
            </Route>
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </AppShell>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
