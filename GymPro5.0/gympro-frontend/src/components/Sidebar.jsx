// src/components/Sidebar.jsx
// Design pass: consistent 18px stroke-icon set (replaces mixed emoji), refined
// logo mark, tighter spacing. Navigation logic, profile-ID resolution, and
// notification wiring are unchanged from the prior version.

import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import NotificationBell from './NotificationBell';
import ThemeToggle from '../theme/ThemeToggle';
import { resolveMemberProfile } from '../api/memberProfile';
import { resolveTrainerProfile } from '../api/trainerProfile';

// ── Icon set — single stroke style, 18x18, currentColor ──────────────────
const ICONS = {
  home: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 11.5 12 4l9 7.5" /><path d="M5.5 10v9a1 1 0 0 0 1 1h4.5v-6h2v6H17.5a1 1 0 0 0 1-1v-9" />
    </svg>
  ),
  users: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="9" cy="8" r="3.2" /><path d="M2.5 20c.7-3.4 3.2-5.5 6.5-5.5s5.8 2.1 6.5 5.5" />
      <path d="M16 8.2a3.2 3.2 0 1 1 3 4.2" /><path d="M15.5 14.7c2.6.4 4.5 2.2 5 5.3" />
    </svg>
  ),
  dumbbell: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M6.5 9v6" /><path d="M17.5 9v6" /><path d="M3 10.5v3" /><path d="M21 10.5v3" />
      <path d="M6.5 12h11" />
    </svg>
  ),
  clipboard: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <rect x="5.5" y="4.5" width="13" height="16" rx="2" /><path d="M9 4.5V3.8A1.3 1.3 0 0 1 10.3 2.5h3.4A1.3 1.3 0 0 1 15 3.8v.7" />
      <path d="M8.5 11h7" /><path d="M8.5 14.5h7" /><path d="M8.5 18h4.5" />
    </svg>
  ),
  calendar: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3.5" y="5" width="17" height="15.5" rx="2" /><path d="M3.5 9.5h17" /><path d="M8 3v4" /><path d="M16 3v4" />
    </svg>
  ),
  card: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="5.5" width="18" height="13" rx="2" /><path d="M3 9.5h18" /><path d="M6.5 14.5h4" />
    </svg>
  ),
  clock: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="8.5" /><path d="M12 7.5V12l3 2" />
    </svg>
  ),
  user: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="8.3" r="3.5" /><path d="M4.5 20.5c1-4 3.7-6.3 7.5-6.3s6.5 2.3 7.5 6.3" />
    </svg>
  ),
  logout: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M9 20.5H5.5a1.5 1.5 0 0 1-1.5-1.5v-14a1.5 1.5 0 0 1 1.5-1.5H9" /><path d="M16 16.5 21 12l-5-4.5" /><path d="M21 12H9" />
    </svg>
  ),
};

const ADMIN_NAV = [
  { section: 'Dashboard' },
  { key: 'overview',  icon: 'home',      label: 'Overview' },
  { section: 'Manage' },
  { key: 'members',   icon: 'users',     label: 'Members' },
  { key: 'trainers',  icon: 'dumbbell',  label: 'Trainers' },
  { key: 'plans',     icon: 'clipboard', label: 'Plans' },
  { section: 'Operations' },
  { key: 'bookings',  icon: 'calendar',  label: 'Bookings' },
  { key: 'payments',  icon: 'card',      label: 'Payments' },
];

const TRAINER_NAV = [
  { section: 'Dashboard' },
  { key: 'overview',  icon: 'home',      label: 'Overview' },
  { section: 'My Work' },
  { key: 'members',   icon: 'users',     label: 'My Members' },
  { key: 'sessions',  icon: 'calendar',  label: 'Sessions' },
  { key: 'schedule',  icon: 'clock',     label: 'My Schedule' },
  { section: 'Account' },
  { key: 'profile',   icon: 'user',      label: 'My Profile' },
];

const MEMBER_NAV = [
  { section: 'Dashboard' },
  { key: 'overview',  icon: 'home',      label: 'Overview' },
  { section: 'My Account' },
  { key: 'plans',     icon: 'clipboard', label: 'My Plan' },
  { key: 'bookings',  icon: 'calendar',  label: 'My Bookings' },
  { key: 'trainers',  icon: 'dumbbell',  label: 'Trainers' },
  { key: 'payments',  icon: 'card',      label: 'Payments' },
  { section: 'Account' },
  { key: 'profile',   icon: 'user',      label: 'My Profile' },
];

const ROLE_COLORS = {
  ADMIN:   { bg: 'var(--amber-wash)',  color: 'var(--amber)' },
  TRAINER: { bg: 'var(--green-wash)',  color: 'var(--green)' },
  MEMBER:  { bg: 'var(--accent-wash)', color: 'var(--accent)' },
};

export default function Sidebar({ active, onNav }) {
  const { role, logout, email, name, userId } = useAuth();
  const navigate = useNavigate();

  const navItems = role === 'ADMIN' ? ADMIN_NAV : role === 'TRAINER' ? TRAINER_NAV : MEMBER_NAV;
  const rc = ROLE_COLORS[role] || ROLE_COLORS.MEMBER;

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const displayName = name || email || 'User';
  const initials = displayName === (email || '')
    ? displayName.slice(0, 2).toUpperCase()
    : displayName.trim().split(/\s+/).map(w => w[0]).slice(0, 2).join('').toUpperCase();

  // ── Eagerly resolve the member/trainer profile ID ─────────────────────
  // TrainerProfileGate / MemberProfileGate (which cache trainerProfile /
  // memberProfile in sessionStorage) only mount when the user visits their
  // "My Profile" tab. TRAINER's default tab is "members", so without this,
  // a trainer who never opens Profile would have its notification bell
  // permanently fall back to the auth userId — a different ID space than
  // trainer-service's Trainer.id, which is what booking-service publishes
  // notification events with. Resolving it here, on mount, regardless of
  // the active tab, keeps the bell's userId correct for every role.
  const [resolvedProfileId, setResolvedProfileId] = useState(null);

  useEffect(() => {
    let cancelled = false;
    if (role === 'MEMBER' && email) {
      resolveMemberProfile(email)
        .then(p => { if (!cancelled && p?.id) setResolvedProfileId(Number(p.id)); })
        .catch(() => {});
    } else if (role === 'TRAINER' && email) {
      resolveTrainerProfile(email)
        .then(p => { if (!cancelled && p?.id) setResolvedProfileId(Number(p.id)); })
        .catch(() => {});
    }
    return () => { cancelled = true; };
  }, [role, email]);

  /**
   * Resolve the notification userId:
   *  - MEMBER: use the stored memberProfile.id (DB ID from member-service)
   *            Fall back to the auth userId if profile not yet loaded.
   *  - TRAINER: use stored trainerProfile.id
   *  - ADMIN: use auth userId directly (admin notifications use auth userId)
   */
  const resolveNotificationUserId = () => {
    if (resolvedProfileId) return resolvedProfileId;
    try {
      if (role === 'MEMBER') {
        const p = JSON.parse(sessionStorage.getItem('memberProfile') || '{}');
        return p?.id ? Number(p.id) : (userId ? Number(userId) : null);
      }
      if (role === 'TRAINER') {
        const p = JSON.parse(sessionStorage.getItem('trainerProfile') || '{}');
        return p?.id ? Number(p.id) : (userId ? Number(userId) : null);
      }
      // ADMIN
      return userId ? Number(userId) : null;
    } catch {
      return userId ? Number(userId) : null;
    }
  };

  const notificationUserId = resolveNotificationUserId();

  return (
    <aside className="sidebar">
      <div
        className="sidebar-logo"
        onClick={() => navigate('/')}
        style={{ cursor: 'pointer' }}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') navigate('/'); }}
      >
        <div className="sidebar-logo-mark">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M6.5 9v6" /><path d="M17.5 9v6" /><path d="M3 10.5v3" /><path d="M21 10.5v3" /><path d="M6.5 12h11" />
          </svg>
        </div>
        <div>
          <div className="sidebar-logo-text">GymPro</div>
          <div className="sidebar-logo-sub">Management System</div>
        </div>
      </div>

      <nav className="sidebar-nav">
        {navItems.map((item, i) =>
          item.section ? (
            <div key={i} className="nav-section-label">{item.section}</div>
          ) : (
            <button
              key={item.key}
              className={`nav-item ${active === item.key ? 'active' : ''}`}
              onClick={() => onNav(item.key)}
            >
              <span className="nav-item-icon">{ICONS[item.icon]}</span>
              {item.label}
            </button>
          )
        )}
      </nav>

      <div className="sidebar-footer">
        {/* Notification bell — works for all roles */}
        <NotificationBell userId={notificationUserId} />

        <div className="user-info">
          <div className="user-avatar" style={{ background: rc.bg, color: rc.color }}>
            {initials}
          </div>
          <div>
            <div className="user-name">{displayName}</div>
            <div className="user-role" style={{ color: rc.color }}>{role}</div>
          </div>
        </div>
        <ThemeToggle />

        <button className="nav-item" onClick={handleLogout} style={{ color: 'var(--red)' }}>
          <span className="nav-item-icon">{ICONS.logout}</span>
          Logout
        </button>
      </div>
    </aside>
  );
}