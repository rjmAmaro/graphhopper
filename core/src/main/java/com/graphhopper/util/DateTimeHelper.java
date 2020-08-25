package com.graphhopper.util;

import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.TimeZoneStorage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DateTimeHelper {
    private final TimeZoneStorage timeZoneStorage;

    public DateTimeHelper(GraphHopperStorage graph) {
        this.timeZoneStorage = graph.getTimeZoneStorage();
    }

    public ZonedDateTime getZonedDateTime(EdgeIteratorState iter, long time) {
        int node = iter.getBaseNode();

        String timeZoneId = timeZoneStorage.getValue(node);
        ZoneId edgeZoneId = ZoneId.of(timeZoneId);
        Instant edgeEnterTime = Instant.ofEpochMilli(time);

        return ZonedDateTime.ofInstant(edgeEnterTime, edgeZoneId);
    }
}
