# Professor demo script (about 15 minutes)

Preparation: database seeded, backend + frontend running, Admin → Elections → **Reset window (+8h)** on `ELC-2026-MOCK`
(the seeded polling window is relative to when the seed ran). Use two browsers (or one normal + one private window) to switch roles quickly.
All data is dummy. Password for every demo user: `Votexa@123`.

| # | Demo | Steps | What to point out |
|---|---|---|---|
| 1 | Login | Sign in as `admin` | JWT + role-based menu (least privilege) |
| 2 | Voter registry | Voter Search; filter by status; open `VTX0000006` | 8 lifecycle statuses, status history, simulated data |
| 3 | Address change | `blo101`: Address Changes → **Verify** `ACR-0001` (Neha Singh). `ero102`: **Approve** | PENDING → VERIFIED → APPROVED; old area SHIFTED → new area ACTIVE; new station `PS-101`; nothing moves without an officer |
| 3b | Mock Aadhaar event | `ero102`: Mock event form, voter `VTX0000002` → a PENDING request appears | VOTEXA reacts to an event with a *request*, not an automatic transfer |
| 4 | Duplicate detection | `ero102`: Duplicate Cases → case `DUP-0001` (Rahul Kumar ×2, 97.5%) → **Confirm duplicate** | Java similarity score; officer decides; second record becomes INACTIVE |
| 4b | New registration check | Voter Search → New voter: "Rahul Kumar", DOB 1995-03-14 | duplicate warning at registration time |
| 5 | Blockchain event | `auditor1`: Blockchain Audit | each action above produced an anchored hash (COMMITTED); only hashes and metadata |
| 6 | Polling validation | `po102`: Polling Dashboard, session `PS-102` → `VTX0000001` → **Validate** → **Authorize** | 8 eligibility checks; counters update; no vote choice anywhere |
| 7 | Second attempt | Authorize `VTX0000001` again | **ALREADY VOTED**; an anomaly alert appears ("review required", no accusation) |
| 8 | Offline mode | Admin → Elections & packages: generate package for `PS-102` (passphrase ≥ 8 chars) and download the authority public key. Start the offline app, **disconnect Internet**, load package, validate/authorize `VTX0000002`, try `VTX0000001` (already VOTED in the snapshot), **Export sync file** | signature check, AES-GCM decryption, replay protection; works fully offline |
| 8b | Sync back | `po102`: Polling Dashboard → upload the sync file | offline authorizations merge into the central record; conflicts are flagged |
| 9 | Reconciliation (match) | `po102`: Close session `PS-102` with recorded = authorized count | result MATCH |
| 9b | Reconciliation (mismatch) | Open session `PS-101`, authorize `VTX0000003`, close with recorded = 2 | RECONCILIATION REQUIRED + CRITICAL alert; the election is *not* declared invalid automatically |
| 10 | Audit history | `auditor1`: Blockchain Audit → **Verify integrity** → INTEGRITY VERIFIED; Reports page | who / what / when / why / result |
| 10b | Tamper detection | In a terminal: `psql -h localhost -U shoeb114 -d votexadb -f votexa-backend/db-scripts/tamper-demo.sql`, then **Verify integrity** | INTEGRITY VIOLATION with the exact event; restore with `./setup-db.sh --reset` |

Tip: after 10b run `./setup-db.sh --reset` (it also clears the local ledger file) before the next rehearsal.
