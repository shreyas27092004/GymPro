import { render, screen, fireEvent } from '@testing-library/react';
import { Alert, StatusBadge, EmptyState, Modal, FormGroup, SectionHeader, LoadingCenter } from '../../components/UI';

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
});

describe('StatusBadge', () => {
  const cases = [
    ['ACTIVE',    'badge-green'],
    ['CONFIRMED', 'badge-green'],
    ['SUCCESS',   'badge-green'],
    ['COMPLETED', 'badge-green'],
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
});

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
});

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
});

describe('FormGroup', () => {
  test('renders label and child input', () => {
    render(<FormGroup label="Email"><input placeholder="Enter email" /></FormGroup>);
    expect(screen.getByText('Email')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Enter email')).toBeInTheDocument();
  });
});

describe('SectionHeader', () => {
  test('renders title and child button', () => {
    render(<SectionHeader title="Members List"><button>Add</button></SectionHeader>);
    expect(screen.getByText('Members List')).toBeInTheDocument();
    expect(screen.getByText('Add')).toBeInTheDocument();
  });
});

describe('LoadingCenter', () => {
  test('renders default loading text', () => {
    render(<LoadingCenter />);
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  test('renders custom loading text', () => {
    render(<LoadingCenter text="Fetching…" />);
    expect(screen.getByText('Fetching…')).toBeInTheDocument();
  });
});
