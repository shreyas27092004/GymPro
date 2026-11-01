// src/auth/Login.jsx

import { useState } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { authApi } from '../api/api';
import { useAuth } from '../context/AuthContext';
import { Spinner } from '../components/UI';

export default function Login() {
  const location = useLocation();
  const navigate = useNavigate();
  const { login } = useAuth();

  const prefilledEmail = location.state?.registeredEmail || '';
  const registerMsg = location.state?.successMsg || '';

  const [email, setEmail] = useState(prefilledEmail);
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e) => {
    e.preventDefault();

    setError('');
    setLoading(true);

    try {
      const res = await authApi.login({
        email: email.trim(),
        password,
      });

      const {
        token,
        name: userName,
        email: userEmail,
        role,
        userId,
      } = res.data;

      login(
        token,
        role,
        userEmail || email,
        userId,
        userName
      );

      switch (role) {
        case 'ADMIN':
          navigate('/admin');
          break;
        case 'TRAINER':
          navigate('/trainer');
          break;
        default:
          navigate('/member');
      }
    } catch (err) {
      setError(
        err.response?.data?.message ||
        'Invalid email or password'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-bg-grid" />
      <div
        className="auth-bg-glow"
        style={{
          top: '-200px',
          left: '50%',
          transform: 'translateX(-50%)',
        }}
      />

      <div className="auth-card fade-in">
        <div
          className="auth-logo"
          onClick={() => navigate('/')}
          style={{ cursor: 'pointer' }}
        >
          GYMPRO
        </div>

        <div className="auth-tagline">
          Gym Management System
        </div>

        <form className="auth-form" onSubmit={handleLogin}>
          {registerMsg && !error && (
            <div className="alert alert-success">
              ✅ {registerMsg}
            </div>
          )}

          {error && (
            <div className="alert alert-error">
              {error}
            </div>
          )}

          <div className="form-group">
            <label className="form-label">
              Email Address
            </label>
            <input
              type="email"
              value={email}
              required
              autoComplete="email"
              placeholder="you@example.com"
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label className="form-label">
              Password
            </label>
            <input
              type="password"
              value={password}
              required
              autoComplete="current-password"
              placeholder="••••••••"
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          <button
            type="submit"
            className="auth-submit"
            disabled={loading}
          >
            {loading ? (
              <span
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: 8,
                }}
              >
                <Spinner size={16} />
                Signing in...
              </span>
            ) : (
              'Sign In'
            )}
          </button>
        </form>

        <p style={{ textAlign: 'center', marginTop: 12 }}>
          <Link
            to="/forgot-password"
            className="auth-link"
            style={{ fontSize: 13 }}
          >
            Forgot password?
          </Link>
        </p>

        <p className="auth-link-row">
          Don't have an account?{' '}
          <Link
            to="/register"
            className="auth-link"
          >
            Register
          </Link>
        </p>
      </div>
    </div>
  );
}