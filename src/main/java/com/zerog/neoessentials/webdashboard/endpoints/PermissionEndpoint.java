package com.zerog.neoessentials.webdashboard.endpoints;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.permissions.PermissionManager;
import com.zerog.neoessentials.permissions.PermissionStorage;
import com.zerog.neoessentials.permissions.PermissionGroup;
import com.zerog.neoessentials.permissions.PermissionUser;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Permission management API endpoint for web dashboard
 * Provides comprehensive permission, group, and user management
 */
public class PermissionEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionEndpoint.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final MinecraftServer server;

    public PermissionEndpoint(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath().replace("/api/permissions", "");

        try {
            JsonObject response;

            switch (method) {
                case "GET":
                    response = handleGet(path);
                    break;
                case "POST":
                    response = handlePost(path, exchange);
                    break;
                case "PUT":
                    response = handlePut(path, exchange);
                    break;
                case "DELETE":
                    response = handleDelete(path, exchange);
                    break;
                default:
                    response = createErrorResponse("Method not allowed");
                    sendResponse(exchange, 405, response);
                    return;
            }

            sendResponse(exchange, 200, response);
        } catch (Exception e) {
            LOGGER.error("Error handling permission endpoint request", e);
            JsonObject error = createErrorResponse("Internal server error: " + e.getMessage());
            sendResponse(exchange, 500, error);
        }
    }

    private JsonObject handleGet(String path) {
        if (path.equals("/overview") || path.equals("")) {
            return getPermissionOverview();
        } else if (path.equals("/groups")) {
            return getAllGroups();
        } else if (path.startsWith("/group/") && path.endsWith("/context")) {
            String groupName = path.substring(7, path.length() - 8);
            return listGroupContextPerms(groupName);
        } else if (path.startsWith("/group/") && path.endsWith("/temp")) {
            String groupName = path.substring(7, path.length() - 5);
            return listGroupTempPerms(groupName);
        } else if (path.startsWith("/group/")) {
            String groupName = path.substring(7);
            return getGroup(groupName);
        } else if (path.equals("/users")) {
            return getAllUsers();
        } else if (path.startsWith("/user/") && path.endsWith("/context")) {
            String username = path.substring(6, path.length() - 8);
            return listUserContextPerms(username);
        } else if (path.startsWith("/user/") && path.endsWith("/temp")) {
            String username = path.substring(6, path.length() - 5);
            return listUserTempPerms(username);
        } else if (path.startsWith("/user/")) {
            String username = path.substring(6);
            return getUser(username);
        } else if (path.equals("/permissions/all")) {
            return getAllAvailablePermissions();
        } else if (path.equals("/system/status")) {
            return getSystemStatus();
        } else if (path.equals("/aliases")) {
            return listAliases();
        }

        return createErrorResponse("Unknown endpoint: " + path);
    }

    private JsonObject handlePost(String path, HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject data = body.isBlank() ? new JsonObject() : JsonParser.parseString(body).getAsJsonObject();

        if (path.equals("/reload")) {
            return reloadPermissions();
        } else if (path.equals("/group/create")) {
            return createGroup(data);
        } else if (path.startsWith("/group/") && path.endsWith("/permission/add")) {
            String groupName = extractGroupName(path, "/permission/add");
            return addPermissionToGroup(groupName, data);
        } else if (path.startsWith("/group/") && path.endsWith("/context")) {
            String groupName = extractGroupName(path, "/context");
            return addGroupContextPerm(groupName, data);
        } else if (path.startsWith("/group/") && path.endsWith("/temp")) {
            String groupName = extractGroupName(path, "/temp");
            return addGroupTempPerm(groupName, data);
        } else if (path.startsWith("/user/") && path.endsWith("/group/set")) {
            String username = extractUsername(path, "/group/set");
            return setUserGroup(username, data);
        } else if (path.startsWith("/user/") && path.endsWith("/permission/add")) {
            String username = extractUsername(path, "/permission/add");
            return addPermissionToUser(username, data);
        } else if (path.startsWith("/user/") && path.endsWith("/context")) {
            String username = extractUsername(path, "/context");
            return addUserContextPerm(username, data);
        } else if (path.startsWith("/user/") && path.endsWith("/temp")) {
            String username = extractUsername(path, "/temp");
            return addUserTempPerm(username, data);
        } else if (path.equals("/aliases")) {
            return addAlias(data);
        }

        return createErrorResponse("Unknown POST endpoint: " + path);
    }

    private JsonObject handlePut(String path, HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject data = JsonParser.parseString(body).getAsJsonObject();

        if (path.startsWith("/group/") && path.endsWith("/update")) {
            String groupName = extractGroupName(path, "/update");
            return updateGroup(groupName, data);
        } else if (path.startsWith("/user/") && path.endsWith("/update")) {
            String username = extractUsername(path, "/update");
            return updateUser(username, data);
        }

        return createErrorResponse("Unknown PUT endpoint: " + path);
    }

    private JsonObject handleDelete(String path, HttpExchange exchange) throws IOException {
        // /group/{name}/context  — body contains {contextKey, node}
        if (path.startsWith("/group/") && path.endsWith("/context")) {
            String groupName = extractGroupName(path, "/context");
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject data = body.isBlank() ? new JsonObject() : JsonParser.parseString(body).getAsJsonObject();
            return removeGroupContextPerm(groupName, data);
        }
        // /group/{name}/temp/{node}
        if (path.startsWith("/group/") && path.contains("/temp/")) {
            int idx = path.lastIndexOf("/temp/");
            String groupName = path.substring(7, idx);
            String node = path.substring(idx + 6);
            return removeGroupTempPerm(groupName, node);
        }
        // /user/{name}/context — body contains {contextKey, node}
        if (path.startsWith("/user/") && path.endsWith("/context")) {
            String username = extractUsername(path, "/context");
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject data = body.isBlank() ? new JsonObject() : JsonParser.parseString(body).getAsJsonObject();
            return removeUserContextPerm(username, data);
        }
        // /user/{name}/temp/{node}
        if (path.startsWith("/user/") && path.contains("/temp/")) {
            int idx = path.lastIndexOf("/temp/");
            String username = path.substring(6, idx);
            String node = path.substring(idx + 6);
            return removeUserTempPerm(username, node);
        }
        // /aliases/{alias}
        if (path.startsWith("/aliases/")) {
            String alias = path.substring(9);
            return removeAlias(alias);
        }
        if (path.startsWith("/group/") && !path.contains("/permission/")) {
            String groupName = path.substring(7);
            return deleteGroup(groupName);
        } else if (path.startsWith("/group/") && path.contains("/permission/remove/")) {
            String[] parts = path.split("/");
            String groupName = parts[2];
            String permission = parts[parts.length - 1];
            return removePermissionFromGroup(groupName, permission);
        } else if (path.startsWith("/user/") && path.contains("/permission/remove/")) {
            String[] parts = path.split("/");
            String username = parts[2];
            String permission = parts[parts.length - 1];
            return removePermissionFromUser(username, permission);
        }

        return createErrorResponse("Unknown DELETE endpoint: " + path);
    }

    // ========== GET Methods ==========

    private JsonObject getPermissionOverview() {
        JsonObject overview = new JsonObject();
        PermissionManager manager = PermissionAPI.getManager();

        if (manager != null) {
            Collection<PermissionGroup> groups = manager.getGroups();
            overview.addProperty("totalGroups", groups.size());
            overview.addProperty("totalUsers", getOnlineUserCount());
            overview.addProperty("usingExternal", false);
            overview.addProperty("systemType", "Internal");

            // Group statistics
            JsonArray groupStats = new JsonArray();
            for (PermissionGroup group : groups) {
                JsonObject stat = new JsonObject();
                stat.addProperty("name", group.getName());
                stat.addProperty("permissionCount", group.getPermissions().size());
                stat.addProperty("isDefault", group.getName().equalsIgnoreCase(manager.getDefaultGroup()));
                groupStats.add(stat);
            }
            overview.add("groupStats", groupStats);
        } else {
            overview.addProperty("totalGroups", 0);
            overview.addProperty("totalUsers", 0);
            overview.addProperty("usingExternal", true);
            overview.addProperty("systemType", "External (LuckPerms/FTB Ranks)");
        }

        overview.addProperty("success", true);
        return overview;
    }

    private JsonObject getAllGroups() {
        JsonObject response = new JsonObject();
        PermissionManager manager = PermissionAPI.getManager();

        if (manager != null) {
            JsonArray groups = new JsonArray();
            for (PermissionGroup group : manager.getGroups()) {
                JsonObject groupObj = new JsonObject();
                groupObj.addProperty("name", group.getName());
                groupObj.addProperty("prefix", group.getPrefix());
                groupObj.addProperty("suffix", group.getSuffix());
                groupObj.addProperty("weight", 0); // PermissionGroup doesn't have weight yet
                groupObj.addProperty("isDefault", group.getName().equalsIgnoreCase(manager.getDefaultGroup()));
                groupObj.addProperty("permissionCount", group.getPermissions().size());

                JsonArray permissions = new JsonArray();
                group.getPermissions().forEach(permissions::add);
                groupObj.add("permissions", permissions);

                groups.add(groupObj);
            }
            response.add("groups", groups);
            response.addProperty("success", true);
        } else {
            response.addProperty("success", false);
            response.addProperty("message", "Using external permission system");
        }

        return response;
    }

    private JsonObject getGroup(String groupName) {
        JsonObject response = new JsonObject();
        PermissionManager manager = PermissionAPI.getManager();

        if (manager != null) {
            PermissionGroup group = manager.getGroup(groupName);
            if (group != null) {
                response.addProperty("name", groupName);
                response.addProperty("prefix", group.getPrefix());
                response.addProperty("suffix", group.getSuffix());
                response.addProperty("weight", 0); // PermissionGroup doesn't have weight yet
                response.addProperty("isDefault", group.getName().equalsIgnoreCase(manager.getDefaultGroup()));

                JsonArray permissions = new JsonArray();
                group.getPermissions().forEach(permissions::add);
                response.add("permissions", permissions);

                response.addProperty("success", true);
            } else {
                response.addProperty("success", false);
                response.addProperty("message", "Group not found: " + groupName);
            }
        } else {
            response.addProperty("success", false);
            response.addProperty("message", "Using external permission system");
        }

        return response;
    }

    private JsonObject getAllUsers() {
        JsonObject response = new JsonObject();
        JsonArray users = new JsonArray();

        // Get all online players
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            JsonObject userObj = new JsonObject();
            userObj.addProperty("username", player.getName().getString());
            userObj.addProperty("uuid", player.getUUID().toString());
            userObj.addProperty("online", true);

            // Get player's group
            PermissionManager manager = PermissionAPI.getManager();
            String group = "default";
            if (manager != null) {
                PermissionUser user = manager.getUser(player.getUUID());
                if (user != null) {
                    group = user.getGroup();
                }
            }
            userObj.addProperty("group", group);

            // Get player's prefix and suffix
            String prefix = PermissionAPI.getPrefix(player.getUUID());
            String suffix = PermissionAPI.getSuffix(player.getUUID());
            userObj.addProperty("prefix", prefix != null ? prefix : "");
            userObj.addProperty("suffix", suffix != null ? suffix : "");

            // Get player permissions if using internal system
            if (manager != null) {
                PermissionUser user = manager.getUser(player.getUUID());
                if (user != null) {
                    Set<String> permissions = user.getPermissions();
                    JsonArray permsArray = new JsonArray();
                    permissions.forEach(permsArray::add);
                    userObj.add("permissions", permsArray);
                }
            }

            users.add(userObj);
        }

        response.add("users", users);
        response.addProperty("count", users.size());
        response.addProperty("success", true);
        return response;
    }

    private JsonObject getUser(String username) {
        JsonObject response = new JsonObject();
        ServerPlayer player = server.getPlayerList().getPlayerByName(username);

        if (player != null) {
            response.addProperty("username", player.getName().getString());
            response.addProperty("uuid", player.getUUID().toString());
            response.addProperty("online", true);

            PermissionManager manager = PermissionAPI.getManager();
            String group = "default";
            if (manager != null) {
                PermissionUser user = manager.getUser(player.getUUID());
                if (user != null) {
                    group = user.getGroup();
                }
            }
            response.addProperty("group", group);

            String prefix = PermissionAPI.getPrefix(player.getUUID());
            String suffix = PermissionAPI.getSuffix(player.getUUID());
            response.addProperty("prefix", prefix != null ? prefix : "");
            response.addProperty("suffix", suffix != null ? suffix : "");

            if (manager != null) {
                PermissionUser user = manager.getUser(player.getUUID());
                if (user != null) {
                    Set<String> permissions = user.getPermissions();
                    JsonArray permsArray = new JsonArray();
                    permissions.forEach(permsArray::add);
                    response.add("permissions", permsArray);
                }
            }

            response.addProperty("success", true);
        } else {
            response.addProperty("success", false);
            response.addProperty("message", "Player not found or offline: " + username);
        }

        return response;
    }

    private JsonObject getAllAvailablePermissions() {
        JsonObject response = new JsonObject();
        JsonArray categories = new JsonArray();

        // Core permissions
        JsonObject core = new JsonObject();
        core.addProperty("category", "Core");
        JsonArray corePerms = new JsonArray();
        corePerms.add("neoessentials.use");
        corePerms.add("neoessentials.admin");
        corePerms.add("neoessentials.reload");
        corePerms.add("neoessentials.info");
        corePerms.add("neoessentials.debug");
        core.add("permissions", corePerms);
        categories.add(core);

        // Economy permissions
        JsonObject economy = new JsonObject();
        economy.addProperty("category", "Economy");
        JsonArray economyPerms = new JsonArray();
        economyPerms.add("neoessentials.economy.*");
        economyPerms.add("neoessentials.economy.balance");
        economyPerms.add("neoessentials.economy.pay");
        economyPerms.add("neoessentials.economy.admin");
        economy.add("permissions", economyPerms);
        categories.add(economy);

        // Teleportation permissions
        JsonObject teleport = new JsonObject();
        teleport.addProperty("category", "Teleportation");
        JsonArray teleportPerms = new JsonArray();
        teleportPerms.add("neoessentials.teleport.*");
        teleportPerms.add("neoessentials.teleport.home");
        teleportPerms.add("neoessentials.teleport.warp");
        teleportPerms.add("neoessentials.teleport.spawn");
        teleportPerms.add("neoessentials.teleport.back");
        teleportPerms.add("neoessentials.teleport.admin");
        teleport.add("permissions", teleportPerms);
        categories.add(teleport);

        // Chat permissions
        JsonObject chat = new JsonObject();
        chat.addProperty("category", "Chat");
        JsonArray chatPerms = new JsonArray();
        chatPerms.add("neoessentials.chat.*");
        chatPerms.add("neoessentials.chat.msg");
        chatPerms.add("neoessentials.chat.color");
        chatPerms.add("neoessentials.chat.format");
        chatPerms.add("neoessentials.chat.staff");
        chat.add("permissions", chatPerms);
        categories.add(chat);

        // Kits permissions
        JsonObject kits = new JsonObject();
        kits.addProperty("category", "Kits");
        JsonArray kitsPerms = new JsonArray();
        kitsPerms.add("neoessentials.kits.*");
        kitsPerms.add("neoessentials.kits.use");
        kitsPerms.add("neoessentials.kits.admin");
        kits.add("permissions", kitsPerms);
        categories.add(kits);

        response.add("categories", categories);
        response.addProperty("success", true);
        return response;
    }

    private JsonObject getSystemStatus() {
        JsonObject status = new JsonObject();
        boolean usingExternal = PermissionAPI.isUsingExternal();
        boolean emergencyMode = PermissionAPI.isEmergencyMode();

        status.addProperty("emergencyMode", emergencyMode);
        status.addProperty("usingExternal", usingExternal);
        status.addProperty("canManage", !usingExternal && !emergencyMode);

        if (emergencyMode) {
            status.addProperty("systemType", "EMERGENCY (OP-only fallback)");
            status.addProperty("message", "Permission system failed to initialise. All checks use vanilla OP status. Run /permissions reload once fixed.");
        } else if (usingExternal) {
            com.zerog.neoessentials.permissions.ExternalPermissionAdapter adapter = PermissionAPI.getExternalAdapter();
            status.addProperty("systemType", "External");
            if (adapter != null) {
                status.addProperty("adapterName", adapter.getName());
                status.addProperty("adapterVersion", adapter.getVersion());
                status.addProperty("adapterHealthy", adapter.isHealthy());
                status.addProperty("adapterFailures", adapter.getConsecutiveFailures());
            }
            status.addProperty("message", "Permission management is handled by external adapter. Internal groups/users are not active.");
        } else {
            PermissionManager manager = PermissionAPI.getManager();
            status.addProperty("systemType", "Internal");
            if (manager != null) {
                status.addProperty("groupCount", manager.getGroups().size());
                status.addProperty("defaultGroup", manager.getDefaultGroup());
                int aliasCount = com.zerog.neoessentials.permissions.PermissionAliasManager.getInstance().getAll().size();
                status.addProperty("aliasCount", aliasCount);
            }
        }

        status.addProperty("success", true);
        return status;
    }

    // ========== POST Methods ==========

    private JsonObject createGroup(JsonObject data) {
        JsonObject response = new JsonObject();
        PermissionManager manager = PermissionAPI.getManager();

        if (manager == null) {
            response.addProperty("success", false);
            response.addProperty("message", "Cannot manage groups with external permission system");
            return response;
        }

        try {
            String name = data.get("name").getAsString();
            String prefix = data.has("prefix") ? data.get("prefix").getAsString() : "";
            String suffix = data.has("suffix") ? data.get("suffix").getAsString() : "";
            boolean isDefault = data.has("isDefault") && data.get("isDefault").getAsBoolean();

            PermissionGroup group = new PermissionGroup(name);
            if (!prefix.isEmpty()) group.setPrefix(prefix);
            if (!suffix.isEmpty()) group.setSuffix(suffix);

            manager.addGroup(group);

            if (isDefault) {
                manager.setDefaultGroup(name);
            }

            PermissionStorage.save(manager);

            response.addProperty("success", true);
            response.addProperty("message", "Group created: " + name);
        } catch (Exception e) {
            response.addProperty("success", false);
            response.addProperty("message", "Failed to create group: " + e.getMessage());
        }

        return response;
    }

    private JsonObject addPermissionToGroup(String groupName, JsonObject data) {
        JsonObject response = new JsonObject();
        PermissionManager manager = PermissionAPI.getManager();

        if (manager == null) {
            response.addProperty("success", false);
            response.addProperty("message", "Cannot manage permissions with external system");
            return response;
        }

        try {
            String permission = data.get("permission").getAsString();
            PermissionGroup group = manager.getGroup(groupName);

            if (group != null) {
                group.addPermission(permission);
                PermissionStorage.save(manager);
                response.addProperty("success", true);
                response.addProperty("message", "Permission added to group: " + groupName);
            } else {
                response.addProperty("success", false);
                response.addProperty("message", "Group not found: " + groupName);
            }
        } catch (Exception e) {
            response.addProperty("success", false);
            response.addProperty("message", "Failed to add permission: " + e.getMessage());
        }

        return response;
    }

    private JsonObject setUserGroup(String username, JsonObject data) {
        JsonObject response = new JsonObject();
        ServerPlayer player = server.getPlayerList().getPlayerByName(username);

        if (player == null) {
            response.addProperty("success", false);
            response.addProperty("message", "Player not found or offline: " + username);
            return response;
        }

        PermissionManager manager = PermissionAPI.getManager();
        if (manager == null) {
            response.addProperty("success", false);
            response.addProperty("message", "Cannot manage user groups with external system");
            return response;
        }

        try {
            String groupName = data.get("group").getAsString();
            PermissionUser user = manager.getUser(player.getUUID());
            if (user != null) {
                user.setGroup(groupName);
                PermissionStorage.save(manager);
                manager.clearCache(); // Clear permission cache

                response.addProperty("success", true);
                response.addProperty("message", "User " + username + " set to group: " + groupName);
            } else {
                response.addProperty("success", false);
                response.addProperty("message", "User not found: " + username);
            }
        } catch (Exception e) {
            response.addProperty("success", false);
            response.addProperty("message", "Failed to set user group: " + e.getMessage());
        }

        return response;
    }

    private JsonObject addPermissionToUser(String username, JsonObject data) {
        JsonObject response = new JsonObject();
        ServerPlayer player = server.getPlayerList().getPlayerByName(username);

        if (player == null) {
            response.addProperty("success", false);
            response.addProperty("message", "Player not found or offline: " + username);
            return response;
        }

        PermissionManager manager = PermissionAPI.getManager();
        if (manager == null) {
            response.addProperty("success", false);
            response.addProperty("message", "Cannot manage user permissions with external system");
            return response;
        }

        try {
            String permission = data.get("permission").getAsString();
            PermissionUser user = manager.getUser(player.getUUID());
            if (user != null) {
                user.addPermission(permission);
                PermissionStorage.save(manager);
                manager.clearCache(); // Clear permission cache

                response.addProperty("success", true);
                response.addProperty("message", "Permission added to user: " + username);
            } else {
                response.addProperty("success", false);
                response.addProperty("message", "User not found: " + username);
            }
        } catch (Exception e) {
            response.addProperty("success", false);
            response.addProperty("message", "Failed to add permission: " + e.getMessage());
        }

        return response;
    }

    // ========== PUT Methods ==========

    private JsonObject updateGroup(String groupName, JsonObject data) {
        JsonObject response = new JsonObject();
        PermissionManager manager = PermissionAPI.getManager();

        if (manager == null) {
            response.addProperty("success", false);
            response.addProperty("message", "Cannot manage groups with external system");
            return response;
        }

        try {
            PermissionGroup group = manager.getGroup(groupName);
            if (group != null) {
                if (data.has("prefix")) group.setPrefix(data.get("prefix").getAsString());
                if (data.has("suffix")) group.setSuffix(data.get("suffix").getAsString());
                if (data.has("isDefault") && data.get("isDefault").getAsBoolean()) {
                    manager.setDefaultGroup(groupName);
                }

                PermissionStorage.save(manager);
                response.addProperty("success", true);
                response.addProperty("message", "Group updated: " + groupName);
            } else {
                response.addProperty("success", false);
                response.addProperty("message", "Group not found: " + groupName);
            }
        } catch (Exception e) {
            response.addProperty("success", false);
            response.addProperty("message", "Failed to update group: " + e.getMessage());
        }

        return response;
    }

    private JsonObject updateUser(String username, JsonObject data) {
        // User update functionality (if needed for future extensions)
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("message", "User update not yet implemented");
        return response;
    }

    // ========== DELETE Methods ==========

    private JsonObject deleteGroup(String groupName) {
        JsonObject response = new JsonObject();
        PermissionManager manager = PermissionAPI.getManager();

        if (manager == null) {
            response.addProperty("success", false);
            response.addProperty("message", "Cannot manage groups with external system");
            return response;
        }

        try {
            manager.getGroups().remove(groupName);
            PermissionStorage.save(manager);

            response.addProperty("success", true);
            response.addProperty("message", "Group deleted: " + groupName);
        } catch (Exception e) {
            response.addProperty("success", false);
            response.addProperty("message", "Failed to delete group: " + e.getMessage());
        }

        return response;
    }

    private JsonObject removePermissionFromGroup(String groupName, String permission) {
        JsonObject response = new JsonObject();
        PermissionManager manager = PermissionAPI.getManager();

        if (manager == null) {
            response.addProperty("success", false);
            response.addProperty("message", "Cannot manage permissions with external system");
            return response;
        }

        try {
            PermissionGroup group = manager.getGroup(groupName);
            if (group != null) {
                group.removePermission(permission);
                PermissionStorage.save(manager);
                response.addProperty("success", true);
                response.addProperty("message", "Permission removed from group: " + groupName);
            } else {
                response.addProperty("success", false);
                response.addProperty("message", "Group not found: " + groupName);
            }
        } catch (Exception e) {
            response.addProperty("success", false);
            response.addProperty("message", "Failed to remove permission: " + e.getMessage());
        }

        return response;
    }

    private JsonObject removePermissionFromUser(String username, String permission) {
        JsonObject response = new JsonObject();
        ServerPlayer player = server.getPlayerList().getPlayerByName(username);

        if (player == null) {
            response.addProperty("success", false);
            response.addProperty("message", "Player not found or offline: " + username);
            return response;
        }

        PermissionManager manager = PermissionAPI.getManager();
        if (manager == null) {
            response.addProperty("success", false);
            response.addProperty("message", "Cannot manage user permissions with external system");
            return response;
        }

        try {
            PermissionUser user = manager.getUser(player.getUUID());
            if (user != null) {
                user.removePermission(permission);
                PermissionStorage.save(manager);
                manager.clearCache(); // Clear permission cache

                response.addProperty("success", true);
                response.addProperty("message", "Permission removed from user: " + username);
            } else {
                response.addProperty("success", false);
                response.addProperty("message", "User not found: " + username);
            }
        } catch (Exception e) {
            response.addProperty("success", false);
            response.addProperty("message", "Failed to remove permission: " + e.getMessage());
        }

        return response;
    }

    // ========== Reload ==========

    private JsonObject reloadPermissions() {
        JsonObject response = new JsonObject();
        try {
            com.zerog.neoessentials.permissions.PermissionSystem.reload();
            response.addProperty("success", true);
            response.addProperty("message", "Permissions reloaded successfully");
        } catch (Exception e) {
            response.addProperty("success", false);
            response.addProperty("message", "Failed to reload: " + e.getMessage());
        }
        return response;
    }

    // ========== Context Permissions ==========

    private JsonObject listGroupContextPerms(String groupName) {
        JsonObject response = new JsonObject();
        PermissionManager manager = PermissionAPI.getManager();
        if (manager == null) return createErrorResponse("Permission manager not available");
        PermissionGroup group = manager.getGroup(groupName);
        if (group == null) return createErrorResponse("Group not found: " + groupName);
        JsonObject ctxObj = new JsonObject();
        group.getContextualPermissions().forEach((ctxKey, nodeMap) -> {
            JsonObject nodes = new JsonObject();
            nodeMap.forEach(nodes::addProperty);
            ctxObj.add(ctxKey, nodes);
        });
        response.add("contextualPermissions", ctxObj);
        response.addProperty("success", true);
        return response;
    }

    private JsonObject addGroupContextPerm(String groupName, JsonObject data) {
        JsonObject response = new JsonObject();
        PermissionManager manager = PermissionAPI.getManager();
        if (manager == null) return createErrorResponse("Permission manager not available");
        PermissionGroup group = manager.getGroup(groupName);
        if (group == null) return createErrorResponse("Group not found: " + groupName);
        if (!data.has("contextKey") || !data.has("node") || !data.has("allow"))
            return createErrorResponse("Required fields: contextKey, node, allow (boolean)");
        String contextKey = data.get("contextKey").getAsString();
        String node = data.get("node").getAsString();
        boolean allow = data.get("allow").getAsBoolean();
        group.addContextPermission(contextKey, node, allow);
        manager.clearCache();
        try { PermissionStorage.save(manager); } catch (Exception e) { return createErrorResponse("Save failed: " + e.getMessage()); }
        response.addProperty("success", true);
        response.addProperty("message", "Contextual override set: " + node + " → " + (allow ? "allow" : "deny") + " in " + contextKey + " for group " + groupName);
        return response;
    }

    private JsonObject removeGroupContextPerm(String groupName, JsonObject data) {
        JsonObject response = new JsonObject();
        PermissionManager manager = PermissionAPI.getManager();
        if (manager == null) return createErrorResponse("Permission manager not available");
        PermissionGroup group = manager.getGroup(groupName);
        if (group == null) return createErrorResponse("Group not found: " + groupName);
        if (!data.has("contextKey") || !data.has("node"))
            return createErrorResponse("Required fields: contextKey, node");
        boolean removed = group.removeContextPermission(data.get("contextKey").getAsString(), data.get("node").getAsString());
        if (!removed) return createErrorResponse("No such contextual override");
        manager.clearCache();
        try { PermissionStorage.save(manager); } catch (Exception e) { return createErrorResponse("Save failed: " + e.getMessage()); }
        response.addProperty("success", true);
        response.addProperty("message", "Contextual override removed");
        return response;
    }

    private JsonObject listUserContextPerms(String username) {
        JsonObject response = new JsonObject();
        PermissionManager manager = PermissionAPI.getManager();
        if (manager == null) return createErrorResponse("Permission manager not available");
        ServerPlayer player = server.getPlayerList().getPlayerByName(username);
        if (player == null) return createErrorResponse("Player not online: " + username);
        com.zerog.neoessentials.permissions.PermissionUser user = manager.getUser(player.getUUID());
        JsonObject ctxObj = new JsonObject();
        user.getContextualPermissions().forEach((ctxKey, nodeMap) -> {
            JsonObject nodes = new JsonObject();
            nodeMap.forEach(nodes::addProperty);
            ctxObj.add(ctxKey, nodes);
        });
        response.add("contextualPermissions", ctxObj);
        response.addProperty("success", true);
        return response;
    }

    private JsonObject addUserContextPerm(String username, JsonObject data) {
        JsonObject response = new JsonObject();
        PermissionManager manager = PermissionAPI.getManager();
        if (manager == null) return createErrorResponse("Permission manager not available");
        ServerPlayer player = server.getPlayerList().getPlayerByName(username);
        if (player == null) return createErrorResponse("Player not online: " + username);
        if (!data.has("contextKey") || !data.has("node") || !data.has("allow"))
            return createErrorResponse("Required fields: contextKey, node, allow");
        com.zerog.neoessentials.permissions.PermissionUser user = manager.getUser(player.getUUID());
        user.addContextPermission(data.get("contextKey").getAsString(), data.get("node").getAsString(), data.get("allow").getAsBoolean());
        manager.clearCache();
        try { PermissionStorage.save(manager); } catch (Exception e) { return createErrorResponse("Save failed: " + e.getMessage()); }
        response.addProperty("success", true);
        response.addProperty("message", "Contextual override set for user " + username);
        return response;
    }

    private JsonObject removeUserContextPerm(String username, JsonObject data) {
        JsonObject response = new JsonObject();
        PermissionManager manager = PermissionAPI.getManager();
        if (manager == null) return createErrorResponse("Permission manager not available");
        ServerPlayer player = server.getPlayerList().getPlayerByName(username);
        if (player == null) return createErrorResponse("Player not online: " + username);
        if (!data.has("contextKey") || !data.has("node"))
            return createErrorResponse("Required fields: contextKey, node");
        com.zerog.neoessentials.permissions.PermissionUser user = manager.getUser(player.getUUID());
        boolean removed = user.removeContextPermission(data.get("contextKey").getAsString(), data.get("node").getAsString());
        if (!removed) return createErrorResponse("No such contextual override");
        manager.clearCache();
        try { PermissionStorage.save(manager); } catch (Exception e) { return createErrorResponse("Save failed: " + e.getMessage()); }
        response.addProperty("success", true);
        response.addProperty("message", "Contextual override removed for user " + username);
        return response;
    }

    // ========== Temporary Permissions ==========

    private JsonObject listGroupTempPerms(String groupName) {
        JsonObject response = new JsonObject();
        PermissionManager manager = PermissionAPI.getManager();
        if (manager == null) return createErrorResponse("Permission manager not available");
        PermissionGroup group = manager.getGroup(groupName);
        if (group == null) return createErrorResponse("Group not found: " + groupName);
        JsonArray arr = new JsonArray();
        long now = System.currentTimeMillis();
        group.getTempPermissions().forEach((node, expiry) -> {
            if (expiry > now) {
                JsonObject entry = new JsonObject();
                entry.addProperty("node", node);
                entry.addProperty("expiryMs", expiry);
                entry.addProperty("remaining", com.zerog.neoessentials.permissions.PermissionManager.formatDuration(expiry - now));
                arr.add(entry);
            }
        });
        response.add("tempPermissions", arr);
        response.addProperty("success", true);
        return response;
    }

    private JsonObject addGroupTempPerm(String groupName, JsonObject data) {
        JsonObject response = new JsonObject();
        PermissionManager manager = PermissionAPI.getManager();
        if (manager == null) return createErrorResponse("Permission manager not available");
        PermissionGroup group = manager.getGroup(groupName);
        if (group == null) return createErrorResponse("Group not found: " + groupName);
        if (!data.has("node") || !data.has("duration"))
            return createErrorResponse("Required fields: node, duration (e.g. 1d, 12h, 30m)");
        try {
            long ms = com.zerog.neoessentials.permissions.PermissionManager.parseDurationMs(data.get("duration").getAsString());
            group.addTempPermission(data.get("node").getAsString(), System.currentTimeMillis() + ms);
            manager.clearCache();
            PermissionStorage.save(manager);
            response.addProperty("success", true);
            response.addProperty("message", "Temp permission added to group " + groupName + " for " + data.get("duration").getAsString());
        } catch (Exception e) { return createErrorResponse(e.getMessage()); }
        return response;
    }

    private JsonObject removeGroupTempPerm(String groupName, String node) {
        JsonObject response = new JsonObject();
        PermissionManager manager = PermissionAPI.getManager();
        if (manager == null) return createErrorResponse("Permission manager not available");
        PermissionGroup group = manager.getGroup(groupName);
        if (group == null) return createErrorResponse("Group not found: " + groupName);
        group.removeTempPermission(node);
        manager.clearCache();
        try { PermissionStorage.save(manager); } catch (Exception e) { return createErrorResponse("Save failed: " + e.getMessage()); }
        response.addProperty("success", true);
        response.addProperty("message", "Temp permission " + node + " removed from group " + groupName);
        return response;
    }

    private JsonObject listUserTempPerms(String username) {
        JsonObject response = new JsonObject();
        PermissionManager manager = PermissionAPI.getManager();
        if (manager == null) return createErrorResponse("Permission manager not available");
        ServerPlayer player = server.getPlayerList().getPlayerByName(username);
        if (player == null) return createErrorResponse("Player not online: " + username);
        com.zerog.neoessentials.permissions.PermissionUser user = manager.getUser(player.getUUID());
        JsonArray arr = new JsonArray();
        long now = System.currentTimeMillis();
        user.getTempPermissions().forEach((node, expiry) -> {
            if (expiry > now) {
                JsonObject entry = new JsonObject();
                entry.addProperty("node", node);
                entry.addProperty("expiryMs", expiry);
                entry.addProperty("remaining", com.zerog.neoessentials.permissions.PermissionManager.formatDuration(expiry - now));
                arr.add(entry);
            }
        });
        response.add("tempPermissions", arr);
        response.addProperty("success", true);
        return response;
    }

    private JsonObject addUserTempPerm(String username, JsonObject data) {
        JsonObject response = new JsonObject();
        PermissionManager manager = PermissionAPI.getManager();
        if (manager == null) return createErrorResponse("Permission manager not available");
        ServerPlayer player = server.getPlayerList().getPlayerByName(username);
        if (player == null) return createErrorResponse("Player not online: " + username);
        if (!data.has("node") || !data.has("duration"))
            return createErrorResponse("Required fields: node, duration");
        try {
            long ms = com.zerog.neoessentials.permissions.PermissionManager.parseDurationMs(data.get("duration").getAsString());
            com.zerog.neoessentials.permissions.PermissionUser user = manager.getUser(player.getUUID());
            user.addTempPermission(data.get("node").getAsString(), System.currentTimeMillis() + ms);
            manager.clearCache();
            PermissionStorage.save(manager);
            response.addProperty("success", true);
            response.addProperty("message", "Temp permission added to " + username + " for " + data.get("duration").getAsString());
        } catch (Exception e) { return createErrorResponse(e.getMessage()); }
        return response;
    }

    private JsonObject removeUserTempPerm(String username, String node) {
        JsonObject response = new JsonObject();
        PermissionManager manager = PermissionAPI.getManager();
        if (manager == null) return createErrorResponse("Permission manager not available");
        ServerPlayer player = server.getPlayerList().getPlayerByName(username);
        if (player == null) return createErrorResponse("Player not online: " + username);
        com.zerog.neoessentials.permissions.PermissionUser user = manager.getUser(player.getUUID());
        user.removeTempPermission(node);
        manager.clearCache();
        try { PermissionStorage.save(manager); } catch (Exception e) { return createErrorResponse("Save failed: " + e.getMessage()); }
        response.addProperty("success", true);
        response.addProperty("message", "Temp permission " + node + " removed from " + username);
        return response;
    }

    // ========== Aliases ==========

    private JsonObject listAliases() {
        JsonObject response = new JsonObject();
        JsonObject aliasObj = new JsonObject();
        com.zerog.neoessentials.permissions.PermissionAliasManager.getInstance()
            .getAll().forEach(aliasObj::addProperty);
        response.add("aliases", aliasObj);
        response.addProperty("count", aliasObj.size());
        response.addProperty("success", true);
        return response;
    }

    private JsonObject addAlias(JsonObject data) {
        JsonObject response = new JsonObject();
        if (!data.has("alias") || !data.has("canonical"))
            return createErrorResponse("Required fields: alias, canonical");
        String alias = data.get("alias").getAsString();
        String canonical = data.get("canonical").getAsString();
        com.zerog.neoessentials.permissions.PermissionAliasManager mgr =
            com.zerog.neoessentials.permissions.PermissionAliasManager.getInstance();
        mgr.addAlias(alias, canonical);
        try { mgr.save(); } catch (Exception e) { return createErrorResponse("Save failed: " + e.getMessage()); }
        response.addProperty("success", true);
        response.addProperty("message", "Alias registered: " + alias + " → " + canonical);
        return response;
    }

    private JsonObject removeAlias(String alias) {
        JsonObject response = new JsonObject();
        com.zerog.neoessentials.permissions.PermissionAliasManager mgr =
            com.zerog.neoessentials.permissions.PermissionAliasManager.getInstance();
        boolean removed = mgr.removeAlias(alias);
        if (!removed) return createErrorResponse("Alias not found: " + alias);
        try { mgr.save(); } catch (Exception e) { return createErrorResponse("Save failed: " + e.getMessage()); }
        response.addProperty("success", true);
        response.addProperty("message", "Alias removed: " + alias);
        return response;
    }

    // ========== Helper Methods ==========

    private String extractGroupName(String path, String suffix) {
        return path.substring(7, path.length() - suffix.length());
    }

    private String extractUsername(String path, String suffix) {
        return path.substring(6, path.length() - suffix.length());
    }

    private int getOnlineUserCount() {
        return server.getPlayerList().getPlayerCount();
    }

    private JsonObject createErrorResponse(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", message);
        return error;
    }

    private void sendResponse(HttpExchange exchange, int statusCode, JsonObject response) throws IOException {
        String jsonResponse = GSON.toJson(response);
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
