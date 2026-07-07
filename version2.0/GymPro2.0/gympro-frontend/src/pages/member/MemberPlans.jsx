// src/pages/member/MemberPlans.jsx
// Data layer: Redux planSlice (fetchPlans, fetchMySubscriptions,
//             cancelSubscription, subscribeToPlan via thunk inside RazorpayModal)
// UI: identical to original — zero visual changes.

import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  fetchPlans,
  fetchMySubscriptions,
  cancelSubscription,
  clearPlanError,
} from '../../store/slices/planSlice';
import { planApi, paymentApi } from '../../api/api';
import { useAuth } from '../../context/AuthContext';
import MemberProfileGate from '../../components/MemberProfileGate';
import { Alert, EmptyState, Modal, StatusBadge } from '../../components/UI';
import { SkeletonMemberDashboard } from '../../components/Skeleton';

export default function MemberPlans() {
  const { email } = useAuth();
  return (
    <div className="fade-in">
      <div className="content-header">
        <div className="page-title">MEMBERSHIP PLANS</div>
        <div className="page-subtitle">Browse and subscribe to gym membership plans</div>
      </div>
      <MemberProfileGate email={email}>
        {(member) => <PlansContent member={member} />}
      </MemberProfileGate>
    </div>
  );
}

// ── PlansContent ──────────────────────────────────────────────────────────────

function PlansContent({ member }) {
  const dispatch = useDispatch();
  const { plans, mySubscriptions: mySubs, loading, error } = useSelector(
    (state) => state.plans
  );

  const [tab,     setTab]     = useState('available');
  const [paying,  setPaying]  = useState(null); // { planId, planName, amount }
  const [localErr, setLocalErr] = useState('');

  // Load both plans and subscriptions on mount / member change
  useEffect(() => {
    dispatch(fetchPlans());
    dispatch(fetchMySubscriptions(member.id));
    return () => { dispatch(clearPlanError()); };
  }, [dispatch, member.id]);

  const reload = () => {
    dispatch(fetchPlans());
    dispatch(fetchMySubscriptions(member.id));
  };

  const handleCancelSub = async (subId) => {
    setLocalErr('');
    const result = await dispatch(cancelSubscription(subId));
    if (cancelSubscription.rejected.match(result)) {
      setLocalErr(result.payload || 'Failed to cancel subscription');
    }
  };

  const displayError = localErr || error;

  const DURATION_COLORS = { MONTHLY: 'blue', QUARTERLY: 'purple', YEARLY: 'green' };

  if (loading) return <SkeletonMemberDashboard />;

  return (
    <div className="content-body">
      {displayError && (
        <Alert type="error" onClose={() => { setLocalErr(''); dispatch(clearPlanError()); }}>
          {displayError}
        </Alert>
      )}

      <div className="tabs">
        <button
          className={`tab ${tab === 'available' ? 'active' : ''}`}
          onClick={() => setTab('available')}
        >
          Available Plans
        </button>
        <button
          className={`tab ${tab === 'mine' ? 'active' : ''}`}
          onClick={() => setTab('mine')}
        >
          My Subscriptions ({mySubs.length})
        </button>
      </div>

      {tab === 'available' ? (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 20 }}>
          {plans.filter(p => p.active).map(plan => {
            const subscribed = mySubs.some(s => s.planId === plan.id && s.status !== 'CANCELLED');
            return (
              <div key={plan.id} className="card fade-in" style={{ position: 'relative' }}>
                <div style={{
                  position: 'absolute', top: 0, left: 0, right: 0, height: 3,
                  background: 'var(--accent)', borderRadius: '16px 16px 0 0',
                }} />
                <div style={{ marginTop: 8, marginBottom: 12 }}>
                  <span className={`badge badge-${DURATION_COLORS[plan.durationType] || 'blue'}`}>
                    {plan.durationType}
                  </span>
                </div>
                <div style={{ fontSize: 20, fontWeight: 700, color: 'var(--text)', marginBottom: 6 }}>
                  {plan.planName}
                </div>
                <div style={{ fontSize: 13, color: 'var(--text3)', marginBottom: 20, minHeight: 40 }}>
                  {plan.description}
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
                  <div>
                    <div style={{ fontFamily: 'var(--font-display)', fontSize: 36, color: 'var(--accent)', lineHeight: 1 }}>
                      ₹{plan.price}
                    </div>
                    <div style={{ fontSize: 11, color: 'var(--text3)', marginTop: 2 }}>{plan.durationDays} days</div>
                  </div>
                  {subscribed ? (
                    <span className="badge badge-green" style={{ alignSelf: 'flex-end' }}>SUBSCRIBED</span>
                  ) : (
                    <button
                      className="btn btn-primary"
                      onClick={() => setPaying({ planId: plan.id, planName: plan.planName, amount: plan.price })}
                    >
                      Subscribe
                    </button>
                  )}
                </div>
              </div>
            );
          })}
          {plans.filter(p => p.active).length === 0 && (
            <EmptyState icon="📋" text="No plans available" />
          )}
        </div>
      ) : (
        <div className="card">
          {mySubs.length === 0 ? (
            <EmptyState icon="📋" text="No active subscriptions" />
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr><th>Plan</th><th>Start</th><th>End</th><th>Status</th><th></th></tr>
                </thead>
                <tbody>
                  {mySubs.map(s => (
                    <tr key={s.id}>
                      <td style={{ color: 'var(--text)', fontWeight: 600 }}>Plan #{s.planId}</td>
                      <td style={{ fontSize: 13 }}>{s.startDate}</td>
                      <td style={{ fontSize: 13 }}>{s.endDate}</td>
                      <td><StatusBadge status={s.status || 'ACTIVE'} /></td>
                      <td>
                        {s.status !== 'CANCELLED' && (
                          <button
                            className="btn btn-danger btn-sm"
                            onClick={() => handleCancelSub(s.id)}
                          >
                            Cancel
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {paying && (
        <RazorpayModal
          planId={paying.planId}
          planName={paying.planName}
          amount={paying.amount}
          member={member}
          onClose={() => setPaying(null)}
          onPaid={() => { setPaying(null); reload(); }}
        />
      )}
    </div>
  );
}

// ── RazorpayModal ─────────────────────────────────────────────────────────────
// Handles Cash + Razorpay payment for a plan subscription.
// Calls planApi / paymentApi directly because the multi-step payment
// orchestration is inherently ephemeral local state; the slice's
// subscribeToPlan thunk is also available but the full flow is kept here
// for clarity and to match the original component exactly.

function RazorpayModal({ planId, planName, amount, member, onClose, onPaid }) {
  const dispatch = useDispatch();

  const [method,  setMethod]  = useState('CASH');
  const [loading, setLoading] = useState(false);
  const [error,   setError]   = useState('');

  const handlePay = async () => {
    setError(''); setLoading(true);

    // ── CASH ──────────────────────────────────────────────────────────────────
    if (method === 'CASH') {
      try {
        const subRes = await planApi.subscribe(member.id, member.email, planId);
        await paymentApi.pay({
          memberId:       member.id,
          memberEmail:    member.email,
          subscriptionId: subRes.data.id,
          amount,
          paymentMethod:  'CASH',
          description:    `${planName} Subscription`,
        });
        onPaid();
      } catch (err) {
        setLoading(false);
        setError(err.response?.data?.message || 'Payment failed. Please try again.');
      }
      return;
    }

    // ── RAZORPAY ──────────────────────────────────────────────────────────────
    try {
      const orderRes = await paymentApi.createOrder(amount, `${planName} Subscription`);
      const { orderId, keyId } = orderRes.data;
      setLoading(false);

      const options = {
        key:         keyId,
        amount:      amount * 100,
        currency:    'INR',
        name:        'GymPro',
        description: `${planName} Subscription`,
        order_id:    orderId,
        prefill:     { email: member.email, name: member.name || member.email },
        theme:       { color: '#6C63FF' },
        handler: async (response) => {
          try {
            const subRes = await planApi.subscribe(member.id, member.email, planId);
            await paymentApi.pay({
              memberId:          member.id,
              memberEmail:       member.email,
              subscriptionId:    subRes.data.id,
              amount,
              paymentMethod:     'RAZORPAY',
              description:       `${planName} Subscription`,
              razorpayOrderId:   response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            });
            onPaid();
          } catch {
            setError('Payment received but subscription setup failed. Contact support.');
          }
        },
        modal: {
          ondismiss: () => {
            setError('Payment cancelled. You can try again.');
            setLoading(false);
          },
        },
      };
      new window.Razorpay(options).open();
    } catch (err) {
      setLoading(false);
      setError(err.response?.data?.message || 'Failed to open payment. Please try again.');
    }
  };

  return (
    <Modal title="Subscribe to Plan" onClose={onClose}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {error && <div className="alert alert-error">{error}</div>}

        <div style={{ background: 'var(--bg2)', borderRadius: 10, padding: 16, textAlign: 'center' }}>
          <div style={{ fontSize: 13, color: 'var(--text3)', marginBottom: 4 }}>Amount to Pay</div>
          <div style={{ fontFamily: 'var(--font-display)', fontSize: 48, color: 'var(--green)' }}>₹{amount}</div>
          <div style={{ fontSize: 13, color: 'var(--text3)', marginTop: 4 }}>{planName}</div>
        </div>

        <div style={{ background: 'var(--bg2)', borderRadius: 10, padding: 12 }}>
          <div style={{ fontSize: 11, color: 'var(--text3)', marginBottom: 2 }}>Billing to</div>
          <div style={{ fontSize: 13, color: 'var(--text)', fontWeight: 600 }}>{member.name || member.email}</div>
          <div style={{ fontSize: 12, color: 'var(--text3)' }}>{member.email}</div>
        </div>

        <div>
          <div style={{ fontSize: 12, fontWeight: 600, color: 'var(--text3)', marginBottom: 6 }}>Payment Method</div>
          <select value={method} onChange={e => setMethod(e.target.value)}>
            <option value="CASH">💵 Cash</option>
            <option value="UPI">📱 UPI (Razorpay)</option>
            <option value="CREDIT_CARD">💳 Credit Card (Razorpay)</option>
            <option value="DEBIT_CARD">🏧 Debit Card (Razorpay)</option>
            <option value="NET_BANKING">🏦 Net Banking (Razorpay)</option>
          </select>
        </div>

        {method !== 'CASH' && (
          <div style={{ background: 'var(--bg2)', borderRadius: 8, padding: '10px 14px', fontSize: 12, color: 'var(--text3)' }}>
            🔒 Secured by Razorpay · UPI, Cards, Net Banking &amp; Wallets
          </div>
        )}

        <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" onClick={handlePay} disabled={loading}>
            {loading ? 'Processing…' : method === 'CASH' ? `Pay ₹${amount} (Cash)` : `Pay ₹${amount} via Razorpay`}
          </button>
        </div>
      </div>
    </Modal>
  );
}