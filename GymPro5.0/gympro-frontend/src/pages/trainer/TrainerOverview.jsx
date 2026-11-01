// src/pages/trainer/TrainerOverview.jsx
// Trainer landing dashboard — mirrors AdminOverview's stat cards + quick
// access pattern, scoped to the logged-in trainer's own data.

import { useState, useEffect } from 'react';
import { trainerApi, bookingApi } from '../../api/api';
import { useAuth } from '../../context/AuthContext';
import { SkeletonAdminOverview } from '../../components/Skeleton';
import Icon from '../../components/icons';

export default function TrainerOverview({ onNav }) {
  const { email, name } = useAuth();
  const [stats,        setStats]        = useState(null);
  const [statsLoading, setStatsLoading] = useState(true);
  const [statsError,   setStatsError]   = useState(false);

  // ── Load stats ────────────────────────────────────────────────────────────
  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      setStatsLoading(true);
      setStatsError(false);
      try {
        const allTrainers = await trainerApi.getAll();
        const me = allTrainers.data.find(t => t.email === email);
        if (!me) { if (!cancelled) { setStatsError(true); setStatsLoading(false); } return; }

        const [bookingsRes, scheduleRes] = await Promise.allSettled([
          bookingApi.getByTrainer(me.id),
          trainerApi.getSchedule(me.id),
        ]);
        if (cancelled) return;

        const allFailed = [bookingsRes, scheduleRes].every(r => r.status === 'rejected');
        if (allFailed) {
          setStatsError(true);
        } else {
          const bookings = bookingsRes.status === 'fulfilled' ? bookingsRes.value.data : [];
          const schedule = scheduleRes.status === 'fulfilled' ? scheduleRes.value.data : [];
          const activeBookings = bookings.filter(b => b.status !== 'CANCELLED');
          const uniqueMembers = new Set(activeBookings.map(b => b.memberId)).size;

          setStats({
            members:      uniqueMembers,
            confirmed:    bookings.filter(b => b.status === 'CONFIRMED').length,
            completed:    bookings.filter(b => b.status === 'COMPLETED').length,
            freeSlots:    schedule.filter(s => s.available).length,
            sessionFee:   me.sessionFee,
          });
        }
      } catch {
        if (!cancelled) setStatsError(true);
      } finally {
        if (!cancelled) setStatsLoading(false);
      }
    };
    load();
    return () => { cancelled = true; };
  }, [email]);

  const QUICK_ACTIONS = [
    { icon: 'users',    label: 'My Members', desc: 'View members you train',       color: 'var(--accent)', key: 'members'  },
    { icon: 'calendar', label: 'Sessions',   desc: 'Manage your training sessions', color: 'var(--purple)', key: 'sessions' },
    { icon: 'clock',    label: 'Schedule',   desc: 'Set your weekly availability',  color: 'var(--green)',  key: 'schedule' },
    { icon: 'user',     label: 'Profile',    desc: 'Update your trainer details',   color: 'var(--amber)',  key: 'profile'  },
  ];

  const STAT_CARDS = [
    { label: 'My Members',        value: stats?.members,   color: 'var(--accent)', icon: 'users' },
    { label: 'Confirmed Sessions',value: stats?.confirmed, color: 'var(--purple)', icon: 'calendar' },
    { label: 'Completed Sessions',value: stats?.completed, color: 'var(--green)',  icon: 'checkCircle' },
    { label: 'Free Slots',        value: stats?.freeSlots, color: 'var(--amber)',  icon: 'clock' },
    {
      label: 'Session Fee',
      value: stats?.sessionFee != null ? `₹${stats.sessionFee}` : 'Not set',
      color: stats?.sessionFee != null ? 'var(--red)' : 'var(--text3)',
      icon: 'cash',
      raw: true,
    },
  ];

  return (
    <div className="fade-in">
      <div className="content-header">
        <div className="page-title">OVERVIEW</div>
        <div className="page-subtitle">Welcome back, {name || 'Trainer'} — here's your schedule at a glance</div>
      </div>

      <div className="content-body">

        {statsError && !statsLoading && (
          <div style={S.errorBanner}>
            <span style={{ color: 'var(--red)' }}><Icon name="alertTriangle" size={18} /></span>
            <div>
              <div style={{ fontWeight: 700, fontSize: 13, color: 'var(--red)' }}>Backend Unavailable</div>
              <div style={{ fontSize: 12, color: 'var(--text3)', marginTop: 2 }}>
                Could not reach the API gateway. Start your microservices and refresh.
              </div>
            </div>
            <button style={S.retryBtn} onClick={() => window.location.reload()}>Retry</button>
          </div>
        )}

        {statsLoading ? (
          <SkeletonAdminOverview />
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: 14, marginBottom: 28 }}>
            {STAT_CARDS.map(s => (
              <div key={s.label} className="stat-card">
                <div className="stat-accent-bar" style={{ background: s.color }} />
                <div className="stat-icon" style={{ color: s.color }}><Icon name={s.icon} size={16} /></div>
                <div className="stat-value" style={{ color: s.color, fontSize: s.raw ? 22 : undefined }}>
                  {s.value != null ? s.value : statsError ? '—' : '…'}
                </div>
                <div className="stat-label">{s.label}</div>
              </div>
            ))}
          </div>
        )}

        {!statsLoading && stats?.sessionFee == null && !statsError && (
          <div style={S.warnBanner}>
            <span style={{ color: 'var(--amber)' }}><Icon name="alertTriangle" size={16} /></span>
            <div style={{ fontSize: 13, color: 'var(--text2)' }}>
              You haven't set a session fee yet. Head to <strong>Profile</strong> to configure one so members can book paid sessions with you.
            </div>
          </div>
        )}

        <div style={{ marginBottom: 24 }}>
          <div style={S.sectionLabel}>Quick Access</div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: 12 }}>
            {QUICK_ACTIONS.map(a => <QuickActionButton key={a.key} action={a} onNav={onNav} />)}
          </div>
        </div>

      </div>
    </div>
  );
}

function QuickActionButton({ action: a, onNav }) {
  const [hovered, setHovered] = useState(false);
  return (
    <button
      onClick={() => onNav(a.key)}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        background: 'var(--bg3)',
        border: `1px solid ${hovered ? a.color : 'var(--border)'}`,
        borderRadius: 14, padding: '18px 14px', cursor: 'pointer',
        textAlign: 'left', fontFamily: 'var(--font-body)',
        transform: hovered ? 'translateY(-2px)' : 'none',
        transition: 'all 0.2s',
      }}
    >
      <div style={{ color: a.color, marginBottom: 8 }}><Icon name={a.icon} size={22} /></div>
      <div style={{ fontWeight: 700, color: a.color, fontSize: 12, marginBottom: 3 }}>{a.label}</div>
      <div style={{ fontSize: 11, color: 'var(--text3)', lineHeight: 1.4 }}>{a.desc}</div>
    </button>
  );
}

const S = {
  sectionLabel: { fontSize: 11, fontWeight: 700, color: 'var(--text3)', letterSpacing: '1.5px', textTransform: 'uppercase', marginBottom: 12 },
  errorBanner:  { display: 'flex', alignItems: 'flex-start', gap: 12, background: 'var(--red-wash)', border: '1px solid rgba(214,48,74,0.25)', borderRadius: 12, padding: '14px 16px', marginBottom: 20 },
  retryBtn:     { marginLeft: 'auto', background: 'var(--red-wash)', border: '1px solid rgba(214,48,74,0.3)', borderRadius: 8, padding: '5px 12px', color: 'var(--red)', fontSize: 12, fontWeight: 600, cursor: 'pointer', fontFamily: 'var(--font-body)' },
  warnBanner:   { display: 'flex', alignItems: 'center', gap: 10, background: 'rgba(255,179,71,0.08)', border: '1px solid rgba(255,179,71,0.25)', borderRadius: 12, padding: '12px 16px', marginBottom: 24 },
};
