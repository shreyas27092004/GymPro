// src/pages/trainer/TrainerMembers.jsx
import { useState, useEffect } from 'react';
import { bookingApi, memberApi, trainerApi } from '../../api/api';
import { useAuth } from '../../context/AuthContext';
import { LoadingCenter, Alert, EmptyState, StatusBadge } from '../../components/UI';

export default function TrainerMembers() {
  const { email } = useAuth();
  const [members,  setMembers]  = useState([]);
  const [loading,  setLoading]  = useState(true);
  const [error,    setError]    = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        // Find trainer by email
        const allTrainers = await trainerApi.getAll();
        const me = allTrainers.data.find(t => t.email === email);
        if (!me) { setError('Trainer profile not found'); setLoading(false); return; }

        const bookingsRes = await bookingApi.getByTrainer(me.id);
        const bookings = bookingsRes.data.filter(b => b.status !== 'CANCELLED');

        // Get unique member IDs
        const memberIds = [...new Set(bookings.map(b => b.memberId))];
        const memberProfiles = await Promise.all(
          memberIds.map(id => memberApi.getById(id).then(r => r.data).catch(() => null))
        );

        // Enrich with booking info
        const enriched = memberProfiles.filter(Boolean).map(m => ({
          ...m,
          bookings: bookings.filter(b => b.memberId === m.id),
        }));
        setMembers(enriched);
      } catch (e) {
        setError('Failed to load members');
      } finally { setLoading(false); }
    };
    load();
  }, [email]);

  return (
    <div className="fade-in">
      <div className="content-header">
        <div className="page-title">MY MEMBERS</div>
        <div className="page-subtitle">Members assigned to your training sessions</div>
      </div>
      <div className="content-body">
        {error && <Alert type="error">{error}</Alert>}
        {loading ? <LoadingCenter /> : members.length === 0 ? (
          <div className="card"><EmptyState icon="👥" text="No members assigned yet" /></div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))', gap: 16 }}>
            {members.map(m => (
              <div key={m.id} className="card fade-in">
                <div style={{ display: 'flex', justify: 'space-between', alignItems: 'flex-start', marginBottom: 14 }}>
                  <div style={{ width: 44, height: 44, borderRadius: '50%', background: 'rgba(0,212,255,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 18, fontWeight: 700, color: 'var(--accent)' }}>
                    {m.name?.charAt(0).toUpperCase()}
                  </div>
                  <StatusBadge status={m.status || 'ACTIVE'} />
                </div>
                <div style={{ fontWeight: 700, color: 'var(--text)', marginBottom: 4 }}>{m.name}</div>
                <div style={{ fontSize: 12, color: 'var(--text3)', marginBottom: 12 }}>{m.email}</div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
                  {[
                    { label: 'Sessions', value: m.bookings.length },
                    { label: 'Confirmed', value: m.bookings.filter(b => b.status === 'CONFIRMED').length },
                    { label: 'Completed', value: m.bookings.filter(b => b.status === 'COMPLETED').length },
                    { label: 'Phone', value: m.phone || '—' },
                  ].map(s => (
                    <div key={s.label} style={{ background: 'var(--bg2)', borderRadius: 8, padding: '8px 10px' }}>
                      <div style={{ fontSize: 16, fontWeight: 700, color: 'var(--text)' }}>{s.value}</div>
                      <div style={{ fontSize: 10, color: 'var(--text3)', textTransform: 'uppercase', letterSpacing: 0.5 }}>{s.label}</div>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
