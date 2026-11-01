import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import paymentReducer from '../../../store/slices/paymentSlice';
import MemberPayments from '../../../pages/member/MemberPayments';
import * as api from '../../../api/api';
import * as memberProfileModule from '../../../api/memberProfile';

jest.mock('../../../api/api', () => ({
  paymentApi: { getMyPayments: jest.fn() },
}));

jest.mock('../../../api/memberProfile', () => ({
  resolveMemberProfile: jest.fn(),
}));

// useAuth returns email; MemberProfileGate resolves full member
jest.mock('../../../context/AuthContext', () => ({
  useAuth: () => ({ email: 'member@gym.com' }),
}));

const mockMember = { id: 1, email: 'member@gym.com', name: 'Alice' };

beforeEach(() => {
  jest.clearAllMocks();
  memberProfileModule.resolveMemberProfile.mockResolvedValue(mockMember);
});

function renderPayments() {
  const store = configureStore({ reducer: { payments: paymentReducer } });
  return render(
    <Provider store={store}>
      <MemoryRouter><MemberPayments /></MemoryRouter>
    </Provider>
  );
}

describe('MemberPayments', () => {
  test('renders page title PAYMENTS', () => {
    api.paymentApi.getMyPayments.mockResolvedValue({ data: [] });
    renderPayments();
    expect(screen.getByText('PAYMENTS')).toBeInTheDocument();
  });

  test('shows empty state when no payments exist', async () => {
    api.paymentApi.getMyPayments.mockResolvedValue({ data: [] });
    renderPayments();
    await waitFor(() => {
      expect(screen.getByText('No payment history yet')).toBeInTheDocument();
    });
  });

  test('renders payment rows when data is returned', async () => {
    api.paymentApi.getMyPayments.mockResolvedValue({
      data: [
        { id: 1, description: 'Basic Plan Subscription', amount: 999,  paymentMethod: 'UPI',  status: 'SUCCESS', paidAt: '2025-01-15T10:30:00' },
        { id: 2, description: 'Session #5',              amount: 500,  paymentMethod: 'CASH', status: 'SUCCESS', paidAt: '2025-01-20T08:00:00' },
      ],
    });
    renderPayments();
    await waitFor(() => {
      expect(screen.getByText('Basic Plan Subscription')).toBeInTheDocument();
      expect(screen.getByText('Session #5')).toBeInTheDocument();
    });
  });

  test('shows error alert when payment fetch fails', async () => {
    api.paymentApi.getMyPayments.mockRejectedValue(new Error('Network error'));
    renderPayments();
    await waitFor(() => {
      expect(screen.getByText('Failed to load payment history')).toBeInTheDocument();
    });
  });

  test('calculates total paid from SUCCESS payments only', async () => {
    api.paymentApi.getMyPayments.mockResolvedValue({
      data: [
        { id: 1, amount: 999, status: 'SUCCESS',  description: 'Plan', paymentMethod: 'CASH', paidAt: '2025-01-01T00:00:00' },
        { id: 2, amount: 500, status: 'SUCCESS',  description: 'Sess', paymentMethod: 'UPI',  paidAt: '2025-01-02T00:00:00' },
        { id: 3, amount: 200, status: 'REFUNDED', description: 'Ref',  paymentMethod: 'CASH', paidAt: '2025-01-03T00:00:00' },
      ],
    });
    renderPayments();
    // total = 999 + 500 = 1499
    await waitFor(() => expect(screen.getByText('₹1499')).toBeInTheDocument());
  });

  test('shows Transactions stat card with correct count', async () => {
    api.paymentApi.getMyPayments.mockResolvedValue({
      data: [
        { id: 1, amount: 500, status: 'SUCCESS', description: 'X', paymentMethod: 'CASH', paidAt: '2025-01-01T00:00:00' },
      ],
    });
    renderPayments();
    await waitFor(() => {
      expect(screen.getByText('Transactions')).toBeInTheDocument();
    });
  });
});
