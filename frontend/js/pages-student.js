
(function () {
  'use strict';
  const U = UMS;
  const { html, put, api, toast, num, fmtDate } = U;

  const ATTENDED = new Set(['PRESENT', 'LATE']);

  function attendanceByCourse(list) {
    const by = new Map();
    list.forEach((a) => {
      const s = by.get(a.courseCode) || { code: a.courseCode, total: 0, attended: 0 };
      s.total += 1;
      if (ATTENDED.has(a.status)) s.attended += 1;
      by.set(a.courseCode, s);
    });
    return [...by.values()].map((s) => ({ ...s, pct: Math.round((s.attended / s.total) * 100) }));
  }

  U.pages.studentDashboard = async function (main) {
    put(main, html`<div id="dash">${U.loadingNote()}</div>`);
    const box = main.querySelector('#dash');
    const load = async () => {
      let profile, enrollments, attendance;
      try {
        [profile, enrollments, attendance] = await Promise.all([
          api.get('/student/profile'), api.get('/student/enrollments'), api.get('/student/attendance')]);
      } catch (e) { U.showError(box, e, load); return; }

      const active = enrollments.filter((e) => e.status === 'ACTIVE');
      const completed = enrollments.filter((e) => e.status === 'COMPLETED');
      const dropped = enrollments.filter((e) => e.status === 'DROPPED');
      const credits = active.reduce((a, e) => a + (e.creditHours || 0), 0);
      const perCourse = attendanceByCourse(attendance);
      const attended = attendance.filter((a) => ATTENDED.has(a.status)).length;
      const overall = attendance.length ? Math.round((attended / attendance.length) * 100) : null;
      const sub = `${profile.departmentName}, semester ${profile.semester}${profile.batch ? `, batch ${profile.batch}` : ''}`;

      put(box, html`
        <div class="welcome"><span class="avatar" aria-hidden="true">${U.initials(profile.fullName)}</span>
          <div><h1>Welcome, ${profile.fullName}</h1><p>${sub}</p></div></div>
        <div class="kpis">
          ${U.kpi({ label: 'Active courses', value: num(active.length), icon: 'course', tone: 't-course', href: '#my-courses' })}
          ${U.kpi({ label: 'Completed courses', value: num(completed.length), sub: 'graded courses', icon: 'award', tone: 't-ok' })}
          ${U.kpi({ label: 'Credit hours', value: num(credits), sub: 'in active courses', icon: 'layers', tone: 't-dept' })}
          ${U.kpi({ label: 'Attendance', value: overall === null ? '–' : overall, unit: overall === null ? '' : '%', sub: overall === null ? 'not recorded yet' : 'present or late', icon: 'calendar', tone: 't-enroll', href: '/student/attendance' })}
        </div>
        <h2 class="dash-title" id="my-courses">Your courses and results</h2>
        <div id="course-table"></div>`);
      drawCourses(box.querySelector('#course-table'), enrollments, load);
    };
    await load();
  };

  function drawCourses(el, enrollments, reload) {
    if (!enrollments.length) {
      put(el, U.emptyNote('You aren’t enrolled in any course yet. Open “Register for courses” to add one.'));
      return;
    }
    put(el, html`<div class="table-wrap"><table>
      <thead><tr><th>Course</th><th>Teacher</th><th>Status</th><th>Grade</th><th></th></tr></thead>
      <tbody>${enrollments.map((e) => html`<tr><td>${e.courseCode} ${e.courseTitle}</td>
        <td>${e.teacherName || html`<span class="muted">Not assigned</span>`}</td>
        <td>${U.statusPill(e.status)}</td><td>${e.grade || html`<span class="muted">Not graded</span>`}</td>
        <td class="actions-col">${e.status === 'ACTIVE' ? html`<button type="button" class="btn link danger-text" data-drop="${e.id}">Drop</button>` : ''}</td></tr>`)}
      </tbody></table></div>`);
    el.querySelectorAll('[data-drop]').forEach((b) => b.addEventListener('click', () => {
      const e = enrollments.find((x) => String(x.id) === b.dataset.drop);
      U.confirmDialog({
        title: 'Drop this course?', confirmLabel: 'Drop course',
        message: `You will be removed from ${e.courseCode} ${e.courseTitle}. You can register again later.`,
        onConfirm: async () => {
          try { await api.del(`/student/enrollments/${e.id}`); toast(`Dropped ${e.courseCode}`); reload(); }
          catch (ex) { toast(ex.message, 'error'); }
        },
      });
    }));
  }

  U.pages.studentRegister = async function (main) {
    put(main, html`${U.pageHeader('Register for courses', 'Pick any course from the catalogue. You can drop it later from your dashboard.')}<div id="area">${U.loadingNote()}</div>`);
    const area = main.querySelector('#area');
    const load = async () => {
      let courses, enrollments;
      try { [courses, enrollments] = await Promise.all([api.get('/courses'), api.get('/student/enrollments')]); }
      catch (e) { U.showError(area, e, load); return; }
      const taken = new Map(enrollments.filter((e) => e.status !== 'DROPPED').map((e) => [e.courseId, e.status]));
      if (!courses.length) { put(area, U.emptyNote('No courses are available yet.')); return; }
      put(area, html`<div class="table-wrap"><table>
        <thead><tr><th>Code</th><th>Title</th><th>Department</th><th>Teacher</th><th>Credits</th><th></th></tr></thead>
        <tbody>${courses.map((c) => html`<tr><td>${c.code}</td><td>${c.title}</td><td>${c.departmentName}</td>
          <td>${c.teacherName || html`<span class="muted">Not assigned</span>`}</td><td>${c.creditHours}</td>
          <td class="actions-col">${taken.has(c.id) ? U.statusPill(taken.get(c.id)) : html`<button type="button" class="btn link" data-enroll="${c.id}">Enroll</button>`}</td></tr>`)}
        </tbody></table></div>`);
      area.querySelectorAll('[data-enroll]').forEach((b) => b.addEventListener('click', async () => {
        const c = courses.find((x) => String(x.id) === b.dataset.enroll);
        b.disabled = true;
        try { await api.post('/student/enrollments', { courseId: c.id }); toast(`Enrolled in ${c.code}`); load(); }
        catch (e) { toast(e.message, 'error'); b.disabled = false; }
      }));
    };
    await load();
  };

  U.pages.studentAttendance = async function (main) {
    put(main, html`${U.pageHeader('Attendance', 'Present and late both count as attended.')}<div id="area">${U.loadingNote()}</div>`);
    const area = main.querySelector('#area');
    const load = async () => {
      let list;
      try { list = await api.get('/student/attendance'); } catch (e) { U.showError(area, e, load); return; }
      if (!list.length) { put(area, U.emptyNote('No attendance has been recorded for you yet.')); return; }
      const per = attendanceByCourse(list);
      put(area, html`
        <div class="panels"><section class="panel wide"><h2>By course</h2>
          <ul class="meter-list">${per.map((s) => U.meterRow({ label: s.code, pct: s.pct, text: `${s.pct}% (${s.attended}/${s.total})`, warnBelow: 75 }))}</ul></section></div>
        <h2>Every class</h2>
        <div class="table-wrap"><table><thead><tr><th>Date</th><th>Course</th><th>Status</th></tr></thead>
        <tbody>${list.map((a) => html`<tr><td>${fmtDate(a.date)}</td><td>${a.courseCode}</td><td>${U.statusPill(a.status)}</td></tr>`)}</tbody></table></div>`);
    };
    await load();
  };

  U.pages.studentAssignments = async function (main) {
    put(main, html`${U.pageHeader('Assignments & Marks', 'View assignments and marks posted by your teachers.')}<div id="area">${U.loadingNote()}</div>`);
    const area = main.querySelector('#area');
    const load = async () => {
      let list;
      try { list = await api.get('/student/assignments'); } catch (e) { U.showError(area, e, load); return; }
      if (!list.length) { put(area, U.emptyNote('No assignments found for your enrolled courses.')); return; }
      put(area, html`
        <div class="table-wrap"><table>
          <thead><tr><th>Course</th><th>Assignment</th><th>Due Date</th><th>Marks</th><th>Feedback</th></tr></thead>
          <tbody>${list.map((a) => html`<tr>
            <td><b>${a.courseCode}</b><br><small class="muted">${a.courseTitle}</small></td>
            <td><b>${a.title}</b>${a.description ? html`<br><small class="muted">${a.description}</small>` : ''}</td>
            <td>${a.dueDate ? U.fmtDate(a.dueDate) : html`<span class="muted">No deadline</span>`}</td>
            <td>${a.obtainedMarks !== null ? html`<b>${a.obtainedMarks}</b> / ${a.maxMarks}` : html`<span class="muted">Not graded (Max ${a.maxMarks})</span>`}</td>
            <td>${a.feedback ? a.feedback : html`<span class="muted">—</span>`}</td>
          </tr>`)}</tbody>
        </table></div>`);
    };
    await load();
  };
})();

