package com.zerog.neoessentials.moderation;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * A single freeform staff note attached to a player's record — matching ban-management
 * plugins' "staff can write notes and view a player's entire record" feature. Unlike a
 * warning, a note carries no punitive weight of its own; it's just staff-visible context
 * (e.g. "suspected alt of X", "reported for scamming, no evidence yet").
 */
public class NoteEntry {

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("MM/dd/yyyy (HH:mm)").withZone(ZoneId.systemDefault());

    private final String id;
    private final UUID targetId;
    private final String targetName;
    private final UUID authorId; // null for console
    private final String authorName;
    private final String text;
    private final long timestamp;

    public NoteEntry(UUID targetId, String targetName, UUID authorId, String authorName, String text) {
        this.id = UUID.randomUUID().toString();
        this.targetId = targetId;
        this.targetName = targetName;
        this.authorId = authorId;
        this.authorName = authorName;
        this.text = text;
        this.timestamp = Instant.now().toEpochMilli();
    }

    // Used when deserialising from JSON
    public NoteEntry(String id, UUID targetId, String targetName, UUID authorId,
                     String authorName, String text, long timestamp) {
        this.id = id;
        this.targetId = targetId;
        this.targetName = targetName;
        this.authorId = authorId;
        this.authorName = authorName;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getId()         { return id; }
    public UUID   getTargetId()   { return targetId; }
    public String getTargetName() { return targetName; }
    public UUID   getAuthorId()   { return authorId; }
    public String getAuthorName() { return authorName; }
    public String getText()       { return text; }
    public long   getTimestamp()  { return timestamp; }

    public String getFormattedTime() {
        return FORMATTER.format(Instant.ofEpochMilli(timestamp));
    }
}
