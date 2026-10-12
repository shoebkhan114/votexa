package com.votexa.blockchain;

import java.util.Optional;

/** Abstraction over the audit ledger: local hash-chain file (default) or Hyperledger Fabric. */
public interface LedgerClient {
    /** Submits a record and returns the ledger transaction id. Must be idempotent per eventId. */
    String submit(LedgerRecord record) throws Exception;
    Optional<String> fetchRecordHash(String eventId) throws Exception;
    /** Number of records on the ledger, or -1 if unknown. */
    long recordCount() throws Exception;
    String mode();
}
