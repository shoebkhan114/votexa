import { useState } from 'react';
import api, { errMsg } from '../services/api.js';
import useLoad from '../hooks/useLoad.js';
import { useAuth } from '../context/AuthContext.jsx';
import { fmtDate } from '../utils/format.js';
import { Badge, Btn, Card, Notice, Page, Table, Td } from '../components/ui.jsx';

export default function Complaints() {
  const { user } = useAuth();
  const isEro = ['ERO', 'SUPER_ADMIN'].includes(user.role);
  const list = useLoad(async () => (await api.get('/api/complaints')).data, []);
  const [msg, setMsg] = useState({ kind: 'ok', text: '' });

  const decide = async (id, status) => {
    const decision = status === 'UNDER_REVIEW' ? '' : window.prompt('Decision note (required):');
    if (status !== 'UNDER_REVIEW' && !decision) return;
    try { await api.post(`/api/complaints/${id}/decide`, { status, decision }); setMsg({ kind: 'ok', text: 'Complaint updated and audit event recorded.' }); list.reload(); }
    catch (e) { setMsg({ kind: 'error', text: errMsg(e) }); }
  };

  return (
    <Page title="Complaints & Disputes" subtitle="Complaint → case ID → officer review → decision → audit event">
      <Notice kind={msg.kind}>{msg.text}</Notice>
      <Notice>{list.error}</Notice>
      <Card>
        <Table head={['Case', 'Voter', 'Category', 'Description', 'Status', 'Decision', 'Filed', 'Actions']}>
          {(list.data || []).map((c) => (
            <tr key={c.id}>
              <Td className="font-mono">{c.caseCode}</Td><Td className="font-mono text-xs">{c.voterCode || '-'}</Td>
              <Td className="text-xs">{c.category.replaceAll('_', ' ')}</Td><Td>{c.description}</Td>
              <Td><Badge text={c.status} /></Td><Td>{c.decision}</Td><Td>{fmtDate(c.createdAt)}</Td>
              <Td>{isEro && !['RESOLVED', 'REJECTED'].includes(c.status) && (
                <div className="flex flex-wrap gap-1">
                  {c.status === 'OPEN' && <Btn variant="ghost" onClick={() => decide(c.id, 'UNDER_REVIEW')}>Start review</Btn>}
                  <Btn variant="success" onClick={() => decide(c.id, 'RESOLVED')}>Resolve</Btn>
                  <Btn variant="danger" onClick={() => decide(c.id, 'REJECTED')}>Reject</Btn>
                </div>)}</Td>
            </tr>
          ))}
        </Table>
      </Card>
    </Page>
  );
}
