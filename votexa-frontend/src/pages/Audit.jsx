import { useState } from 'react';
import api, { errMsg } from '../services/api.js';
import useLoad from '../hooks/useLoad.js';
import { fmtDate, short } from '../utils/format.js';
import { Badge, Btn, Card, Notice, Page, Table, Td } from '../components/ui.jsx';

export default function Audit() {
  const events = useLoad(async () => (await api.get('/api/blockchain/events')).data, [], 10000);
  const status = useLoad(async () => (await api.get('/api/blockchain/status')).data, [], 10000);
  const trail = useLoad(async () => (await api.get('/api/audit/events')).data, [], 10000);
  const [report, setReport] = useState(null);
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);

  const verify = async () => {
    setBusy(true); setErr('');
    try { setReport((await api.post('/api/blockchain/verify')).data); events.reload(); }
    catch (e) { setErr(errMsg(e)); } finally { setBusy(false); }
  };

  return (
    <Page title="Blockchain Audit" subtitle="Only hashes and metadata are anchored. Personal data and vote choice are never on-chain.">
      <Notice>{err || events.error}</Notice>
      <Card title="Ledger status" actions={<Btn onClick={verify} disabled={busy}>{busy ? 'Verifying…' : 'Verify integrity'}</Btn>}>
        <div className="flex flex-wrap gap-6 text-sm">
          <div>Mode: <b>{status.data?.mode}</b></div>
          <div>Committed: <b>{status.data?.committed}</b></div>
          <div>Pending: <b>{status.data?.pending}</b></div>
          <div>Failed: <b>{status.data?.failed}</b></div>
        </div>
        {status.data?.mode === 'local' && <p className="mt-2 text-xs text-amber-700">Local development ledger (hash-chained file). Start the Fabric network and run the backend with the fabric profile for the real permissioned blockchain.</p>}
        {report && (
          <div className="mt-3">
            <div className={`rounded px-3 py-2 text-sm font-semibold ${report.verified ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>
              {report.verified ? 'INTEGRITY VERIFIED' : 'INTEGRITY VIOLATION'}: {report.eventsChecked} events and {report.ledgerRecordsChecked} ledger records checked ({report.ledgerMode}).
            </div>
            {report.violations.length > 0 && <ul className="mt-2 list-disc pl-5 text-sm text-red-700">{report.violations.map((v, i) => <li key={i}>{v}</li>)}</ul>}
          </div>
        )}
      </Card>
      <Card title="Anchored events">
        <Table head={['#', 'Event', 'Record hash', 'Previous hash', 'Timestamp', 'Authority', 'Transaction ID', 'Anchor', 'Integrity']}>
          {(events.data || []).map((e) => (
            <tr key={e.id}>
              <Td>{e.id}</Td><Td>{e.eventType}<div className="text-xs text-slate-500">{e.action}</div></Td>
              <Td className="font-mono text-xs" title={e.recordHash}>{short(e.recordHash)}</Td>
              <Td className="font-mono text-xs" title={e.previousHash}>{short(e.previousHash)}</Td>
              <Td>{fmtDate(e.timestamp)}</Td><Td>{e.authority}</Td>
              <Td className="font-mono text-xs">{e.txId || '-'}</Td>
              <Td><Badge text={e.status} /></Td><Td><Badge text={e.integrity} /></Td>
            </tr>
          ))}
        </Table>
      </Card>
      <Card title="Audit trail (who / what / when / why / result)">
        <Table head={['When', 'Who', 'What', 'Entity', 'Change', 'Why', 'Result']}>
          {(trail.data || []).map((a) => (
            <tr key={a.id}>
              <Td>{fmtDate(a.createdAt)}</Td><Td>{a.actor}</Td><Td>{a.action}</Td><Td className="text-xs">{a.entityType}<div className="font-mono">{a.entityId}</div></Td>
              <Td className="text-xs">{a.oldValue || ''}{a.oldValue || a.newValue ? ' → ' : ''}{a.newValue || ''}</Td><Td>{a.reason}</Td><Td><Badge text={a.result === 'SUCCESS' ? 'VERIFIED' : 'REJECTED'} /></Td>
            </tr>
          ))}
        </Table>
      </Card>
    </Page>
  );
}
