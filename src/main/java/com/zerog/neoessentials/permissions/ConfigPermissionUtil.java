
package com.zerog.neoessentials.permissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConfigPermissionUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigPermissionUtil.class);
    private static Map<UUID, UserPermData> userPerms = new HashMap<>();
    private static boolean loaded = false;

    public static boolean hasPermission(UUID uuid, String permission) {
        ensureLoaded();
        UserPermData data = userPerms.get(uuid);
        if (data != null) {
            return data.permissions.containsKey(permission) && data.permissions.get(permission);
        }
        return false;
    }

    public static String getPrefix(UUID uuid) {
        ensureLoaded();
        UserPermData data = userPerms.get(uuid);
        return data != null ? data.prefix : null;
    }

    public static String getSuffix(UUID uuid) {
        ensureLoaded();
        UserPermData data = userPerms.get(uuid);
        return data != null ? data.suffix : null;
    }

    public static void reload() {
        loaded = false;
        userPerms.clear();
        ensureLoaded();
    }

    private static void ensureLoaded() {
        if (loaded) return;
        try {
            File configFile = com.zerog.neoessentials.util.ResourceUtil.getConfigFile("config.json");
            if (configFile.exists()) {
                String json = new String(Files.readAllBytes(configFile.toPath()));
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                if (obj.has("permissions") && obj.getAsJsonObject("permissions").has("users")) {
                    JsonObject users = obj.getAsJsonObject("permissions").getAsJsonObject("users");
                    for (String key : users.keySet()) {
                        UUID uuid = null;
                        try { uuid = UUID.fromString(key); } catch (Exception ignore) {}
                        if (uuid == null) continue;
                        JsonObject userObj = users.getAsJsonObject(key);
                        UserPermData data = new UserPermData();
                        if (userObj.has("prefix")) data.prefix = userObj.get("prefix").getAsString();
                        if (userObj.has("suffix")) data.suffix = userObj.get("suffix").getAsString();
                        if (userObj.has("permissions")) {
                            JsonObject perms = userObj.getAsJsonObject("permissions");
                            for (String perm : perms.keySet()) {
                                data.permissions.put(perm, perms.get(perm).getAsBoolean());
                            }
                        }
                        userPerms.put(uuid, data);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load permission configuration", e);
        }
        loaded = true;
    }

    private static class UserPermData {
        String prefix = null;
        String suffix = null;
        Map<String, Boolean> permissions = new HashMap<>();
    }
}