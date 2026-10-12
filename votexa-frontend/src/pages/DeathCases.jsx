import { useState } from 'react';
import api, { errMsg } from '../services/api.js';
import useLoad from '../hooks/useLoad.js';
import { useAuth } from '../context/AuthContext.jsx';
import { fmtDate } from '../utils/format.js';
import { Badge, Btn, Card, Input, Notice, Page, Table, Td } from '../components/ui.jsx';

export default function DeathCases() {
  const { user } = useAuth();
  const isEro = ['ERO', 'SUPER_ADMIN'].includes(user.role);
  const list = useLoad(async () => (await api.get('/api/death-cases')).data, []);
  const [msg, setMsg] = useState({ kind: 'ok', text: '' });
  const [code, setCode] = useState('VTX0000002');

  const call = async (fn, ok) => {
    try { await fn(); setMsg({ kind: 'ok', text: ok }); list.reload(); } catch (e) { setMsg({ kind: 'error', text: errMsg(e) }); }
  };

  return (
    <Page title="Deceased / Inactive Voter Management" subtitle="Possible match → field verification → officer review → status change → audit. No automatic deletion.">
      <Notice kind={msg.kind}>{msg.text}</Notice>
      <Notice>{list.error}</Notice>
      <Card title="Cases">
        <Table head={['Case', 'Voter', 'Source', 'Field verified', 'Status', 'Created', 'Actions']}>
          {(list.data || []).map((c) => (
            <tr key={c.id}>
              <Td className="font-mono">{c.caseCode}</Td>
              <Td>{c.voterName}<div className="font-mono text-xs text-slate-500">{c.voterCode}</div></Td>
              <Td className="text-xs">{c.source.replaceAll('_', ' ')}</Td>
              <Td>{c.verified ? 'Yes' : 'No'}</Td>
              <Td><Badge text={c.status} /></Td>
              <Td>{fmtDate(c.createdAt)}</Td>
              <Td>
                {c.status === 'OPEN' && (
                  <div className="flex flex-wrap gap-1">
                    {!c.verified && <Btn variant="ghost" onClick={() => call(() => api.post(`/api/death-cases/${c.id}/verify`, { remarks: 'Field verification done' }), 'Field verification recorded.')}>Record field verification</Btn>}
                    {isEro && c.verified && (
                      <>
                        <Btn variant="danger" onClick={() => call(() => api.post(`/api/death-cases/${c.id}/review`, { decision: 'CONFIRM', remarks: 'Confirmed' }), 'Voter marked DECEASED.')}>Confirm death</Btn>
                        <Btn variant="ghost" onClick={() => call(() => api.post(`/api/death-cases/${c.id}/review`, { decision: 'REJECT', remarks: 'Not a match' }), 'Match rejected, status restored.')}>Reject match</Btn>
                      </>
                    )}
                  </div>
                )}
              </Td>
            </tr>
          ))}
        </Table>
      </Card>
      {isEro && (
        <Card title="Mock death-registry event (simulation)">
          <div className="flex items-end gap-2">
            <Input label="Voter ID" value={code} onChange={(e) => setCode(e.target.value)} className="w-56" />
            <Btn onClick={() => call(() => api.post('/api/mock/death-event', { voterCode: code }), 'Possible match created; voter set UNDER_VERIFICATION.')}>Send event</Btn>
          </div>
          <p className="mt-2 text-xs text-slate-500">Simulated feed, not a real registry. The voter is only flagged UNDER_VERIFICATION until an officer decides.</p>
        </Card>
      )}
    </Page>
  );
}
