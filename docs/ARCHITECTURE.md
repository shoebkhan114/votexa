# VOTEXA architecture

```text
React (Vite, Tailwind, Chart.js)
        │ REST + JWT
Spring Boot (Java 21)  ──►  PostgreSQL (operational data, Flyway migrations)
   │   │   └─ Java similarity engine (duplicate / anomaly decision support)
   │   └───── audit_events (append-only, hash-chained)
   │                 │  LedgerSubmitter (async, ordered, retried)
   │                 ▼
   │           LedgerClient ── LocalLedgerClient (default, hash-chained file)
   │                       └─ FabricLedgerClient (Fabric Gateway SDK, `-Pfabric`) ─► Hyperledger Fabric
   └─ Election package: snapshot JSON → SHA-256 → ECDSA signature → AES-256-GCM (PBKDF2 key)
                                     │ file / USB
                              JavaFX offline validator (SQLite, field-level AES-GCM)
                                     │ sync file (JSON)  → POST /api/polling/sync
```

## Backend packages (`com.votexa`)
`auth` login, JWT, rate limit · `voter` registry, status lifecycle · `address` migration workflow ·
`verification` death/inactive workflow · `duplicate` similarity + review · `election` elections, roll freeze, offline package ·
`polling` eligibility, authorization, reconciliation, sync · `audit` hash-chained events · `blockchain` ledger clients + integrity verification ·
`anomaly` alerts · `complaint` · `dashboard` · `admin` · `mock` simulated feeds · `config` / `common`.

## Key design decisions
1. **Audit chain.** `record_hash = SHA-256(uuid | type | entity | actor | action | old | new | reason | result | timestamp | previous_hash)`.
   A DB trigger blocks UPDATE/DELETE on `audit_events`; a DB owner can still drop the trigger, which is why every hash is also anchored on the ledger.
   `POST /api/blockchain/verify` recomputes the chain and compares it with the ledger copy and the authority signatures.
2. **Off-chain vs on-chain.** On-chain: event id, type, record hash, previous hash, timestamp, authority id, signature, tx id.
   Everything personal stays in PostgreSQL. Polling events reference voters only by a short hash.
3. **One-time authorization.** `voting_authorizations` holds one row per (election, voter) with `UNIQUE(election_id, voter_id)`.
   Authorization is `UPDATE … WHERE voting_status='NOT_VOTED'`, so concurrent attempts cannot both succeed. No candidate data exists anywhere.
4. **Human in the loop.** Address moves, duplicates and deceased records are requests/cases an authorized officer verifies and approves.
   Alerts say "review required"; they never accuse a person or invalidate an election.
5. **Roll freeze.** Freezing copies ACTIVE voters into `voter_assignments`, creates polling sessions and `NOT_VOTED` rows and stores a snapshot hash.
   Later registry edits do not change a frozen election.
6. **Offline package.** Verified in this order: decrypt → hash check → ECDSA signature (authority public key delivered out-of-band) →
   header/content match → expiry → replay check (package-id registry on the device).
7. **Ledger abstraction.** The same code runs with the local ledger (development) or Fabric (`LEDGER_MODE=fabric`, build with `-Pfabric`).

## Roles (least privilege)
Citizen: own record, requests, complaints · BLO: verify, recommend · ERO: approve/reject, duplicates, status, roll freeze, packages ·
Polling Officer: validate/authorize, open/close sessions, sync · Auditor: audit, ledger verification, reports · Super Admin: users, elections, configuration.

## Database
Flyway migrations in `votexa-backend/src/main/resources/db/migration` (`V1__schema.sql`, `V2__seed_demo_data.sql`).
20 tables: the 18 in the design plus `polling_attempts` and `anomaly_alerts`.
