// src/pages/admin/Bookings.jsx

import { useState, useEffect } from 'react';
import { bookingApi, memberApi, trainerApi } from '../../api/api';
import { LoadingCenter, Alert, Modal, ConfirmModal, StatusBadge, EmptyState, SectionHeader, FormGroup } from '../../components/UI';
import Icon from '../../components/icons';

export default function AdminBookings() {
  const [bookings, setBookings] = useState([]);
  const [loading,  setLoading]  = useState(true);
  const [error,    setError]    = useState('');
  const [filter,   setFilter]   = useState('ALL');
  const [showForm, setShowForm] = useState(false);
  // Set when the backend responds with confirmationRequired=true for a future-dated
  // booking — holds the booking id + warning message until the admin confirms/cancels.
  const [confirmComplete, setConfirmComplete] = useState(null);
  const [confirmCancelBooking, setConfirmCancelBooking] = useState(null);

  // Load all bookings by fetching for each member (admin workaround)
  const load = async () => {
    try {
      setLoading(true);
      // Get all members then load bookings for each
      const mRes = await memberApi.getAll();
      const allBookings = [];
      await Promise.all(mRes.data.map(async m => {
        try {
          const r = await bookingApi.getByMember(m.id);
          allBookings.push(...r.data);
        } catch {}
      }));
      // De-dupe by id
      const unique = [...new Map(allBookings.map(b => [b.id, b])).values()];
      unique.sort((a,b) => b.id - a.id);
      setBookings(unique);
    } catch (e) {
      setError('Failed to load bookings');
    } finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);

  const filtered = filter === 'ALL' ? bookings : bookings.filter(b => b.status === filter);

  const handleAction = async (id, action) => {
    try {
      if (action === 'cancel') await bookingApi.cancel(id);
      if (action === 'complete') {
        const res = await bookingApi.complete(id);
        if (res.data?.confirmationRequired) {
          // Future-dated booking — don't reload yet, ask the admin to confirm first.
          setConfirmComplete({ id, message: res.data.message });
          return;
        }
      }
      load();
    } catch (e) {
      setError(e.response?.data?.message || 'Action failed');
    }
  };

  const confirmCompleteBooking = async () => {
    if (!confirmComplete) return;
    try {
      await bookingApi.complete(confirmComplete.id, true);
      setConfirmComplete(null);
      load();
    } catch (e) {
      setError(e.response?.data?.message || 'Action failed');
      setConfirmComplete(null);
    }
  };

  const confirmCancelBookingAction = async () => {
    if (!confirmCancelBooking) return;
    const id = confirmCancelBooking.id;
    setConfirmCancelBooking(null);
    await handleAction(id, 'cancel');
  };

  return (
    <div className="fade-in">
      <div className="content-header">
        <div className="page-title">BOOKINGS</div>
        <div className="page-subtitle">All session bookings across the gym</div>
      </div>
      <div className="content-body">
        {error && <Alert type="error">{error}</Alert>}

        <div className="card">
          <SectionHeader title={`${filtered.length} Bookings`}>
            <div className="tabs" style={{ marginBottom: 0, flex: 'none' }}>
              {['ALL','CONFIRMED','COMPLETED','CANCELLED'].map(s => (
                <button key={s} className={`tab ${filter === s ? 'active' : ''}`} onClick={() => setFilter(s)}>
                  {s === 'ALL' ? 'All' : s.charAt(0) + s.slice(1).toLowerCase()}
                </button>
              ))}
            </div>
            <button className="btn btn-primary" onClick={() => setShowForm(true)}>+ New Booking</button>
          </SectionHeader>

          {loading ? <LoadingCenter /> : filtered.length === 0 ? (
            <EmptyState icon={<Icon name="calendar" size={20} style={{ color: 'var(--text3)' }} />} text="No bookings found" />
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>ID</th><th>Member</th><th>Trainer</th>
                    <th>Day</th><th>Time</th><th>Date</th><th>Status</th><th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map(b => (
                    <tr key={b.id}>
                      <td style={{ color: 'var(--text3)', fontSize: 12 }}>#{b.id}</td>
                      <td style={{ color: 'var(--text)', fontWeight: 600 }}>{b.memberEmail}</td>
                      <td>{b.trainerEmail}</td>
                      <td>
                        <span style={{ background: 'var(--bg2)', padding: '2px 8px', borderRadius: 6, fontSize: 12, fontWeight: 700 }}>{b.sessionDay}</span>
                      </td>
                      <td style={{ fontSize: 13 }}>{b.sessionTime}</td>
                      <td style={{ fontSize: 13 }}>{b.bookingDate}</td>
                      <td><StatusBadge status={b.status} /></td>
                      <td>
                        <div style={{ display: 'flex', gap: 8 }}>
                          {b.status === 'CONFIRMED' && (
                            <>
                              <button className="btn btn-success btn-sm" onClick={() => handleAction(b.id, 'complete')}>Complete</button>
                              <button className="btn btn-danger btn-sm" onClick={() => setConfirmCancelBooking(b)}>Cancel</button>
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {showForm && <NewBookingForm onClose={() => setShowForm(false)} onSaved={() => { setShowForm(false); load(); }} />}

        {confirmComplete && (
          <ConfirmModal
            title="Confirm Completion"
            message={confirmComplete.message}
            danger={false}
            onConfirm={confirmCompleteBooking}
            onClose={() => setConfirmComplete(null)}
          />
        )}

        {confirmCancelBooking && (
          <ConfirmModal
            title="Cancel Booking"
            message={`Cancel booking #${confirmCancelBooking.id} for ${confirmCancelBooking.memberEmail}? This cannot be undone.`}
            onConfirm={confirmCancelBookingAction}
            onClose={() => setConfirmCancelBooking(null)}
          />
        )}
      </div>
    </div>
  );
}

function NewBookingForm({ onClose, onSaved }) {
  const [members,   setMembers]   = useState([]);
  const [trainers,  setTrainers]  = useState([]);
  const [slots,     setSlots]     = useState([]);
  const [form, setForm] = useState({ memberId: '', memberEmail: '', trainerId: '', trainerEmail: '', scheduleId: '', sessionDay: '', sessionTime: '', sessionDate: '', notes: '' });
  const [loading, setLoading] = useState(false);
  const [error,   setError]   = useState('');

  useEffect(() => {
    Promise.all([memberApi.getAll(), trainerApi.getAll()])
      .then(([m, t]) => { setMembers(m.data); setTrainers(t.data); })
      .catch(() => {});
  }, []);

  const onTrainerChange = async (e) => {
    const trainer = trainers.find(t => t.id === +e.target.value);
    if (!trainer) return;
    setForm(f => ({ ...f, trainerId: trainer.id, trainerEmail: trainer.email }));
    const r = await trainerApi.getAvailableSlots(trainer.id).catch(() => ({ data: [] }));
    setSlots(r.data);
  };

  const submit = async (e) => {
    e.preventDefault(); setError(''); setLoading(true);
    try {
      await bookingApi.create(form);
      onSaved();
    } catch (err) { setError(err.response?.data?.message || 'Failed to create booking'); }
    finally { setLoading(false); }
  };

  return (
    <Modal title="New Booking" onClose={onClose}>
      <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {error && <Alert type="error">{error}</Alert>}
        <FormGroup label="Member">
          <select value={form.memberId} onChange={e => {
            const m = members.find(x => x.id === +e.target.value);
            setForm(f => ({ ...f, memberId: m?.id || '', memberEmail: m?.email || '' }));
          }} required>
            <option value="">Select member…</option>
            {members.map(m => <option key={m.id} value={m.id}>{m.name} ({m.email})</option>)}
          </select>
        </FormGroup>
        <FormGroup label="Trainer">
          <select value={form.trainerId} onChange={onTrainerChange} required>
            <option value="">Select trainer…</option>
            {trainers.map(t => <option key={t.id} value={t.id}>{t.name} — {t.specialization}</option>)}
          </select>
        </FormGroup>
        {slots.length > 0 && (
          <FormGroup label="Available Session">
            <select value={form.scheduleId} onChange={e => {
              const s = slots.find(x => x.id === +e.target.value);
              const dayName = s?.sessionDate
                ? new Date(s.sessionDate + 'T00:00:00').toLocaleDateString(undefined, { weekday: 'long' })
                : '';
              setForm(f => ({ ...f, scheduleId: s?.id || '', sessionDay: dayName, sessionTime: s ? `${s.startTime} - ${s.endTime}` : '', sessionDate: s?.sessionDate || '' }));
            }} required>
              <option value="">Select session…</option>
              {slots.map(s => <option key={s.id} value={s.id}>{s.sessionDate}: {s.startTime} – {s.endTime}</option>)}
            </select>
          </FormGroup>
        )}
        <FormGroup label="Notes (optional)"><input value={form.notes} onChange={e => setForm(f => ({ ...f, notes: e.target.value }))} /></FormGroup>
        <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={loading}>{loading ? 'Booking…' : 'Create Booking'}</button>
        </div>
      </form>
    </Modal>
  );
}
