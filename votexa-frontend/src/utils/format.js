export const fmtDate = (d) => (d ? new Date(d).toLocaleString() : '-');
export const short = (h, n = 12) => (h ? `${h.slice(0, n)}…` : '-');
export const staffRoles = ['BLO', 'ERO', 'POLLING_OFFICER', 'AUDITOR', 'SUPER_ADMIN'];
