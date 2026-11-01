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
    getUpgradeQuote:    jest.fn(),
    upgrade:            jest.fn(),
    getPrivileges:      jest.fn(),
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

// Basic (priority 1) and Premium (priority 3) — Premium is a valid upgrade target.
const mockPlans = [
  { id: 1, planName: 'Basic',   description: 'Entry level', price: 1000, durationType: 'MONTHLY', durationDays: 30, active: true, priorityLevel: 1 },
  { id: 2, planName: 'Premium', description: 'All access',  price: 2000, durationType: 'MONTHLY', durationDays: 30, active: true, priorityLevel: 3 },
];

const mockActiveSub = {
  id: 10, planId: 1, planName: 'Basic',
  startDate: '2025-01-01', endDate: '2025-01-31', status: 'ACTIVE',
};

beforeEach(() => {
  jest.clearAllMocks();
  memberProfileModule.resolveMemberProfile.mockResolvedValue(mockMember);
  api.planApi.getAll.mockResolvedValue({ data: mockPlans });
  api.planApi.getMySubscriptions.mockResolvedValue({ data: [mockActiveSub] });
});

function renderPlans() {
  const store = configureStore({ reducer: { plans: planReducer } });
  return render(
    <Provider store={store}>
      <MemoryRouter><MemberPlans /></MemoryRouter>
    </Provider>
  );
}

async function goToMySubscriptions(user) {
  renderPlans();
  await waitFor(() => screen.getByText(/my subscriptions \(1\)/i));
  await user.click(screen.getByText(/my subscriptions \(1\)/i));
  await waitFor(() => screen.getByText('Upgrade'));
}

describe('MemberPlans — Upgrade flow', () => {
  test('shows Upgrade and Cancel buttons for an ACTIVE subscription', async () => {
    const user = userEvent.setup();
    await goToMySubscriptions(user);
    expect(screen.getByText('Upgrade')).toBeInTheDocument();
    expect(screen.getByText('Cancel')).toBeInTheDocument();
  });

  test('clicking Upgrade opens the modal and fetches a quote for the eligible plan', async () => {
    api.planApi.getUpgradeQuote.mockResolvedValueOnce({
      data: { amountToPay: 1334, remainingValue: 666, newPlanName: 'Premium' },
    });
    const user = userEvent.setup();
    await goToMySubscriptions(user);

    await user.click(screen.getByText('Upgrade'));
    await waitFor(() => screen.getByText('Upgrade Plan'));

    await waitFor(() => {
      expect(api.planApi.getUpgradeQuote).toHaveBeenCalledWith(mockMember.id, 2);
      expect(screen.getByText('₹1334')).toBeInTheDocument();
    });
  });

  test('confirming upgrade calls upgrade() then pay() with the prorated amount', async () => {
    api.planApi.getUpgradeQuote.mockResolvedValueOnce({
      data: { amountToPay: 1334, remainingValue: 666, newPlanName: 'Premium' },
    });
    api.planApi.upgrade.mockResolvedValueOnce({ data: { id: 20 } });
    api.paymentApi.pay.mockResolvedValueOnce({ data: {} });

    const user = userEvent.setup();
    await goToMySubscriptions(user);

    await user.click(screen.getByText('Upgrade'));
    await waitFor(() => screen.getByText('₹1334'));

    await user.click(screen.getByText(/Pay ₹1334 & Upgrade/));

    await waitFor(() => {
      expect(api.planApi.upgrade).toHaveBeenCalledWith(mockMember.id, mockMember.email, 2);
      expect(api.paymentApi.pay).toHaveBeenCalledWith(
        expect.objectContaining({
          memberId:       mockMember.id,
          subscriptionId: 20,
          amount:         1334,
          paymentMethod:  'CASH',
        })
      );
    });
  });

  test('shows "nothing to upgrade to" message when no higher-tier plan exists', async () => {
    api.planApi.getAll.mockResolvedValue({
      data: [{ id: 1, planName: 'Elite', price: 3000, durationType: 'MONTHLY', durationDays: 30, active: true, priorityLevel: 4 }],
    });
    api.planApi.getMySubscriptions.mockResolvedValue({
      data: [{ id: 10, planId: 1, planName: 'Elite', startDate: '2025-01-01', endDate: '2025-01-31', status: 'ACTIVE' }],
    });

    const user = userEvent.setup();
    await goToMySubscriptions(user);

    await user.click(screen.getByText('Upgrade'));
    await waitFor(() => screen.getByText(/already on the highest available tier/i));
  });
});

describe('MemberPlans — Cancellation refund message', () => {
  test('shows the refund message returned by the cancel endpoint', async () => {
    api.planApi.cancelSubscription.mockResolvedValueOnce({
      data: 'Subscription cancelled ✅ — refund: ₹733.33',
    });

    const user = userEvent.setup();
    await goToMySubscriptions(user);

    await user.click(screen.getByText('Cancel'));
    await waitFor(() => screen.getByText('Cancel Subscription'));
    await user.click(screen.getByText('Confirm'));

    await waitFor(() => {
      expect(screen.getByText(/refund: ₹733\.33/)).toBeInTheDocument();
    });
  });
});
