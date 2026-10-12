import { useState } from 'react';
import api, { errMsg } from '../services/api.js';
import useLoad from '../hooks/useLoad.js';
import { useAuth } from '../context/AuthContext.jsx';
import { fmtDate } from '../utils/format.js';
import { Badge, Btn, Card, Input, Notice, Page, Select, Table, Td } from '../components/ui.jsx';

export default function AddressChanges() {
  const { user } = useAuth();
  const isEro = ['ERO', 'SUPER_ADMIN'].includes(user.role);
  const [status, setStatus] = useState('');
  const list = useLoad(async () => (await api.get('/api/address-changes', { params: { status } })).data, [status]);
  const [msg, setMsg] = useState({ kind: 'ok', text: '' });
  const [station, setStation] = useState({});
  const [ev, setEv] = useState({ voterCode: 'VTX0000010', line1: 'House 7, Mock Street', locality: 'Demo Locality 1', pincode: '302001', stationCode: 'PS-101' });

  const act = async (id, action, body = {}) => {
    try { await api.post(`/api/address-changes/${id}/${action}`, body); setMsg({ kind: 'ok', text: `Request ${action} done. Audit event recorded.` }); list.reload(); }
    catch (e) { setMsg({ kind: 'error', text: errMsg(e) }); }
  };
  const reject = (id) => {
    const remarks = window.prompt('Reason for rejection (required):');
    if (remarks) act(id, 'reject', { remarks });
  };
  const sendEvent = async (e) => {
    e.preventDefault();
    try { await api.post('/api/mock/aadhaar-address-event', ev); setMsg({ kind: 'ok', text: 'Mock Aadhaar address-change event received; a PENDING request was created (no automatic transfer).' }); list.reload(); }
    catch (x) { setMsg({ kind: 'error', text: errMsg(x) }); }
  };

  return (
    <Page title="Address / Constituency Migration" subtitle="Request → verification → ERO approval → old area SHIFTED → new area ACTIVE with new polling station → audit">
      <Notice kind={msg.kind}>{msg.text}</Notice>
      <Notice>{list.error}</Notice>
      <Card title="Requests" actions={
        <Select value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">All</option>{['PENDING', 'VERIFIED', 'APPROVED', 'REJECTED'].map((s) => <option key={s}>{s}</option>)}
        </Select>}>
        <Table head={['Request', 'Voter', 'New address', 'Target station', 'Source', 'Status', 'Created', 'Actions']}>
          {(list.data || []).map((r) => (
            <tr key={r.id}>
              <Td className="font-mono">{r.requestCode}</Td>
              <Td>{r.voterName}<div className="font-mono text-xs text-slate-500">{r.voterCode}</div></Td>
              <Td>{r.newLine1}, {r.newLocality} {r.newPincode}</Td>
              <Td>{r.newPollingStationCode || '-'}</Td>
              <Td className="text-xs">{r.source.replaceAll('_', ' ')}</Td>
              <Td><Badge text={r.status} /></Td>
              <Td>{fmtDate(r.createdAt)}</Td>
              <Td>
                <div className="flex flex-wrap items-center gap-1">
                  {r.status === 'PENDING' && <Btn variant="ghost" onClick={() => act(r.id, 'verify', { remarks: 'Verified by field check' })}>Verify</Btn>}
                  {isEro && r.status === 'VERIFIED' && (
                    <>
                      {!r.newPollingStationCode && (
                        <input className="w-20 rounded border px-1 py-1 text-xs" placeholder="PS-101" value={station[r.id] || ''}
                          onChange={(e) => setStation({ ...station, [r.id]: e.target.value })} />
                      )}
                      <Btn variant="success" onClick={() => act(r.id, 'approve', { stationCode: station[r.id] || null, remarks: 'Approved by ERO' })}>Approve</Btn>
                    </>
                  )}
                  {isEro && ['PENDING', 'VERIFIED'].includes(r.status) && <Btn variant="danger" onClick={() => reject(r.id)}>Reject</Btn>}
                </div>
              </Td>
            </tr>
          ))}
        </Table>
      </Card>
      {isEro && (
        <Card title="Mock Aadhaar address-change event (simulation)">
          <form onSubmit={sendEvent} className="grid items-end gap-2 md:grid-cols-6">
            {[['voterCode', 'Voter ID'], ['line1', 'New address'], ['locality', 'Locality'], ['pincode', 'Pincode'], ['stationCode', 'Target station']].map(([k, l]) => (
              <Input key={k} label={l} value={ev[k]} onChange={(e) => setEv({ ...ev, [k]: e.target.value })} required={k !== 'stationCode'} />
            ))}
            <Btn type="submit">Send event</Btn>
          </form>
          <p className="mt-2 text-xs text-slate-500">This is a simulated feed, not a real government integration. VOTEXA only creates a request; an officer must verify and approve.</p>
        </Card>
      )}
    </Page>
  );
}
