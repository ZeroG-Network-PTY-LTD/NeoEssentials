package com.zerog.neoessentials.webdashboard.endpoints;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.webdashboard.security.AuthenticationManager;
import com.zerog.neoessentials.webdashboard.security.Session;
import com.zerog.neoessentials.webdashboard.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * REST handler for dashboard user management (RBAC).
 *
 * All routes require ADMIN role unless noted.
 *
 *   GET    /api/users/list               – list all users
 *   GET    /api/users/sessions           – list active sessions
 *   POST   /api/users/create             – create user {username, password, email, role}
 *   POST   /api/users/{id}/role          – change role {role}
 *   POST   /api/users/{id}/password      – reset password {password} or generate temp if empty
 *   POST   /api/users/{id}/enable        – enable account
 *   POST   /api/users/{id}/disable       – disable account
 *   DELETE /api/users/{id}               – delete user
 *   DELETE /api/users/sessions/{sid}     – revoke session
 */
public class UserManagementEndpoint implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserManagementEndpoint.class);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        // All user-management endpoints require admin
        Boolean admin = (Boolean) exchange.getAttribute("auth-admin");
        if (!Boolean.TRUE.equals(admin)) {
            sendJson(exchange, 403, "{\"success\":false,\"error\":\"Admin access required\"}");
            return;
        }

        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if ("GET".equals(method) && path.endsWith("/list")) {
                handleList(exchange);
            } else if ("GET".equals(method) && path.endsWith("/sessions")) {
                handleSessions(exchange);
            } else if ("POST".equals(method) && path.endsWith("/create")) {
                handleCreate(exchange);
            } else if ("POST".equals(method) && path.endsWith("/sync")) {
                handleSync(exchange);
            } else if ("POST".equals(method) && path.contains("/role")) {
                handleSetRole(exchange, extractId(path, "/role"));
            } else if ("POST".equals(method) && path.contains("/password")) {
                handleSetPassword(exchange, extractId(path, "/password"));
            } else if ("POST".equals(method) && path.endsWith("/enable")) {
                handleSetEnabled(exchange, extractId(path, "/enable"), true);
            } else if ("POST".equals(method) && path.endsWith("/disable")) {
                handleSetEnabled(exchange, extractId(path, "/disable"), false);
            } else if ("DELETE".equals(method) && path.contains("/sessions/")) {
                handleRevokeSession(exchange, path.substring(path.lastIndexOf('/') + 1));
            } else if ("DELETE".equals(method)) {
                handleDelete(exchange, path.substring(path.lastIndexOf('/') + 1));
            } else {
                sendJson(exchange, 404, "{\"success\":false,\"error\":\"Unknown user management endpoint\"}");
            }
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, json(false, e.getMessage()));
        } catch (Exception e) {
            LOGGER.error("Error in UserManagementEndpoint", e);
            sendJson(exchange, 500, json(false, "Internal error: " + e.getMessage()));
        }
    }

    // ── List users ─────────────────────────────────────────────────────────────

    private void handleList(HttpExchange exchange) throws IOException {
        Collection<User> users = AuthenticationManager.getInstance().getAllUsers();
        StringBuilder sb = new StringBuilder("{\"success\":true,\"count\":").append(users.size()).append(",\"users\":[");
        boolean first = true;
        for (User u : users) {
            if (!first) sb.append(",");
            first = false;
            sb.append(u.toJson().toString());
        }
        sb.append("]}");
        sendJson(exchange, 200, sb.toString());
    }

    // ── List sessions ──────────────────────────────────────────────────────────

    private void handleSessions(HttpExchange exchange) throws IOException {
        Collection<Session> sessions = AuthenticationManager.getInstance().getActiveSessions();
        StringBuilder sb = new StringBuilder("{\"success\":true,\"count\":").append(sessions.size()).append(",\"sessions\":[");
        boolean first = true;
        for (Session s : sessions) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{");
            sb.append("\"sessionId\":\"").append(esc(s.getSessionId())).append("\",");
            sb.append("\"username\":\"").append(esc(s.getUsername())).append("\",");
            sb.append("\"role\":\"").append(esc(s.getRole().name())).append("\",");
            sb.append("\"ipAddress\":\"").append(esc(s.getIpAddress())).append("\",");
            sb.append("\"createdAt\":").append(s.getCreatedAt()).append(",");
            sb.append("\"lastAccessAt\":").append(s.getLastAccessTime());
            sb.append("}");
        }
        sb.append("]}");
        sendJson(exchange, 200, sb.toString());
    }

    // ── Create user ────────────────────────────────────────────────────────────

    private void handleCreate(HttpExchange exchange) throws Exception {
        JsonObject body = readBody(exchange);
        String username = body.has("username") ? body.get("username").getAsString() : "";
        String password = body.has("password") ? body.get("password").getAsString() : "";
        String email    = body.has("email")    ? body.get("email").getAsString()    : "";
        String roleStr  = body.has("role")     ? body.get("role").getAsString()     : "VIEWER";

        User.Role role;
        try { role = User.Role.valueOf(roleStr.toUpperCase()); }
        catch (Exception e) { role = User.Role.VIEWER; }

        User created = AuthenticationManager.getInstance().createUser(username, password, email, role);
        sendJson(exchange, 200, "{\"success\":true,\"message\":\"User created\",\"user\":" + created.toJson() + "}");
        LOGGER.info("Dashboard user created: {} ({})", username, role);
    }

    // ── Sync (idempotent create-or-update) ──────────────────────────────────────
    //
    // For an external dashboard that manages its own user accounts and wants the mod to
    // mirror them, rather than the reverse (see DashboardUserSyncWebhook for that direction).
    // Unlike /create, this never errors on an existing username — it just updates role/email
    // in place — so the caller can safely re-POST the same account's current state any time
    // without first checking whether it already exists here.

    private void handleSync(HttpExchange exchange) throws Exception {
        JsonObject body = readBody(exchange);
        String username = body.has("username") ? body.get("username").getAsString() : "";
        if (username.isBlank()) throw new IllegalArgumentException("Missing required field: username");

        String email = body.has("email") ? body.get("email").getAsString() : "";
        String roleStr = body.has("role") ? body.get("role").getAsString() : "VIEWER";
        User.Role role;
        try { role = User.Role.valueOf(roleStr.toUpperCase()); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid role: " + roleStr); }

        AuthenticationManager auth = AuthenticationManager.getInstance();
        User existing = auth.getUserByUsername(username);

        User result;
        boolean created;
        if (existing == null) {
            // No mod-side password is meaningful here — the external dashboard is the actual
            // login surface for this account; generate one only to satisfy createUser()'s
            // validation, and mark it as requiring a change so it's obviously a placeholder if
            // anyone ever tries to log in with it directly against this mod's own /api/auth.
            String placeholder = auth.generateSecureRandomPassword();
            result = auth.createUser(username, placeholder, email, role);
            created = true;
        } else {
            if (existing.getRole() != role) auth.updateUserRole(existing.getId(), role);
            if (email != null && !email.isBlank()) existing.setEmail(email);
            auth.saveUsers();
            result = existing;
            created = false;
        }

        sendJson(exchange, created ? 201 : 200,
            "{\"success\":true,\"created\":" + created + ",\"user\":" + result.toJson() + "}");
        LOGGER.info("Dashboard user synced from external source: {} ({}, created={})", username, role, created);
    }

    // ── Set role ───────────────────────────────────────────────────────────────

    private void handleSetRole(HttpExchange exchange, String userId) throws Exception {
        JsonObject body = readBody(exchange);
        String roleStr = body.has("role") ? body.get("role").getAsString() : "VIEWER";
        User.Role role;
        try { role = User.Role.valueOf(roleStr.toUpperCase()); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid role: " + roleStr); }

        AuthenticationManager.getInstance().updateUserRole(userId, role);
        sendJson(exchange, 200, json(true, "Role updated to " + role));
    }

    // ── Set password ───────────────────────────────────────────────────────────

    private void handleSetPassword(HttpExchange exchange, String userId) throws Exception {
        JsonObject body = readBody(exchange);
        if (body.has("password") && !body.get("password").getAsString().isEmpty()) {
            AuthenticationManager.getInstance().updatePassword(userId, body.get("password").getAsString());
            sendJson(exchange, 200, json(true, "Password updated"));
        } else {
            // Generate temp password
            User user = AuthenticationManager.getInstance().getUser(userId);
            if (user == null) throw new IllegalArgumentException("User not found");
            String temp = AuthenticationManager.getInstance().generateTempPassword(user.getUsername());
            sendJson(exchange, 200, "{\"success\":true,\"message\":\"Temporary password generated\",\"tempPassword\":\"" + esc(temp) + "\"}");
        }
    }

    // ── Enable / disable ───────────────────────────────────────────────────────

    private void handleSetEnabled(HttpExchange exchange, String userId, boolean enabled) throws IOException {
        AuthenticationManager.getInstance().setUserEnabled(userId, enabled);
        sendJson(exchange, 200, json(true, "User " + (enabled ? "enabled" : "disabled")));
    }

    // ── Delete user ────────────────────────────────────────────────────────────

    private void handleDelete(HttpExchange exchange, String userId) throws IOException {
        // Prevent deleting self
        String selfUsername = (String) exchange.getAttribute("auth-username");
        User target = AuthenticationManager.getInstance().getUser(userId);
        if (target != null && target.getUsername().equalsIgnoreCase(selfUsername)) {
            sendJson(exchange, 400, json(false, "You cannot delete your own account"));
            return;
        }
        AuthenticationManager.getInstance().deleteUser(userId);
        sendJson(exchange, 200, json(true, "User deleted"));
    }

    // ── Revoke session ─────────────────────────────────────────────────────────

    private void handleRevokeSession(HttpExchange exchange, String sessionId) throws IOException {
        AuthenticationManager.getInstance().logout(sessionId);
        sendJson(exchange, 200, json(true, "Session revoked"));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Extract the user ID from a path like /api/users/{id}/role */
    private String extractId(String path, String suffix) {
        // path = /api/users/SOME-UUID/role  → strip /api/users/ prefix and the suffix
        String stripped = path.replaceFirst(".*/users/", "").replace(suffix, "");
        return stripped.replaceAll("/", "").trim();
    }

    private JsonObject readBody(HttpExchange exchange) throws IOException {
        return com.zerog.neoessentials.webdashboard.util.RequestBodyUtil.readJsonObject(exchange);
    }

    private void sendJson(HttpExchange exchange, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private String json(boolean ok, String msg) {
        return "{\"success\":" + ok + ",\"message\":\"" + esc(msg) + "\"}";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r");
    }
}


