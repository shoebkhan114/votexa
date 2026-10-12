package com.votexa.chaincode;

import com.owlike.genson.Genson;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * VOTEXA audit ledger chaincode (Java).
 * registerEvent   - anchor an audit event (hash + metadata)
 * getEvent        - read one anchor
 * verifyAuditEvent- compare a recomputed hash with the anchored one
 * getAllEvents    - list anchors
 * getEventHistory - ledger history of one key
 * The type-specific operations of the design (recordAddressChange, recordStatusChange, recordVerification,
 * recordPollingEvent) are all registerEvent with a different eventType.
 */
@Contract(name = "votexa-audit",
        info = @Info(title = "VOTEXA Audit Ledger", description = "Tamper-evident audit anchors (hashes and metadata only)",
                version = "0.1.0", license = @License(name = "Apache-2.0")))
@Default
public final class VotexaAuditContract implements ContractInterface {
    private static final Pattern HASH = Pattern.compile("^[0-9a-f]{64}$");
    private static final Set<String> TYPES = Set.of("ADDRESS_CHANGE", "STATUS_CHANGE", "VOTER_VERIFICATION", "DUPLICATE_REVIEW",
            "POLLING_PACKAGE_CREATED", "POLLING_EVENT", "AUDIT_EVENT");

    private final Genson genson = new Genson();

    private boolean exists(final ChaincodeStub stub, final String key) {
        final String v = stub.getStringState(key);
        return v != null && !v.isEmpty();
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public AuditRecord registerEvent(final Context ctx, final String eventId, final String eventType, final String recordHash,
                                     final String previousHash, final String timestamp, final String authorityId,
                                     final String signature) {
        final ChaincodeStub stub = ctx.getStub();
        if (eventId == null || eventId.isBlank()) throw new ChaincodeException("eventId is required", "INVALID_INPUT");
        if (!TYPES.contains(eventType)) throw new ChaincodeException("Unknown eventType: " + eventType, "INVALID_INPUT");
        if (!HASH.matcher(recordHash).matches() || !HASH.matcher(previousHash).matches())
            throw new ChaincodeException("recordHash and previousHash must be 64 hex chars (SHA-256)", "INVALID_INPUT");
        if (exists(stub, eventId)) throw new ChaincodeException("Event already exists: " + eventId, "ALREADY_EXISTS");

        final AuditRecord rec = new AuditRecord(eventId, eventType, recordHash, previousHash, timestamp, authorityId, signature, stub.getTxId());
        stub.putStringState(eventId, genson.serialize(rec));
        return rec;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public AuditRecord getEvent(final Context ctx, final String eventId) {
        final String json = ctx.getStub().getStringState(eventId);
        if (json == null || json.isEmpty()) throw new ChaincodeException("Event not found: " + eventId, "NOT_FOUND");
        return genson.deserialize(json, AuditRecord.class);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean verifyAuditEvent(final Context ctx, final String eventId, final String recordHash) {
        final String json = ctx.getStub().getStringState(eventId);
        if (json == null || json.isEmpty()) return false;
        return genson.deserialize(json, AuditRecord.class).getRecordHash().equals(recordHash);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getAllEvents(final Context ctx) {
        final List<AuditRecord> out = new ArrayList<>();
        try (QueryResultsIterator<KeyValue> results = ctx.getStub().getStateByRange("", "")) {
            for (KeyValue kv : results) out.add(genson.deserialize(kv.getStringValue(), AuditRecord.class));
        } catch (Exception e) {
            throw new ChaincodeException("Could not read events: " + e.getMessage(), "READ_ERROR");
        }
        return genson.serialize(out);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getEventHistory(final Context ctx, final String eventId) {
        final List<Map<String, Object>> out = new ArrayList<>();
        try (QueryResultsIterator<KeyModification> it = ctx.getStub().getHistoryForKey(eventId)) {
            for (KeyModification km : it) {
                final Map<String, Object> m = new LinkedHashMap<>();
                m.put("txId", km.getTxId());
                m.put("timestamp", km.getTimestamp().toString());
                m.put("deleted", km.isDeleted());
                m.put("value", km.getStringValue());
                out.add(m);
            }
        } catch (Exception e) {
            throw new ChaincodeException("Could not read history: " + e.getMessage(), "READ_ERROR");
        }
        return genson.serialize(out);
    }
}
