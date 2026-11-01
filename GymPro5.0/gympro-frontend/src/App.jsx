// src/App.jsx — Updated
// Changes vs original:
//   1. Added ToastProvider wrapper
//   2. Added ChatbotWidget (rendered outside Routes, visible after login)
//   3. Wrapped each Dashboard with ErrorBoundary

import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ToastProvider } from './components/Toast';
import ProtectedRoute from './components/ProtectedRoute';
import ErrorBoundary  from './components/ErrorBoundary';
import ChatbotWidget  from './components/ChatbotWidget';

import Landing           from './pages/Landing';
import Login            from './auth/Login';
import Register         from './auth/Register';
import ForgotPassword   from './auth/ForgotPassword';
import AdminDashboard   from './pages/AdminDashboard';
import TrainerDashboard from './pages/TrainerDashboard';
import MemberDashboard  from './pages/MemberDashboard';
import Forbidden        from './pages/Forbidden';

// Inner component has access to AuthContext for ChatbotWidget visibility
function AppRoutes() {
  return (
    <>
      <Routes>
        <Route path="/"                element={<Landing />} />
        <Route path="/login"           element={<Login />} />
        <Route path="/register"        element={<Register />} />
        <Route path="/403"             element={<Forbidden />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />

        <Route path="/admin" element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <ErrorBoundary title="Admin Dashboard Error">
              <AdminDashboard />
            </ErrorBoundary>
          </ProtectedRoute>
        } />

        <Route path="/trainer" element={
          <ProtectedRoute allowedRoles={['TRAINER', 'ADMIN']}>
            <ErrorBoundary title="Trainer Dashboard Error">
              <TrainerDashboard />
            </ErrorBoundary>
          </ProtectedRoute>
        } />

        <Route path="/member" element={
          <ProtectedRoute allowedRoles={['MEMBER', 'ADMIN']}>
            <ErrorBoundary title="Member Dashboard Error">
              <MemberDashboard />
            </ErrorBoundary>
          </ProtectedRoute>
        } />

        <Route path="*"  element={<Navigate to="/" replace />} />
      </Routes>

      {/* Floating chatbot — only shown when authenticated */}
      <ChatbotWidget />
    </>
  );
}

function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <BrowserRouter>
          <AppRoutes />
        </BrowserRouter>
      </ToastProvider>
    </AuthProvider>
  );
}

export default App;