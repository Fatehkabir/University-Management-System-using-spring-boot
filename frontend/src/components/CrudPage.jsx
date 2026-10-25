import { useEffect, useMemo, useState } from 'react';
import { api, ApiError } from '../api.js';
import { useAsync } from '../hooks.js';
import { useToast } from '../toast.jsx';
import { ConfirmDialog, Empty, ErrorNote, Loading, Modal, PageHeader } from './ui.jsx';

/**
 * One page component drives every admin table (teachers, students, departments, courses, ...).
 *
 * props
 *  title, note        heading text
 *  endpoint           REST collection, e.g. "/admin/teachers" (GET list, POST create, PUT/DELETE /:id)
 *  itemName           singular noun used in buttons and messages ("teacher")
 *  label(row)         human name of a row, used in the delete confirmation
 *  columns            [{ label, key | render(row) }]
 *  fields             form fields: { name, label, type, required, hint, number, options, emptyLabel, default }
 *  lookups            { name: "/path" } extra lists loaded for select options
 *  canEdit            set false for create/delete-only resources (enrollments)
 */
export default function CrudPage({
  title, note, endpoint, itemName, label, columns, fields, lookups = {}, canEdit = true, empty,
}) {
  const toast = useToast();
  const list = useAsync(() => api.get(endpoint), [endpoint]);
  const [lookupData, setLookupData] = useState({});
  const [dialog, setDialog] = useState(null); // { mode: 'create' | 'edit', item }
  const [toDelete, setToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [query, setQuery] = useState('');

  useEffect(() => {
    const entries = Object.entries(lookups);
    if (!entries.length) return;
    Promise.all(entries.map(async ([k, path]) => [k, await api.get(path)]))
      .then((pairs) => setLookupData(Object.fromEntries(pairs)))
      .catch((e) => toast(e.message, 'error'));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const rows = useMemo(() => {
    const all = list.data || [];
    const q = query.trim().toLowerCase();
    if (!q) return all;
    return all.filter((r) => JSON.stringify(Object.values(r)).toLowerCase().includes(q));
  }, [list.data, query]);

  const remove = async () => {
    setDeleting(true);
    try {
      await api.del(`${endpoint}/${toDelete.id}`);
      toast(`Deleted ${label(toDelete)}`);
      setToDelete(null);
      list.reload();
    } catch (e) {
      toast(e.message, 'error');
      setToDelete(null);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <>
      <PageHeader title={title} note={note}>
        <button className="btn primary" onClick={() => setDialog({ mode: 'create', item: null })}>
          Add {itemName}
        </button>
      </PageHeader>

      <div className="toolbar">
        <label className="search">
          <span className="sr-only">Search {title}</span>
          <input
            type="search"
            placeholder={`Search ${title.toLowerCase()}`}
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </label>
        <span className="muted">{rows.length} shown</span>
      </div>

      <ErrorNote error={list.error} onRetry={list.reload} />
      {list.loading && !list.data && <Loading />}

      {list.data && (rows.length === 0 ? (
        <Empty>{query ? `No ${title.toLowerCase()} match “${query}”.` : empty || `No ${title.toLowerCase()} yet. Add the first ${itemName}.`}</Empty>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                {columns.map((c) => <th key={c.label}>{c.label}</th>)}
                <th className="actions-col"><span className="sr-only">Actions</span></th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id}>
                  {columns.map((c) => <td key={c.label}>{c.render ? c.render(row) : row[c.key]}</td>)}
                  <td className="actions-col">
                    {canEdit && (
                      <button className="btn link" onClick={() => setDialog({ mode: 'edit', item: row })}>Edit</button>
                    )}
                    <button className="btn link danger-text" onClick={() => setToDelete(row)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ))}

      {dialog && (
        <FormModal
          mode={dialog.mode}
          item={dialog.item}
          fields={fields}
          lookupData={lookupData}
          itemName={itemName}
          endpoint={endpoint}
          onClose={() => setDialog(null)}
          onSaved={(msg) => { setDialog(null); toast(msg); list.reload(); }}
        />
      )}

      {toDelete && (
        <ConfirmDialog
          title={`Delete ${itemName}?`}
          message={`“${label(toDelete)}” will be removed permanently. This can't be undone.`}
          busy={deleting}
          onConfirm={remove}
          onClose={() => setToDelete(null)}
        />
      )}
    </>
  );
}

function FormModal({ mode, item, fields, lookupData, itemName, endpoint, onClose, onSaved }) {
  const [values, setValues] = useState(() => Object.fromEntries(fields.map((f) => {
    let v = item ? item[f.name] : undefined;
    if (f.type === 'checkbox') v = item ? Boolean(v) : (f.default ?? true);
    else v = v == null ? (f.default ?? '') : String(v);
    return [f.name, v];
  })));
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const set = (name, v) => setValues((s) => ({ ...s, [name]: v }));

  const submit = async (e) => {
    e.preventDefault();
    const payload = {};
    for (const f of fields) {
      const v = values[f.name];
      if (f.type === 'checkbox') payload[f.name] = v;
      else if (v === '' || v == null) {
        if (f.type !== 'password') payload[f.name] = null; // blank password on edit = keep the old one
      } else payload[f.name] = f.number ? Number(v) : v;
    }
    setSaving(true);
    setFieldErrors({});
    setFormError('');
    try {
      if (mode === 'create') await api.post(endpoint, payload);
      else await api.put(`${endpoint}/${item.id}`, payload);
      onSaved(mode === 'create' ? `Added ${itemName}` : `Saved changes to ${itemName}`);
    } catch (err) {
      const hasFieldErrors = err instanceof ApiError && Object.keys(err.fieldErrors).length > 0;
      if (hasFieldErrors) setFieldErrors(err.fieldErrors);
      setFormError(hasFieldErrors ? 'Fix the highlighted fields and try again.' : err.message);
      setSaving(false);
    }
  };

  return (
    <Modal title={mode === 'create' ? `Add ${itemName}` : `Edit ${itemName}`} onClose={onClose} wide>
      <form onSubmit={submit} noValidate>
        <div className="form-grid">
          {fields.map((f) => {
            const id = `f-${f.name}`;
            const err = fieldErrors[f.name];
            const hint = f.type === 'password' && mode === 'edit' ? 'Leave blank to keep the current password' : f.hint;
            const common = {
              id,
              name: f.name,
              'aria-invalid': err ? 'true' : undefined,
              'aria-describedby': err ? `${id}-err` : hint ? `${id}-hint` : undefined,
            };
            let input;
            if (f.type === 'select') {
              const opts = typeof f.options === 'function' ? f.options(lookupData) : f.options || [];
              input = (
                <select {...common} value={values[f.name]} onChange={(e) => set(f.name, e.target.value)}>
                  <option value="">{f.emptyLabel || 'Select…'}</option>
                  {opts.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                </select>
              );
            } else if (f.type === 'textarea') {
              input = <textarea {...common} rows={5} value={values[f.name]} onChange={(e) => set(f.name, e.target.value)} />;
            } else if (f.type === 'checkbox') {
              input = (
                <input {...common} type="checkbox" checked={values[f.name]} onChange={(e) => set(f.name, e.target.checked)} />
              );
            } else {
              input = (
                <input
                  {...common}
                  type={f.type || 'text'}
                  step={f.step}
                  min={f.min}
                  autoComplete={f.type === 'password' ? 'new-password' : 'off'}
                  value={values[f.name]}
                  onChange={(e) => set(f.name, e.target.value)}
                />
              );
            }
            return (
              <div key={f.name} className={`field${f.type === 'checkbox' ? ' inline' : ''}${f.wide ? ' wide' : ''}`}>
                <label htmlFor={id}>
                  {f.label}
                  {f.required && !(f.type === 'password' && mode === 'edit') ? <span aria-hidden="true"> *</span> : null}
                </label>
                {input}
                {hint && !err && <p id={`${id}-hint`} className="hint">{hint}</p>}
                {err && <p id={`${id}-err`} className="field-error">{err}</p>}
              </div>
            );
          })}
        </div>
        {formError && <p className="form-error" role="alert">{formError}</p>}
        <div className="form-actions">
          <button type="button" className="btn" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn primary" disabled={saving}>
            {saving ? 'Saving…' : mode === 'create' ? `Add ${itemName}` : 'Save changes'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
