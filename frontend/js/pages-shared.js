
(function () {
  'use strict';
  const { html, put, api, pageHeader, loadingNote, emptyNote, showError, fmtDate, toast } = UMS;

  UMS.pages.announcements = async function (main) {
    put(main, html`${pageHeader('Announcements', 'Notices from the university office.')}<div id="list">${loadingNote()}</div>`);
    const list = main.querySelector('#list');
    const load = async () => {
      try {
        const items = await api.get('/announcements');
        put(list, items.length === 0 ? emptyNote('No announcements right now.') : html`<div class="notices">${items.map((a) => html`
          <article class="notice"><h2>${a.title}</h2><p>${a.content}</p>
          <p class="meta">Posted by ${a.postedByName} on ${fmtDate(a.createdAt)}</p></article>`)}</div>`);
      } catch (e) { showError(list, e, load); }
    };
    await load();
  };

  UMS.pages.account = async function (main, user) {
    put(main, html`${pageHeader('My account')}
      <div class="two-col">
        <section><h2>Your details</h2><div id="details">${loadingNote()}</div></section>
        <section><h2>Change password</h2>
          <form id="pw" class="stack" novalidate>
            <div class="field"><label for="cur">Current password</label>
              <input id="cur" name="currentPassword" type="password" autocomplete="current-password" required></div>
            <div class="field"><label for="new">New password</label>
              <input id="new" name="newPassword" type="password" autocomplete="new-password" minlength="6" required>
              <p class="hint">At least 6 characters.</p></div>
            <div class="field"><label for="conf">Confirm new password</label>
              <input id="conf" name="confirm" type="password" autocomplete="new-password" required></div>
            <p class="form-error" role="alert" hidden id="pw-error"></p>
            <div><button class="btn primary" type="submit">Change password</button></div>
          </form></section>
      </div>`);

    const details = main.querySelector('#details');
    const loadDetails = async () => {
      try {
        const p = await api.get(user.role === 'TEACHER' ? '/teacher/profile' : user.role === 'STUDENT' ? '/student/profile' : '/auth/me');
        const rows = user.role === 'TEACHER' ? [
          ['Name', p.fullName], ['Username', p.username], ['Employee ID', p.employeeId], ['Department', p.departmentName],
          ['Designation', p.designation], ['Qualification', p.qualification], ['Email', p.email], ['Phone', p.phone],
          ['Address', p.address], ['Joined', fmtDate(p.joiningDate)],
        ] : user.role === 'STUDENT' ? [
          ['Name', p.fullName], ['Username', p.username], ['Student ID', p.studentId], ['Department', p.departmentName],
          ['Batch', p.batch], ['Semester', p.semester], ['Email', p.email], ['Phone', p.phone], ['Address', p.address],
        ] : [['Name', p.fullName], ['Username', p.username], ['Role', 'Administrator']];
        put(details, html`<dl class="details">${rows.filter(([, v]) => v !== null && v !== undefined && v !== '')
          .map(([k, v]) => html`<div><dt>${k}</dt><dd>${v}</dd></div>`)}</dl>
          ${user.role !== 'ADMIN' ? html`<p class="hint">These details can only be changed by an administrator. Contact the university office if something is wrong.</p>` : ''}`);
      } catch (e) { showError(details, e, loadDetails); }
    };
    loadDetails();

    const form = main.querySelector('#pw');
    const err = main.querySelector('#pw-error');
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      err.hidden = true;
      if (form.newPassword.value !== form.confirm.value) {
        err.textContent = 'The new password and its confirmation do not match.';
        err.hidden = false;
        return;
      }
      const btn = form.querySelector('button');
      btn.disabled = true;
      try {
        await api.post('/auth/change-password', { currentPassword: form.currentPassword.value, newPassword: form.newPassword.value });
        toast('Password changed');
        form.reset();
      } catch (ex) {
        err.textContent = (ex.fieldErrors && ex.fieldErrors.newPassword) || ex.message;
        err.hidden = false;
      } finally { btn.disabled = false; }
    });
  };
})();

