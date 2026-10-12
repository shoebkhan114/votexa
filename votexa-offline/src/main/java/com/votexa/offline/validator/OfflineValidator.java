package com.votexa.offline.validator;

import com.votexa.offline.database.LocalDb;
import com.votexa.offline.database.LocalDb.VoterRow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Offline eligibility checks against the signed election snapshot. No network is used. */
public final class OfflineValidator {
    public record Check(String name, boolean passed) {}
    public record Result(String result, boolean eligible, boolean authorized, String message, String voterName, List<Check> checks) {}

    private final LocalDb db;
    public OfflineValidator(LocalDb db) { this.db = db; }

    public Result run(String voterCode, boolean identityConfirmed, boolean authorize) throws Exception {
        String code = voterCode.trim();
        List<Check> checks = new ArrayList<>();
        String result = null, message = null;

        boolean pkgValid = Instant.now().isBefore(Instant.parse(db.config("expires_at")));
        checks.add(new Check("Election package valid (not expired)", pkgValid));
        if (!pkgValid) { result = "POLLING_CLOSED"; message = "The election package has expired"; }

        Optional<VoterRow> v = db.find(code);
        checks.add(new Check("Voter is on this station's signed roll", v.isPresent()));
        if (result == null && v.isEmpty()) { result = "NOT_ELIGIBLE"; message = "Voter is not on this polling station's roll"; }

        if (authorize) {
            checks.add(new Check("Identity authenticated by polling officer", identityConfirmed));
            if (result == null && !identityConfirmed) { result = "NOT_ELIGIBLE"; message = "Voter identity has not been confirmed"; }
        }

        boolean notVoted = v.isPresent() && "NOT_VOTED".equals(v.get().votingStatus());
        checks.add(new Check("Voting not already authorized", notVoted));
        if (result == null && !notVoted) { result = "ALREADY_VOTED"; message = "Voting authorization was already completed for this voter"; }

        String name = v.map(VoterRow::name).orElse(null);
        if (result != null) {
            db.logEvent(code, authorize ? "AUTHORIZE" : "VALIDATE", result);
            return new Result(result, false, false, message, name, checks);
        }
        if (!authorize) {
            db.logEvent(code, "VALIDATE", "ELIGIBLE");
            return new Result("ELIGIBLE", true, false, "Voter is eligible", name, checks);
        }
        if (!db.authorize(code)) {   // lost a race / double click
            db.logEvent(code, "AUTHORIZE", "ALREADY_VOTED");
            return new Result("ALREADY_VOTED", false, false, "Voting authorization was already completed for this voter", name, checks);
        }
        db.logEvent(code, "AUTHORIZE", "AUTHORIZED");
        return new Result("AUTHORIZED", true, true, "Voting authorized. Voter marked VOTED.", name, checks);
    }
}
