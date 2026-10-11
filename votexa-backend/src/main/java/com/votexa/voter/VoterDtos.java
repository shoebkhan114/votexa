package com.votexa.voter;

import com.votexa.duplicate.DuplicateDtos.DuplicateMatch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.List;

public final class VoterDtos {
    private VoterDtos() {}

    public record AddressDto(String line1, String locality, String district, String state, String pincode) {}

    public record VoterDto(String voterCode, String fullName, LocalDate dob, String gender, String status,
                           String constituencyCode, String constituencyName, String district, String state,
                           String pollingStationCode, String pollingStationName, LocalDate registrationDate,
                           AddressDto address) {}

    public record CheckDto(String name, boolean passed) {}

    public record VerifyRequest(@NotBlank String voterCode) {}

    public record VerifyResult(String voterCode, boolean found, boolean eligible, List<CheckDto> checks, String message) {}

    public record StatusChangeRequest(@NotBlank String newStatus, @NotBlank String reason, String pollingStationCode) {}

    public record CreateVoterRequest(@NotBlank String fullName,
                                     @NotNull @Past LocalDate dob,
                                     @NotBlank @Pattern(regexp = "MALE|FEMALE|OTHER") String gender,
                                     @NotBlank String constituencyCode,
                                     @NotBlank String line1,
                                     @NotBlank String locality,
                                     @NotBlank @Pattern(regexp = "[0-9]{6}") String pincode) {}

    public record CreateVoterResponse(VoterDto voter, List<DuplicateMatch> possibleDuplicates) {}

    public record StatusChangeDto(String oldStatus, String newStatus, String reason, String sourceRef, java.time.Instant at) {}
}
