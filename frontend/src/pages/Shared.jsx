import { useState } from 'react';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Empty, ErrorNote, Loading, PageHeader } from '../components/ui.jsx';
import { fmtDate, useAsync } from '../hooks.js';
import { useToast } from '../toast.jsx';

export function Announcements() {
  const list = useAsync(() => api.get('/announcements'), []);
  return (
    <>
      <PageHeader title="Announcements" note="Notices from the university office." />
      <ErrorNote error={list.error} onRetry={list.reload} />
      {list.loading && !list.data && <Loading />}
      {list.data && list.data.length === 0 && <Empty>No announcements right now.</Empty>}
      <div className="notices">
        {list.data?.map((a) => (
          <article key={a.id} className="notice">
            <h2>{a.title}</h2>
            <p>{a.content}</p>
            <p className="meta">Posted by {a.postedByName} on {fmtDate(a.createdAt)}</p>
          </article>
        ))}
      </div>
    </>
  );
}

export function Account() {
  const { user } = useAuth();
  const toast = useToast();
  const profile = useAsync(() => {
    if (user.role === 'TEACHER') return api.get('/teacher/profile');
    if (user.role === 'STUDENT') return api.get('/student/profile');
    return api.get('/auth/me');
  }, [user.role]);

  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirm: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    if (form.newPassword !== form.confirm) {
      setError('The new password and its confirmation do not match.');
      return;
    }
    setBusy(true);
    try {
      await api.post('/auth/change-password', {
        currentPassword: form.currentPassword,
        newPassword: form.newPassword,
      });
      toast('Password changed');
      setForm({ currentPassword: '', newPassword: '', confirm: '' });
    } catch (err) {
      setError(err.fieldErrors?.newPassword || err.message);
    } finally {
      setBusy(false);
    }
  };

  const p = profile.data;
  const rows = p && user.role === 'TEACHER' ? [
    ['Name', p.fullName], ['Username', p.username], ['Employee ID', p.employeeId],
    ['Department', p.departmentName], ['Designation', p.designation], ['Qualification', p.qualification],
    ['Email', p.email], ['Phone', p.phone], ['Address', p.address], ['Joined', fmtDate(p.joiningDate)],
  ] : p && user.role === 'STUDENT' ? [
    ['Name', p.fullName], ['Username', p.username], ['Student ID', p.studentId],
    ['Department', p.departmentName], ['Batch', p.batch], ['Semester', p.semester],
    ['Email', p.email], ['Phone', p.phone], ['Address', p.address],
  ] : p ? [['Name', p.fullName], ['Username', p.username], ['Role', 'Administrator']] : [];

  return (
    <>
      <PageHeader title="My account" />
      <ErrorNote error={profile.error} onRetry={profile.reload} />
      <div className="two-col">
        <section>
          <h2>Your details</h2>
          {profile.loading && !p && <Loading />}
          <dl className="details">
            {rows.filter(([, v]) => v !== null && v !== undefined && v !== '').map(([k, v]) => (
              <div key={k}><dt>{k}</dt><dd>{v}</dd></div>
            ))}
          </dl>
          {user.role !== 'ADMIN' && (
            <p className="hint">These details can only be changed by an administrator. Contact the university office if something is wrong.</p>
          )}
        </section>
        <section>
          <h2>Change password</h2>
          <form onSubmit={submit} className="stack">
            <div className="field">
              <label htmlFor="cur">Current password</label>
              <input id="cur" type="password" autoComplete="current-password" value={form.currentPassword}
                onChange={(e) => setForm({ ...form, currentPassword: e.target.value })} required />
            </div>
            <div className="field">
              <label htmlFor="new">New password</label>
              <input id="new" type="password" autoComplete="new-password" minLength={6} value={form.newPassword}
                onChange={(e) => setForm({ ...form, newPassword: e.target.value })} required />
              <p className="hint">At least 6 characters.</p>
            </div>
            <div className="field">
              <label htmlFor="conf">Confirm new password</label>
              <input id="conf" type="password" autoComplete="new-password" value={form.confirm}
                onChange={(e) => setForm({ ...form, confirm: e.target.value })} required />
            </div>
            {error && <p className="form-error" role="alert">{error}</p>}
            <div><button className="btn primary" disabled={busy}>{busy ? 'Saving…' : 'Change password'}</button></div>
          </form>
        </section>
      </div>
    </>
  );
}
