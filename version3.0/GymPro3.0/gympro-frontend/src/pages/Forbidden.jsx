import { useNavigate } from 'react-router-dom';

export default function Forbidden() {
  const navigate = useNavigate();
  return (
    <div style={{
      minHeight: '100vh', background: 'var(--bg)',
      display: 'flex', flexDirection: 'column',
      alignItems: 'center', justifyContent: 'center', gap: 16,
    }}>
      <div style={{ fontFamily: 'var(--font-display)', fontSize: 96, color: 'var(--red)', lineHeight: 1 }}>403</div>
      <div style={{ fontSize: 18, fontWeight: 700, color: 'var(--text)' }}>Access Denied</div>
      <div style={{ fontSize: 14, color: 'var(--text3)' }}>You don't have permission to view this page.</div>
      <button className="btn btn-primary" onClick={() => navigate('/login')} style={{ marginTop: 8 }}>
        Go to Login
      </button>
    </div>
  );
}
