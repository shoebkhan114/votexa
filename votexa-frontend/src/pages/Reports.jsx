import { Bar } from 'react-chartjs-2';
import api, { errMsg } from '../services/api.js';
import useLoad from '../hooks/useLoad.js';
import { useAuth } from '../context/AuthContext.jsx';
import { PALETTE } from '../utils/charts.js';
import { fmtDate } from '../utils/format.js';
import { Badge, Btn, Card, Notice, Page, Table, Td } from '../components/ui.jsx';
import { useState } from 'react';

export default function Reports() {
  const { user } = useAuth();
  const canResolve = ['AUDITOR', 'ERO', 'SUPER_ADMIN'].includes(user.role);
  const rec = useLoad(async () => (await api.get('/api/polling/reconciliation')).data, [], 10000);
  const alerts = useLoad(async () => (await api.get('/api/anomalies')).data, [], 10000);
  const [msg, setMsg] = useState('');

  const act = async (id, action) => {
    try { await api.post(`/api/anomalies/${id}/${action}`); alerts.reload(); } catch (e) { setMsg(errMsg(e)); }
  };

  const rows = rec.data || [];
  const chart = {
    labels: rows.map((r) => r.stationCode),
    datasets: [
      { label: 'Eligible', data: rows.map((r) => r.eligible), backgroundColor: PALETTE[0] },
      { label: 'Authorized', data: rows.map((r) => r.authorized), backgroundColor: PALETTE[1] },
      { label: 'Recorded', data: rows.map((r) => r.recorded ?? 0), backgroundColor: PALETTE[2] },
    ],
  };

  return (
    <Page title="Election Reconciliation & Anomalies" subtitle="Recorded ≤ Authorized ≤ Eligible. Mismatches require review; they never invalidate an election automatically.">
      <Notice>{msg || rec.error || alerts.error}</Notice>
      <Card title="Reconciliation by polling station">
        <Table head={['Station', 'Eligible', 'Authorized', 'Recorded', 'Result', 'Note']}>
          {rows.map((r) => (
            <tr key={r.sessionId}><Td className="font-mono">{r.stationCode}</Td><Td>{r.eligible}</Td><Td>{r.authorized}</Td><Td>{r.recorded ?? '-'}</Td>
              <Td><Badge text={r.status} /></Td><Td className="text-xs">{r.note}</Td></tr>
          ))}
        </Table>
      </Card>
      <Card title="Eligible vs authorized vs recorded"><Bar data={chart} options={{ scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } }} /></Card>
      <Card title="Anomaly alerts (review required, not accusations)">
        <Table head={['When', 'Type', 'Severity', 'Message', 'Status', '']} empty="No alerts">
          {(alerts.data || []).map((a) => (
            <tr key={a.id}>
              <Td>{fmtDate(a.createdAt)}</Td><Td className="text-xs">{a.alertType.replaceAll('_', ' ')}</Td><Td><Badge text={a.severity} /></Td>
              <Td>{a.message}</Td><Td><Badge text={a.status} /></Td>
              <Td>{canResolve && a.status !== 'RESOLVED' && (
                <div className="flex gap-1">
                  {a.status === 'OPEN' && <Btn variant="ghost" onClick={() => act(a.id, 'acknowledge')}>Acknowledge</Btn>}
                  <Btn variant="ghost" onClick={() => act(a.id, 'resolve')}>Resolve</Btn>
                </div>)}</Td>
            </tr>
          ))}
        </Table>
      </Card>
    </Page>
  );
}
