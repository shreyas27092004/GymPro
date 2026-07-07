// src/components/Toast.jsx
// Toast notification system: ToastProvider, useToast hook, and rendered toasts.
// Usage:
//   1. Wrap your app: <ToastProvider><App /></ToastProvider>
//   2. Inside any component: const { showToast } = useToast();
//      showToast('Booking confirmed!', 'success');

import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';

/* ─── Types ──────────────────────────────────────────────────────────── */
// type ToastType = 'success' | 'error' | 'warning' | 'info'

const COLORS = {
  success: { bg: '#0d2b1f', border: '#00E5A0', icon: '✓', color: '#00E5A0' },
  error:   { bg: '#2b0d13', border: '#FF5B6E', icon: '✕', color: '#FF5B6E' },
  warning: { bg: '#2b1f0d', border: '#FFB347', icon: '⚠', color: '#FFB347' },
  info:    { bg: '#0d1e2b', border: '#00D4FF', icon: 'ℹ', color: '#00D4FF' },
};

const AUTO_DISMISS_MS = 3000;

/* ─── Context ────────────────────────────────────────────────────────── */
const ToastContext = createContext(null);

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const idRef = useRef(0);

  const dismiss = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  // Listen for toasts emitted by axiosInstance (avoids circular import)
  useEffect(() => {
    const handler = (e) => showToast(e.detail.message, e.detail.type);
    window.addEventListener('gympro:toast', handler);
    return () => window.removeEventListener('gympro:toast', handler);
  // showToast is defined below — safe because useCallback memo is stable
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const showToast = useCallback(
    (message, type = 'info') => {
      const id = ++idRef.current;
      setToasts((prev) => [...prev, { id, message, type, leaving: false }]);
      setTimeout(() => {
        // Trigger fade-out animation
        setToasts((prev) =>
          prev.map((t) => (t.id === id ? { ...t, leaving: true } : t))
        );
        // Remove from DOM after animation
        setTimeout(() => dismiss(id), 350);
      }, AUTO_DISMISS_MS);
    },
    [dismiss]
  );

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <ToastContainer toasts={toasts} onDismiss={dismiss} />
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used inside <ToastProvider>');
  return ctx;
}

/* ─── Components ─────────────────────────────────────────────────────── */
function ToastContainer({ toasts, onDismiss }) {
  if (toasts.length === 0) return null;
  return (
    <div style={styles.container} role="region" aria-live="polite">
      {toasts.map((t) => (
        <ToastItem key={t.id} toast={t} onDismiss={onDismiss} />
      ))}
    </div>
  );
}

function ToastItem({ toast, onDismiss }) {
  const { id, message, type, leaving } = toast;
  const palette = COLORS[type] || COLORS.info;

  return (
    <div
      style={{
        ...styles.toast,
        background: palette.bg,
        borderLeft: `3px solid ${palette.border}`,
        animation: leaving
          ? 'toastSlideOut 0.35s ease forwards'
          : 'toastSlideIn 0.35s ease forwards',
      }}
      role="alert"
    >
      <span style={{ ...styles.icon, color: palette.color }}>{palette.icon}</span>
      <span style={styles.message}>{message}</span>
      <button
        onClick={() => onDismiss(id)}
        style={styles.close}
        aria-label="Dismiss notification"
      >
        ×
      </button>

      <style>{`
        @keyframes toastSlideIn {
          from { opacity: 0; transform: translateX(120%); }
          to   { opacity: 1; transform: translateX(0); }
        }
        @keyframes toastSlideOut {
          from { opacity: 1; transform: translateX(0); }
          to   { opacity: 0; transform: translateX(120%); }
        }
      `}</style>
    </div>
  );
}

/* ─── Styles ─────────────────────────────────────────────────────────── */
const styles = {
  container: {
    position: 'fixed',
    top: '80px',        // below Navbar
    right: '20px',
    zIndex: 10000,
    display: 'flex',
    flexDirection: 'column',
    gap: '10px',
    pointerEvents: 'none',
    maxWidth: '360px',
    width: 'calc(100vw - 40px)',
  },
  toast: {
    pointerEvents: 'all',
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    padding: '14px 16px',
    borderRadius: '10px',
    boxShadow: '0 8px 32px rgba(0,0,0,0.5)',
    fontFamily: 'var(--font-body, DM Sans, sans-serif)',
    fontSize: '14px',
    color: '#E8EDF5',
    backdropFilter: 'blur(8px)',
  },
  icon: {
    fontSize: '16px',
    fontWeight: '700',
    flexShrink: 0,
    width: '20px',
    textAlign: 'center',
  },
  message: {
    flex: 1,
    lineHeight: '1.4',
    wordBreak: 'break-word',
  },
  close: {
    background: 'none',
    border: 'none',
    color: '#8B9BB4',
    fontSize: '20px',
    cursor: 'pointer',
    padding: '0 2px',
    lineHeight: 1,
    flexShrink: 0,
    transition: 'color 0.2s',
    width: 'auto',
  },
};