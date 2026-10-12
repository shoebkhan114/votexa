import { useState } from 'react';
import api, { errMsg } from '../services/api.js';
import { Badge, Btn, Card, Input, Notice, Page } from '../components/ui.jsx';

export default function Verify() {
  const [code, setCode] = useState('');
  const [res, setRes] = useState(null);
  const [err, setErr] = useState('');
  const submit = async (e) => {
    e.preventDefault(); setErr(''); setRes(null);
    try { setRes((await api.post('/api/voters/verify', { voterCode: code.trim() })).data); } catch (x) { setErr(errMsg(x)); }
  };
  return (
    <Page title="Voter Verification" subtitle="Enter Voter ID → search record → eligibility checks → verification result">
      <Card>
        <form onSubmit={submit} className="flex items-end gap-2">
          <Input label="Voter ID" value={code} onChange={(e) => setCode(e.target.value)} placeholder="VTX0000001" className="w-64" required />
          <Btn type="submit">Verify</Btn>
        </form>
      </Card>
      <Notice>{err}</Notice>
      {res && (
        <Card title={`Result · ${res.voterCode}`}>
          <div className={`mb-3 rounded px-3 py-2 text-sm font-semibold ${res.eligible ? 'bg-green-50 text-green-800' : 'bg-red-50 text-red-800'}`}>
            {res.eligible ? 'ELIGIBLE' : 'NOT ELIGIBLE'}: {res.message}
          </div>
          <ul className="space-y-1 text-sm">
            {res.checks.map((c) => <li key={c.name}>{c.passed ? '✓' : '✗'} {c.name} <Badge text={c.passed ? 'VERIFIED' : 'REJECTED'} /></li>)}
          </ul>
        </Card>
      )}
    </Page>
  );
}
