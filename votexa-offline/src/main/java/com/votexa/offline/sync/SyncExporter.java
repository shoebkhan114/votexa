package com.votexa.offline.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.votexa.offline.database.LocalDb;
import com.votexa.offline.database.LocalDb.SyncRow;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Writes the not-yet-synced authorizations as a JSON file the polling officer uploads to the central system. */
public final class SyncExporter {
    private SyncExporter() {}

    public static int export(LocalDb db, Path target) throws Exception {
        List<SyncRow> rows = db.pendingSync();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("stationCode", db.config("station_code"));
        m.put("deviceId", db.device("device_id"));
        m.put("packageId", db.config("package_id"));
        m.put("exportedAt", Instant.now().toString());
        m.put("events", rows.stream().map(r -> Map.of("voterCode", r.voterCode(), "authorizedAt", r.authorizedAt())).toList());
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(Files.newOutputStream(target), m);
        db.markSynced();
        return rows.size();
    }
}
