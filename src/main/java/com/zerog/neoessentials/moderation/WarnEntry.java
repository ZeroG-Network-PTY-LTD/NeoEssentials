package com.zerog.neoessentials.moderation;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Represents a single warning issued to a player.
 */
public class WarnEntry {

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("MM/dd/yyyy (HH:mm)").withZone(ZoneId.systemDefault());

    private final String id;
    private final UUID targetId;
    private final String targetName;
    private final UUID warnedById;   // null for console
    private final String warnedBy;
    private final String reason;
    private final long timestamp;    // epoch millis

    public WarnEntry(UUID targetId, String targetName, UUID warnedById,
                     String warnedBy, String reason) {
        this.id         = UUID.randomUUID().toString();
        this.targetId   = targetId;
        this.targetName = targetName;
        this.warnedById = warnedById;
        this.warnedBy   = warnedBy;
        this.reason     = reason;
        this.timestamp  = Instant.now().toEpochMilli();
    }

    // Used when deserialising from JSON
    public WarnEntry(String id, UUID targetId, String targetName, UUID warnedById,
                     String warnedBy, String reason, long timestamp) {
        this.id         = id;
        this.targetId   = targetId;
        this.targetName = targetName;
        this.warnedById = warnedById;
        this.warnedBy   = warnedBy;
        this.reason     = reason;
        this.timestamp  = timestamp;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getId()          { return id; }
    public UUID   getTargetId()    { return targetId; }
    public String getTargetName()  { return targetName; }
    public UUID   getWarnedById()  { return warnedById; }
    public String getWarnedBy()    { return warnedBy; }
    public String getReason()      { return reason; }
    public long   getTimestamp()   { return timestamp; }

    public String getFormattedTime() {
        return FORMATTER.format(Instant.ofEpochMilli(timestamp));
    }
}

