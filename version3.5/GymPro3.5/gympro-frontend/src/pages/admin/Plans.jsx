// src/pages/admin/Plans.jsx

import { useState, useEffect } from 'react';
import { planApi } from '../../api/api';
import { LoadingCenter, Alert, Modal, StatusBadge, EmptyState, SectionHeader, FormGroup, ConfirmModal } from '../../components/UI';
import Icon from '../../components/icons';

export default function AdminPlans() {
  const [plans,   setPlans]   = useState([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');
  const [showForm, setShowForm] = useState(false);
  const [editItem, setEditItem] = useState(null);
  const [confirm,  setConfirm] = useState(null);

  const load = async () => {
    try {
      setLoading(true);
      const res = await planApi.getAll();
      setPlans(res.data);
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to load plans');
    } finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);

  const DURATION_COLORS = { MONTHLY: 'blue', QUARTERLY: 'purple', YEARLY: 'green' };

  return (
    <div className="fade-in">
      <div className="content-header">
        <div className="page-title">PLANS</div>
        <div className="page-subtitle">Create and manage membership plans</div>
      </div>
      <div className="content-body">
        {error && <Alert type="error">{error}</Alert>}

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 20, marginBottom: 24 }}>
          {!loading && plans.map(plan => (
            <div key={plan.id} className="card fade-in" style={{ position: 'relative', borderColor: plan.active ? 'var(--border)' : 'rgba(255,91,110,0.3)' }}>
              <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: 3, background: plan.active ? 'var(--accent)' : 'var(--red)', borderRadius: '16px 16px 0 0' }} />
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12, marginTop: 8 }}>
                <span className={`badge badge-${DURATION_COLORS[plan.durationType] || 'blue'}`}>{plan.durationType}</span>
                <StatusBadge status={plan.active ? 'ACTIVE' : 'INACTIVE'} />
              </div>
              <div style={{ fontSize: 20, fontWeight: 700, color: 'var(--text)', marginBottom: 4 }}>{plan.planName}</div>
              <div style={{ fontSize: 13, color: 'var(--text3)', marginBottom: 12 }}>{plan.description}</div>

              {/* Free sessions badge */}
              <div style={{ marginBottom: 14 }}>
                <span style={{
                  display: 'inline-flex', alignItems: 'center', gap: 5,
                  background: plan.sessionsIncluded > 0 ? 'rgba(0,229,160,0.1)' : 'rgba(255,255,255,0.05)',
                  color: plan.sessionsIncluded > 0 ? 'var(--green)' : 'var(--text3)',
                  border: `1px solid ${plan.sessionsIncluded > 0 ? 'rgba(0,229,160,0.3)' : 'var(--border)'}`,
                  padding: '3px 10px', borderRadius: 20, fontSize: 12, fontWeight: 600,
                }}>
                  <Icon name="ticket" size={11} style={{ marginRight: 4, verticalAlign: '-1px' }} />{plan.sessionsIncluded ?? 0} Free Session{plan.sessionsIncluded !== 1 ? 's' : ''}
                </span>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <div style={{ fontFamily: 'var(--font-display)', fontSize: 32, color: 'var(--accent)', lineHeight: 1 }}>₹{plan.price}</div>
                  <div style={{ fontSize: 11, color: 'var(--text3)', marginTop: 2 }}>{plan.durationDays} days</div>
                </div>
                <div style={{ display: 'flex', gap: 8 }}>
                  <button className="btn btn-ghost btn-sm" onClick={() => { setEditItem(plan); setShowForm(true); }}>Edit</button>
                  <button className="btn btn-danger btn-sm" onClick={() => setConfirm(plan)}>Remove</button>
                </div>
              </div>
            </div>
          ))}
          {!loading && (
            <button
              onClick={() => { setEditItem(null); setShowForm(true); }}
              style={{
                background: 'transparent', border: '2px dashed var(--border2)',
                borderRadius: 16, padding: 24, cursor: 'pointer',
                color: 'var(--text3)', fontSize: 14, fontFamily: 'var(--font-body)',
                display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 8, minHeight: 160,
                transition: 'all 0.2s',
              }}
              onMouseOver={e => e.currentTarget.style.borderColor = 'var(--accent)'}
              onMouseOut={e => e.currentTarget.style.borderColor = 'var(--border2)'}
            >
              <span style={{ fontSize: 28 }}>+</span>
              <span>Create New Plan</span>
            </button>
          )}
        </div>

        {loading && <LoadingCenter />}
        {!loading && plans.length === 0 && <EmptyState icon={<Icon name="clipboard" size={20} style={{ color: 'var(--text3)' }} />} text="No plans created yet" />}

        {showForm && (
          <PlanForm initial={editItem} onClose={() => setShowForm(false)} onSaved={() => { setShowForm(false); load(); }} />
        )}
        {confirm && (
          <ConfirmModal
            title="Deactivate Plan"
            message={`Deactivate "${confirm.planName}"? Members with this plan won't be affected.`}
            onClose={() => setConfirm(null)}
            onConfirm={async () => { await planApi.deactivate(confirm.id); setConfirm(null); load(); }}
          />
        )}
      </div>
    </div>
  );
}

function PlanForm({ initial, onClose, onSaved }) {
  const [form, setForm] = useState({
    planName: '', description: '', durationType: 'MONTHLY',
    price: '', durationDays: 30, active: true,
    sessionsIncluded: initial?.sessionsIncluded ?? 0,
    ...initial,
  });
  const [loading, setLoading] = useState(false);
  const [error,   setError]   = useState('');
  const set = k => e => setForm({ ...form, [k]: e.target.value });

  const DURATION_PRESETS = { MONTHLY: 30, QUARTERLY: 90, YEARLY: 365 };

  const submit = async (e) => {
    e.preventDefault(); setError(''); setLoading(true);
    try {
      const payload = {
        ...form,
        sessionsIncluded: parseInt(form.sessionsIncluded, 10) || 0,
      };
      if (initial) await planApi.update(initial.id, payload);
      else         await planApi.create(payload);
      onSaved();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save plan');
    } finally { setLoading(false); }
  };

  return (
    <Modal title={initial ? 'Edit Plan' : 'Create Plan'} onClose={onClose}>
      <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {error && <Alert type="error">{error}</Alert>}
        <FormGroup label="Plan Name"><input value={form.planName} onChange={set('planName')} placeholder="e.g. Gold Monthly" required /></FormGroup>
        <FormGroup label="Description"><input value={form.description} onChange={set('description')} placeholder="Brief description" /></FormGroup>
        <div className="form-row">
          <FormGroup label="Duration Type">
            <select value={form.durationType} onChange={e => setForm({ ...form, durationType: e.target.value, durationDays: DURATION_PRESETS[e.target.value] || 30 })}>
              <option value="MONTHLY">Monthly</option>
              <option value="QUARTERLY">Quarterly</option>
              <option value="YEARLY">Yearly</option>
            </select>
          </FormGroup>
          <FormGroup label="Duration (days)"><input type="number" min="1" value={form.durationDays} onChange={set('durationDays')} required /></FormGroup>
        </div>
        <div className="form-row">
          <FormGroup label="Price (₹)">
            <input type="number" min="0" step="0.01" value={form.price} onChange={set('price')} placeholder="999" required />
          </FormGroup>
          <FormGroup label="Free Trainer Sessions">
            <input
              type="number" min="0" max="100"
              value={form.sessionsIncluded}
              onChange={set('sessionsIncluded')}
              placeholder="0"
            />
          </FormGroup>
        </div>
        <div style={{
          background: 'rgba(0,229,160,0.06)', border: '1px solid rgba(0,229,160,0.2)',
          borderRadius: 8, padding: '10px 14px', fontSize: 12, color: 'var(--text3)',
        }}>
          <Icon name="ticket" size={13} style={{ marginRight: 4, verticalAlign: '-2px' }} />Members on this plan will receive <strong style={{ color: 'var(--green)' }}>{form.sessionsIncluded || 0} free trainer session{form.sessionsIncluded !== 1 ? 's' : ''}</strong> included in their membership. Payment is required after this limit.
        </div>
        <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 8 }}>
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={loading}>{loading ? 'Saving…' : initial ? 'Update' : 'Create'}</button>
        </div>
      </form>
    </Modal>
  );
}