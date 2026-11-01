// src/components/ProtectedRoute.jsx
// Reads BOTH React context state AND sessionStorage so the route is never
// blank immediately after login (before the context state re-render settles).

import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function ProtectedRoute({ allowedRoles, children }) {
  const { token: ctxToken, role: ctxRole } = useAuth();

  // sessionStorage is written synchronously inside login() BEFORE navigate()
  // is called, so it is always up-to-date even on the very first render after
  // navigation — whereas React context state may not have propagated yet.
  const token = ctxToken || sessionStorage.getItem('token');
  const role  = ctxRole  || sessionStorage.getItem('role');

  if (!token) return <Navigate to="/login" replace />;
  if (allowedRoles && !allowedRoles.includes(role)) return <Navigate to="/403" replace />;
  return children;
}