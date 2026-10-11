import { useState } from 'react';
import api, { errMsg } from '../services/api.js';
import useLoad from '../hooks/useLoad.js';
import { useAuth } from '../context/AuthContext.jsx';
import { fmtDate } from '../utils/format.js';
import { Badge, Btn, Card, Input, Notice, Page, Select, Table, Td } from '../components/ui.jsx';

const STATUSES = ['ACTIVE', 'PENDING', 'UNDER_VERIFICATION', 'SHIFTED', 'DECEASED', 'DUPLICATE_REVIEW', 'INACTIVE', 'BLOCKED'];

function Profile({ code, canEdit, onChanged }) {
  const v = useLoad(async () => (await api.get(`/api/voters/${code}`)).data, [code]);
  const h = useLoad(async () => (await api.get(`/api/voters/${code}/history`)).data, [code]);
  const [f, setF] = useState({ newStatus: 'INACTIVE', reason: '', pollingStationCode: '' });
  const [msg, setMsg] = useState({ kind: 'ok', text: '' });

  const change = async (e) => {
    e.preventDefault();
    try {
      await api.post(`/api/voters/${code}/status`, f);
      setMsg({ kind: 'ok', text: 'Status updated and audit event recorded.' });
      setF({ ...f, reason: '' }); v.reload(); h.reload(); onChanged();
    } catch (err) { setMsg({ kind: 'error', text: errMsg(err) }); }
  };

  const d = v.data;
  return (
    <Card title={`Voter profile · ${code}`}>
      <Notice>{v.error}</Notice>
      {d && (
        <div className="grid gap-2 text-sm md:grid-cols-3">
          <div><span className="text-slate-500">Name</span><div className="font-medium">{d.fullName}</div></div>
          <div><span className="text-slate-500">DOB / Gender</span><div>{d.dob} · {d.gender}</div></div>
          <div><span className="text-slate-500">Status</span><div><Badge text={d.status} /></div></div>
          <div><span className="text-slate-500">Constituency</span><div>{d.constituencyName} ({d.constituencyCode})</div></div>
          <div><span className="text-slate-500">Polling station</span><div>{d.pollingStationCode || 'Not assigned'}</div></div>
          <div><span className="text-slate-500">Address</span><div>{d.address ? `${d.address.line1}, ${d.address.locality} ${d.address.pincode}` : '-'}</div></div>
        </div>
      )}
      <h3 className="mt-4 text-xs font-semibold uppercase text-slate-500">Status history</h3>
      <Table head={['When', 'Change', 'Reason', 'Ref']} empty="No status changes">
        {(h.data || []).map((s, i) => (
          <tr key={i}><Td>{fmtDate(s.at)}</Td><Td><Badge text={s.oldStatus} /> → <Badge text={s.newStatus} /></Td><Td>{s.reason}</Td><Td className="font-mono">{s.sourceRef}</Td></tr>
        ))}
      </Table>
      {canEdit && (
        <form onSubmit={change} className="mt-4 grid items-end gap-2 md:grid-cols-4">
          <Select label="New status" value={f.newStatus} onChange={(e) => setF({ ...f, newStatus: e.target.value })}>
            {STATUSES.map((s) => <option key={s}>{s}</option>)}
          </Select>
          <Input label="Reason (required)" value={f.reason} onChange={(e) => setF({ ...f, reason: e.target.value })} required />
          <Input label="Polling station (for ACTIVE)" placeholder="PS-101" value={f.pollingStationCode} onChange={(e) => setF({ ...f, pollingStationCode: e.target.value })} />
          <Btn type="submit">Change status</Btn>
        </form>
      )}
      <Notice kind={msg.kind}>{msg.text}</Notice>
    </Card>
  );
}

function CreateVoter({ onDone }) {
  const [f, setF] = useState({ fullName: '', dob: '', gender: 'MALE', constituencyCode: 'RJ-JP-01', line1: '', locality: '', pincode: '' });
  const [res, setRes] = useState(null);
  const [err, setErr] = useState('');
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const submit = async (e) => {
    e.preventDefault(); setErr('');
    try { setRes((await api.post('/api/voters', f)).data); onDone(); } catch (x) { setErr(errMsg(x)); }
  };
  return (
    <Card title="Register new voter (dummy data only)">
      <form onSubmit={submit} className="grid gap-2 md:grid-cols-4">
        <Input label="Full name" value={f.fullName} onChange={set('fullName')} required />
        <Input label="Date of birth" type="date" value={f.dob} onChange={set('dob')} required />
        <Select label="Gender" value={f.gender} onChange={set('gender')}><option>MALE</option><option>FEMALE</option><option>OTHER</option></Select>
        <Input label="Constituency code" value={f.constituencyCode} onChange={set('constituencyCode')} required />
        <Input label="Address line" value={f.line1} onChange={set('line1')} required />
        <Input label="Locality" value={f.locality} onChange={set('locality')} required />
        <Input label="Pincode" value={f.pincode} pattern="[0-9]{6}" onChange={set('pincode')} required />
        <div className="flex items-end"><Btn type="submit">Register</Btn></div>
      </form>
      <Notice>{err}</Notice>
      {res && (
        <Notice kind={res.possibleDuplicates.length ? 'warn' : 'ok'}>
          Created {res.voter.voterCode} (PENDING).
          {res.possibleDuplicates.length > 0 && ` Possible duplicates: ${res.possibleDuplicates.map((d) => `${d.voterCode} (${d.score}%)`).join(', ')}. Run duplicate review.`}
        </Notice>
      )}
    </Card>
  );
}

export default function Voters() {
  const { user } = useAuth();
  const canEdit = ['ERO', 'SUPER_ADMIN'].includes(user.role);
  const [q, setQ] = useState('');
  const [status, setStatus] = useState('');
  const [sel, setSel] = useState(null);
  const [showNew, setShowNew] = useState(false);
  const list = useLoad(async () => (await api.get('/api/voters', { params: { q, status, limit: 100 } })).data, [q, status]);

  return (
    <Page title="Voter Registry" subtitle="Search simulated voter records (dummy data)">
      <Card actions={canEdit && <Btn variant="ghost" onClick={() => setShowNew(!showNew)}>{showNew ? 'Close form' : 'New voter'}</Btn>}>
        <div className="grid gap-2 md:grid-cols-3">
          <Input label="Search name or voter ID" value={q} onChange={(e) => setQ(e.target.value)} placeholder="e.g. Rahul or VTX0000003" />
          <Select label="Status" value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">All</option>{STATUSES.map((s) => <option key={s}>{s}</option>)}
          </Select>
        </div>
      </Card>
      {showNew && canEdit && <CreateVoter onDone={list.reload} />}
      <Notice>{list.error}</Notice>
      <Card title={`Results (${list.data?.length ?? 0})`}>
        <Table head={['Voter ID', 'Name', 'DOB', 'Status', 'Constituency', 'Station']}>
          {(list.data || []).map((v) => (
            <tr key={v.voterCode} onClick={() => setSel(v.voterCode)} className="cursor-pointer hover:bg-slate-50">
              <Td className="font-mono">{v.voterCode}</Td><Td>{v.fullName}</Td><Td>{v.dob}</Td><Td><Badge text={v.status} /></Td>
              <Td>{v.constituencyCode}</Td><Td>{v.pollingStationCode || '-'}</Td>
            </tr>
          ))}
        </Table>
      </Card>
      {sel && <Profile code={sel} canEdit={canEdit} onChanged={list.reload} />}
    </Page>
  );
}
