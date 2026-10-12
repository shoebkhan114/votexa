package com.votexa.mock;

import com.votexa.address.AddressChangeService;
import com.votexa.address.AddressDtos.AddressChangeDto;
import com.votexa.address.AddressDtos.CreateAddressChangeRequest;
import com.votexa.auth.AuthUser;
import com.votexa.common.Roles;
import com.votexa.verification.DeathService;
import com.votexa.verification.DeathService.DeathCaseDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** SIMULATED government event feeds (mock Aadhaar address event, mock death registry). Not real integrations. */
@RestController
@RequestMapping("/api/mock")
@PreAuthorize(Roles.ERO_ADMIN)
public class MockServicesController {
    public record AddressEvent(@NotBlank String voterCode, @NotBlank String line1, @NotBlank String locality,
                               String district, String state, @NotBlank @Pattern(regexp = "[0-9]{6}") String pincode,
                               String stationCode) {}
    public record DeathEvent(@NotBlank String voterCode) {}

    private final AddressChangeService addressService;
    private final DeathService deathService;

    public MockServicesController(AddressChangeService addressService, DeathService deathService) {
        this.addressService = addressService; this.deathService = deathService;
    }

    @PostMapping("/aadhaar-address-event")
    public AddressChangeDto addressEvent(@Valid @RequestBody AddressEvent e, @AuthenticationPrincipal AuthUser me) {
        return addressService.create(new CreateAddressChangeRequest(e.voterCode(), e.line1(), e.locality(), e.district(),
                e.state(), e.pincode(), e.stationCode(), "Mock Aadhaar address-change event"), "MOCK_AADHAAR_EVENT", me);
    }

    @PostMapping("/death-event")
    public DeathCaseDto deathEvent(@Valid @RequestBody DeathEvent e, @AuthenticationPrincipal AuthUser me) {
        return deathService.open(e.voterCode(), "MOCK_DEATH_REGISTRY", me);
    }
}
