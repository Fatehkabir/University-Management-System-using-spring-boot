
(function () {
  'use strict';
  const U = UMS;
  const { html, put, api, toast, num, plural } = U;

  const GRADES = ['A+', 'A', 'A-', 'B+', 'B', 'B-', 'C+', 'C', 'C-', 'D', 'F'];
  const MARKS = [['PRESENT', 'Present'], ['LATE', 'Late'], ['ABSENT', 'Absent'], ['EXCUSED', 'Excused']];
  const classUrl = (id) => `/teacher/course?id=${encodeURIComponent(id)}`;
  const pctOf = (part, whole) => (whole > 0 ? Math.round((part / whole) * 100) : null);

  U.pages.teacherDashboard = async function (main, user) {
    put(main, html`<div id="dash">${U.loadingNote()}</div>`);
    const box = main.querySelector('#dash');
    const load = async () => {
      let profile, stats, courses;
      try {
        [profile, stats, courses] = await Promise.all([api.get('/teacher/profile'), api.get('/teacher/stats'), api.get('/teacher/courses')]);
      } catch (e) { U.showError(box, e, load); return; }
      const subtitle = [profile.designation, profile.departmentName].filter(Boolean).join(', ');
      const cs = stats.courseStats;

      put(box, html`
        <div class="welcome"><span class="avatar" aria-hidden="true">${U.initials(profile.fullName)}</span>
          <div><h1>Welcome, ${profile.fullName}</h1>${subtitle ? html`<p>${subtitle}</p>` : ''}</div></div>
        <div class="kpis">
          ${U.kpi({ label: 'Courses you teach', value: num(stats.courses), icon: 'course', tone: 't-course' })}
          ${U.kpi({ label: 'Enrolled students', value: num(stats.students), sub: 'across your courses', icon: 'student', tone: 't-student' })}
          ${U.kpi({ label: 'Graded', value: num(stats.graded), unit: stats.students ? `/ ${num(stats.students)}` : '', sub: 'enrollments graded', icon: 'award', tone: 't-ok' })}
          ${U.kpi({ label: 'Attendance', value: stats.attendanceRate === null ? '–' : stats.attendanceRate, unit: stats.attendanceRate === null ? '' : '%', sub: 'average attendance', icon: 'calendar', tone: 't-enroll' })}
        </div>
        <h2 class="dash-title">Your courses</h2>
        ${courses.length === 0 ? U.emptyNote('You have no courses assigned yet. An administrator assigns courses to teachers.') : html`
          <div class="table-wrap"><table>
            <thead><tr><th>Code</th><th>Course Title</th><th>Department</th><th>Credits</th><th>Action</th></tr></thead>
            <tbody>${courses.map((c) => html`<tr><td><b>${c.code}</b></td><td>${c.title}</td><td>${c.departmentName}</td><td>${c.creditHours}</td>
              <td class="actions-col"><a class="btn primary" href="${classUrl(c.id)}">Open class & assignments</a></td></tr>`)}</tbody></table></div>`}`);
    };
    await load();
  };

  U.pages.teacherCourse = async function (main) {
    const id = new URLSearchParams(location.search).get('id');
    put(main, html`<p class="crumb"><a href="/teacher/">← Dashboard</a></p><div id="head">${U.loadingNote()}</div><div id="body"></div>`);
    const head = main.querySelector('#head');
    const body = main.querySelector('#body');
    let courses;
    try { courses = await api.get('/teacher/courses'); } catch (e) { U.showError(head, e, () => location.reload()); return; }
    const course = courses.find((c) => String(c.id) === String(id));
    if (!course) { put(head, U.pageHeader('Class')); put(body, U.emptyNote('You don’t teach this course.')); return; }
    document.title = `${course.code} · University Management System`;
    put(head, U.pageHeader(`${course.code} ${course.title}`));
    put(body, html`<div class="tabs" role="tablist" aria-label="Class tools">
        <button type="button" role="tab" id="tab-grades" aria-selected="true">Students & grades</button>
        <button type="button" role="tab" id="tab-assignments" aria-selected="false">Assignments & marks</button>
        <button type="button" role="tab" id="tab-att" aria-selected="false">Attendance</button></div>
      <div id="panel"></div>`);
    const panel = body.querySelector('#panel');
    const tabs = {
      grades: body.querySelector('#tab-grades'),
      assignments: body.querySelector('#tab-assignments'),
      att: body.querySelector('#tab-att')
    };
    const show = (which) => {
      tabs.grades.setAttribute('aria-selected', String(which === 'grades'));
      tabs.assignments.setAttribute('aria-selected', String(which === 'assignments'));
      tabs.att.setAttribute('aria-selected', String(which === 'att'));
      if (which === 'grades') gradesPanel(panel, id);
      else if (which === 'assignments') assignmentsPanel(panel, id);
      else attendancePanel(panel, id);
    };
    tabs.grades.addEventListener('click', () => show('grades'));
    tabs.assignments.addEventListener('click', () => show('assignments'));
    tabs.att.addEventListener('click', () => show('att'));
    show('grades');
  };

  async function gradesPanel(panel, courseId) {
    put(panel, U.loadingNote());
    const load = async () => {
      let roster;
      try { roster = await api.get(`/teacher/courses/${courseId}/students`); } catch (e) { U.showError(panel, e, load); return; }
      if (!roster.length) { put(panel, U.emptyNote('No students are enrolled in this course yet.')); return; }
      put(panel, html`<div class="table-wrap"><table>
        <thead><tr><th>Student</th><th>Student ID</th><th>Status</th><th>Grade</th></tr></thead>
        <tbody>${roster.map((e) => html`<tr><td>${e.studentName}</td><td>${e.studentCode}</td><td>${U.statusPill(e.status)}</td>
          <td><label class="sr-only" for="g-${e.id}">Grade for ${e.studentName}</label>
          <select id="g-${e.id}" class="compact" data-enrollment="${e.id}"${e.status === 'DROPPED' ? ' disabled' : ''}>
            <option value="">Not graded</option>
            ${GRADES.map((g) => html`<option value="${g}"${e.grade === g ? ' selected' : ''}>${g}</option>`)}
          </select></td></tr>`)}</tbody></table></div>
        <p class="hint">Saving a grade marks the enrollment as completed. Grades save as soon as you pick one.</p>`);
      panel.querySelectorAll('select[data-enrollment]').forEach((sel) => sel.addEventListener('change', async () => {
        const row = roster.find((r) => String(r.id) === sel.dataset.enrollment);
        try {
          await api.put(`/teacher/enrollments/${row.id}/grade`, { grade: sel.value });
          toast(sel.value ? `Saved ${sel.value} for ${row.studentName}` : `Cleared grade for ${row.studentName}`);
          load();
        } catch (e) { toast(e.message, 'error'); load(); }
      }));
    };
    await load();
  }

  async function attendancePanel(panel, courseId) {
    let date = U.todayISO();
    put(panel, html`
      <div class="toolbar">
        <label class="inline-field"><span>Date</span><input type="date" id="att-date" value="${date}" max="${U.todayISO()}"></label>
        <button type="button" class="btn" id="all-present">Mark everyone present</button>
        <span class="muted" id="saved-note"></span>
      </div>
      <div id="att-area">${U.loadingNote()}</div>`);
    const area = panel.querySelector('#att-area');
    const note = panel.querySelector('#saved-note');
    let active = [];

    const load = async () => {
      put(area, U.loadingNote());
      let roster, existing;
      try {
        [roster, existing] = await Promise.all([
          api.get(`/teacher/courses/${courseId}/students`),
          api.get(`/teacher/courses/${courseId}/attendance?date=${date}`),
        ]);
      } catch (e) { U.showError(area, e, load); return; }
      active = roster.filter((e) => e.status === 'ACTIVE');
      note.textContent = existing.length ? 'Attendance for this date was already saved. Saving again updates it.' : '';
      if (!active.length) { put(area, U.emptyNote('No active students to take attendance for.')); return; }
      const saved = Object.fromEntries(existing.map((a) => [a.studentId, a.status]));
      put(area, html`<div class="table-wrap"><table>
        <thead><tr><th>Student</th><th>Student ID</th><th>Attendance</th></tr></thead>
        <tbody>${active.map((e) => html`<tr><td>${e.studentName}</td><td>${e.studentCode}</td><td>
          <fieldset class="segmented"><legend class="sr-only">Attendance for ${e.studentName}</legend>
          ${MARKS.map(([v, text]) => html`<label class="seg ${v.toLowerCase()}"><input type="radio" name="a-${e.studentId}" value="${v}"${(saved[e.studentId] || 'PRESENT') === v ? ' checked' : ''}>${text}</label>`)}
          </fieldset></td></tr>`)}</tbody></table></div>
        <div class="form-actions left"><button type="button" class="btn primary" id="save-att">Save attendance</button></div>`);
      area.querySelector('#save-att').addEventListener('click', save);
    };

    const save = async () => {
      const btn = area.querySelector('#save-att');
      btn.disabled = true;
      try {
        await api.post(`/teacher/courses/${courseId}/attendance`, {
          date,
          entries: active.map((e) => ({ studentId: e.studentId, status: (area.querySelector(`input[name="a-${e.studentId}"]:checked`) || {}).value || 'PRESENT' })),
        });
        toast(`Attendance saved for ${date}`);
        load();
      } catch (e) { toast(e.message, 'error'); btn.disabled = false; }
    };

    panel.querySelector('#att-date').addEventListener('change', (e) => { if (e.target.value) { date = e.target.value; load(); } });
    panel.querySelector('#all-present').addEventListener('click', () => {
      area.querySelectorAll('input[type=radio][value=PRESENT]').forEach((r) => { r.checked = true; });
    });
    await load();
  }

  async function assignmentsPanel(panel, courseId) {
    put(panel, html`
      <div class="toolbar">
        <button type="button" class="btn primary" id="btn-create-assign">+ Create assignment</button>
      </div>
      <div id="assign-area">${U.loadingNote()}</div>`);
    const area = panel.querySelector('#assign-area');
    const btnCreate = panel.querySelector('#btn-create-assign');

    const load = async () => {
      put(area, U.loadingNote());
      let list;
      try {
        list = await api.get(`/teacher/courses/${courseId}/assignments`);
      } catch (e) { U.showError(area, e, load); return; }

      if (!list.length) {
        put(area, U.emptyNote('No assignments created yet for this course. Click "+ Create assignment" above to add one.'));
        return;
      }

      put(area, html`
        <div class="table-wrap"><table>
          <thead><tr><th>Title</th><th>Max Marks</th><th>Due Date</th><th>Graded</th><th>Actions</th></tr></thead>
          <tbody>${list.map((a) => html`<tr>
            <td><b>${a.title}</b>${a.description ? html`<br><small class="muted">${a.description}</small>` : ''}</td>
            <td>${a.maxMarks}</td>
            <td>${a.dueDate ? U.fmtDate(a.dueDate) : html`<span class="muted">No deadline</span>`}</td>
            <td>${a.submissionsCount}</td>
            <td class="actions-col">
              <button type="button" class="btn link" data-view-marks="${a.id}">Grade / Marks</button>
              <button type="button" class="btn link danger-text" data-del-assign="${a.id}">Delete</button>
            </td>
          </tr>`)}</tbody>
        </table></div>
        <div id="assign-marks-modal-slot"></div>`);

      area.querySelectorAll('[data-view-marks]').forEach((btn) => {
        btn.addEventListener('click', () => {
          const a = list.find((x) => String(x.id) === btn.dataset.viewMarks);
          openAssignmentMarksDialog(courseId, a);
        });
      });

      area.querySelectorAll('[data-del-assign]').forEach((btn) => {
        btn.addEventListener('click', () => {
          const a = list.find((x) => String(x.id) === btn.dataset.delAssign);
          U.confirmDialog({
            title: `Delete assignment "${a.title}"?`,
            message: 'All recorded student marks for this assignment will also be permanently deleted.',
            confirmLabel: 'Delete assignment',
            onConfirm: async () => {
              try {
                await api.del(`/teacher/courses/${courseId}/assignments/${a.id}`);
                toast(`Deleted ${a.title}`);
                load();
              } catch (e) { toast(e.message, 'error'); }
            }
          });
        });
      });
    };

    btnCreate.addEventListener('click', () => {
      U.formDialog({
        title: 'Create assignment',
        submitLabel: 'Create assignment',
        fields: [
          { name: 'title', label: 'Assignment Title', required: true, maxlength: 120 },
          { name: 'maxMarks', label: 'Maximum Marks', type: 'number', number: true, step: '0.01', min: 0, required: true },
          { name: 'dueDate', label: 'Due Date', type: 'date' },
          { name: 'description', label: 'Instructions / Description', type: 'textarea', wide: true }
        ],
        onSubmit: async (payload) => {
          return await api.post(`/teacher/courses/${courseId}/assignments`, payload);
        },
        onDone: () => {
          toast('Assignment created successfully');
          load();
        }
      });
    });

    await load();
  }

  function openAssignmentMarksDialog(courseId, assignment) {
    const dlg = U.openDialog(`Grade Marks: ${assignment.title} (Max: ${assignment.maxMarks})`, html`
      <div id="marks-dialog-body">${U.loadingNote()}</div>
    `, { wide: true });

    const body = dlg.content.querySelector('#marks-dialog-body');
    const loadScores = async () => {
      put(body, U.loadingNote());
      let scores;
      try {
        scores = await api.get(`/teacher/courses/${courseId}/assignments/${assignment.id}/scores`);
      } catch (e) { U.showError(body, e, loadScores); return; }

      if (!scores.length) {
        put(body, U.emptyNote('No students enrolled in this course to grade.'));
        return;
      }

      put(body, html`
        <div class="table-wrap"><table>
          <thead><tr><th>Student</th><th>ID</th><th>Obtained Marks</th><th>Feedback</th><th>Action</th></tr></thead>
          <tbody>${scores.map((s) => html`<tr data-student="${s.studentId}">
            <td>${s.studentName}</td>
            <td>${s.studentCode}</td>
            <td><input type="number" class="compact marks-input" step="0.01" min="0" max="${assignment.maxMarks}" value="${s.obtainedMarks !== null ? s.obtainedMarks : ''}" placeholder="0 - ${assignment.maxMarks}" style="width:110px"></td>
            <td><input type="text" class="compact feedback-input" value="${s.feedback || ''}" placeholder="Feedback (optional)"></td>
            <td><button type="button" class="btn link save-score-btn">Save</button></td>
          </tr>`)}</tbody>
        </table></div>`);

      body.querySelectorAll('.save-score-btn').forEach((btn) => {
        btn.addEventListener('click', async () => {
          const row = btn.closest('tr');
          const studentId = row.dataset.student;
          const marksInput = row.querySelector('.marks-input');
          const feedbackInput = row.querySelector('.feedback-input');
          const val = marksInput.value.trim();

          if (val === '') {
            toast('Please enter obtained marks before saving', 'error');
            return;
          }
          const numVal = Number(val);
          if (isNaN(numVal) || numVal < 0 || numVal > assignment.maxMarks) {
            toast(`Marks must be between 0 and ${assignment.maxMarks}`, 'error');
            return;
          }

          btn.disabled = true;
          try {
            await api.put(`/teacher/courses/${courseId}/assignments/${assignment.id}/scores`, {
              studentId: Number(studentId),
              obtainedMarks: numVal,
              feedback: feedbackInput.value.trim() || null
            });
            toast('Mark saved');
            btn.disabled = false;
          } catch (e) {
            toast(e.message, 'error');
            btn.disabled = false;
          }
        });
      });
    };

    loadScores();
  }
})();

