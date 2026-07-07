// src/components/Sidebar.jsx
// Updated: NotificationBell now uses userId from AuthContext for ALL roles,
// not just the member profile. Trainers and Admins also see their notifications.

import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import NotificationBell from './NotificationBell';
import { resolveMemberProfile } from '../api/memberProfile';
import { resolveTrainerProfile } from '../api/trainerProfile';

const ADMIN_NAV = [
  { section: 'Dashboard' },
  { key: 'overview',  icon: '🏠', label: 'Overview' },
  { section: 'Manage' },
  { key: 'members',   icon: '👥', label: 'Members' },
  { key: 'trainers',  icon: '💪', label: 'Trainers' },
  { key: 'plans',     icon: '📋', label: 'Plans' },
  { section: 'Operations' },
  { key: 'bookings',  icon: '📅', label: 'Bookings' },
  { key: 'payments',  icon: '💳', label: 'Payments' },
];

const TRAINER_NAV = [
  { section: 'My Work' },
  { key: 'members',   icon: '👥', label: 'My Members' },
  { key: 'sessions',  icon: '📅', label: 'Sessions' },
  { key: 'schedule',  icon: '🗓️', label: 'My Schedule' },
  { section: 'Account' },
  { key: 'profile',   icon: '👤', label: 'My Profile' },
];

const MEMBER_NAV = [
  { section: 'My Account' },
  { key: 'plans',     icon: '📋', label: 'My Plan' },
  { key: 'bookings',  icon: '📅', label: 'My Bookings' },
  { key: 'trainers',  icon: '💪', label: 'Trainers' },
  { key: 'payments',  icon: '💳', label: 'Payments' },
  { section: 'Account' },
  { key: 'profile',   icon: '👤', label: 'My Profile' },
];

const ROLE_COLORS = {
  ADMIN:   { bg: 'rgba(255,179,71,0.2)', color: 'var(--amber)' },
  TRAINER: { bg: 'rgba(0,229,160,0.2)',  color: 'var(--green)' },
  MEMBER:  { bg: 'rgba(0,212,255,0.2)',  color: 'var(--accent)' },
};

export default function Sidebar({ active, onNav }) {
  const { role, logout, email, userId } = useAuth();
  const navigate = useNavigate();

  const navItems = role === 'ADMIN' ? ADMIN_NAV : role === 'TRAINER' ? TRAINER_NAV : MEMBER_NAV;
  const rc = ROLE_COLORS[role] || ROLE_COLORS.MEMBER;

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const initials = (email || 'U').slice(0, 2).toUpperCase();

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
      <div className="sidebar-logo">
        <div className="sidebar-logo-text">GYMPRO</div>
        <div className="sidebar-logo-sub">Management System</div>
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
              <span className="nav-item-icon">{item.icon}</span>
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
            <div className="user-name">{email || 'User'}</div>
            <div className="user-role" style={{ color: rc.color }}>{role}</div>
          </div>
        </div>
        <button className="nav-item" onClick={handleLogout} style={{ color: 'var(--red)', width: '100%' }}>
          <span className="nav-item-icon">🚪</span>
          Logout
        </button>
      </div>
    </aside>
  );
}