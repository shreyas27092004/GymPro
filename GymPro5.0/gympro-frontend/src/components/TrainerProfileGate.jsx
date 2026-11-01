// src/components/TrainerProfileGate.jsx
// Resolves the logged-in trainer's profile automatically by email.
//
// Usage:
//   <TrainerProfileGate email={email}>
//     {(trainer, reload) => <YourComponent trainer={trainer} reload={reload} />}
//   </TrainerProfileGate>

import { useState, useEffect, useCallback } from 'react';
import { resolveTrainerProfile } from '../api/trainerProfile';
import { LoadingCenter, Alert } from './UI';

export default function TrainerProfileGate({ email, children }) {
  const [trainer, setTrainer] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');

  const resolve = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const t = await resolveTrainerProfile(email);
      setTrainer(t);
    } catch (e) {
      setError(e.message || 'Could not load trainer profile.');
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

  return children(trainer, resolve);
}
