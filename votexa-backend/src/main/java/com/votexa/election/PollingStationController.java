package com.votexa.election;

import com.votexa.common.ApiException;
import com.votexa.common.Roles;
import com.votexa.voter.VoterRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/polling-stations")
@PreAuthorize(Roles.STAFF)
public class PollingStationController {
    public record StationDto(String code, String name, String constituencyCode, String constituencyName,
                             String area, Integer capacity, long activeVoters) {}

    private final PollingStationRepository stations;
    private final VoterRepository voters;

    public PollingStationController(PollingStationRepository stations, VoterRepository voters) {
        this.stations = stations; this.voters = voters;
    }

    private StationDto dto(PollingStation s) {
        return new StationDto(s.getCode(), s.getName(), s.getConstituency().getCode(), s.getConstituency().getName(),
                s.getArea(), s.getCapacity(), voters.countByPollingStationIdAndStatus(s.getId(), "ACTIVE"));
    }

    @GetMapping
    public List<StationDto> list() { return stations.findAll().stream().map(this::dto).toList(); }

    @GetMapping("/{code}")
    public StationDto get(@PathVariable String code) {
        return stations.findByCode(code).map(this::dto)
                .orElseThrow(() -> ApiException.notFound("Polling station not found: " + code));
    }
}
