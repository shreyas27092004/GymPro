// src/pages/member/MemberTrainers.jsx
import { useState, useEffect } from 'react';
import { trainerApi } from '../../api/api';
import { LoadingCenter, Alert, EmptyState } from '../../components/UI';
import Icon from '../../components/icons';

export default function MemberTrainers() {
  const [trainers, setTrainers] = useState([]);
  const [loading,  setLoading]  = useState(true);
  const [error,    setError]    = useState('');
  const [search,   setSearch]   = useState('');
  const [selected, setSelected] = useState(null);

  useEffect(() => {
    trainerApi.getAll()
      .then(r => setTrainers(r.data))
      .catch(() => setError('Failed to load trainers'))
      .finally(() => setLoading(false));
  }, []);

  const filtered = trainers.filter(t =>
    t.name?.toLowerCase().includes(search.toLowerCase()) ||
    t.specialization?.toLowerCase().includes(search.toLowerCase())
  );

  const SPEC_COLORS = {
    'Weight Loss': 'var(--accent)', Yoga: 'var(--purple)', Cardio: 'var(--green)',
    'Strength Training': 'var(--amber)', CrossFit: 'var(--red)', Pilates: 'var(--pink)',
    Zumba: 'var(--pink)', HIIT: 'var(--red)', Boxing: 'var(--amber)', Swimming: 'var(--accent)',
  };

  return (
    <div className="fade-in">
      <div className="content-header">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
          <div>
            <div className="page-title">TRAINERS</div>
            <div className="page-subtitle">Browse available trainers and book a session</div>
          </div>
          <input
            placeholder="Search trainers…"
            value={search} onChange={e => setSearch(e.target.value)}
            style={{ width: 220, marginBottom: 2 }}
          />
        </div>
      </div>
      <div className="content-body">
        {error && <Alert type="error">{error}</Alert>}
        {loading ? <LoadingCenter /> : filtered.length === 0 ? (
          <EmptyState icon={<Icon name="dumbbell" size={20} style={{ color: 'var(--text3)' }} />} text="No trainers found" />
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))', gap: 16 }}>
            {filtered.map(t => {
              const specColor = SPEC_COLORS[t.specialization] || 'var(--accent)';
              return (
                <div
                  key={t.id} className="card fade-in"
                  style={{ cursor: 'pointer', transition: 'all 0.2s' }}
                  onClick={() => setSelected(t === selected ? null : t)}
                  onMouseOver={e => e.currentTarget.style.borderColor = specColor}
                  onMouseOut={e => e.currentTarget.style.borderColor = 'var(--border)'}
                >
                  {/* Avatar */}
                  <div style={{
                    width: 56, height: 56, borderRadius: '50%',
                    background: `${specColor}22`,
                    border: `2px solid ${specColor}44`,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: 22, fontWeight: 700, color: specColor,
                    marginBottom: 14,
                  }}>
                    {t.name?.charAt(0).toUpperCase()}
                  </div>

                  <div style={{ fontWeight: 700, color: 'var(--text)', fontSize: 16, marginBottom: 2 }}>{t.name}</div>
                  <div style={{ fontSize: 12, color: 'var(--text3)', marginBottom: 12 }}>{t.email}</div>

                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginBottom: 12 }}>
                    <span style={{ background: `${specColor}18`, color: specColor, padding: '3px 10px', borderRadius: 20, fontSize: 11, fontWeight: 700 }}>
                      {t.specialization}
                    </span>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: t.sessionFee != null ? 10 : 0 }}>
                    <div style={{ fontSize: 12, color: 'var(--text3)' }}>
                      <span style={{ color: 'var(--text)', fontWeight: 600 }}>{t.experienceYears}</span> yrs experience
                    </div>
                    <span className={`badge ${t.status === 'ACTIVE' ? 'badge-green' : 'badge-red'}`}>{t.status || 'ACTIVE'}</span>
                  </div>

                  {/* Session fee */}
                  {t.sessionFee != null && (
                    <div style={{
                      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                      background: 'rgba(0,229,160,0.07)', border: '1px solid rgba(0,229,160,0.2)',
                      borderRadius: 8, padding: '6px 10px',
                    }}>
                      <span style={{ fontSize: 11, color: 'var(--text3)', fontWeight: 600 }}>Session Fee</span>
                      <span style={{ fontSize: 14, fontWeight: 700, color: 'var(--green)' }}>₹{t.sessionFee}</span>
                    </div>
                  )}

                  {selected?.id === t.id && (
                    <TrainerScheduleInline trainerId={t.id} />
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

function TrainerScheduleInline({ trainerId }) {
  const [slots,   setSlots]   = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    trainerApi.getAvailableSlots(trainerId)
      .then(r => setSlots(r.data))
      .catch(() => setSlots([]))
      .finally(() => setLoading(false));
  }, [trainerId]);

  return (
    <div style={{ marginTop: 16, paddingTop: 16, borderTop: '1px solid var(--border)' }}>
      <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--text3)', letterSpacing: 1, textTransform: 'uppercase', marginBottom: 8 }}>
        Available Slots
      </div>
      {loading ? (
        <div style={{ fontSize: 12, color: 'var(--text3)' }}>Loading…</div>
      ) : slots.length === 0 ? (
        <div style={{ fontSize: 12, color: 'var(--text3)' }}>No available slots</div>
      ) : (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
          {slots.map(s => (
            <span key={s.id} style={{
              background: 'rgba(0,229,160,0.1)', color: 'var(--green)',
              border: '1px solid rgba(0,229,160,0.25)',
              padding: '3px 8px', borderRadius: 6, fontSize: 11, fontWeight: 600,
            }}>
              {s.dayOfWeek} {s.startTime}
            </span>
          ))}
        </div>
      )}
    </div>
  );
}
