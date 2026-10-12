# votexa-blockchain

Permissioned Hyperledger Fabric network used as the **tamper-evident audit layer**.

| Item | Value |
|---|---|
| Network | Fabric `test-network` (2 organisations) |
| Mapping | Org1 = Election Authority, Org2 = Audit Authority |
| Channel | `votexachannel` |
| Chaincode | `votexa-audit` (Java, `chaincode/`) |
| Backend integration | Java Fabric Gateway SDK (`FabricLedgerClient`, Maven profile `fabric`) |

## What is stored on-chain
`eventId, eventType, recordHash (SHA-256), previousHash, timestamp, authorityId, signature, txId` only.
Never Aadhaar-like IDs, addresses, phone numbers, passwords, face images, or any vote choice.

## Start / stop
```bash
./scripts/setup-fabric.sh        # needs Docker + Internet; installs fabric-samples, starts network, deploys chaincode
./scripts/test-chaincode.sh      # anchors one dummy event and reads it back
./scripts/teardown-fabric.sh
```
`organizations/`, `channels/` and `network/` are filled by Fabric's `network.sh` inside `fabric-samples/test-network`
(they are listed here only to mirror the project structure).

## Use it from the backend
```bash
cd ../votexa-backend
LEDGER_MODE=fabric mvn -Pfabric spring-boot:run -Dspring-boot.run.profiles=fabric
```
Without Fabric, the backend uses a local hash-chained ledger file (`LEDGER_MODE=local`, the default) so the whole
system still runs for development and demos.

## Notes
* `test-network` is a development network, not a production deployment.
* Chaincode functions: `registerEvent`, `getEvent`, `verifyAuditEvent`, `getAllEvents`, `getEventHistory`.
  The design's `recordAddressChange / recordStatusChange / recordVerification / recordPollingEvent` are `registerEvent` with a different `eventType`.
