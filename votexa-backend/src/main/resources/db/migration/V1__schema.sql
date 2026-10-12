-- =====================================================================
-- VOTEXA  |  V1 schema  |  PostgreSQL 16
-- Academic prototype. Dummy / simulated data only.
-- Blockchain stores only hashes + metadata (see blockchain_transactions).
-- Actual vote choice is NEVER stored anywhere in this schema.
-- =====================================================================

-- ---------- helper: auto-maintain updated_at -------------------------
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ---------- 1. roles -------------------------------------------------
CREATE TABLE roles (
    id          INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(30)  NOT NULL UNIQUE
                CHECK (name IN ('CITIZEN','BLO','ERO','POLLING_OFFICER','AUDITOR','SUPER_ADMIN')),
    description VARCHAR(200)
);

-- ---------- 2. constituencies ---------------------------------------
CREATE TABLE constituencies (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code       VARCHAR(20)  NOT NULL UNIQUE,
    name       VARCHAR(100) NOT NULL,
    district   VARCHAR(100) NOT NULL,
    state      VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------- 3. polling_stations -------------------------------------
CREATE TABLE polling_stations (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code            VARCHAR(20)  NOT NULL UNIQUE,
    name            VARCHAR(150) NOT NULL,
    constituency_id BIGINT       NOT NULL REFERENCES constituencies(id),
    area            VARCHAR(100) NOT NULL,
    address_text    VARCHAR(255),
    capacity        INTEGER      NOT NULL DEFAULT 1000 CHECK (capacity > 0),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_polling_stations_constituency ON polling_stations(constituency_id);

-- ---------- 4. users -------------------------------------------------
CREATE TABLE users (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username              VARCHAR(50)  NOT NULL UNIQUE,
    password_hash         VARCHAR(100) NOT NULL,           -- BCrypt
    full_name             VARCHAR(100) NOT NULL,
    email                 VARCHAR(150) UNIQUE,
    role_id               INTEGER      NOT NULL REFERENCES roles(id),
    constituency_id       BIGINT       REFERENCES constituencies(id),  -- officer scope (optional)
    enabled               BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_login_attempts INTEGER      NOT NULL DEFAULT 0,
    last_login_at         TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_users_role ON users(role_id);

-- ---------- 5. elections --------------------------------------------
CREATE TABLE elections (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code          VARCHAR(30)  NOT NULL UNIQUE,
    name          VARCHAR(150) NOT NULL,
    election_type VARCHAR(20)  NOT NULL
                  CHECK (election_type IN ('LOK_SABHA','VIDHAN_SABHA','LOCAL_BODY','MOCK')),
    status        VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                  CHECK (status IN ('DRAFT','ROLL_FROZEN','POLLING_OPEN','POLLING_CLOSED','RECONCILED')),
    roll_version  INTEGER      NOT NULL DEFAULT 1 CHECK (roll_version > 0),
    polling_start TIMESTAMPTZ,
    polling_end   TIMESTAMPTZ,
    snapshot_hash CHAR(64),                                -- SHA-256 of signed roll snapshot
    frozen_at     TIMESTAMPTZ,
    created_by    BIGINT       REFERENCES users(id),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CHECK (polling_end IS NULL OR polling_start IS NULL OR polling_end > polling_start)
);

-- ---------- 6. voters ------------------------------------------------
CREATE TABLE voters (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    voter_code       VARCHAR(20)  NOT NULL UNIQUE,         -- simulated voter ID (dummy)
    full_name        VARCHAR(120) NOT NULL,
    dob              DATE         NOT NULL,
    gender           VARCHAR(10)  NOT NULL CHECK (gender IN ('MALE','FEMALE','OTHER')),
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('ACTIVE','PENDING','UNDER_VERIFICATION','SHIFTED',
                                       'DECEASED','DUPLICATE_REVIEW','INACTIVE','BLOCKED')),
    constituency_id  BIGINT       NOT NULL REFERENCES constituencies(id),
    polling_station_id BIGINT     REFERENCES polling_stations(id),
    citizen_user_id  BIGINT       UNIQUE REFERENCES users(id),   -- links citizen login to record
    registration_date DATE        NOT NULL DEFAULT CURRENT_DATE,
    record_hash      CHAR(64),                              -- SHA-256 fingerprint of the record
    version          BIGINT       NOT NULL DEFAULT 0,       -- JPA @Version (optimistic locking)
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- an ACTIVE voter must always be mapped to a polling station
    CONSTRAINT chk_active_has_station CHECK (status <> 'ACTIVE' OR polling_station_id IS NOT NULL)
);
CREATE INDEX idx_voters_status        ON voters(status);
CREATE INDEX idx_voters_constituency  ON voters(constituency_id);
CREATE INDEX idx_voters_station       ON voters(polling_station_id);
CREATE INDEX idx_voters_name_lower    ON voters(lower(full_name));
CREATE INDEX idx_voters_name_dob      ON voters(lower(full_name), dob);   -- duplicate candidate lookup

-- ---------- 7. addresses --------------------------------------------
CREATE TABLE addresses (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    voter_id   BIGINT       NOT NULL REFERENCES voters(id),
    line1      VARCHAR(200) NOT NULL,
    locality   VARCHAR(100) NOT NULL,
    district   VARCHAR(100) NOT NULL,
    state      VARCHAR(100) NOT NULL,
    pincode    CHAR(6)      NOT NULL CHECK (pincode ~ '^[0-9]{6}$'),
    is_current BOOLEAN      NOT NULL DEFAULT TRUE,
    valid_from DATE         NOT NULL DEFAULT CURRENT_DATE,
    valid_to   DATE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CHECK (valid_to IS NULL OR valid_to >= valid_from)
);
CREATE INDEX idx_addresses_voter ON addresses(voter_id);
-- only ONE current address per voter
CREATE UNIQUE INDEX uq_addresses_one_current ON addresses(voter_id) WHERE is_current;

-- ---------- 8. voter_assignments (per election / roll version) ------
CREATE TABLE voter_assignments (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    voter_id           BIGINT      NOT NULL REFERENCES voters(id),
    election_id        BIGINT      NOT NULL REFERENCES elections(id),
    polling_station_id BIGINT      NOT NULL REFERENCES polling_stations(id),
    roll_version       INTEGER     NOT NULL CHECK (roll_version > 0),
    assigned_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (voter_id, election_id)
);
CREATE INDEX idx_assign_election_station ON voter_assignments(election_id, polling_station_id);

-- ---------- 9. verification_requests --------------------------------
CREATE TABLE verification_requests (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    voter_id     BIGINT      NOT NULL REFERENCES voters(id),
    request_type VARCHAR(30) NOT NULL
                 CHECK (request_type IN ('NEW_REGISTRATION','RECORD_VERIFICATION',
                                         'CORRECTION','ADDRESS_VERIFICATION')),
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                 CHECK (status IN ('PENDING','VERIFIED','REJECTED')),
    requested_by BIGINT      REFERENCES users(id),
    assigned_blo BIGINT      REFERENCES users(id),
    remarks      VARCHAR(500),
    reviewed_by  BIGINT      REFERENCES users(id),
    reviewed_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_verif_voter  ON verification_requests(voter_id);
CREATE INDEX idx_verif_status ON verification_requests(status);

-- ---------- 10. address_change_requests -----------------------------
CREATE TABLE address_change_requests (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    request_code           VARCHAR(30)  NOT NULL UNIQUE,
    voter_id               BIGINT       NOT NULL REFERENCES voters(id),
    source                 VARCHAR(25)  NOT NULL DEFAULT 'CITIZEN'
                           CHECK (source IN ('CITIZEN','MOCK_AADHAAR_EVENT')),
    new_line1              VARCHAR(200) NOT NULL,
    new_locality           VARCHAR(100) NOT NULL,
    new_district           VARCHAR(100) NOT NULL,
    new_state              VARCHAR(100) NOT NULL,
    new_pincode            CHAR(6)      NOT NULL CHECK (new_pincode ~ '^[0-9]{6}$'),
    new_constituency_id    BIGINT       REFERENCES constituencies(id),
    new_polling_station_id BIGINT       REFERENCES polling_stations(id),
    status                 VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                           CHECK (status IN ('PENDING','UNDER_VERIFICATION','VERIFIED','APPROVED','REJECTED')),
    reason                 VARCHAR(300),
    verified_by            BIGINT       REFERENCES users(id),
    verified_at            TIMESTAMPTZ,
    decided_by             BIGINT       REFERENCES users(id),
    decided_at             TIMESTAMPTZ,
    decision_remarks       VARCHAR(500),
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_acr_voter  ON address_change_requests(voter_id);
CREATE INDEX idx_acr_status ON address_change_requests(status);

-- ---------- 11. status_changes --------------------------------------
CREATE TABLE status_changes (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    voter_id   BIGINT      NOT NULL REFERENCES voters(id),
    old_status VARCHAR(20) NOT NULL
               CHECK (old_status IN ('ACTIVE','PENDING','UNDER_VERIFICATION','SHIFTED',
                                     'DECEASED','DUPLICATE_REVIEW','INACTIVE','BLOCKED')),
    new_status VARCHAR(20) NOT NULL
               CHECK (new_status IN ('ACTIVE','PENDING','UNDER_VERIFICATION','SHIFTED',
                                     'DECEASED','DUPLICATE_REVIEW','INACTIVE','BLOCKED')),
    reason     VARCHAR(300) NOT NULL,
    changed_by BIGINT      REFERENCES users(id),           -- NULL = system action
    source_ref VARCHAR(60),                                 -- e.g. ACR-0001, DUP-0001
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (old_status <> new_status)
);
CREATE INDEX idx_status_changes_voter ON status_changes(voter_id, created_at DESC);

-- ---------- 12. duplicate_cases -------------------------------------
CREATE TABLE duplicate_cases (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    case_code          VARCHAR(30)  NOT NULL UNIQUE,
    voter_a_id         BIGINT       NOT NULL REFERENCES voters(id),
    voter_b_id         BIGINT       NOT NULL REFERENCES voters(id),
    similarity_score   NUMERIC(5,2) NOT NULL CHECK (similarity_score BETWEEN 0 AND 100),
    matched_attributes VARCHAR(200),
    status             VARCHAR(15)  NOT NULL DEFAULT 'OPEN'
                       CHECK (status IN ('OPEN','CONFIRMED','REJECTED')),
    detected_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    reviewed_by        BIGINT       REFERENCES users(id),
    reviewed_at        TIMESTAMPTZ,
    remarks            VARCHAR(500),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CHECK (voter_a_id < voter_b_id),                        -- one row per unordered pair
    UNIQUE (voter_a_id, voter_b_id)
);
CREATE INDEX idx_dup_status ON duplicate_cases(status);

-- ---------- 13. death_verification_cases ----------------------------
CREATE TABLE death_verification_cases (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    case_code         VARCHAR(30) NOT NULL UNIQUE,
    voter_id          BIGINT      NOT NULL REFERENCES voters(id),
    source            VARCHAR(25) NOT NULL
                      CHECK (source IN ('MOCK_DEATH_REGISTRY','CITIZEN_COMPLAINT','BLO_REPORT')),
    registry_ref_hash CHAR(64),                              -- hash only, never the certificate number
    status            VARCHAR(15) NOT NULL DEFAULT 'OPEN'
                      CHECK (status IN ('OPEN','VERIFIED','REJECTED')),
    verified_by       BIGINT      REFERENCES users(id),      -- BLO / field verification
    reviewed_by       BIGINT      REFERENCES users(id),      -- ERO final decision
    reviewed_at       TIMESTAMPTZ,
    remarks           VARCHAR(500),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_death_voter  ON death_verification_cases(voter_id);
CREATE INDEX idx_death_status ON death_verification_cases(status);

-- ---------- 14. polling_sessions ------------------------------------
CREATE TABLE polling_sessions (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    election_id        BIGINT      NOT NULL REFERENCES elections(id),
    polling_station_id BIGINT      NOT NULL REFERENCES polling_stations(id),
    status             VARCHAR(15) NOT NULL DEFAULT 'NOT_STARTED'
                       CHECK (status IN ('NOT_STARTED','OPEN','CLOSED')),
    eligible_count     INTEGER     NOT NULL DEFAULT 0 CHECK (eligible_count >= 0),
    recorded_count     INTEGER     CHECK (recorded_count IS NULL OR recorded_count >= 0),
                                   -- cast/recorded count reported at close (for reconciliation)
    snapshot_hash      CHAR(64),
    opened_by          BIGINT      REFERENCES users(id),
    opened_at          TIMESTAMPTZ,
    closed_by          BIGINT      REFERENCES users(id),
    closed_at          TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (election_id, polling_station_id)
);

-- ---------- 15. voting_authorizations -------------------------------
-- One row per (election, voter), created NOT_VOTED when the roll is frozen.
-- Authorization = atomic UPDATE ... WHERE voting_status = 'NOT_VOTED'.
-- This table never links a voter to a candidate.
CREATE TABLE voting_authorizations (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    election_id        BIGINT      NOT NULL REFERENCES elections(id),
    polling_session_id BIGINT      NOT NULL REFERENCES polling_sessions(id),
    voter_id           BIGINT      NOT NULL REFERENCES voters(id),
    voting_status      VARCHAR(10) NOT NULL DEFAULT 'NOT_VOTED'
                       CHECK (voting_status IN ('NOT_VOTED','VOTED')),
    authenticated_at   TIMESTAMPTZ,
    authorized_at      TIMESTAMPTZ,
    authorized_by      BIGINT      REFERENCES users(id),
    device_id          VARCHAR(60),
    UNIQUE (election_id, voter_id),                          -- DB-level one-time guarantee
    CHECK (voting_status = 'NOT_VOTED' OR authorized_at IS NOT NULL)
);
CREATE INDEX idx_vauth_session_status ON voting_authorizations(polling_session_id, voting_status);

-- ---------- 16. polling_attempts (extra) ----------------------------
-- Every validation / authorization attempt, including failures.
-- Feeds anomaly detection (e.g. repeated ALREADY_VOTED).
CREATE TABLE polling_attempts (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    polling_session_id BIGINT      NOT NULL REFERENCES polling_sessions(id),
    voter_id           BIGINT      REFERENCES voters(id),    -- NULL if voter code not found
    voter_code_entered VARCHAR(20) NOT NULL,
    result             VARCHAR(25) NOT NULL
                       CHECK (result IN ('AUTHORIZED','NOT_ELIGIBLE','ALREADY_VOTED',
                                         'WRONG_POLLING_STATION','RECORD_INACTIVE',
                                         'VOTER_NOT_FOUND','POLLING_CLOSED')),
    detail             VARCHAR(300),
    performed_by       BIGINT      REFERENCES users(id),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_attempts_session_voter ON polling_attempts(polling_session_id, voter_id);

-- ---------- 17. audit_events (append-only) --------------------------
-- WHO / WHAT / WHEN / WHY / RESULT, hash-chained via previous_hash.
CREATE TABLE audit_events (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_uuid    UUID         NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    event_type    VARCHAR(30)  NOT NULL
                  CHECK (event_type IN ('ADDRESS_CHANGE','STATUS_CHANGE','VOTER_VERIFICATION',
                                        'DUPLICATE_REVIEW','POLLING_PACKAGE_CREATED',
                                        'POLLING_EVENT','AUDIT_EVENT')),
    entity_type   VARCHAR(40)  NOT NULL,                    -- WHAT: e.g. VOTER, ELECTION
    entity_id     VARCHAR(64)  NOT NULL,
    actor_user_id BIGINT       REFERENCES users(id),        -- WHO
    actor_label   VARCHAR(60)  NOT NULL,                    -- e.g. ERO-102, SYSTEM
    action        VARCHAR(60)  NOT NULL,
    old_value     VARCHAR(200),
    new_value     VARCHAR(200),
    reason        VARCHAR(300),                              -- WHY
    result        VARCHAR(10)  NOT NULL DEFAULT 'SUCCESS'
                  CHECK (result IN ('SUCCESS','FAILURE','DENIED')),
    record_hash   CHAR(64)     NOT NULL,                    -- SHA-256 of this event
    previous_hash CHAR(64)     NOT NULL,                    -- record_hash of previous event
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()       -- WHEN
);
CREATE INDEX idx_audit_entity  ON audit_events(entity_type, entity_id);
CREATE INDEX idx_audit_type    ON audit_events(event_type, created_at DESC);
CREATE INDEX idx_audit_actor   ON audit_events(actor_user_id);

CREATE OR REPLACE FUNCTION prevent_audit_modification() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_events is append-only (% not allowed)', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_events_immutable
    BEFORE UPDATE OR DELETE ON audit_events
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_modification();

-- ---------- 18. blockchain_transactions -----------------------------
CREATE TABLE blockchain_transactions (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    audit_event_id   BIGINT       NOT NULL UNIQUE REFERENCES audit_events(id),
    fabric_tx_id     VARCHAR(100) UNIQUE,                   -- NULL until committed on Fabric
    event_type       VARCHAR(30)  NOT NULL,
    record_hash      CHAR(64)     NOT NULL,
    previous_hash    CHAR(64)     NOT NULL,
    authority_id     VARCHAR(60)  NOT NULL,
    signature        TEXT,
    status           VARCHAR(12)  NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING','COMMITTED','FAILED')),
    integrity_status VARCHAR(12)  NOT NULL DEFAULT 'UNVERIFIED'
                     CHECK (integrity_status IN ('UNVERIFIED','VERIFIED','VIOLATION')),
    retry_count      INTEGER      NOT NULL DEFAULT 0,
    submitted_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    committed_at     TIMESTAMPTZ,
    last_verified_at TIMESTAMPTZ
);
CREATE INDEX idx_bc_status ON blockchain_transactions(status);

-- ---------- 19. complaints ------------------------------------------
CREATE TABLE complaints (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    case_code    VARCHAR(30) NOT NULL UNIQUE,
    voter_id     BIGINT      REFERENCES voters(id),
    submitted_by BIGINT      NOT NULL REFERENCES users(id),
    category     VARCHAR(30) NOT NULL
                 CHECK (category IN ('WRONG_ADDRESS','DUPLICATE_RECORD','INCORRECT_INFORMATION',
                                     'WRONG_POLLING_STATION','DECEASED_PERSON_LISTED','OTHER')),
    description  TEXT        NOT NULL,
    status       VARCHAR(15) NOT NULL DEFAULT 'OPEN'
                 CHECK (status IN ('OPEN','UNDER_REVIEW','RESOLVED','REJECTED')),
    assigned_to  BIGINT      REFERENCES users(id),
    decision     TEXT,
    decided_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_complaints_status ON complaints(status);
CREATE INDEX idx_complaints_voter  ON complaints(voter_id);

-- ---------- 20. anomaly_alerts (extra) ------------------------------
-- Alerts are "review required" flags. They never accuse a voter or officer
-- and never invalidate an election automatically.
CREATE TABLE anomaly_alerts (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    election_id        BIGINT      NOT NULL REFERENCES elections(id),
    polling_session_id BIGINT      REFERENCES polling_sessions(id),
    alert_type         VARCHAR(40) NOT NULL
                       CHECK (alert_type IN ('RECONCILIATION_MISMATCH',
                                             'POSSIBLE_MULTIPLE_VOTING_ATTEMPT',
                                             'UNEXPECTED_POLLING_ACTIVITY')),
    severity           VARCHAR(10) NOT NULL CHECK (severity IN ('INFO','REVIEW','CRITICAL')),
    message            VARCHAR(500) NOT NULL,
    status             VARCHAR(15) NOT NULL DEFAULT 'OPEN'
                       CHECK (status IN ('OPEN','ACKNOWLEDGED','RESOLVED')),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_by        BIGINT      REFERENCES users(id),
    resolved_at        TIMESTAMPTZ
);
CREATE INDEX idx_anomaly_election_status ON anomaly_alerts(election_id, status);

-- ---------- updated_at triggers -------------------------------------
DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['users','elections','voters','verification_requests',
                             'address_change_requests','duplicate_cases',
                             'death_verification_cases','complaints']
    LOOP
        EXECUTE format(
            'CREATE TRIGGER trg_%s_updated_at BEFORE UPDATE ON %I
             FOR EACH ROW EXECUTE FUNCTION set_updated_at()', t, t);
    END LOOP;
END $$;
