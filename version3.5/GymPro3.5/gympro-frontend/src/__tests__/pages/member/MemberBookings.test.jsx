import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import bookingReducer from '../../../store/slices/bookingSlice';
import MemberBookings from '../../../pages/member/MemberBookings';
import * as api from '../../../api/api';
import * as memberProfileModule from '../../../api/memberProfile';

jest.mock('../../../api/api', () => ({
  bookingApi: {
    getByMember: jest.fn(),
    create:      jest.fn(),
    cancel:      jest.fn(),
  },
  trainerApi: {
    getAll:            jest.fn(),
    getAvailableSlots: jest.fn(),
  },
  planApi:    { useSession: jest.fn(), checkFreeSession: jest.fn() },
  paymentApi: { createOrder:  jest.fn(), pay: jest.fn() },
}));

jest.mock('../../../api/memberProfile', () => ({
  resolveMemberProfile: jest.fn(),
}));

jest.mock('../../../context/AuthContext', () => ({
  useAuth: () => ({ email: 'member@gym.com' }),
}));

const mockMember = { id: 1, email: 'member@gym.com', name: 'Alice' };

const mockBookings = [
  { id: 1, trainerEmail: 'trainer@gym.com',  sessionDay: 'MONDAY',    sessionTime: '09:00 - 10:00', bookingDate: '2025-01-15', status: 'CONFIRMED'  },
  { id: 2, trainerEmail: 'coach@gym.com',    sessionDay: 'WEDNESDAY', sessionTime: '11:00 - 12:00', bookingDate: '2025-01-10', status: 'COMPLETED'  },
];

beforeEach(() => {
  jest.clearAllMocks();
  memberProfileModule.resolveMemberProfile.mockResolvedValue(mockMember);
  api.bookingApi.getByMember.mockResolvedValue({ data: [...mockBookings] });
  api.trainerApi.getAll.mockResolvedValue({ data: [] });
  api.planApi.checkFreeSession.mockResolvedValue({ data: { eligible: false } });
});

function renderBookings() {
  const store = configureStore({ reducer: { bookings: bookingReducer } });
  return render(
    <Provider store={store}>
      <MemoryRouter><MemberBookings /></MemoryRouter>
    </Provider>
  );
}

// ── List tests ────────────────────────────────────────────────────────────────
describe('MemberBookings — list', () => {
  test('renders page title MY BOOKINGS', () => {
    renderBookings();
    expect(screen.getByText('MY BOOKINGS')).toBeInTheDocument();
  });

  test('shows booking trainer emails', async () => {
    renderBookings();
    await waitFor(() => {
      expect(screen.getByText('trainer@gym.com')).toBeInTheDocument();
      expect(screen.getByText('coach@gym.com')).toBeInTheDocument();
    });
  });

  test('shows empty state when no bookings', async () => {
    api.bookingApi.getByMember.mockResolvedValueOnce({ data: [] });
    renderBookings();
    await waitFor(() => {
      expect(screen.getByText('No bookings yet. Book your first session!')).toBeInTheDocument();
    });
  });

  test('shows error alert when fetch fails', async () => {
    api.bookingApi.getByMember.mockRejectedValueOnce(new Error('fail'));
    renderBookings();
    await waitFor(() => {
      expect(screen.getByText('Failed to load bookings')).toBeInTheDocument();
    });
  });

  test('shows + Book Session button', async () => {
    renderBookings();
    await waitFor(() => {
      expect(screen.getByText('+ Book Session')).toBeInTheDocument();
    });
  });

  test('Cancel button only appears for CONFIRMED bookings', async () => {
    renderBookings();
    await waitFor(() => {
      expect(screen.getAllByText('Cancel')).toHaveLength(1);
    });
  });

  test('Completed tab filters to completed bookings only', async () => {
    const user = userEvent.setup();
    renderBookings();
    await waitFor(() => screen.getByText('trainer@gym.com'));
    await user.click(screen.getByRole('button', { name: 'COMPLETED' }));
    expect(screen.getByText('coach@gym.com')).toBeInTheDocument();
    expect(screen.queryByText('trainer@gym.com')).not.toBeInTheDocument();
  });
});

// ── Book session modal ────────────────────────────────────────────────────────
describe('MemberBookings — book session modal', () => {
  test('opens modal when + Book Session is clicked', async () => {
    const user = userEvent.setup();
    renderBookings();
    await waitFor(() => screen.getByText('+ Book Session'));
    await user.click(screen.getByText('+ Book Session'));
    expect(screen.getByText('Book Training Session')).toBeInTheDocument();
  });

  test('modal closes when Cancel button is clicked', async () => {
    const user = userEvent.setup();
    renderBookings();
    await waitFor(() => screen.getByText('+ Book Session'));
    await user.click(screen.getByText('+ Book Session'));
    // Click the Cancel button inside the form (not the table Cancel)
    const cancelBtns = screen.getAllByText('Cancel');
    await user.click(cancelBtns[cancelBtns.length - 1]);
    expect(screen.queryByText('Book Training Session')).not.toBeInTheDocument();
  });
});

// ── Payment dropdown (paid session) ──────────────────────────────────────────
describe('MemberBookings — payment method dropdown', () => {
  const mockTrainers = [
    { id: 10, name: 'John Trainer', email: 'john@gym.com', specialization: 'Yoga', status: 'ACTIVE' },
  ];
  const mockSlots = [
    { id: 101, dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '10:00' },
  ];
  const mockCreatedBooking = {
    success: true,
    paymentRequired: true,
    freeSessionUsed: false,
    remainingFreeSessions: 0,
    amount: 500,
    booking: { id: 99, sessionDay: 'MONDAY', sessionTime: '09:00 - 10:00' },
  };

  beforeEach(() => {
    api.trainerApi.getAll.mockResolvedValue({ data: mockTrainers });
    api.trainerApi.getAvailableSlots.mockResolvedValue({ data: mockSlots });
    api.bookingApi.create.mockResolvedValue({ data: mockCreatedBooking });
    // Return false → paid session
    api.planApi.useSession.mockResolvedValue({ data: false });
  });

  async function reachPaymentStep() {
    const user = userEvent.setup();
    renderBookings();

    await waitFor(() => screen.getByText('+ Book Session'));
    await user.click(screen.getByText('+ Book Session'));

    // Select trainer — the first combobox in the modal
    await waitFor(() => screen.getByText('Book Training Session'));
    await waitFor(() => screen.getByText('John Trainer', { exact: false }));
    const selects = screen.getAllByRole('combobox');
    await user.selectOptions(selects[0], '10');

    // Wait for slot dropdown to appear
    await waitFor(() => screen.getByText(/Available Slot/i));

    const selects2 = screen.getAllByRole('combobox');
    await user.selectOptions(selects2[selects2.length - 1], '101');

    // Submit
    await user.click(screen.getByText('Book Session'));

    // Wait for payment step
    await waitFor(() => screen.getByText('Payment Method'));

    return user;
  }

  test('shows Payment Method dropdown with 5 options', async () => {
    await reachPaymentStep();
    expect(screen.getByRole('option', { name: /cash/i })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: /upi/i })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: /credit card/i })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: /debit card/i })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: /net banking/i })).toBeInTheDocument();
  });

  test('defaults to CASH and shows correct button label', async () => {
    await reachPaymentStep();
    const select = screen.getByRole('combobox');
    expect(select.value).toBe('CASH');
    expect(screen.getByText('Pay ₹500 (Cash)')).toBeInTheDocument();
  });

  test('button label changes to Razorpay when UPI selected', async () => {
    const user = await reachPaymentStep();
    await user.selectOptions(screen.getByRole('combobox'), 'UPI');
    expect(screen.getByText('Pay ₹500 via Razorpay')).toBeInTheDocument();
  });

  test('Razorpay security notice shows for non-CASH method', async () => {
    const user = await reachPaymentStep();
    await user.selectOptions(screen.getByRole('combobox'), 'NET_BANKING');
    expect(screen.getByText(/secured by razorpay/i)).toBeInTheDocument();
  });

  test('Razorpay security notice hidden when CASH selected', async () => {
    await reachPaymentStep();
    expect(screen.queryByText(/secured by razorpay/i)).not.toBeInTheDocument();
  });

  test('shows ₹500 fee on payment step', async () => {
    await reachPaymentStep();
    expect(screen.getByText('₹500')).toBeInTheDocument();
  });

  test('CASH payment calls paymentApi.pay with CASH method', async () => {
    api.paymentApi.pay.mockResolvedValueOnce({ data: {} });
    const user = await reachPaymentStep();
    await user.click(screen.getByText('Pay ₹500 (Cash)'));
    await waitFor(() => {
      expect(api.paymentApi.pay).toHaveBeenCalledWith(
        expect.objectContaining({
          memberId:      mockMember.id,
          memberEmail:   mockMember.email,
          bookingId:     mockCreatedBooking.booking.id,
          amount:        500,
          paymentMethod: 'CASH',
        })
      );
    });
  });
});
