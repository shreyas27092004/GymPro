// src/pages/trainer/TrainerProfile.jsx
// Lets a logged-in trainer view and edit their own profile, including session fee.

import { useState } from 'react';
import { trainerApi } from '../../api/api';
import { useAuth } from '../../context/AuthContext';
import TrainerProfileGate from '../../components/TrainerProfileGate';
import { Alert, FormGroup, Modal } from '../../components/UI';

const SPECIALIZATIONS = [
  'Weight Loss','Yoga','Cardio','Strength Training',
  'CrossFit','Pilates','Zumba','HIIT','Boxing','Swimming','General',
];

export default function TrainerProfile() {
  const { email } = useAuth();

  return (
    <TrainerProfileGate email={email}>
      {(trainer, reload) => <ProfileCard trainer={trainer} reload={reload} />}
    </TrainerProfileGate>
  );
}

function ProfileCard({ trainer, reload }) {
  const [showEdit, setShowEdit] = useState(false);

  const avatarLetter = (trainer.name || trainer.email || 'T')[0].toUpperCase();

  return (
    <div className="fade-in">
      <div className="content-header">
        <div className="page-title">MY PROFILE</div>
        <div className="page-subtitle">View and update your trainer information</div>
      </div>

      <div className="content-body">
        <div className="card" style={{ maxWidth: 640 }}>
          {/* Avatar + name */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 20, marginBottom: 28 }}>
            <div style={{
              width: 72, height: 72, borderRadius: '50%',
              background: 'rgba(0,229,160,0.15)', color: 'var(--green)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 28, fontWeight: 700, flexShrink: 0,
            }}>
              {avatarLetter}
            </div>
            <div>
              <div style={{ fontSize: 22, fontWeight: 700, color: 'var(--text)' }}>{trainer.name}</div>
              <div style={{ color: 'var(--green)', fontSize: 13, fontWeight: 600, marginTop: 2 }}>TRAINER</div>
            </div>
          </div>

          {/* Info grid */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 18, marginBottom: 28 }}>
            <InfoRow label="Email"          value={trainer.email} />
            <InfoRow label="Phone"          value={trainer.phone || '—'} />
            <InfoRow label="Specialization" value={trainer.specialization || '—'}
              valueStyle={{ color: 'var(--purple)', fontWeight: 600 }}
            />
            <InfoRow label="Experience"
              value={trainer.experienceYears != null ? `${trainer.experienceYears} years` : '—'}
            />
            <InfoRow label="Status" value={trainer.status || 'ACTIVE'}
              valueStyle={{
                color: trainer.status === 'INACTIVE' ? 'var(--red)' : 'var(--green)',
                fontWeight: 600,
              }}
            />
            <InfoRow
              label="Session Fee"
              value={trainer.sessionFee != null ? `₹${trainer.sessionFee} / session` : 'Not set'}
              valueStyle={{
                color: trainer.sessionFee != null ? 'var(--accent)' : 'var(--text3)',
                fontWeight: 600,
              }}
            />
          </div>

          {/* Warning if no fee set */}
          {trainer.sessionFee == null && (
            <div style={{
              background: 'rgba(255,179,71,0.08)', border: '1px solid rgba(255,179,71,0.3)',
              borderRadius: 10, padding: '10px 14px', fontSize: 13, color: 'var(--amber)',
              marginBottom: 20,
            }}>
              ⚠️ You haven't set a session fee yet. Members cannot complete paid bookings with you until you configure one.
            </div>
          )}

          <button
            className="btn btn-primary"
            style={{ width: 'fit-content' }}
            onClick={() => setShowEdit(true)}
          >
            ✏️ Edit Profile
          </button>
        </div>
      </div>

      {showEdit && (
        <EditTrainerModal
          trainer={trainer}
          onClose={() => setShowEdit(false)}
          onSaved={() => {
            sessionStorage.removeItem('trainerProfile');
            setShowEdit(false);
            reload();
          }}
        />
      )}
    </div>
  );
}

function EditTrainerModal({ trainer, onClose, onSaved }) {
  const [form, setForm] = useState({
    name:            trainer.name            || '',
    phone:           trainer.phone           || '',
    specialization:  trainer.specialization  || '',
    experienceYears: trainer.experienceYears ?? 0,
    status:          trainer.status          || 'ACTIVE',
    sessionFee:      trainer.sessionFee      != null ? String(trainer.sessionFee) : '',
  });
  const [saving,  setSaving]  = useState(false);
  const [error,   setError]   = useState('');
  const [success, setSuccess] = useState('');

  const handleChange = (field) => (e) => {
    setForm(f => ({ ...f, [field]: e.target.value }));
    setError('');
  };

  // Validate session fee
  const validateFee = () => {
    const raw = form.sessionFee.trim();
    if (raw === '') return null; // optional — no fee provided
    const num = parseFloat(raw);
    if (isNaN(num) || num <= 0) {
      setError('Session fee must be a number greater than 0 (e.g. 500)');
      return false;
    }
    if (num > 100000) {
      setError('Session fee cannot exceed ₹1,00,000');
      return false;
    }
    return num;
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    // Validate fee if provided
    const feeRaw = form.sessionFee.trim();
    if (feeRaw !== '') {
      const fee = validateFee();
      if (fee === false) return; // validation failed
    }

    setSaving(true);
    try {
      const payload = {
        name:            form.name,
        phone:           form.phone,
        specialization:  form.specialization,
        experienceYears: parseInt(form.experienceYears, 10) || 0,
        status:          form.status,
        // Only include sessionFee if the user typed something
        sessionFee:      feeRaw !== '' ? parseFloat(feeRaw) : trainer.sessionFee,
      };

      await trainerApi.update(trainer.id, payload);
      setSuccess('Profile updated successfully!');
      setTimeout(onSaved, 800);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save profile. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  // Quick fee-only update shortcut
  const handleFeeOnly = async () => {
    const fee = validateFee();
    if (!fee) return;
    setSaving(true);
    setError('');
    try {
      await trainerApi.updateSessionFee(trainer.id, fee);
      setSuccess('Session fee updated!');
      setTimeout(onSaved, 800);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update session fee.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal title="Edit Profile" onClose={onClose}>
      <form onSubmit={handleSave} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
        {error   && <div className="alert alert-error">{error}</div>}
        {success && <div className="alert alert-success">{success}</div>}

        <FormGroup label="Name">
          <input value={form.name} onChange={handleChange('name')} placeholder="Full name" required />
        </FormGroup>

        <FormGroup label="Phone">
          <input value={form.phone} onChange={handleChange('phone')} placeholder="+91 98765 43210" />
        </FormGroup>

        <FormGroup label="Specialization">
          <select value={form.specialization} onChange={handleChange('specialization')}>
            <option value="">Select…</option>
            {SPECIALIZATIONS.map(s => <option key={s} value={s}>{s}</option>)}
          </select>
        </FormGroup>

        <FormGroup label="Experience (years)">
          <input
            type="number" min="0" max="50"
            value={form.experienceYears}
            onChange={handleChange('experienceYears')}
          />
        </FormGroup>

        <FormGroup label="Status">
          <select value={form.status} onChange={handleChange('status')}>
            <option value="ACTIVE">ACTIVE</option>
            <option value="INACTIVE">INACTIVE</option>
          </select>
        </FormGroup>

        {/* Session fee with validation hint */}
        <FormGroup label="Session Fee (₹)">
          <div style={{ display: 'flex', gap: 8 }}>
            <input
              type="number"
              min="1"
              max="100000"
              step="0.01"
              value={form.sessionFee}
              onChange={handleChange('sessionFee')}
              placeholder="e.g. 500"
              style={{ flex: 1 }}
            />
            
          </div>
          <div style={{ fontSize: 11, color: 'var(--text3)', marginTop: 4 }}>
            This amount will be shown to members when browsing trainers.
            Leave blank to keep the existing fee.
          </div>
        </FormGroup>

        <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 4 }}>
          <button type="button" className="btn btn-ghost" onClick={onClose} disabled={saving}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? 'Saving…' : 'Save Changes'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

function InfoRow({ label, value, valueStyle = {} }) {
  return (
    <div>
      <div style={{ fontSize: 11, color: 'var(--text3)', fontWeight: 600,
                    letterSpacing: 0.5, textTransform: 'uppercase', marginBottom: 4 }}>
        {label}
      </div>
      <div style={{ fontSize: 15, color: 'var(--text)', ...valueStyle }}>{value}</div>
    </div>
  );
}
