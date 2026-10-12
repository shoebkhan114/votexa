package com.votexa.chaincode;

import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

/** What is stored on-chain: hashes and metadata only. Never personal data, never a vote choice. */
@DataType()
public final class AuditRecord {
    @Property() private final String eventId;
    @Property() private final String eventType;
    @Property() private final String recordHash;
    @Property() private final String previousHash;
    @Property() private final String timestamp;
    @Property() private final String authorityId;
    @Property() private final String signature;
    @Property() private final String txId;

    public AuditRecord(@JsonProperty("eventId") final String eventId,
                       @JsonProperty("eventType") final String eventType,
                       @JsonProperty("recordHash") final String recordHash,
                       @JsonProperty("previousHash") final String previousHash,
                       @JsonProperty("timestamp") final String timestamp,
                       @JsonProperty("authorityId") final String authorityId,
                       @JsonProperty("signature") final String signature,
                       @JsonProperty("txId") final String txId) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.recordHash = recordHash;
        this.previousHash = previousHash;
        this.timestamp = timestamp;
        this.authorityId = authorityId;
        this.signature = signature;
        this.txId = txId;
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getRecordHash() { return recordHash; }
    public String getPreviousHash() { return previousHash; }
    public String getTimestamp() { return timestamp; }
    public String getAuthorityId() { return authorityId; }
    public String getSignature() { return signature; }
    public String getTxId() { return txId; }
}
