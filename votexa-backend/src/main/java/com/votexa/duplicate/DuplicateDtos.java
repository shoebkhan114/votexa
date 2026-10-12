package com.votexa.duplicate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.time.LocalDate;

public final class DuplicateDtos {
    private DuplicateDtos() {}
    public record VoterRef(String voterCode, String fullName, LocalDate dob, String status) {}
    public record DuplicateMatch(String voterCode, String fullName, LocalDate dob, double score) {}
    public record DuplicateCheckRequest(@NotBlank String fullName, @NotNull LocalDate dob, String address) {}
    public record DuplicateCaseDto(Long id, String caseCode, VoterRef voterA, VoterRef voterB, double similarity,
                                   String matchedAttributes, String status, String remarks, Instant detectedAt) {}
    public record ReviewRequest(@NotBlank @Pattern(regexp = "CONFIRM|REJECT") String decision, String remarks) {}
    public record ScanResult(int pairsChecked, int casesCreated) {}
}
