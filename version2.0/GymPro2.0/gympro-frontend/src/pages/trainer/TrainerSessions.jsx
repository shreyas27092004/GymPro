// src/pages/trainer/TrainerSessions.jsx
import { useState, useEffect } from 'react';
import { bookingApi, trainerApi } from '../../api/api';
import { useAuth } from '../../context/AuthContext';
import { LoadingCenter, Alert, StatusBadge, EmptyState, SectionHeader } from '../../components/UI';

export default function TrainerSessions() {
  const { email } = useAuth();
  const [bookings, setBookings] = useState([]);
  const [loading,  setLoading]  = useState(true);
  const [error,    setError]    = useState('');
  const [filter,   setFilter]   = useState('CONFIRMED');

  const load = async () => {
    try {
      setLoading(true);
      const allTrainers = await trainerApi.getAll();
      const me = allTrainers.data.find(t => t.email === email);
      if (!me) { setError('Trainer profile not found'); setLoading(false); return; }
      const res = await bookingApi.getByTrainer(me.id);
      setBookings(res.data.sort((a, b) => b.id - a.id));
    } catch { setError('Failed to load sessions'); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, [email]);

  const filtered = filter === 'ALL' ? bookings : bookings.filter(b => b.status === filter);

  const handleComplete = async (id) => {
    try { await bookingApi.complete(id); load(); }
    catch (e) { setError(e.response?.data?.message || 'Failed to complete'); }
  };

  return (
    <div className="fade-in">
      <div className="content-header">
        <div className="page-title">SESSIONS</div>
        <div className="page-subtitle">Manage your training sessions</div>
      </div>
      <div className="content-body">
        {error && <Alert type="error">{error}</Alert>}
        <div className="card">
          <SectionHeader title={`${filtered.length} Sessions`}>
            <div className="tabs" style={{ marginBottom: 0 }}>
              {['ALL','CONFIRMED','COMPLETED','CANCELLED'].map(s => (
                <button key={s} className={`tab ${filter === s ? 'active' : ''}`} onClick={() => setFilter(s)}>
                  {s === 'ALL' ? 'All' : s.charAt(0) + s.slice(1).toLowerCase()}
                </button>
              ))}
            </div>
          </SectionHeader>

          {loading ? <LoadingCenter /> : filtered.length === 0 ? (
            <EmptyState icon="📅" text="No sessions found" />
          ) : (
            <div className="table-wrap">
              <table>
                <thead><tr><th>Member</th><th>Day</th><th>Time</th><th>Date</th><th>Notes</th><th>Status</th><th></th></tr></thead>
                <tbody>
                  {filtered.map(b => (
                    <tr key={b.id}>
                      <td style={{ color: 'var(--text)', fontWeight: 600 }}>{b.memberEmail}</td>
                      <td><span style={{ background: 'var(--bg2)', padding: '2px 8px', borderRadius: 6, fontSize: 12, fontWeight: 700 }}>{b.sessionDay}</span></td>
                      <td style={{ fontSize: 13 }}>{b.sessionTime}</td>
                      <td style={{ fontSize: 13 }}>{b.bookingDate}</td>
                      <td style={{ fontSize: 12, color: 'var(--text3)' }}>{b.notes || '—'}</td>
                      <td><StatusBadge status={b.status} /></td>
                      <td>
                        {b.status === 'CONFIRMED' && (
                          <button className="btn btn-success btn-sm" onClick={() => handleComplete(b.id)}>Mark Done</button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
