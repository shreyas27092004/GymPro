import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import Login from '../../auth/Login';
import { AuthProvider } from '../../context/AuthContext';
import * as api from '../../api/api';

jest.mock('../../api/api', () => ({
  authApi: { login: jest.fn() },
}));

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
  useLocation: () => ({ state: null }),
}));

beforeEach(() => {
  jest.clearAllMocks();
  sessionStorage.clear();
});

function renderLogin() {
  return render(
    <AuthProvider>
      <MemoryRouter>
        <Login />
      </MemoryRouter>
    </AuthProvider>
  );
}

describe('Login', () => {
  test('renders email, password fields and Sign In button', () => {
    renderLogin();
    expect(screen.getByPlaceholderText('you@example.com')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('••••••••')).toBeInTheDocument();
    // Button text is 'Sign In' (capital I)
    expect(screen.getByText('Sign In')).toBeInTheDocument();
  });

  test('shows error alert on failed login', async () => {
    api.authApi.login.mockRejectedValueOnce({
      response: { data: { message: 'Invalid email or password' } },
    });
    const user = userEvent.setup();
    renderLogin();
    await user.type(screen.getByPlaceholderText('you@example.com'), 'bad@email.com');
    await user.type(screen.getByPlaceholderText('••••••••'), 'wrongpass');
    await user.click(screen.getByText('Sign In'));
    await waitFor(() => {
      expect(screen.getByText('Invalid email or password')).toBeInTheDocument();
    });
  });

  test('navigates to /member on successful MEMBER login', async () => {
    api.authApi.login.mockResolvedValueOnce({
      data: { token: 'tok', role: 'MEMBER', email: 'member@gym.com', name: 'Alice' },
    });
    const user = userEvent.setup();
    renderLogin();
    await user.type(screen.getByPlaceholderText('you@example.com'), 'member@gym.com');
    await user.type(screen.getByPlaceholderText('••••••••'), 'pass123');
    await user.click(screen.getByText('Sign In'));
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/member'));
  });

  test('navigates to /admin on successful ADMIN login', async () => {
    api.authApi.login.mockResolvedValueOnce({
      data: { token: 'tok', role: 'ADMIN', email: 'admin@gym.com', name: 'Boss' },
    });
    const user = userEvent.setup();
    renderLogin();
    await user.type(screen.getByPlaceholderText('you@example.com'), 'admin@gym.com');
    await user.type(screen.getByPlaceholderText('••••••••'), 'admin123');
    await user.click(screen.getByText('Sign In'));
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/admin'));
  });

  test('navigates to /trainer on successful TRAINER login', async () => {
    api.authApi.login.mockResolvedValueOnce({
      data: { token: 'tok', role: 'TRAINER', email: 'trainer@gym.com', name: 'Coach' },
    });
    const user = userEvent.setup();
    renderLogin();
    await user.type(screen.getByPlaceholderText('you@example.com'), 'trainer@gym.com');
    await user.type(screen.getByPlaceholderText('••••••••'), 'coach123');
    await user.click(screen.getByText('Sign In'));
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/trainer'));
  });

  test('shows Forgot password? and Register links', () => {
    renderLogin();
    expect(screen.getByText('Forgot password?')).toBeInTheDocument();
    expect(screen.getByText('Register')).toBeInTheDocument();
  });
});
