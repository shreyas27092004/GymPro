// src/components/Navbar.jsx
// Not currently mounted in App.jsx (Sidebar carries navigation + branding),
// kept in sync with the design system for any future top-bar use case.

import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

function Navbar() {
  const { role, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const roleBadgeClass = { ADMIN: 'badge-amber', TRAINER: 'badge-green', MEMBER: 'badge-blue' }[role] || 'badge-blue';

  return (
    <nav style={styles.nav}>
      <span style={styles.logo}>GymPro</span>
      <div style={styles.right}>
        <span className={`badge ${roleBadgeClass}`}>{role}</span>
        <button onClick={handleLogout} className="btn btn-ghost btn-sm">Logout</button>
      </div>
    </nav>
  );
}

const styles = {
  nav: {
    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
    background: 'var(--bg2)', padding: '12px 24px',
    borderBottom: '1px solid var(--border)',
  },
  logo: { color: 'var(--text)', fontSize: '15px', fontWeight: '700', letterSpacing: '-0.01em' },
  right: { display: 'flex', alignItems: 'center', gap: '12px' },
};

export default Navbar;
