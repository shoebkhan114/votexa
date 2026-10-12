package com.votexa.election;

import com.votexa.auth.AuthUser;
import com.votexa.common.Roles;
import com.votexa.election.ElectionService.CreateElectionRequest;
import com.votexa.election.ElectionService.ElectionDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/elections")
public class ElectionController {
    public record PackageRequest(@NotBlank String stationCode, @NotBlank String passphrase) {}

    private final ElectionService service;
    private final PackageService packages;

    public ElectionController(ElectionService service, PackageService packages) {
        this.service = service; this.packages = packages;
    }

    @GetMapping
    @PreAuthorize(Roles.STAFF)
    public List<ElectionDto> list() { return service.list(); }

    @PostMapping
    @PreAuthorize(Roles.ADMIN)
    public ElectionDto create(@Valid @RequestBody CreateElectionRequest r, @AuthenticationPrincipal AuthUser me) {
        return service.create(r, me);
    }

    @PostMapping("/{id}/freeze")
    @PreAuthorize(Roles.ERO_ADMIN)
    public ElectionDto freeze(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { return service.freeze(id, me); }

    @PostMapping("/{id}/open-polling")
    @PreAuthorize(Roles.ADMIN)
    public ElectionDto open(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { return service.openPolling(id, me); }

    @PostMapping("/{id}/close-polling")
    @PreAuthorize(Roles.ADMIN)
    public ElectionDto close(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { return service.closePolling(id, me); }

    @PostMapping("/{id}/reset-window")
    @PreAuthorize(Roles.ADMIN)
    public ElectionDto resetWindow(@PathVariable Long id, @RequestParam(defaultValue = "8") int hours, @AuthenticationPrincipal AuthUser me) {
        return service.resetWindow(id, hours, me);
    }

    @PostMapping("/{id}/package")
    @PreAuthorize(Roles.ERO_ADMIN)
    public ResponseEntity<Map<String, Object>> pkg(@PathVariable Long id, @Valid @RequestBody PackageRequest r,
                                                   @AuthenticationPrincipal AuthUser me) {
        Map<String, Object> env = packages.build(id, r.stationCode(), r.passphrase(), me);
        String file = env.get("electionCode") + "-" + env.get("stationCode") + ".vxpkg";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(env);
    }
}
