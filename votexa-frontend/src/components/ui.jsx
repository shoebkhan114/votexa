const BADGE = {
  ACTIVE: 'bg-green-100 text-green-800', VERIFIED: 'bg-green-100 text-green-800', APPROVED: 'bg-green-100 text-green-800',
  COMMITTED: 'bg-green-100 text-green-800', RESOLVED: 'bg-green-100 text-green-800', MATCH: 'bg-green-100 text-green-800',
  CONFIRMED: 'bg-green-100 text-green-800', AUTHORIZED: 'bg-green-100 text-green-800', ELIGIBLE: 'bg-green-100 text-green-800',
  OPEN: 'bg-blue-100 text-blue-800', PENDING: 'bg-amber-100 text-amber-800', UNDER_VERIFICATION: 'bg-amber-100 text-amber-800',
  UNDER_REVIEW: 'bg-amber-100 text-amber-800', DUPLICATE_REVIEW: 'bg-amber-100 text-amber-800', REVIEW: 'bg-amber-100 text-amber-800',
  UNVERIFIED: 'bg-slate-100 text-slate-700', NOT_STARTED: 'bg-slate-100 text-slate-700', NOT_VOTED: 'bg-slate-100 text-slate-700',
  SHIFTED: 'bg-purple-100 text-purple-800', INACTIVE: 'bg-slate-200 text-slate-700', DECEASED: 'bg-slate-300 text-slate-800',
  BLOCKED: 'bg-red-100 text-red-800', REJECTED: 'bg-red-100 text-red-800', FAILED: 'bg-red-100 text-red-800',
  VIOLATION: 'bg-red-100 text-red-800', CRITICAL: 'bg-red-100 text-red-800', RECONCILIATION_REQUIRED: 'bg-red-100 text-red-800',
  VOTED: 'bg-indigo-100 text-indigo-800', CLOSED: 'bg-slate-200 text-slate-700', ACKNOWLEDGED: 'bg-blue-100 text-blue-800',
};

export function Badge({ text }) {
  if (!text) return null;
  return <span className={`inline-block rounded px-2 py-0.5 text-xs font-medium ${BADGE[text] || 'bg-slate-100 text-slate-700'}`}>{String(text).replaceAll('_', ' ')}</span>;
}

export function Card({ title, actions, children, className = '' }) {
  return (
    <section className={`rounded-lg bg-white p-4 shadow-sm ring-1 ring-slate-200 ${className}`}>
      {(title || actions) && (
        <div className="mb-3 flex items-center justify-between gap-2">
          <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-600">{title}</h2>
          <div className="flex gap-2">{actions}</div>
        </div>
      )}
      {children}
    </section>
  );
}

export function Stat({ label, value, tone = 'text-slate-900' }) {
  return (
    <div className="rounded-lg bg-white p-4 shadow-sm ring-1 ring-slate-200">
      <div className="text-xs uppercase tracking-wide text-slate-500">{label}</div>
      <div className={`mt-1 text-2xl font-bold ${tone}`}>{value ?? '-'}</div>
    </div>
  );
}

export function Btn({ children, variant = 'primary', className = '', ...p }) {
  const v = {
    primary: 'bg-brand-600 text-white hover:bg-brand-700',
    ghost: 'bg-white text-slate-700 ring-1 ring-slate-300 hover:bg-slate-50',
    danger: 'bg-red-600 text-white hover:bg-red-700',
    success: 'bg-green-600 text-white hover:bg-green-700',
  }[variant];
  return <button className={`rounded px-3 py-1.5 text-sm font-medium disabled:opacity-50 ${v} ${className}`} {...p}>{children}</button>;
}

export function Input({ label, className = '', ...p }) {
  return (
    <label className={`block text-sm ${className}`}>
      {label && <span className="mb-1 block text-slate-600">{label}</span>}
      <input className="w-full rounded border border-slate-300 px-2 py-1.5 focus:border-brand-600 focus:outline-none" {...p} />
    </label>
  );
}

export function Select({ label, children, className = '', ...p }) {
  return (
    <label className={`block text-sm ${className}`}>
      {label && <span className="mb-1 block text-slate-600">{label}</span>}
      <select className="w-full rounded border border-slate-300 bg-white px-2 py-1.5 focus:border-brand-600 focus:outline-none" {...p}>{children}</select>
    </label>
  );
}

export function Notice({ kind = 'error', children }) {
  if (!children) return null;
  const c = kind === 'error' ? 'bg-red-50 text-red-800 ring-red-200' : kind === 'ok' ? 'bg-green-50 text-green-800 ring-green-200' : 'bg-amber-50 text-amber-800 ring-amber-200';
  return <div className={`my-2 rounded px-3 py-2 text-sm ring-1 ${c}`}>{children}</div>;
}

export function Table({ head, children, empty = 'No records' }) {
  const rows = Array.isArray(children) ? children.length : children ? 1 : 0;
  return (
    <div className="overflow-x-auto">
      <table className="min-w-full text-left text-sm">
        <thead className="border-b bg-slate-50 text-xs uppercase text-slate-500">
          <tr>{head.map((h) => <th key={h} className="px-3 py-2 font-medium">{h}</th>)}</tr>
        </thead>
        <tbody className="divide-y">{children}</tbody>
      </table>
      {rows === 0 && <div className="p-4 text-center text-sm text-slate-500">{empty}</div>}
    </div>
  );
}

export const Td = ({ children, className = '' }) => <td className={`px-3 py-2 align-top ${className}`}>{children}</td>;
export const Page = ({ title, subtitle, children }) => (
  <div className="space-y-4">
    <div>
      <h1 className="text-xl font-bold text-slate-900">{title}</h1>
      {subtitle && <p className="text-sm text-slate-500">{subtitle}</p>}
    </div>
    {children}
  </div>
);
