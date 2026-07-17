// src/auth/Register.jsx
// Register no longer returns a JWT token (AOP-based backend).
// On success, shows the server message and redirects to /login.
//
// SPECIAL CASE — role=ADMIN:
//   If at least one admin already exists, the account is NOT created on
//   submit. The backend emails a 6-digit code to every existing admin and
//   responds with verificationRequired: true. The form then shows a second
//   step where the new admin enters that code (given to them verbally/in
//   person by an existing admin) to actually create the account.
//   The very first admin ever is created immediately — no code needed.

import { useState, useRef } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authApi } from '../api/api';
import { Spinner } from '../components/UI';

export default function Register() {
  const [step, setStep] = useState(1); // 1 = registration form, 2 = admin code verification
  const [form, setForm]       = useState({ name: '', email: '', password: '', role: 'MEMBER' });
  const [otp, setOtp]         = useState(['', '', '', '', '', '']); // 6 boxes
  const [error, setError]     = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [pendingMsg, setPendingMsg] = useState('');

  const otpRefs = useRef([]);
  const navigate = useNavigate();
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  // ── Step 1: submit registration form ──────────────────────────────────
  const handleRegister = async (e) => {
    e.preventDefault();
    setError(''); setSuccess(''); setLoading(true);
    try {
      const res = await authApi.register(form);
      // Backend returns: { token: null, email, role, message, verificationRequired }

      if (res.data?.verificationRequired) {
        // ADMIN registration is pending — an existing admin was emailed a code.
        setPendingMsg(res.data.message || 'A verification code was sent to an existing admin.');
        setStep(2);
        setLoading(false);
        return;
      }

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

  // ── OTP input handling (mirrors ForgotPassword.jsx) ───────────────────
  const handleOtpChange = (index, value) => {
    if (value && !/^\d$/.test(value)) return;
    const updated = [...otp];
    updated[index] = value;
    setOtp(updated);
    if (value && index < 5) otpRefs.current[index + 1]?.focus();
  };

  const handleOtpKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      otpRefs.current[index - 1]?.focus();
    }
    if (e.key === 'ArrowLeft' && index > 0) otpRefs.current[index - 1]?.focus();
    if (e.key === 'ArrowRight' && index < 5) otpRefs.current[index + 1]?.focus();
  };

  const handleOtpPaste = (e) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    if (!pasted) return;
    const updated = [...otp];
    pasted.split('').forEach((ch, i) => { updated[i] = ch; });
    setOtp(updated);
    otpRefs.current[Math.min(pasted.length, 5)]?.focus();
  };

  // ── Step 2: verify the admin approval code ────────────────────────────
  const handleVerifyAdminCode = async (e) => {
    e.preventDefault();
    const otpStr = otp.join('');
    if (otpStr.length < 6) { setError('Please enter all 6 digits.'); return; }
    setError(''); setLoading(true);
    try {
      const res = await authApi.verifyAdminRegistration({ email: form.email, otp: otpStr });
      // Backend returns verificationRequired:true again on failure (invalid/expired code)
      if (res.data?.verificationRequired) {
        setError(res.data.message || 'Invalid or expired code. Try again.');
        setOtp(['', '', '', '', '', '']);
        otpRefs.current[0]?.focus();
        return;
      }
      const msg = res.data?.message || 'Admin account created! Please sign in.';
      setSuccess(msg);
      setTimeout(() => {
        navigate('/login', { state: { registeredEmail: form.email, successMsg: msg } });
      }, 1800);
    } catch (err) {
      setError(err.response?.data?.message || 'Verification failed. Try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-bg-grid" />
      <div className="auth-bg-glow" style={{ top: '-200px', left: '50%', transform: 'translateX(-50%)' }} />

      <div className="auth-card fade-in">
<div
  className="auth-logo"
  onClick={() => navigate('/')}
  style={{ cursor: 'pointer' }}
>
  GYMPRO
</div>        <div className="auth-tagline">
          {step === 1 ? 'Create your account' : 'Admin approval required'}
        </div>

        {error   && <div className="alert alert-error" style={{ marginBottom: 16 }}>{error}</div>}
        {success && (
          <div className="alert alert-success" style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 16 }}>
            <span>✅</span>
            <span>{success} Redirecting to sign in…</span>
          </div>
        )}

        {/* ── STEP 1: registration form ── */}
        {step === 1 && (
          <form className="auth-form" onSubmit={handleRegister}>
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
              {form.role === 'ADMIN' && (
                <div style={{ fontSize: 11, color: 'var(--text3)', marginTop: 6, lineHeight: 1.5 }}>
                  🔐 For security, new admin accounts need approval from an existing admin.
                  If any admins already exist, you'll be asked to enter a code they were emailed.
                </div>
              )}
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
        )}

        {/* ── STEP 2: enter the code an existing admin gave you ── */}
        {step === 2 && (
          <form onSubmit={handleVerifyAdminCode} style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
            <div>
              <p style={{ fontSize: 13, color: 'var(--text3)', marginBottom: 4, lineHeight: 1.6 }}>
                {pendingMsg}
              </p>
              <p style={{ fontSize: 14, fontWeight: 700, color: 'var(--accent)', marginTop: 8 }}>
                {form.email}
              </p>
              <p style={{ fontSize: 12, color: 'var(--text3)', marginTop: 4 }}>
                Ask an existing admin for the 6-digit code sent to their inbox. Valid for 10 minutes.
              </p>
            </div>

            {/* 6 OTP boxes */}
            <div style={{ display: 'flex', gap: 10, justifyContent: 'center' }}>
              {otp.map((digit, i) => (
                <input
                  key={i}
                  ref={el => otpRefs.current[i] = el}
                  type="text"
                  inputMode="numeric"
                  maxLength={1}
                  value={digit}
                  onChange={e => handleOtpChange(i, e.target.value)}
                  onKeyDown={e => handleOtpKeyDown(i, e)}
                  onPaste={i === 0 ? handleOtpPaste : undefined}
                  style={{
                    width: 48, height: 56, textAlign: 'center',
                    fontSize: 22, fontWeight: 700,
                    background: digit ? 'rgba(0,212,255,0.1)' : 'var(--bg2)',
                    border: `2px solid ${digit ? 'var(--accent)' : 'var(--border2)'}`,
                    borderRadius: 10, color: 'var(--text)',
                    outline: 'none', transition: 'all 0.15s',
                    caretColor: 'var(--accent)',
                  }}
                />
              ))}
            </div>

            <button type="submit" className="auth-submit" disabled={loading || !!success}>
              {loading
                ? <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}><Spinner size={16}/> Verifying…</span>
                : success
                ? <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}><Spinner size={16}/> Redirecting…</span>
                : 'Verify & Create Admin Account'}
            </button>

            {!success && (
              <button
                type="button"
                onClick={() => { setStep(1); setOtp(['', '', '', '', '', '']); setError(''); }}
                style={{ background: 'none', border: 'none', color: 'var(--text3)', fontSize: 12, cursor: 'pointer', fontFamily: 'var(--font-body)' }}
              >
                ← Back to registration form
              </button>
            )}
          </form>
        )}

        <p className="auth-link-row">
          Already have an account?{' '}
          <Link to="/login" className="auth-link">Sign in</Link>
        </p>
      </div>
    </div>
  );
}
