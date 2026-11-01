// src/auth/ForgotPassword.jsx
// 3-step password reset flow:
//   Step 1 — Enter email → POST /auth/forgot-password → OTP sent to inbox
//   Step 2 — Enter 6-digit OTP → POST /auth/verify-otp → OTP confirmed
//   Step 3 — Enter new password → POST /auth/reset-password → done, redirect to login

import { useState, useRef, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authApi } from '../api/api';
import { Spinner } from '../components/UI';

export default function ForgotPassword() {
  const [step,        setStep]        = useState(1); // 1 | 2 | 3
  const [email,       setEmail]       = useState('');
  const [otp,         setOtp]         = useState(['', '', '', '', '', '']); // 6 boxes
  const [newPassword, setNewPassword] = useState('');
  const [confirmPass, setConfirmPass] = useState('');
  const [showPass,    setShowPass]    = useState(false);
  const [loading,     setLoading]     = useState(false);
  const [error,       setError]       = useState('');
  const [success,     setSuccess]     = useState('');
  const [resendTimer, setResendTimer] = useState(0);

  const otpRefs = useRef([]);
  const navigate = useNavigate();

  // Countdown timer for resend
  useEffect(() => {
    if (resendTimer <= 0) return;
    const t = setTimeout(() => setResendTimer(r => r - 1), 1000);
    return () => clearTimeout(t);
  }, [resendTimer]);

  // ── Step 1: Send OTP ──────────────────────────────────────────────────
  const handleSendOtp = async (e) => {
    e.preventDefault();
    setError(''); setLoading(true);
    try {
      const res = await authApi.forgotPassword({ email });
      if (res.data.success) {
        setStep(2);
        setResendTimer(60); // 60s before allowing resend
      } else {
        setError(res.data.message);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to send OTP. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  // Resend OTP (same as step 1 but without moving step)
  const handleResend = async () => {
    if (resendTimer > 0) return;
    setError(''); setLoading(true);
    try {
      await authApi.forgotPassword({ email });
      setOtp(['', '', '', '', '', '']);
      setResendTimer(60);
      otpRefs.current[0]?.focus();
    } catch {
      setError('Failed to resend OTP.');
    } finally {
      setLoading(false);
    }
  };

  // ── OTP input handling ────────────────────────────────────────────────
  const handleOtpChange = (index, value) => {
    // Accept only digits
    if (value && !/^\d$/.test(value)) return;
    const updated = [...otp];
    updated[index] = value;
    setOtp(updated);
    // Auto-advance
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

  // ── Step 2: Verify OTP ────────────────────────────────────────────────
  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    const otpStr = otp.join('');
    if (otpStr.length < 6) { setError('Please enter all 6 digits.'); return; }
    setError(''); setLoading(true);
    try {
      const res = await authApi.verifyOtp({ email, otp: otpStr });
      if (res.data.success) {
        setStep(3);
      } else {
        setError(res.data.message || 'Invalid OTP. Please try again.');
        setOtp(['', '', '', '', '', '']);
        otpRefs.current[0]?.focus();
      }
    } catch (err) {
      setError(err.response?.data?.message || 'OTP verification failed.');
      setOtp(['', '', '', '', '', '']);
      otpRefs.current[0]?.focus();
    } finally {
      setLoading(false);
    }
  };

  // ── Step 3: Reset password ────────────────────────────────────────────
  const handleResetPassword = async (e) => {
    e.preventDefault();
    if (newPassword.length < 6) { setError('Password must be at least 6 characters.'); return; }
    if (newPassword !== confirmPass) { setError('Passwords do not match.'); return; }
    setError(''); setLoading(true);
    try {
      const res = await authApi.resetPassword({
        email, otp: otp.join(''), newPassword,
      });
      if (res.data.success) {
        setSuccess('Password reset successfully! Redirecting to login…');
        setTimeout(() => navigate('/login', {
          state: { successMsg: 'Password reset successful! Please sign in.' },
        }), 2000);
      } else {
        setError(res.data.message || 'Reset failed. Please try again.');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Password reset failed.');
    } finally {
      setLoading(false);
    }
  };

  // ── Step labels ───────────────────────────────────────────────────────
  const STEPS = ['Enter Email', 'Verify OTP', 'New Password'];

  return (
    <div className="auth-page">
      <div className="auth-bg-grid" />
      <div className="auth-bg-glow" style={{ top: '-200px', left: '50%', transform: 'translateX(-50%)' }} />

      <div className="auth-card fade-in" style={{ maxWidth: 460 }}>
        {/* Logo */}
        <div className="auth-logo">GYMPRO</div>
        <div className="auth-tagline">Reset your password</div>

        {/* Step indicator */}
        <div style={{ display: 'flex', gap: 8, marginBottom: 28 }}>
          {STEPS.map((label, i) => {
            const num     = i + 1;
            const active  = step === num;
            const done    = step > num;
            return (
              <div key={i} style={{ flex: 1, textAlign: 'center' }}>
                <div style={{
                  width: 28, height: 28, borderRadius: '50%', margin: '0 auto 4px',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: 12, fontWeight: 700,
                  background: done ? 'var(--green)' : active ? 'var(--accent)' : 'var(--bg4)',
                  color: done || active ? '#000' : 'var(--text3)',
                  transition: 'all 0.3s',
                }}>
                  {done ? '✓' : num}
                </div>
                <div style={{ fontSize: 10, color: active ? 'var(--accent)' : done ? 'var(--green)' : 'var(--text3)', letterSpacing: 0.3 }}>
                  {label}
                </div>
              </div>
            );
          })}
        </div>

        {/* Alerts */}
        {error   && <div className="alert alert-error"   style={{ marginBottom: 16 }}>{error}</div>}
        {success && <div className="alert alert-success" style={{ marginBottom: 16 }}>✅ {success}</div>}

        {/* ── STEP 1 ── */}
        {step === 1 && (
          <form onSubmit={handleSendOtp} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <div>
              <p style={{ fontSize: 13, color: 'var(--text3)', marginBottom: 16, lineHeight: 1.6 }}>
                Enter your registered email address and we'll send you a 6-digit OTP to reset your password.
              </p>
            </div>
            <div className="form-group">
              <label className="form-label">Email Address</label>
              <input
                type="email" value={email} required
                onChange={e => setEmail(e.target.value)}
                placeholder="you@example.com"
              />
            </div>
            <button type="submit" className="auth-submit" disabled={loading}>
              {loading
                ? <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}><Spinner size={16}/> Sending OTP…</span>
                : 'Send OTP'}
            </button>
          </form>
        )}

        {/* ── STEP 2 ── */}
        {step === 2 && (
          <form onSubmit={handleVerifyOtp} style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
            <div>
              <p style={{ fontSize: 13, color: 'var(--text3)', marginBottom: 4, lineHeight: 1.6 }}>
                We sent a 6-digit OTP to
              </p>
              <p style={{ fontSize: 14, fontWeight: 700, color: 'var(--accent)' }}>{email}</p>
              <p style={{ fontSize: 12, color: 'var(--text3)', marginTop: 4 }}>
                Check your inbox (and spam folder). Valid for 10 minutes.
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

            <button type="submit" className="auth-submit" disabled={loading}>
              {loading
                ? <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}><Spinner size={16}/> Verifying…</span>
                : 'Verify OTP'}
            </button>

            {/* Resend */}
            <div style={{ textAlign: 'center', fontSize: 13, color: 'var(--text3)' }}>
              Didn't receive it?{' '}
              {resendTimer > 0 ? (
                <span>Resend in <span style={{ color: 'var(--accent)', fontWeight: 600 }}>{resendTimer}s</span></span>
              ) : (
                <button
                  type="button"
                  onClick={handleResend}
                  disabled={loading}
                  style={{ background: 'none', border: 'none', color: 'var(--accent)', fontWeight: 600, cursor: 'pointer', padding: 0, fontSize: 13, fontFamily: 'var(--font-body)' }}
                >
                  {loading ? 'Sending…' : 'Resend OTP'}
                </button>
              )}
            </div>

            <button
              type="button"
              onClick={() => { setStep(1); setOtp(['', '', '', '', '', '']); setError(''); }}
              style={{ background: 'none', border: 'none', color: 'var(--text3)', fontSize: 12, cursor: 'pointer', fontFamily: 'var(--font-body)' }}
            >
              ← Change email
            </button>
          </form>
        )}

        {/* ── STEP 3 ── */}
        {step === 3 && (
          <form onSubmit={handleResetPassword} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <p style={{ fontSize: 13, color: 'var(--text3)', lineHeight: 1.6, marginBottom: 4 }}>
              OTP verified ✅ Set a new password for <span style={{ color: 'var(--accent)', fontWeight: 600 }}>{email}</span>
            </p>
            <div className="form-group">
              <label className="form-label">New Password</label>
              <div style={{ position: 'relative' }}>
                <input
                  type={showPass ? 'text' : 'password'}
                  value={newPassword}
                  onChange={e => setNewPassword(e.target.value)}
                  placeholder="Min. 6 characters"
                  required
                  style={{ paddingRight: 44 }}
                />
                <button
                  type="button"
                  onClick={() => setShowPass(s => !s)}
                  style={{
                    position: 'absolute', right: 12, top: '50%', transform: 'translateY(-50%)',
                    background: 'none', border: 'none', cursor: 'pointer', fontSize: 16,
                    color: 'var(--text3)', padding: 0, lineHeight: 1,
                  }}
                >
                  {showPass ? '🙈' : '👁'}
                </button>
              </div>
              {newPassword && (
                <PasswordStrength password={newPassword} />
              )}
            </div>
            <div className="form-group">
              <label className="form-label">Confirm Password</label>
              <input
                type={showPass ? 'text' : 'password'}
                value={confirmPass}
                onChange={e => setConfirmPass(e.target.value)}
                placeholder="Re-enter new password"
                required
              />
              {confirmPass && newPassword !== confirmPass && (
                <div style={{ fontSize: 11, color: 'var(--red)', marginTop: 4 }}>Passwords do not match</div>
              )}
            </div>
            <button type="submit" className="auth-submit" disabled={loading || !success === false && (newPassword !== confirmPass || newPassword.length < 6)}>
              {loading
                ? <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}><Spinner size={16}/> Resetting…</span>
                : 'Reset Password'}
            </button>
          </form>
        )}

        <p className="auth-link-row" style={{ marginTop: 20 }}>
          <Link to="/login" className="auth-link">← Back to Sign In</Link>
        </p>
      </div>
    </div>
  );
}

// Password strength indicator
function PasswordStrength({ password }) {
  const checks = [
    password.length >= 6,
    password.length >= 8,
    /[A-Z]/.test(password),
    /[0-9]/.test(password),
    /[^A-Za-z0-9]/.test(password),
  ];
  const score  = checks.filter(Boolean).length;
  const labels = ['Too short', 'Weak', 'Fair', 'Good', 'Strong', 'Very strong'];
  const colors = ['var(--red)', 'var(--red)', 'var(--amber)', 'var(--amber)', 'var(--green)', 'var(--green)'];

  return (
    <div style={{ marginTop: 8 }}>
      <div style={{ display: 'flex', gap: 4, marginBottom: 4 }}>
        {[1, 2, 3, 4, 5].map(i => (
          <div key={i} style={{
            flex: 1, height: 3, borderRadius: 2,
            background: i <= score ? colors[score] : 'var(--border2)',
            transition: 'all 0.3s',
          }} />
        ))}
      </div>
      <div style={{ fontSize: 11, color: colors[score] }}>{labels[score]}</div>
    </div>
  );
}
