// src/pages/admin/Members.jsx

import { useState, useEffect } from 'react';
import { memberApi } from '../../api/api';
import { LoadingCenter, Alert, Modal, StatusBadge, EmptyState, SectionHeader, FormGroup, ConfirmModal } from '../../components/UI';

export default function AdminMembers() {
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');
  const [showForm, setShowForm] = useState(false);
  const [editItem, setEditItem] = useState(null);
  const [confirm,  setConfirm] = useState(null);
  const [search,   setSearch]  = useState('');

  const load = async () => {
    try {
      setLoading(true);
      const res = await memberApi.getAll();
      setMembers(res.data);
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to load members');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const filtered = members.filter(m =>
    m.name?.toLowerCase().includes(search.toLowerCase()) ||
    m.email?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="fade-in">
      <div className="content-header">
        <div className="page-title">MEMBERS</div>
        <div className="page-subtitle">Manage all gym members</div>
      </div>
      <div className="content-body">
        {error && <Alert type="error">{error}</Alert>}

        <div className="card">
          <SectionHeader title={`${filtered.length} Members`}>
            <input
              placeholder="Search members…"
              value={search} onChange={e => setSearch(e.target.value)}
              style={{ width: 220 }}
            />
            <button className="btn btn-primary" onClick={() => { setEditItem(null); setShowForm(true); }}>
              + Add Member
            </button>
          </SectionHeader>

          {loading ? <LoadingCenter /> : filtered.length === 0 ? (
            <EmptyState icon="👥" text="No members found" />
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Name</th><th>Email</th><th>Phone</th>
                    <th>Gender</th><th>Status</th><th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map(m => (
                    <tr key={m.id}>
                      <td style={{ color: 'var(--text)', fontWeight: 600 }}>{m.name}</td>
                      <td>{m.email}</td>
                      <td>{m.phone || '—'}</td>
                      <td>{m.gender || '—'}</td>
                      <td><StatusBadge status={m.status || 'ACTIVE'} /></td>
                      <td>
                        <div style={{ display: 'flex', gap: 8 }}>
                          <button className="btn btn-ghost btn-sm" onClick={() => { setEditItem(m); setShowForm(true); }}>Edit</button>
                          <button className="btn btn-danger btn-sm" onClick={() => setConfirm(m)}>Delete</button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {showForm && (
          <MemberForm
            initial={editItem}
            onClose={() => setShowForm(false)}
            onSaved={() => { setShowForm(false); load(); }}
          />
        )}

        {confirm && (
          <ConfirmModal
            title="Delete Member"
            message={`Remove ${confirm.name} from the system? This cannot be undone.`}
            onClose={() => setConfirm(null)}
            onConfirm={async () => {
              await memberApi.delete(confirm.id);
              setConfirm(null);
              load();
            }}
          />
        )}
      </div>
    </div>
  );
}

function MemberForm({ initial, onClose, onSaved }) {
  const [form, setForm] = useState({
    name: '', email: '', phone: '', address: '', gender: '', status: 'ACTIVE',
    ...initial,
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const set = k => e => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault(); setError(''); setLoading(true);
    try {
      if (initial) await memberApi.update(initial.id, form);
      else         await memberApi.create(form);
      onSaved();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal title={initial ? 'Edit Member' : 'Add Member'} onClose={onClose}>
      <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {error && <Alert type="error">{error}</Alert>}
        <div className="form-row">
          <FormGroup label="Full Name"><input value={form.name} onChange={set('name')} required /></FormGroup>
          <FormGroup label="Email"><input type="email" value={form.email} onChange={set('email')} required disabled={!!initial} /></FormGroup>
        </div>
        <div className="form-row">
          <FormGroup label="Phone"><input value={form.phone} onChange={set('phone')} /></FormGroup>
          <FormGroup label="Gender">
            <select value={form.gender} onChange={set('gender')}>
              <option value="">Select…</option>
              <option>Male</option><option>Female</option><option>Other</option>
            </select>
          </FormGroup>
        </div>
        <FormGroup label="Address"><input value={form.address} onChange={set('address')} /></FormGroup>
        <FormGroup label="Status">
          <select value={form.status} onChange={set('status')}>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
        </FormGroup>
        <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 8 }}>
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Saving…' : initial ? 'Update' : 'Create'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
