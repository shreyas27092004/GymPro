// src/pages/member/MemberPayments.jsx
// Lets a logged-in member view their own payment history.

import { useState, useEffect } from 'react';
import { paymentApi } from '../../api/api';
import { useAuth } from '../../context/AuthContext';
import MemberProfileGate from '../../components/MemberProfileGate';
import { LoadingCenter, Alert, StatusBadge, EmptyState, SectionHeader } from '../../components/UI';
import Icon from '../../components/icons';

export default function MemberPayments() {
  const { email } = useAuth();

  return (
    <div className="fade-in">
      <div className="content-header">
        <div className="page-title">PAYMENTS</div>
        <div className="page-subtitle">Your payment and transaction history</div>
      </div>
      <MemberProfileGate email={email}>
        {(member) => <PaymentsContent member={member} />}
      </MemberProfileGate>
    </div>
  );
}

const METHOD_ICONS = { UPI: 'smartphone', CASH: 'cash', CREDIT_CARD: 'card', DEBIT_CARD: 'card', QR_CODE: 'qrcode', RAZORPAY: 'card' };

function PaymentsContent({ member }) {
  const [payments, setPayments] = useState([]);
  const [loading,  setLoading]  = useState(true);
  const [error,    setError]    = useState('');
  const [filter,   setFilter]   = useState('ALL');

  const load = async () => {
    try {
      setLoading(true);
      const res = await paymentApi.getMyPayments(member.id);
      setPayments((res.data || []).slice().sort((a, b) => b.id - a.id));
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to load payment history');
    } finally { setLoading(false); }
  };

  useEffect(() => { load(); }, [member.id]);

  const filtered = filter === 'ALL' ? payments : payments.filter(p => p.status === filter);
  const totalPaid = payments.filter(p => p.status === 'SUCCESS').reduce((s, p) => s + (p.amount || 0), 0);
  const totalRefunded = payments.filter(p => p.status === 'REFUNDED').reduce((s, p) => s + (p.amount || 0), 0);

  return (
    <div className="content-body">
      {error && <Alert type="error">{error}</Alert>}

        <div className="stats-grid stats-grid-3" style={{ marginBottom: 24 }}>
          {[
            { label: 'Total Paid', value: `₹${totalPaid.toFixed(0)}`, color: 'var(--green)', icon: 'wallet' },
            { label: 'Transactions', value: payments.length, color: 'var(--accent)', icon: 'card' },
            { label: 'Total Refunded', value: `₹${totalRefunded.toFixed(0)}`, color: 'var(--red)', icon: 'undo' },
          ].map(s => (
            <div key={s.label} className="stat-card">
              <div className="stat-accent-bar" style={{ background: s.color }} />
              <div className="stat-icon" style={{ color: s.color }}><Icon name={s.icon} size={16} /></div>
              <div className="stat-value" style={{ color: s.color }}>{s.value}</div>
              <div className="stat-label">{s.label}</div>
            </div>
          ))}
        </div>

        <div className="card">
          <SectionHeader title={`${filtered.length} Transactions`}>
            <div className="tabs" style={{ marginBottom: 0 }}>
              {['ALL', 'SUCCESS', 'PENDING', 'FAILED', 'REFUNDED'].map(s => (
                <button key={s} className={`tab ${filter === s ? 'active' : ''}`} onClick={() => setFilter(s)}>
                  {s === 'ALL' ? 'All' : s.charAt(0) + s.slice(1).toLowerCase()}
                </button>
              ))}
            </div>
          </SectionHeader>

          {loading ? <LoadingCenter /> : filtered.length === 0 ? (
            <EmptyState icon={<Icon name="card" size={20} style={{ color: 'var(--text3)' }} />} text="No payment history yet" />
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Txn ID</th><th>Amount</th><th>Method</th>
                    <th>Description</th><th>Date</th><th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map(p => (
                    <tr key={p.id}>
                      <td style={{ fontSize: 11, color: 'var(--text3)', fontFamily: 'monospace' }}>
                        {p.transactionId ? `${p.transactionId.slice(0, 16)}…` : `#${p.id}`}
                      </td>
                      <td style={{ color: 'var(--green)', fontWeight: 700 }}>₹{p.amount}</td>
                      <td>
                        <span style={{ fontSize: 13, display: 'inline-flex', alignItems: 'center', gap: 6 }}>
                          <Icon name={METHOD_ICONS[p.paymentMethod] || 'card'} size={14} style={{ color: 'var(--text3)' }} />
                          {p.paymentMethod}
                        </span>
                      </td>
                      <td style={{ maxWidth: 220, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', fontSize: 13 }}>
                        {p.description}
                      </td>
                      <td style={{ fontSize: 12 }}>{p.paidAt?.slice(0, 16).replace('T', ' ')}</td>
                      <td><StatusBadge status={p.status} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
    </div>
  );
}
