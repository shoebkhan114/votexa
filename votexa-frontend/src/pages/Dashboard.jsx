import { Bar, Doughnut, Line } from 'react-chartjs-2';
import api from '../services/api.js';
import useLoad from '../hooks/useLoad.js';
import { PALETTE } from '../utils/charts.js';
import { Card, Notice, Page, Stat } from '../components/ui.jsx';

const dataset = (obj, label) => ({
  labels: Object.keys(obj || {}),
  datasets: [{ label, data: Object.values(obj || {}), backgroundColor: PALETTE, borderColor: PALETTE[0] }],
});

export default function Dashboard() {
  const { data: s, loading, error } = useLoad(async () => (await api.get('/api/dashboard/summary')).data, [], 15000);
  if (loading && !s) return <p className="text-sm text-slate-500">Loading…</p>;
  if (!s) return <Notice>{error}</Notice>;
  const by = s.votersByStatus || {};
  const opts = { plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } };
  return (
    <Page title="Admin Dashboard" subtitle="Electoral roll health, workflows and polling activity">
      <Notice>{error}</Notice>
      <div className="grid grid-cols-2 gap-3 md:grid-cols-4 xl:grid-cols-7">
        <Stat label="Total voters" value={s.totalVoters} />
        <Stat label="Active" value={by.ACTIVE ?? 0} tone="text-green-700" />
        <Stat label="Pending verification" value={s.pendingVerification} tone="text-amber-600" />
        <Stat label="Duplicate review" value={by.DUPLICATE_REVIEW ?? 0} tone="text-amber-600" />
        <Stat label="Deceased / ineligible" value={(by.DECEASED ?? 0) + (by.INACTIVE ?? 0)} />
        <Stat label="Shifted" value={by.SHIFTED ?? 0} tone="text-purple-700" />
        <Stat label="Blocked" value={by.BLOCKED ?? 0} tone="text-red-700" />
      </div>
      <div className="grid gap-4 lg:grid-cols-3">
        <Card title="Voters by status"><div className="mx-auto max-w-xs"><Doughnut data={dataset(by, 'Voters')} /></div></Card>
        <Card title="Registration trend"><Line data={dataset(s.registrationsByMonth, 'Registrations')} options={opts} /></Card>
        <Card title="Address change requests"><Bar data={dataset(s.addressChangesByStatus, 'Requests')} options={opts} /></Card>
        <Card title="Duplicate cases"><Bar data={dataset(s.duplicateCasesByStatus, 'Cases')} options={opts} /></Card>
        <Card title="Polling activity (authorized by station)"><Bar data={dataset(s.pollingActivityByStation, 'Authorized')} options={opts} /></Card>
        <Card title="Open work items">
          <ul className="space-y-1 text-sm">
            <li>Open anomaly alerts: <b className={s.openAnomalies ? 'text-red-700' : ''}>{s.openAnomalies}</b></li>
            <li>Open complaints: <b>{s.complaintsByStatus?.OPEN ?? 0}</b></li>
            <li>Open death-verification cases: <b>{s.deathCasesByStatus?.OPEN ?? 0}</b></li>
            <li>Audit events: <b>{s.auditEvents}</b></li>
            <li>Ledger anchors: {Object.entries(s.ledgerAnchorsByStatus || {}).map(([k, v]) => `${k} ${v}`).join(' · ') || '-'}</li>
          </ul>
        </Card>
      </div>
    </Page>
  );
}
