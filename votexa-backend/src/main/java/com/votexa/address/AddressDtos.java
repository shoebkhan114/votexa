package com.votexa.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

public final class AddressDtos {
    private AddressDtos() {}

    public record CreateAddressChangeRequest(String voterCode, @NotBlank String line1, @NotBlank String locality,
                                             String district, String state,
                                             @NotBlank @Pattern(regexp = "[0-9]{6}") String pincode,
                                             String stationCode, String reason) {}

    public record DecisionRequest(String remarks, String stationCode) {}

    public record AddressChangeDto(Long id, String requestCode, String voterCode, String voterName, String source,
                                   String newLine1, String newLocality, String newDistrict, String newState,
                                   String newPincode, String newConstituencyCode, String newPollingStationCode,
                                   String status, String reason, String decisionRemarks, Instant createdAt) {}
}
