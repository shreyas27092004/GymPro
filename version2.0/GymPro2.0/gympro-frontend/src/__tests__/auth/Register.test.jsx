import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import Register from '../../auth/Register';
import * as api from '../../api/api';

jest.mock('../../api/api', () => ({
  authApi: { register: jest.fn() },
}));

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

beforeAll(() => jest.useFakeTimers());
afterAll(() => jest.useRealTimers());
beforeEach(() => jest.clearAllMocks());

function renderRegister() {
  return render(<MemoryRouter><Register /></MemoryRouter>);
}

describe('Register', () => {
  test('renders all form fields', () => {
    renderRegister();
    expect(screen.getByPlaceholderText('John Doe')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('you@example.com')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('••••••••')).toBeInTheDocument();
    // Button text is 'Create Account'
    expect(screen.getByText('Create Account')).toBeInTheDocument();
  });

  test('shows success message on successful registration', async () => {
    api.authApi.register.mockResolvedValueOnce({
      data: { message: 'Registered successfully! Please sign in.' },
    });
    const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
    renderRegister();
    await user.type(screen.getByPlaceholderText('John Doe'), 'Alice');
    await user.type(screen.getByPlaceholderText('you@example.com'), 'alice@gym.com');
    await user.type(screen.getByPlaceholderText('••••••••'), 'pass123');
    await user.click(screen.getByText('Create Account'));
    await waitFor(() => {
      expect(screen.getByText(/registered successfully/i)).toBeInTheDocument();
    });
  });

  test('shows error message on failed registration', async () => {
    api.authApi.register.mockRejectedValueOnce({
      response: { data: { message: 'Email already exists' } },
    });
    const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
    renderRegister();
    await user.type(screen.getByPlaceholderText('John Doe'), 'Bob');
    await user.type(screen.getByPlaceholderText('you@example.com'), 'bob@gym.com');
    await user.type(screen.getByPlaceholderText('••••••••'), 'pass123');
    await user.click(screen.getByText('Create Account'));
    await waitFor(() => {
      expect(screen.getByText('Email already exists')).toBeInTheDocument();
    });
  });

  test('role dropdown defaults to MEMBER', () => {
    renderRegister();
    expect(screen.getByRole('combobox').value).toBe('MEMBER');
  });

  test('role dropdown can be changed to TRAINER', async () => {
    const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
    renderRegister();
    await user.selectOptions(screen.getByRole('combobox'), 'TRAINER');
    expect(screen.getByRole('combobox').value).toBe('TRAINER');
  });

  test('has a link back to Sign in', () => {
    renderRegister();
    // Link text is 'Sign in' (lowercase i)
    expect(screen.getByText('Sign in')).toBeInTheDocument();
  });
});
