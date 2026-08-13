package com.zerog.neoessentials.webdashboard.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Handler for /api/commands endpoint
 * Executes server commands and returns results
 */
public class CommandExecutionHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandExecutionHandler.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    // Command history storage
    private static final List<CommandHistoryEntry> commandHistory = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_HISTORY_SIZE = 100;
    
    // Command output capture
    private static final Map<String, List<String>> commandOutputs = new ConcurrentHashMap<>();
    
    // Restricted commands (require special permission)
    private static final Set<String> RESTRICTED_COMMANDS = new HashSet<>(Arrays.asList(
        "stop", "restart", "reload", "op", "deop", "whitelist", "ban", "ban-ip"
    ));
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Add CORS headers
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        
        // Handle OPTIONS preflight
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "CommandExecutionHandler handling request: {} {}", method, path);

        try {
            // This whole endpoint runs arbitrary commands at OP level 4 (see
            // createDashboardCommandSource below) — that's not limited to the
            // RESTRICTED_COMMANDS list (e.g. /gamemode, /give, /execute are just as
            // dangerous), so the endpoint itself must be admin-only. Previously it had NO
            // role check at all (only DashboardAPI.withAuth ran, which merely verifies a
            // valid session, not its role) — any authenticated session, including a
            // self-registered default VIEWER via /dashboardregister, could run this.
            if (!Boolean.TRUE.equals(exchange.getAttribute("auth-admin"))) {
                sendJsonResponse(exchange, 403, createErrorResponse("Admin access required"));
                return;
            }
            if ("POST".equals(method) && path.endsWith("/execute")) {
                handleExecute(exchange);
            } else if ("GET".equals(method) && path.endsWith("/history")) {
                handleHistory(exchange);
            } else if ("GET".equals(method) && path.endsWith("/suggestions")) {
                handleSuggestions(exchange);
            } else {
                sendJsonResponse(exchange, 400, createErrorResponse("Invalid endpoint"));
            }
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Error handling command execution request", e);
            sendJsonResponse(exchange, 500, createErrorResponse("Internal server error: " + e.getMessage()));
        }
    }
    
    /**
     * Execute a server command
     * POST /api/commands/execute
     * Body: {"command": "list", "checkPermissions": true}
     */
    private void handleExecute(HttpExchange exchange) throws IOException {
        String requestBody = readRequestBody(exchange);
        JsonObject request = GSON.fromJson(requestBody, JsonObject.class);
        
        if (!request.has("command")) {
            sendJsonResponse(exchange, 400, createErrorResponse("Missing 'command' field"));
            return;
        }
        
        String command = request.get("command").getAsString().trim();

        // Remove leading slash if present
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        // Restricted commands are always blocked here, regardless of any client-supplied
        // flag — this used to be gated behind a "checkPermissions" boolean read straight from
        // the request body (defaulting to false), so a caller could just omit/falsify it to
        // run op/whitelist/ban-ip/stop unchecked. Now that the whole endpoint is admin-only
        // (see handle() above), this is defense-in-depth: even an admin session is nudged
        // toward the dedicated AdminEndpoint actions for these instead of raw execution.
        String baseCommand = command.split(" ")[0];
        if (RESTRICTED_COMMANDS.contains(baseCommand.toLowerCase())) {
            sendJsonResponse(exchange, 403, createErrorResponse("Command '" + baseCommand + "' requires elevated permissions"));
            return;
        }
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            sendJsonResponse(exchange, 503, createErrorResponse("Server not available"));
            return;
        }
        
        // Execute command and capture output
        String executionId = UUID.randomUUID().toString();
        List<String> output = new ArrayList<>();
        commandOutputs.put(executionId, output);

        Object authUsername = exchange.getAttribute("auth-username");
        NeoLog.debug(LOGGER, LogCategory.WEB_DASHBOARD, "Executing dashboard command '{}' (by {}, executionId={})",
            command, authUsername != null ? authUsername : "unknown", executionId);

        try {
            // Create command source for dashboard
            CommandSourceStack source = createDashboardCommandSource(server, output);
            
            // Execute command on server thread
            int result = server.getCommands().getDispatcher().execute(command, source);
            
            // Add to history
            addToHistory(command, result > 0, String.join("\n", output));
            
            // Build response
            JsonObject response = new JsonObject();
            response.addProperty("success", result > 0);
            response.addProperty("command", command);
            response.addProperty("result", result);
            response.addProperty("executionId", executionId);
            
            JsonArray outputArray = new JsonArray();
            output.forEach(outputArray::add);
            response.add("output", outputArray);
            
            sendJsonResponse(exchange, 200, response);
            
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.WEB_DASHBOARD, "Error executing command: {}", command, e);
            
            addToHistory(command, false, "Error: " + e.getMessage());
            
            JsonObject response = new JsonObject();
            response.addProperty("success", false);
            response.addProperty("command", command);
            response.addProperty("error", e.getMessage());
            response.addProperty("executionId", executionId);
            
            sendJsonResponse(exchange, 200, response);
        } finally {
            // Cleanup output after a delay
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    commandOutputs.remove(executionId);
                }
            }, 60000); // Remove after 1 minute
        }
    }
    
    /**
     * Get command history
     * GET /api/commands/history?limit=50
     */
    private void handleHistory(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        int limit = Integer.parseInt(params.getOrDefault("limit", "50"));
        
        JsonObject response = new JsonObject();
        JsonArray history = new JsonArray();
        
        synchronized (commandHistory) {
            int start = Math.max(0, commandHistory.size() - limit);
            commandHistory.subList(start, commandHistory.size()).forEach(entry -> {
                JsonObject item = new JsonObject();
                item.addProperty("command", entry.command);
                item.addProperty("success", entry.success);
                item.addProperty("output", entry.output);
                item.addProperty("timestamp", entry.timestamp);
                history.add(item);
            });
        }
        
        response.add("history", history);
        response.addProperty("total", commandHistory.size());
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * Get command suggestions/autocomplete
     * GET /api/commands/suggestions?input=game
     */
    private void handleSuggestions(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
        String input = params.getOrDefault("input", "").toLowerCase();
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            sendJsonResponse(exchange, 503, createErrorResponse("Server not available"));
            return;
        }
        
        JsonObject response = new JsonObject();
        JsonArray suggestions = new JsonArray();
        
        // Get all available commands
        server.getCommands().getDispatcher().getRoot().getChildren().forEach(node -> {
            String commandName = node.getName();
            if (commandName.toLowerCase().startsWith(input)) {
                JsonObject suggestion = new JsonObject();
                suggestion.addProperty("command", commandName);
                suggestion.addProperty("restricted", RESTRICTED_COMMANDS.contains(commandName.toLowerCase()));
                suggestions.add(suggestion);
            }
        });
        
        response.add("suggestions", suggestions);
        response.addProperty("input", input);
        
        sendJsonResponse(exchange, 200, response);
    }
    
    /**
     * Create a command source for dashboard execution
     */
    private CommandSourceStack createDashboardCommandSource(MinecraftServer server, List<String> outputCapture) {
        ServerLevel overworld = server.overworld();
        
        // Create a custom command source that captures output
        CommandSource outputSource = new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                outputCapture.add(message.getString());
            }
            
            @Override
            public boolean acceptsSuccess() {
                return true;
            }
            
            @Override
            public boolean acceptsFailure() {
                return true;
            }
            
            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        
        return new CommandSourceStack(
            outputSource,
            Vec3.ZERO,
            Vec2.ZERO,
            overworld,
            4, // Permission level (admin)
            "Dashboard",
            Component.literal("Dashboard"),
            server,
            null
        );
    }
    
    /**
     * Add command to history
     */
    private void addToHistory(String command, boolean success, String output) {
        synchronized (commandHistory) {
            commandHistory.add(new CommandHistoryEntry(command, success, output, System.currentTimeMillis()));
            
            // Trim history if too large
            if (commandHistory.size() > MAX_HISTORY_SIZE) {
                commandHistory.remove(0);
            }
        }
    }
    
    /**
     * Parse query parameters
     */
    private Map<String, String> parseQueryParams(String query) {
        if (query == null || query.isEmpty()) {
            return Collections.emptyMap();
        }
        
        return Arrays.stream(query.split("&"))
            .map(param -> param.split("=", 2))
            .filter(parts -> parts.length == 2)
            .collect(Collectors.toMap(
                parts -> parts[0],
                parts -> parts[1]
            ));
    }
    
    /**
     * Read request body
     */
    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
    
    /**
     * Send JSON response
     */
    private void sendJsonResponse(HttpExchange exchange, int statusCode, JsonObject json) throws IOException {
        byte[] response = GSON.toJson(json).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
    
    /**
     * Create error response JSON
     */
    private JsonObject createErrorResponse(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        error.addProperty("timestamp", System.currentTimeMillis());
        return error;
    }
    
    /**
     * Command history entry
     */
    private static class CommandHistoryEntry {
        final String command;
        final boolean success;
        final String output;
        final long timestamp;
        
        CommandHistoryEntry(String command, boolean success, String output, long timestamp) {
            this.command = command;
            this.success = success;
            this.output = output;
            this.timestamp = timestamp;
        }
    }
}
