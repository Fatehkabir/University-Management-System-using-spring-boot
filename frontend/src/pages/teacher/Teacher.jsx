import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../../api.js';
import { Empty, ErrorNote, Loading, PageHeader, StatusPill } from '../../components/ui.jsx';
import { todayISO, useAsync } from '../../hooks.js';
import { useToast } from '../../toast.jsx';

const GRADES = ['A+', 'A', 'A-', 'B+', 'B', 'B-', 'C+', 'C', 'C-', 'D', 'F'];
const ATTENDANCE = [
  ['PRESENT', 'Present'], ['LATE', 'Late'], ['ABSENT', 'Absent'], ['EXCUSED', 'Excused'],
];

export function MyCourses() {
  const profile = useAsync(() => api.get('/teacher/profile'), []);
  const courses = useAsync(() => api.get('/teacher/courses'), []);
  const p = profile.data;
  return (
    <>
      <PageHeader
        title={p ? `Welcome, ${p.fullName}` : 'My courses'}
        note={p ? [p.designation, p.departmentName].filter(Boolean).join(', ') : undefined}
      />
      <h2>Courses you teach</h2>
      <ErrorNote error={courses.error} onRetry={courses.reload} />
      {courses.loading && !courses.data && <Loading />}
      {courses.data && courses.data.length === 0 && (
        <Empty>You have no courses yet. An administrator assigns courses to teachers.</Empty>
      )}
      {courses.data && courses.data.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead><tr><th>Code</th><th>Title</th><th>Department</th><th>Credits</th><th /></tr></thead>
            <tbody>
              {courses.data.map((c) => (
                <tr key={c.id}>
                  <td>{c.code}</td><td>{c.title}</td><td>{c.departmentName}</td><td>{c.creditHours}</td>
                  <td className="actions-col"><Link className="btn link" to={`/teacher/courses/${c.id}`}>Open class</Link></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}

export function CourseClass() {
  const { id } = useParams();
  const courses = useAsync(() => api.get('/teacher/courses'), []);
  const [tab, setTab] = useState('grades');
  const course = courses.data?.find((c) => String(c.id) === id);

  return (
    <>
      <p className="crumb"><Link to="/teacher">My courses</Link></p>
      <PageHeader title={course ? `${course.code} ${course.title}` : 'Class'} />
      <ErrorNote error={courses.error} onRetry={courses.reload} />
      {courses.data && !course && <Empty>You don't teach this course.</Empty>}
      {course && (
        <>
          <div className="tabs" role="tablist" aria-label="Class tools">
            <button role="tab" aria-selected={tab === 'grades'} className={tab === 'grades' ? 'on' : ''} onClick={() => setTab('grades')}>
              Students and grades
            </button>
            <button role="tab" aria-selected={tab === 'attendance'} className={tab === 'attendance' ? 'on' : ''} onClick={() => setTab('attendance')}>
              Attendance
            </button>
          </div>
          {tab === 'grades' ? <GradesPanel courseId={id} /> : <AttendancePanel courseId={id} />}
        </>
      )}
    </>
  );
}

function GradesPanel({ courseId }) {
  const toast = useToast();
  const roster = useAsync(() => api.get(`/teacher/courses/${courseId}/students`), [courseId]);

  const setGrade = async (enrollment, grade) => {
    try {
      await api.put(`/teacher/enrollments/${enrollment.id}/grade`, { grade });
      toast(grade ? `Saved ${grade} for ${enrollment.studentName}` : `Cleared grade for ${enrollment.studentName}`);
      roster.reload();
    } catch (e) {
      toast(e.message, 'error');
    }
  };

  return (
    <>
      <ErrorNote error={roster.error} onRetry={roster.reload} />
      {roster.loading && !roster.data && <Loading />}
      {roster.data && roster.data.length === 0 && <Empty>No students are enrolled in this course yet.</Empty>}
      {roster.data && roster.data.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead><tr><th>Student</th><th>Student ID</th><th>Status</th><th>Grade</th></tr></thead>
            <tbody>
              {roster.data.map((e) => (
                <tr key={e.id}>
                  <td>{e.studentName}</td>
                  <td>{e.studentCode}</td>
                  <td><StatusPill status={e.status} /></td>
                  <td>
                    <label className="sr-only" htmlFor={`g-${e.id}`}>Grade for {e.studentName}</label>
                    <select id={`g-${e.id}`} className="compact" value={e.grade || ''} disabled={e.status === 'DROPPED'}
                      onChange={(ev) => setGrade(e, ev.target.value)}>
                      <option value="">Not graded</option>
                      {GRADES.map((g) => <option key={g} value={g}>{g}</option>)}
                    </select>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <p className="hint">Saving a grade marks the enrollment as completed. Grades save as soon as you pick one.</p>
    </>
  );
}

function AttendancePanel({ courseId }) {
  const toast = useToast();
  const [date, setDate] = useState(todayISO());
  const roster = useAsync(() => api.get(`/teacher/courses/${courseId}/students`), [courseId]);
  const existing = useAsync(() => api.get(`/teacher/courses/${courseId}/attendance?date=${date}`), [courseId, date]);
  const [marks, setMarks] = useState({});
  const [saving, setSaving] = useState(false);

  const active = (roster.data || []).filter((e) => e.status === 'ACTIVE');

  // Start from what was already saved for this date; everyone else defaults to present.
  useEffect(() => {
    if (!roster.data || !existing.data) return;
    const saved = Object.fromEntries(existing.data.map((a) => [a.studentId, a.status]));
    setMarks(Object.fromEntries(active.map((e) => [e.studentId, saved[e.studentId] || 'PRESENT'])));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roster.data, existing.data]);

  const save = async () => {
    setSaving(true);
    try {
      await api.post(`/teacher/courses/${courseId}/attendance`, {
        date,
        entries: active.map((e) => ({ studentId: e.studentId, status: marks[e.studentId] || 'PRESENT' })),
      });
      toast(`Attendance saved for ${date}`);
      existing.reload();
    } catch (e) {
      toast(e.message, 'error');
    } finally {
      setSaving(false);
    }
  };

  const allPresent = () => setMarks(Object.fromEntries(active.map((e) => [e.studentId, 'PRESENT'])));
  const wasSaved = (existing.data || []).length > 0;

  return (
    <>
      <div className="toolbar">
        <label className="inline-field">
          <span>Date</span>
          <input type="date" value={date} max={todayISO()} onChange={(e) => e.target.value && setDate(e.target.value)} />
        </label>
        <button className="btn" onClick={allPresent} disabled={!active.length}>Mark everyone present</button>
        {wasSaved && <span className="muted">Attendance for this date was already saved. Saving again updates it.</span>}
      </div>
      <ErrorNote error={roster.error || existing.error} onRetry={() => { roster.reload(); existing.reload(); }} />
      {roster.loading && !roster.data && <Loading />}
      {roster.data && active.length === 0 && <Empty>No active students to take attendance for.</Empty>}
      {active.length > 0 && (
        <>
          <div className="table-wrap">
            <table>
              <thead><tr><th>Student</th><th>Student ID</th><th>Attendance</th></tr></thead>
              <tbody>
                {active.map((e) => (
                  <tr key={e.id}>
                    <td>{e.studentName}</td>
                    <td>{e.studentCode}</td>
                    <td>
                      <fieldset className="segmented">
                        <legend className="sr-only">Attendance for {e.studentName}</legend>
                        {ATTENDANCE.map(([value, text]) => (
                          <label key={value} className={`seg ${value.toLowerCase()}${marks[e.studentId] === value ? ' on' : ''}`}>
                            <input type="radio" name={`a-${e.studentId}`} value={value}
                              checked={marks[e.studentId] === value}
                              onChange={() => setMarks((m) => ({ ...m, [e.studentId]: value }))} />
                            {text}
                          </label>
                        ))}
                      </fieldset>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="form-actions left">
            <button className="btn primary" onClick={save} disabled={saving}>{saving ? 'Saving…' : 'Save attendance'}</button>
          </div>
        </>
      )}
    </>
  );
}
