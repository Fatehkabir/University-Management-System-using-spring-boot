import { Navigate, Route, Routes } from 'react-router-dom';
import { homeFor, useAuth } from './auth.jsx';
import Layout from './components/Layout.jsx';
import Login from './pages/Login.jsx';
import { Account, Announcements } from './pages/Shared.jsx';
import {
  AnnouncementsAdmin, Courses, Departments, Enrollments, Overview, Students, Teachers,
} from './pages/admin/Admin.jsx';
import { CourseClass, MyCourses as TeacherCourses } from './pages/teacher/Teacher.jsx';
import { MyAttendance, MyCourses as StudentCourses, Register } from './pages/student/Student.jsx';

function RequireAuth({ children }) {
  const { user } = useAuth();
  return user ? children : <Navigate to="/login" replace />;
}

function RequireRole({ role, children }) {
  const { user } = useAuth();
  return user.role === role ? children : <Navigate to={homeFor(user.role)} replace />;
}

const admin = (el) => <RequireRole role="ADMIN">{el}</RequireRole>;
const teacher = (el) => <RequireRole role="TEACHER">{el}</RequireRole>;
const student = (el) => <RequireRole role="STUDENT">{el}</RequireRole>;

function Home() {
  const { user } = useAuth();
  return <Navigate to={user ? homeFor(user.role) : '/login'} replace />;
}

export default function App() {
  const { ready } = useAuth();
  if (!ready) return <p className="boot muted" role="status">Loading…</p>;

  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<RequireAuth><Layout /></RequireAuth>}>
        <Route path="/admin" element={admin(<Overview />)} />
        <Route path="/admin/teachers" element={admin(<Teachers />)} />
        <Route path="/admin/students" element={admin(<Students />)} />
        <Route path="/admin/departments" element={admin(<Departments />)} />
        <Route path="/admin/courses" element={admin(<Courses />)} />
        <Route path="/admin/enrollments" element={admin(<Enrollments />)} />
        <Route path="/admin/announcements" element={admin(<AnnouncementsAdmin />)} />

        <Route path="/teacher" element={teacher(<TeacherCourses />)} />
        <Route path="/teacher/courses/:id" element={teacher(<CourseClass />)} />

        <Route path="/student" element={student(<StudentCourses />)} />
        <Route path="/student/register" element={student(<Register />)} />
        <Route path="/student/attendance" element={student(<MyAttendance />)} />

        <Route path="/announcements" element={<Announcements />} />
        <Route path="/account" element={<Account />} />
      </Route>
      <Route path="*" element={<Home />} />
    </Routes>
  );
}
