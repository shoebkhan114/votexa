import { useEffect, useState } from 'react';
import api, { errMsg } from '../services/api.js';
import useLoad from '../hooks/useLoad.js';
import { useAuth } from '../context/AuthContext.jsx';
import { fmtDate } from '../utils/format.js';
import { Badge, Btn, Card, Input, Notice, Page, Select, Stat, Table, Td } from '../components/ui.jsx';

export default function Polling() {
  const { user } = useAuth();
  const isPo = ['POLLING_OFFICER', 'SUPER_ADMIN'].includes(user.role);
  const sessions = useLoad(async () => (await api.get('/api/polling/sessions')).data, [], 8000);
  const [sid, setSid] = useState('');
  useEffect(() => { if (!sid && sessions.data?.length) setSid(String(sessions.data.find((s) => s.status === 'OPEN')?.sessionId || sessions.data[0].sessionId)); }, [sessions.data, sid]);

  const stats = sessions.data?.find((s) => String(s.sessionId) === sid);
  const attempts = useLoad(async () => (sid ? (await api.get(`/api/polling/sessions/${sid}/attempts`)).data : []), [sid], 5000);

  const [voterCode, setVoterCode] = useState('');
  const [confirmed, setConfirmed] = useState(true);
  const [res, setRes] = useState(null);
  const [msg, setMsg] = useState({ kind: 'ok', text: '' });
  const [recorded, setRecorded] = useState('');
  const [syncRes, setSyncRes] = useState(null);

  const call = async (action) => {
    if (!stats) return;
    setMsg({ kind: 'ok', text: '' });
    try {
      const { data } = await api.post(`/api/polling/${action}`, { stationCode: stats.stationCode, voterCode: voterCode.trim(), identityConfirmed: confirmed });
      setRes(data); sessions.reload(); attempts.reload();
    } catch (e) { setRes(null); setMsg({ kind: 'error', text: errMsg(e) }); }
  };
  const sessionAction = async (path, body) => {
    try { const { data } = await api.post(`/api/polling/sessions/${sid}/${path}`, body); setMsg({ kind: 'ok', text: path === 'close' ? `Session closed: ${data.status}. ${data.note}` : 'Session opened.' }); sessions.reload(); }
    catch (e) { setMsg({ kind: 'error', text: errMsg(e) }); }
  };
  const uploadSync = async (e) => {
    const file = e.target.files?.[0]; if (!file) return;
    try {
      const body = JSON.parse(await file.text());
      setSyncRes((await api.post('/api/polling/sync', body)).data); sessions.reload(); setMsg({ kind: 'ok', text: 'Offline sync file applied.' });
    } catch (x) { setMsg({ kind: 'error', text: errMsg(x) }); }
    e.target.value = '';
  };

  const good = res && (res.result === 'ELIGIBLE' || res.result === 'AUTHORIZED');
  return (
    <Page title="Polling Dashboard" subtitle="Eligibility validation and one-time voting authorization (no vote choice is ever stored)">
      <Notice kind={msg.kind}>{msg.text}</Notice>
      <Notice>{sessions.error}</Notice>
      <Card>
        <div className="grid items-end gap-2 md:grid-cols-4">
          <Select label="Polling session" value={sid} onChange={(e) => { setSid(e.target.value); setRes(null); }}>
            {(sessions.data || []).map((s) => <option key={s.sessionId} value={s.sessionId}>{s.stationCode} · {s.stationName} ({s.status})</option>)}
          </Select>
          {stats && <div className="text-sm">Status: <Badge text={stats.status} /></div>}
          {isPo && stats?.status === 'NOT_STARTED' && <Btn onClick={() => sessionAction('open')}>Open session</Btn>}
        </div>
      </Card>
      {stats && (
        <div className="grid grid-cols-2 gap-3 md:grid-cols-5">
          <Stat label="Eligible voters" value={stats.eligible} />
          <Stat label="Authenticated" value={stats.authenticated} />
          <Stat label="Authorized" value={stats.authorized} tone="text-green-700" />
          <Stat label="Remaining" value={stats.remaining} tone="text-amber-600" />
          <Stat label="Anomalies" value={stats.anomalies} tone={stats.anomalies ? 'text-red-700' : 'text-slate-900'} />
        </div>
      )}
      {isPo && stats && (
        <Card title={`Voter validation · ${stats.stationCode}`}>
          <div className="grid items-end gap-2 md:grid-cols-4">
            <Input label="Voter ID" value={voterCode} onChange={(e) => setVoterCode(e.target.value)} placeholder="VTX0000001" />
            <label className="flex items-center gap-2 pb-2 text-sm"><input type="checkbox" checked={confirmed} onChange={(e) => setConfirmed(e.target.checked)} />Identity confirmed by officer</label>
            <div className="flex gap-2">
              <Btn variant="ghost" onClick={() => call('validate')} disabled={!voterCode.trim()}>Validate</Btn>
              <Btn onClick={() => call('authorize')} disabled={!voterCode.trim()}>Authorize voting</Btn>
            </div>
          </div>
          {res && (
            <div className="mt-3">
              <div className={`rounded px-3 py-2 text-sm font-semibold ${good ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>
                {good ? '✓' : '✗'} {res.result.replaceAll('_', ' ')}: {res.message}{res.voterName ? ` (${res.voterName})` : ''}
              </div>
              <ul className="mt-2 grid gap-1 text-sm md:grid-cols-2">
                {res.checks.map((c) => <li key={c.name}>{c.passed ? '✓' : '✗'} {c.name}</li>)}
              </ul>
            </div>
          )}
        </Card>
      )}
      {isPo && stats?.status === 'OPEN' && (
        <Card title="Close session & reconcile">
          <div className="flex items-end gap-2">
            <Input label="Recorded / cast count reported by the machine" type="number" min="0" value={recorded} onChange={(e) => setRecorded(e.target.value)} className="w-80" />
            <Btn variant="danger" disabled={recorded === ''} onClick={() => sessionAction('close', { recordedCount: Number(recorded) })}>Close session</Btn>
          </div>
          <p className="mt-2 text-xs text-slate-500">Rule: recorded ≤ authorized ≤ eligible. A mismatch raises a "reconciliation required" alert; it never invalidates the election automatically.</p>
        </Card>
      )}
      {isPo && (
        <Card title="Offline validator sync">
          <p className="mb-2 text-sm text-slate-600">Upload the sync file exported by the JavaFX offline validator.</p>
          <input type="file" accept=".json" onChange={uploadSync} className="text-sm" />
          {syncRes && <Notice kind={syncRes.conflicts ? 'warn' : 'ok'}>Applied {syncRes.applied}, conflicts {syncRes.conflicts}, unknown voters {syncRes.unknownVoters}, wrong station {syncRes.wrongStation}.</Notice>}
        </Card>
      )}
      <Card title="Recent attempts">
        <Table head={['Time', 'Voter ID entered', 'Result', 'Detail']} empty="No attempts yet">
          {(attempts.data || []).map((a) => (
            <tr key={a.id}><Td>{fmtDate(a.at)}</Td><Td className="font-mono">{a.voterCodeEntered}</Td><Td><Badge text={a.result} /></Td><Td>{a.detail}</Td></tr>
          ))}
        </Table>
      </Card>
    </Page>
  );
}
