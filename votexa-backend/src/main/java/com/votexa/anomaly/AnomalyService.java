package com.votexa.anomaly;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Alerts are "review required" flags: they never accuse a voter/officer or invalidate an election. */
@Service
public class AnomalyService {
    private final AnomalyAlertRepository alerts;
    public AnomalyService(AnomalyAlertRepository alerts) { this.alerts = alerts; }

    @Transactional
    public AnomalyAlert raiseOnce(Long electionId, Long sessionId, String type, String severity, String message) {
        if (alerts.existsByPollingSessionIdAndMessageAndStatus(sessionId, message, "OPEN")) return null;
        AnomalyAlert a = new AnomalyAlert();
        a.setElectionId(electionId);
        a.setPollingSessionId(sessionId);
        a.setAlertType(type);
        a.setSeverity(severity);
        a.setMessage(message.length() > 500 ? message.substring(0, 500) : message);
        return alerts.save(a);
    }
}
