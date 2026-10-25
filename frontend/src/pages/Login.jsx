import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { homeFor, useAuth } from '../auth.jsx';

// A week-view timetable drawn as a grid: purely decorative, it is the visual signature of the login page.
const DAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu'];
const SLOTS = ['9:00', '10:00', '11:00', '12:00', '2:00', '3:00'];
const BLOCKS = {
  '0-0': ['CSE 3101', 'teal'], '2-0': ['CSE 3101', 'teal'],
  '1-1': ['EEE 2101', 'blue'], '3-1': ['EEE 2101', 'blue'],
  '0-2': ['Lab', 'plum'], '4-2': ['SE 3201', 'teal'],
  '2-4': ['Seminar', 'yellow'], '3-3': ['MAT 1201', 'blue'],
  '1-5': ['Lab', 'plum'], '4-4': ['CSE 4101', 'teal'],
};

function Timetable() {
  return (
    <div className="timetable" aria-hidden="true">
      <div className="tt-corner" />
      {DAYS.map((d) => <div key={d} className="tt-day">{d}</div>)}
      {SLOTS.map((s, r) => (
        <div key={s} className="tt-row">
          <div className="tt-time">{s}</div>
          {DAYS.map((d, c) => {
            const b = BLOCKS[`${c}-${r}`];
            return <div key={d} className={`tt-cell${b ? ` filled ${b[1]}` : ''}`}>{b ? b[0] : ''}</div>;
          })}
        </div>
      ))}
    </div>
  );
}

export default function Login() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to={homeFor(user.role)} replace />;

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      const u = await login(username.trim(), password);
      navigate(homeFor(u.role), { replace: true });
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
  };

  return (
    <div className="login">
      <section className="login-panel">
        <h1>Sign in</h1>
        <p className="note">Use the username and password given to you by your university office.</p>
        <form onSubmit={submit}>
          <div className="field">
            <label htmlFor="username">Username</label>
            <input id="username" autoComplete="username" autoFocus value={username}
              onChange={(e) => setUsername(e.target.value)} required />
          </div>
          <div className="field">
            <label htmlFor="password">Password</label>
            <input id="password" type="password" autoComplete="current-password" value={password}
              onChange={(e) => setPassword(e.target.value)} required />
          </div>
          {error && <p className="form-error" role="alert">{error}</p>}
          <button className="btn primary block" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
        </form>
        <p className="hint">Forgot your password? Ask an administrator to reset it for you.</p>
      </section>
      <section className="login-art">
        <p className="art-title">University Management System</p>
        <Timetable />
      </section>
    </div>
  );
}
