import { useState } from 'react';
import api, { errMsg } from '../services/api.js';
import useLoad from '../hooks/useLoad.js';
import { useAuth } from '../context/AuthContext.jsx';
import { fmtDate, short } from '../utils/format.js';
import { Badge, Btn, Card, Input, Notice, Page, Select, Table, Td } from '../components/ui.jsx';

function download(name, text, type = 'application/json') {
  const url = URL.createObjectURL(new Blob([text], { type }));
  const a = document.createElement('a'); a.href = url; a.download = name; a.click(); URL.revokeObjectURL(url);
}

function Elections({ isAdmin }) {
  const list = useLoad(async () => (await api.get('/api/elections')).data, []);
  const stations = useLoad(async () => (await api.get('/api/polling-stations')).data, []);
  const key = useLoad(async () => (await api.get('/api/blockchain/authority-public-key')).data, []);
  const [msg, setMsg] = useState({ kind: 'ok', text: '' });
  const [f, setF] = useState({ code: '', name: '', type: 'MOCK' });
  const [pkg, setPkg] = useState({ electionId: '', stationCode: '', passphrase: '' });

  const call = async (fn, ok) => {
    try { await fn(); setMsg({ kind: 'ok', text: ok }); list.reload(); } catch (e) { setMsg({ kind: 'error', text: errMsg(e) }); }
  };
  const makePackage = async (e) => {
    e.preventDefault();
    try {
      const { data } = await api.post(`/api/elections/${pkg.electionId}/package`, { stationCode: pkg.stationCode, passphrase: pkg.passphrase });
      download(`${data.electionCode}-${data.stationCode}.vxpkg`, JSON.stringify(data, null, 2));
      setMsg({ kind: 'ok', text: 'Signed + encrypted package downloaded. Share the passphrase through a separate channel.' });
    } catch (x) { setMsg({ kind: 'error', text: errMsg(x) }); }
  };

  return (
    <div className="space-y-4">
      <Notice kind={msg.kind}>{msg.text}</Notice>
      <Notice>{list.error}</Notice>
      <Card title="Elections & roll versions">
        <Table head={['Code', 'Name', 'Type', 'Status', 'Roll', 'Window', 'Snapshot hash', 'Actions']}>
          {(list.data || []).map((e) => (
            <tr key={e.id}>
              <Td className="font-mono">{e.code}</Td><Td>{e.name}</Td><Td className="text-xs">{e.type}</Td><Td><Badge text={e.status} /></Td><Td>V{e.rollVersion}</Td>
              <Td className="text-xs">{fmtDate(e.pollingStart)}<br />{fmtDate(e.pollingEnd)}</Td>
              <Td className="font-mono text-xs">{short(e.snapshotHash, 16)}</Td>
              <Td>
                <div className="flex flex-wrap gap-1">
                  {e.status === 'DRAFT' && <Btn variant="ghost" onClick={() => call(() => api.post(`/api/elections/${e.id}/freeze`), 'Roll frozen; snapshot created.')}>Freeze roll</Btn>}
                  {isAdmin && e.status === 'ROLL_FROZEN' && <Btn variant="ghost" onClick={() => call(() => api.post(`/api/elections/${e.id}/open-polling`), 'Polling opened.')}>Open polling</Btn>}
                  {isAdmin && e.status === 'POLLING_OPEN' && (
                    <>
                      <Btn variant="ghost" onClick={() => call(() => api.post(`/api/elections/${e.id}/reset-window`, null, { params: { hours: 8 } }), 'Polling window reset to the next 8 hours.')}>Reset window (+8h)</Btn>
                      <Btn variant="danger" onClick={() => call(() => api.post(`/api/elections/${e.id}/close-polling`), 'Polling closed.')}>Close polling</Btn>
                    </>
                  )}
                </div>
              </Td>
            </tr>
          ))}
        </Table>
      </Card>
      {isAdmin && (
        <Card title="Create election">
          <form className="grid items-end gap-2 md:grid-cols-4" onSubmit={(e) => { e.preventDefault(); call(() => api.post('/api/elections', f), 'Election created (DRAFT).'); }}>
            <Input label="Code" value={f.code} onChange={(e) => setF({ ...f, code: e.target.value })} required />
            <Input label="Name" value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} required />
            <Select label="Type" value={f.type} onChange={(e) => setF({ ...f, type: e.target.value })}>{['MOCK', 'LOCAL_BODY', 'VIDHAN_SABHA', 'LOK_SABHA'].map((t) => <option key={t}>{t}</option>)}</Select>
            <Btn type="submit">Create</Btn>
          </form>
        </Card>
      )}
      <Card title="Offline election package (signed + encrypted)">
        <form className="grid items-end gap-2 md:grid-cols-4" onSubmit={makePackage}>
          <Select label="Election" value={pkg.electionId} onChange={(e) => setPkg({ ...pkg, electionId: e.target.value })} required>
            <option value="">Select…</option>{(list.data || []).filter((e) => e.status !== 'DRAFT').map((e) => <option key={e.id} value={e.id}>{e.code}</option>)}
          </Select>
          <Select label="Polling station" value={pkg.stationCode} onChange={(e) => setPkg({ ...pkg, stationCode: e.target.value })} required>
            <option value="">Select…</option>{(stations.data || []).map((s) => <option key={s.code} value={s.code}>{s.code} · {s.name}</option>)}
          </Select>
          <Input label="Passphrase (min 8)" type="password" value={pkg.passphrase} onChange={(e) => setPkg({ ...pkg, passphrase: e.target.value })} minLength={8} required />
          <Btn type="submit">Generate & download</Btn>
        </form>
        {key.data && (
          <div className="mt-3 text-xs text-slate-600">
            <div>Authority public key ({key.data.algorithm}). Copy it to the offline device out-of-band:</div>
            <div className="mt-1 break-all rounded bg-slate-50 p-2 font-mono">{key.data.publicKeyBase64}</div>
            <Btn variant="ghost" className="mt-2" onClick={() => download('authority-public.b64', key.data.publicKeyBase64, 'text/plain')}>Download authority-public.b64</Btn>
          </div>
        )}
      </Card>
    </div>
  );
}

function Users() {
  const list = useLoad(async () => (await api.get('/api/admin/users')).data, []);
  const [msg, setMsg] = useState({ kind: 'ok', text: '' });
  const [f, setF] = useState({ username: '', password: '', fullName: '', email: '', role: 'BLO' });
  const call = async (fn, ok) => { try { await fn(); setMsg({ kind: 'ok', text: ok }); list.reload(); } catch (e) { setMsg({ kind: 'error', text: errMsg(e) }); } };
  return (
    <div className="space-y-4">
      <Notice kind={msg.kind}>{msg.text}</Notice>
      <Card title="Users">
        <Table head={['Username', 'Name', 'Role', 'Enabled', '']}>
          {(list.data || []).map((u) => (
            <tr key={u.id}><Td className="font-mono">{u.username}</Td><Td>{u.fullName}</Td><Td><Badge text={u.role} /></Td><Td>{u.enabled ? 'Yes' : 'No'}</Td>
              <Td><Btn variant="ghost" onClick={() => call(() => api.post(`/api/admin/users/${u.id}/toggle`), 'User updated.')}>{u.enabled ? 'Disable' : 'Enable'}</Btn></Td></tr>
          ))}
        </Table>
      </Card>
      <Card title="Create user (least privilege: pick the lowest role needed)">
        <form className="grid items-end gap-2 md:grid-cols-6" onSubmit={(e) => { e.preventDefault(); call(() => api.post('/api/admin/users', f), 'User created.'); }}>
          <Input label="Username" value={f.username} onChange={(e) => setF({ ...f, username: e.target.value })} required />
          <Input label="Password (min 8)" type="password" value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} minLength={8} required />
          <Input label="Full name" value={f.fullName} onChange={(e) => setF({ ...f, fullName: e.target.value })} required />
          <Input label="Email" value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} />
          <Select label="Role" value={f.role} onChange={(e) => setF({ ...f, role: e.target.value })}>{['CITIZEN', 'BLO', 'ERO', 'POLLING_OFFICER', 'AUDITOR', 'SUPER_ADMIN'].map((r) => <option key={r}>{r}</option>)}</Select>
          <Btn type="submit">Create</Btn>
        </form>
      </Card>
    </div>
  );
}

export default function Admin() {
  const { user } = useAuth();
  const isAdmin = user.role === 'SUPER_ADMIN';
  const [tab, setTab] = useState('elections');
  return (
    <Page title="Administration" subtitle="Elections, roll freeze, offline packages and user management">
      <div className="flex gap-2">
        <Btn variant={tab === 'elections' ? 'primary' : 'ghost'} onClick={() => setTab('elections')}>Elections & packages</Btn>
        {isAdmin && <Btn variant={tab === 'users' ? 'primary' : 'ghost'} onClick={() => setTab('users')}>Users</Btn>}
      </div>
      {tab === 'elections' ? <Elections isAdmin={isAdmin} /> : <Users />}
    </Page>
  );
}
