package com.votexa.address;

import com.votexa.address.AddressDtos.*;
import com.votexa.auth.AuthUser;
import com.votexa.common.Roles;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/address-changes")
public class AddressChangeController {
    private final AddressChangeService service;
    public AddressChangeController(AddressChangeService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAnyRole('CITIZEN','BLO','ERO','SUPER_ADMIN')")
    public AddressChangeDto create(@Valid @RequestBody CreateAddressChangeRequest r, @AuthenticationPrincipal AuthUser me) {
        return service.create(r, "CITIZEN", me);
    }

    @GetMapping("/mine")
    @PreAuthorize(Roles.CITIZEN)
    public List<AddressChangeDto> mine(@AuthenticationPrincipal AuthUser me) { return service.mine(me); }

    @GetMapping
    @PreAuthorize(Roles.BLO_ERO_ADMIN)
    public List<AddressChangeDto> list(@RequestParam(defaultValue = "") String status) { return service.list(status); }

    @PostMapping("/{id}/verify")
    @PreAuthorize(Roles.BLO_ERO_ADMIN)
    public AddressChangeDto verify(@PathVariable Long id, @RequestBody(required = false) DecisionRequest d, @AuthenticationPrincipal AuthUser me) {
        return service.verify(id, d == null ? new DecisionRequest(null, null) : d, me);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(Roles.ERO_ADMIN)
    public AddressChangeDto approve(@PathVariable Long id, @RequestBody(required = false) DecisionRequest d, @AuthenticationPrincipal AuthUser me) {
        return service.approve(id, d == null ? new DecisionRequest(null, null) : d, me);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize(Roles.ERO_ADMIN)
    public AddressChangeDto reject(@PathVariable Long id, @RequestBody DecisionRequest d, @AuthenticationPrincipal AuthUser me) {
        return service.reject(id, d, me);
    }
}
