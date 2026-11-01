// src/pages/member/MemberBookings.jsx
import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  fetchMemberBookings,
  createBooking,
  cancelBooking,
  clearBookingError,
  clearLastBookingResult,
} from '../../store/slices/bookingSlice';
import { trainerApi, paymentApi, planApi } from '../../api/api';
import { useAuth } from '../../context/AuthContext';
import MemberProfileGate from '../../components/MemberProfileGate';
import { Alert, StatusBadge, EmptyState, SectionHeader, Modal, FormGroup, LoadingCenter, ConfirmModal } from '../../components/UI';
import Icon from '../../components/icons';

// Maps the 3-letter day codes used by TrainerSchedule to JS Date.getDay() indices.
const DAY_CODE_TO_INDEX = { SUN: 0, MON: 1, TUE: 2, WED: 3, THU: 4, FRI: 5, SAT: 6 };

/**
 * Returns the next `count` upcoming calendar dates (as 'YYYY-MM-DD' strings)
 * that fall on the given weekday code (e.g. "MON"). Today is included if it
 * matches, but nothing before today is ever returned — this is what keeps
 * members from picking a session date in the past.
 */
function getUpcomingDatesForDay(dayCode, count = 6) {
  const targetIdx = DAY_CODE_TO_INDEX[dayCode];
  if (targetIdx === undefined) return [];

  const dates = [];
  const cursor = new Date();
  cursor.setHours(0, 0, 0, 0);

  for (let i = 0; dates.length < count && i < 60; i++) {
    if (cursor.getDay() === targetIdx) {
      const y = cursor.getFullYear();
      const m = String(cursor.getMonth() + 1).padStart(2, '0');
      const d = String(cursor.getDate()).padStart(2, '0');
      dates.push(`${y}-${m}-${d}`);
    }
    cursor.setDate(cursor.getDate() + 1);
  }
  return dates;
}

function formatDateLabel(iso) {
  const d = new Date(iso + 'T00:00:00');
  return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short' });
}

export default function MemberBookings() {
  const { email } = useAuth();
  return (
    <div className="fade-in">
      <div className="content-header">
        <div className="page-title">MY BOOKINGS</div>
        <div className="page-subtitle">Your upcoming and past training sessions</div>
      </div>
      <MemberProfileGate email={email}>
        {(member) => <BookingsContent member={member} />}
      </MemberProfileGate>
    </div>
  );
}

function BookingsContent({ member }) {
  const dispatch = useDispatch();
  const { bookings, loading, error } = useSelector((state) => state.bookings);

  const [filter,         setFilter]         = useState('ALL');
  const [showBook,       setShowBook]       = useState(false);
  const [localErr,       setLocalErr]       = useState('');
  const [freeSessionInfo, setFreeSessionInfo] = useState(null);
  const [freeInfoLoading, setFreeInfoLoading] = useState(true);
  const [confirmCancelId, setConfirmCancelId] = useState(null);

  useEffect(() => {
    dispatch(fetchMemberBookings(member.id));
    refreshFreeSessionInfo();
    return () => { dispatch(clearBookingError()); };
  }, [dispatch, member.id]);

  const refreshFreeSessionInfo = () => {
    setFreeInfoLoading(true);
    planApi.checkFreeSession(member.id)
      .then(r => setFreeSessionInfo(r.data))
      .catch(() => setFreeSessionInfo(null))
      .finally(() => setFreeInfoLoading(false));
  };

  const reload = () => {
    dispatch(fetchMemberBookings(member.id));
    refreshFreeSessionInfo();
  };

  const filtered = filter === 'ALL' ? bookings : bookings.filter(b => b.status === filter);

  const handleCancel = async (id) => {
    const result = await dispatch(cancelBooking(id));
    if (cancelBooking.rejected.match(result)) {
      setLocalErr(result.payload || 'Cancel failed');
    } else {
      // Refresh free session info since cancelling a FREE booking restores a session
      refreshFreeSessionInfo();
    }
  };

  const confirmCancel = async () => {
    const id = confirmCancelId;
    setConfirmCancelId(null);
    await handleCancel(id);
  };

  const displayError = localErr || error;

  return (
    <div className="content-body">
      {displayError && (
        <Alert type="error" onClose={() => { setLocalErr(''); dispatch(clearBookingError()); }}>
          {displayError}
        </Alert>
      )}

      {/* Free session status banner */}
      {!freeInfoLoading && freeSessionInfo && (
        <FreeSessionBanner info={freeSessionInfo} />
      )}

      <div className="card">
        <SectionHeader title={`${filtered.length} Bookings`}>
          <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
            <div className="tabs" style={{ marginBottom: 0 }}>
              {['ALL', 'CONFIRMED', 'COMPLETED', 'CANCELLED'].map(s => (
                <button
                  key={s}
                  className={`tab ${filter === s ? 'active' : ''}`}
                  onClick={() => setFilter(s)}
                >
                  {s}
                </button>
              ))}
            </div>
            <button
              className="btn btn-primary"
              style={{ whiteSpace: 'nowrap' }}
              onClick={() => setShowBook(true)}
            >
              + Book Session
            </button>
          </div>
        </SectionHeader>

        {loading ? <LoadingCenter /> : filtered.length === 0 ? (
          <EmptyState icon={<Icon name="calendar" size={20} style={{ color: 'var(--text3)' }} />} text="No bookings yet. Book your first session!" />
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table className="table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Trainer</th>
                  <th>Day / Time</th>
                  <th>Date</th>
                  <th>Type</th>
                  <th>Payment</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map(b => (
                  <tr key={b.id}>
                    <td style={{ color: 'var(--text3)', fontSize: 12 }}>#{b.id}</td>
                    <td style={{ fontWeight: 600 }}>{b.trainerEmail}</td>
                    <td>{b.sessionDay} · {b.sessionTime}</td>
                    <td style={{ fontSize: 12, color: 'var(--text3)' }}>{b.bookingDate}</td>
                    <td>
                      {b.sessionType === 'FREE_SESSION' ? (
                        <span style={{
                          background: 'rgba(0,229,160,0.12)', color: 'var(--green)',
                          padding: '2px 8px', borderRadius: 20, fontSize: 11, fontWeight: 700,
                        }}><Icon name="ticket" size={11} style={{ marginRight: 4 }} />Free</span>
                      ) : (
                        <span style={{
                          background: 'rgba(108,99,255,0.12)', color: 'var(--accent)',
                          padding: '2px 8px', borderRadius: 20, fontSize: 11, fontWeight: 700,
                        }}><Icon name="card" size={11} style={{ marginRight: 4 }} />Paid</span>
                      )}
                    </td>
                    <td>
                      <PaymentStatusBadge status={b.sessionType === 'FREE_SESSION' ? 'FREE' : b.paymentStatus} />
                    </td>
                    <td><StatusBadge status={b.status} /></td>
                    <td>
                      {b.status === 'CONFIRMED' && (
                        <button
                          className="btn btn-sm btn-ghost"
                          onClick={() => setConfirmCancelId(b.id)}
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

      {showBook && (
        <BookSessionModal
          member={member}
          onClose={() => setShowBook(false)}
          onSaved={() => { setShowBook(false); reload(); }}
        />
      )}

      {confirmCancelId && (
        <ConfirmModal
          title="Cancel Session"
          message="Are you sure you want to cancel this session? This action cannot be undone."
          onConfirm={confirmCancel}
          onClose={() => setConfirmCancelId(null)}
        />
      )}
    </div>
  );
}

// ── Free Session Banner ───────────────────────────────────────────────────────

function FreeSessionBanner({ info }) {
  const { remainingFreeSessions, totalFreeSessions } = info;

  // Non-subscribed member with 1 free session available
  if (totalFreeSessions === 1 && remainingFreeSessions === 1) {
    return (
      <div style={{
        display: 'flex', alignItems: 'center', gap: 12,
        background: 'rgba(0,229,160,0.08)', border: '1px solid rgba(0,229,160,0.3)',
        borderRadius: 12, padding: '12px 18px', marginBottom: 20,
      }}>
        <span style={{ color: 'var(--green)' }}><Icon name="ticket" size={22} /></span>
        <div>
          <span style={{ fontWeight: 700, color: 'var(--green)' }}>
            1 free trial session available
          </span>
          <span style={{ fontSize: 12, color: 'var(--text3)', marginLeft: 8 }}>
            Your complimentary session — no subscription needed!
          </span>
        </div>
      </div>
    );
  }

  // Unlimited sessions
  if (remainingFreeSessions === -1) {
    return (
      <div style={{
        display: 'flex', alignItems: 'center', gap: 12,
        background: 'rgba(0,229,160,0.08)', border: '1px solid rgba(0,229,160,0.3)',
        borderRadius: 12, padding: '12px 18px', marginBottom: 20,
      }}>
        <span style={{ color: 'var(--green)' }}><Icon name="infinity" size={22} /></span>
        <div>
          <span style={{ fontWeight: 700, color: 'var(--green)' }}>Unlimited free sessions</span>
          <span style={{ fontSize: 12, color: 'var(--text3)', marginLeft: 8 }}>included in your plan</span>
        </div>
      </div>
    );
  }

  // Has some free sessions remaining (subscribed)
  if (remainingFreeSessions > 0) {
    return (
      <div style={{
        display: 'flex', alignItems: 'center', gap: 12,
        background: 'rgba(0,229,160,0.08)', border: '1px solid rgba(0,229,160,0.3)',
        borderRadius: 12, padding: '12px 18px', marginBottom: 20,
      }}>
        <span style={{ color: 'var(--green)' }}><Icon name="ticket" size={22} /></span>
        <div>
          <span style={{ fontWeight: 700, color: 'var(--green)' }}>
            {remainingFreeSessions} free session{remainingFreeSessions !== 1 ? 's' : ''} remaining
          </span>
          <span style={{ fontSize: 12, color: 'var(--text3)', marginLeft: 8 }}>
            included in your membership plan
          </span>
        </div>
      </div>
    );
  }

  // No free sessions (used or not subscribed after trial)
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 12,
      background: 'rgba(255,179,71,0.08)', border: '1px solid rgba(255,179,71,0.3)',
      borderRadius: 12, padding: '12px 18px', marginBottom: 20,
    }}>
      <span style={{ color: 'var(--amber)' }}><Icon name="card" size={22} /></span>
      <div>
        <span style={{ fontWeight: 700, color: 'var(--amber)' }}>No free sessions remaining</span>
        <span style={{ fontSize: 12, color: 'var(--text3)', marginLeft: 8 }}>
          Payment will be required for new sessions
        </span>
      </div>
    </div>
  );
}

// ── Payment Status Badge ──────────────────────────────────────────────────────

function PaymentStatusBadge({ status }) {
  const map = {
    COMPLETED: { bg: 'rgba(0,229,160,0.12)',   color: 'var(--green)',  label: 'Paid' },
    PENDING:   { bg: 'rgba(255,179,71,0.12)',  color: 'var(--amber)',  label: 'Pending' },
    FAILED:    { bg: 'rgba(255,70,70,0.12)',   color: 'var(--red)',    label: 'Failed' },
    FREE:      { bg: 'rgba(0,229,160,0.12)',   color: 'var(--green)',  label: 'Free' },
  };
  const s = map[status] || { bg: 'var(--bg2)', color: 'var(--text3)', label: status };
  return (
    <span style={{
      background: s.bg, color: s.color,
      padding: '2px 8px', borderRadius: 20, fontSize: 11, fontWeight: 700,
    }}>
      {s.label}
    </span>
  );
}

// ── Book Session Modal ────────────────────────────────────────────────────────

function BookSessionModal({ member, onClose, onSaved }) {
  const dispatch = useDispatch();

  const [step,             setStep]             = useState('book');
  const [trainers,         setTrainers]         = useState([]);
  const [selectedTrainer,  setSelectedTrainer]  = useState(null);
  const [slots,            setSlots]            = useState([]);
  const [booking,          setBooking]          = useState(null);
  const [remainingFree,    setRemainingFree]     = useState(null);
  const [form,             setForm]             = useState({
    memberId:    member.id,
    memberEmail: member.email,
    trainerId:   '', trainerEmail: '',
    scheduleId:  '', sessionDay:   '', sessionTime: '', sessionDate: '', notes: '',
  });
  const [sessionAmount, setSessionAmount] = useState(0);
  const [method,        setMethod]        = useState('CASH');
  const [loading,       setLoading]       = useState(false);
  const [error,         setError]         = useState('');

  useEffect(() => {
    trainerApi.getAll()
      .then(r => setTrainers(r.data.filter(t => t.status === 'ACTIVE')))
      .catch(() => {});
    return () => { dispatch(clearLastBookingResult()); };
  }, []);

  const onTrainerChange = async (e) => {
    const t = trainers.find(x => x.id === +e.target.value);
    if (!t) return;
    setSelectedTrainer(t);
    if (t.sessionFee != null) setSessionAmount(Number(t.sessionFee));
    setForm(f => ({ ...f, trainerId: t.id, trainerEmail: t.email, scheduleId: '', sessionDay: '', sessionTime: '', sessionDate: '' }));
    const r = await trainerApi.getAvailableSlots(t.id).catch(() => ({ data: [] }));
    setSlots(r.data);
  };

  // ── Step 1: Create booking (backend decides free vs paid) ─────────────────
  const submitBooking = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const result = await dispatch(createBooking(form));

      if (createBooking.rejected.match(result)) {
        setError(result.payload || 'Booking failed');
        setLoading(false);
        return;
      }

      const { booking: newBooking, paymentRequired, freeSessionUsed, remainingFreeSessions, amount } = result.payload;

      setBooking(newBooking);
      setRemainingFree(remainingFreeSessions ?? null);

      // Use backend-provided amount (authoritative)
      if (amount && Number(amount) > 0) setSessionAmount(Number(amount));
      else if (selectedTrainer?.sessionFee) setSessionAmount(Number(selectedTrainer.sessionFee));

      if (!paymentRequired || freeSessionUsed) {
        setStep('free_success');
      } else {
        setStep('pay');
      }
    } catch {
      setError('Booking failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  // ── Step 2: Handle payment ────────────────────────────────────────────────
  const handlePayment = async () => {
    setError('');
    setLoading(true);
    const desc = `Session #${booking.id} – ${booking.sessionDay} ${booking.sessionTime}`;

    if (method === 'CASH') {
      try {
        await paymentApi.pay({
          memberId: member.id, memberEmail: member.email,
          bookingId: booking.id, amount: sessionAmount,
          paymentMethod: 'CASH', description: desc,
        });
        onSaved();
      } catch (err) {
        setLoading(false);
        setError(err.response?.data?.message || 'Payment failed. Your booking is saved (pending payment).');
      }
      return;
    }

    // Razorpay flow
    try {
      const orderRes = await paymentApi.createOrder(sessionAmount, desc);
      const { orderId, keyId } = orderRes.data;
      setLoading(false);

      const options = {
        key: keyId,
        amount: sessionAmount * 100,
        currency: 'INR',
        name: 'GymPro',
        description: desc,
        order_id: orderId,
        prefill: { email: member.email, name: member.name || member.email },
        theme: { color: '#6C63FF' },
        handler: async (response) => {
          try {
            await paymentApi.pay({
              memberId: member.id, memberEmail: member.email,
              bookingId: booking.id, amount: sessionAmount,
              paymentMethod: 'RAZORPAY', description: desc,
              razorpayOrderId:   response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            });
          } catch (err) {
            console.warn('Payment record save failed, booking is in PENDING state:', err);
          }
          onSaved();
        },
        modal: {
          ondismiss: () => {
            // User closed without paying — booking stays PENDING; just close the modal
            onSaved();
          },
        },
      };
      new window.Razorpay(options).open();
    } catch (err) {
      setLoading(false);
      setError(err.response?.data?.message || 'Could not open payment gateway. Your booking is saved (pending payment).');
    }
  };

  // ── Step: book ────────────────────────────────────────────────────────────
  if (step === 'book') {
    return (
      <Modal title="Book Training Session" onClose={onClose}>
        <form onSubmit={submitBooking} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {error && <div className="alert alert-error">{error}</div>}

          <FormGroup label="Select Trainer">
            <select value={form.trainerId} onChange={onTrainerChange} required>
              <option value="">Choose a trainer…</option>
              {trainers.map(t => (
                <option key={t.id} value={t.id}>
                  {t.name} — {t.specialization}
                  {t.sessionFee != null ? ` — ₹${t.sessionFee}/session` : ' — fee not set'}
                </option>
              ))}
            </select>
          </FormGroup>

          {/* Trainer fee hint */}
          {selectedTrainer && (
            <div style={{
              display: 'flex', alignItems: 'center', justifyContent: 'space-between',
              background: selectedTrainer.sessionFee != null
                ? 'rgba(0,229,160,0.07)' : 'rgba(255,179,71,0.07)',
              border: `1px solid ${selectedTrainer.sessionFee != null
                ? 'rgba(0,229,160,0.2)' : 'rgba(255,179,71,0.3)'}`,
              borderRadius: 8, padding: '8px 14px',
            }}>
              <span style={{ fontSize: 12, color: 'var(--text3)' }}>Session fee</span>
              {selectedTrainer.sessionFee != null ? (
                <span style={{ fontWeight: 700, color: 'var(--green)' }}>
                  ₹{selectedTrainer.sessionFee}
                </span>
              ) : (
                <span style={{ fontWeight: 600, color: 'var(--amber)', fontSize: 12 }}>
                  <Icon name="alertTriangle" size={13} style={{ marginRight: 4, verticalAlign: '-2px' }} />Trainer hasn't set a fee yet
                </span>
              )}
            </div>
          )}

          {slots.length > 0 && (
            <FormGroup label="Available Session">
              <select
                value={form.scheduleId}
                onChange={e => {
                  const s = slots.find(x => x.id === +e.target.value);
                  const dayName = s?.sessionDate
                    ? new Date(s.sessionDate + 'T00:00:00').toLocaleDateString(undefined, { weekday: 'long' })
                    : '';
                  setForm(f => ({
                    ...f,
                    scheduleId:  s?.id          || '',
                    sessionDay:  dayName,
                    sessionTime: s ? `${s.startTime} - ${s.endTime}` : '',
                    sessionDate: s?.sessionDate || '',
                  }));
                }}
                required
              >
                <option value="">Choose a session…</option>
                {slots.map(s => (
                  <option key={s.id} value={s.id}>
                    {formatDateLabel(s.sessionDate)}: {s.startTime} – {s.endTime}
                    {typeof s.maxCapacity === 'number' ? ` (${s.bookedCount ?? 0}/${s.maxCapacity})` : ''}
                  </option>
                ))}
              </select>
            </FormGroup>
          )}

          {form.trainerId && slots.length === 0 && (
            <div className="alert alert-error">No available slots for this trainer</div>
          )}

          <FormGroup label="Notes (optional)">
            <input
              value={form.notes}
              onChange={e => setForm(f => ({ ...f, notes: e.target.value }))}
              placeholder="Any special requests…"
            />
          </FormGroup>

          <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
            <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading || !form.scheduleId || !form.sessionDate}
            >
              {loading ? 'Booking…' : 'Book Session'}
            </button>
          </div>
        </form>
      </Modal>
    );
  }

  // ── Step: free_success ────────────────────────────────────────────────────
  if (step === 'free_success') {
    return (
      <Modal title="Session Booked!" onClose={onSaved}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div className="alert alert-success" style={{ display: 'flex', alignItems: 'center', gap: 8 }}><Icon name="checkCircle" size={15} />Session booked successfully!</div>
          <div style={{
            background: 'rgba(0,229,160,0.08)', border: '1px solid rgba(0,229,160,0.3)',
            borderRadius: 12, padding: 20, textAlign: 'center',
          }}>
            <div style={{ marginBottom: 8, color: 'var(--green)', display: 'flex', justifyContent: 'center' }}><Icon name="checkCircle" size={36} /></div>
            <div style={{ fontSize: 18, fontWeight: 700, color: 'var(--green)', marginBottom: 6 }}>
              Covered by Your Plan
            </div>
            <div style={{ fontSize: 13, color: 'var(--text3)', marginBottom: 12 }}>
              This session is free — no payment needed.
            </div>
            <div style={{ fontSize: 13, color: 'var(--text2)', fontWeight: 600 }}>
              {booking?.sessionDay} · {booking?.sessionTime}
            </div>
            {booking?.bookingDate && (
              <div style={{ fontSize: 12, color: 'var(--text3)', marginTop: 2 }}>
                {formatDateLabel(booking.bookingDate)}
              </div>
            )}
          </div>

          {/* Remaining sessions counter */}
          {remainingFree !== null && (
            <div style={{
              display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
              background: 'rgba(108,99,255,0.06)', border: '1px solid rgba(108,99,255,0.15)',
              borderRadius: 8, padding: '8px 14px', fontSize: 13,
            }}>
              <span style={{ color: 'var(--text3)' }}><Icon name="ticket" size={15} /></span>
              <span style={{ color: 'var(--text3)' }}>
                {remainingFree === -1 ? (
                  <span style={{ color: 'var(--green)', fontWeight: 700 }}>Unlimited sessions remaining</span>
                ) : remainingFree > 0 ? (
                  <>
                    <strong style={{ color: 'var(--accent)' }}>{remainingFree}</strong>
                    {' '}free session{remainingFree !== 1 ? 's' : ''} still remaining in your plan
                  </>
                ) : (
                  <span style={{ color: 'var(--amber)' }}>
                    You've used all free sessions — next session will require payment
                  </span>
                )}
              </span>
            </div>
          )}

          <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <button className="btn btn-primary" onClick={onSaved}>Done</button>
          </div>
        </div>
      </Modal>
    );
  }

  // ── Step: pay ─────────────────────────────────────────────────────────────
  return (
    <Modal title="Complete Payment" onClose={onSaved}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {error && <div className="alert alert-error">{error}</div>}

        <div className="alert alert-success" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <Icon name="checkCircle" size={15} />Session booked! Complete payment to confirm your spot.
        </div>

        {/* Amount display */}
        <div style={{ background: 'var(--bg2)', borderRadius: 10, padding: 16, textAlign: 'center' }}>
          <div style={{ fontSize: 12, color: 'var(--text3)', marginBottom: 4 }}>Session Fee</div>
          <div style={{ fontFamily: 'var(--font-display)', fontSize: 48, color: 'var(--green)' }}>
            ₹{sessionAmount}
          </div>
          <div style={{ fontSize: 12, color: 'var(--text3)', marginTop: 4 }}>
            {booking?.sessionDay} · {booking?.sessionTime}
            {booking?.bookingDate ? ` · ${formatDateLabel(booking.bookingDate)}` : ''}
          </div>
          {selectedTrainer?.name && (
            <div style={{ fontSize: 12, color: 'var(--text3)', marginTop: 2 }}>
              with {selectedTrainer.name}
            </div>
          )}
        </div>

        {/* Payment method */}
        <div>
          <div style={{ fontSize: 12, fontWeight: 600, color: 'var(--text3)', marginBottom: 6 }}>
            Payment Method
          </div>
          <select value={method} onChange={e => setMethod(e.target.value)}>
            <option value="CASH">Cash</option>
            <option value="ONLINE">Online (UPI / Card / Net Banking)</option>
          </select>
        </div>

        {method !== 'CASH' && (
          <div style={{
            background: 'var(--bg2)', borderRadius: 8,
            padding: '10px 14px', fontSize: 12, color: 'var(--text3)',
            display: 'flex', alignItems: 'center', gap: 6,
          }}>
            <Icon name="lock" size={13} />Secured by Razorpay · UPI, Cards, Net Banking &amp; Wallets
          </div>
        )}

        <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
          <button className="btn btn-ghost" onClick={onSaved}>Pay Later</button>
          <button
            className="btn btn-primary"
            onClick={handlePayment}
            disabled={loading}
          >
            {loading
              ? 'Processing…'
              : method === 'CASH'
                ? `Pay ₹${sessionAmount} (Cash)`
                : `Pay ₹${sessionAmount} Online`
            }
          </button>
        </div>
      </div>
    </Modal>
  );
}
