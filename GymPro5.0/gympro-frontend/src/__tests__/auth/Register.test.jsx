import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import Register from '../../auth/Register';
import * as api from '../../api/api';

jest.mock('../../api/api', () => ({
  authApi: { register: jest.fn(), verifyAdminRegistration: jest.fn() },
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

  describe('ADMIN registration approval flow', () => {
    test('shows the code-entry step when backend asks for verification', async () => {
      api.authApi.register.mockResolvedValueOnce({
        data: {
          verificationRequired: true,
          message: 'Registration pending. An existing admin has been emailed a verification code.',
        },
      });
      const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
      renderRegister();
      await user.type(screen.getByPlaceholderText('John Doe'), 'Carol');
      await user.type(screen.getByPlaceholderText('you@example.com'), 'carol@gym.com');
      await user.type(screen.getByPlaceholderText('••••••••'), 'pass123');
      await user.selectOptions(screen.getByRole('combobox'), 'ADMIN');
      await user.click(screen.getByText('Create Account'));

      await waitFor(() => {
        expect(screen.getByText(/an existing admin has been emailed/i)).toBeInTheDocument();
        expect(screen.getByText('Verify & Create Admin Account')).toBeInTheDocument();
      });
    });

    test('creates the admin account once the correct code is entered', async () => {
      api.authApi.register.mockResolvedValueOnce({
        data: { verificationRequired: true, message: 'Registration pending.' },
      });
      api.authApi.verifyAdminRegistration.mockResolvedValueOnce({
        data: { message: 'Admin account verified and created ✅ Please login to get your token.' },
      });
      const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
      renderRegister();
      await user.type(screen.getByPlaceholderText('John Doe'), 'Carol');
      await user.type(screen.getByPlaceholderText('you@example.com'), 'carol@gym.com');
      await user.type(screen.getByPlaceholderText('••••••••'), 'pass123');
      await user.selectOptions(screen.getByRole('combobox'), 'ADMIN');
      await user.click(screen.getByText('Create Account'));
      await waitFor(() => screen.getByText('Verify & Create Admin Account'));

      const boxes = screen.getAllByRole('textbox').filter(el => el.getAttribute('maxlength') === '1');
      for (let i = 0; i < 6; i++) {
        await user.type(boxes[i], String(i + 1));
      }
      await user.click(screen.getByText('Verify & Create Admin Account'));

      await waitFor(() => {
        expect(api.authApi.verifyAdminRegistration).toHaveBeenCalledWith({
          email: 'carol@gym.com', otp: '123456',
        });
        expect(screen.getByText(/admin account verified and created/i)).toBeInTheDocument();
      });
    });

    test('shows an error and lets the code be retried when it is wrong', async () => {
      api.authApi.register.mockResolvedValueOnce({
        data: { verificationRequired: true, message: 'Registration pending.' },
      });
      api.authApi.verifyAdminRegistration.mockResolvedValueOnce({
        data: { verificationRequired: true, message: 'Invalid or expired verification code.' },
      });
      const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
      renderRegister();
      await user.type(screen.getByPlaceholderText('John Doe'), 'Dave');
      await user.type(screen.getByPlaceholderText('you@example.com'), 'dave@gym.com');
      await user.type(screen.getByPlaceholderText('••••••••'), 'pass123');
      await user.selectOptions(screen.getByRole('combobox'), 'ADMIN');
      await user.click(screen.getByText('Create Account'));
      await waitFor(() => screen.getByText('Verify & Create Admin Account'));

      const boxes = screen.getAllByRole('textbox').filter(el => el.getAttribute('maxlength') === '1');
      for (let i = 0; i < 6; i++) {
        await user.type(boxes[i], '9');
      }
      await user.click(screen.getByText('Verify & Create Admin Account'));

      await waitFor(() => {
        expect(screen.getByText('Invalid or expired verification code.')).toBeInTheDocument();
      });
    });
  });
});
