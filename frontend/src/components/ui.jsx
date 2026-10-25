import { useEffect, useRef } from 'react';

export function PageHeader({ title, children, note }) {
  return (
    <header className="page-header">
      <div>
        <h1>{title}</h1>
        {note && <p className="note">{note}</p>}
      </div>
      {children && <div className="page-actions">{children}</div>}
    </header>
  );
}

export function Loading({ what = 'Loading' }) {
  return <p className="muted" role="status">{what}…</p>;
}

export function ErrorNote({ error, onRetry }) {
  if (!error) return null;
  return (
    <div className="error-note" role="alert">
      <span>{error.message}</span>
      {onRetry && <button className="btn link" onClick={onRetry}>Try again</button>}
    </div>
  );
}

export function Empty({ children }) {
  return <p className="empty">{children}</p>;
}

export function Pill({ tone = 'neutral', children }) {
  return <span className={`pill ${tone}`}>{children}</span>;
}

const STATUS_TONE = {
  ACTIVE: 'ok', COMPLETED: 'info', DROPPED: 'muted',
  PRESENT: 'ok', LATE: 'warn', ABSENT: 'bad', EXCUSED: 'info',
};
const STATUS_LABEL = {
  ACTIVE: 'Active', COMPLETED: 'Completed', DROPPED: 'Dropped',
  PRESENT: 'Present', LATE: 'Late', ABSENT: 'Absent', EXCUSED: 'Excused',
};
export const StatusPill = ({ status }) => (
  <Pill tone={STATUS_TONE[status] || 'neutral'}>{STATUS_LABEL[status] || status}</Pill>
);

/** Native <dialog>: gives focus trapping, Escape to close and a backdrop for free. */
export function Modal({ title, onClose, children, wide }) {
  const ref = useRef(null);
  useEffect(() => {
    const d = ref.current;
    if (d && !d.open) d.showModal();
  }, []);
  return (
    <dialog
      ref={ref}
      className={`modal${wide ? ' wide' : ''}`}
      onClose={onClose}
      onClick={(e) => { if (e.target === ref.current) onClose(); }}
      aria-labelledby="modal-title"
    >
      <div className="modal-body">
        <header className="modal-head">
          <h2 id="modal-title">{title}</h2>
          <button type="button" className="icon-btn" onClick={onClose} aria-label="Close">×</button>
        </header>
        {children}
      </div>
    </dialog>
  );
}

export function ConfirmDialog({ title, message, confirmLabel = 'Delete', busy, onConfirm, onClose }) {
  return (
    <Modal title={title} onClose={onClose}>
      <p>{message}</p>
      <div className="form-actions">
        <button type="button" className="btn" onClick={onClose}>Cancel</button>
        <button type="button" className="btn danger" onClick={onConfirm} disabled={busy}>
          {busy ? 'Working…' : confirmLabel}
        </button>
      </div>
    </Modal>
  );
}
