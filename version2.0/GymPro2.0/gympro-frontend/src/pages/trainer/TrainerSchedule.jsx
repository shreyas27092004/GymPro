// src/pages/trainer/TrainerSchedule.jsx
import { useState, useEffect } from 'react';
import { trainerApi } from '../../api/api';
import { useAuth } from '../../context/AuthContext';
import { LoadingCenter, Alert, EmptyState, Modal, FormGroup } from '../../components/UI';

const DAYS = ['MON','TUE','WED','THU','FRI','SAT','SUN'];
const DAY_LABELS = { MON: 'Monday', TUE: 'Tuesday', WED: 'Wednesday', THU: 'Thursday', FRI: 'Friday', SAT: 'Saturday', SUN: 'Sunday' };

export default function TrainerSchedule() {
  const { email } = useAuth();
  const [trainer,     setTrainer]     = useState(null);
  const [slots,       setSlots]       = useState([]);
  const [loading,     setLoading]     = useState(true);
  const [error,       setError]       = useState('');
  const [showAdd,     setShowAdd]     = useState(false);
  const [deletingId,  setDeletingId]  = useState(null);   // slot being deleted (spinner)
  const [deleteError, setDeleteError] = useState('');     // per-slot error message
  const [confirmSlot, setConfirmSlot] = useState(null);   // slot awaiting confirm dialog

  const load = async () => {
    try {
      setLoading(true);
      const all = await trainerApi.getAll();
      const me  = all.data.find(t => t.email === email);
      if (!me) { setError('Trainer profile not found'); setLoading(false); return; }
      setTrainer(me);
      const res = await trainerApi.getSchedule(me.id);
      setSlots(res.data);
    } catch { setError('Failed to load schedule'); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, [email]);

  const handleDeleteConfirm = async () => {
    const slot = confirmSlot;
    setConfirmSlot(null);
    setDeleteError('');
    setDeletingId(slot.id);
    try {
      await trainerApi.deleteSchedule(slot.id);
      await load();
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to delete slot';
      setDeleteError(msg);
    } finally {
      setDeletingId(null);
    }
  };

  // Group by day
  const byDay = DAYS.reduce((acc, d) => {
    acc[d] = slots.filter(s => s.dayOfWeek === d);
    return acc;
  }, {});

  return (
    <div className="fade-in">
      <div className="content-header">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div>
            <div className="page-title">MY SCHEDULE</div>
            <div className="page-subtitle">Your weekly training slots</div>
          </div>
          {trainer && <button className="btn btn-primary" style={{ marginTop: 8 }} onClick={() => setShowAdd(true)}>+ Add Slot</button>}
        </div>
      </div>
      <div className="content-body">
        {error && <Alert type="error">{error}</Alert>}
        {deleteError && <Alert type="error">{deleteError}</Alert>}

        {loading ? <LoadingCenter /> : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 12 }}>
            {DAYS.map(day => (
              <div key={day}>
                <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--text3)', letterSpacing: 1, textTransform: 'uppercase', marginBottom: 8, textAlign: 'center' }}>
                  {day}
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                  {byDay[day].length === 0 ? (
                    <div style={{ background: 'var(--bg3)', border: '1px dashed var(--border)', borderRadius: 10, padding: '16px 8px', textAlign: 'center', fontSize: 11, color: 'var(--text3)' }}>
                      Free
                    </div>
                  ) : byDay[day].map(slot => {
                    const isBooked   = !slot.available;
                    const isDeleting = deletingId === slot.id;
                    return (
                      <div key={slot.id} style={{
                        background: isBooked ? 'rgba(255,91,110,0.08)' : 'rgba(0,229,160,0.08)',
                        border: `1px solid ${isBooked ? 'rgba(255,91,110,0.25)' : 'rgba(0,229,160,0.25)'}`,
                        borderRadius: 10, padding: '10px 8px', textAlign: 'center',
                        position: 'relative',
                      }}>
                        <div style={{ fontSize: 10, color: isBooked ? 'var(--red)' : 'var(--green)', fontWeight: 700, marginBottom: 4 }}>
                          {isBooked ? 'BOOKED' : 'FREE'}
                        </div>
                        <div style={{ fontSize: 11, color: 'var(--text2)', lineHeight: 1.4 }}>
                          {slot.startTime}<br />–<br />{slot.endTime}
                        </div>

                        {/* Delete button — shown for FREE slots only */}
                        {!isBooked && (
                          <button
                            onClick={() => { setDeleteError(''); setConfirmSlot(slot); }}
                            disabled={isDeleting}
                            title="Delete this slot"
                            style={{
                              marginTop: 8,
                              background: 'rgba(255,77,109,0.12)',
                              border: '1px solid rgba(255,77,109,0.3)',
                              borderRadius: 6,
                              color: 'var(--red)',
                              fontSize: 11,
                              fontWeight: 700,
                              cursor: isDeleting ? 'not-allowed' : 'pointer',
                              padding: '3px 8px',
                              width: '100%',
                              transition: 'all 0.15s',
                            }}
                            onMouseOver={e => { if (!isDeleting) e.currentTarget.style.background = 'rgba(255,77,109,0.22)'; }}
                            onMouseOut={e => { e.currentTarget.style.background = 'rgba(255,77,109,0.12)'; }}
                          >
                            {isDeleting ? '…' : '🗑 Delete'}
                          </button>
                        )}

                        {/* Lock icon for booked slots */}
                        {isBooked && (
                          <div style={{ marginTop: 6, fontSize: 10, color: 'var(--text3)' }} title="Cannot delete — already booked">
                            🔒 Locked
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Add Slot Modal */}
        {showAdd && trainer && (
          <AddSlotModal trainerId={trainer.id} onClose={() => setShowAdd(false)} onSaved={() => { setShowAdd(false); load(); }} />
        )}

        {/* Delete Confirmation Modal */}
        {confirmSlot && (
          <Modal title="Delete Schedule Slot" onClose={() => setConfirmSlot(null)}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
              <div style={{ background: 'var(--bg2)', borderRadius: 10, padding: 16, textAlign: 'center' }}>
                <div style={{ fontSize: 28, marginBottom: 8 }}>🗑️</div>
                <div style={{ fontWeight: 700, color: 'var(--text)', marginBottom: 6 }}>
                  {DAY_LABELS[confirmSlot.dayOfWeek]}
                </div>
                <div style={{ fontSize: 15, color: 'var(--accent)', fontWeight: 700 }}>
                  {confirmSlot.startTime} – {confirmSlot.endTime}
                </div>
              </div>
              <div style={{ fontSize: 13, color: 'var(--text3)', textAlign: 'center', lineHeight: 1.6 }}>
                Are you sure you want to delete this free slot?<br />
                This action cannot be undone.
              </div>
              <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
                <button className="btn btn-ghost" onClick={() => setConfirmSlot(null)}>Cancel</button>
                <button className="btn btn-danger" onClick={handleDeleteConfirm}>Delete Slot</button>
              </div>
            </div>
          </Modal>
        )}
      </div>
    </div>
  );
}

function AddSlotModal({ trainerId, onClose, onSaved }) {
  const [form, setForm] = useState({ trainerId, dayOfWeek: 'MON', startTime: '09:00', endTime: '11:00' });
  const [loading, setLoading] = useState(false);
  const [error,   setError]   = useState('');

  const submit = async (e) => {
    e.preventDefault(); setError(''); setLoading(true);
    try { await trainerApi.addSchedule(form); onSaved(); }
    catch (err) { setError(err.response?.data?.message || 'Failed to add slot'); }
    finally { setLoading(false); }
  };

  return (
    <Modal title="Add Schedule Slot" onClose={onClose}>
      <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {error && <div className="alert alert-error">{error}</div>}
        <FormGroup label="Day of Week">
          <select value={form.dayOfWeek} onChange={e => setForm({...form, dayOfWeek: e.target.value})}>
            {DAYS.map(d => <option key={d} value={d}>{DAY_LABELS[d]}</option>)}
          </select>
        </FormGroup>
        <div className="form-row">
          <FormGroup label="Start Time"><input type="time" value={form.startTime} onChange={e => setForm({...form, startTime: e.target.value})} required /></FormGroup>
          <FormGroup label="End Time"><input type="time" value={form.endTime} onChange={e => setForm({...form, endTime: e.target.value})} required /></FormGroup>
        </div>
        <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={loading}>{loading ? 'Adding…' : 'Add Slot'}</button>
        </div>
      </form>
    </Modal>
  );
}
