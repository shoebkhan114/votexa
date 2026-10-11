# VOTEXA

**Blockchain-Assisted Electoral Roll Integrity, Voter Validation and Polling Audit System**

Java Project Based Learning (PBL) | 5th Semester, CSE / AI&DS / IT | Academic Year 2026-27

> **PROTOTYPE / DEMO DATA ONLY.** VOTEXA is an academic prototype. It uses dummy voter data and mock government services. It is **not** a replacement for the Election Commission of India, EVMs or any official election infrastructure, and it does not claim to eliminate election fraud.

---

## Table of Contents

1. [Problem Statement](#1-problem-statement)
2. [Proposed Solution](#2-proposed-solution)
3. [Abstract](#3-abstract)
4. [Objectives](#4-objectives)
5. [Key Features](#5-key-features)
6. [System Architecture](#6-system-architecture)
7. [Technology Stack](#7-technology-stack)
8. [Modules and Roles](#8-modules-and-roles)
9. [Project Structure](#9-project-structure)
10. [Getting Started (Ubuntu)](#10-getting-started-ubuntu)
11. [Demo Flow](#11-demo-flow)
12. [Security Design](#12-security-design)
13. [Testing](#13-testing)
14. [Limitations](#14-limitations)
15. [Future Scope](#15-future-scope)
16. [Team](#16-team)
17. [Project Guide](#17-project-guide)
18. [Git Workflow for Contributors](#18-git-workflow-for-contributors)
19. [Documentation](#19-documentation)
20. [Disclaimer](#20-disclaimer)

---

## 1. Problem Statement

Electoral rolls are large, constantly changing records. Keeping them accurate and auditable raises several system-design challenges:

- Duplicate voter records and possible impersonation or multiple voting attempts.
- Voters who relocate, or who are deceased, may stay on the roll until verification and removal.
- Wrong polling-station assignment and roll inconsistencies.
- Manual verification is slow, and it is hard to trace **who changed what, when and why**.
- Polling counts can mismatch (eligible voters, authorized voters, recorded polling activity).
- There is no independent, tamper-evident audit layer for sensitive record changes.
- Some digital validation workflows depend on internet connectivity.

These are treated as **system-design problems**. This project does not claim that existing Indian election infrastructure has failed in each of these cases.

## 2. Proposed Solution

VOTEXA is built on one idea:

> **Right voter + right polling area + one-time voting authorization + verified electoral records + tamper-evident audit trail.**

- **Registry and workflows:** electoral registry, address-change workflow, deceased/ineligible workflow and complaint handling, each with officer review. Nothing is deleted or changed automatically.
- **Decision support:** duplicate detection (name, date of birth and address similarity) that only *suggests* matches; an officer confirms or rejects.
- **Polling validation:** eligibility checks, polling-station match and one-time authorization (`NOT_VOTED` to `VOTED`; a second attempt returns `ALREADY_VOTED`).
- **Tamper-evident audit:** every sensitive action is hashed (SHA-256) and linked to the previous event, forming a verifiable chain. A Hyperledger Fabric chaincode and an optional Fabric ledger client are included for anchoring audit hashes.
- **Offline validator:** a JavaFX application that validates voters from a signed, encrypted election snapshot without internet.
- **Reconciliation:** `Recorded <= Authorized <= Eligible`. Mismatches raise alerts for review, never automatic accusations.

**Privacy rules:** the actual vote choice is never stored. The audit layer stores only hashes and event metadata, never personal data.

## 3. Abstract

Maintaining an accurate and auditable electoral roll is a difficult data-integrity problem: records change through relocation, death and correction, duplicates appear, and it is hard to prove afterwards who changed what and when. VOTEXA is a prototype platform that addresses these issues through a role-based, workflow-driven system. A Spring Boot backend with a PostgreSQL database manages voters, address-change requests, complaints, duplicate cases, deceased-record verification, polling-station assignment and one-time voting authorization. A React web application provides citizen and officer dashboards. Every sensitive action is recorded in a SHA-256 hash-chained audit trail that can be verified for tampering, and a Hyperledger Fabric chaincode is provided for anchoring audit hashes on a permissioned ledger. An offline JavaFX polling validator checks voters against a digitally signed, encrypted election snapshot and works without internet. Duplicate and anomaly detection are implemented as decision-support tools, and a reconciliation report compares recorded, authorized and eligible counts. The system uses only dummy data and simulated government services, and it never stores voter-to-candidate mappings.

## 4. Objectives

1. Maintain an electoral registry with controlled status changes (`ACTIVE`, `PENDING`, `UNDER_VERIFICATION`, `SHIFTED`, `DECEASED`, `DUPLICATE_REVIEW`, `INACTIVE`, `BLOCKED`).
2. Provide officer-approved workflows for address change, deceased/ineligible records and complaints.
3. Detect potential duplicate records and assist officers in reviewing them.
4. Validate voter eligibility and polling-station match, and authorize each voter only once.
5. Record WHO, WHAT, WHEN, WHY and RESULT for sensitive actions in a tamper-evident audit chain.
6. Support offline polling validation using signed and encrypted election snapshots.
7. Detect reconciliation anomalies and generate alerts and reports.
8. Enforce least-privilege role-based access control.

## 5. Key Features

| Area | Features |
|---|---|
| Electoral registry | Voter CRUD, status management, roll versioning and freeze |
| Address migration | Mock Aadhaar event, request, BLO verification, ERO approval, new polling station |
| Deceased/ineligible | Mock death event, match, verification, authorized review (no auto-delete) |
| Duplicate detection | Similarity score (for example "Potential Duplicate: 87%"), officer confirm/reject |
| Polling validation | Eligibility checks, station match, one-time authorization |
| Audit trail | SHA-256 hash chain, previous-hash linking, `VERIFIED` / `INTEGRITY VIOLATION` |
| Blockchain | Fabric chaincode for audit records (optional ledger client, local ledger by default) |
| Offline validator | JavaFX + SQLite, signature-verified package, encrypted fields, sync export |
| Anomaly detection | Reconciliation errors, multiple-attempt alerts, review-required alerts |
| Dashboards | Admin statistics, Chart.js charts, reports |
| Security | JWT, RBAC, BCrypt, rate limiting, input validation, audit logging |

## 6. System Architecture

```text
                    CITIZEN / OFFICERS
                           |
                           v
                 +---------------------+
                 |  React Web Portal   |
                 +----------+----------+
                            |  REST + JWT
                            v
                 +---------------------+
                 |  Spring Boot API    |-----> Auth / RBAC / Rate limit
                 +----------+----------+
                            |
          +-----------------+------------------+
          v                                    v
     PostgreSQL                       Audit Event Layer
 (operational data)            SHA-256 hash + previous hash
                                         |
                                         v
                              Ledger Client (local ledger /
                              Hyperledger Fabric chaincode)

   POLLING STATION
          |
          v
 +--------------------+      signed + encrypted
 | JavaFX Offline     | <---- election snapshot
 | Validator (SQLite) |
 +---------+----------+
           |  encrypted local audit
           v
     Secure sync export
```

Operational data stays in PostgreSQL. The blockchain/audit layer stores only hashes and event metadata.

## 7. Technology Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot, Spring Security, Spring Data JPA, Maven |
| Frontend | React, Vite, Tailwind CSS, Chart.js |
| Database | PostgreSQL, Flyway migrations |
| Offline validator | Java, JavaFX, SQLite |
| Blockchain | Hyperledger Fabric (Java chaincode) |
| Cryptography | SHA-256, AES, ECDSA digital signatures |
| Auth | JWT, BCrypt, role-based access control |
| Testing | JUnit, Mockito, Postman |
| DevOps | Git, GitHub, Docker, Docker Compose |

## 8. Modules and Roles

**Roles (least privilege)**

| Role | Permissions |
|---|---|
| Citizen | View own data, submit requests and complaints, track requests |
| BLO | Verify records, recommend changes |
| ERO / Election Officer | Approve or reject changes, review duplicates, update statuses |
| Polling Officer | Validate voters, handle polling session |
| Auditor | View audit history, verify chain, generate reports |
| Super Admin | System configuration only |

**Backend packages** (`votexa-backend/src/main/java/com/votexa/`)

`auth`, `voter`, `address`, `complaint`, `verification`, `duplicate`, `anomaly`, `election`, `polling`, `audit`, `blockchain`, `dashboard`, `admin`, `mock`, `common`, `config`

## 9. Project Structure

```text
votexa/
├── docs/                    API, architecture, demo script, known limitations
├── votexa-backend/          Spring Boot API (Java 21)
├── votexa-frontend/         React + Vite + Tailwind web app
├── votexa-offline/          JavaFX + SQLite offline polling validator
├── votexa-blockchain/       Hyperledger Fabric chaincode and scripts
├── docker-compose.yml
├── .env.example
└── README.md
```

## 10. Getting Started (Ubuntu)

> Commands below target Ubuntu. If a script name or port differs in your copy, check `docs/` and each module's own files.

### 10.1 Prerequisites

```bash
sudo apt update
sudo apt install -y git curl unzip openjdk-21-jdk maven docker.io docker-compose-v2
sudo usermod -aG docker $USER      # log out and log in again after this
```

Install Node.js (LTS):

```bash
curl -fsSL https://deb.nodesource.com/setup_lts.x | sudo -E bash -
sudo apt install -y nodejs
```

Check versions: `java -version`, `mvn -v`, `node -v`, `docker --version`.

### 10.2 Clone and configure

```bash
git clone git@github.com:<github-username>/votexa.git
cd votexa
cp .env.example .env
```

### 10.3 Start the database

```bash
docker compose up -d
```

### 10.4 Run the backend

```bash
cd votexa-backend
mvn clean install
mvn spring-boot:run          # or: ./run-backend.sh
```

Flyway creates the schema and seeds the dummy demo data on first start.

### 10.5 Run the frontend

```bash
cd votexa-frontend
cp .env.example .env
npm install
npm run dev
```

Open the URL printed in the terminal (Vite default: `http://localhost:5173`).

### 10.6 Run the offline validator (optional)

```bash
cd votexa-offline
mvn javafx:run
```

### 10.7 Blockchain (optional)

See [`votexa-blockchain/README.md`](votexa-blockchain/README.md). The backend uses a local hash-chain ledger by default; the Fabric profile is optional (`mvn -Pfabric`).

### 10.8 Demo credentials

Demo users are created by the seed data (`V2__seed_demo_data.sql`). List them here after confirming:

| Role | Username | Password |
|---|---|---|
| Super Admin | `admin` | `Votexa@123` |
| ERO (Election Officer) | `ero102` | `Votexa@123` |
| BLO | `blo101` | `Votexa@123` |
| Polling Officer | `po102` | `Votexa@123` |
| Auditor | `auditor1` | `Votexa@123` |
| Citizen | `citizen1`, `citizen2` | `Votexa@123` |

These are demo-only credentials. Never use real data or reuse real passwords.
These are demo-only credentials. Never use real data or reuse real passwords.

## 11. Demo Flow

1. Login as Citizen and submit an address-change request.
2. Login as BLO to verify it, then as ERO to approve it.
3. Open the Audit page and see the new hash-chained event (`VERIFIED`).
4. Run `votexa-backend/db-scripts/tamper-demo.sql` to modify a record, then re-verify the chain and see `INTEGRITY VIOLATION`.
5. Run duplicate detection and review a potential duplicate as ERO.
6. Freeze the roll, generate the election package, load it in the offline validator, verify a voter and authorize once.
7. Try again with the same voter and see `ALREADY_VOTED`.
8. Open the reconciliation report and anomaly alerts.

Full script: [`docs/DEMO_SCRIPT.md`](docs/DEMO_SCRIPT.md).

## 12. Security Design

- JWT authentication, BCrypt password hashing, role-based access control.
- Login rate limiting, input validation, JPA (no raw SQL concatenation).
- SHA-256 hash chain for audit events; ECDSA signatures for election packages.
- Offline snapshot is signature-verified and sensitive fields are encrypted.
- The audit layer never stores personal data or vote choice.
- Threats considered: replay attacks, tampered offline packages, privilege escalation, database manipulation.
- Offline operation reduces network attack surface but does not eliminate physical or local security threats.

## 13. Testing

```bash
cd votexa-backend && mvn test
cd votexa-offline && mvn test
```

Included tests cover audit hashing, cryptography, name similarity and offline package verification. API testing is done with Postman and Swagger/OpenAPI.

## 14. Limitations

- Prototype with dummy data and mock government services only. No real Aadhaar, EPIC or ECI integration.
- Does not replace official election systems and cannot guarantee elimination of fraud.
- The Fabric network is optional and is not hosted in the public demo.
- Duplicate and anomaly detection is rule-based decision support, not a final decision-maker.

See [`docs/KNOWN_LIMITATIONS.md`](docs/KNOWN_LIMITATIONS.md).

## 15. Future Scope

- Hosted multi-organization Hyperledger Fabric network (Election, District and Audit authorities).
- Machine-learning based anomaly and duplicate detection (for example Isolation Forest).
- Hardware-backed key storage for polling devices.
- Integration with authorized government APIs, if officially available.
- Accessibility and multilingual interface.

## 16. Team

**Team size:** 5 | **Team Leader:** `[FILL]`

| # | Name | Roll No. | GitHub | Role / Module |
|---|---|---|---|---|
| M1 | `Shoeb Khan` (Team Leader) | `24EARAD151` | `https://github.com/shoebkhan114` | Auth, JWT, RBAC, voter registry, project setup |
| M2 | `Rohit Singh Goad` | `24EARAD141` | `[FILL]` | Address change, complaints, death verification, mock services |
| M3 | `Udit Singh Shekhawat` | `24EARAD168` | `[FILL]` | Duplicate detection, anomaly detection, offline validator |
| M4 | `Tanu Kumar` | `24EARAD162` | `[FILL]` | Election, polling station validation, one-time authorization |
| M5 | `Suresh Singh` | `24EARAD160` | `[FILL]` | Audit hash chain, blockchain, dashboard, reports |

## 17. Project Guide

| | |
|---|---|
| **Mentor** | Er. Ram Babu Buri |
| **Department** | Computer Science and Engineering |
| **Institution** | `Arya College of Engineering & I.T.` |
| **Course** | Java Project Based Learning (PBL), 5th Semester |
| **Academic Year** | 2026-27 |

## 18. Git Workflow for Contributors

```bash
git clone git@github.com:<github-username>/votexa.git
cd votexa
git config user.name  "Your Name"
git config user.email "email-used-on-github"
git checkout -b feature/<member>-<module>      # example: feature/m2-address-complaints

git pull origin main                           # before starting work
git add <your module folder>
git commit -m "Add address change request service"
git push origin feature/<member>-<module>
```

Rules:

1. Work only inside your own module folders.
2. Shared files (`pom.xml`, `application.properties`, migrations, `SecurityConfig`) are changed only by the Team Leader.
3. Open a Pull Request; the Team Leader reviews and merges.
4. Minimum 3 commits per week per member, from your own GitHub account.
5. Never commit `.env`, `keys/`, `ledger/`, `target/` or `node_modules/`.

## 19. Documentation

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)
- [`docs/API.md`](docs/API.md)
- [`docs/DEMO_SCRIPT.md`](docs/DEMO_SCRIPT.md)
- [`docs/KNOWN_LIMITATIONS.md`](docs/KNOWN_LIMITATIONS.md)
- [`SRS`](https://drive.google.com/file/d/1CiGCn29RNf89t3LNrwveDU91btLdos4a/view?usp=drive_link)

## 20. Disclaimer

VOTEXA is a college project built for learning and demonstration. All data is fictional. Government services are simulated. The system is not intended for use in real elections.
