
(function () {
  'use strict';
  const U = UMS;
  const { html, put, api, crud, pill, fmtDate, toast, plural, num } = U;

  const deptOptions = (lk) => (lk.departments || []).map((d) => ({ value: d.id, label: `${d.name} (${d.code})` }));
  const activePill = (r) => (r.enabled ? pill('ok', 'Active') : pill('muted', 'Disabled'));

  U.pages.adminDashboard = async function (main, user) {
    put(main, html`
      <div class="welcome"><span class="avatar" aria-hidden="true">${U.initials(user.fullName)}</span>
        <div><h1>University overview</h1><p>Everything at a glance. Select a tile to manage it.</p></div></div>
      <div id="dash">${U.loadingNote()}</div>`);
    const box = main.querySelector('#dash');
    const load = async () => {
      let s, notices;
      try {
        [s, notices] = await Promise.all([api.get('/admin/stats'), api.get('/admin/announcements')]);
      } catch (e) { U.showError(box, e, load); return; }

      const att = [];
      if (s.coursesWithoutTeacher > 0) {
        att.push({ tone: 'warn', body: html`<a href="/admin/courses">${plural(s.coursesWithoutTeacher, 'course')}</a> ${s.coursesWithoutTeacher === 1 ? 'has' : 'have'} no teacher assigned.` });
      }
      if (s.disabledAccounts > 0) {
        att.push({ tone: 'warn', body: html`${plural(s.disabledAccounts, 'account is', 'accounts are')} disabled. Check <a href="/admin/teachers">teachers</a> and <a href="/admin/students">students</a> for people waiting for approval.` });
      }
      if (!att.length) att.push({ tone: 'ok', icon: 'check', body: 'Nothing needs attention right now.' });

      put(box, html`
        <div class="kpis">
          ${U.kpi({ label: 'Teachers', value: num(s.teachers), icon: 'teacher', tone: 't-teacher', href: '/admin/teachers' })}
          ${U.kpi({ label: 'Students', value: num(s.students), icon: 'student', tone: 't-student', href: '/admin/students' })}
          ${U.kpi({ label: 'Courses', value: num(s.courses), icon: 'course', tone: 't-course', href: '/admin/courses' })}
          ${U.kpi({ label: 'Departments', value: num(s.departments), icon: 'dept', tone: 't-dept', href: '/admin/departments' })}
          ${U.kpi({ label: 'Enrollments', value: num(s.enrollments), sub: `${num(s.activeEnrollments)} active`, icon: 'enroll', tone: 't-enroll', href: '/admin/enrollments' })}
        </div>
        <div class="panels">
          ${U.panel('Quick actions', 'Common administrative tasks.', html`
            <div style="display:flex;gap:12px;flex-wrap:wrap;padding:6px 0;">
              <a class="btn primary" href="/admin/courses">+ Add / Manage courses</a>
              <a class="btn" href="/admin/teachers">Manage teachers</a>
              <a class="btn" href="/admin/students">Manage students</a>
              <a class="btn" href="/admin/announcements">Post announcement</a>
            </div>`)}
          ${U.panel('Needs attention', null, U.attention(att))}
          ${U.panel('Latest announcements', null, notices.length === 0
            ? html`<p class="panel-empty">Nothing posted yet. <a href="/admin/announcements">Post an announcement</a>.</p>`
            : html`<ul class="plain-list">${notices.slice(0, 5).map((a) => html`<li><b>${a.title}</b>
              <span class="muted"> — ${a.targetRole ? `for ${a.targetRole.toLowerCase()}s` : 'for everyone'}, ${fmtDate(a.createdAt)}</span></li>`)}</ul>`)}
        </div>`);
    };
    await load();
  };

  const GOES_AWAY = (items) => html`<ul>${items.map((i) => html`<li>${i}</li>`)}</ul>`;

  function teacherToStudent(row, ctx) {
    U.formDialog({
      title: `Change ${row.fullName} to a student`,
      submitLabel: 'Change to student',
      lookups: ctx.lookups,
      item: { departmentId: row.departmentId, semester: 1 },
      intro: html`<div class="callout info">
        <p><b>Kept:</b> username, password, email, name, phone and address. They sign in with the same login.</p>
        <p><b>Removed:</b> employee ID, designation, qualification and joining date.</p>
        <p><b>Courses they teach</b> become unassigned.</p>
        <p>If they are signed in right now, they land on the student dashboard the next time they sign in or open a page.</p></div>`,
      fields: [
        { name: 'studentId', label: 'Student ID', required: true },
        { name: 'departmentId', label: 'Department', type: 'select', number: true, options: deptOptions, emptyLabel: 'Keep current' },
        { name: 'batch', label: 'Batch', hint: 'For example 2024' },
        { name: 'semester', label: 'Semester', type: 'number', number: true, min: 1 },
      ],
      onSubmit: (p) => api.post(`/admin/teachers/${row.id}/convert-to-student`, p),
      onDone: () => { toast(`${row.fullName} is now a student`); ctx.reload(); },
    });
  }

  function studentToTeacher(row, ctx) {
    U.formDialog({
      title: `Change ${row.fullName} to a teacher`,
      submitLabel: 'Change to teacher',
      lookups: ctx.lookups,
      item: { departmentId: row.departmentId, deleteRecords: false },
      intro: html`<div class="callout info">
        <p><b>Kept:</b> username, password, email, name, phone and address. They sign in with the same login.</p>
        <p><b>Removed:</b> student ID, batch and semester.</p></div>
        <div class="callout"><p><b>Their study records are deleted.</b> A teacher cannot also be a student, so any of these are removed for good:</p>
        ${GOES_AWAY(['enrollments and grades', 'attendance records'])}
        <p>If this student has none, you do not need to tick the box.</p></div>`,
      fields: [
        { name: 'employeeId', label: 'Employee ID', required: true },
        { name: 'departmentId', label: 'Department', type: 'select', number: true, options: deptOptions, emptyLabel: 'Keep current' },
        { name: 'designation', label: 'Designation', hint: 'For example Lecturer' },
        { name: 'qualification', label: 'Qualification' },
        { name: 'joiningDate', label: 'Joining date', type: 'date' },
        { name: 'deleteRecords', label: 'Delete this student’s enrollments, grades and attendance', type: 'checkbox', default: false, wide: true },
      ],
      onSubmit: (p) => api.post(`/admin/students/${row.id}/convert-to-teacher`, p),
      onDone: () => { toast(`${row.fullName} is now a teacher`); ctx.reload(); },
    });
  }

  U.pages.adminTeachers = (main) => crud(main, {
    title: 'Teachers',
    note: 'Create teacher accounts and keep their details up to date. Only administrators can change these.',
    endpoint: '/admin/teachers', itemName: 'teacher', label: (r) => r.fullName,
    lookups: { departments: '/admin/departments' },
    columns: [
      { label: 'Name', key: 'fullName' }, { label: 'Employee ID', key: 'employeeId' },
      { label: 'Department', key: 'departmentName' }, { label: 'Designation', key: 'designation' },
      { label: 'Email', key: 'email' }, { label: 'Status', render: activePill },
    ],
    fields: [
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
    ],
    extraActions: [{ label: 'Change to student', run: teacherToStudent }],
  });

  U.pages.adminStudents = (main) => crud(main, {
    title: 'Students',
    note: 'Create student accounts and keep their details up to date. Only administrators can change these.',
    endpoint: '/admin/students', itemName: 'student', label: (r) => r.fullName,
    lookups: { departments: '/admin/departments' },
    columns: [
      { label: 'Name', key: 'fullName' }, { label: 'Student ID', key: 'studentId' },
      { label: 'Department', key: 'departmentName' }, { label: 'Batch', key: 'batch' },
      { label: 'Semester', key: 'semester' }, { label: 'Status', render: activePill },
    ],
    fields: [
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
    ],
    extraActions: [{ label: 'Change to teacher', run: studentToTeacher }],
  });

  U.pages.adminDepartments = (main) => crud(main, {
    title: 'Departments',
    note: 'A department can only be deleted once it has no teachers, students or courses.',
    endpoint: '/admin/departments', itemName: 'department', label: (r) => r.name,
    columns: [{ label: 'Name', key: 'name' }, { label: 'Code', key: 'code' }],
    fields: [
      { name: 'name', label: 'Name', required: true },
      { name: 'code', label: 'Code', required: true, hint: 'Short code such as CSE' },
    ],
  });

  U.pages.adminCourses = (main) => crud(main, {
    title: 'Courses', endpoint: '/admin/courses', itemName: 'course', label: (r) => `${r.code} ${r.title}`,
    lookups: { departments: '/admin/departments', teachers: '/admin/teachers' },
    columns: [
      { label: 'Code', key: 'code' }, { label: 'Title', key: 'title' }, { label: 'Credits', key: 'creditHours' },
      { label: 'Department', key: 'departmentName' },
      { label: 'Teacher', render: (r) => r.teacherName || html`<span class="muted">Not assigned</span>` },
    ],
    fields: [
      { name: 'code', label: 'Course code', required: true, hint: 'For example CSE-3101' },
      { name: 'title', label: 'Title', required: true },
      { name: 'creditHours', label: 'Credit hours', type: 'number', number: true, step: '0.5', min: 0.5, required: true, default: '3' },
      { name: 'departmentId', label: 'Department', type: 'select', number: true, required: true, options: deptOptions },
      { name: 'teacherId', label: 'Teacher', type: 'select', number: true, emptyLabel: 'Not assigned',
        options: (lk) => (lk.teachers || []).map((t) => ({ value: t.id, label: `${t.fullName} (${t.departmentName})` })) },
    ],
  });

  U.pages.adminEnrollments = (main) => crud(main, {
    title: 'Enrollments',
    note: 'Enroll a student in a course, or remove an enrollment. Removing one also deletes that student’s attendance for the course.',
    endpoint: '/admin/enrollments', itemName: 'enrollment', canEdit: false,
    label: (r) => `${r.studentName} in ${r.courseCode}`,
    lookups: { students: '/admin/students', courses: '/admin/courses' },
    columns: [
      { label: 'Student', render: (r) => `${r.studentName} (${r.studentCode})` },
      { label: 'Course', render: (r) => `${r.courseCode} ${r.courseTitle}` },
      { label: 'Status', render: (r) => U.statusPill(r.status) },
      { label: 'Grade', render: (r) => r.grade || html`<span class="muted">Not graded</span>` },
      { label: 'Enrolled', render: (r) => fmtDate(r.enrolledAt) },
    ],
    fields: [
      { name: 'studentId', label: 'Student', type: 'select', number: true, required: true,
        options: (lk) => (lk.students || []).map((s) => ({ value: s.id, label: `${s.fullName} (${s.studentId})` })) },
      { name: 'courseId', label: 'Course', type: 'select', number: true, required: true,
        options: (lk) => (lk.courses || []).map((c) => ({ value: c.id, label: `${c.code} ${c.title}` })) },
    ],
  });

  U.pages.adminAnnouncements = (main) => crud(main, {
    title: 'Announcements', note: 'Choose who sees each notice: everyone, only teachers, or only students.',
    endpoint: '/admin/announcements', itemName: 'announcement', label: (r) => r.title,
    columns: [
      { label: 'Title', key: 'title' },
      { label: 'Audience', render: (r) => (r.targetRole ? `${U.cap(r.targetRole)}s` : 'Everyone') },
      { label: 'Posted by', key: 'postedByName' }, { label: 'Date', render: (r) => fmtDate(r.createdAt) },
    ],
    fields: [
      { name: 'title', label: 'Title', required: true, wide: true },
      { name: 'content', label: 'Message', type: 'textarea', required: true, wide: true },
      { name: 'targetRole', label: 'Audience', type: 'select', emptyLabel: 'Everyone',
        options: [{ value: 'TEACHER', label: 'Teachers only' }, { value: 'STUDENT', label: 'Students only' }] },
    ],
  });
})();

