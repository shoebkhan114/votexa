import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { staffRoles } from '../utils/format.js';

const NAV = [
  { to: '/dashboard', label: 'Dashboard', roles: staffRoles },
  { to: '/my', label: 'My Voter Profile', roles: ['CITIZEN'] },
  { to: '/voters', label: 'Voter Search', roles: staffRoles },
  { to: '/verify', label: 'Verification', roles: ['BLO', 'ERO', 'POLLING_OFFICER', 'SUPER_ADMIN'] },
  { to: '/address-changes', label: 'Address Changes', roles: ['BLO', 'ERO', 'SUPER_ADMIN'] },
  { to: '/duplicates', label: 'Duplicate Cases', roles: ['BLO', 'ERO', 'SUPER_ADMIN'] },
  { to: '/death-cases', label: 'Deceased / Inactive', roles: ['BLO', 'ERO', 'SUPER_ADMIN'] },
  { to: '/polling', label: 'Polling Dashboard', roles: ['POLLING_OFFICER', 'ERO', 'AUDITOR', 'SUPER_ADMIN'] },
  { to: '/audit', label: 'Blockchain Audit', roles: ['AUDITOR', 'ERO', 'SUPER_ADMIN'] },
  { to: '/reports', label: 'Reports', roles: ['POLLING_OFFICER', 'ERO', 'AUDITOR', 'SUPER_ADMIN'] },
  { to: '/complaints', label: 'Complaints', roles: ['BLO', 'ERO', 'SUPER_ADMIN'] },
  { to: '/admin', label: 'Admin', roles: ['ERO', 'SUPER_ADMIN'] },
];

export default function Layout() {
  const { user, logout } = useAuth();
  const nav = useNavigate();
  const items = NAV.filter((n) => n.roles.includes(user.role));
  return (
    <div className="flex min-h-screen">
      <aside className="w-56 shrink-0 bg-brand-900 text-indigo-100">
        <div className="px-4 py-4 text-lg font-bold tracking-wide text-white">VOTEXA</div>
        <nav className="space-y-0.5 px-2 pb-4">
          {items.map((n) => (
            <NavLink key={n.to} to={n.to}
              className={({ isActive }) => `block rounded px-3 py-2 text-sm ${isActive ? 'bg-brand-600 text-white' : 'hover:bg-brand-700'}`}>
              {n.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex items-center justify-between border-b bg-white px-6 py-3">
          <span className="rounded bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800">Academic prototype · dummy data only</span>
          <div className="flex items-center gap-3 text-sm">
            <span className="text-slate-600">{user.fullName} <span className="text-slate-400">({user.role})</span></span>
            <button onClick={() => { logout(); nav('/login'); }} className="rounded border px-2 py-1 text-slate-700 hover:bg-slate-50">Logout</button>
          </div>
        </header>
        <main className="min-w-0 flex-1 p-6"><Outlet /></main>
      </div>
    </div>
  );
}
