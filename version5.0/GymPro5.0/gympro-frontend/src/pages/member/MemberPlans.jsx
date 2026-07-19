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
  clearCancelMessage,
} from '../../store/slices/planSlice';
import { planApi, paymentApi } from '../../api/api';
import { useAuth } from '../../context/AuthContext';
import MemberProfileGate from '../../components/MemberProfileGate';
import { Alert, EmptyState, Modal, StatusBadge, ConfirmModal } from '../../components/UI';
import { SkeletonMemberDashboard } from '../../components/Skeleton';
import Icon from '../../components/icons';

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
  const { plans, mySubscriptions: mySubs, loading, error, cancelMessage } = useSelector(
    (state) => state.plans
  );

  const [tab,       setTab]       = useState('available');
  const [paying,    setPaying]    = useState(null); // { planId, planName, amount }
  const [upgrading, setUpgrading] = useState(null); // { subscription, currentPlan, eligiblePlans }
  const [localErr,  setLocalErr]  = useState('');
  const [confirmCancelSub, setConfirmCancelSub] = useState(null); // subscription awaiting cancel confirm

  // Load both plans and subscriptions on mount / member change
  useEffect(() => {
    dispatch(fetchPlans());
    dispatch(fetchMySubscriptions(member.id));
    return () => { dispatch(clearPlanError()); dispatch(clearCancelMessage()); };
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

  const confirmCancelSubscription = async () => {
    const sub = confirmCancelSub;
    setConfirmCancelSub(null);
    if (sub) await handleCancelSub(sub.id);
  };

  const openUpgrade = (sub) => {
    const currentPlan = plans.find(p => p.id === sub.planId);
    if (!currentPlan) return;
    const eligiblePlans = plans.filter(p => p.active && p.priorityLevel > currentPlan.priorityLevel);
    setUpgrading({ subscription: sub, currentPlan, eligiblePlans });
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

      {cancelMessage && (
        <Alert type="success" onClose={() => dispatch(clearCancelMessage())}>
          {cancelMessage}
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
            <EmptyState icon={<Icon name="clipboard" size={20} style={{ color: 'var(--text3)' }} />} text="No plans available" />
          )}
        </div>
      ) : (
        <div className="card">
          {mySubs.length === 0 ? (
            <EmptyState icon={<Icon name="clipboard" size={20} style={{ color: 'var(--text3)' }} />} text="No active subscriptions" />
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr><th>Plan</th><th>Start</th><th>End</th><th>Status</th><th></th></tr>
                </thead>
                <tbody>
                  {mySubs.map(s => (
                    <tr key={s.id}>
                      <td style={{ color: 'var(--text)', fontWeight: 600 }}>{s.planName || `Plan #${s.planId}`}</td>
                      <td style={{ fontSize: 13 }}>{s.startDate}</td>
                      <td style={{ fontSize: 13 }}>{s.endDate}</td>
                      <td><StatusBadge status={s.status || 'ACTIVE'} /></td>
                      <td>
                        {s.status === 'ACTIVE' && (
                          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
                            <button
                              className="btn btn-ghost btn-sm"
                              onClick={() => openUpgrade(s)}
                              title="Move to a higher-tier plan"
                            >
                              <Icon name="trendingUp" size={13} /> Upgrade
                            </button>
                            <button
                              className="btn btn-danger btn-sm"
                              onClick={() => setConfirmCancelSub(s)}
                            >
                              Cancel
                            </button>
                          </div>
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

      {upgrading && (
        <UpgradeModal
          subscription={upgrading.subscription}
          currentPlan={upgrading.currentPlan}
          eligiblePlans={upgrading.eligiblePlans}
          member={member}
          onClose={() => setUpgrading(null)}
          onUpgraded={() => { setUpgrading(null); reload(); }}
        />
      )}

      {confirmCancelSub && (
        <ConfirmModal
          title="Cancel Subscription"
          message={`Are you sure you want to cancel your "${confirmCancelSub.planName || 'plan'}" subscription? This cannot be undone.`}
          onConfirm={confirmCancelSubscription}
          onClose={() => setConfirmCancelSub(null)}
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
            <option value="CASH">Cash</option>
            <option value="ONLINE">Online (UPI / Card / Net Banking)</option>
          </select>
        </div>

        {method !== 'CASH' && (
          <div style={{ background: 'var(--bg2)', borderRadius: 8, padding: '10px 14px', fontSize: 12, color: 'var(--text3)', display: 'flex', alignItems: 'center', gap: 6 }}>
            <Icon name="lock" size={13} /> Secured by Razorpay · UPI, Cards, Net Banking &amp; Wallets
          </div>
        )}

        <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" onClick={handlePay} disabled={loading}>
            {loading ? 'Processing…' : method === 'CASH' ? `Pay ₹${amount} (Cash)` : `Pay ₹${amount} Online`}
          </button>
        </div>
      </div>
    </Modal>
  );
}

// ── UpgradeModal ─────────────────────────────────────────────────────────────
// Two steps:
//   1. Pick a higher-tier plan → fetch the prorated quote (read-only preview).
//   2. Confirm → POST /plans/upgrade (closes old sub, opens new one), then
//      charge amountToPay via the existing /payments/pay flow (CASH or
//      Razorpay), mirroring RazorpayModal's payment logic.

function UpgradeModal({ subscription, currentPlan, eligiblePlans, member, onClose, onUpgraded }) {
  const [selectedPlanId, setSelectedPlanId] = useState(eligiblePlans[0]?.id ?? '');
  const [quote,          setQuote]          = useState(null);
  const [method,         setMethod]         = useState('CASH');
  const [loadingQuote,   setLoadingQuote]   = useState(false);
  const [loading,        setLoading]        = useState(false);
  const [error,          setError]          = useState('');

  useEffect(() => {
    if (!selectedPlanId) { setQuote(null); return; }
    let cancelled = false;
    setLoadingQuote(true);
    setError('');
    Promise.resolve(planApi.getUpgradeQuote(member.id, selectedPlanId))
      .then(res => { if (!cancelled && res) setQuote(res.data); })
      .catch(err => { if (!cancelled) setError(err.response?.data?.message || 'Could not calculate upgrade cost'); })
      .finally(() => { if (!cancelled) setLoadingQuote(false); });
    return () => { cancelled = true; };
  }, [selectedPlanId, member.id]);

  const selectedPlan = eligiblePlans.find(p => p.id === Number(selectedPlanId));

  const chargeUpgrade = async (paymentExtra) => {
    const upgradeRes = await planApi.upgrade(member.id, member.email, selectedPlanId);
    const newSub = upgradeRes.data;
    await paymentApi.pay({
      memberId:       member.id,
      memberEmail:    member.email,
      subscriptionId: newSub.id,
      amount:         quote.amountToPay,
      paymentMethod:  method === 'CASH' ? 'CASH' : 'RAZORPAY',
      description:    `Upgrade: ${currentPlan.planName} → ${selectedPlan?.planName}`,
      ...paymentExtra,
    });
  };

  const handleConfirm = async () => {
    if (!quote) return;
    setError(''); setLoading(true);

    // Nothing to charge — the credit from the old plan fully covers the new plan.
    if (quote.amountToPay <= 0) {
      try {
        await planApi.upgrade(member.id, member.email, selectedPlanId);
        onUpgraded();
      } catch (err) {
        setLoading(false);
        setError(err.response?.data?.message || 'Upgrade failed. Please try again.');
      }
      return;
    }

    // ── CASH ──────────────────────────────────────────────────────────────
    if (method === 'CASH') {
      try {
        await chargeUpgrade({});
        onUpgraded();
      } catch (err) {
        setLoading(false);
        setError(err.response?.data?.message || 'Upgrade payment failed. Please try again.');
      }
      return;
    }

    // ── RAZORPAY ──────────────────────────────────────────────────────────
    try {
      const orderRes = await paymentApi.createOrder(quote.amountToPay, `Upgrade to ${selectedPlan?.planName}`);
      const { orderId, keyId } = orderRes.data;
      setLoading(false);

      const options = {
        key:         keyId,
        amount:      quote.amountToPay * 100,
        currency:    'INR',
        name:        'GymPro',
        description: `Upgrade to ${selectedPlan?.planName}`,
        order_id:    orderId,
        prefill:     { email: member.email, name: member.name || member.email },
        theme:       { color: '#6C63FF' },
        handler: async (response) => {
          try {
            await chargeUpgrade({
              razorpayOrderId:   response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            });
            onUpgraded();
          } catch {
            setError('Payment received but upgrade setup failed. Contact support.');
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

  if (eligiblePlans.length === 0) {
    return (
      <Modal title="Upgrade Plan" onClose={onClose}>
        <Alert type="info">
          You're already on the highest available tier — there's nothing to upgrade to right now.
        </Alert>
        <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 16 }}>
          <button className="btn btn-ghost" onClick={onClose}>Close</button>
        </div>
      </Modal>
    );
  }

  return (
    <Modal title="Upgrade Plan" onClose={onClose}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {error && <div className="alert alert-error">{error}</div>}

        <div style={{ background: 'var(--bg2)', borderRadius: 10, padding: 12 }}>
          <div style={{ fontSize: 11, color: 'var(--text3)', marginBottom: 2 }}>Current plan</div>
          <div style={{ fontSize: 13, color: 'var(--text)', fontWeight: 600 }}>{currentPlan.planName} · ₹{currentPlan.price}</div>
        </div>

        <div>
          <div style={{ fontSize: 12, fontWeight: 600, color: 'var(--text3)', marginBottom: 6 }}>Upgrade to</div>
          <select value={selectedPlanId} onChange={e => setSelectedPlanId(Number(e.target.value))}>
            {eligiblePlans.map(p => (
              <option key={p.id} value={p.id}>{p.planName} · ₹{p.price}</option>
            ))}
          </select>
        </div>

        <div style={{ background: 'var(--bg2)', borderRadius: 10, padding: 16, textAlign: 'center' }}>
          {loadingQuote ? (
            <div style={{ fontSize: 13, color: 'var(--text3)' }}>Calculating prorated amount…</div>
          ) : quote ? (
            <>
              <div style={{ fontSize: 13, color: 'var(--text3)', marginBottom: 4 }}>Amount to Pay Now</div>
              <div style={{ fontFamily: 'var(--font-display)', fontSize: 48, color: 'var(--green)' }}>
                ₹{quote.amountToPay}
              </div>
              <div style={{ fontSize: 12, color: 'var(--text3)', marginTop: 4 }}>
                ₹{quote.remainingValue} credited from your current plan's unused days
              </div>
            </>
          ) : (
            <div style={{ fontSize: 13, color: 'var(--text3)' }}>Select a plan to see the cost</div>
          )}
        </div>

        {quote && quote.amountToPay > 0 && (
          <div>
            <div style={{ fontSize: 12, fontWeight: 600, color: 'var(--text3)', marginBottom: 6 }}>Payment Method</div>
            <select value={method} onChange={e => setMethod(e.target.value)}>
              <option value="CASH">Cash</option>
              <option value="ONLINE">Online (UPI / Card / Net Banking)</option>
            </select>
          </div>
        )}

        <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
          <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" onClick={handleConfirm} disabled={loading || loadingQuote || !quote}>
            {loading ? 'Processing…' : quote && quote.amountToPay <= 0 ? 'Confirm Upgrade (Fully Covered)' : `Pay ₹${quote?.amountToPay ?? ''} & Upgrade`}
          </button>
        </div>
      </div>
    </Modal>
  );
}