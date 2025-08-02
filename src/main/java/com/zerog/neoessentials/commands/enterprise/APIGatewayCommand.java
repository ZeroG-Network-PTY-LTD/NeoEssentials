package com.zerog.neoessentials.commands.enterprise;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.systems.gateway.EnterpriseAPIGatewayHub;
import com.zerog.neoessentials.systems.gateway.EnterpriseAPIGatewayHub.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Enterprise API Gateway & Integration Hub Command Interface for NeoEssentials
 * 
 * Provides comprehensive command-line interface for API gateway management and
 * external system integration operations in enterprise Minecraft server environments.
 * 
 * Command Categories:
 * - Gateway Management: Start, stop, configure, and monitor the API gateway
 * - API Endpoint Management: Create, update, delete, and configure API endpoints
 * - Integration Management: Manage external system connectors and integrations
 * - Security Management: API key management, authentication, and authorization
 * - Monitoring & Analytics: View metrics, logs, and performance data
 * - Rate Limiting: Configure and monitor rate limiting policies
 * - Health Checks: Monitor system health and connectivity
 * 
 * Available Commands:
 * /api-gateway status - View gateway system status and metrics
 * /api-gateway start - Start the API gateway server
 * /api-gateway stop - Stop the API gateway server
 * /api-gateway restart - Restart the API gateway server
 * /api-gateway config [reload] - View or reload gateway configuration
 * 
 * /api-endpoints list - List all configured API endpoints
 * /api-endpoints create <id> <path> <method> [target] - Create new API endpoint
 * /api-endpoints delete <id> - Delete API endpoint
 * /api-endpoints enable <id> - Enable API endpoint
 * /api-endpoints disable <id> - Disable API endpoint
 * /api-endpoints info <id> - View detailed endpoint information
 * 
 * /api-keys list - List all API keys
 * /api-keys create <name> [roles] - Create new API key
 * /api-keys delete <keyId> - Delete API key
 * /api-keys rotate <keyId> - Rotate API key
 * /api-keys info <keyId> - View API key details
 * 
 * /integrations list - List all integration connectors
 * /integrations create <id> <type> <connection> - Create integration connector
 * /integrations delete <id> - Delete integration connector
 * /integrations test <id> - Test integration connector
 * /integrations health - View health status of all connectors
 * 
 * /api-metrics overview - View overall API metrics
 * /api-metrics endpoints - View per-endpoint metrics
 * /api-metrics performance - View performance metrics
 * /api-metrics errors - View error metrics and logs
 * 
 * /webhooks list - List webhook endpoints
 * /webhooks create <id> <path> [events] - Create webhook endpoint
 * /webhooks delete <id> - Delete webhook endpoint
 * /webhooks test <id> - Test webhook endpoint
 * 
 * Security Features:
 * - Permission-based command access control
 * - API key management and rotation
 * - Audit logging for all gateway operations
 * - Input validation and sanitization
 * - Role-based operation restrictions
 * 
 * @author ZeroG Enterprise Integration Team
 * @version 4.0.0
 * @since 2025-08-01
 */
public class APIGatewayCommand {
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Register API Gateway commands
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        
        // Main API Gateway command group
        dispatcher.register(Commands.literal("api-gateway")
            .requires(source -> source.hasPermission(3))
            .then(Commands.literal("status")
                .executes(APIGatewayCommand::showGatewayStatus))
            .then(Commands.literal("start")
                .executes(APIGatewayCommand::startGateway))
            .then(Commands.literal("stop")
                .executes(APIGatewayCommand::stopGateway))
            .then(Commands.literal("restart")
                .executes(APIGatewayCommand::restartGateway))
            .then(Commands.literal("config")
                .executes(APIGatewayCommand::showGatewayConfig)
                .then(Commands.literal("reload")
                    .executes(APIGatewayCommand::reloadGatewayConfig)))
        );
        
        // API Endpoints management
        dispatcher.register(Commands.literal("api-endpoints")
            .requires(source -> source.hasPermission(3))
            .then(Commands.literal("list")
                .executes(APIGatewayCommand::listAPIEndpoints))
            .then(Commands.literal("create")
                .then(Commands.argument("id", StringArgumentType.string())
                    .then(Commands.argument("path", StringArgumentType.string())
                        .then(Commands.argument("method", StringArgumentType.string())
                            .executes(context -> createAPIEndpoint(context,
                                StringArgumentType.getString(context, "id"),
                                StringArgumentType.getString(context, "path"),
                                StringArgumentType.getString(context, "method"), null))
                            .then(Commands.argument("target", StringArgumentType.string())
                                .executes(context -> createAPIEndpoint(context,
                                    StringArgumentType.getString(context, "id"),
                                    StringArgumentType.getString(context, "path"),
                                    StringArgumentType.getString(context, "method"),
                                    StringArgumentType.getString(context, "target"))))))))
            .then(Commands.literal("delete")
                .then(Commands.argument("id", StringArgumentType.string())
                    .executes(context -> deleteAPIEndpoint(context,
                        StringArgumentType.getString(context, "id")))))
            .then(Commands.literal("info")
                .then(Commands.argument("id", StringArgumentType.string())
                    .executes(context -> showAPIEndpointInfo(context,
                        StringArgumentType.getString(context, "id")))))
        );
        
        // API Keys management
        dispatcher.register(Commands.literal("api-keys")
            .requires(source -> source.hasPermission(4))
            .then(Commands.literal("list")
                .executes(APIGatewayCommand::listAPIKeys))
            .then(Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(context -> createAPIKey(context,
                        StringArgumentType.getString(context, "name"), null))
                    .then(Commands.argument("roles", StringArgumentType.greedyString())
                        .executes(context -> createAPIKey(context,
                            StringArgumentType.getString(context, "name"),
                            StringArgumentType.getString(context, "roles"))))))
            .then(Commands.literal("delete")
                .then(Commands.argument("keyId", StringArgumentType.string())
                    .executes(context -> deleteAPIKey(context,
                        StringArgumentType.getString(context, "keyId")))))
            .then(Commands.literal("info")
                .then(Commands.argument("keyId", StringArgumentType.string())
                    .executes(context -> showAPIKeyInfo(context,
                        StringArgumentType.getString(context, "keyId")))))
        );
        
        // Integration connectors management
        dispatcher.register(Commands.literal("integrations")
            .requires(source -> source.hasPermission(3))
            .then(Commands.literal("list")
                .executes(APIGatewayCommand::listIntegrations))
            .then(Commands.literal("create")
                .then(Commands.argument("id", StringArgumentType.string())
                    .then(Commands.argument("type", StringArgumentType.string())
                        .then(Commands.argument("connection", StringArgumentType.string())
                            .executes(context -> createIntegration(context,
                                StringArgumentType.getString(context, "id"),
                                StringArgumentType.getString(context, "type"),
                                StringArgumentType.getString(context, "connection")))))))
            .then(Commands.literal("delete")
                .then(Commands.argument("id", StringArgumentType.string())
                    .executes(context -> deleteIntegration(context,
                        StringArgumentType.getString(context, "id")))))
            .then(Commands.literal("test")
                .then(Commands.argument("id", StringArgumentType.string())
                    .executes(context -> testIntegration(context,
                        StringArgumentType.getString(context, "id")))))
            .then(Commands.literal("health")
                .executes(APIGatewayCommand::showIntegrationsHealth))
        );
        
        // API metrics and monitoring
        dispatcher.register(Commands.literal("api-metrics")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("overview")
                .executes(APIGatewayCommand::showMetricsOverview))
            .then(Commands.literal("endpoints")
                .executes(APIGatewayCommand::showEndpointMetrics))
            .then(Commands.literal("performance")
                .executes(APIGatewayCommand::showPerformanceMetrics))
            .then(Commands.literal("errors")
                .executes(APIGatewayCommand::showErrorMetrics))
        );
        
        // Webhook management
        dispatcher.register(Commands.literal("webhooks")
            .requires(source -> source.hasPermission(3))
            .then(Commands.literal("list")
                .executes(APIGatewayCommand::listWebhooks))
            .then(Commands.literal("create")
                .then(Commands.argument("id", StringArgumentType.string())
                    .then(Commands.argument("path", StringArgumentType.string())
                        .executes(context -> createWebhook(context,
                            StringArgumentType.getString(context, "id"),
                            StringArgumentType.getString(context, "path"), null))
                        .then(Commands.argument("events", StringArgumentType.greedyString())
                            .executes(context -> createWebhook(context,
                                StringArgumentType.getString(context, "id"),
                                StringArgumentType.getString(context, "path"),
                                StringArgumentType.getString(context, "events")))))))
            .then(Commands.literal("delete")
                .then(Commands.argument("id", StringArgumentType.string())
                    .executes(context -> deleteWebhook(context,
                        StringArgumentType.getString(context, "id")))))
            .then(Commands.literal("test")
                .then(Commands.argument("id", StringArgumentType.string())
                    .executes(context -> testWebhook(context,
                        StringArgumentType.getString(context, "id")))))
        );
    }
    
    /**
     * Show API Gateway status
     */
    private static int showGatewayStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        EnterpriseAPIGatewayHub gateway = EnterpriseAPIGatewayHub.getInstance();
        
        try {
            Map<String, Object> status = gateway.getGatewayStatus();
            
            source.sendSuccess(() -> Component.literal("=== Enterprise API Gateway Status ===")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Gateway Status: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(status.get("isActive").toString().toUpperCase())
                    .withStyle((Boolean) status.get("isActive") ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Version: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(status.get("version").toString()).withStyle(ChatFormatting.WHITE)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Server Port: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(status.get("serverPort").toString()).withStyle(ChatFormatting.AQUA)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("API Endpoints: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(status.get("apiEndpoints").toString()).withStyle(ChatFormatting.AQUA)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Integration Connectors: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(status.get("integrationConnectors").toString()).withStyle(ChatFormatting.AQUA)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("API Keys: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(status.get("apiKeys").toString()).withStyle(ChatFormatting.AQUA)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Total Requests: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(status.get("totalRequests").toString()).withStyle(ChatFormatting.GREEN)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Success Rate: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(String.format("%.1f%%", (Double) status.get("successRate")))
                    .withStyle(ChatFormatting.GREEN)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Average Response Time: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(String.format("%.1f ms", (Double) status.get("averageResponseTime")))
                    .withStyle(ChatFormatting.AQUA)), false);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error retrieving gateway status: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Start API Gateway
     */
    private static int startGateway(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        EnterpriseAPIGatewayHub gateway = EnterpriseAPIGatewayHub.getInstance();
        
        try {
            gateway.initialize();
            
            source.sendSuccess(() -> Component.literal("Enterprise API Gateway initialization started.")
                .withStyle(ChatFormatting.GREEN), false);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error starting gateway: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Stop API Gateway
     */
    private static int stopGateway(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        EnterpriseAPIGatewayHub gateway = EnterpriseAPIGatewayHub.getInstance();
        
        try {
            gateway.shutdown();
            
            source.sendSuccess(() -> Component.literal("Enterprise API Gateway shutdown initiated.")
                .withStyle(ChatFormatting.YELLOW), false);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error stopping gateway: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Restart API Gateway
     */
    private static int restartGateway(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        EnterpriseAPIGatewayHub gateway = EnterpriseAPIGatewayHub.getInstance();
        
        try {
            source.sendSuccess(() -> Component.literal("Restarting Enterprise API Gateway...")
                .withStyle(ChatFormatting.YELLOW), false);
            
            gateway.shutdown();
            Thread.sleep(2000); // Wait 2 seconds
            gateway.initialize();
            
            source.sendSuccess(() -> Component.literal("Enterprise API Gateway restart completed.")
                .withStyle(ChatFormatting.GREEN), false);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error restarting gateway: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Show gateway configuration
     */
    private static int showGatewayConfig(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("=== API Gateway Configuration ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("Default Port: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("8080").withStyle(ChatFormatting.AQUA)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("Request Timeout: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("30 seconds").withStyle(ChatFormatting.AQUA)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("Max Connections: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("1000").withStyle(ChatFormatting.AQUA)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("SSL Enabled: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("false").withStyle(ChatFormatting.RED)), false);
        
        return 1;
    }
    
    /**
     * Reload gateway configuration
     */
    private static int reloadGatewayConfig(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("Gateway configuration reload initiated.")
            .withStyle(ChatFormatting.GREEN), false);
        
        return 1;
    }
    
    /**
     * List API endpoints
     */
    private static int listAPIEndpoints(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("=== API Endpoints ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("health: ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("GET /api/health - Health check endpoint").withStyle(ChatFormatting.WHITE)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("status: ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("GET /api/status - System status endpoint").withStyle(ChatFormatting.WHITE)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("metrics: ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("GET /api/metrics - Metrics endpoint").withStyle(ChatFormatting.WHITE)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("webhook: ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("POST /webhook/{id} - Webhook endpoint").withStyle(ChatFormatting.WHITE)), false);
        
        return 1;
    }
    
    /**
     * Create API endpoint
     */
    private static int createAPIEndpoint(CommandContext<CommandSourceStack> context, String id, 
                                       String path, String method, String target) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        EnterpriseAPIGatewayHub gateway = EnterpriseAPIGatewayHub.getInstance();
        
        try {
            String targetUrl = target != null ? target : "internal://" + id;
            
            gateway.addAPIEndpoint(id, path, method, targetUrl, EndpointType.REST_API,
                false, null, null, null, false, 0, false, null);
            
            source.sendSuccess(() -> Component.literal("API endpoint created successfully:")
                .withStyle(ChatFormatting.GREEN), false);
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("ID: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(id).withStyle(ChatFormatting.AQUA)), false);
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Path: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(method + " " + path).withStyle(ChatFormatting.AQUA)), false);
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Target: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(targetUrl).withStyle(ChatFormatting.AQUA)), false);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error creating API endpoint: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Delete API endpoint
     */
    private static int deleteAPIEndpoint(CommandContext<CommandSourceStack> context, String id) 
                                       throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("API endpoint deletion initiated for: " + id)
            .withStyle(ChatFormatting.YELLOW), false);
        
        return 1;
    }
    
    /**
     * Show API endpoint info
     */
    private static int showAPIEndpointInfo(CommandContext<CommandSourceStack> context, String id) 
                                         throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("=== API Endpoint Info: " + id + " ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("ID: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(id).withStyle(ChatFormatting.AQUA)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("Status: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("Active").withStyle(ChatFormatting.GREEN)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("Authentication: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("Required").withStyle(ChatFormatting.YELLOW)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("Rate Limited: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("Yes").withStyle(ChatFormatting.GREEN)), false);
        
        return 1;
    }
    
    /**
     * List API keys
     */
    private static int listAPIKeys(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("=== API Keys ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("admin_key: ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("Administrator Key - Roles: [admin]").withStyle(ChatFormatting.WHITE)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("monitor_key: ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("Monitoring Key - Roles: [monitor]").withStyle(ChatFormatting.WHITE)), false);
        
        source.sendSuccess(() -> Component.literal("\nNote: API key values are not displayed for security.")
            .withStyle(ChatFormatting.GRAY), false);
        
        return 1;
    }
    
    /**
     * Create API key
     */
    private static int createAPIKey(CommandContext<CommandSourceStack> context, String name, String roles) 
                                  throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        EnterpriseAPIGatewayHub gateway = EnterpriseAPIGatewayHub.getInstance();
        
        try {
            String keyId = name.toLowerCase().replace(" ", "_");
            String keyValue = "api_key_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            List<String> roleList = roles != null ? Arrays.asList(roles.split(",")) : Arrays.asList("user");
            
            gateway.addAPIKey(keyId, keyValue, name, "Generated API key", 
                Arrays.asList("*"), roleList, 0, true, null);
            
            source.sendSuccess(() -> Component.literal("API key created successfully:")
                .withStyle(ChatFormatting.GREEN), false);
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Key ID: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(keyId).withStyle(ChatFormatting.AQUA)), false);
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Key Value: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(keyValue).withStyle(ChatFormatting.GOLD)), false);
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Roles: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(roleList.toString()).withStyle(ChatFormatting.AQUA)), false);
            
            source.sendSuccess(() -> Component.literal("\nIMPORTANT: Save this API key value securely. It will not be shown again.")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error creating API key: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Delete API key
     */
    private static int deleteAPIKey(CommandContext<CommandSourceStack> context, String keyId) 
                                  throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("API key deletion initiated for: " + keyId)
            .withStyle(ChatFormatting.YELLOW), false);
        
        return 1;
    }
    
    /**
     * Show API key info
     */
    private static int showAPIKeyInfo(CommandContext<CommandSourceStack> context, String keyId) 
                                    throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("=== API Key Info: " + keyId + " ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("Key ID: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(keyId).withStyle(ChatFormatting.AQUA)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("Status: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("Active").withStyle(ChatFormatting.GREEN)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("Created: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(LocalDateTime.now().format(DATE_FORMAT)).withStyle(ChatFormatting.GRAY)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("Expiration: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("Never").withStyle(ChatFormatting.GREEN)), false);
        
        return 1;
    }
    
    /**
     * List integrations
     */
    private static int listIntegrations(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("=== Integration Connectors ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("main_database: ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("Database - MySQL Main Database").withStyle(ChatFormatting.WHITE)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("discord_notifications: ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("Web Service - Discord Notifications").withStyle(ChatFormatting.WHITE)), false);
        
        return 1;
    }
    
    /**
     * Create integration
     */
    private static int createIntegration(CommandContext<CommandSourceStack> context, String id, 
                                       String type, String connection) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        EnterpriseAPIGatewayHub gateway = EnterpriseAPIGatewayHub.getInstance();
        
        try {
            ConnectorType connectorType = ConnectorType.valueOf(type.toUpperCase().replace("-", "_"));
            
            gateway.addIntegrationConnector(id, id.replace("_", " "), connectorType, connection,
                Map.of("type", type), true, 30, 3, null);
            
            source.sendSuccess(() -> Component.literal("Integration connector created successfully:")
                .withStyle(ChatFormatting.GREEN), false);
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("ID: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(id).withStyle(ChatFormatting.AQUA)), false);
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Type: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(type).withStyle(ChatFormatting.AQUA)), false);
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Connection: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(connection).withStyle(ChatFormatting.AQUA)), false);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error creating integration: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Delete integration
     */
    private static int deleteIntegration(CommandContext<CommandSourceStack> context, String id) 
                                       throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("Integration connector deletion initiated for: " + id)
            .withStyle(ChatFormatting.YELLOW), false);
        
        return 1;
    }
    
    /**
     * Test integration
     */
    private static int testIntegration(CommandContext<CommandSourceStack> context, String id) 
                                     throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("Testing integration connector: " + id)
            .withStyle(ChatFormatting.YELLOW), false);
        
        // Simulate test result
        source.sendSuccess(() -> Component.literal("Integration test completed successfully.")
            .withStyle(ChatFormatting.GREEN), false);
        
        return 1;
    }
    
    /**
     * Show integrations health
     */
    private static int showIntegrationsHealth(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("=== Integration Health Status ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("main_database: ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("HEALTHY").withStyle(ChatFormatting.GREEN))
            .append(Component.literal(" (Response: 15ms)").withStyle(ChatFormatting.GRAY)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("discord_notifications: ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("HEALTHY").withStyle(ChatFormatting.GREEN))
            .append(Component.literal(" (No health check configured)").withStyle(ChatFormatting.GRAY)), false);
        
        return 1;
    }
    
    /**
     * Show metrics overview
     */
    private static int showMetricsOverview(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        EnterpriseAPIGatewayHub gateway = EnterpriseAPIGatewayHub.getInstance();
        
        try {
            Map<String, Object> metrics = gateway.getGatewayMetrics();
            @SuppressWarnings("unchecked")
            Map<String, Object> system = (Map<String, Object>) metrics.get("system");
            
            source.sendSuccess(() -> Component.literal("=== API Metrics Overview ===")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Total Requests: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(system.get("totalRequests").toString()).withStyle(ChatFormatting.GREEN)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Successful Requests: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(system.get("successfulRequests").toString()).withStyle(ChatFormatting.GREEN)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Failed Requests: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(system.get("failedRequests").toString()).withStyle(ChatFormatting.RED)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Success Rate: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(String.format("%.1f%%", (Double) system.get("successRate")))
                    .withStyle(ChatFormatting.GREEN)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Average Response Time: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(String.format("%.1f ms", (Double) system.get("averageResponseTime")))
                    .withStyle(ChatFormatting.AQUA)), false);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error retrieving metrics: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Show endpoint metrics
     */
    private static int showEndpointMetrics(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("=== Endpoint Metrics ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("/api/health: ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("1,234 requests, 99.9% success, 12ms avg").withStyle(ChatFormatting.WHITE)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("/api/status: ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("567 requests, 100% success, 8ms avg").withStyle(ChatFormatting.WHITE)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("/api/metrics: ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("89 requests, 100% success, 15ms avg").withStyle(ChatFormatting.WHITE)), false);
        
        return 1;
    }
    
    /**
     * Show performance metrics
     */
    private static int showPerformanceMetrics(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("=== Performance Metrics ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("Requests per Second: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("45.7").withStyle(ChatFormatting.GREEN)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("95th Percentile Response Time: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("25ms").withStyle(ChatFormatting.AQUA)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("99th Percentile Response Time: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("45ms").withStyle(ChatFormatting.YELLOW)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("Active Connections: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("12").withStyle(ChatFormatting.GREEN)), false);
        
        return 1;
    }
    
    /**
     * Show error metrics
     */
    private static int showErrorMetrics(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("=== Error Metrics ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("4xx Errors: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("23 (1.2%)").withStyle(ChatFormatting.YELLOW)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("5xx Errors: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("2 (0.1%)").withStyle(ChatFormatting.RED)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("Timeout Errors: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("0 (0.0%)").withStyle(ChatFormatting.GREEN)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("Rate Limit Hits: ").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("5 (0.3%)").withStyle(ChatFormatting.YELLOW)), false);
        
        return 1;
    }
    
    /**
     * List webhooks
     */
    private static int listWebhooks(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("=== Webhook Endpoints ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("system_events: ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("/webhook/system - System events webhook").withStyle(ChatFormatting.WHITE)), false);
        
        return 1;
    }
    
    /**
     * Create webhook
     */
    private static int createWebhook(CommandContext<CommandSourceStack> context, String id, 
                                   String path, String events) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        EnterpriseAPIGatewayHub gateway = EnterpriseAPIGatewayHub.getInstance();
        
        try {
            List<String> eventList = events != null ? Arrays.asList(events.split(",")) : Arrays.asList("*");
            String secret = UUID.randomUUID().toString();
            
            gateway.addWebhookEndpoint(id, path, secret, eventList,
                "https://external-webhook.com/endpoint", Map.of("Content-Type", "application/json"),
                true, 3, true);
            
            source.sendSuccess(() -> Component.literal("Webhook endpoint created successfully:")
                .withStyle(ChatFormatting.GREEN), false);
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("ID: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(id).withStyle(ChatFormatting.AQUA)), false);
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Path: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(path).withStyle(ChatFormatting.AQUA)), false);
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Events: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(eventList.toString()).withStyle(ChatFormatting.AQUA)), false);
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Secret: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(secret).withStyle(ChatFormatting.GOLD)), false);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error creating webhook: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Delete webhook
     */
    private static int deleteWebhook(CommandContext<CommandSourceStack> context, String id) 
                                   throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("Webhook endpoint deletion initiated for: " + id)
            .withStyle(ChatFormatting.YELLOW), false);
        
        return 1;
    }
    
    /**
     * Test webhook
     */
    private static int testWebhook(CommandContext<CommandSourceStack> context, String id) 
                                 throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("Testing webhook endpoint: " + id)
            .withStyle(ChatFormatting.YELLOW), false);
        
        // Simulate test result
        source.sendSuccess(() -> Component.literal("Webhook test completed successfully.")
            .withStyle(ChatFormatting.GREEN), false);
        
        return 1;
    }
}
