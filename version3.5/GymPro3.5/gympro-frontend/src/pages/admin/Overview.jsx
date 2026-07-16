// src/pages/admin/Overview.jsx

import { useState, useEffect } from 'react';
import { memberApi, trainerApi, planApi, paymentApi } from '../../api/api';
import { SkeletonAdminOverview } from '../../components/Skeleton';
import Icon from '../../components/icons';

export default function AdminOverview({ onNav }) {
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
        const [membersRes, trainersRes, plansRes, paymentsRes] = await Promise.allSettled([
          memberApi.getAll(),
          trainerApi.getAll(),
          planApi.getAll(),
          paymentApi.getAll(),
        ]);
        if (cancelled) return;
        const allFailed = [membersRes, trainersRes, plansRes, paymentsRes].every(r => r.status === 'rejected');
        if (allFailed) {
          setStatsError(true);
        } else {
          const members  = membersRes.status  === 'fulfilled' ? membersRes.value.data  : [];
          const trainers = trainersRes.status === 'fulfilled' ? trainersRes.value.data : [];
          const plans    = plansRes.status    === 'fulfilled' ? plansRes.value.data    : [];
          const payments = paymentsRes.status === 'fulfilled' ? paymentsRes.value.data : [];
          setStats({
            members:       members.length,
            activeMembers: members.filter(m => m.status === 'ACTIVE').length,
            trainers:      trainers.length,
            plans:         plans.length,
            payments:      payments.length,
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
  }, []);

  const QUICK_ACTIONS = [
    { icon: 'users',    label: 'Members',  desc: 'View & manage all gym members',   color: 'var(--accent)',  key: 'members'  },
    { icon: 'dumbbell', label: 'Trainers', desc: 'Assign and manage trainers',       color: 'var(--green)',   key: 'trainers' },
    { icon: 'clipboard',label: 'Plans',    desc: 'Create & update membership plans', color: 'var(--amber)',   key: 'plans'    },
    { icon: 'calendar', label: 'Bookings', desc: 'View all session bookings',        color: 'var(--purple)',  key: 'bookings' },
    { icon: 'card',     label: 'Payments', desc: 'Track payment transactions',       color: 'var(--red)',     key: 'payments' },
  ];

  const STAT_CARDS = [
    { label: 'Total Members',   value: stats?.members,       color: 'var(--accent)', icon: 'users' },
    { label: 'Active Members',  value: stats?.activeMembers, color: 'var(--green)',  icon: 'checkCircle' },
    { label: 'Trainers',        value: stats?.trainers,      color: 'var(--amber)',  icon: 'dumbbell' },
    { label: 'Plans Available', value: stats?.plans,         color: 'var(--purple)', icon: 'clipboard' },
    { label: 'Total Payments',  value: stats?.payments,      color: 'var(--red)',    icon: 'card' },
  ];

  return (
    <div className="fade-in">
      <div className="content-header">
        <div className="page-title">OVERVIEW</div>
        <div className="page-subtitle">Welcome back, Admin — here's your gym at a glance</div>
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
                <div className="stat-value" style={{ color: s.color }}>
                  {s.value != null ? s.value : statsError ? '—' : '…'}
                </div>
                <div className="stat-label">{s.label}</div>
              </div>
            ))}
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
};
