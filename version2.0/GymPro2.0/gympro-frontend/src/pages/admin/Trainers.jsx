// src/pages/admin/Trainers.jsx

import { useState, useEffect } from 'react';
import { trainerApi } from '../../api/api';
import { LoadingCenter, Alert, Modal, StatusBadge, EmptyState, SectionHeader, FormGroup, ConfirmModal } from '../../components/UI';

export default function AdminTrainers() {
  const [trainers, setTrainers] = useState([]);
  const [loading,  setLoading]  = useState(true);
  const [error,    setError]    = useState('');
  const [showForm, setShowForm] = useState(false);
  const [editItem, setEditItem] = useState(null);
  const [confirm,  setConfirm]  = useState(null);
  const [search,   setSearch]   = useState('');
  const [viewSchedule, setViewSchedule] = useState(null);
  const [togglingId, setTogglingId] = useState(null);

  const load = async () => {
    try {
      setLoading(true);
      const res = await trainerApi.getAll();
      setTrainers(res.data);
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to load trainers');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const filtered = trainers.filter(t =>
    t.name?.toLowerCase().includes(search.toLowerCase()) ||
    t.specialization?.toLowerCase().includes(search.toLowerCase())
  );

  const toggleStatus = async (trainer) => {
    setTogglingId(trainer.id);
    try {
      const newStatus = trainer.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
      await trainerApi.update(trainer.id, { ...trainer, status: newStatus });
      setTrainers(prev =>
        prev.map(t => t.id === trainer.id ? { ...t, status: newStatus } : t)
      );
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to update trainer status');
    } finally {
      setTogglingId(null);
    }
  };

  return (
    <div className="fade-in">
      <div className="content-header">
        <div className="page-title">TRAINERS</div>
        <div className="page-subtitle">Manage gym trainers and their schedules</div>
      </div>
      <div className="content-body">
        {error && <Alert type="error">{error}</Alert>}

        <div className="card">
          <SectionHeader title={`${filtered.length} Trainers`}>
            <input
              placeholder="Search trainers…"
              value={search}
              onChange={e => setSearch(e.target.value)}
              style={{ width: 220 }}
            />
            <button className="btn btn-primary" onClick={() => { setEditItem(null); setShowForm(true); }}>
              + Add Trainer
            </button>
          </SectionHeader>

          {loading ? <LoadingCenter /> : filtered.length === 0 ? (
            <EmptyState icon="💪" text="No trainers found" />
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Name</th><th>Email</th><th>Specialization</th>
                    <th>Experience</th><th>Session Fee</th><th>Status</th><th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map(t => (
                    <tr key={t.id}>
                      <td style={{ color: 'var(--text)', fontWeight: 600 }}>{t.name}</td>
                      <td>{t.email}</td>
                      <td>
                        <span style={{
                          background: 'rgba(167,139,250,0.1)',
                          color: 'var(--purple)',
                          padding: '3px 10px',
                          borderRadius: 20,
                          fontSize: 12,
                          fontWeight: 600,
                        }}>
                          {t.specialization}
                        </span>
                      </td>
                      <td>{t.experienceYears} yrs</td>
                      <td>
                        {t.sessionFee != null
                          ? <span style={{ color: 'var(--green)', fontWeight: 700 }}>₹{t.sessionFee}</span>
                          : <span style={{ color: 'var(--text3)', fontSize: 12 }}>Not set</span>
                        }
                      </td>
                      <td><StatusBadge status={t.status || 'ACTIVE'} /></td>
                      <td>
                        <div style={{ display: 'flex', gap: 8 }}>
                          <button
                            className="btn btn-ghost btn-sm"
                            onClick={() => setViewSchedule(t)}
                          >
                            Schedule
                          </button>
                          <button
                            className="btn btn-ghost btn-sm"
                            onClick={() => { setEditItem(t); setShowForm(true); }}
                          >
                            Edit
                          </button>
                          <button
                            className="btn btn-sm"
                            style={{
                              background: t.status === 'ACTIVE'
                                ? 'rgba(255,179,71,0.15)'
                                : 'rgba(0,229,160,0.15)',
                              color: t.status === 'ACTIVE'
                                ? 'var(--amber)'
                                : 'var(--green)',
                              border: `1px solid ${t.status === 'ACTIVE' ? 'var(--amber)' : 'var(--green)'}`,
                              padding: '4px 10px',
                              borderRadius: 6,
                              cursor: 'pointer',
                              fontSize: 12,
                              fontWeight: 600,
                            }}
                            disabled={togglingId === t.id}
                            onClick={() => toggleStatus(t)}
                          >
                            {togglingId === t.id
                              ? '…'
                              : t.status === 'ACTIVE'
                                ? 'Deactivate'
                                : 'Activate'}
                          </button>
                          <button
                            className="btn btn-danger btn-sm"
                            onClick={() => setConfirm(t)}
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {showForm && (
          <TrainerForm
            initial={editItem}
            onClose={() => setShowForm(false)}
            onSaved={() => { setShowForm(false); load(); }}
          />
        )}
        {confirm && (
          <ConfirmModal
            title="Remove Trainer"
            message={`Remove ${confirm.name} from the system?`}
            onClose={() => setConfirm(null)}
            onConfirm={async () => {
              await trainerApi.delete(confirm.id);
              setConfirm(null);
              load();
            }}
          />
        )}
        {viewSchedule && (
          <ScheduleModal trainer={viewSchedule} onClose={() => setViewSchedule(null)} />
        )}
      </div>
    </div>
  );
}

function TrainerForm({ initial, onClose, onSaved }) {
  const [form, setForm] = useState({
    name: '', email: '', phone: '', specialization: '', experienceYears: 0, status: 'ACTIVE',
    ...initial,
    sessionFee: initial?.sessionFee ?? '',
  });
  const [loading, setLoading] = useState(false);
  const [error,   setError]   = useState('');
  const set = k => e => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault(); setError(''); setLoading(true);
    try {
      const payload = {
        ...form,
        sessionFee: form.sessionFee !== '' ? parseFloat(form.sessionFee) : null,
      };
      if (initial) await trainerApi.update(initial.id, payload);
      else         await trainerApi.create(payload);
      onSaved();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save');
    } finally { setLoading(false); }
  };

  return (
    <Modal title={initial ? 'Edit Trainer' : 'Add Trainer'} onClose={onClose}>
      <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {error && <Alert type="error">{error}</Alert>}
        <div className="form-row">
          <FormGroup label="Full Name">
            <input value={form.name} onChange={set('name')} required />
          </FormGroup>
          <FormGroup label="Email">
            <input type="email" value={form.email} onChange={set('email')} required disabled={!!initial} />
          </FormGroup>
        </div>
        <div className="form-row">
          <FormGroup label="Phone">
            <input value={form.phone} onChange={set('phone')} />
          </FormGroup>
          <FormGroup label="Experience (years)">
            <input type="number" min="0" value={form.experienceYears} onChange={set('experienceYears')} />
          </FormGroup>
        </div>
        <div className="form-row">
          <FormGroup label="Specialization">
            <select value={form.specialization} onChange={set('specialization')} required>
              <option value="">Select…</option>
              {['Weight Loss','Yoga','Cardio','Strength Training','CrossFit','Pilates','Zumba','HIIT','Boxing','Swimming'].map(s => (
                <option key={s}>{s}</option>
              ))}
            </select>
          </FormGroup>
          <FormGroup label="Session Fee (₹)">
            <input
              type="number" min="0" step="0.01"
              value={form.sessionFee}
              onChange={set('sessionFee')}
              placeholder="e.g. 500"
            />
          </FormGroup>
        </div>
        <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 8 }}>
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Saving…' : initial ? 'Update' : 'Create'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

function ScheduleModal({ trainer, onClose }) {
  const [slots,   setSlots]   = useState([]);
  const [loading, setLoading] = useState(true);
  const [showAdd, setShowAdd] = useState(false);
  const [form,    setForm]    = useState({
    trainerId: trainer.id, dayOfWeek: 'MON', startTime: '09:00', endTime: '11:00', isBooked: false,
  });
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    trainerApi.getSchedule(trainer.id)
      .then(r => setSlots(r.data))
      .catch(() => setSlots([]))
      .finally(() => setLoading(false));
  }, [trainer.id]);

  const addSlot = async (e) => {
    e.preventDefault(); setSaving(true);
    try {
      await trainerApi.addSchedule(form);
      const r = await trainerApi.getSchedule(trainer.id);
      setSlots(r.data);
      setShowAdd(false);
    } catch {} finally { setSaving(false); }
  };

  const DAYS = ['MON','TUE','WED','THU','FRI','SAT','SUN'];

  return (
    <Modal title={`${trainer.name}'s Schedule`} onClose={onClose} size="modal-lg">
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'flex-end' }}>
        <button className="btn btn-primary btn-sm" onClick={() => setShowAdd(!showAdd)}>
          + Add Slot
        </button>
      </div>
      {showAdd && (
        <form
          onSubmit={addSlot}
          style={{
            display: 'grid', gridTemplateColumns: '1fr 1fr 1fr auto',
            gap: 12, alignItems: 'end', marginBottom: 20,
            padding: 16, background: 'var(--bg2)', borderRadius: 10,
          }}
        >
          <FormGroup label="Day">
            <select value={form.dayOfWeek} onChange={e => setForm({ ...form, dayOfWeek: e.target.value })}>
              {DAYS.map(d => <option key={d}>{d}</option>)}
            </select>
          </FormGroup>
          <FormGroup label="Start Time">
            <input type="time" value={form.startTime} onChange={e => setForm({ ...form, startTime: e.target.value })} />
          </FormGroup>
          <FormGroup label="End Time">
            <input type="time" value={form.endTime} onChange={e => setForm({ ...form, endTime: e.target.value })} />
          </FormGroup>
          <button type="submit" className="btn btn-success" disabled={saving}>
            {saving ? '…' : 'Add'}
          </button>
        </form>
      )}
      {loading ? <LoadingCenter /> : slots.length === 0 ? (
        <EmptyState icon="🗓️" text="No schedule slots yet" />
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>Day</th><th>Time</th><th>Status</th></tr>
            </thead>
            <tbody>
              {slots.map(s => (
                <tr key={s.id}>
                  <td style={{ fontWeight: 600, color: 'var(--text)' }}>{s.dayOfWeek}</td>
                  <td>{s.startTime} – {s.endTime}</td>
                  <td>
                    <span className={`badge ${s.booked ? 'badge-red' : 'badge-green'}`}>
                      {s.booked ? 'BOOKED' : 'AVAILABLE'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Modal>
  );
}
