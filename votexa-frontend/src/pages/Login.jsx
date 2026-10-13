import { useState } from 'react';
import { useNavigate, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { errMsg } from '../services/api.js';
import { Input, Notice } from '../components/ui.jsx';

const DEMO = [
  ['admin', 'Super Admin'], ['ero102', 'ERO'], ['blo101', 'BLO'], ['po102', 'Polling Officer'],
  ['auditor1', 'Auditor'], ['citizen1', 'Citizen'],
];

const FEATURES = [
  'Officer-approved electoral roll changes',
  'Right voter, right polling station, one-time authorization',
  'Tamper-evident audit trail with hash chain',
  'Offline polling validation with signed election packages',
];

const pageBg = {
  backgroundImage:
    'radial-gradient(circle at 15% 20%, rgba(255,255,255,0.10) 0, transparent 38%), ' +
    'radial-gradient(circle at 85% 85%, rgba(59,130,246,0.30) 0, transparent 45%), ' +
    'linear-gradient(135deg, #0b1f3a 0%, #123a6b 55%, #0b2a4a 100%)',
};
const dotsBg = {
  backgroundImage: 'radial-gradient(rgba(255,255,255,0.10) 1px, transparent 1px)',
  backgroundSize: '22px 22px',
};

function ShieldIcon({ className = 'h-9 w-9' }) {
  return (
    <svg viewBox="0 0 24 24" className={className} fill="none" stroke="currentColor" strokeWidth="1.8"
      strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M12 3l7 3v5c0 4.5-3 8-7 10-4-2-7-5.5-7-10V6l7-3z" />
      <path d="M9 12l2 2 4-4" />
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg viewBox="0 0 24 24" className="mt-0.5 h-5 w-5 shrink-0 text-amber-400" fill="none" stroke="currentColor"
      strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <circle cx="12" cy="12" r="9" />
      <path d="M8 12l3 3 5-6" />
    </svg>
  );
}

export default function Login() {
  const { user, login } = useAuth();
  const nav = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to="/" replace />;

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true); setError('');
    try { await login(username.trim(), password); nav('/'); } catch (err) { setError(errMsg(err)); } finally { setBusy(false); }
  };

  return (
    <div className="relative flex min-h-screen flex-col text-slate-800" style={pageBg}>
      <div className="pointer-events-none absolute inset-0" style={dotsBg} />

      <div className="relative z-10 bg-slate-950/70 px-4 py-1.5 text-center text-xs text-amber-200">
        Academic prototype · dummy data only · not an official Election Commission website
      </div>

      <header className="relative z-10 border-b-4 border-amber-500 bg-white shadow">
        <div className="mx-auto flex max-w-6xl items-center gap-3 px-4 py-3">
          <span className="text-blue-900"><ShieldIcon /></span>
          <div>
            <div className="text-xl font-bold leading-tight tracking-wide text-blue-900">VOTEXA</div>
            <div className="text-xs text-slate-600 sm:text-sm">
              Electoral Roll Integrity, Voter Validation and Polling Audit System
            </div>
          </div>
        </div>
      </header>

      <main className="relative z-10 mx-auto flex w-full max-w-6xl flex-1 flex-col items-center gap-10 px-4 py-10 lg:flex-row lg:justify-between">
        <section className="hidden max-w-xl text-white lg:block">
          <p className="mb-2 text-sm font-semibold uppercase tracking-widest text-amber-300">Electoral integrity platform</p>
          <h1 className="text-4xl font-bold leading-tight">Verified records. Accountable changes. Auditable polling.</h1>
          <p className="mt-4 text-blue-100">
            A prototype that shows how electoral roll updates, voter validation and polling activity can be reviewed
            and audited, without storing any vote choice.
          </p>
          <ul className="mt-6 space-y-3">
            {FEATURES.map((f) => (
              <li key={f} className="flex items-start gap-3 text-blue-50"><CheckIcon /><span>{f}</span></li>
            ))}
          </ul>
        </section>

        <div className="w-full max-w-md overflow-hidden rounded-lg bg-white shadow-2xl">
          <div className="bg-blue-900 px-6 py-4 text-white">
            <h2 className="text-lg font-semibold">Sign in</h2>
            <p className="text-xs text-blue-200">Authorized users only. Access depends on your role.</p>
          </div>
          <div className="p-6">
            <form onSubmit={submit} className="space-y-3">
              <Input label="Username" value={username} onChange={(e) => setUsername(e.target.value)}
                autoComplete="username" autoFocus required />
              <Input label="Password" type="password" value={password} onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password" required />
              <Notice>{error}</Notice>
              <button type="submit" disabled={busy}
                className="w-full rounded bg-blue-900 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-blue-800 disabled:opacity-60">
                {busy ? 'Signing in…' : 'Sign in'}
              </button>
            </form>

            <div className="mt-6 rounded-md border border-amber-200 bg-amber-50 p-3 text-xs text-slate-600">
              <div className="mb-2 font-semibold text-amber-900">
                Demo accounts (password <code className="rounded bg-white px-1">Votexa@123</code>, dummy data)
              </div>
              <div className="flex flex-wrap gap-1.5">
                {DEMO.map(([u, r]) => (
                  <button key={u} type="button" onClick={() => { setUsername(u); setPassword('Votexa@123'); }}
                    className="rounded-full border border-amber-300 bg-white px-2.5 py-0.5 text-slate-700 hover:bg-amber-100">
                    {u} · {r}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>
      </main>

      <footer className="relative z-10 border-t border-white/10 bg-slate-950/60 px-4 py-3 text-center text-xs text-blue-200">
        VOTEXA · B.Tech Java PBL project (A.Y. 2026-27) · Prototype with dummy data and mock government services.
        Not a replacement for official election systems.
      </footer>
    </div>
  );
}
