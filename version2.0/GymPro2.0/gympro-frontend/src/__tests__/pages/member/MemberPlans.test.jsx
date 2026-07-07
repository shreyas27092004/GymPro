import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import planReducer from '../../../store/slices/planSlice';
import MemberPlans from '../../../pages/member/MemberPlans';
import * as api from '../../../api/api';
import * as memberProfileModule from '../../../api/memberProfile';

jest.mock('../../../api/api', () => ({
  planApi: {
    getAll:             jest.fn(),
    getMySubscriptions: jest.fn(),
    subscribe:          jest.fn(),
    cancelSubscription: jest.fn(),
  },
  paymentApi: {
    createOrder: jest.fn(),
    pay:         jest.fn(),
  },
}));

jest.mock('../../../api/memberProfile', () => ({
  resolveMemberProfile: jest.fn(),
}));

jest.mock('../../../context/AuthContext', () => ({
  useAuth: () => ({ email: 'member@gym.com' }),
}));

const mockMember = { id: 1, email: 'member@gym.com', name: 'Alice' };

const mockPlans = [
  { id: 1, planName: 'Basic', description: 'Entry level', price: 999,  durationType: 'MONTHLY',   durationDays: 30, active: true },
  { id: 2, planName: 'Pro',   description: 'All access',  price: 2499, durationType: 'QUARTERLY', durationDays: 90, active: true },
];

beforeEach(() => {
  jest.clearAllMocks();
  memberProfileModule.resolveMemberProfile.mockResolvedValue(mockMember);
  api.planApi.getAll.mockResolvedValue({ data: mockPlans });
  api.planApi.getMySubscriptions.mockResolvedValue({ data: [] });
});

function renderPlans() {
  const store = configureStore({ reducer: { plans: planReducer } });
  return render(
    <Provider store={store}>
      <MemoryRouter><MemberPlans /></MemoryRouter>
    </Provider>
  );
}

describe('MemberPlans — listing', () => {
  test('renders page title MEMBERSHIP PLANS', () => {
    renderPlans();
    expect(screen.getByText('MEMBERSHIP PLANS')).toBeInTheDocument();
  });

  test('shows all active plans', async () => {
    renderPlans();
    await waitFor(() => {
      expect(screen.getByText('Basic')).toBeInTheDocument();
      expect(screen.getByText('Pro')).toBeInTheDocument();
    });
  });

  test('shows Subscribe button for non-subscribed plans', async () => {
    renderPlans();
    await waitFor(() => {
      expect(screen.getAllByText('Subscribe')).toHaveLength(2);
    });
  });

  test('shows SUBSCRIBED badge for subscribed plan', async () => {
    api.planApi.getMySubscriptions.mockResolvedValueOnce({
      data: [{ id: 10, planId: 1, status: 'ACTIVE' }],
    });
    renderPlans();
    await waitFor(() => {
      expect(screen.getByText('SUBSCRIBED')).toBeInTheDocument();
      expect(screen.getAllByText('Subscribe')).toHaveLength(1);
    });
  });
});

describe('MemberPlans — payment modal', () => {
  async function openPaymentModal() {
    const user = userEvent.setup();
    renderPlans();
    await waitFor(() => screen.getAllByText('Subscribe'));
    await user.click(screen.getAllByText('Subscribe')[0]);
    await waitFor(() => screen.getByText('Subscribe to Plan'));
    return user;
  }

  test('opens payment modal showing plan price', async () => {
    await openPaymentModal();
    expect(screen.getAllByText('₹999').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Basic').length).toBeGreaterThan(0);
  });

  test('shows member billing info in modal', async () => {
    await openPaymentModal();
    expect(screen.getByText('Alice')).toBeInTheDocument();
    expect(screen.getByText('member@gym.com')).toBeInTheDocument();
  });

  test('payment method dropdown defaults to CASH', async () => {
    await openPaymentModal();
    expect(screen.getByRole('combobox').value).toBe('CASH');
  });

  test('all 5 payment options present in dropdown', async () => {
    await openPaymentModal();
    expect(screen.getByRole('option', { name: /💵 cash/i })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: /upi/i })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: /credit card/i })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: /debit card/i })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: /net banking/i })).toBeInTheDocument();
  });

  test('button shows Pay (Cash) label for CASH', async () => {
    await openPaymentModal();
    expect(screen.getByText('Pay ₹999 (Cash)')).toBeInTheDocument();
  });

  test('button switches to Razorpay label when UPI selected', async () => {
    const user = await openPaymentModal();
    await user.selectOptions(screen.getByRole('combobox'), 'UPI');
    expect(screen.getByText('Pay ₹999 via Razorpay')).toBeInTheDocument();
  });

  test('Razorpay security notice visible for non-CASH', async () => {
    const user = await openPaymentModal();
    await user.selectOptions(screen.getByRole('combobox'), 'CREDIT_CARD');
    expect(screen.getByText(/secured by razorpay/i)).toBeInTheDocument();
  });

  test('Razorpay security notice hidden for CASH', async () => {
    await openPaymentModal();
    expect(screen.queryByText(/secured by razorpay/i)).not.toBeInTheDocument();
  });

  test('CASH payment calls subscribe then paymentApi.pay', async () => {
    api.planApi.subscribe.mockResolvedValueOnce({ data: { id: 55 } });
    api.paymentApi.pay.mockResolvedValueOnce({ data: {} });
    const user = await openPaymentModal();
    await user.click(screen.getByText('Pay ₹999 (Cash)'));
    await waitFor(() => {
      expect(api.planApi.subscribe).toHaveBeenCalledWith(mockMember.id, mockMember.email, 1);
      expect(api.paymentApi.pay).toHaveBeenCalledWith(
        expect.objectContaining({
          memberId:       mockMember.id,
          memberEmail:    mockMember.email,
          subscriptionId: 55,
          amount:         999,
          paymentMethod:  'CASH',
          description:    'Basic Subscription',
        })
      );
    });
  });

  test('modal closes when Cancel clicked', async () => {
    const user = await openPaymentModal();
    await user.click(screen.getByText('Cancel'));
    expect(screen.queryByText('Subscribe to Plan')).not.toBeInTheDocument();
  });
});

describe('MemberPlans — My Subscriptions tab', () => {
  test('switches to My Subscriptions tab and shows data', async () => {
    api.planApi.getMySubscriptions.mockResolvedValue({
      data: [{ id: 10, planId: 1, startDate: '2025-01-01', endDate: '2025-02-01', status: 'ACTIVE' }],
    });
    const user = userEvent.setup();
    renderPlans();
    await waitFor(() => screen.getByText(/my subscriptions \(1\)/i));
    await user.click(screen.getByText(/my subscriptions \(1\)/i));
    await waitFor(() => {
      expect(screen.getByText('2025-01-01')).toBeInTheDocument();
    });
  });
});
