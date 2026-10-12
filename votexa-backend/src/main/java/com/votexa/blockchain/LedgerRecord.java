package com.votexa.blockchain;

/** Exactly what goes on-chain: hashes + metadata only. */
public record LedgerRecord(String eventId, String eventType, String recordHash, String previousHash,
                           String timestamp, String authorityId, String signature) {}
