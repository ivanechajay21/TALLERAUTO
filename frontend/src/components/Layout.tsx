import { Suspense, useState } from 'react';
import { Outlet, Navigate } from 'react-router-dom';
import Sidebar from './Sidebar';
import Header from './Header';
import RouteLoading from './RouteLoading';
import { PermissionProvider } from '../context/PermissionContext';
import { SettingsProvider } from '../context/SettingsContext';
import { getCurrentUser } from '../services/authService';

export default function Layout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(false);
  const user = getCurrentUser();

  if (!user) {
    return <Navigate to="/acceso" replace />;
  }

  return (
    <PermissionProvider>
      <SettingsProvider>
        <div className="flex h-screen overflow-hidden bg-slate-50 dark:bg-slate-950">
          <Sidebar
            open={sidebarOpen}
            onClose={() => setSidebarOpen(false)}
            collapsed={collapsed}
            onToggleCollapse={() => setCollapsed((v) => !v)}
          />
          <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
            <Header onToggleSidebar={() => setSidebarOpen((v) => !v)} />
            <main className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
              <Suspense fallback={<RouteLoading />}>
                <Outlet />
              </Suspense>
            </main>
          </div>
        </div>
      </SettingsProvider>
    </PermissionProvider>
  );
}