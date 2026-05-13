package com.zerog.neoessentials.webdashboard.endpoints;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.hologram.*;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
/**
 * REST endpoint for hologram management via the dashboard.
 *
 *  GET  /api/holograms/list            – list all holograms
 *  GET  /api/holograms/stats           – count / world breakdown
 *  GET  /api/holograms/{id}            – get single hologram
 *  POST /api/holograms/create          – create hologram (body = HologramData JSON)
 *  PUT  /api/holograms/{id}            – update hologram
 *  DELETE /api/holograms/{id}          – delete hologram
 *  POST /api/holograms/{id}/spawn      – force re-spawn
 *  POST /api/holograms/{id}/despawn    – despawn
 *  POST /api/holograms/{id}/visible    – toggle visible
 */
public class HologramEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(HologramEndpoint.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            if (path.endsWith("/list")) {
                handleList(exchange);
            } else if (path.endsWith("/stats")) {
                handleStats(exchange);
            } else if (path.endsWith("/create") && "POST".equals(method)) {
                handleCreate(exchange);
            } else {
                String id     = path.substring(path.lastIndexOf('/') + 1);
                String parent = path.contains("/") ? path.substring(0, path.lastIndexOf('/')) : "";
                String action = parent.substring(parent.lastIndexOf('/') + 1);
                if ("spawn".equals(id))   { handleAction(exchange, action, "spawn"); return; }
                if ("despawn".equals(id)) { handleAction(exchange, action, "despawn"); return; }
                if ("visible".equals(id)) { handleAction(exchange, action, "visible"); return; }
                switch (method) {
                    case "GET"    -> handleGet(exchange, id);
                    case "PUT"    -> handleUpdate(exchange, id);
                    case "DELETE" -> handleDelete(exchange, id);
                    default       -> sendJson(exchange, 405, "{\"success\":false,\"error\":\"Method not allowed\"}");
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error in HologramEndpoint", e);
            sendJson(exchange, 500, "{\"success\":false,\"error\":\"" + esc(e.getMessage()) + "\"}");
        }
    }
    // ── Handlers ──────────────────────────────────────────────────────────────
    private void handleList(HttpExchange ex) throws IOException {
        Collection<HologramData> all = HologramManager.getInstance().getAllHolograms();
        JsonArray arr = new JsonArray();
        for (HologramData d : all) arr.add(toJson(d));
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("count", all.size());
        resp.add("holograms", arr);
        sendJson(ex, 200, GSON.toJson(resp));
    }
    private void handleStats(HttpExchange ex) throws IOException {
        Collection<HologramData> all = HologramManager.getInstance().getAllHolograms();
        long visible   = all.stream().filter(d -> d.visible).count();
        long animated  = all.stream().filter(d -> d.lines.stream().anyMatch(l -> !l.frames.isEmpty())).count();
        long shopHolos = all.stream().filter(d -> d.id.startsWith("shop_")).count();
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("total", all.size());
        resp.addProperty("visible", visible);
        resp.addProperty("animated", animated);
        resp.addProperty("shopHolograms", shopHolos);
        sendJson(ex, 200, GSON.toJson(resp));
    }
    private void handleGet(HttpExchange ex, String id) throws IOException {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { sendJson(ex, 404, "{\"success\":false,\"error\":\"Not found\"}"); return; }
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.add("hologram", toJson(data));
        sendJson(ex, 200, GSON.toJson(resp));
    }
    private void handleCreate(HttpExchange ex) throws IOException {
        String body = readBody(ex);
        HologramData data = GSON.fromJson(body, HologramData.class);
        if (data == null || data.id == null || data.id.isEmpty()) {
            sendJson(ex, 400, "{\"success\":false,\"error\":\"id is required\"}");
            return;
        }
        if (data.entityUUIDs == null) data.entityUUIDs = new java.util.ArrayList<>();
        HologramManager.getInstance().registerHologram(data);
        ServerLevel level = findLevel(data.world);
        if (level != null) executeOnMain(() -> HologramRenderer.spawn(data, level));
        sendJson(ex, 200, "{\"success\":true,\"id\":\"" + esc(data.id) + "\"}");
    }
    private void handleUpdate(HttpExchange ex, String id) throws IOException {
        HologramData existing = HologramManager.getInstance().getHologram(id);
        if (existing == null) { sendJson(ex, 404, "{\"success\":false,\"error\":\"Not found\"}"); return; }
        String body = readBody(ex);
        HologramData updated = GSON.fromJson(body, HologramData.class);
        if (updated == null) { sendJson(ex, 400, "{\"success\":false,\"error\":\"Invalid body\"}"); return; }
        updated.id = existing.id; // keep original id
        if (updated.entityUUIDs == null) updated.entityUUIDs = new java.util.ArrayList<>();
        ServerLevel level = findLevel(existing.world);
        if (level != null) executeOnMain(() -> HologramRenderer.despawn(existing, level));
        HologramManager.getInstance().registerHologram(updated);
        ServerLevel newLevel = findLevel(updated.world);
        if (newLevel != null) executeOnMain(() -> HologramRenderer.spawn(updated, newLevel));
        sendJson(ex, 200, "{\"success\":true}");
    }
    private void handleDelete(HttpExchange ex, String id) throws IOException {
        HologramData data = HologramManager.getInstance().removeHologram(id);
        if (data == null) { sendJson(ex, 404, "{\"success\":false,\"error\":\"Not found\"}"); return; }
        ServerLevel level = findLevel(data.world);
        if (level != null) executeOnMain(() -> HologramRenderer.despawn(data, level));
        sendJson(ex, 200, "{\"success\":true}");
    }
    private void handleAction(HttpExchange ex, String id, String action) throws IOException {
        HologramData data = HologramManager.getInstance().getHologram(id);
        if (data == null) { sendJson(ex, 404, "{\"success\":false,\"error\":\"Not found\"}"); return; }
        ServerLevel level = findLevel(data.world);
        switch (action) {
            case "spawn"   -> { if (level != null) executeOnMain(() -> HologramRenderer.spawn(data, level)); }
            case "despawn" -> { if (level != null) executeOnMain(() -> HologramRenderer.despawn(data, level)); }
            case "visible" -> {
                data.visible = !data.visible;
                HologramManager.getInstance().registerHologram(data);
                if (level != null) {
                    if (data.visible) executeOnMain(() -> HologramRenderer.spawn(data, level));
                    else executeOnMain(() -> HologramRenderer.despawn(data, level));
                }
            }
        }
        sendJson(ex, 200, "{\"success\":true}");
    }
    // ── Helpers ───────────────────────────────────────────────────────────────
    private JsonObject toJson(HologramData d) {
        JsonObject o = new JsonObject();
        o.addProperty("id", d.id);
        o.addProperty("world", d.world);
        o.addProperty("x", d.x);
        o.addProperty("y", d.y);
        o.addProperty("z", d.z);
        o.addProperty("visible", d.visible);
        o.addProperty("refreshInterval", d.refreshInterval);
        o.addProperty("lineCount", d.lines.size());
        // Visual appearance
        o.addProperty("scale", d.scale);
        o.addProperty("lineSpacing", d.lineSpacing);
        o.addProperty("textShadow", d.textShadow);
        o.addProperty("textOpacity", d.textOpacity);
        o.addProperty("backgroundColorArgb", d.backgroundColorArgb);
        // Billboard & animations
        o.addProperty("billboardMode", d.billboardMode);
        o.addProperty("spinEnabled", d.spinEnabled);
        o.addProperty("spinSpeedDegrees", d.spinSpeedDegrees);
        o.addProperty("spinAxis", d.spinAxis);
        o.addProperty("hoverEnabled", d.hoverEnabled);
        o.addProperty("hoverAmplitude", d.hoverAmplitude);
        o.addProperty("hoverSpeedDegrees", d.hoverSpeedDegrees);
        JsonArray lines = new JsonArray();
        for (HologramLine ln : d.lines) {
            JsonObject l = new JsonObject();
            l.addProperty("lineId", ln.lineId);
            l.addProperty("text", ln.text);
            l.addProperty("animFrameIntervalTicks", ln.animFrameIntervalTicks);
            JsonArray frames = new JsonArray();
            for (String f : ln.frames) frames.add(f);
            l.add("frames", frames);
            lines.add(l);
        }
        o.add("lines", lines);
        return o;
    }
    private ServerLevel findLevel(String dimensionKey) {
        try {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return null;
            for (ServerLevel level : server.getAllLevels()) {
                if (HologramRenderer.dimensionKey(level).equals(dimensionKey)) return level;
            }
        } catch (Exception ignored) {}
        return null;
    }
    private void executeOnMain(Runnable r) {
        try {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) server.execute(r);
        } catch (Exception ignored) {}
    }
    private String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody();
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
    private void sendJson(HttpExchange ex, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
