import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import ForgotPassword from '../../auth/ForgotPassword';
import * as api from '../../api/api';

jest.mock('../../api/api', () => ({
  authApi: {
    forgotPassword: jest.fn(),
    verifyOtp:      jest.fn(),
    resetPassword:  jest.fn(),
  },
}));

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

beforeAll(() => jest.useFakeTimers());
afterAll(() => jest.useRealTimers());
beforeEach(() => jest.clearAllMocks());

function renderFP() {
  return render(<MemoryRouter><ForgotPassword /></MemoryRouter>);
}

// ── Step 1: Email entry ────────────────────────────────────────────────────────
describe('ForgotPassword — Step 1 (Enter Email)', () => {
  test('renders GYMPRO logo and reset heading', () => {
    renderFP();
    expect(screen.getByText('GYMPRO')).toBeInTheDocument();
    expect(screen.getByText('Reset your password')).toBeInTheDocument();
  });

  test('renders all 3 step labels', () => {
    renderFP();
    expect(screen.getByText('Enter Email')).toBeInTheDocument();
    expect(screen.getByText('Verify OTP')).toBeInTheDocument();
    expect(screen.getByText('New Password')).toBeInTheDocument();
  });

  test('shows email input and Send OTP button', () => {
    renderFP();
    expect(screen.getByPlaceholderText('you@example.com')).toBeInTheDocument();
    expect(screen.getByText('Send OTP')).toBeInTheDocument();
  });

  test('renders Back to Sign In link', () => {
    renderFP();
    expect(screen.getByText('← Back to Sign In')).toBeInTheDocument();
  });

  test('advances to step 2 after successful OTP send', async () => {
    api.authApi.forgotPassword.mockResolvedValueOnce({ data: { success: true } });
    const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
    renderFP();
    await user.type(screen.getByPlaceholderText('you@example.com'), 'user@gym.com');
    await user.click(screen.getByText('Send OTP'));
    // "Verify OTP" step label is already present at step 1, so wait for the
    // actual step-2 content (6 OTP input boxes) to appear instead.
    await waitFor(() => {
      expect(screen.getAllByRole('textbox')).toHaveLength(6);
    });
  });

  test('shows error alert on failed OTP send', async () => {
    api.authApi.forgotPassword.mockRejectedValueOnce({
      response: { data: { message: 'Email not found' } },
    });
    const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
    renderFP();
    await user.type(screen.getByPlaceholderText('you@example.com'), 'noone@gym.com');
    await user.click(screen.getByText('Send OTP'));
    await waitFor(() => {
      expect(screen.getByText('Email not found')).toBeInTheDocument();
    });
  });

  test('shows generic error when no response message', async () => {
    api.authApi.forgotPassword.mockRejectedValueOnce(new Error('Network error'));
    const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
    renderFP();
    await user.type(screen.getByPlaceholderText('you@example.com'), 'user@gym.com');
    await user.click(screen.getByText('Send OTP'));
    await waitFor(() => {
      expect(screen.getByText('Failed to send OTP. Please try again.')).toBeInTheDocument();
    });
  });

  test('shows error alert when server returns success:false', async () => {
    api.authApi.forgotPassword.mockResolvedValueOnce({
      data: { success: false, message: 'Account disabled' },
    });
    const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
    renderFP();
    await user.type(screen.getByPlaceholderText('you@example.com'), 'user@gym.com');
    await user.click(screen.getByText('Send OTP'));
    await waitFor(() => {
      expect(screen.getByText('Account disabled')).toBeInTheDocument();
    });
  });
});

// ── Step 2: OTP verification ───────────────────────────────────────────────────
describe('ForgotPassword — Step 2 (Verify OTP)', () => {
  async function reachStep2() {
    api.authApi.forgotPassword.mockResolvedValueOnce({ data: { success: true } });
    const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
    renderFP();
    await user.type(screen.getByPlaceholderText('you@example.com'), 'user@gym.com');
    await user.click(screen.getByText('Send OTP'));
    await waitFor(() => expect(screen.getAllByRole('textbox')).toHaveLength(6));
    return user;
  }

  test('renders 6 OTP input boxes in step 2', async () => {
    await reachStep2();
    expect(screen.getAllByRole('textbox')).toHaveLength(6);
  });

  test('shows email address in OTP step', async () => {
    await reachStep2();
    expect(screen.getByText('user@gym.com')).toBeInTheDocument();
  });

  test('shows Verify OTP submit button', async () => {
    await reachStep2();
    expect(screen.getByRole('button', { name: /verify otp/i })).toBeInTheDocument();
  });

  test('shows resend countdown timer after OTP sent', async () => {
    await reachStep2();
    expect(screen.getByText(/resend in/i)).toBeInTheDocument();
  });

  test('shows Change email button to go back to step 1', async () => {
    await reachStep2();
    expect(screen.getByText('← Change email')).toBeInTheDocument();
  });

  test('clicking Change email returns to step 1', async () => {
    const user = await reachStep2();
    await user.click(screen.getByText('← Change email'));
    await waitFor(() => {
      expect(screen.getByText('Send OTP')).toBeInTheDocument();
    });
  });

  test('advances to step 3 on successful OTP verify', async () => {
    api.authApi.verifyOtp.mockResolvedValueOnce({ data: { success: true } });
    const user = await reachStep2();
    const inputs = screen.getAllByRole('textbox');
    for (let i = 0; i < 6; i++) {
      await user.type(inputs[i], String(i + 1));
    }
    await user.click(screen.getByRole('button', { name: /verify otp/i }));
    await waitFor(() => {
      expect(screen.getByPlaceholderText('Min. 6 characters')).toBeInTheDocument();
    });
  });

  test('shows error on invalid OTP', async () => {
    api.authApi.verifyOtp.mockRejectedValueOnce({
      response: { data: { message: 'OTP expired or incorrect' } },
    });
    const user = await reachStep2();
    const inputs = screen.getAllByRole('textbox');
    for (let i = 0; i < 6; i++) {
      await user.type(inputs[i], '9');
    }
    await user.click(screen.getByRole('button', { name: /verify otp/i }));
    await waitFor(() => {
      expect(screen.getByText('OTP expired or incorrect')).toBeInTheDocument();
    });
  });

  test('shows validation error if fewer than 6 digits submitted', async () => {
    const user = await reachStep2();
    // Only type in 3 boxes
    const inputs = screen.getAllByRole('textbox');
    await user.type(inputs[0], '1');
    await user.type(inputs[1], '2');
    await user.type(inputs[2], '3');
    await user.click(screen.getByRole('button', { name: /verify otp/i }));
    await waitFor(() => {
      expect(screen.getByText('Please enter all 6 digits.')).toBeInTheDocument();
    });
  });
});

// ── Step 3: Password reset ─────────────────────────────────────────────────────
describe('ForgotPassword — Step 3 (New Password)', () => {
  async function reachStep3() {
    api.authApi.forgotPassword.mockResolvedValueOnce({ data: { success: true } });
    api.authApi.verifyOtp.mockResolvedValueOnce({ data: { success: true } });
    const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
    renderFP();
    await user.type(screen.getByPlaceholderText('you@example.com'), 'user@gym.com');
    await user.click(screen.getByText('Send OTP'));
    await waitFor(() => expect(screen.getAllByRole('textbox')).toHaveLength(6));
    const inputs = screen.getAllByRole('textbox');
    for (let i = 0; i < 6; i++) {
      await user.type(inputs[i], '1');
    }
    await user.click(screen.getByRole('button', { name: /verify otp/i }));
    await waitFor(() => expect(screen.getByPlaceholderText('Min. 6 characters')).toBeInTheDocument());
    return user;
  }

  test('renders new password and confirm password fields', async () => {
    await reachStep3();
    expect(screen.getByPlaceholderText('Min. 6 characters')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Re-enter new password')).toBeInTheDocument();
  });

  test('renders Reset Password button', async () => {
    await reachStep3();
    expect(screen.getByRole('button', { name: /reset password/i })).toBeInTheDocument();
  });

  test('shows error when passwords do not match', async () => {
    const user = await reachStep3();
    await user.type(screen.getByPlaceholderText('Min. 6 characters'), 'abc123');
    await user.type(screen.getByPlaceholderText('Re-enter new password'), 'abc456');
    await user.click(screen.getByRole('button', { name: /reset password/i }));
    await waitFor(() => {
      expect(screen.getByText('Passwords do not match.')).toBeInTheDocument();
    });
  });

  test('shows error when password is shorter than 6 characters', async () => {
    const user = await reachStep3();
    await user.type(screen.getByPlaceholderText('Min. 6 characters'), 'abc');
    await user.type(screen.getByPlaceholderText('Re-enter new password'), 'abc');
    await user.click(screen.getByRole('button', { name: /reset password/i }));
    await waitFor(() => {
      expect(screen.getByText('Password must be at least 6 characters.')).toBeInTheDocument();
    });
  });

  test('shows success message and navigates to login after successful reset', async () => {
    api.authApi.resetPassword.mockResolvedValueOnce({ data: { success: true } });
    const user = await reachStep3();
    await user.type(screen.getByPlaceholderText('Min. 6 characters'), 'newpass123');
    await user.type(screen.getByPlaceholderText('Re-enter new password'), 'newpass123');
    await user.click(screen.getByRole('button', { name: /reset password/i }));
    await waitFor(() => {
      expect(screen.getByText(/password reset successfully/i)).toBeInTheDocument();
    });
    jest.advanceTimersByTime(2000);
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/login', expect.any(Object));
    });
  });

  test('shows error on failed password reset', async () => {
    api.authApi.resetPassword.mockRejectedValueOnce({
      response: { data: { message: 'OTP already used' } },
    });
    const user = await reachStep3();
    await user.type(screen.getByPlaceholderText('Min. 6 characters'), 'newpass123');
    await user.type(screen.getByPlaceholderText('Re-enter new password'), 'newpass123');
    await user.click(screen.getByRole('button', { name: /reset password/i }));
    await waitFor(() => {
      expect(screen.getByText('OTP already used')).toBeInTheDocument();
    });
  });

  test('password strength indicator appears when typing new password', async () => {
    const user = await reachStep3();
    await user.type(screen.getByPlaceholderText('Min. 6 characters'), 'abc');
    await waitFor(() => {
      // Strength label should appear (Too short / Weak / Fair etc.)
      expect(
        screen.getByText(/too short|weak|fair|good|strong/i)
      ).toBeInTheDocument();
    });
  });

  test('mismatch indicator appears inline when confirm does not match', async () => {
    const user = await reachStep3();
    await user.type(screen.getByPlaceholderText('Min. 6 characters'), 'abc123');
    await user.type(screen.getByPlaceholderText('Re-enter new password'), 'wrong');
    await waitFor(() => {
      expect(screen.getByText('Passwords do not match')).toBeInTheDocument();
    });
  });
});
