// src/pages/member/MemberProfile.jsx
// Lets a logged-in member view and edit their own profile.

import { useState } from 'react';
import { memberApi } from '../../api/api';
import { useAuth } from '../../context/AuthContext';
import MemberProfileGate from '../../components/MemberProfileGate';
import { Alert, FormGroup, Modal } from '../../components/UI';

export default function MemberProfile() {
  const { email } = useAuth();

  return (
    <MemberProfileGate email={email}>
      {(member, reload) => <ProfileCard member={member} reload={reload} />}
    </MemberProfileGate>
  );
}

function ProfileCard({ member, reload }) {
  const [showEdit, setShowEdit] = useState(false);

  const avatarLetter = (member.name || member.email || 'M')[0].toUpperCase();

  return (
    <div className="fade-in">
      <div className="content-header">
        <div className="page-title">MY PROFILE</div>
        <div className="page-subtitle">View and update your personal information</div>
      </div>

      <div className="content-body">
        <div className="card" style={{ maxWidth: 640 }}>
          {/* Avatar + name */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 20, marginBottom: 28 }}>
            <div style={{
              width: 72, height: 72, borderRadius: '50%',
              background: 'rgba(0,212,255,0.15)', color: 'var(--accent)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 28, fontWeight: 700, flexShrink: 0,
            }}>
              {avatarLetter}
            </div>
            <div>
              <div style={{ fontSize: 22, fontWeight: 700, color: 'var(--text)' }}>{member.name}</div>
              <div style={{ color: 'var(--accent)', fontSize: 13, fontWeight: 600, marginTop: 2 }}>
                MEMBER
              </div>
            </div>
          </div>

          {/* Info grid */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 18, marginBottom: 28 }}>
            <InfoRow label="Email"   value={member.email}          />
            <InfoRow label="Phone"   value={member.phone || '—'}   />
            <InfoRow label="Gender"  value={member.gender || '—'}  />
            <InfoRow label="Status"  value={member.status || 'ACTIVE'}
              valueStyle={{ color: member.status === 'INACTIVE' ? 'var(--red)' : 'var(--green)', fontWeight: 600 }}
            />
            <InfoRow label="Address" value={member.address || '—'} style={{ gridColumn: '1 / -1' }} />
          </div>

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
        <EditMemberModal
          member={member}
          onClose={() => setShowEdit(false)}
          onSaved={() => {
            // Invalidate cache so MemberProfileGate re-fetches
            sessionStorage.removeItem('memberProfile');
            setShowEdit(false);
            reload();
          }}
        />
      )}
    </div>
  );
}

function InfoRow({ label, value, valueStyle = {}, style = {} }) {
  return (
    <div style={{ ...style }}>
      <div style={{ fontSize: 11, fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 4 }}>
        {label}
      </div>
      <div style={{ fontSize: 15, color: 'var(--text)', ...valueStyle }}>{value}</div>
    </div>
  );
}

function EditMemberModal({ member, onClose, onSaved }) {
  const [form, setForm] = useState({
    name:    member.name    || '',
    phone:   member.phone   || '',
    address: member.address || '',
    gender:  member.gender  || '',
    status:  member.status  || 'ACTIVE',
    email:   member.email,          // sent but not editable
  });
  const [loading, setLoading] = useState(false);
  const [error,   setError]   = useState('');

  const set = k => e => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError(''); setLoading(true);
    try {
      await memberApi.update(member.id, form);
      onSaved();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update profile');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal title="Edit My Profile" onClose={onClose}>
      <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {error && <Alert type="error">{error}</Alert>}

        <div className="form-row">
          <FormGroup label="Full Name">
            <input value={form.name} onChange={set('name')} required />
          </FormGroup>
          <FormGroup label="Email">
            <input value={form.email} disabled style={{ opacity: 0.5 }} />
          </FormGroup>
        </div>

        <div className="form-row">
          <FormGroup label="Phone">
            <input value={form.phone} onChange={set('phone')} />
          </FormGroup>
          <FormGroup label="Gender">
            <select value={form.gender} onChange={set('gender')}>
              <option value="">Select…</option>
              <option>Male</option>
              <option>Female</option>
              <option>Other</option>
            </select>
          </FormGroup>
        </div>

        <FormGroup label="Address">
          <input value={form.address} onChange={set('address')} />
        </FormGroup>

        <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 8 }}>
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Saving…' : 'Save Changes'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
