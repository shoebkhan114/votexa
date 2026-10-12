package com.votexa.dashboard;

import com.votexa.common.Roles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize(Roles.STAFF)
public class DashboardController {
    private final JdbcTemplate jdbc;
    public DashboardController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private Map<String, Long> group(String sql) {
        Map<String, Long> m = new LinkedHashMap<>();
        jdbc.query(sql, rs -> { m.put(rs.getString(1), rs.getLong(2)); });
        return m;
    }

    private long one(String sql) {
        Long v = jdbc.queryForObject(sql, Long.class);
        return v == null ? 0 : v;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        Map<String, Long> byStatus = group("SELECT status, count(*) FROM voters GROUP BY status ORDER BY status");
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalVoters", total);
        m.put("votersByStatus", byStatus);
        m.put("pendingVerification", byStatus.getOrDefault("PENDING", 0L) + byStatus.getOrDefault("UNDER_VERIFICATION", 0L));
        m.put("addressChangesByStatus", group("SELECT status, count(*) FROM address_change_requests GROUP BY status ORDER BY status"));
        m.put("duplicateCasesByStatus", group("SELECT status, count(*) FROM duplicate_cases GROUP BY status ORDER BY status"));
        m.put("deathCasesByStatus", group("SELECT status, count(*) FROM death_verification_cases GROUP BY status ORDER BY status"));
        m.put("complaintsByStatus", group("SELECT status, count(*) FROM complaints GROUP BY status ORDER BY status"));
        m.put("registrationsByMonth", group("SELECT to_char(date_trunc('month', registration_date), 'YYYY-MM') AS m, count(*) FROM voters GROUP BY 1 ORDER BY 1"));
        m.put("openAnomalies", one("SELECT count(*) FROM anomaly_alerts WHERE status = 'OPEN'"));
        m.put("auditEvents", one("SELECT count(*) FROM audit_events"));
        m.put("ledgerAnchorsByStatus", group("SELECT status, count(*) FROM blockchain_transactions GROUP BY status ORDER BY status"));
        m.put("pollingActivityByStation", group(
                "SELECT p.code, count(*) FROM voting_authorizations va JOIN polling_sessions s ON s.id = va.polling_session_id " +
                "JOIN polling_stations p ON p.id = s.polling_station_id WHERE va.voting_status = 'VOTED' GROUP BY p.code ORDER BY p.code"));
        return m;
    }
}
