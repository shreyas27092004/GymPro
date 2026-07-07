import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  Alert, StatusBadge, EmptyState, Modal, ConfirmModal,
  FormGroup, SectionHeader, LoadingCenter, Spinner,
} from '../../components/UI';

// ── Alert ─────────────────────────────────────────────────────────────────────
describe('Alert', () => {
  test('renders error alert with correct class and text', () => {
    render(<Alert type="error">Something went wrong</Alert>);
    const el = screen.getByText('Something went wrong');
    expect(el).toBeInTheDocument();
    expect(el.className).toContain('alert-error');
  });

  test('renders success alert', () => {
    render(<Alert type="success">All good!</Alert>);
    expect(screen.getByText('All good!').className).toContain('alert-success');
  });

  test('defaults to error type', () => {
    render(<Alert>Default</Alert>);
    expect(screen.getByText('Default').className).toContain('alert-error');
  });

  test('renders warning alert type', () => {
    render(<Alert type="warning">Watch out!</Alert>);
    expect(screen.getByText('Watch out!').className).toContain('alert-warning');
  });

  test('renders children as rich content', () => {
    render(<Alert type="error"><strong>Bold error</strong></Alert>);
    expect(screen.getByText('Bold error')).toBeInTheDocument();
  });
});

// ── StatusBadge ───────────────────────────────────────────────────────────────
describe('StatusBadge', () => {
  const cases = [
    ['ACTIVE',    'badge-green'],
    ['CONFIRMED', 'badge-green'],
    ['SUCCESS',   'badge-green'],
    ['COMPLETED', 'badge-green'],
    ['INACTIVE',  'badge-red'],
    ['CANCELLED', 'badge-red'],
    ['FAILED',    'badge-red'],
    ['PENDING',   'badge-amber'],
    ['REFUNDED',  'badge-amber'],
  ];

  test.each(cases)('status %s maps to %s', (status, expectedClass) => {
    render(<StatusBadge status={status} />);
    expect(screen.getByText(status).className).toContain(expectedClass);
  });

  test('unknown status falls back to badge-blue', () => {
    render(<StatusBadge status="UNKNOWN" />);
    expect(screen.getByText('UNKNOWN').className).toContain('badge-blue');
  });

  test('renders the status text as label', () => {
    render(<StatusBadge status="ACTIVE" />);
    expect(screen.getByText('ACTIVE')).toBeInTheDocument();
  });

  test('renders custom unknown status text verbatim', () => {
    render(<StatusBadge status="PROCESSING" />);
    expect(screen.getByText('PROCESSING')).toBeInTheDocument();
  });
});

// ── EmptyState ────────────────────────────────────────────────────────────────
describe('EmptyState', () => {
  test('renders icon and text', () => {
    render(<EmptyState icon="📭" text="Nothing here" />);
    expect(screen.getByText('📭')).toBeInTheDocument();
    expect(screen.getByText('Nothing here')).toBeInTheDocument();
  });

  test('renders with default props', () => {
    render(<EmptyState />);
    expect(screen.getByText('📭')).toBeInTheDocument();
    expect(screen.getByText('No data found')).toBeInTheDocument();
  });

  test('renders custom emoji icon', () => {
    render(<EmptyState icon="💪" text="No trainers" />);
    expect(screen.getByText('💪')).toBeInTheDocument();
  });

  test('renders custom text correctly', () => {
    render(<EmptyState text="No bookings yet. Book your first session!" />);
    expect(screen.getByText('No bookings yet. Book your first session!')).toBeInTheDocument();
  });
});

// ── Modal ─────────────────────────────────────────────────────────────────────
describe('Modal', () => {
  test('renders title and children', () => {
    render(<Modal title="Test Modal" onClose={() => {}}><p>Modal body</p></Modal>);
    expect(screen.getByText('Test Modal')).toBeInTheDocument();
    expect(screen.getByText('Modal body')).toBeInTheDocument();
  });

  test('calls onClose when ✕ button is clicked', () => {
    const onClose = jest.fn();
    render(<Modal title="Close Me" onClose={onClose}><p>Content</p></Modal>);
    fireEvent.click(screen.getByText('✕'));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  test('calls onClose when clicking the overlay backdrop', () => {
    const onClose = jest.fn();
    const { container } = render(
      <Modal title="Backdrop" onClose={onClose}><p>Inside</p></Modal>
    );
    const overlay = container.querySelector('.modal-overlay');
    fireEvent.click(overlay);
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  test('does NOT call onClose when clicking inside the modal content', () => {
    const onClose = jest.fn();
    render(<Modal title="No close" onClose={onClose}><p>Click me</p></Modal>);
    fireEvent.click(screen.getByText('Click me'));
    expect(onClose).not.toHaveBeenCalled();
  });

  test('renders modal with a close button labeled ✕', () => {
    render(<Modal title="X Test" onClose={() => {}}><p>Body</p></Modal>);
    expect(screen.getByText('✕')).toBeInTheDocument();
  });
});

// ── ConfirmModal ──────────────────────────────────────────────────────────────
describe('ConfirmModal', () => {
  test('renders title and message', () => {
    render(
      <ConfirmModal
        title="Delete Member"
        message="Are you sure you want to delete this member?"
        onConfirm={jest.fn()}
        onClose={jest.fn()}
      />
    );
    expect(screen.getByText('Delete Member')).toBeInTheDocument();
    expect(screen.getByText('Are you sure you want to delete this member?')).toBeInTheDocument();
  });

  test('renders Confirm and Cancel buttons', () => {
    render(
      <ConfirmModal
        title="Test" message="Test msg"
        onConfirm={jest.fn()} onClose={jest.fn()}
      />
    );
    expect(screen.getByText('Confirm')).toBeInTheDocument();
    expect(screen.getByText('Cancel')).toBeInTheDocument();
  });

  test('calls onClose when Cancel is clicked', () => {
    const onClose = jest.fn();
    render(
      <ConfirmModal
        title="Test" message="Test msg"
        onConfirm={jest.fn()} onClose={onClose}
      />
    );
    fireEvent.click(screen.getByText('Cancel'));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  test('calls onConfirm when Confirm is clicked', async () => {
    const onConfirm = jest.fn().mockResolvedValueOnce(undefined);
    render(
      <ConfirmModal
        title="Test" message="Test msg"
        onConfirm={onConfirm} onClose={jest.fn()}
      />
    );
    fireEvent.click(screen.getByText('Confirm'));
    await waitFor(() => expect(onConfirm).toHaveBeenCalledTimes(1));
  });

  test('Confirm button has danger class by default', () => {
    const { container } = render(
      <ConfirmModal
        title="Test" message="msg"
        onConfirm={jest.fn()} onClose={jest.fn()}
      />
    );
    const confirmBtn = screen.getByText('Confirm');
    expect(confirmBtn.className).toContain('btn-danger');
  });

  test('Confirm button uses primary class when danger=false', () => {
    render(
      <ConfirmModal
        title="Test" message="msg" danger={false}
        onConfirm={jest.fn()} onClose={jest.fn()}
      />
    );
    expect(screen.getByText('Confirm').className).toContain('btn-primary');
  });
});

// ── FormGroup ─────────────────────────────────────────────────────────────────
describe('FormGroup', () => {
  test('renders label and child input', () => {
    render(<FormGroup label="Email"><input placeholder="Enter email" /></FormGroup>);
    expect(screen.getByText('Email')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Enter email')).toBeInTheDocument();
  });

  test('renders multiple children', () => {
    render(
      <FormGroup label="Options">
        <input placeholder="First" />
        <input placeholder="Second" />
      </FormGroup>
    );
    expect(screen.getByPlaceholderText('First')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Second')).toBeInTheDocument();
  });

  test('label has form-label class', () => {
    render(<FormGroup label="Phone"><input /></FormGroup>);
    expect(screen.getByText('Phone').className).toContain('form-label');
  });
});

// ── SectionHeader ─────────────────────────────────────────────────────────────
describe('SectionHeader', () => {
  test('renders title and child button', () => {
    render(<SectionHeader title="Members List"><button>Add</button></SectionHeader>);
    expect(screen.getByText('Members List')).toBeInTheDocument();
    expect(screen.getByText('Add')).toBeInTheDocument();
  });

  test('renders multiple children', () => {
    render(
      <SectionHeader title="Bookings">
        <button>Filter</button>
        <button>Export</button>
      </SectionHeader>
    );
    expect(screen.getByText('Filter')).toBeInTheDocument();
    expect(screen.getByText('Export')).toBeInTheDocument();
  });

  test('renders title with section-title class', () => {
    render(<SectionHeader title="Payments" />);
    expect(screen.getByText('Payments').className).toContain('section-title');
  });
});

// ── LoadingCenter ─────────────────────────────────────────────────────────────
describe('LoadingCenter', () => {
  test('renders default loading text', () => {
    render(<LoadingCenter />);
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  test('renders custom loading text', () => {
    render(<LoadingCenter text="Fetching…" />);
    expect(screen.getByText('Fetching…')).toBeInTheDocument();
  });

  test('includes a spinner element', () => {
    const { container } = render(<LoadingCenter />);
    // Spinner renders as a div with animation styling
    const spinner = container.querySelector('.loading-center');
    expect(spinner).not.toBeNull();
  });
});

// ── Spinner ───────────────────────────────────────────────────────────────────
describe('Spinner', () => {
  test('renders without crashing', () => {
    const { container } = render(<Spinner />);
    expect(container.firstChild).not.toBeNull();
  });

  test('applies custom size via inline style', () => {
    const { container } = render(<Spinner size={32} />);
    const el = container.firstChild;
    expect(el.style.width).toBe('32px');
    expect(el.style.height).toBe('32px');
  });

  test('defaults to size 20', () => {
    const { container } = render(<Spinner />);
    expect(container.firstChild.style.width).toBe('20px');
  });
});
