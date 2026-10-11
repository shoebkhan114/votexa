-- =====================================================================
-- VOTEXA  |  V2 demo seed  |  DUMMY DATA ONLY
-- All names, addresses and IDs below are fictional.
-- Demo login password for every user: Votexa@123  (change before any deployment)
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;   -- crypt()/digest() for seed convenience

-- ---------- roles ----------------------------------------------------
INSERT INTO roles (name, description) VALUES
 ('CITIZEN',         'View own record, request changes, submit complaints'),
 ('BLO',             'Booth Level Officer: verifies records, recommends changes'),
 ('ERO',             'Electoral Registration Officer: approves/rejects requests'),
 ('POLLING_OFFICER', 'Validates voters and authorizes voting at a polling station'),
 ('AUDITOR',         'Views audit events, verifies integrity, generates reports'),
 ('SUPER_ADMIN',     'Manages users, roles, configuration and elections');

-- ---------- constituencies & polling stations ------------------------
INSERT INTO constituencies (code, name, district, state) VALUES
 ('RJ-JP-01', 'Demo Constituency A', 'Jaipur', 'Rajasthan'),
 ('RJ-JP-02', 'Demo Constituency B', 'Jaipur', 'Rajasthan'),
 ('RJ-DA-01', 'Demo Constituency C', 'Dausa',  'Rajasthan');

INSERT INTO polling_stations (code, name, constituency_id, area, address_text, capacity)
SELECT p.code, p.name, c.id, p.area, p.addr, p.cap
FROM (VALUES
 ('PS-101', 'Demo Govt School 101', 'RJ-JP-01', 'Ward 1', 'Dummy Address, Demo Nagar',   900),
 ('PS-102', 'Demo Govt School 102', 'RJ-JP-01', 'Ward 2', 'Dummy Address, Sample Colony', 900),
 ('PS-201', 'Demo Community Hall 201', 'RJ-JP-02', 'Ward 5', 'Dummy Address, Test Vihar', 800),
 ('PS-301', 'Demo Panchayat Bhawan 301', 'RJ-DA-01', 'Ward 9', 'Dummy Address, Mock Gram', 700)
) AS p(code, name, cons, area, addr, cap)
JOIN constituencies c ON c.code = p.cons;

-- ---------- users (BCrypt via pgcrypto; Spring accepts $2a$) ---------
INSERT INTO users (username, password_hash, full_name, email, role_id, constituency_id)
SELECT u.username, crypt('Votexa@123', gen_salt('bf', 10)), u.full_name, u.email, r.id, c.id
FROM (VALUES
 ('admin',    'Super Admin',            'admin@votexa.test',    'SUPER_ADMIN',     NULL),
 ('ero102',   'ERO Constituency A',     'ero102@votexa.test',   'ERO',             'RJ-JP-01'),
 ('blo101',   'BLO Ward 1',             'blo101@votexa.test',   'BLO',             'RJ-JP-01'),
 ('po102',    'Polling Officer PS-102', 'po102@votexa.test',    'POLLING_OFFICER', 'RJ-JP-01'),
 ('auditor1', 'Audit Authority',        'auditor1@votexa.test', 'AUDITOR',         NULL),
 ('citizen1', 'Aarav Sharma',           'citizen1@votexa.test', 'CITIZEN',         NULL),
 ('citizen2', 'Priya Verma',            'citizen2@votexa.test', 'CITIZEN',         NULL)
) AS u(username, full_name, email, role_name, cons_code)
JOIN roles r ON r.name = u.role_name
LEFT JOIN constituencies c ON c.code = u.cons_code;

-- ---------- scenario voters (VTX0000001 .. VTX0000010) --------------
INSERT INTO voters (voter_code, full_name, dob, gender, status, constituency_id, polling_station_id)
SELECT v.code, v.name, v.dob::date, v.gender, v.status, c.id, ps.id
FROM (VALUES
 ('VTX0000001', 'Aarav Sharma',   '1998-05-12', 'MALE',   'ACTIVE',             'RJ-JP-01', 'PS-102'),
 ('VTX0000002', 'Priya Verma',    '1996-11-03', 'FEMALE', 'ACTIVE',             'RJ-JP-01', 'PS-102'),
 ('VTX0000003', 'Rahul Kumar',    '1995-03-14', 'MALE',   'ACTIVE',             'RJ-JP-01', 'PS-101'),
 ('VTX0000004', 'Rahul Kumar',    '1995-03-14', 'MALE',   'DUPLICATE_REVIEW',   'RJ-JP-01', 'PS-101'),
 ('VTX0000005', 'Sunita Devi',    '1948-07-21', 'FEMALE', 'UNDER_VERIFICATION', 'RJ-JP-01', 'PS-101'),
 ('VTX0000006', 'Mohan Lal',      '1970-02-02', 'MALE',   'SHIFTED',            'RJ-JP-02', NULL),
 ('VTX0000007', 'Kavita Meena',   '2003-09-30', 'FEMALE', 'PENDING',            'RJ-JP-01', NULL),
 ('VTX0000008', 'Imran Qureshi',  '1985-12-18', 'MALE',   'BLOCKED',            'RJ-JP-01', NULL),
 ('VTX0000009', 'Anil Gupta',     '1960-04-09', 'MALE',   'INACTIVE',           'RJ-JP-02', NULL),
 ('VTX0000010', 'Neha Singh',     '1999-01-25', 'FEMALE', 'ACTIVE',             'RJ-JP-02', 'PS-201')
) AS v(code, name, dob, gender, status, cons_code, ps_code)
JOIN constituencies c ON c.code = v.cons_code
LEFT JOIN polling_stations ps ON ps.code = v.ps_code;

-- ---------- bulk dummy voters (VTX0000011 .. VTX0000130) ------------
WITH st AS (
    SELECT id, constituency_id, row_number() OVER (ORDER BY id) - 1 AS idx
    FROM polling_stations
)
INSERT INTO voters (voter_code, full_name, dob, gender, status, constituency_id, polling_station_id)
SELECT 'VTX' || lpad(n::text, 7, '0'),
       (ARRAY['Aditya','Anjali','Deepak','Isha','Karan','Meera',
              'Nikhil','Pooja','Rohit','Sneha','Vikram','Zoya'])[1 + (n % 12)]
       || ' ' ||
       (ARRAY['Agarwal','Bhatt','Chauhan','Dixit','Joshi',
              'Khan','Mathur','Nair','Rathore','Saini'])[1 + ((n / 12) % 10)],
       DATE '1960-01-01' + ((n * 211) % 14000),
       CASE WHEN n % 2 = 0 THEN 'FEMALE' ELSE 'MALE' END,
       'ACTIVE',
       st.constituency_id,
       st.id
FROM generate_series(11, 130) AS n
JOIN st ON st.idx = n % 4;

-- link citizen logins to their voter records
UPDATE voters SET citizen_user_id = (SELECT id FROM users WHERE username = 'citizen1')
 WHERE voter_code = 'VTX0000001';
UPDATE voters SET citizen_user_id = (SELECT id FROM users WHERE username = 'citizen2')
 WHERE voter_code = 'VTX0000002';

-- ---------- one current address per voter ---------------------------
INSERT INTO addresses (voter_id, line1, locality, district, state, pincode)
SELECT v.id,
       'House ' || (v.id % 90 + 10) || ', Dummy Street ' || (v.id % 7 + 1),
       'Demo Locality ' || (v.id % 5 + 1),
       c.district, c.state,
       CASE c.district WHEN 'Jaipur' THEN '302001' ELSE '303303' END
FROM voters v
JOIN constituencies c ON c.id = v.constituency_id;

-- make the two "Rahul Kumar" records share an address (duplicate scenario)
UPDATE addresses a
   SET line1 = b.line1, locality = b.locality
  FROM addresses b
 WHERE a.voter_id = (SELECT id FROM voters WHERE voter_code = 'VTX0000004')
   AND b.voter_id = (SELECT id FROM voters WHERE voter_code = 'VTX0000003');

-- ---------- record fingerprints (SHA-256) ---------------------------
UPDATE voters
   SET record_hash = encode(digest(
         voter_code || '|' || full_name || '|' || dob::text || '|' || status, 'sha256'), 'hex');

-- ---------- history: status changes ---------------------------------
INSERT INTO status_changes (voter_id, old_status, new_status, reason, changed_by, source_ref)
SELECT v.id, s.old_status, s.new_status, s.reason, u.id, s.ref
FROM (VALUES
 ('VTX0000006', 'ACTIVE', 'SHIFTED',  'Moved to another constituency (demo)', 'ACR-0000'),
 ('VTX0000009', 'ACTIVE', 'INACTIVE', 'Verified inactive by BLO (demo)',      NULL)
) AS s(code, old_status, new_status, reason, ref)
JOIN voters v ON v.voter_code = s.code
CROSS JOIN (SELECT id FROM users WHERE username = 'ero102') u;

-- ---------- workflow cases ------------------------------------------
-- pending address change: Neha Singh moves from Constituency B to A
INSERT INTO address_change_requests
       (request_code, voter_id, source, new_line1, new_locality, new_district, new_state,
        new_pincode, new_constituency_id, new_polling_station_id, status, reason)
SELECT 'ACR-0001', v.id, 'CITIZEN', 'House 42, Dummy Street 3', 'Demo Locality 2',
       'Jaipur', 'Rajasthan', '302001', c.id, ps.id, 'PENDING', 'Relocated for work (demo)'
FROM voters v, constituencies c, polling_stations ps
WHERE v.voter_code = 'VTX0000010' AND c.code = 'RJ-JP-01' AND ps.code = 'PS-101';

-- potential duplicate: VTX0000003 vs VTX0000004
INSERT INTO duplicate_cases (case_code, voter_a_id, voter_b_id, similarity_score, matched_attributes)
SELECT 'DUP-0001', a.id, b.id, 97.50, 'NAME,DOB,ADDRESS'
FROM voters a, voters b
WHERE a.voter_code = 'VTX0000003' AND b.voter_code = 'VTX0000004';

-- possible death match from the mock registry
INSERT INTO death_verification_cases (case_code, voter_id, source, registry_ref_hash)
SELECT 'DTH-0001', id, 'MOCK_DEATH_REGISTRY',
       encode(digest('mock-registry-ref-0001', 'sha256'), 'hex')
FROM voters WHERE voter_code = 'VTX0000005';

-- pending new-registration verification
INSERT INTO verification_requests (voter_id, request_type, requested_by, assigned_blo, remarks)
SELECT v.id, 'NEW_REGISTRATION', cu.id, blo.id, 'New voter registration (demo)'
FROM voters v, users blo, users cu
WHERE v.voter_code = 'VTX0000007' AND blo.username = 'blo101' AND cu.username = 'citizen1';

-- sample complaint
INSERT INTO complaints (case_code, voter_id, submitted_by, category, description)
SELECT 'CMP-0001', v.id, u.id, 'WRONG_ADDRESS',
       'Demo complaint: house number in my record looks incorrect.'
FROM voters v, users u
WHERE v.voter_code = 'VTX0000001' AND u.username = 'citizen1';

-- ---------- demo election (polling currently open) ------------------
INSERT INTO elections (code, name, election_type, status, roll_version,
                       polling_start, polling_end, frozen_at, created_by)
SELECT 'ELC-2026-MOCK', 'Mock Election 2026 (Demo)', 'MOCK', 'POLLING_OPEN', 3,
       now() - interval '1 hour', now() + interval '10 hours', now() - interval '2 days', u.id
FROM users u WHERE u.username = 'admin';

-- roll snapshot: every ACTIVE voter is assigned to their station
INSERT INTO voter_assignments (voter_id, election_id, polling_station_id, roll_version)
SELECT v.id, e.id, v.polling_station_id, e.roll_version
FROM voters v
CROSS JOIN elections e
WHERE v.status = 'ACTIVE' AND e.code = 'ELC-2026-MOCK';

-- one polling session per station; PS-102 is OPEN for the demo
INSERT INTO polling_sessions (election_id, polling_station_id, status, eligible_count,
                              opened_by, opened_at)
SELECT e.id, ps.id,
       CASE WHEN ps.code = 'PS-102' THEN 'OPEN' ELSE 'NOT_STARTED' END,
       (SELECT count(*) FROM voter_assignments va
         WHERE va.election_id = e.id AND va.polling_station_id = ps.id),
       CASE WHEN ps.code = 'PS-102' THEN (SELECT id FROM users WHERE username = 'po102') END,
       CASE WHEN ps.code = 'PS-102' THEN now() - interval '1 hour' END
FROM polling_stations ps
CROSS JOIN elections e
WHERE e.code = 'ELC-2026-MOCK';

-- everyone starts NOT_VOTED
INSERT INTO voting_authorizations (election_id, polling_session_id, voter_id)
SELECT va.election_id, s.id, va.voter_id
FROM voter_assignments va
JOIN polling_sessions s
  ON s.election_id = va.election_id AND s.polling_station_id = va.polling_station_id;

-- mark 3 PS-102 voters (not the two demo citizens) as already VOTED
UPDATE voting_authorizations
   SET voting_status    = 'VOTED',
       authenticated_at = now() - interval '40 minutes',
       authorized_at    = now() - interval '39 minutes',
       authorized_by    = (SELECT id FROM users WHERE username = 'po102'),
       device_id        = 'DEMO-DEVICE-01'
 WHERE id IN (
       SELECT x.id
         FROM voting_authorizations x
         JOIN voters v           ON v.id = x.voter_id
         JOIN polling_sessions s ON s.id = x.polling_session_id
         JOIN polling_stations p ON p.id = s.polling_station_id
        WHERE p.code = 'PS-102'
          AND v.voter_code NOT IN ('VTX0000001', 'VTX0000002')
        ORDER BY v.id
        LIMIT 3);

-- NOTE: audit_events / blockchain_transactions are intentionally left empty.
-- The Spring Boot backend creates hash-chained audit events (and their
-- Fabric transactions) live, which is what Demo 5 and Demo 10 show.
