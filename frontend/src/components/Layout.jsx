import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth.jsx';

const NAV = {
  ADMIN: [
    { to: '/admin', label: 'Overview', end: true },
    { to: '/admin/teachers', label: 'Teachers' },
    { to: '/admin/students', label: 'Students' },
    { to: '/admin/departments', label: 'Departments' },
    { to: '/admin/courses', label: 'Courses' },
    { to: '/admin/enrollments', label: 'Enrollments' },
    { to: '/admin/announcements', label: 'Announcements' },
    { to: '/account', label: 'My account' },
  ],
  TEACHER: [
    { to: '/teacher', label: 'My courses', end: true },
    { to: '/announcements', label: 'Announcements' },
    { to: '/account', label: 'My account' },
  ],
  STUDENT: [
    { to: '/student', label: 'My courses', end: true },
    { to: '/student/register', label: 'Register for courses' },
    { to: '/student/attendance', label: 'Attendance' },
    { to: '/announcements', label: 'Announcements' },
    { to: '/account', label: 'My account' },
  ],
};

const ROLE_LABEL = { ADMIN: 'Administrator', TEACHER: 'Teacher', STUDENT: 'Student' };

export default function Layout() {
  const { user, logout } = useAuth();
  return (
    <div className={`shell role-${user.role.toLowerCase()}`}>
      <a className="skip" href="#main">Skip to content</a>
      <aside className="sidebar">
        <div className="brand">
          <svg width="28" height="28" viewBox="0 0 32 32" aria-hidden="true">
            <rect width="32" height="32" rx="6" fill="#0E5C57" />
            <path d="M6 12 16 7l10 5v2H6z" fill="#fff" />
            <rect x="8" y="16" width="3" height="7" fill="#E9B949" />
            <rect x="14.5" y="16" width="3" height="7" fill="#E9B949" />
            <rect x="21" y="16" width="3" height="7" fill="#E9B949" />
            <rect x="6" y="24" width="20" height="2" fill="#fff" />
          </svg>
          <span>University<br />Management</span>
        </div>
        <nav aria-label="Main">
          {NAV[user.role].map((n) => (
            <NavLink key={n.to} to={n.to} end={n.end} className={({ isActive }) => (isActive ? 'active' : '')}>
              {n.label}
            </NavLink>
          ))}
        </nav>
        <div className="who">
          <div className="who-name">{user.fullName}</div>
          <div className="who-role">{ROLE_LABEL[user.role]}</div>
          <button className="btn ghost" onClick={logout}>Sign out</button>
        </div>
      </aside>
      <main id="main" tabIndex={-1}>
        <Outlet />
      </main>
    </div>
  );
}
