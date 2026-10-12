#!/usr/bin/env bash
# Installs Hyperledger Fabric samples/binaries (needs Docker + Internet), starts the test network with a
# channel "votexachannel" and deploys the Java chaincode "votexa-audit".
#   Org1 = Election Authority, Org2 = Audit Authority (the two test-network organizations)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ ! -d fabric-samples ]]; then
  curl -sSLO https://raw.githubusercontent.com/hyperledger/fabric/main/scripts/install-fabric.sh
  chmod +x install-fabric.sh
  ./install-fabric.sh docker samples binary
fi

# test-network builds Java chaincode with ./gradlew installDist -> borrow the wrapper from the official sample
SAMPLE="fabric-samples/asset-transfer-basic/chaincode-java"
cp -r "$SAMPLE/gradle" "$SAMPLE/gradlew" chaincode/
chmod +x chaincode/gradlew

cd fabric-samples/test-network
./network.sh down
./network.sh up createChannel -c votexachannel -ca
./network.sh deployCC -c votexachannel -ccn votexa-audit -ccp "$ROOT/chaincode" -ccl java
echo
echo "Fabric is up. Run the backend with:"
echo "  cd ../../../votexa-backend && LEDGER_MODE=fabric mvn -Pfabric spring-boot:run -Dspring-boot.run.profiles=fabric"
