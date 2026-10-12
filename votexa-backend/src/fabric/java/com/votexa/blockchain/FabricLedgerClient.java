package com.votexa.blockchain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;
import org.hyperledger.fabric.client.*;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Hyperledger Fabric ledger client (Java Fabric Gateway SDK). Compiled only with: mvn -Pfabric ...
 * Chaincode: votexa-blockchain/chaincode (registerEvent, getEvent, getAllEvents, verifyAuditEvent).
 */
@Component
@ConditionalOnProperty(name = "votexa.ledger.mode", havingValue = "fabric")
public class FabricLedgerClient implements LedgerClient {
    private final ManagedChannel channel;
    private final Gateway gateway;
    private final Contract contract;
    private final ObjectMapper om = new ObjectMapper();

    public FabricLedgerClient(@Value("${votexa.fabric.channel}") String channelName,
                              @Value("${votexa.fabric.chaincode}") String chaincode,
                              @Value("${votexa.fabric.msp-id}") String mspId,
                              @Value("${votexa.fabric.peer-endpoint}") String peerEndpoint,
                              @Value("${votexa.fabric.peer-host-alias}") String hostAlias,
                              @Value("${votexa.fabric.base-dir}") String baseDir) throws Exception {
        Path base = Path.of(baseDir);
        Path tlsCert = base.resolve("peers/peer0.org1.example.com/tls/ca.crt");
        Path certDir = base.resolve("users/User1@org1.example.com/msp/signcerts");
        Path keyDir = base.resolve("users/User1@org1.example.com/msp/keystore");

        Path certFile;
        try (Stream<Path> s = Files.list(certDir)) { certFile = s.findFirst().orElseThrow(); }
        X509Certificate cert;
        try (Reader r = Files.newBufferedReader(certFile, StandardCharsets.UTF_8)) { cert = Identities.readX509Certificate(r); }
        Path keyFile;
        try (Stream<Path> s = Files.list(keyDir)) { keyFile = s.findFirst().orElseThrow(); }
        PrivateKey key;
        try (Reader r = Files.newBufferedReader(keyFile, StandardCharsets.UTF_8)) { key = Identities.readPrivateKey(r); }

        ChannelCredentials creds = TlsChannelCredentials.newBuilder().trustManager(tlsCert.toFile()).build();
        this.channel = Grpc.newChannelBuilder(peerEndpoint, creds).overrideAuthority(hostAlias).build();
        this.gateway = Gateway.newInstance()
                .identity(new X509Identity(mspId, cert))
                .signer(Signers.newPrivateKeySigner(key))
                .connection(channel)
                .connect();
        this.contract = gateway.getNetwork(channelName).getContract(chaincode);
    }

    @Override
    public String submit(LedgerRecord r) throws Exception {
        Proposal proposal = contract.newProposal("registerEvent")
                .addArguments(r.eventId(), r.eventType(), r.recordHash(), r.previousHash(), r.timestamp(), r.authorityId(), r.signature())
                .build();
        Transaction tx = proposal.endorse();
        String txId = tx.getTransactionId();
        SubmittedTransaction submitted = tx.submitAsync();
        Status status = submitted.getStatus();
        if (!status.isSuccessful())
            throw new IllegalStateException("Fabric commit failed with code " + status.getCode() + " for tx " + txId);
        return txId;
    }

    @Override
    public Optional<String> fetchRecordHash(String eventId) throws Exception {
        try {
            byte[] res = contract.evaluateTransaction("getEvent", eventId);
            JsonNode n = om.readTree(res);
            return Optional.of(n.path("recordHash").asText());
        } catch (GatewayException e) {
            return Optional.empty();
        }
    }

    @Override
    public long recordCount() throws Exception {
        byte[] res = contract.evaluateTransaction("getAllEvents");
        JsonNode n = om.readTree(res);
        return n.isArray() ? n.size() : -1;
    }

    @Override
    public String mode() { return "fabric"; }

    @PreDestroy
    void close() throws InterruptedException {
        gateway.close();
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
}
