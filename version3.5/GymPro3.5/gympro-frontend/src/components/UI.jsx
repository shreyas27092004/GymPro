// src/components/UI.jsx — shared reusable components

import { useState } from 'react';
import Icon from './icons';

// ── Spinner ───────────────────────────────────────────────────────────────
export function Spinner({ size = 20 }) {
  return (
    <div
      style={{
        width: size, height: size,
        border: `2px solid var(--border2)`,
        borderTopColor: 'var(--accent)',
        borderRadius: '50%',
        animation: 'spin 0.7s linear infinite',
        display: 'inline-block',
        flexShrink: 0,
      }}
    />
  );
}

// ── LoadingCenter ─────────────────────────────────────────────────────────
export function LoadingCenter({ text = 'Loading...' }) {
  return (
    <div className="loading-center">
      <Spinner /> <span>{text}</span>
    </div>
  );
}

// ── Alert ─────────────────────────────────────────────────────────────────
export function Alert({ type = 'error', children }) {
  return <div className={`alert alert-${type}`}>{children}</div>;
}

// ── Modal ─────────────────────────────────────────────────────────────────
export function Modal({ title, onClose, children, size = '' }) {
  return (
    <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className={`modal ${size}`}>
        <div className="modal-header">
          <span className="modal-title">{title}</span>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>
        {children}
      </div>
    </div>
  );
}

// ── ConfirmModal ──────────────────────────────────────────────────────────
export function ConfirmModal({ title, message, onConfirm, onClose, danger = true }) {
  const [loading, setLoading] = useState(false);
  return (
    <Modal title={title} onClose={onClose}>
      <p style={{ color: 'var(--text2)', fontSize: 14, marginBottom: 24 }}>{message}</p>
      <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
        <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
        <button
          className={`btn ${danger ? 'btn-danger' : 'btn-primary'}`}
          disabled={loading}
          onClick={async () => { setLoading(true); await onConfirm(); setLoading(false); }}
        >
          {loading ? <Spinner size={14} /> : 'Confirm'}
        </button>
      </div>
    </Modal>
  );
}

// ── Badge ─────────────────────────────────────────────────────────────────
export function StatusBadge({ status }) {
  const map = {
    ACTIVE:     'green', CONFIRMED: 'green', SUCCESS: 'green', COMPLETED: 'green',
    INACTIVE:   'red',   CANCELLED: 'red',   FAILED:   'red',  REFUNDED:  'amber',
    PENDING:    'amber',
  };
  return <span className={`badge badge-${map[status] || 'blue'}`}>{status}</span>;
}

// ── EmptyState ────────────────────────────────────────────────────────────
export function EmptyState({ icon = <Icon name="inbox" size={20} style={{ color: 'var(--text3)' }} />, text = 'No data found' }) {
  return (
    <div className="empty-state">
      <div className="empty-state-icon">{icon}</div>
      <div className="empty-state-text">{text}</div>
    </div>
  );
}

// ── FormGroup ─────────────────────────────────────────────────────────────
export function FormGroup({ label, children }) {
  return (
    <div className="form-group">
      <label className="form-label">{label}</label>
      {children}
    </div>
  );
}

// ── SectionHeader ──────────────────────────────────────────────────────────
export function SectionHeader({ title, children }) {
  return (
    <div className="section-header">
      <span className="section-title">{title}</span>
      <div style={{ display: 'flex', gap: 8 }}>{children}</div>
    </div>
  );
}
