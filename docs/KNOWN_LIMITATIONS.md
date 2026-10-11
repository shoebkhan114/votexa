# Known limitations (be upfront about these in the viva)

* **Academic prototype, dummy data only.** Mock Aadhaar and death-registry feeds are simulations, not integrations.
* **Not production security.** Demo passwords are published, the JWT secret has a development default, the rate limiter is in-memory,
  there is no MFA (listed as an advanced feature) and no device authentication for the offline validator.
* **Ledger.** Default mode is a local hash-chained file (development). Real tamper-evidence across organizations needs the Fabric network;
  Fabric `test-network` is a development network, not a production deployment.
* **Audit immutability.** The DB trigger can be removed by the database owner; verification (hash chain + ledger + signatures) detects this, it does not prevent it.
* **Offline database.** Voter names are encrypted per field (AES-256-GCM); the SQLite file itself is not encrypted (SQLCipher would be a hardening step).
  The sync file is not signed by the device. A stolen unlocked device, USB tampering and insider threats remain out of scope.
* **Voter authentication.** "Identity authenticated" is an officer confirmation checkbox; biometrics and real ID checks are out of scope.
* **Anomaly detection** is rule-based (repeat attempts, reconciliation mismatch). Tribuo/Weka/Smile models are future work.
* **Single active election per polling station** is assumed when resolving a station's session.
* **Not yet exercised in the generation environment:** Maven/npm builds, the running Spring application, the JavaFX UI and the Fabric network.
