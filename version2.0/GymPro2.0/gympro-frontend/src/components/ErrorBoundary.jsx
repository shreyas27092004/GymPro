// src/components/ErrorBoundary.jsx
// Class-based React error boundary.
// Wraps any subtree and catches render/lifecycle errors.
// Shows a styled GymPro-themed error card with a Retry button.

import { Component } from 'react';

export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, info) {
    // Log to console — swap in your analytics/Sentry here
    console.error('[ErrorBoundary] Caught error:', error, info.componentStack);
  }

  handleRetry = () => {
    this.setState({ hasError: false, error: null });
  };

  render() {
    if (!this.state.hasError) return this.props.children;

    const { title = 'Something went wrong', error } = this.props;

    return (
      <div style={styles.overlay}>
        <div style={styles.card}>
          <div style={styles.iconWrap}>
            <span style={styles.icon}>⚡</span>
          </div>

          <h2 style={styles.heading}>{title}</h2>
          <p style={styles.sub}>
            An unexpected error occurred in this section. Your other pages are
            unaffected.
          </p>

          {error?.message && (
            <pre style={styles.errorMsg}>{error.message}</pre>
          )}

          <button style={styles.btn} onClick={this.handleRetry}>
            <span>↺</span> Retry
          </button>
        </div>
      </div>
    );
  }
}

const styles = {
  overlay: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '60vh',
    padding: '32px 16px',
    fontFamily: 'var(--font-body, DM Sans, sans-serif)',
  },
  card: {
    background: 'var(--bg2, #0F1318)',
    border: '1px solid var(--border, #1E2A3A)',
    borderTop: '3px solid var(--red, #FF5B6E)',
    borderRadius: '16px',
    padding: '40px 36px',
    maxWidth: '440px',
    width: '100%',
    textAlign: 'center',
    boxShadow: '0 8px 48px rgba(0,0,0,0.6)',
  },
  iconWrap: {
    width: '56px',
    height: '56px',
    background: 'rgba(255, 91, 110, 0.12)',
    borderRadius: '50%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    margin: '0 auto 20px',
  },
  icon: {
    fontSize: '24px',
    lineHeight: 1,
  },
  heading: {
    color: 'var(--text, #E8EDF5)',
    fontSize: '20px',
    fontWeight: '700',
    marginBottom: '10px',
  },
  sub: {
    color: 'var(--text2, #8B9BB4)',
    fontSize: '14px',
    lineHeight: '1.6',
    marginBottom: '20px',
  },
  errorMsg: {
    background: 'var(--bg3, #161B24)',
    border: '1px solid var(--border, #1E2A3A)',
    borderRadius: '8px',
    padding: '12px',
    fontSize: '12px',
    color: 'var(--red, #FF5B6E)',
    textAlign: 'left',
    overflowX: 'auto',
    marginBottom: '24px',
    fontFamily: 'monospace',
    whiteSpace: 'pre-wrap',
    wordBreak: 'break-all',
  },
  btn: {
    background: 'var(--accent, #00D4FF)',
    color: '#000',
    border: 'none',
    borderRadius: '10px',
    padding: '11px 28px',
    fontSize: '14px',
    fontWeight: '700',
    cursor: 'pointer',
    display: 'inline-flex',
    alignItems: 'center',
    gap: '8px',
    fontFamily: 'inherit',
    transition: 'background 0.2s, transform 0.15s',
  },
};