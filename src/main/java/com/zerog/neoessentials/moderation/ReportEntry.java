package com.zerog.neoessentials.moderation;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * A player-submitted report of another player's behavior, reviewable by staff even
 * while they're offline — matching ban-management plugins' "players can report
 * wrongful behaviour even when staff are offline" feature.
 */
public class ReportEntry {

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("MM/dd/yyyy (HH:mm)").withZone(ZoneId.systemDefault());

    public enum Status { PENDING, REVIEWED, DISMISSED }

    private final String id;
    private final UUID reporterId;
    private final String reporterName;
    private final UUID targetId;
    private final String targetName;
    private final String reason;
    private final long timestamp;
    private Status status;
    private UUID reviewedById;
    private String reviewedBy;
    private long reviewedAt;
    private String reviewNotes;

    public ReportEntry(UUID reporterId, String reporterName, UUID targetId, String targetName, String reason) {
        this.id = UUID.randomUUID().toString();
        this.reporterId = reporterId;
        this.reporterName = reporterName;
        this.targetId = targetId;
        this.targetName = targetName;
        this.reason = reason;
        this.timestamp = Instant.now().toEpochMilli();
        this.status = Status.PENDING;
    }

    // Used when deserialising from JSON
    public ReportEntry(String id, UUID reporterId, String reporterName, UUID targetId, String targetName,
                       String reason, long timestamp, Status status, UUID reviewedById, String reviewedBy,
                       long reviewedAt, String reviewNotes) {
        this.id = id;
        this.reporterId = reporterId;
        this.reporterName = reporterName;
        this.targetId = targetId;
        this.targetName = targetName;
        this.reason = reason;
        this.timestamp = timestamp;
        this.status = status;
        this.reviewedById = reviewedById;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.reviewNotes = reviewNotes;
    }

    public String getId()           { return id; }
    public UUID   getReporterId()   { return reporterId; }
    public String getReporterName() { return reporterName; }
    public UUID   getTargetId()     { return targetId; }
    public String getTargetName()   { return targetName; }
    public String getReason()       { return reason; }
    public long   getTimestamp()    { return timestamp; }
    public Status getStatus()       { return status; }
    public UUID   getReviewedById() { return reviewedById; }
    public String getReviewedBy()   { return reviewedBy; }
    public long   getReviewedAt()   { return reviewedAt; }
    public String getReviewNotes()  { return reviewNotes; }

    public void review(Status newStatus, UUID reviewerId, String reviewerName, String notes) {
        this.status = newStatus;
        this.reviewedById = reviewerId;
        this.reviewedBy = reviewerName;
        this.reviewedAt = Instant.now().toEpochMilli();
        this.reviewNotes = notes;
    }

    public String getFormattedTime() {
        return FORMATTER.format(Instant.ofEpochMilli(timestamp));
    }
}
