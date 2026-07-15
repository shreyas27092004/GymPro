import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import MemberTrainers from '../../../pages/member/MemberTrainers';
import * as api from '../../../api/api';

jest.mock('../../../api/api', () => ({
  trainerApi: {
    getAll:            jest.fn(),
    getAvailableSlots: jest.fn(),
  },
}));

const mockTrainers = [
  {
    id: 1, name: 'Alice Coach',  email: 'alice@gym.com',
    specialization: 'Yoga', experienceYears: 5, sessionFee: 800, status: 'ACTIVE',
  },
  {
    id: 2, name: 'Bob Trainer',  email: 'bob@gym.com',
    specialization: 'Cardio', experienceYears: 3, sessionFee: 600, status: 'ACTIVE',
  },
  {
    id: 3, name: 'Carol Fit',    email: 'carol@gym.com',
    specialization: 'CrossFit', experienceYears: 7, sessionFee: 1000, status: 'INACTIVE',
  },
];

beforeEach(() => {
  jest.clearAllMocks();
  api.trainerApi.getAll.mockResolvedValue({ data: mockTrainers });
  api.trainerApi.getAvailableSlots.mockResolvedValue({ data: [] });
});

function renderTrainers() {
  return render(<MemoryRouter><MemberTrainers /></MemoryRouter>);
}

// ── Page listing ───────────────────────────────────────────────────────────────
describe('MemberTrainers — listing', () => {
  test('renders page title TRAINERS', () => {
    renderTrainers();
    expect(screen.getByText('TRAINERS')).toBeInTheDocument();
  });

  test('renders subtitle text', () => {
    renderTrainers();
    expect(screen.getByText('Browse available trainers and book a session')).toBeInTheDocument();
  });

  test('renders search input', () => {
    renderTrainers();
    expect(screen.getByPlaceholderText('Search trainers…')).toBeInTheDocument();
  });

  test('displays all trainer names after load', async () => {
    renderTrainers();
    await waitFor(() => {
      expect(screen.getByText('Alice Coach')).toBeInTheDocument();
      expect(screen.getByText('Bob Trainer')).toBeInTheDocument();
      expect(screen.getByText('Carol Fit')).toBeInTheDocument();
    });
  });

  test('displays trainer email addresses', async () => {
    renderTrainers();
    await waitFor(() => {
      expect(screen.getByText('alice@gym.com')).toBeInTheDocument();
      expect(screen.getByText('bob@gym.com')).toBeInTheDocument();
    });
  });

  test('displays specialization badges', async () => {
    renderTrainers();
    await waitFor(() => {
      expect(screen.getByText('Yoga')).toBeInTheDocument();
      expect(screen.getByText('Cardio')).toBeInTheDocument();
      expect(screen.getByText('CrossFit')).toBeInTheDocument();
    });
  });

  test('displays session fees', async () => {
    renderTrainers();
    await waitFor(() => {
      expect(screen.getByText('₹800')).toBeInTheDocument();
      expect(screen.getByText('₹600')).toBeInTheDocument();
      expect(screen.getByText('₹1000')).toBeInTheDocument();
    });
  });

  test('shows ACTIVE status badges', async () => {
    renderTrainers();
    await waitFor(() => {
      const activeBadges = screen.getAllByText('ACTIVE');
      expect(activeBadges.length).toBeGreaterThanOrEqual(2);
    });
  });

  test('shows INACTIVE badge for inactive trainer', async () => {
    renderTrainers();
    await waitFor(() => {
      expect(screen.getByText('INACTIVE')).toBeInTheDocument();
    });
  });

  test('shows avatar initials for each trainer', async () => {
    renderTrainers();
    await waitFor(() => {
      expect(screen.getByText('A')).toBeInTheDocument(); // Alice
      expect(screen.getByText('B')).toBeInTheDocument(); // Bob
      expect(screen.getByText('C')).toBeInTheDocument(); // Carol
    });
  });

  test('shows empty state when no trainers returned', async () => {
    api.trainerApi.getAll.mockResolvedValueOnce({ data: [] });
    renderTrainers();
    await waitFor(() => {
      expect(screen.getByText('No trainers found')).toBeInTheDocument();
    });
  });

  test('shows error alert when fetch fails', async () => {
    api.trainerApi.getAll.mockRejectedValueOnce(new Error('Network error'));
    renderTrainers();
    await waitFor(() => {
      expect(screen.getByText('Failed to load trainers')).toBeInTheDocument();
    });
  });

  test('shows experience years for each trainer', async () => {
    renderTrainers();
    await waitFor(() => {
      expect(screen.getByText('5')).toBeInTheDocument();
      expect(screen.getByText('3')).toBeInTheDocument();
      expect(screen.getByText('7')).toBeInTheDocument();
    });
  });
});

// ── Search / filter ────────────────────────────────────────────────────────────
describe('MemberTrainers — search', () => {
  test('filters trainers by name (case-insensitive)', async () => {
    const user = userEvent.setup();
    renderTrainers();
    await waitFor(() => screen.getByText('Alice Coach'));
    await user.type(screen.getByPlaceholderText('Search trainers…'), 'alice');
    expect(screen.getByText('Alice Coach')).toBeInTheDocument();
    expect(screen.queryByText('Bob Trainer')).not.toBeInTheDocument();
    expect(screen.queryByText('Carol Fit')).not.toBeInTheDocument();
  });

  test('filters trainers by specialization', async () => {
    const user = userEvent.setup();
    renderTrainers();
    await waitFor(() => screen.getByText('Alice Coach'));
    await user.type(screen.getByPlaceholderText('Search trainers…'), 'cardio');
    expect(screen.getByText('Bob Trainer')).toBeInTheDocument();
    expect(screen.queryByText('Alice Coach')).not.toBeInTheDocument();
  });

  test('shows empty state when search matches nothing', async () => {
    const user = userEvent.setup();
    renderTrainers();
    await waitFor(() => screen.getByText('Alice Coach'));
    await user.type(screen.getByPlaceholderText('Search trainers…'), 'zzznomatch');
    expect(screen.getByText('No trainers found')).toBeInTheDocument();
  });

  test('shows all trainers again when search is cleared', async () => {
    const user = userEvent.setup();
    renderTrainers();
    await waitFor(() => screen.getByText('Alice Coach'));
    await user.type(screen.getByPlaceholderText('Search trainers…'), 'alice');
    await user.clear(screen.getByPlaceholderText('Search trainers…'));
    expect(screen.getByText('Alice Coach')).toBeInTheDocument();
    expect(screen.getByText('Bob Trainer')).toBeInTheDocument();
    expect(screen.getByText('Carol Fit')).toBeInTheDocument();
  });
});

// ── Trainer card expansion (available slots) ───────────────────────────────────
describe('MemberTrainers — slot expansion', () => {
  test('clicking a trainer card calls getAvailableSlots', async () => {
    api.trainerApi.getAvailableSlots.mockResolvedValueOnce({ data: [] });
    const user = userEvent.setup();
    renderTrainers();
    await waitFor(() => screen.getByText('Alice Coach'));
    await user.click(screen.getByText('Alice Coach'));
    await waitFor(() => {
      expect(api.trainerApi.getAvailableSlots).toHaveBeenCalledWith(1);
    });
  });

  test('shows "Available Slots" section after card click', async () => {
    api.trainerApi.getAvailableSlots.mockResolvedValueOnce({ data: [] });
    const user = userEvent.setup();
    renderTrainers();
    await waitFor(() => screen.getByText('Alice Coach'));
    await user.click(screen.getByText('Alice Coach'));
    await waitFor(() => {
      expect(screen.getByText('Available Slots')).toBeInTheDocument();
    });
  });

  test('shows slots when trainer has available slots', async () => {
    api.trainerApi.getAvailableSlots.mockResolvedValueOnce({
      data: [
        { id: 10, dayOfWeek: 'MONDAY',    startTime: '09:00', endTime: '10:00' },
        { id: 11, dayOfWeek: 'WEDNESDAY', startTime: '11:00', endTime: '12:00' },
      ],
    });
    const user = userEvent.setup();
    renderTrainers();
    await waitFor(() => screen.getByText('Alice Coach'));
    await user.click(screen.getByText('Alice Coach'));
    await waitFor(() => {
      expect(screen.getByText('MONDAY 09:00')).toBeInTheDocument();
      expect(screen.getByText('WEDNESDAY 11:00')).toBeInTheDocument();
    });
  });

  test('shows "No available slots" when trainer has none', async () => {
    api.trainerApi.getAvailableSlots.mockResolvedValueOnce({ data: [] });
    const user = userEvent.setup();
    renderTrainers();
    await waitFor(() => screen.getByText('Alice Coach'));
    await user.click(screen.getByText('Alice Coach'));
    await waitFor(() => {
      expect(screen.getByText('No available slots')).toBeInTheDocument();
    });
  });

  test('clicking same card again collapses the slot section', async () => {
    api.trainerApi.getAvailableSlots.mockResolvedValue({ data: [] });
    const user = userEvent.setup();
    renderTrainers();
    await waitFor(() => screen.getByText('Alice Coach'));
    await user.click(screen.getByText('Alice Coach'));
    await waitFor(() => screen.getByText('Available Slots'));
    await user.click(screen.getByText('Alice Coach'));
    await waitFor(() => {
      expect(screen.queryByText('Available Slots')).not.toBeInTheDocument();
    });
  });
});
