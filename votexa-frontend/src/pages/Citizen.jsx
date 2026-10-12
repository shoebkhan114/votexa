import { useState } from 'react';
import api, { errMsg } from '../services/api.js';
import useLoad from '../hooks/useLoad.js';
import { fmtDate } from '../utils/format.js';
import { Badge, Btn, Card, Input, Notice, Page, Select, Table, Td } from '../components/ui.jsx';

const CATEGORIES = ['WRONG_ADDRESS', 'DUPLICATE_RECORD', 'INCORRECT_INFORMATION', 'WRONG_POLLING_STATION', 'DECEASED_PERSON_LISTED', 'OTHER'];

export default function Citizen() {
  const me = useLoad(async () => (await api.get('/api/voters/me')).data, []);
  const reqs = useLoad(async () => (await api.get('/api/address-changes/mine')).data, []);
  const cmps = useLoad(async () => (await api.get('/api/complaints/mine')).data, []);
  const [addr, setAddr] = useState({ line1: '', locality: '', pincode: '', reason: '' });
  const [cmp, setCmp] = useState({ category: 'WRONG_ADDRESS', description: '' });
  const [msg, setMsg] = useState({ kind: 'ok', text: '' });

  const submitAddr = async (e) => {
    e.preventDefault();
    try { await api.post('/api/address-changes', addr); setMsg({ kind: 'ok', text: 'Address change request submitted.' }); setAddr({ line1: '', locality: '', pincode: '', reason: '' }); reqs.reload(); }
    catch (err) { setMsg({ kind: 'error', text: errMsg(err) }); }
  };
  const submitCmp = async (e) => {
    e.preventDefault();
    try { await api.post('/api/complaints', cmp); setMsg({ kind: 'ok', text: 'Complaint submitted.' }); setCmp({ ...cmp, description: '' }); cmps.reload(); }
    catch (err) { setMsg({ kind: 'error', text: errMsg(err) }); }
  };

  const v = me.data;
  return (
    <Page title="My Voter Profile" subtitle="Citizen portal: view your record, request changes, file complaints, track applications">
      <Notice kind={msg.kind}>{msg.text}</Notice>
      <Notice>{me.error}</Notice>
      {v && (
        <Card title="Voter record">
          <div className="grid gap-2 text-sm md:grid-cols-3">
            <div><span className="text-slate-500">Voter ID</span><div className="font-mono font-semibold">{v.voterCode}</div></div>
            <div><span className="text-slate-500">Name</span><div>{v.fullName}</div></div>
            <div><span className="text-slate-500">Status</span><div><Badge text={v.status} /></div></div>
            <div><span className="text-slate-500">Constituency</span><div>{v.constituencyName} ({v.constituencyCode})</div></div>
            <div><span className="text-slate-500">Polling station</span><div>{v.pollingStationName ? `${v.pollingStationName} (${v.pollingStationCode})` : 'Not assigned'}</div></div>
            <div><span className="text-slate-500">Address</span><div>{v.address ? `${v.address.line1}, ${v.address.locality}, ${v.address.district} ${v.address.pincode}` : '-'}</div></div>
          </div>
        </Card>
      )}
      <div className="grid gap-4 lg:grid-cols-2">
        <Card title="Request address change">
          <form onSubmit={submitAddr} className="space-y-2">
            <Input label="New address line" value={addr.line1} onChange={(e) => setAddr({ ...addr, line1: e.target.value })} required />
            <Input label="Locality" value={addr.locality} onChange={(e) => setAddr({ ...addr, locality: e.target.value })} required />
            <Input label="Pincode (6 digits)" value={addr.pincode} pattern="[0-9]{6}" onChange={(e) => setAddr({ ...addr, pincode: e.target.value })} required />
            <Input label="Reason" value={addr.reason} onChange={(e) => setAddr({ ...addr, reason: e.target.value })} />
            <Btn type="submit">Submit request</Btn>
          </form>
        </Card>
        <Card title="Submit a complaint">
          <form onSubmit={submitCmp} className="space-y-2">
            <Select label="Category" value={cmp.category} onChange={(e) => setCmp({ ...cmp, category: e.target.value })}>
              {CATEGORIES.map((c) => <option key={c} value={c}>{c.replaceAll('_', ' ')}</option>)}
            </Select>
            <label className="block text-sm"><span className="mb-1 block text-slate-600">Description</span>
              <textarea className="w-full rounded border border-slate-300 px-2 py-1.5" rows={3} value={cmp.description}
                onChange={(e) => setCmp({ ...cmp, description: e.target.value })} required />
            </label>
            <Btn type="submit">Submit complaint</Btn>
          </form>
        </Card>
      </div>
      <Card title="My address change requests">
        <Table head={['Request', 'New address', 'Status', 'Remarks', 'Submitted']}>
          {(reqs.data || []).map((r) => (
            <tr key={r.id}><Td className="font-mono">{r.requestCode}</Td><Td>{r.newLine1}, {r.newLocality} {r.newPincode}</Td>
              <Td><Badge text={r.status} /></Td><Td>{r.decisionRemarks}</Td><Td>{fmtDate(r.createdAt)}</Td></tr>
          ))}
        </Table>
      </Card>
      <Card title="My complaints">
        <Table head={['Case', 'Category', 'Description', 'Status', 'Decision']}>
          {(cmps.data || []).map((c) => (
            <tr key={c.id}><Td className="font-mono">{c.caseCode}</Td><Td>{c.category.replaceAll('_', ' ')}</Td><Td>{c.description}</Td>
              <Td><Badge text={c.status} /></Td><Td>{c.decision}</Td></tr>
          ))}
        </Table>
      </Card>
    </Page>
  );
}
