package com.zerog.neoessentials.moderation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages player-submitted reports, reviewable by staff even while offline.
 */
public class ReportManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportManager.class);
    private static final ReportManager INSTANCE = new ReportManager();
    public static ReportManager getInstance() { return INSTANCE; }

    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final List<ReportEntry> reports = new CopyOnWriteArrayList<>();
    private final File reportsFile;

    private ReportManager() {
        File dir = new File(com.zerog.neoessentials.util.ResourceUtil.DATA_DIR + "moderation");
        if (!dir.exists()) dir.mkdirs();
        reportsFile = new File(dir, "reports.json");
        load();
    }

    public ReportEntry addReport(UUID reporterId, String reporterName, UUID targetId, String targetName, String reason) {
        ReportEntry entry = new ReportEntry(reporterId, reporterName, targetId, targetName, reason);
        reports.add(entry);
        save();
        LOGGER.info("[Report] {} reported {}: {}", reporterName, targetName, reason);
        return entry;
    }

    /** All reports with PENDING status, newest first — the staff review queue. */
    public List<ReportEntry> getPendingReports() {
        return reports.stream()
            .filter(r -> r.getStatus() == ReportEntry.Status.PENDING)
            .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
            .toList();
    }

    /** Every report ever filed against a player, newest first. */
    public List<ReportEntry> getReportsAgainst(UUID targetId) {
        return reports.stream()
            .filter(r -> targetId.equals(r.getTargetId()))
            .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
            .toList();
    }

    /** Every report ever filed by a player, newest first. */
    public List<ReportEntry> getReportsBy(UUID reporterId) {
        return reports.stream()
            .filter(r -> reporterId.equals(r.getReporterId()))
            .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
            .toList();
    }

    /** Every report ever filed, newest first — for dashboard use. */
    public List<ReportEntry> getAllReports() {
        List<ReportEntry> all = new ArrayList<>(reports);
        all.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        return all;
    }

    public ReportEntry findById(String reportId) {
        return reports.stream()
            .filter(r -> r.getId().startsWith(reportId) || r.getId().equals(reportId))
            .findFirst()
            .orElse(null);
    }

    /** Marks a report reviewed (accepted/actioned) or dismissed, with staff attribution. */
    public boolean reviewReport(String reportId, ReportEntry.Status newStatus, UUID reviewerId, String reviewerName, String notes) {
        ReportEntry report = findById(reportId);
        if (report == null) return false;
        report.review(newStatus, reviewerId, reviewerName, notes);
        save();
        return true;
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void save() {
        try (FileWriter fw = new FileWriter(reportsFile)) {
            JsonArray root = new JsonArray();
            for (ReportEntry e : reports) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", e.getId());
                obj.addProperty("reporterId", e.getReporterId() != null ? e.getReporterId().toString() : "");
                obj.addProperty("reporterName", e.getReporterName());
                obj.addProperty("targetId", e.getTargetId().toString());
                obj.addProperty("targetName", e.getTargetName());
                obj.addProperty("reason", e.getReason());
                obj.addProperty("timestamp", e.getTimestamp());
                obj.addProperty("status", e.getStatus().name());
                obj.addProperty("reviewedById", e.getReviewedById() != null ? e.getReviewedById().toString() : "");
                obj.addProperty("reviewedBy", e.getReviewedBy());
                obj.addProperty("reviewedAt", e.getReviewedAt());
                obj.addProperty("reviewNotes", e.getReviewNotes());
                root.add(obj);
            }
            gson.toJson(root, fw);
        } catch (Exception ex) {
            LOGGER.error("Failed to save reports.json: {}", ex.getMessage());
        }
    }

    private void load() {
        reports.clear();
        if (reportsFile == null || !reportsFile.exists()) return;
        try (FileReader fr = new FileReader(reportsFile)) {
            JsonArray root = gson.fromJson(fr, JsonArray.class);
            if (root == null) return;
            for (JsonElement el : root) {
                JsonObject obj = el.getAsJsonObject();
                String id = obj.get("id").getAsString();
                String reporterIdStr = obj.has("reporterId") ? obj.get("reporterId").getAsString() : "";
                UUID reporterId = reporterIdStr.isEmpty() ? null : UUID.fromString(reporterIdStr);
                String reporterName = obj.get("reporterName").getAsString();
                UUID targetId = UUID.fromString(obj.get("targetId").getAsString());
                String targetName = obj.get("targetName").getAsString();
                String reason = obj.get("reason").getAsString();
                long timestamp = obj.get("timestamp").getAsLong();
                ReportEntry.Status status = obj.has("status")
                    ? ReportEntry.Status.valueOf(obj.get("status").getAsString())
                    : ReportEntry.Status.PENDING;
                String reviewedByIdStr = obj.has("reviewedById") ? obj.get("reviewedById").getAsString() : "";
                UUID reviewedById = reviewedByIdStr.isEmpty() ? null : UUID.fromString(reviewedByIdStr);
                String reviewedBy = obj.has("reviewedBy") && !obj.get("reviewedBy").isJsonNull() ? obj.get("reviewedBy").getAsString() : null;
                long reviewedAt = obj.has("reviewedAt") ? obj.get("reviewedAt").getAsLong() : 0;
                String reviewNotes = obj.has("reviewNotes") && !obj.get("reviewNotes").isJsonNull() ? obj.get("reviewNotes").getAsString() : null;

                reports.add(new ReportEntry(id, reporterId, reporterName, targetId, targetName, reason,
                    timestamp, status, reviewedById, reviewedBy, reviewedAt, reviewNotes));
            }
            LOGGER.info("ReportManager: loaded {} report(s).", reports.size());
        } catch (Exception ex) {
            LOGGER.error("Failed to load reports.json: {}", ex.getMessage());
        }
    }
}
