# REST API summary
Interactive docs: `/swagger-ui.html`. All endpoints except `POST /api/auth/login` need `Authorization: Bearer <JWT>`.

| Area | Endpoint | Roles |
|---|---|---|
| Auth | `POST /api/auth/login`, `GET /api/auth/me` | public / any |
| Voters | `GET /api/voters?q=&status=`, `GET /api/voters/{code}`, `GET /api/voters/{code}/history` | staff |
| | `GET /api/voters/me` | citizen |
| | `POST /api/voters`, `POST /api/voters/{code}/status` | ERO, admin |
| | `POST /api/voters/verify` | BLO, ERO, polling officer, admin |
| | `POST /api/voters/duplicate-check` | BLO, ERO, admin |
| Address | `POST /api/address-changes`, `GET /api/address-changes/mine` | citizen (staff may create) |
| | `GET /api/address-changes`, `POST …/{id}/verify` | BLO, ERO, admin |
| | `POST …/{id}/approve`, `POST …/{id}/reject` | ERO, admin |
| Duplicates | `GET /api/duplicates`, `POST /api/duplicates/scan`, `POST /api/duplicates/{id}/review` | BLO read; ERO/admin act |
| Deceased | `GET /api/death-cases`, `POST …/{id}/verify`, `POST …/{id}/review` | BLO verify; ERO review |
| Mock feeds | `POST /api/mock/aadhaar-address-event`, `POST /api/mock/death-event` | ERO, admin |
| Stations | `GET /api/polling-stations`, `GET /api/polling-stations/{code}` | staff |
| Elections | `GET/POST /api/elections`, `POST …/{id}/freeze`, `open-polling`, `close-polling`, `reset-window`, `package` | staff read; ERO/admin act |
| Polling | `POST /api/polling/validate`, `/authorize`, `/sync` | polling officer, admin |
| | `GET /api/polling/sessions`, `…/{id}/dashboard`, `…/{id}/attempts`, `…/{id}/reconciliation`, `GET /api/polling/reconciliation` | polling officer, ERO, auditor, admin |
| | `POST /api/polling/sessions/{id}/open`, `…/close` | polling officer, admin |
| Audit | `GET /api/audit/events`, `GET /api/blockchain/events`, `POST /api/blockchain/verify`, `GET /api/blockchain/status`, `GET /api/blockchain/authority-public-key` | auditor, ERO, admin |
| Alerts | `GET /api/anomalies`, `POST /api/anomalies/{id}/acknowledge` or `resolve` | read: polling roles; act: auditor, ERO, admin |
| Complaints | `POST /api/complaints`, `GET /api/complaints/mine`, `GET /api/complaints`, `POST …/{id}/decide` | citizen / BLO, ERO / ERO |
| Dashboard | `GET /api/dashboard/summary` | staff |
| Admin | `GET/POST /api/admin/users`, `POST /api/admin/users/{id}/toggle` | admin |
