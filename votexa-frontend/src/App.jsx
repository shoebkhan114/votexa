import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './context/AuthContext.jsx';
import Layout from './layouts/Layout.jsx';
import Login from './pages/Login.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Citizen from './pages/Citizen.jsx';
import Voters from './pages/Voters.jsx';
import Verify from './pages/Verify.jsx';
import AddressChanges from './pages/AddressChanges.jsx';
import Duplicates from './pages/Duplicates.jsx';
import DeathCases from './pages/DeathCases.jsx';
import Polling from './pages/Polling.jsx';
import Audit from './pages/Audit.jsx';
import Reports from './pages/Reports.jsx';
import Complaints from './pages/Complaints.jsx';
import Admin from './pages/Admin.jsx';

function Protected({ children }) {
  const { user } = useAuth();
  return user ? children : <Navigate to="/login" replace />;
}

function Home() {
  const { user } = useAuth();
  return <Navigate to={user.role === 'CITIZEN' ? '/my' : '/dashboard'} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<Protected><Layout /></Protected>}>
        <Route path="/" element={<Home />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/my" element={<Citizen />} />
        <Route path="/voters" element={<Voters />} />
        <Route path="/verify" element={<Verify />} />
        <Route path="/address-changes" element={<AddressChanges />} />
        <Route path="/duplicates" element={<Duplicates />} />
        <Route path="/death-cases" element={<DeathCases />} />
        <Route path="/polling" element={<Polling />} />
        <Route path="/audit" element={<Audit />} />
        <Route path="/reports" element={<Reports />} />
        <Route path="/complaints" element={<Complaints />} />
        <Route path="/admin" element={<Admin />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
