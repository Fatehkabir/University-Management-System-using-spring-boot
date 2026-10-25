import { Link } from 'react-router-dom';
import { api } from '../../api.js';
import CrudPage from '../../components/CrudPage.jsx';
import { ErrorNote, Loading, PageHeader, Pill } from '../../components/ui.jsx';
import { fmtDate, useAsync } from '../../hooks.js';

const deptOptions = (lk) => (lk.departments || []).map((d) => ({ value: d.id, label: `${d.name} (${d.code})` }));
const activePill = (r) => (r.enabled ? <Pill tone="ok">Active</Pill> : <Pill tone="muted">Disabled</Pill>);

export function Overview() {
  const stats = useAsync(() => api.get('/admin/stats'), []);
  const notices = useAsync(() => api.get('/admin/announcements'), []);
  const s = stats.data;
  return (
    <>
      <PageHeader title="Overview" />
      <ErrorNote error={stats.error} onRetry={stats.reload} />
      {stats.loading && !s && <Loading />}
      {s && (
        <p className="lede">
          <Link to="/admin/teachers"><b>{s.teachers}</b> {s.teachers === 1 ? 'teacher' : 'teachers'}</Link> run{' '}
          <Link to="/admin/courses"><b>{s.courses}</b> {s.courses === 1 ? 'course' : 'courses'}</Link> across{' '}
          <Link to="/admin/departments"><b>{s.departments}</b> {s.departments === 1 ? 'department' : 'departments'}</Link>.{' '}
          <Link to="/admin/students"><b>{s.students}</b> {s.students === 1 ? 'student is' : 'students are'}</Link> registered, with{' '}
          <Link to="/admin/enrollments"><b>{s.enrollments}</b> {s.enrollments === 1 ? 'enrollment' : 'enrollments'}</Link> in total.
        </p>
      )}
      <h2>Latest announcements</h2>
      {notices.data && notices.data.length === 0 && (
        <p className="empty">Nothing posted yet. <Link to="/admin/announcements">Post an announcement</Link>.</p>
      )}
      <ul className="plain-list">
        {notices.data?.slice(0, 4).map((a) => (
          <li key={a.id}>
            <b>{a.title}</b>
            <span className="muted"> — {a.targetRole ? `for ${a.targetRole.toLowerCase()}s` : 'for everyone'}, {fmtDate(a.createdAt)}</span>
          </li>
        ))}
      </ul>
    </>
  );
}

export function Teachers() {
  return (
    <CrudPage
      title="Teachers"
      note="Create teacher accounts and keep their details up to date. Only administrators can change these."
      endpoint="/admin/teachers"
      itemName="teacher"
      label={(r) => r.fullName}
      lookups={{ departments: '/admin/departments' }}
      columns={[
        { label: 'Name', key: 'fullName' },
        { label: 'Employee ID', key: 'employeeId' },
        { label: 'Department', key: 'departmentName' },
        { label: 'Designation', key: 'designation' },
        { label: 'Email', key: 'email' },
        { label: 'Status', render: activePill },
      ]}
      fields={[
        { name: 'fullName', label: 'Full name', required: true },
        { name: 'username', label: 'Username', required: true },
        { name: 'password', label: 'Password', type: 'password', required: true, hint: 'At least 6 characters' },
        { name: 'email', label: 'Email', type: 'email', required: true },
        { name: 'employeeId', label: 'Employee ID', required: true },
        { name: 'departmentId', label: 'Department', type: 'select', number: true, required: true, options: deptOptions },
        { name: 'designation', label: 'Designation', hint: 'For example Lecturer or Professor' },
        { name: 'qualification', label: 'Qualification' },
        { name: 'phone', label: 'Phone' },
        { name: 'joiningDate', label: 'Joining date', type: 'date' },
        { name: 'address', label: 'Address', wide: true },
        { name: 'enabled', label: 'Account is active', type: 'checkbox', default: true },
      ]}
    />
  );
}

export function Students() {
  return (
    <CrudPage
      title="Students"
      note="Create student accounts and keep their details up to date. Only administrators can change these."
      endpoint="/admin/students"
      itemName="student"
      label={(r) => r.fullName}
      lookups={{ departments: '/admin/departments' }}
      columns={[
        { label: 'Name', key: 'fullName' },
        { label: 'Student ID', key: 'studentId' },
        { label: 'Department', key: 'departmentName' },
        { label: 'Batch', key: 'batch' },
        { label: 'Semester', key: 'semester' },
        { label: 'Status', render: activePill },
      ]}
      fields={[
        { name: 'fullName', label: 'Full name', required: true },
        { name: 'username', label: 'Username', required: true },
        { name: 'password', label: 'Password', type: 'password', required: true, hint: 'At least 6 characters' },
        { name: 'email', label: 'Email', type: 'email', required: true },
        { name: 'studentId', label: 'Student ID', required: true },
        { name: 'departmentId', label: 'Department', type: 'select', number: true, required: true, options: deptOptions },
        { name: 'batch', label: 'Batch', hint: 'For example 2022' },
        { name: 'semester', label: 'Semester', type: 'number', number: true, min: 1, default: '1' },
        { name: 'phone', label: 'Phone' },
        { name: 'address', label: 'Address', wide: true },
        { name: 'enabled', label: 'Account is active', type: 'checkbox', default: true },
      ]}
    />
  );
}

export function Departments() {
  return (
    <CrudPage
      title="Departments"
      note="A department can only be deleted once it has no teachers, students or courses."
      endpoint="/admin/departments"
      itemName="department"
      label={(r) => r.name}
      columns={[{ label: 'Name', key: 'name' }, { label: 'Code', key: 'code' }]}
      fields={[
        { name: 'name', label: 'Name', required: true },
        { name: 'code', label: 'Code', required: true, hint: 'Short code such as CSE' },
      ]}
    />
  );
}

export function Courses() {
  return (
    <CrudPage
      title="Courses"
      endpoint="/admin/courses"
      itemName="course"
      label={(r) => `${r.code} ${r.title}`}
      lookups={{ departments: '/admin/departments', teachers: '/admin/teachers' }}
      columns={[
        { label: 'Code', key: 'code' },
        { label: 'Title', key: 'title' },
        { label: 'Credits', key: 'creditHours' },
        { label: 'Department', key: 'departmentName' },
        { label: 'Teacher', render: (r) => r.teacherName || <span className="muted">Not assigned</span> },
      ]}
      fields={[
        { name: 'code', label: 'Course code', required: true, hint: 'For example CSE-3101' },
        { name: 'title', label: 'Title', required: true },
        { name: 'creditHours', label: 'Credit hours', type: 'number', number: true, step: '0.5', min: 0.5, required: true, default: '3' },
        { name: 'departmentId', label: 'Department', type: 'select', number: true, required: true, options: deptOptions },
        {
          name: 'teacherId', label: 'Teacher', type: 'select', number: true, emptyLabel: 'Not assigned',
          options: (lk) => (lk.teachers || []).map((t) => ({ value: t.id, label: `${t.fullName} (${t.departmentName})` })),
        },
      ]}
    />
  );
}

export function Enrollments() {
  return (
    <CrudPage
      title="Enrollments"
      note="Enroll a student in a course, or remove an enrollment. Removing one also deletes that student's attendance for the course."
      endpoint="/admin/enrollments"
      itemName="enrollment"
      canEdit={false}
      label={(r) => `${r.studentName} in ${r.courseCode}`}
      lookups={{ students: '/admin/students', courses: '/admin/courses' }}
      columns={[
        { label: 'Student', render: (r) => `${r.studentName} (${r.studentCode})` },
        { label: 'Course', render: (r) => `${r.courseCode} ${r.courseTitle}` },
        { label: 'Status', key: 'status', render: (r) => r.status.charAt(0) + r.status.slice(1).toLowerCase() },
        { label: 'Grade', render: (r) => r.grade || <span className="muted">Not graded</span> },
        { label: 'Enrolled', render: (r) => fmtDate(r.enrolledAt) },
      ]}
      fields={[
        {
          name: 'studentId', label: 'Student', type: 'select', number: true, required: true,
          options: (lk) => (lk.students || []).map((s) => ({ value: s.id, label: `${s.fullName} (${s.studentId})` })),
        },
        {
          name: 'courseId', label: 'Course', type: 'select', number: true, required: true,
          options: (lk) => (lk.courses || []).map((c) => ({ value: c.id, label: `${c.code} ${c.title}` })),
        },
      ]}
    />
  );
}

export function AnnouncementsAdmin() {
  return (
    <CrudPage
      title="Announcements"
      note="Choose who sees each notice: everyone, only teachers, or only students."
      endpoint="/admin/announcements"
      itemName="announcement"
      label={(r) => r.title}
      columns={[
        { label: 'Title', key: 'title' },
        { label: 'Audience', render: (r) => (r.targetRole ? `${r.targetRole.charAt(0)}${r.targetRole.slice(1).toLowerCase()}s` : 'Everyone') },
        { label: 'Posted by', key: 'postedByName' },
        { label: 'Date', render: (r) => fmtDate(r.createdAt) },
      ]}
      fields={[
        { name: 'title', label: 'Title', required: true, wide: true },
        { name: 'content', label: 'Message', type: 'textarea', required: true, wide: true },
        {
          name: 'targetRole', label: 'Audience', type: 'select', emptyLabel: 'Everyone',
          options: [{ value: 'TEACHER', label: 'Teachers only' }, { value: 'STUDENT', label: 'Students only' }],
        },
      ]}
    />
  );
}
