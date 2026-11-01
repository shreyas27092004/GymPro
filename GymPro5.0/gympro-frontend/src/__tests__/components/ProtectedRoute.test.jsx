// ProtectedRoute uses useAuth() internally — wrap with real AuthProvider
// and set sessionStorage to simulate different auth states.
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ProtectedRoute from '../../components/ProtectedRoute';
import { AuthProvider } from '../../context/AuthContext';

function renderRoute(sessionData, allowedRoles) {
  // Set sessionStorage BEFORE rendering so AuthProvider picks it up
  sessionStorage.clear();
  if (sessionData.token) sessionStorage.setItem('token', sessionData.token);
  if (sessionData.role)  sessionStorage.setItem('role',  sessionData.role);

  return render(
    <MemoryRouter>
      <AuthProvider>
        <ProtectedRoute allowedRoles={allowedRoles}>
          <div>Protected Content</div>
        </ProtectedRoute>
      </AuthProvider>
    </MemoryRouter>
  );
}

afterEach(() => sessionStorage.clear());

describe('ProtectedRoute', () => {
  test('redirects to /login when there is no token', () => {
    renderRoute({}, ['MEMBER']);
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
  });

  test('renders children when token exists and role is allowed', () => {
    renderRoute({ token: 'tok', role: 'MEMBER' }, ['MEMBER']);
    expect(screen.getByText('Protected Content')).toBeInTheDocument();
  });

  test('redirects when role is not in allowedRoles', () => {
    renderRoute({ token: 'tok', role: 'MEMBER' }, ['ADMIN']);
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
  });

  test('allows ADMIN when ADMIN is in allowedRoles', () => {
    renderRoute({ token: 'tok', role: 'ADMIN' }, ['ADMIN', 'TRAINER']);
    expect(screen.getByText('Protected Content')).toBeInTheDocument();
  });

  test('renders children when no allowedRoles restriction is set', () => {
    renderRoute({ token: 'tok', role: 'TRAINER' }, null);
    expect(screen.getByText('Protected Content')).toBeInTheDocument();
  });
});
