// src/components/MemberProfileGate.jsx
// Reusable wrapper that resolves the member profile automatically by email.
// No manual ID input needed — profile is looked up or auto-created seamlessly.
//
// Usage:
//   <MemberProfileGate email={email}>
//     {(member, reload) => <YourComponent member={member} reload={reload} />}
//   </MemberProfileGate>

import { useState, useEffect, useCallback } from 'react';
import { resolveMemberProfile } from '../api/memberProfile';
import { LoadingCenter, Alert } from './UI';

export default function MemberProfileGate({ email, children }) {
  const [member,  setMember]  = useState(null);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');

  const resolve = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const me = await resolveMemberProfile(email);
      setMember(me);
    } catch (e) {
      setError(e.message || 'Could not load member profile.');
    } finally {
      setLoading(false);
    }
  }, [email]);

  useEffect(() => { resolve(); }, [resolve]);

  if (loading) return <LoadingCenter />;

  if (error) return (
    <div style={{ padding: '0 32px' }}>
      <Alert type="error">{error}</Alert>
      <button className="btn btn-ghost" onClick={resolve} style={{ marginTop: 12 }}>
        Retry
      </button>
    </div>
  );

  return children(member, resolve);
}
