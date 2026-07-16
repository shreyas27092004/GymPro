// src/pages/member/MemberPayments.jsx
import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchMyPayments, clearPaymentError } from '../../store/slices/paymentSlice';
import { useAuth } from '../../context/AuthContext';
import MemberProfileGate from '../../components/MemberProfileGate';
import { LoadingCenter, Alert, StatusBadge, EmptyState } from '../../components/UI';
import Icon from '../../components/icons';

export default function MemberPayments() {
  const { email } = useAuth();
  return (
    <div className="fade-in">
      <div className="content-header">
        <div className="page-title">PAYMENTS</div>
        <div className="page-subtitle">Your billing history and transactions</div>
      </div>
      <MemberProfileGate email={email}>
        {(member) => <PaymentsContent member={member} />}
      </MemberProfileGate>
    </div>
  );
}

function PaymentsContent({ member }) {
  const dispatch = useDispatch();
  const { payments, loading, error } = useSelector((state) => state.payments);

  useEffect(() => {
    dispatch(fetchMyPayments(member.id));
    return () => { dispatch(clearPaymentError()); };
  }, [dispatch, member.id]);

  const total   = payments.filter(p => p.status === 'SUCCESS').reduce((s, p) => s + (p.amount || 0), 0);
  const pending = payments.filter(p => p.status === 'PENDING').length;

  const METHOD_ICONS = { UPI: 'smartphone', CASH: 'cash', CREDIT_CARD: 'card', DEBIT_CARD: 'card', QR_CODE: 'qrcode' };

  return (
    <div className="content-body">
      {error && (
        <Alert type="error" onClose={() => dispatch(clearPaymentError())}>
          {error}
        </Alert>
      )}

      <div className="stats-grid stats-grid-3" style={{ marginBottom: 24 }}>
        {[
          { label: 'Total Paid',   value: `₹${total.toFixed(0)}`, color: 'var(--green)',  icon: 'checkCircle' },
          { label: 'Transactions', value: payments.length,         color: 'var(--accent)', icon: 'clipboard' },
          { label: 'Pending',      value: pending,                 color: 'var(--amber)',  icon: 'clock' },
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
        <div className="section-header" style={{ marginBottom: 16 }}>
          <span className="section-title">Transaction History</span>
        </div>
        {loading ? <LoadingCenter /> : payments.length === 0 ? (
          <EmptyState icon={<Icon name="card" size={20} style={{ color: 'var(--text3)' }} />} text="No payment history yet" />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>Date</th><th>Description</th><th>Method</th><th>Amount</th><th>Status</th></tr>
              </thead>
              <tbody>
                {payments.map(p => (
                  <tr key={p.id}>
                    <td style={{ fontSize: 12, color: 'var(--text3)', whiteSpace: 'nowrap' }}>
                      {p.paidAt?.slice(0, 16).replace('T', ' ')}
                    </td>
                    <td style={{ fontSize: 13 }}>{p.description}</td>
                    <td>
                      <span style={{ fontSize: 13, display: 'inline-flex', alignItems: 'center', gap: 6 }}>
                        <Icon name={METHOD_ICONS[p.paymentMethod] || 'card'} size={14} style={{ color: 'var(--text3)' }} />
                        {p.paymentMethod}
                      </span>
                    </td>
                    <td style={{ fontWeight: 700, color: p.status === 'REFUNDED' ? 'var(--red)' : 'var(--green)' }}>
                      {p.status === 'REFUNDED' ? '-' : ''}₹{p.amount}
                    </td>
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