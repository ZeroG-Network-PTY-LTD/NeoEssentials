package com.zerog.neoessentials.webdashboard.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.nbt.CompoundTag;
import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Handler for /api/playerdata endpoint
 * Returns all player .dat NBT data as JSON keyed by UUID
 */
public class PlayerDataHandler implements HttpHandler {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, createErrorResponse("Method not allowed"));
            return;
        }

        String path = exchange.getRequestURI().getPath();
        // Robust path parsing for /api/playerdata/{uuid} (handles trailing slashes)
        String[] parts = path.replaceAll("^/+|/+$", "").split("/");
        if (parts.length >= 3 && parts[0].equals("api") && parts[1].equals("playerdata")) {
            String uuid = parts[2];
            // Search all world folders under the current working directory for playerdata/{uuid}.dat
            File cwd = new File(System.getProperty("user.dir"));
            File foundFile = null;
            String foundWorld = null;
            if (cwd.exists() && cwd.isDirectory()) {
                File[] worldDirs = cwd.listFiles(File::isDirectory);
                if (worldDirs != null) {
                    for (File worldDir : worldDirs) {
                        File playerdataDir = new File(worldDir, "playerdata");
                        File candidate = new File(playerdataDir, uuid + ".dat");
                        if (candidate.exists()) {
                            foundFile = candidate;
                            foundWorld = worldDir.getName();
                            break;
                        }
                    }
                }
            }
            if (foundFile == null) {
                sendJsonResponse(exchange, 404, createErrorResponse("Player data file not found for uuid: " + uuid + " in any world folder under server root ('" + cwd.getAbsolutePath() + "')"));
                return;
            }
            try (FileInputStream fis = new FileInputStream(foundFile)) {
                CompoundTag tag = null;
                try {
                    tag = net.minecraft.nbt.NbtIo.readCompressed(fis, net.minecraft.nbt.NbtAccounter.create(Long.MAX_VALUE));
                } catch (Exception e) {
                    sendJsonResponse(exchange, 500, createErrorResponse("NBT parse error for file: " + foundFile.getAbsolutePath() + ", reason: " + e));
                    return;
                }
                if (tag != null) {
                    JsonObject result = nbtToJson(tag);
                    result.addProperty("_world", foundWorld); // Add world name for context
                    sendJsonResponse(exchange, 200, result);
                } else {
                    sendJsonResponse(exchange, 500, createErrorResponse("NBT tag is null for file: " + foundFile.getAbsolutePath()));
                }
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, createErrorResponse("Error reading file: " + foundFile.getAbsolutePath() + ", reason: " + e));
            }
            return;
        }

        // Default: return all player data
        try {
            JsonObject response = getAllPlayerNBTData();
            sendJsonResponse(exchange, 200, response);
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, createErrorResponse("Internal error: " + e.getMessage()));
        }
    }

    @SuppressWarnings("unused") // Reserved for future single-player data retrieval API
    private JsonObject getPlayerNBTData(String uuid) {
        File file = new File("run/world/playerdata/" + uuid + ".dat");
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                CompoundTag tag = null;
                try {
                    tag = net.minecraft.nbt.NbtIo.readCompressed(fis, net.minecraft.nbt.NbtAccounter.create(Long.MAX_VALUE));
                } catch (Exception e) {
                    System.err.println("[PlayerDataHandler] Failed to parse NBT for file: " + file.getName() + " - " + e);
                    return null;
                }
                if (tag != null) {
                    return nbtToJson(tag);
                }
            } catch (Exception e) {
                System.err.println("[PlayerDataHandler] Error reading file: " + file.getName() + " - " + e);
            }
        }
        return null;
    }

    private JsonObject getAllPlayerNBTData() {
        JsonObject allPlayers = new JsonObject();
        Map<String, String> uuidToName = new HashMap<>();
        // Load usercache.json
        try {
            File usercacheFile = new File("run/usercache.json");
            if (usercacheFile.exists()) {
                String usercacheContent = java.nio.file.Files.readString(usercacheFile.toPath());
                com.google.gson.JsonArray arr = com.google.gson.JsonParser.parseString(usercacheContent).getAsJsonArray();
                for (com.google.gson.JsonElement el : arr) {
                    com.google.gson.JsonObject obj = el.getAsJsonObject();
                    String uuid = obj.get("uuid").getAsString();
                    String name = obj.get("name").getAsString();
                    uuidToName.put(uuid, name);
                }
            }
        } catch (Exception e) {
            System.err.println("[PlayerDataHandler] Failed to load usercache.json: " + e);
        }
        // Load usernamecache.json
        try {
            File usernamecacheFile = new File("run/usernamecache.json");
            if (usernamecacheFile.exists()) {
                String usernamecacheContent = java.nio.file.Files.readString(usernamecacheFile.toPath());
                com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(usernamecacheContent).getAsJsonObject();
                for (String uuid : obj.keySet()) {
                    String name = obj.get(uuid).getAsString();
                    uuidToName.put(uuid, name);
                }
            }
        } catch (Exception e) {
            System.err.println("[PlayerDataHandler] Failed to load usernamecache.json: " + e);
        }
        // Scan all playerdata .dat files as before
        Set<String> allUuids = new HashSet<>(uuidToName.keySet());
        File cwd = new File(System.getProperty("user.dir"));
        if (cwd.exists() && cwd.isDirectory()) {
            File[] worldDirs = cwd.listFiles(File::isDirectory);
            if (worldDirs != null) {
                for (File worldDir : worldDirs) {
                    File playerdataDir = new File(worldDir, "playerdata");
                    if (playerdataDir.exists() && playerdataDir.isDirectory()) {
                        File[] files = playerdataDir.listFiles((dir, name) -> name.endsWith(".dat"));
                        if (files != null) {
                            for (File file : files) {
                                String uuid = file.getName().replace(".dat", "");
                                allUuids.add(uuid);
                                try (FileInputStream fis = new FileInputStream(file)) {
                                    CompoundTag tag = null;
                                    try {
                                        tag = net.minecraft.nbt.NbtIo.readCompressed(fis, net.minecraft.nbt.NbtAccounter.create(Long.MAX_VALUE));
                                    } catch (Exception e) {
                                        System.err.println("[PlayerDataHandler] Failed to parse NBT for file: " + file.getName() + " - " + e);
                                        continue;
                                    }
                                    if (tag != null) {
                                        JsonObject playerJson = nbtToJson(tag);
                                        playerJson.addProperty("_world", worldDir.getName());
                                        String playerName = uuidToName.getOrDefault(uuid, uuid);
                                        if (tag.contains("Name")) {
                                            playerName = tag.getString("Name");
                                        } else if (tag.contains("name")) {
                                            playerName = tag.getString("name");
                                        } else if (playerJson.has("Name")) {
                                            playerName = playerJson.get("Name").getAsString();
                                        } else if (playerJson.has("name")) {
                                            playerName = playerJson.get("name").getAsString();
                                        }
                                        JsonObject entry = new JsonObject();
                                        entry.addProperty("uuid", uuid);
                                        entry.addProperty("name", playerName != null ? playerName : uuid);
                                        entry.add("data", playerJson);
                                        allPlayers.add(uuid, entry);
                                    } else {
                                        System.err.println("[PlayerDataHandler] NBT tag is null for file: " + file.getName());
                                    }
                                } catch (Exception e) {
                                    System.err.println("[PlayerDataHandler] Error reading file: " + file.getName() + " - " + e);
                                }
                            }
                        }
                    }
                }
            }
        }
        // Add placeholder entries for players missing NBT data
        for (String uuid : allUuids) {
            if (!allPlayers.has(uuid)) {
                JsonObject entry = new JsonObject();
                entry.addProperty("uuid", uuid);
                entry.addProperty("name", uuidToName.getOrDefault(uuid, uuid));
                entry.add("data", new JsonObject());
                allPlayers.add(uuid, entry);
            }
        }
        return allPlayers;
    }

    // Converts CompoundTag to a JsonObject (simple implementation)
    private JsonObject nbtToJson(CompoundTag tag) {
        JsonObject obj = new JsonObject();
        for (String key : tag.getAllKeys()) {
            if (tag.contains(key)) {
                net.minecraft.nbt.Tag nbtValue = tag.get(key);
                if (nbtValue == null) {
                    obj.addProperty(key, (String) null);
                    continue;
                }
                int typeId = nbtValue.getId();
                switch (typeId) {
                    case net.minecraft.nbt.Tag.TAG_INT:
                        obj.addProperty(key, tag.getInt(key));
                        break;
                    case net.minecraft.nbt.Tag.TAG_FLOAT:
                        obj.addProperty(key, tag.getFloat(key));
                        break;
                    case net.minecraft.nbt.Tag.TAG_STRING:
                        obj.addProperty(key, tag.getString(key));
                        break;
                    case net.minecraft.nbt.Tag.TAG_DOUBLE:
                        obj.addProperty(key, tag.getDouble(key));
                        break;
                    case net.minecraft.nbt.Tag.TAG_LONG:
                        obj.addProperty(key, tag.getLong(key));
                        break;
                    case net.minecraft.nbt.Tag.TAG_BYTE:
                        obj.addProperty(key, tag.getByte(key));
                        break;
                    case net.minecraft.nbt.Tag.TAG_SHORT:
                        obj.addProperty(key, tag.getShort(key));
                        break;
                    default:
                        // Null-safe toString for NBT values
                        try {
                            obj.addProperty(key, nbtValue != null ? nbtValue.toString() : "null");
                        } catch (Exception e) {
                            obj.addProperty(key, "null");
                        }
                        break;
                }
            }
        }
        return obj;
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, JsonObject json) throws IOException {
        byte[] response = GSON.toJson(json).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private JsonObject createErrorResponse(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        error.addProperty("timestamp", System.currentTimeMillis());
        return error;
    }
}
