import { useState } from 'react';
import api, { errMsg } from '../services/api.js';
import useLoad from '../hooks/useLoad.js';
import { useAuth } from '../context/AuthContext.jsx';
import { Badge, Btn, Card, Input, Notice, Page, Table, Td } from '../components/ui.jsx';

export default function Duplicates() {
  const { user } = useAuth();
  const isEro = ['ERO', 'SUPER_ADMIN'].includes(user.role);
  const list = useLoad(async () => (await api.get('/api/duplicates')).data, []);
  const [msg, setMsg] = useState({ kind: 'ok', text: '' });
  const [chk, setChk] = useState({ fullName: '', dob: '', address: '' });
  const [matches, setMatches] = useState(null);

  const scan = async () => {
    try { const { data } = await api.post('/api/duplicates/scan'); setMsg({ kind: 'ok', text: `Scan complete: ${data.pairsChecked} pairs checked, ${data.casesCreated} new case(s).` }); list.reload(); }
    catch (e) { setMsg({ kind: 'error', text: errMsg(e) }); }
  };
  const review = async (id, decision) => {
    const remarks = window.prompt(decision === 'CONFIRM' ? 'Remarks for confirming duplicate:' : 'Remarks for rejecting match:') || '';
    try { await api.post(`/api/duplicates/${id}/review`, { decision, remarks }); setMsg({ kind: 'ok', text: `Case ${decision === 'CONFIRM' ? 'confirmed' : 'rejected'}.` }); list.reload(); }
    catch (e) { setMsg({ kind: 'error', text: errMsg(e) }); }
  };
  const check = async (e) => {
    e.preventDefault();
    try { setMatches((await api.post('/api/voters/duplicate-check', chk)).data); } catch (x) { setMsg({ kind: 'error', text: errMsg(x) }); }
  };

  return (
    <Page title="Duplicate Voter Detection" subtitle="Rule-based + Java similarity scoring (name 50%, DOB 30%, address 20%). Decision support only: an officer confirms or rejects.">
      <Notice kind={msg.kind}>{msg.text}</Notice>
      <Notice>{list.error}</Notice>
      <Card title="Cases" actions={isEro && <Btn onClick={scan}>Run duplicate scan</Btn>}>
        <Table head={['Case', 'Voter A', 'Voter B', 'Potential duplicate', 'Matched on', 'Status', 'Actions']}>
          {(list.data || []).map((c) => (
            <tr key={c.id}>
              <Td className="font-mono">{c.caseCode}</Td>
              <Td>{c.voterA.fullName}<div className="font-mono text-xs text-slate-500">{c.voterA.voterCode} · {c.voterA.dob}</div></Td>
              <Td>{c.voterB.fullName}<div className="font-mono text-xs text-slate-500">{c.voterB.voterCode} · {c.voterB.dob}</div></Td>
              <Td><b>{c.similarity}%</b></Td>
              <Td className="text-xs">{c.matchedAttributes}</Td>
              <Td><Badge text={c.status} /></Td>
              <Td>{isEro && c.status === 'OPEN' && (
                <div className="flex gap-1"><Btn variant="danger" onClick={() => review(c.id, 'CONFIRM')}>Confirm duplicate</Btn><Btn variant="ghost" onClick={() => review(c.id, 'REJECT')}>Reject match</Btn></div>)}</Td>
            </tr>
          ))}
        </Table>
      </Card>
      <Card title="Check a record against the registry">
        <form onSubmit={check} className="grid items-end gap-2 md:grid-cols-4">
          <Input label="Full name" value={chk.fullName} onChange={(e) => setChk({ ...chk, fullName: e.target.value })} required />
          <Input label="Date of birth" type="date" value={chk.dob} onChange={(e) => setChk({ ...chk, dob: e.target.value })} required />
          <Input label="Address (optional)" value={chk.address} onChange={(e) => setChk({ ...chk, address: e.target.value })} />
          <Btn type="submit">Check</Btn>
        </form>
        {matches && (matches.length === 0 ? <Notice kind="ok">No potential duplicates found.</Notice> : (
          <Table head={['Voter ID', 'Name', 'DOB', 'Potential duplicate']}>
            {matches.map((m) => <tr key={m.voterCode}><Td className="font-mono">{m.voterCode}</Td><Td>{m.fullName}</Td><Td>{m.dob}</Td><Td><b>{m.score}%</b></Td></tr>)}
          </Table>
        ))}
      </Card>
    </Page>
  );
}
