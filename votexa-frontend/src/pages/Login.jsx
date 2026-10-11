import { useState } from 'react';
import { useNavigate, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { errMsg } from '../services/api.js';
import { Btn, Input, Notice } from '../components/ui.jsx';

const DEMO = [
  ['admin', 'Super Admin'], ['ero102', 'ERO'], ['blo101', 'BLO'], ['po102', 'Polling Officer'],
  ['auditor1', 'Auditor'], ['citizen1', 'Citizen'],
];

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
    <div className="flex min-h-screen items-center justify-center bg-brand-900 p-4">
      <div className="w-full max-w-md rounded-xl bg-white p-8 shadow-xl">
        <h1 className="text-2xl font-bold text-brand-900">VOTEXA</h1>
        <p className="mb-6 text-sm text-slate-500">Blockchain-assisted electoral roll integrity, voter validation and polling audit (academic prototype)</p>
        <form onSubmit={submit} className="space-y-3">
          <Input label="Username" value={username} onChange={(e) => setUsername(e.target.value)} autoFocus required />
          <Input label="Password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          <Notice>{error}</Notice>
          <Btn type="submit" disabled={busy} className="w-full">{busy ? 'Signing in…' : 'Sign in'}</Btn>
        </form>
        <div className="mt-6 border-t pt-4 text-xs text-slate-500">
          <div className="mb-1 font-medium">Demo accounts (password <code>Votexa@123</code>, dummy data):</div>
          <div className="flex flex-wrap gap-1">
            {DEMO.map(([u, r]) => (
              <button key={u} type="button" onClick={() => { setUsername(u); setPassword('Votexa@123'); }}
                className="rounded border px-2 py-0.5 hover:bg-slate-50">{u} · {r}</button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
