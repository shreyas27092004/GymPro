// src/auth/Register.jsx
// Updated: register no longer returns a JWT token (AOP-based backend).
// On success, shows the server message and redirects to /login.

import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authApi } from '../api/api';
import { Spinner } from '../components/UI';

export default function Register() {
  const [form, setForm]       = useState({ name: '', email: '', password: '', role: 'MEMBER' });
  const [error, setError]     = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const navigate = useNavigate();
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const handleRegister = async (e) => {
    e.preventDefault();
    setError(''); setSuccess(''); setLoading(true);
    try {
      const res = await authApi.register(form);
      // Backend now returns: { token: null, email, role, message }
      // No token on register — must login separately to get JWT
      const msg = res.data?.message || 'Registered successfully! Please sign in.';
      setSuccess(msg);

      // Redirect to login after a short delay, passing email as state
      // so the login form can pre-fill it
      setTimeout(() => {
        navigate('/login', { state: { registeredEmail: form.email, successMsg: msg } });
      }, 1800);

    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed. Try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-bg-grid" />
      <div className="auth-bg-glow" style={{ top: '-200px', left: '50%', transform: 'translateX(-50%)' }} />

      <div className="auth-card fade-in">
        <div className="auth-logo">GYMPRO</div>
        <div className="auth-tagline">Create your account</div>

        <form className="auth-form" onSubmit={handleRegister}>
          {error   && <div className="alert alert-error">{error}</div>}
          {success && (
            <div className="alert alert-success" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span>✅</span>
              <span>{success} Redirecting to sign in…</span>
            </div>
          )}

          <div className="form-group">
            <label className="form-label">Full Name</label>
            <input
              value={form.name} onChange={set('name')}
              placeholder="John Doe" required
              disabled={!!success}
            />
          </div>
          <div className="form-group">
            <label className="form-label">Email Address</label>
            <input
              type="email" value={form.email} onChange={set('email')}
              placeholder="you@example.com" required
              disabled={!!success}
            />
          </div>
          <div className="form-group">
            <label className="form-label">Password</label>
            <input
              type="password" value={form.password} onChange={set('password')}
              placeholder="••••••••" required
              disabled={!!success}
            />
          </div>
          <div className="form-group">
            <label className="form-label">Role</label>
            <select value={form.role} onChange={set('role')} disabled={!!success}>
              <option value="MEMBER">Member</option>
              <option value="TRAINER">Trainer</option>
              <option value="ADMIN">Admin</option>
            </select>
          </div>

          <button type="submit" className="auth-submit" disabled={loading || !!success}>
            {loading
              ? <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
                  <Spinner size={16} /> Creating account…
                </span>
              : success
              ? <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
                  <Spinner size={16} /> Redirecting…
                </span>
              : 'Create Account'}
          </button>
        </form>

        <p className="auth-link-row">
          Already have an account?{' '}
          <Link to="/login" className="auth-link">Sign in</Link>
        </p>
      </div>
    </div>
  );
}
