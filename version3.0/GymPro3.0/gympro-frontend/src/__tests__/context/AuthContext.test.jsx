import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AuthProvider, useAuth } from '../../context/AuthContext';

function AuthConsumer() {
  const { token, role, email, name, login, logout } = useAuth();
  return (
    <div>
      <span data-testid="token">{token || 'null'}</span>
      <span data-testid="role">{role || 'null'}</span>
      <span data-testid="email">{email || 'null'}</span>
      <span data-testid="name">{name || 'null'}</span>
      <button onClick={() => login('tok123', 'MEMBER', 'user@gym.com', 1, 'Alice')}>Login</button>
      <button onClick={logout}>Logout</button>
    </div>
  );
}

beforeEach(() => sessionStorage.clear());

describe('AuthContext', () => {
  test('initial state is null when sessionStorage is empty', () => {
    render(<AuthProvider><AuthConsumer /></AuthProvider>);
    expect(screen.getByTestId('token').textContent).toBe('null');
    expect(screen.getByTestId('role').textContent).toBe('null');
    expect(screen.getByTestId('email').textContent).toBe('null');
  });

  test('login() sets state and persists to sessionStorage', async () => {
    const user = userEvent.setup();
    render(<AuthProvider><AuthConsumer /></AuthProvider>);
    await user.click(screen.getByText('Login'));
    expect(screen.getByTestId('token').textContent).toBe('tok123');
    expect(screen.getByTestId('role').textContent).toBe('MEMBER');
    expect(screen.getByTestId('email').textContent).toBe('user@gym.com');
    expect(screen.getByTestId('name').textContent).toBe('Alice');
    expect(sessionStorage.getItem('token')).toBe('tok123');
    expect(sessionStorage.getItem('role')).toBe('MEMBER');
    expect(sessionStorage.getItem('email')).toBe('user@gym.com');
  });

  test('logout() clears all state and sessionStorage', async () => {
    const user = userEvent.setup();
    render(<AuthProvider><AuthConsumer /></AuthProvider>);
    await user.click(screen.getByText('Login'));
    await user.click(screen.getByText('Logout'));
    expect(screen.getByTestId('token').textContent).toBe('null');
    expect(screen.getByTestId('role').textContent).toBe('null');
    expect(sessionStorage.getItem('token')).toBeNull();
  });

  test('restores state from existing sessionStorage on mount', () => {
    sessionStorage.setItem('token', 'existing-tok');
    sessionStorage.setItem('role', 'ADMIN');
    sessionStorage.setItem('email', 'admin@gym.com');
    render(<AuthProvider><AuthConsumer /></AuthProvider>);
    expect(screen.getByTestId('token').textContent).toBe('existing-tok');
    expect(screen.getByTestId('role').textContent).toBe('ADMIN');
    expect(screen.getByTestId('email').textContent).toBe('admin@gym.com');
  });
});
