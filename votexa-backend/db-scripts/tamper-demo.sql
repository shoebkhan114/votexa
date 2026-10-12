-- DEMO ONLY: simulates an insider with database-owner rights editing an audit row.
-- 1) Run this, 2) click "Verify integrity" on the Blockchain Audit page -> INTEGRITY VIOLATION.
-- Usage: psql -h localhost -U shoeb114 -d votexadb -f tamper-demo.sql
BEGIN;
ALTER TABLE audit_events DISABLE TRIGGER trg_audit_events_immutable;
UPDATE audit_events
   SET new_value = 'TAMPERED'
 WHERE id = (SELECT min(id) FROM audit_events);
ALTER TABLE audit_events ENABLE TRIGGER trg_audit_events_immutable;
COMMIT;
SELECT id, action, new_value FROM audit_events ORDER BY id LIMIT 3;
