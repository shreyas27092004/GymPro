// src/pages/admin/AdminProfile.jsx
// Admin profile page — shows logged-in admin's account info.
// Admins are stored only in auth-service (no separate profile table),
// so this is a read-only view of session data with a name-edit hint.

import { useAuth } from '../../context/AuthContext';

export default function AdminProfile() {
  const { email, name, role } = useAuth();

  const avatarLetter = (name || email || 'A')[0].toUpperCase();

  return (
    <div className="fade-in">
      <div className="content-header">
        <div className="page-title">MY PROFILE</div>
        <div className="page-subtitle">Admin account information</div>
      </div>

      <div className="content-body">
        <div className="card" style={{ maxWidth: 560 }}>
          {/* Avatar + name */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 20, marginBottom: 28 }}>
            <div style={{
              width: 72, height: 72, borderRadius: '50%',
              background: 'rgba(255,179,71,0.15)', color: 'var(--amber)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 28, fontWeight: 700, flexShrink: 0,
            }}>
              {avatarLetter}
            </div>
            <div>
              <div style={{ fontSize: 22, fontWeight: 700, color: 'var(--text)' }}>
                {name || 'Admin'}
              </div>
              <div style={{ color: 'var(--amber)', fontSize: 13, fontWeight: 600, marginTop: 2 }}>
                ADMINISTRATOR
              </div>
            </div>
          </div>

          {/* Info */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 18 }}>
            <InfoRow label="Email"  value={email || '—'} />
            <InfoRow label="Role"   value={role  || 'ADMIN'}
              valueStyle={{ color: 'var(--amber)', fontWeight: 600 }}
            />
            <InfoRow label="Name"   value={name  || '—'} />
            <InfoRow label="Access" value="Full System Access"
              valueStyle={{ color: 'var(--green)', fontWeight: 600 }}
            />
          </div>

          <div style={{
            marginTop: 28, padding: '14px 16px',
            background: 'rgba(255,179,71,0.08)', borderRadius: 10,
            border: '1px solid rgba(255,179,71,0.2)',
            fontSize: 13, color: 'var(--text-muted)',
          }}>
            ℹ️ Admin account details are managed directly in the auth-service database.
            To change your password or email, update the record in <code>gympro_auth.users</code>.
          </div>
        </div>
      </div>
    </div>
  );
}

function InfoRow({ label, value, valueStyle = {} }) {
  return (
    <div>
      <div style={{ fontSize: 11, fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 4 }}>
        {label}
      </div>
      <div style={{ fontSize: 15, color: 'var(--text)', ...valueStyle }}>{value}</div>
    </div>
  );
}
