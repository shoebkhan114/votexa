#!/usr/bin/env bash
# Smoke test: anchor one dummy event and read it back (run after setup-fabric.sh).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TEST="$ROOT/fabric-samples/test-network"
ORG1="$TEST/organizations/peerOrganizations/org1.example.com"
ORG2="$TEST/organizations/peerOrganizations/org2.example.com"
export PATH="$ROOT/fabric-samples/bin:$PATH"
export FABRIC_CFG_PATH="$ROOT/fabric-samples/config/"
export CORE_PEER_TLS_ENABLED=true
export CORE_PEER_LOCALMSPID=Org1MSP
export CORE_PEER_TLS_ROOTCERT_FILE="$ORG1/peers/peer0.org1.example.com/tls/ca.crt"
export CORE_PEER_MSPCONFIGPATH="$ORG1/users/Admin@org1.example.com/msp"
export CORE_PEER_ADDRESS=localhost:7051

ID="smoke-$(date +%s)"
H="$(printf '%064d' 1)"
peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls \
  --cafile "$TEST/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem" \
  -C votexachannel -n votexa-audit \
  --peerAddresses localhost:7051 --tlsRootCertFiles "$ORG1/peers/peer0.org1.example.com/tls/ca.crt" \
  --peerAddresses localhost:9051 --tlsRootCertFiles "$ORG2/peers/peer0.org2.example.com/tls/ca.crt" \
  -c "{\"function\":\"registerEvent\",\"Args\":[\"$ID\",\"AUDIT_EVENT\",\"$H\",\"$(printf '%064d' 0)\",\"2026-10-08T10:00:00Z\",\"SMOKE\",\"sig\"]}"
sleep 3
peer chaincode query -C votexachannel -n votexa-audit -c "{\"function\":\"getEvent\",\"Args\":[\"$ID\"]}"
