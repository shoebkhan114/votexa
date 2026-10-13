-- Ledger table used when LEDGER_MODE=db (hosted demo). Append-only, hash-chained rows.
-- Stores hashes and metadata only, never personal data or vote choice.
CREATE TABLE IF NOT EXISTS ledger_records (
    seq             BIGSERIAL PRIMARY KEY,
    event_id        TEXT NOT NULL UNIQUE,
    tx_id           TEXT NOT NULL,
    event_type      TEXT NOT NULL,
    record_hash     TEXT NOT NULL,
    previous_hash   TEXT,
    event_timestamp TEXT,
    authority_id    TEXT,
    signature       TEXT,
    prev_line_hash  TEXT NOT NULL,
    line_hash       TEXT NOT NULL
);
