import { useMemo, useState } from 'react';
import { api } from '../../api.js';
import { ConfirmDialog, Empty, ErrorNote, Loading, PageHeader, StatusPill } from '../../components/ui.jsx';
import { fmtDate, useAsync } from '../../hooks.js';
import { useToast } from '../../toast.jsx';

export function MyCourses() {
  const toast = useToast();
  const profile = useAsync(() => api.get('/student/profile'), []);
  const enrollments = useAsync(() => api.get('/student/enrollments'), []);
  const [dropping, setDropping] = useState(null);
  const [busy, setBusy] = useState(false);
  const p = profile.data;

  const drop = async () => {
    setBusy(true);
    try {
      await api.del(`/student/enrollments/${dropping.id}`);
      toast(`Dropped ${dropping.courseCode}`);
      setDropping(null);
      enrollments.reload();
    } catch (e) {
      toast(e.message, 'error');
      setDropping(null);
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <PageHeader
        title={p ? `Welcome, ${p.fullName}` : 'My courses'}
        note={p ? `${p.departmentName}, semester ${p.semester}${p.batch ? `, batch ${p.batch}` : ''}` : undefined}
      />
      <h2>Your courses and results</h2>
      <ErrorNote error={enrollments.error} onRetry={enrollments.reload} />
      {enrollments.loading && !enrollments.data && <Loading />}
      {enrollments.data && enrollments.data.length === 0 && (
        <Empty>You aren't enrolled in any course yet. Open “Register for courses” to add one.</Empty>
      )}
      {enrollments.data && enrollments.data.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead><tr><th>Course</th><th>Teacher</th><th>Status</th><th>Grade</th><th /></tr></thead>
            <tbody>
              {enrollments.data.map((e) => (
                <tr key={e.id}>
                  <td>{e.courseCode} {e.courseTitle}</td>
                  <td>{e.teacherName || <span className="muted">Not assigned</span>}</td>
                  <td><StatusPill status={e.status} /></td>
                  <td>{e.grade || <span className="muted">Not graded</span>}</td>
                  <td className="actions-col">
                    {e.status === 'ACTIVE' && (
                      <button className="btn link danger-text" onClick={() => setDropping(e)}>Drop</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {dropping && (
        <ConfirmDialog
          title="Drop this course?"
          message={`You will be removed from ${dropping.courseCode} ${dropping.courseTitle}. You can register again later.`}
          confirmLabel="Drop course"
          busy={busy}
          onConfirm={drop}
          onClose={() => setDropping(null)}
        />
      )}
    </>
  );
}

export function Register() {
  const toast = useToast();
  const courses = useAsync(() => api.get('/courses'), []);
  const enrollments = useAsync(() => api.get('/student/enrollments'), []);
  const [busyId, setBusyId] = useState(null);

  const taken = useMemo(
    () => new Map((enrollments.data || []).filter((e) => e.status !== 'DROPPED').map((e) => [e.courseId, e.status])),
    [enrollments.data],
  );

  const enroll = async (c) => {
    setBusyId(c.id);
    try {
      await api.post('/student/enrollments', { courseId: c.id });
      toast(`Enrolled in ${c.code}`);
      enrollments.reload();
    } catch (e) {
      toast(e.message, 'error');
    } finally {
      setBusyId(null);
    }
  };

  return (
    <>
      <PageHeader title="Register for courses" note="Pick any course from the catalogue. You can drop it later from My courses." />
      <ErrorNote error={courses.error || enrollments.error} onRetry={() => { courses.reload(); enrollments.reload(); }} />
      {courses.loading && !courses.data && <Loading />}
      {courses.data && courses.data.length === 0 && <Empty>No courses are available yet.</Empty>}
      {courses.data && courses.data.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead><tr><th>Code</th><th>Title</th><th>Department</th><th>Teacher</th><th>Credits</th><th /></tr></thead>
            <tbody>
              {courses.data.map((c) => (
                <tr key={c.id}>
                  <td>{c.code}</td><td>{c.title}</td><td>{c.departmentName}</td>
                  <td>{c.teacherName || <span className="muted">Not assigned</span>}</td>
                  <td>{c.creditHours}</td>
                  <td className="actions-col">
                    {taken.has(c.id)
                      ? <StatusPill status={taken.get(c.id)} />
                      : <button className="btn link" disabled={busyId === c.id} onClick={() => enroll(c)}>Enroll</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}

export function MyAttendance() {
  const list = useAsync(() => api.get('/student/attendance'), []);

  const summary = useMemo(() => {
    const by = new Map();
    (list.data || []).forEach((a) => {
      const s = by.get(a.courseCode) || { code: a.courseCode, total: 0, attended: 0 };
      s.total += 1;
      if (a.status === 'PRESENT' || a.status === 'LATE') s.attended += 1;
      by.set(a.courseCode, s);
    });
    return [...by.values()];
  }, [list.data]);

  return (
    <>
      <PageHeader title="Attendance" note="Present and late both count as attended." />
      <ErrorNote error={list.error} onRetry={list.reload} />
      {list.loading && !list.data && <Loading />}
      {list.data && list.data.length === 0 && <Empty>No attendance has been recorded for you yet.</Empty>}
      {summary.length > 0 && (
        <>
          <h2>By course</h2>
          <div className="table-wrap">
            <table>
              <thead><tr><th>Course</th><th>Classes</th><th>Attended</th><th>Attendance</th></tr></thead>
              <tbody>
                {summary.map((s) => {
                  const pct = Math.round((s.attended / s.total) * 100);
                  return (
                    <tr key={s.code}>
                      <td>{s.code}</td><td>{s.total}</td><td>{s.attended}</td>
                      <td>
                        <span className="meter" role="img" aria-label={`${pct} percent`}>
                          <span style={{ width: `${pct}%` }} className={pct < 75 ? 'low' : ''} />
                        </span>
                        {pct}%
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          <h2>Every class</h2>
          <div className="table-wrap">
            <table>
              <thead><tr><th>Date</th><th>Course</th><th>Status</th></tr></thead>
              <tbody>
                {list.data.map((a) => (
                  <tr key={a.id}><td>{fmtDate(a.date)}</td><td>{a.courseCode}</td><td><StatusPill status={a.status} /></td></tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </>
  );
}
