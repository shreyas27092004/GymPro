// src/pages/trainer/TrainerSchedule.jsx
import { useState, useEffect } from 'react';
import { trainerApi } from '../../api/api';
import { useAuth } from '../../context/AuthContext';
import { LoadingCenter, Alert, Modal, FormGroup } from '../../components/UI';
import Icon from '../../components/icons';

function formatDateLabel(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr + 'T00:00:00');
  return d.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' });
}

export default function TrainerSchedule() {
  const { email } = useAuth();
  const [trainer,     setTrainer]     = useState(null);
  const [slots,       setSlots]       = useState([]);
  const [loading,     setLoading]     = useState(true);
  const [error,       setError]       = useState('');
  const [showAdd,     setShowAdd]     = useState(false);
  const [deletingId,  setDeletingId]  = useState(null);   // slot being deleted (spinner)
  const [deleteError, setDeleteError] = useState('');     // per-slot error message
  const [confirmSlot, setConfirmSlot] = useState(null);   // slot awaiting delete-confirm dialog
  const [cancellingId,setCancellingId]= useState(null);
  const [confirmCancelSlot, setConfirmCancelSlot] = useState(null); // slot awaiting cancel-session confirm

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

  const handleCancelConfirm = async () => {
    const slot = confirmCancelSlot;
    setConfirmCancelSlot(null);
    setDeleteError('');
    setCancellingId(slot.id);
    try {
      await trainerApi.cancelSchedule(slot.id);
      await load();
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to cancel session';
      setDeleteError(msg);
    } finally {
      setCancellingId(null);
    }
  };

  // Sort upcoming-first by date then start time
  const sorted = [...slots].sort((a, b) => {
    const d = (a.sessionDate || '').localeCompare(b.sessionDate || '');
    return d !== 0 ? d : (a.startTime || '').localeCompare(b.startTime || '');
  });

  const today = new Date().toISOString().slice(0, 10);
  const upcoming = sorted.filter(s => (s.sessionDate || '') >= today && !s.cancelled);
  const past     = sorted.filter(s => (s.sessionDate || '') < today || s.cancelled);

  const renderSlot = (slot) => {
    const isFull      = !slot.cancelled && slot.bookedCount >= slot.maxCapacity;
    const isCancelled = slot.cancelled;
    const isDeleting  = deletingId === slot.id;
    const isCancelling= cancellingId === slot.id;
    const hasBookings = slot.bookedCount > 0;

    let statusLabel = 'OPEN', statusColor = 'var(--green)', bg = 'rgba(0,229,160,0.08)', border = 'rgba(0,229,160,0.25)';
    if (isCancelled) { statusLabel = 'CANCELLED'; statusColor = 'var(--red)'; bg = 'rgba(255,91,110,0.08)'; border = 'rgba(255,91,110,0.25)'; }
    else if (isFull)  { statusLabel = 'FULL'; statusColor = 'var(--amber)'; bg = 'rgba(255,179,71,0.08)'; border = 'rgba(255,179,71,0.25)'; }

    return (
      <div key={slot.id} style={{
        background: bg, border: `1px solid ${border}`, borderRadius: 12,
        padding: '14px 16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 16, marginBottom: 10,
      }}>
        <div>
          <div style={{ fontSize: 10, color: statusColor, fontWeight: 700, marginBottom: 4, letterSpacing: 0.5 }}>{statusLabel}</div>
          <div style={{ fontWeight: 700, color: 'var(--text)', fontSize: 14 }}>{formatDateLabel(slot.sessionDate)}</div>
          <div style={{ fontSize: 12, color: 'var(--text3)', marginTop: 2 }}>{slot.startTime} – {slot.endTime}</div>
          <div style={{ fontSize: 11, color: 'var(--text3)', marginTop: 4 }}>
            <Icon name="users" size={11} style={{ marginRight: 4, verticalAlign: '-1px' }} />
            {slot.bookedCount} / {slot.maxCapacity} booked
          </div>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6, minWidth: 90 }}>
          {!isCancelled && !hasBookings && (
            <button
              onClick={() => { setDeleteError(''); setConfirmSlot(slot); }}
              disabled={isDeleting}
              className="btn btn-ghost btn-sm"
              style={{ color: 'var(--red)' }}
            >
              {isDeleting ? '…' : <><Icon name="trash" size={12} style={{ marginRight: 4 }} />Delete</>}
            </button>
          )}
          {!isCancelled && hasBookings && (
            <button
              onClick={() => { setDeleteError(''); setConfirmCancelSlot(slot); }}
              disabled={isCancelling}
              className="btn btn-danger btn-sm"
            >
              {isCancelling ? '…' : 'Cancel Session'}
            </button>
          )}
          {isCancelled && (
            <div style={{ fontSize: 10, color: 'var(--text3)', textAlign: 'center' }}>
              <Icon name="lock" size={11} style={{ marginRight: 3, verticalAlign: '-1px' }} />No action
            </div>
          )}
        </div>
      </div>
    );
  };

  return (
    <div className="fade-in">
      <div className="content-header">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div>
            <div className="page-title">MY SCHEDULE</div>
            <div className="page-subtitle">Your upcoming training sessions</div>
          </div>
          {trainer && <button className="btn btn-primary" style={{ marginTop: 8 }} onClick={() => setShowAdd(true)}>+ Add Session</button>}
        </div>
      </div>
      <div className="content-body">
        {error && <Alert type="error">{error}</Alert>}
        {deleteError && <Alert type="error" onClose={() => setDeleteError('')}>{deleteError}</Alert>}

        {loading ? <LoadingCenter /> : (
          <>
            <div style={{ fontSize: 13, fontWeight: 700, color: 'var(--text3)', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 10 }}>
              Upcoming ({upcoming.length})
            </div>
            {upcoming.length === 0 ? (
              <div style={{ background: 'var(--bg3)', border: '1px dashed var(--border)', borderRadius: 10, padding: '20px 8px', textAlign: 'center', fontSize: 12, color: 'var(--text3)', marginBottom: 24 }}>
                No upcoming sessions — add one to get started.
              </div>
            ) : (
              <div style={{ marginBottom: 24 }}>{upcoming.map(renderSlot)}</div>
            )}

            {past.length > 0 && (
              <>
                <div style={{ fontSize: 13, fontWeight: 700, color: 'var(--text3)', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 10 }}>
                  Past / Cancelled ({past.length})
                </div>
                <div style={{ opacity: 0.7 }}>{past.map(renderSlot)}</div>
              </>
            )}
          </>
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
                <div style={{ marginBottom: 8, color: 'var(--red)', display: 'flex', justifyContent: 'center' }}><Icon name="trash" size={28} /></div>
                <div style={{ fontWeight: 700, color: 'var(--text)', marginBottom: 6 }}>
                  {formatDateLabel(confirmSlot.sessionDate)}
                </div>
                <div style={{ fontSize: 15, color: 'var(--accent)', fontWeight: 700 }}>
                  {confirmSlot.startTime} – {confirmSlot.endTime}
                </div>
              </div>
              <div style={{ fontSize: 13, color: 'var(--text3)', textAlign: 'center', lineHeight: 1.6 }}>
                Are you sure you want to delete this open slot?<br />
                This action cannot be undone.
              </div>
              <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
                <button className="btn btn-ghost" onClick={() => setConfirmSlot(null)}>Cancel</button>
                <button className="btn btn-danger" onClick={handleDeleteConfirm}>Delete Slot</button>
              </div>
            </div>
          </Modal>
        )}

        {/* Cancel Session Confirmation Modal (has bookings) */}
        {confirmCancelSlot && (
          <Modal title="Cancel Session" onClose={() => setConfirmCancelSlot(null)}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
              <div style={{ background: 'var(--bg2)', borderRadius: 10, padding: 16, textAlign: 'center' }}>
                <div style={{ marginBottom: 8, color: 'var(--red)', display: 'flex', justifyContent: 'center' }}><Icon name="alertTriangle" size={28} /></div>
                <div style={{ fontWeight: 700, color: 'var(--text)', marginBottom: 6 }}>
                  {formatDateLabel(confirmCancelSlot.sessionDate)}
                </div>
                <div style={{ fontSize: 15, color: 'var(--accent)', fontWeight: 700 }}>
                  {confirmCancelSlot.startTime} – {confirmCancelSlot.endTime}
                </div>
              </div>
              <div style={{ fontSize: 13, color: 'var(--text3)', textAlign: 'center', lineHeight: 1.6 }}>
                This session has <strong>{confirmCancelSlot.bookedCount}</strong> member{confirmCancelSlot.bookedCount !== 1 ? 's' : ''} booked.
                Cancelling will notify them and cannot be undone. Are you sure?
              </div>
              <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
                <button className="btn btn-ghost" onClick={() => setConfirmCancelSlot(null)}>Keep Session</button>
                <button className="btn btn-danger" onClick={handleCancelConfirm}>Cancel Session</button>
              </div>
            </div>
          </Modal>
        )}
      </div>
    </div>
  );
}

function AddSlotModal({ trainerId, onClose, onSaved }) {
  const todayStr = new Date().toISOString().slice(0, 10);
  const [form, setForm] = useState({ trainerId, sessionDate: todayStr, startTime: '09:00', endTime: '10:00', maxCapacity: 1 });
  const [loading, setLoading] = useState(false);
  const [error,   setError]   = useState('');

  const submit = async (e) => {
    e.preventDefault(); setError(''); setLoading(true);
    try {
      await trainerApi.addSchedule({ ...form, maxCapacity: parseInt(form.maxCapacity, 10) || 1 });
      onSaved();
    } catch (err) { setError(err.response?.data?.message || 'Failed to add slot'); }
    finally { setLoading(false); }
  };

  return (
    <Modal title="Add Session Slot" onClose={onClose}>
      <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {error && <Alert type="error">{error}</Alert>}
        <FormGroup label="Date">
          <input type="date" min={todayStr} value={form.sessionDate} onChange={e => setForm({...form, sessionDate: e.target.value})} required />
        </FormGroup>
        <div className="form-row">
          <FormGroup label="Start Time"><input type="time" value={form.startTime} onChange={e => setForm({...form, startTime: e.target.value})} required /></FormGroup>
          <FormGroup label="End Time"><input type="time" value={form.endTime} onChange={e => setForm({...form, endTime: e.target.value})} required /></FormGroup>
        </div>
        <FormGroup label="Max Capacity">
          <input type="number" min="1" max="200" value={form.maxCapacity} onChange={e => setForm({...form, maxCapacity: e.target.value})} required />
        </FormGroup>
        <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={loading}>{loading ? 'Adding…' : 'Add Slot'}</button>
        </div>
      </form>
    </Modal>
  );
}
