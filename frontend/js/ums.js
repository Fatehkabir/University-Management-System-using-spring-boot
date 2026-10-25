
(function () {
  'use strict';

  class Safe { constructor(s) { this.s = s; } toString() { return this.s; } }
  const ESC = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
  const esc = (v) => String(v).replace(/[&<>"']/g, (c) => ESC[c]);
  const raw = (s) => new Safe(String(s));
  function part(v) {
    if (v === null || v === undefined || v === false || v === true) return '';
    if (v instanceof Safe) return v.s;
    if (Array.isArray(v)) return v.map(part).join('');
    return esc(v);
  }
  function html(strings, ...vals) {
    let out = strings[0];
    vals.forEach((v, i) => { out += part(v) + strings[i + 1]; });
    return new Safe(out);
  }
  const put = (el, v) => { el.innerHTML = part(v); };

  /* ------------------------------------------------------------------ small helpers */
  const pad = (n) => String(n).padStart(2, '0');
  const todayISO = () => { const d = new Date(); return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`; };
  function fmtDate(v) {
    if (!v) return '';
    const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(v);
    const d = m ? new Date(+m[1], +m[2] - 1, +m[3]) : new Date(v);
    return Number.isNaN(d.getTime()) ? '' : new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium' }).format(d);
  }
  const initials = (name) => (name || '?').split(/\s+/).filter(Boolean).slice(0, 2).map((s) => s[0].toUpperCase()).join('');
  const plural = (n, one, many) => `${n} ${n === 1 ? one : (many || one + 's')}`;
  const cap = (s) => (s ? s.charAt(0) + s.slice(1).toLowerCase() : '');
  const num = (n) => new Intl.NumberFormat('en-GB').format(n);
  const home = (role) => ({ ADMIN: '/admin/', TEACHER: '/teacher/', STUDENT: '/student/' }[role] || '/login');

  /* ------------------------------------------------------------------ API client */
  const TOKEN_KEY = 'ums_token';
  const store = {
    get() { try { return localStorage.getItem(TOKEN_KEY); } catch (e) { return null; } },
    set(t) { try { localStorage.setItem(TOKEN_KEY, t); } catch (e) { /* private mode: this tab only */ } },
    clear() { try { localStorage.removeItem(TOKEN_KEY); } catch (e) { /* ignore */ } },
  };

  class ApiError extends Error {
    constructor(status, message, fieldErrors) {
      super(message);
      this.status = status;
      this.fieldErrors = fieldErrors || {};
    }
  }

  const state = { user: null };

  function signOutLocally() {
    store.clear();
    state.user = null;
    location.replace('/login');
  }

  // After a 403 the signed-in role may have changed (an admin can turn a teacher into a student).
  // Re-read who we are and move to the right dashboard if so.
  async function checkRoleAfter403() {
    try {
      const me = await request('GET', '/auth/me', undefined, { silent: true });
      if (state.user && me.role !== state.user.role) location.replace(home(me.role));
    } catch (e) { /* ignore */ }
  }

  async function request(method, path, body, opts = {}) {
    const headers = {};
    const token = store.get();
    if (token) headers.Authorization = `Bearer ${token}`;
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    let res;
    try {
      res = await fetch(`/api${path}`, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
    } catch (e) {
      throw new ApiError(0, 'Cannot reach the server. Check that the backend is running.');
    }
    if (res.status === 204) return null;
    const text = await res.text();
    let data = null;
    if (text) { try { data = JSON.parse(text); } catch (e) { /* not JSON */ } }
    if (!res.ok) {
      if (!opts.silent) {
        if (res.status === 401 && token && path !== '/auth/login') signOutLocally();
        if (res.status === 403 && token) checkRoleAfter403();
      }
      throw new ApiError(res.status, (data && data.message) || res.statusText || 'Request failed', data && data.fieldErrors);
    }
    return data;
  }
  const api = {
    get: (p, o) => request('GET', p, undefined, o),
    post: (p, b, o) => request('POST', p, b === undefined ? {} : b, o),
    put: (p, b) => request('PUT', p, b === undefined ? {} : b),
    del: (p) => request('DELETE', p),
  };

  /* ------------------------------------------------------------------ icons */
  const ICONS = {
    teacher: '<circle cx="12" cy="8" r="4"/><path d="M4 21v-1a6 6 0 0 1 6-6h4a6 6 0 0 1 6 6v1"/>',
    student: '<circle cx="9" cy="8" r="3.5"/><path d="M2 20v-1a5 5 0 0 1 5-5h4a5 5 0 0 1 5 5v1"/><path d="M16 4.6a3.5 3.5 0 0 1 0 6.8"/><path d="M22 20v-1a5 5 0 0 0-3.5-4.8"/>',
    course: '<path d="M4 4.5A2.5 2.5 0 0 1 6.5 2H20v16H6.5A2.5 2.5 0 0 0 4 20.5z"/><path d="M4 20.5A2.5 2.5 0 0 0 6.5 23H20v-5"/><path d="M9 7h6"/>',
    dept: '<path d="M3 21h18"/><path d="M5 21V8l7-5 7 5v13"/><path d="M9 21v-6h6v6"/>',
    enroll: '<rect x="5" y="4" width="14" height="17" rx="2"/><path d="M9 4h6v3H9z"/><path d="m9 14 2 2 4-4"/>',
    check: '<circle cx="12" cy="12" r="9"/><path d="m8 12 3 3 5-6"/>',
    alert: '<path d="M12 3 2 20h20z"/><path d="M12 10v4"/><path d="M12 17.2v.01"/>',
    calendar: '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M3 10h18M8 3v4M16 3v4"/>',
    award: '<circle cx="12" cy="9" r="6"/><path d="m8.5 14.5-1.5 7 5-3 5 3-1.5-7"/>',
    layers: '<path d="m12 3 9 5-9 5-9-5z"/><path d="m3 13 9 5 9-5"/>',
  };
  const icon = (name) => raw(`<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ICONS[name] || ''}</svg>`);

  /* ------------------------------------------------------------------ toasts */
  function toast(message, kind = 'ok') {
    let box = document.querySelector('.toasts');
    if (!box) {
      box = document.createElement('div');
      box.className = 'toasts';
      box.setAttribute('role', 'status');
      box.setAttribute('aria-live', 'polite');
      document.body.appendChild(box);
    }
    const t = document.createElement('div');
    t.className = `toast ${kind}`;
    t.textContent = message;
    box.appendChild(t);
    setTimeout(() => t.remove(), kind === 'error' ? 6000 : 3500);
  }

  /* ------------------------------------------------------------------ common blocks */
  const pageHeader = (title, note, actions) => html`
    <header class="page-header">
      <div><h1>${title}</h1>${note ? html`<p class="note">${note}</p>` : ''}</div>
      ${actions ? html`<div class="page-actions">${actions}</div>` : ''}
    </header>`;
  const loadingNote = (what = 'Loading') => html`<p class="muted" role="status">${what}…</p>`;
  const emptyNote = (text) => html`<p class="empty">${text}</p>`;
  const errorNote = (err, withRetry) => html`
    <div class="error-note" role="alert"><span>${err && err.message ? err.message : String(err)}</span>
    ${withRetry ? raw('<button type="button" class="btn link" data-retry>Try again</button>') : ''}</div>`;
  const pill = (tone, text) => html`<span class="pill ${tone}">${text}</span>`;
  const STATUS_TONE = { ACTIVE: 'ok', COMPLETED: 'info', DROPPED: 'muted', PRESENT: 'ok', LATE: 'warn', ABSENT: 'bad', EXCUSED: 'info' };
  const statusPill = (s) => pill(STATUS_TONE[s] || 'neutral', cap(s));

  /** Shows an error note in `el` with a working "Try again" button. */
  function showError(el, err, retry) {
    put(el, errorNote(err, Boolean(retry)));
    const b = el.querySelector('[data-retry]');
    if (b && retry) b.addEventListener('click', retry);
  }

  /* ------------------------------------------------------------------ dialogs */
  function openDialog(title, bodyHtml, { wide = false } = {}) {
    const d = document.createElement('dialog');
    d.className = `modal${wide ? ' wide' : ''}`;
    d.setAttribute('aria-labelledby', 'modal-title');
    put(d, html`<div class="modal-body">
      <header class="modal-head"><h2 id="modal-title">${title}</h2>
      <button type="button" class="icon-btn" data-close aria-label="Close">×</button></header>
      <div data-dialog-content>${bodyHtml}</div></div>`);
    document.body.appendChild(d);
    const close = () => { if (d.open) d.close(); };
    d.addEventListener('close', () => d.remove());
    d.addEventListener('click', (e) => { if (e.target === d) close(); });
    d.querySelector('[data-close]').addEventListener('click', close);
    d.showModal();
    return { el: d, close, content: d.querySelector('[data-dialog-content]') };
  }

  function confirmDialog({ title, message, confirmLabel = 'Delete', onConfirm }) {
    const dlg = openDialog(title, html`<p>${message}</p>
      <div class="form-actions">
        <button type="button" class="btn" data-close-btn>Cancel</button>
        <button type="button" class="btn danger" data-ok>${confirmLabel}</button>
      </div>`);
    dlg.el.querySelector('[data-close-btn]').addEventListener('click', dlg.close);
    const ok = dlg.el.querySelector('[data-ok]');
    ok.addEventListener('click', async () => {
      ok.disabled = true;
      ok.textContent = 'Working…';
      try { await onConfirm(); } finally { dlg.close(); }
    });
  }

  /* ------------------------------------------------------------------ forms */
  function fieldHtml(f, value, ctx) {
    const id = `f-${f.name}`;
    const hint = f.type === 'password' && ctx.mode === 'edit' ? 'Leave blank to keep the current password' : f.hint;
    const common = (extra = '') => raw(`id="${id}" name="${esc(f.name)}"${hint ? ` aria-describedby="${id}-hint"` : ''}${extra}`);
    let input;
    if (f.type === 'select') {
      const opts = typeof f.options === 'function' ? f.options(ctx.lookups || {}) : (f.options || []);
      input = html`<select ${common()}>
        <option value="">${f.emptyLabel || 'Select…'}</option>
        ${opts.map((o) => html`<option value="${o.value}"${String(o.value) === String(value) ? raw(' selected') : ''}>${o.label}</option>`)}
      </select>`;
    } else if (f.type === 'textarea') {
      input = html`<textarea ${common()} rows="5">${value}</textarea>`;
    } else if (f.type === 'checkbox') {
      input = html`<input ${common()} type="checkbox"${value ? raw(' checked') : ''}>`;
    } else {
      const attrs = [
        `type="${esc(f.type || 'text')}"`,
        f.step ? `step="${esc(f.step)}"` : '',
        f.min !== undefined ? `min="${esc(f.min)}"` : '',
        f.maxlength ? `maxlength="${esc(f.maxlength)}"` : '',
        `autocomplete="${f.type === 'password' ? 'new-password' : 'off'}"`,
      ].join(' ');
      input = html`<input ${common(' ' + attrs)} value="${value}">`;
    }
    const star = f.required && !(f.type === 'password' && ctx.mode === 'edit');
    return html`<div class="field${f.type === 'checkbox' ? ' inline' : ''}${f.wide ? ' wide' : ''}" data-field="${f.name}">
      <label for="${id}">${f.label}${star ? raw('<span aria-hidden="true"> *</span>') : ''}</label>
      ${input}
      ${hint ? html`<p id="${id}-hint" class="hint">${hint}</p>` : ''}
    </div>`;
  }

  function initialValue(f, item) {
    let v = item ? item[f.name] : undefined;
    if (f.type === 'checkbox') return item ? Boolean(v) : (f.default === undefined ? true : f.default);
    return v === null || v === undefined ? (f.default === undefined ? '' : f.default) : String(v);
  }

  function readPayload(form, fields) {
    const payload = {};
    for (const f of fields) {
      const el = form.elements[f.name];
      if (!el) continue;
      if (f.type === 'checkbox') { payload[f.name] = el.checked; continue; }
      const v = el.value;
      if (v === '') { if (f.type !== 'password') payload[f.name] = null; } // blank password on edit keeps the old one
      else payload[f.name] = f.number ? Number(v) : v;
    }
    return payload;
  }

  function showFieldErrors(form, errors) {
    form.querySelectorAll('.field-error').forEach((n) => n.remove());
    form.querySelectorAll('[aria-invalid]').forEach((n) => n.removeAttribute('aria-invalid'));
    Object.entries(errors || {}).forEach(([name, msg]) => {
      const wrap = form.querySelector(`[data-field="${CSS.escape(name)}"]`);
      if (!wrap) return;
      const input = wrap.querySelector('input,select,textarea');
      if (input) input.setAttribute('aria-invalid', 'true');
      const p = document.createElement('p');
      p.className = 'field-error';
      p.textContent = msg;
      wrap.appendChild(p);
    });
  }

  /**
   * Opens a form in a dialog.
   * opts: title, fields, item (existing values), mode ('create'|'edit'), lookups, submitLabel, intro (html),
   *       wide, onSubmit(payload) -> Promise (throw ApiError to show errors), onDone(result)
   */
  function formDialog(opts) {
    const { fields, item = null, mode = 'create', lookups = {} } = opts;
    const ctx = { mode, lookups };
    const dlg = openDialog(opts.title, html`
      ${opts.intro || ''}
      <form novalidate>
        <div class="form-grid">${fields.map((f) => fieldHtml(f, initialValue(f, item), ctx))}</div>
        <p class="form-error" role="alert" hidden data-form-error></p>
        <div class="form-actions">
          <button type="button" class="btn" data-cancel>Cancel</button>
          <button type="submit" class="btn primary">${opts.submitLabel || 'Save'}</button>
        </div>
      </form>`, { wide: opts.wide !== false });
    const form = dlg.el.querySelector('form');
    const err = dlg.el.querySelector('[data-form-error]');
    const submit = form.querySelector('[type=submit]');
    dlg.el.querySelector('[data-cancel]').addEventListener('click', dlg.close);
    const first = form.querySelector('input:not([type=checkbox]),select,textarea');
    if (first) first.focus();

    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      err.hidden = true;
      showFieldErrors(form, {});
      const label = submit.textContent;
      submit.disabled = true;
      submit.textContent = 'Saving…';
      try {
        const result = await opts.onSubmit(readPayload(form, fields));
        dlg.close();
        if (opts.onDone) opts.onDone(result);
      } catch (ex) {
        const fe = ex && ex.fieldErrors ? ex.fieldErrors : {};
        showFieldErrors(form, fe);
        err.textContent = Object.keys(fe).length ? 'Fix the highlighted fields and try again.' : (ex.message || 'Something went wrong');
        err.hidden = false;
        submit.disabled = false;
        submit.textContent = label;
      }
    });
    return dlg;
  }

  /* ------------------------------------------------------------------ chart components (HTML + CSS only) */
  function kpi({ label, value, unit, sub, icon: ic, tone = '', href }) {
    const inner = html`
      <div class="kpi-top"><span>${label}</span><span class="kpi-icon">${icon(ic)}</span></div>
      <div class="kpi-value">${value}${unit ? html`<small>${unit}</small>` : ''}</div>
      ${sub ? html`<p class="kpi-sub">${sub}</p>` : ''}`;
    return href
      ? html`<a class="kpi ${tone}" href="${href}">${inner}</a>`
      : html`<div class="kpi ${tone}">${inner}</div>`;
  }

  const panel = (title, note, body, { wide = false } = {}) => html`
    <section class="panel${wide ? ' wide' : ''}">
      <h2>${title}</h2>${note ? html`<p class="panel-note">${note}</p>` : ''}${body}
    </section>`;

  /** Single-series horizontal bars, one colour, value printed on every row. items: [{label, sub, value, href}] */
  function barList(items, emptyText = 'Nothing to show yet.') {
    if (!items.length) return html`<p class="panel-empty">${emptyText}</p>`;
    const max = Math.max(1, ...items.map((i) => i.value));
    return html`<ul class="bars">${items.map((i) => html`
      <li class="bar-row">
        ${i.href ? html`<a class="bar-label" href="${i.href}">${i.label}${i.sub ? html`<small>${i.sub}</small>` : ''}</a>`
                 : html`<span class="bar-label">${i.label}${i.sub ? html`<small>${i.sub}</small>` : ''}</span>`}
        <span class="bar-track" role="img" aria-label="${i.label}: ${i.value}"><span class="bar-fill" style="width:${Math.round((i.value / max) * 100)}%"></span></span>
        <span class="bar-value">${num(i.value)}</span>
      </li>`)}</ul>`;
  }

  /** Part-to-whole: one stacked bar with a legend that lists every value. segments: [{key, label, value}] */
  function stackedBar(segments, emptyText = 'Nothing to show yet.') {
    const total = segments.reduce((a, s) => a + s.value, 0);
    if (!total) return html`<p class="panel-empty">${emptyText}</p>`;
    return html`
      <div class="stacked" role="img" aria-label="${segments.map((s) => `${s.label} ${s.value}`).join(', ')}">
        ${segments.filter((s) => s.value > 0).map((s) => html`<span class="s-${s.key}" style="flex:${s.value} 1 0"></span>`)}
      </div>
      <ul class="legend">${segments.map((s) => html`
        <li><span class="sw s-${s.key}"></span><span>${s.label}</span>
        <span><b>${num(s.value)}</b><span class="pct">${Math.round((s.value / total) * 100)}%</span></span></li>`)}</ul>`;
  }

  const WARN_ICON = () => icon('alert');
  const flagBelow = (limit) => html`<span class="flag">${WARN_ICON()}Below ${limit}%</span>`;

  /** A labelled progress meter. pct may be null (no data yet). */
  function meterRow({ label, sub, pct, text, warnBelow }) {
    if (pct === null || pct === undefined) {
      return html`<li class="meter-row"><span class="bar-label">${label}${sub ? html`<small>${sub}</small>` : ''}</span>
        <span class="meter" role="img" aria-label="${label}: no data yet"></span><span class="meter-text muted">No data yet</span></li>`;
    }
    const warn = warnBelow && pct < warnBelow;
    return html`<li class="meter-row"><span class="bar-label">${label}${sub ? html`<small>${sub}</small>` : ''}</span>
      <span class="meter${warn ? ' warn' : ''}" role="img" aria-label="${label}: ${pct} percent"><span style="width:${Math.max(0, Math.min(100, pct))}%"></span></span>
      <span class="meter-text">${text || `${pct}%`}${warn ? flagBelow(warnBelow) : ''}</span></li>`;
  }

  function bigMeter({ pct, caption, empty, warnBelow }) {
    if (pct === null || pct === undefined) return html`<p class="panel-empty">${empty || 'No data yet.'}</p>`;
    const warn = warnBelow && pct < warnBelow;
    return html`<div class="big-meter">
      <div class="figure">${pct}<small>%</small>${warn ? flagBelow(warnBelow) : ''}</div>
      <span class="meter${warn ? ' warn' : ''}" role="img" aria-label="${pct} percent"><span style="width:${Math.max(0, Math.min(100, pct))}%"></span></span>
    </div>${caption ? html`<p class="hint">${caption}</p>` : ''}`;
  }

  const attention = (items) => html`<ul class="attention">${items.map((i) => html`
    <li class="${i.tone === 'warn' ? 'is-warn' : i.tone === 'ok' ? 'is-ok' : ''}">${icon(i.icon || (i.tone === 'ok' ? 'check' : 'alert'))}<span>${i.body}</span></li>`)}</ul>`;

  /* ------------------------------------------------------------------ page shell */
  const NAV = {
    ADMIN: [
      ['/admin/', 'Dashboard'], ['/admin/teachers', 'Teachers'], ['/admin/students', 'Students'],
      ['/admin/departments', 'Departments'], ['/admin/courses', 'Courses'],
      ['/admin/enrollments', 'Enrollments'], ['/admin/announcements', 'Announcements'],
      ['/account', 'My account'],
    ],
    TEACHER: [['/teacher/', 'Dashboard'], ['/announcements', 'Announcements'], ['/account', 'My account']],
    STUDENT: [
      ['/student/', 'Dashboard'], ['/student/register', 'Register for courses'],
      ['/student/assignments', 'Assignments & marks'],
      ['/student/attendance', 'Attendance'], ['/announcements', 'Announcements'], ['/account', 'My account'],
    ],
  };
  const ROLE_LABEL = { ADMIN: 'Administrator', TEACHER: 'Teacher', STUDENT: 'Student' };
  const LOGO = '<svg width="28" height="28" viewBox="0 0 32 32" aria-hidden="true"><rect width="32" height="32" rx="6" fill="#0E5C57"/><path d="M6 12 16 7l10 5v2H6z" fill="#fff"/><rect x="8" y="16" width="3" height="7" fill="#E9B949"/><rect x="14.5" y="16" width="3" height="7" fill="#E9B949"/><rect x="21" y="16" width="3" height="7" fill="#E9B949"/><rect x="6" y="24" width="20" height="2" fill="#fff"/></svg>';

  function buildSidebar(user, activeHref) {
    const side = document.getElementById('sidebar');
    const here = location.pathname.replace(/index(\.html)?$/, '').replace(/\.html$/, '');
    put(side, html`
      <div class="brand">${raw(LOGO)}<span>University<br>Management</span></div>
      <nav aria-label="Main">${NAV[user.role].map(([href, label]) => {
        const cleanHref = href.replace(/\.html$/, '');
        const isCurrent = cleanHref === here || href === activeHref || cleanHref === (activeHref || '').replace(/\.html$/, '');
        return html`<a href="${href}"${isCurrent ? raw(' aria-current="page"') : ''}>${label}</a>`;
      })}</nav>
      <div class="who">
        <div class="who-name">${user.fullName}</div>
        <div class="who-role">${ROLE_LABEL[user.role]}</div>
        <button type="button" class="btn ghost" id="signout">Sign out</button>
      </div>`);
    document.getElementById('signout').addEventListener('click', signOutLocally);
  }

  /**
   * Starts a signed-in page. role = 'ADMIN' | 'TEACHER' | 'STUDENT' | null (any signed-in user).
   * fn(main, user) renders the page into <main>.
   */
  async function run(role, fn, opts = {}) {
    const main = document.getElementById('main');
    put(main, raw('<p class="boot" role="status">Loading…</p>'));
    let user = null;
    if (store.get()) {
      try { user = await api.get('/auth/me', { silent: true }); } catch (e) { store.clear(); }
    }
    if (!user) { location.replace('/login'); return; }
    if (role && user.role !== role) { location.replace(home(user.role)); return; }
    state.user = user;
    document.body.classList.add(`role-${user.role.toLowerCase()}`);
    buildSidebar(user, opts.active);
    main.innerHTML = '';
    if (typeof fn !== 'function') {
      showError(main, new Error(`Page function is not defined. Please check that page scripts are loaded.`));
      return;
    }
    try { await fn(main, user); } catch (e) { showError(main, e, () => run(role, fn, opts)); }
  }

  /* ------------------------------------------------------------------ generic admin table */
  /**
   * One function drives every admin table. config:
   *  title, note, endpoint ("/admin/teachers"), itemName, label(row), columns [{label, key | render(row)}],
   *  fields (see formDialog), lookups {name: "/path"}, canEdit (default true), empty,
   *  extraActions [{label, show(row), run(row, ctx)}]  ctx = {reload, lookups}
   */
  async function crud(main, config) {
    const { title, note, endpoint, itemName, label, columns, fields, lookups = {}, canEdit = true, extraActions = [] } = config;
    let rows = [];
    let query = '';
    let lookupData = {};

    put(main, html`
      ${pageHeader(title, note, html`<button type="button" class="btn primary" id="add">Add ${itemName}</button>`)}
      <div class="toolbar">
        <label class="search"><span class="sr-only">Search ${title}</span>
          <input type="search" id="q" placeholder="Search ${title.toLowerCase()}"></label>
        <span class="muted" id="count"></span>
      </div>
      <div id="table-area">${loadingNote()}</div>`);
    const area = main.querySelector('#table-area');
    const countEl = main.querySelector('#count');

    function draw() {
      const q = query.trim().toLowerCase();
      const shown = q ? rows.filter((r) => JSON.stringify(Object.values(r)).toLowerCase().includes(q)) : rows;
      countEl.textContent = `${shown.length} shown`;
      if (!shown.length) {
        put(area, emptyNote(q ? `No ${title.toLowerCase()} match “${query}”.` : (config.empty || `No ${title.toLowerCase()} yet. Add the first ${itemName}.`)));
        return;
      }
      put(area, html`<div class="table-wrap"><table>
        <thead><tr>${columns.map((c) => html`<th>${c.label}</th>`)}<th class="actions-col"><span class="sr-only">Actions</span></th></tr></thead>
        <tbody>${shown.map((r) => html`<tr>
          ${columns.map((c) => html`<td>${c.render ? c.render(r) : r[c.key]}</td>`)}
          <td class="actions-col">
            ${canEdit ? html`<button type="button" class="btn link" data-act="edit" data-id="${r.id}">Edit</button>` : ''}
            ${extraActions.map((a, i) => (!a.show || a.show(r)) ? html`<button type="button" class="btn link" data-act="extra" data-i="${i}" data-id="${r.id}">${a.label}</button>` : '')}
            <button type="button" class="btn link danger-text" data-act="delete" data-id="${r.id}">Delete</button>
          </td></tr>`)}</tbody></table></div>`);
    }

    async function reload() {
      try {
        rows = await api.get(endpoint);
        draw();
      } catch (e) {
        showError(area, e, reload);
      }
    }

    // lookups (departments, teachers, ...) feed the select boxes in the forms
    Promise.all(Object.entries(lookups).map(async ([k, p]) => [k, await api.get(p)]))
      .then((pairs) => { lookupData = Object.fromEntries(pairs); })
      .catch((e) => toast(e.message, 'error'));

    const reloadAll = async () => {
      await reload();
      // keep select options fresh after changes (a converted teacher disappears from the teacher list)
      Promise.all(Object.entries(lookups).map(async ([k, p]) => [k, await api.get(p)]))
        .then((pairs) => { lookupData = Object.fromEntries(pairs); }).catch(() => {});
    };

    main.querySelector('#q').addEventListener('input', (e) => { query = e.target.value; draw(); });
    main.querySelector('#add').addEventListener('click', () => formDialog({
      title: `Add ${itemName}`, fields, lookups: lookupData, mode: 'create', submitLabel: `Add ${itemName}`,
      onSubmit: (payload) => api.post(endpoint, payload),
      onDone: () => { toast(`Added ${itemName}`); reloadAll(); },
    }));

    area.addEventListener('click', (e) => {
      const btn = e.target.closest('[data-act]');
      if (!btn) return;
      const row = rows.find((r) => String(r.id) === btn.dataset.id);
      if (!row) return;
      if (btn.dataset.act === 'edit') {
        formDialog({
          title: `Edit ${itemName}`, fields, item: row, lookups: lookupData, mode: 'edit', submitLabel: 'Save changes',
          onSubmit: (payload) => api.put(`${endpoint}/${row.id}`, payload),
          onDone: () => { toast(`Saved changes to ${itemName}`); reloadAll(); },
        });
      } else if (btn.dataset.act === 'delete') {
        confirmDialog({
          title: `Delete ${itemName}?`,
          message: `“${label(row)}” will be removed permanently. This can't be undone.`,
          onConfirm: async () => {
            try { await api.del(`${endpoint}/${row.id}`); toast(`Deleted ${label(row)}`); reloadAll(); }
            catch (ex) { toast(ex.message, 'error'); }
          },
        });
      } else if (btn.dataset.act === 'extra') {
        extraActions[Number(btn.dataset.i)].run(row, { reload: reloadAll, lookups: lookupData });
      }
    });

    await reload();
  }

  /* ------------------------------------------------------------------ public surface */
  window.UMS = {
    html, raw, esc, put, api, ApiError, store, state, home, run, toast,
    fmtDate, todayISO, initials, plural, cap, num,
    pageHeader, loadingNote, emptyNote, errorNote, showError, pill, statusPill,
    openDialog, confirmDialog, formDialog, crud,
    icon, kpi, panel, barList, stackedBar, meterRow, bigMeter, attention,
    signOut: signOutLocally,
    pages: {},
  };
})();

