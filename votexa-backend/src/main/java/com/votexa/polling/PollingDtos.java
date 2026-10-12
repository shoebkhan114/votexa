package com.votexa.polling;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class PollingDtos {
    private PollingDtos() {}

    public record Check(String name, boolean passed) {}
    public record PollingRequest(@NotBlank String stationCode, @NotBlank String voterCode, Boolean identityConfirmed) {}
    public record PollingResult(String result, boolean eligible, boolean authorized, String message,
                                String voterName, List<Check> checks) {}
    public record CloseRequest(@NotNull @PositiveOrZero Integer recordedCount) {}
    public record SessionStats(Long sessionId, String stationCode, String stationName, String status, long eligible,
                               long authenticated, long authorized, long remaining, long anomalies, Integer recordedCount) {}
    public record Reconciliation(Long sessionId, String stationCode, long eligible, long authorized, Integer recorded,
                                 String status, String note) {}
    public record AttemptView(Long id, String voterCodeEntered, String result, String detail, Instant at) {}

    public record SyncEvent(@NotBlank String voterCode, String authorizedAt) {}
    public record SyncRequest(@NotBlank String stationCode, String deviceId, String packageId,
                              @NotNull @Size(max = 5000) List<@Valid SyncEvent> events) {}
    public record SyncResult(int applied, int conflicts, int unknownVoters, int wrongStation) {}
}
