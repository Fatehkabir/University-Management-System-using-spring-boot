
(function () {
  'use strict';
  const { api, store, home, toast } = UMS;

  const byId = (id) => document.getElementById(id);
  const showError = (el, msg) => { el.textContent = msg; el.hidden = false; };

  UMS.pages.login = async function () {

    if (store.get()) {
      try { const me = await api.get('/auth/me', { silent: true }); location.replace(home(me.role)); return; }
      catch (e) { store.clear(); }
    }
    const form = byId('login-form');
    const err = byId('login-error');
    const btn = form.querySelector('button[type=submit]');
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      err.hidden = true;
      btn.disabled = true;
      btn.textContent = 'Signing in…';
      try {
        const res = await api.post('/auth/login', { username: form.username.value.trim(), password: form.password.value }, { silent: true });
        store.set(res.token);
        location.replace(home(res.user.role));
      } catch (ex) {
        showError(err, ex.message);
        btn.disabled = false;
        btn.textContent = 'Sign in';
      }
    });
  };

  UMS.pages.signup = async function () {
    const form = byId('signup-form');
    const err = byId('signup-error');
    const closed = byId('signup-closed');
    const done = byId('signup-done');
    const btn = form.querySelector('button[type=submit]');

    let info;
    try { info = await api.get('/public/signup-info', { silent: true }); }
    catch (e) { showError(err, e.message); return; }
    if (!info.enabled) { form.hidden = true; closed.hidden = false; return; }

    const dep = form.departmentId;
    info.departments.forEach((d) => {
      const o = document.createElement('option');
      o.value = d.id;
      o.textContent = `${d.name} (${d.code})`;
      dep.appendChild(o);
    });
    if (info.requireApproval) byId('approval-note').hidden = false;

    const apply = () => {
      const role = form.role.value;
      form.querySelectorAll('[data-for]').forEach((g) => {
        const on = g.dataset.for === role;
        g.hidden = !on;
        g.querySelectorAll('input,select,textarea').forEach((i) => { i.disabled = !on; });
      });
    };
    form.querySelectorAll('input[name=role]').forEach((r) => r.addEventListener('change', apply));
    apply();

    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      err.hidden = true;
      form.querySelectorAll('.field-error').forEach((n) => n.remove());
      form.querySelectorAll('[aria-invalid]').forEach((n) => n.removeAttribute('aria-invalid'));
      if (form.password.value !== form.confirm.value) {
        showError(err, 'The password and its confirmation do not match.');
        form.confirm.setAttribute('aria-invalid', 'true');
        return;
      }
      const body = {};
      new FormData(form).forEach((v, k) => { if (k !== 'confirm' && v !== '') body[k] = v; });
      if (body.departmentId) body.departmentId = Number(body.departmentId);
      if (body.semester) body.semester = Number(body.semester);
      btn.disabled = true;
      btn.textContent = 'Creating account…';
      try {
        const res = await api.post('/auth/register', body, { silent: true });
        if (res.approvalRequired) {
          form.hidden = true;
          byId('done-text').textContent = res.message;
          done.hidden = false;
          return;
        }

        const login = await api.post('/auth/login', { username: body.username, password: body.password }, { silent: true });
        store.set(login.token);
        location.replace(home(login.user.role));
      } catch (ex) {
        const fe = ex.fieldErrors || {};
        Object.entries(fe).forEach(([name, msg]) => {
          const input = form.elements[name];
          if (!input) return;
          input.setAttribute('aria-invalid', 'true');
          const p = document.createElement('p');
          p.className = 'field-error';
          p.textContent = msg;
          input.closest('.field').appendChild(p);
        });
        showError(err, Object.keys(fe).length ? 'Fix the highlighted fields and try again.' : ex.message);
        btn.disabled = false;
        btn.textContent = 'Create account';
        err.scrollIntoView({ block: 'nearest' });
      }
    });
  };
})();

