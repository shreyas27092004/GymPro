// src/components/Navbar.jsx
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

function Navbar() {
  const { role, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const roleColors = { ADMIN: '#F59E0B', TRAINER: '#10B981', MEMBER: '#0EA5E9' };
  const roleColor = roleColors[role] || '#94A3B8';

  return (
    <nav style={styles.nav}>
      <span style={styles.logo}>🏋️ GymPro</span>
      <div style={styles.right}>
        <span style={{ ...styles.roleBadge, background: roleColor }}>
          {role}
        </span>
        <button onClick={handleLogout} style={styles.logoutBtn}>Logout</button>
      </div>
    </nav>
  );
}

const styles = {
  nav: {
    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
    background: '#1E293B', padding: '14px 28px',
    borderBottom: '1px solid #334155',
  },
  logo: { color: '#38BDF8', fontSize: '20px', fontWeight: '700' },
  right: { display: 'flex', alignItems: 'center', gap: '14px' },
  roleBadge: {
    color: '#fff', padding: '4px 12px', borderRadius: '20px',
    fontSize: '12px', fontWeight: '700', letterSpacing: '0.5px',
  },
  logoutBtn: {
    background: 'transparent', border: '1px solid #475569', color: '#94A3B8',
    padding: '6px 16px', borderRadius: '6px', cursor: 'pointer', fontSize: '13px',
  },
};

export default Navbar;
