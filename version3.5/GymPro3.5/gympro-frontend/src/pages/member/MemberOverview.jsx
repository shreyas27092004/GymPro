// src/pages/member/MemberOverview.jsx
// Member-facing landing dashboard — mirrors AdminOverview's stat-card +
// quick-action layout so both roles share one visual language.

import { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import MemberProfileGate from '../../components/MemberProfileGate';
import { bookingApi, planApi, paymentApi } from '../../api/api';
import { SkeletonAdminOverview } from '../../components/Skeleton';
import Icon from '../../components/icons';

export default function MemberOverview({ onNav }) {
  const { email } = useAuth();
  return (
    <div className="fade-in">
      <div className="content-header">
        <div className="page-title">OVERVIEW</div>
        <div className="page-subtitle">Welcome back — here's your membership at a glance</div>
      </div>
      <MemberProfileGate email={email}>
        {(member) => <OverviewContent member={member} onNav={onNav} />}
      </MemberProfileGate>
    </div>
  );
}

function OverviewContent({ member, onNav }) {
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
        const [subsRes, bookingsRes, paymentsRes, freeRes] = await Promise.allSettled([
          planApi.getMySubscriptions(member.id),
          bookingApi.getByMember(member.id),
          paymentApi.getMyPayments(member.id),
          planApi.checkFreeSession(member.id),
        ]);
        if (cancelled) return;
        const allFailed = [subsRes, bookingsRes, paymentsRes].every(r => r.status === 'rejected');
        if (allFailed) {
          setStatsError(true);
        } else {
          const subs     = subsRes.status     === 'fulfilled' ? subsRes.value.data     : [];
          const bookings = bookingsRes.status === 'fulfilled' ? bookingsRes.value.data : [];
          const payments = paymentsRes.status === 'fulfilled' ? paymentsRes.value.data : [];
          const free      = freeRes.status    === 'fulfilled' ? freeRes.value.data     : null;

          const activeSub = subs.find(s => s.status !== 'CANCELLED');
          const totalPaid = payments
            .filter(p => p.status === 'SUCCESS')
            .reduce((sum, p) => sum + (p.amount || 0), 0);

          setStats({
            hasActivePlan:     !!activeSub,
            upcomingBookings:  bookings.filter(b => b.status === 'CONFIRMED').length,
            totalPaid,
            freeSessions:      free?.remainingFreeSessions,
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
  }, [member.id]);

  const QUICK_ACTIONS = [
    { icon: 'clipboard', label: 'My Plan',    desc: 'View or subscribe to a membership plan', color: 'var(--accent)', key: 'plans'    },
    { icon: 'calendar',  label: 'Bookings',   desc: 'Book and manage training sessions',       color: 'var(--purple)', key: 'bookings' },
    { icon: 'dumbbell',  label: 'Trainers',   desc: 'Browse available trainers',                color: 'var(--green)',  key: 'trainers' },
    { icon: 'card',      label: 'Payments',   desc: 'View your billing history',                color: 'var(--amber)',  key: 'payments' },
    { icon: 'user',      label: 'My Profile', desc: 'Update your personal information',          color: 'var(--red)',    key: 'profile'  },
  ];

  const STAT_CARDS = [
    {
      label: 'Plan Status',
      value: stats ? (stats.hasActivePlan ? 'Active' : 'None') : undefined,
      color: stats?.hasActivePlan ? 'var(--green)' : 'var(--amber)',
      icon: 'clipboard',
    },
    {
      label: 'Upcoming Bookings',
      value: stats?.upcomingBookings,
      color: 'var(--accent)', icon: 'calendar',
    },
    {
      label: 'Total Paid',
      value: stats ? `\u20b9${stats.totalPaid.toFixed(0)}` : undefined,
      color: 'var(--green)', icon: 'card',
    },
    {
      label: 'Free Sessions Left',
      value: stats?.freeSessions === -1 ? '\u221e' : stats?.freeSessions,
      color: 'var(--purple)', icon: 'ticket',
    },
  ];

  return (
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
                {s.value != null ? s.value : statsError ? '\u2014' : '\u2026'}
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
