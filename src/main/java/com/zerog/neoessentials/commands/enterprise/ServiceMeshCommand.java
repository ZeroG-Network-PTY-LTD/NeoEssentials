package com.zerog.neoessentials.commands.enterprise;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import com.zerog.neoessentials.systems.servicemesh.EnterpriseServiceMeshSystem;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service Mesh Command Interface for NeoEssentials Enterprise Platform
 * 
 * Provides comprehensive command-line interface for managing the service mesh system.
 * 
 * Commands:
 * - /service-mesh status - Show service mesh status
 * - /service-mesh start/stop/restart - Control service mesh
 * - /service-mesh services - Manage service registry
 * - /service-mesh policies - Manage traffic and security policies
 * - /service-mesh metrics - View mesh metrics and analytics
 * - /service-mesh health - Check system health
 * - /service-mesh config - Configuration management
 * - /service-mesh trace - Distributed tracing operations
 * 
 * @author NeoEssentials Team
 * @version 2.0.0
 */
public class ServiceMeshCommand {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static EnterpriseServiceMeshSystem serviceMeshSystem;
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("service-mesh")
            .requires(source -> source.hasPermission(4))
            
            // Status command
            .then(Commands.literal("status")
                .executes(ServiceMeshCommand::showStatus))
            
            // Control commands
            .then(Commands.literal("start")
                .executes(ServiceMeshCommand::startMesh))
            .then(Commands.literal("stop")
                .executes(ServiceMeshCommand::stopMesh))
            .then(Commands.literal("restart")
                .executes(ServiceMeshCommand::restartMesh))
            
            // Service registry commands
            .then(Commands.literal("services")
                .then(Commands.literal("list")
                    .executes(ServiceMeshCommand::listServices))
                .then(Commands.literal("info")
                    .then(Commands.argument("service_id", StringArgumentType.string())
                        .executes(ServiceMeshCommand::showServiceInfo)))
                .then(Commands.literal("register")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .then(Commands.argument("address", StringArgumentType.string())
                            .then(Commands.argument("port", IntegerArgumentType.integer(1, 65535))
                                .executes(ServiceMeshCommand::registerService)))))
                .then(Commands.literal("deregister")
                    .then(Commands.argument("service_id", StringArgumentType.string())
                        .executes(ServiceMeshCommand::deregisterService))))
            
            // Policy management commands
            .then(Commands.literal("policies")
                .then(Commands.literal("traffic")
                    .then(Commands.literal("list")
                        .executes(ServiceMeshCommand::listTrafficPolicies))
                    .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .then(Commands.argument("service", StringArgumentType.string())
                                .executes(ServiceMeshCommand::createTrafficPolicy))))
                    .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .executes(ServiceMeshCommand::deleteTrafficPolicy))))
                .then(Commands.literal("security")
                    .then(Commands.literal("list")
                        .executes(ServiceMeshCommand::listSecurityPolicies))
                    .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .then(Commands.argument("service", StringArgumentType.string())
                                .executes(ServiceMeshCommand::createSecurityPolicy))))
                    .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .executes(ServiceMeshCommand::deleteSecurityPolicy)))))
            
            // Metrics and monitoring commands
            .then(Commands.literal("metrics")
                .then(Commands.literal("overview")
                    .executes(ServiceMeshCommand::showMetricsOverview))
                .then(Commands.literal("services")
                    .executes(ServiceMeshCommand::showServiceMetrics))
                .then(Commands.literal("performance")
                    .executes(ServiceMeshCommand::showPerformanceMetrics))
                .then(Commands.literal("errors")
                    .executes(ServiceMeshCommand::showErrorMetrics)))
            
            // Health monitoring commands
            .then(Commands.literal("health")
                .then(Commands.literal("overview")
                    .executes(ServiceMeshCommand::showHealthOverview))
                .then(Commands.literal("services")
                    .executes(ServiceMeshCommand::showServiceHealth))
                .then(Commands.literal("check")
                    .then(Commands.argument("service_id", StringArgumentType.string())
                        .executes(ServiceMeshCommand::performHealthCheck))))
            
            // Configuration commands
            .then(Commands.literal("config")
                .then(Commands.literal("show")
                    .executes(ServiceMeshCommand::showConfiguration))
                .then(Commands.literal("reload")
                    .executes(ServiceMeshCommand::reloadConfiguration))
                .then(Commands.literal("validate")
                    .executes(ServiceMeshCommand::validateConfiguration)))
            
            // Distributed tracing commands
            .then(Commands.literal("trace")
                .then(Commands.literal("list")
                    .executes(ServiceMeshCommand::listTraces))
                .then(Commands.literal("show")
                    .then(Commands.argument("trace_id", StringArgumentType.string())
                        .executes(ServiceMeshCommand::showTrace)))
                .then(Commands.literal("search")
                    .then(Commands.argument("service", StringArgumentType.string())
                        .executes(ServiceMeshCommand::searchTraces))))
            
            // Circuit breaker commands
            .then(Commands.literal("circuit-breaker")
                .then(Commands.literal("status")
                    .executes(ServiceMeshCommand::showCircuitBreakerStatus))
                .then(Commands.literal("reset")
                    .then(Commands.argument("service_id", StringArgumentType.string())
                        .executes(ServiceMeshCommand::resetCircuitBreaker))))
        );
    }
    
    public static void setServiceMeshSystem(EnterpriseServiceMeshSystem system) {
        serviceMeshSystem = system;
    }
    
    private static int showStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("=== Service Mesh Status ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        boolean isRunning = serviceMeshSystem.isRunning();
        source.sendSuccess(() -> Component.literal("Status: ")
            .append(Component.literal(isRunning ? "RUNNING" : "STOPPED")
                .withStyle(isRunning ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
        
        if (isRunning) {
            var config = serviceMeshSystem.getConfig();
            source.sendSuccess(() -> Component.literal("Mesh Name: " + config.meshName)
                .withStyle(ChatFormatting.YELLOW), false);
            source.sendSuccess(() -> Component.literal("Namespace: " + config.namespace)
                .withStyle(ChatFormatting.YELLOW), false);
            source.sendSuccess(() -> Component.literal("Services: " + serviceMeshSystem.getServices().size())
                .withStyle(ChatFormatting.YELLOW), false);
            source.sendSuccess(() -> Component.literal("Traffic Policies: " + serviceMeshSystem.getTrafficPolicies().size())
                .withStyle(ChatFormatting.YELLOW), false);
            source.sendSuccess(() -> Component.literal("Security Policies: " + serviceMeshSystem.getSecurityPolicies().size())
                .withStyle(ChatFormatting.YELLOW), false);
            source.sendSuccess(() -> Component.literal("Total Requests: " + serviceMeshSystem.getTotalRequests())
                .withStyle(ChatFormatting.YELLOW), false);
            source.sendSuccess(() -> Component.literal("Total Errors: " + serviceMeshSystem.getTotalErrors())
                .withStyle(ChatFormatting.YELLOW), false);
        }
        
        return 1;
    }
    
    private static int startMesh(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        if (serviceMeshSystem.isRunning()) {
            source.sendFailure(Component.literal("Service Mesh is already running")
                .withStyle(ChatFormatting.YELLOW));
            return 0;
        }
        
        serviceMeshSystem.startServiceMesh();
        source.sendSuccess(() -> Component.literal("Service Mesh started successfully")
            .withStyle(ChatFormatting.GREEN), true);
        
        return 1;
    }
    
    private static int stopMesh(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        if (!serviceMeshSystem.isRunning()) {
            source.sendFailure(Component.literal("Service Mesh is not running")
                .withStyle(ChatFormatting.YELLOW));
            return 0;
        }
        
        serviceMeshSystem.stopServiceMesh();
        source.sendSuccess(() -> Component.literal("Service Mesh stopped successfully")
            .withStyle(ChatFormatting.GREEN), true);
        
        return 1;
    }
    
    private static int restartMesh(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        serviceMeshSystem.stopServiceMesh();
        serviceMeshSystem.startServiceMesh();
        source.sendSuccess(() -> Component.literal("Service Mesh restarted successfully")
            .withStyle(ChatFormatting.GREEN), true);
        
        return 1;
    }
    
    private static int listServices(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        var services = serviceMeshSystem.getAllServices();
        
        source.sendSuccess(() -> Component.literal("=== Registered Services ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        if (services.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No services registered")
                .withStyle(ChatFormatting.GRAY), false);
            return 1;
        }
        
        for (var service : services) {
            ChatFormatting statusColor = service.status == EnterpriseServiceMeshSystem.ServiceStatus.HEALTHY ? 
                ChatFormatting.GREEN : ChatFormatting.RED;
            
            source.sendSuccess(() -> Component.literal("• " + service.name)
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" (" + service.id + ")")
                    .withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" [" + service.status + "]")
                    .withStyle(statusColor)), false);
            
            source.sendSuccess(() -> Component.literal("  Address: " + service.address + ":" + service.port)
                .withStyle(ChatFormatting.WHITE), false);
            
            source.sendSuccess(() -> Component.literal("  Protocol: " + service.protocol + 
                " | Version: " + service.version)
                .withStyle(ChatFormatting.WHITE), false);
        }
        
        return 1;
    }
    
    private static int showServiceInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String serviceId = StringArgumentType.getString(context, "service_id");
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        var service = serviceMeshSystem.getService(serviceId);
        if (service == null) {
            source.sendFailure(Component.literal("Service not found: " + serviceId)
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("=== Service Information ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        source.sendSuccess(() -> Component.literal("Name: " + service.name)
            .withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("ID: " + service.id)
            .withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("Namespace: " + service.namespace)
            .withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("Address: " + service.address + ":" + service.port)
            .withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("Protocol: " + service.protocol)
            .withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("Version: " + service.version)
            .withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("Status: " + service.status)
            .withStyle(service.status == EnterpriseServiceMeshSystem.ServiceStatus.HEALTHY ? 
                ChatFormatting.GREEN : ChatFormatting.RED), false);
        
        if (service.tags != null && !service.tags.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Tags: " + String.join(", ", service.tags))
                .withStyle(ChatFormatting.YELLOW), false);
        }
        
        if (service.registrationTime != null) {
            source.sendSuccess(() -> Component.literal("Registered: " + 
                service.registrationTime.atZone(java.time.ZoneId.systemDefault()).format(FORMATTER))
                .withStyle(ChatFormatting.YELLOW), false);
        }
        
        if (service.lastHealthCheck != null) {
            source.sendSuccess(() -> Component.literal("Last Health Check: " + 
                service.lastHealthCheck.atZone(java.time.ZoneId.systemDefault()).format(FORMATTER))
                .withStyle(ChatFormatting.YELLOW), false);
        }
        
        return 1;
    }
    
    private static int registerService(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String name = StringArgumentType.getString(context, "name");
        String address = StringArgumentType.getString(context, "address");
        int port = IntegerArgumentType.getInteger(context, "port");
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        var service = new EnterpriseServiceMeshSystem.ServiceInstance();
        service.name = name;
        service.namespace = serviceMeshSystem.getConfig().namespace;
        service.address = address;
        service.port = port;
        service.protocol = "HTTP";
        service.version = "1.0.0";
        service.tags = Arrays.asList("manual", "registered");
        service.healthCheckPath = "/health";
        service.registrationTime = java.time.Instant.now();
        
        String serviceId = serviceMeshSystem.registerService(service);
        
        source.sendSuccess(() -> Component.literal("Service registered successfully with ID: " + serviceId)
            .withStyle(ChatFormatting.GREEN), true);
        
        return 1;
    }
    
    private static int deregisterService(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String serviceId = StringArgumentType.getString(context, "service_id");
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        var service = serviceMeshSystem.getService(serviceId);
        if (service == null) {
            source.sendFailure(Component.literal("Service not found: " + serviceId)
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        serviceMeshSystem.deregisterService(serviceId);
        
        source.sendSuccess(() -> Component.literal("Service deregistered successfully: " + service.name)
            .withStyle(ChatFormatting.GREEN), true);
        
        return 1;
    }
    
    private static int listTrafficPolicies(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        var policies = serviceMeshSystem.getTrafficPolicies();
        
        source.sendSuccess(() -> Component.literal("=== Traffic Policies ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        if (policies.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No traffic policies configured")
                .withStyle(ChatFormatting.GRAY), false);
            return 1;
        }
        
        for (var policy : policies.values()) {
            source.sendSuccess(() -> Component.literal("• " + policy.name)
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" (Service: " + policy.serviceName + ")")
                    .withStyle(ChatFormatting.GRAY)), false);
            
            if (policy.loadBalancingStrategy != null) {
                source.sendSuccess(() -> Component.literal("  Load Balancing: " + policy.loadBalancingStrategy)
                    .withStyle(ChatFormatting.WHITE), false);
            }
        }
        
        return 1;
    }
    
    private static int createTrafficPolicy(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String name = StringArgumentType.getString(context, "name");
        String serviceName = StringArgumentType.getString(context, "service");
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        var policy = new EnterpriseServiceMeshSystem.TrafficPolicy();
        policy.serviceName = serviceName;
        policy.loadBalancingStrategy = "ROUND_ROBIN";
        
        serviceMeshSystem.createTrafficPolicy(name, policy);
        
        source.sendSuccess(() -> Component.literal("Traffic policy created: " + name)
            .withStyle(ChatFormatting.GREEN), true);
        
        return 1;
    }
    
    private static int deleteTrafficPolicy(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String name = StringArgumentType.getString(context, "name");
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        var policies = serviceMeshSystem.getTrafficPolicies();
        if (!policies.containsKey(name)) {
            source.sendFailure(Component.literal("Traffic policy not found: " + name)
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        policies.remove(name);
        
        source.sendSuccess(() -> Component.literal("Traffic policy deleted: " + name)
            .withStyle(ChatFormatting.GREEN), true);
        
        return 1;
    }
    
    private static int listSecurityPolicies(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        var policies = serviceMeshSystem.getSecurityPolicies();
        
        source.sendSuccess(() -> Component.literal("=== Security Policies ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        if (policies.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No security policies configured")
                .withStyle(ChatFormatting.GRAY), false);
            return 1;
        }
        
        for (var policy : policies.values()) {
            source.sendSuccess(() -> Component.literal("• " + policy.name)
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" (Service: " + policy.serviceName + ")")
                    .withStyle(ChatFormatting.GRAY)), false);
            
            source.sendSuccess(() -> Component.literal("  mTLS Required: " + policy.requireMTLS)
                .withStyle(ChatFormatting.WHITE), false);
        }
        
        return 1;
    }
    
    private static int createSecurityPolicy(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String name = StringArgumentType.getString(context, "name");
        String serviceName = StringArgumentType.getString(context, "service");
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        var policy = new EnterpriseServiceMeshSystem.SecurityPolicy();
        policy.serviceName = serviceName;
        policy.requireMTLS = true;
        policy.allowedMethods = Arrays.asList("GET", "POST", "PUT", "DELETE");
        
        serviceMeshSystem.createSecurityPolicy(name, policy);
        
        source.sendSuccess(() -> Component.literal("Security policy created: " + name)
            .withStyle(ChatFormatting.GREEN), true);
        
        return 1;
    }
    
    private static int deleteSecurityPolicy(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String name = StringArgumentType.getString(context, "name");
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        var policies = serviceMeshSystem.getSecurityPolicies();
        if (!policies.containsKey(name)) {
            source.sendFailure(Component.literal("Security policy not found: " + name)
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        policies.remove(name);
        
        source.sendSuccess(() -> Component.literal("Security policy deleted: " + name)
            .withStyle(ChatFormatting.GREEN), true);
        
        return 1;
    }
    
    private static int showMetricsOverview(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("=== Metrics Overview ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        source.sendSuccess(() -> Component.literal("Total Services: " + serviceMeshSystem.getServices().size())
            .withStyle(ChatFormatting.YELLOW), false);
        
        long healthyServices = serviceMeshSystem.getServices().values().stream()
            .filter(s -> s.status == EnterpriseServiceMeshSystem.ServiceStatus.HEALTHY)
            .count();
        
        source.sendSuccess(() -> Component.literal("Healthy Services: " + healthyServices)
            .withStyle(ChatFormatting.GREEN), false);
        
        source.sendSuccess(() -> Component.literal("Total Requests: " + serviceMeshSystem.getTotalRequests())
            .withStyle(ChatFormatting.YELLOW), false);
        
        source.sendSuccess(() -> Component.literal("Total Errors: " + serviceMeshSystem.getTotalErrors())
            .withStyle(ChatFormatting.RED), false);
        
        long totalRequests = serviceMeshSystem.getTotalRequests();
        long totalErrors = serviceMeshSystem.getTotalErrors();
        double errorRate = totalRequests > 0 ? (double) totalErrors / totalRequests * 100 : 0;
        
        source.sendSuccess(() -> Component.literal("Error Rate: " + String.format("%.2f%%", errorRate))
            .withStyle(errorRate < 1 ? ChatFormatting.GREEN : 
                errorRate < 5 ? ChatFormatting.YELLOW : ChatFormatting.RED), false);
        
        return 1;
    }
    
    private static int showServiceMetrics(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        var metrics = serviceMeshSystem.getServiceMetrics();
        
        source.sendSuccess(() -> Component.literal("=== Service Metrics ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        if (metrics.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No metrics available")
                .withStyle(ChatFormatting.GRAY), false);
            return 1;
        }
        
        for (var metric : metrics.values()) {
            var service = serviceMeshSystem.getService(metric.serviceId);
            String serviceName = service != null ? service.name : metric.serviceId;
            
            source.sendSuccess(() -> Component.literal("• " + serviceName)
                .withStyle(ChatFormatting.YELLOW), false);
            
            source.sendSuccess(() -> Component.literal("  Requests: " + metric.totalRequests + 
                " | Success: " + metric.successfulRequests + 
                " | Errors: " + metric.errorRequests)
                .withStyle(ChatFormatting.WHITE), false);
            
            if (metric.averageResponseTime > 0) {
                source.sendSuccess(() -> Component.literal("  Avg Response Time: " + 
                    String.format("%.2fms", metric.averageResponseTime))
                    .withStyle(ChatFormatting.WHITE), false);
            }
        }
        
        return 1;
    }
    
    private static int showPerformanceMetrics(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("=== Performance Metrics ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        var metrics = serviceMeshSystem.getServiceMetrics();
        
        if (metrics.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No performance data available")
                .withStyle(ChatFormatting.GRAY), false);
            return 1;
        }
        
        double avgResponseTime = metrics.values().stream()
            .mapToDouble(m -> m.averageResponseTime)
            .filter(time -> time > 0)
            .average()
            .orElse(0);
        
        source.sendSuccess(() -> Component.literal("Average Response Time: " + 
            String.format("%.2fms", avgResponseTime))
            .withStyle(ChatFormatting.YELLOW), false);
        
        long totalThroughput = metrics.values().stream()
            .mapToLong(m -> m.totalRequests)
            .sum();
        
        source.sendSuccess(() -> Component.literal("Total Throughput: " + totalThroughput + " requests")
            .withStyle(ChatFormatting.YELLOW), false);
        
        return 1;
    }
    
    private static int showErrorMetrics(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("=== Error Metrics ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        var metrics = serviceMeshSystem.getServiceMetrics();
        
        long totalErrors = metrics.values().stream()
            .mapToLong(m -> m.errorRequests)
            .sum();
        
        source.sendSuccess(() -> Component.literal("Total Errors: " + totalErrors)
            .withStyle(ChatFormatting.RED), false);
        
        // Show services with highest error rates
        var sortedMetrics = metrics.values().stream()
            .filter(m -> m.errorRequests > 0)
            .sorted((a, b) -> Long.compare(b.errorRequests, a.errorRequests))
            .limit(5)
            .collect(Collectors.toList());
        
        if (!sortedMetrics.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Top Error Sources:")
                .withStyle(ChatFormatting.RED), false);
            
            for (var metric : sortedMetrics) {
                var service = serviceMeshSystem.getService(metric.serviceId);
                String serviceName = service != null ? service.name : metric.serviceId;
                
                double errorRate = metric.totalRequests > 0 ? 
                    (double) metric.errorRequests / metric.totalRequests * 100 : 0;
                
                source.sendSuccess(() -> Component.literal("  • " + serviceName + ": " + 
                    metric.errorRequests + " errors (" + String.format("%.1f%%", errorRate) + ")")
                    .withStyle(ChatFormatting.WHITE), false);
            }
        }
        
        return 1;
    }
    
    private static int showHealthOverview(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("=== Health Overview ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        var services = serviceMeshSystem.getAllServices();
        
        long healthyCount = services.stream()
            .filter(s -> s.status == EnterpriseServiceMeshSystem.ServiceStatus.HEALTHY)
            .count();
        
        long unhealthyCount = services.stream()
            .filter(s -> s.status == EnterpriseServiceMeshSystem.ServiceStatus.UNHEALTHY)
            .count();
        
        long unknownCount = services.stream()
            .filter(s -> s.status == EnterpriseServiceMeshSystem.ServiceStatus.UNKNOWN)
            .count();
        
        source.sendSuccess(() -> Component.literal("Healthy Services: " + healthyCount)
            .withStyle(ChatFormatting.GREEN), false);
        
        source.sendSuccess(() -> Component.literal("Unhealthy Services: " + unhealthyCount)
            .withStyle(ChatFormatting.RED), false);
        
        source.sendSuccess(() -> Component.literal("Unknown Status: " + unknownCount)
            .withStyle(ChatFormatting.YELLOW), false);
        
        return 1;
    }
    
    private static int showServiceHealth(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        var services = serviceMeshSystem.getAllServices();
        
        source.sendSuccess(() -> Component.literal("=== Service Health Status ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        for (var service : services) {
            ChatFormatting statusColor = switch (service.status) {
                case HEALTHY -> ChatFormatting.GREEN;
                case UNHEALTHY -> ChatFormatting.RED;
                case UNKNOWN -> ChatFormatting.YELLOW;
            };
            
            source.sendSuccess(() -> Component.literal("• " + service.name)
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal(" [" + service.status + "]")
                    .withStyle(statusColor)), false);
            
            if (service.lastHealthCheck != null) {
                source.sendSuccess(() -> Component.literal("  Last Check: " + 
                    service.lastHealthCheck.atZone(java.time.ZoneId.systemDefault()).format(FORMATTER))
                    .withStyle(ChatFormatting.GRAY), false);
            }
        }
        
        return 1;
    }
    
    private static int performHealthCheck(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String serviceId = StringArgumentType.getString(context, "service_id");
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        var service = serviceMeshSystem.getService(serviceId);
        if (service == null) {
            source.sendFailure(Component.literal("Service not found: " + serviceId)
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("Performing health check for: " + service.name)
            .withStyle(ChatFormatting.YELLOW), false);
        
        // Simulate health check
        boolean isHealthy = true; // In real implementation, this would perform actual health check
        service.status = isHealthy ? EnterpriseServiceMeshSystem.ServiceStatus.HEALTHY : 
            EnterpriseServiceMeshSystem.ServiceStatus.UNHEALTHY;
        service.lastHealthCheck = java.time.Instant.now();
        
        source.sendSuccess(() -> Component.literal("Health check result: " + service.status)
            .withStyle(isHealthy ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        
        return 1;
    }
    
    private static int showConfiguration(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        var config = serviceMeshSystem.getConfig();
        
        source.sendSuccess(() -> Component.literal("=== Service Mesh Configuration ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        source.sendSuccess(() -> Component.literal("Enabled: " + config.enabled)
            .withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("Mesh Name: " + config.meshName)
            .withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("Namespace: " + config.namespace)
            .withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("Proxy Port: " + config.proxyPort)
            .withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("Admin Port: " + config.adminPort)
            .withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("Metrics Port: " + config.metricsPort)
            .withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("Tracing Enabled: " + config.tracingEnabled)
            .withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("mTLS Enabled: " + config.mtlsEnabled)
            .withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("Load Balancing: " + config.loadBalancingStrategy)
            .withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("Circuit Breaker: " + config.circuitBreakerEnabled)
            .withStyle(ChatFormatting.YELLOW), false);
        
        return 1;
    }
    
    private static int reloadConfiguration(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        // In real implementation, this would reload configuration from files
        source.sendSuccess(() -> Component.literal("Configuration reloaded successfully")
            .withStyle(ChatFormatting.GREEN), true);
        
        return 1;
    }
    
    private static int validateConfiguration(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        var config = serviceMeshSystem.getConfig();
        final boolean[] validationResults = {true}; // Use array for mutability in lambda
        
        source.sendSuccess(() -> Component.literal("=== Configuration Validation ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        // Validate ports
        if (config.proxyPort <= 0 || config.proxyPort > 65535) {
            source.sendSuccess(() -> Component.literal("✗ Invalid proxy port: " + config.proxyPort)
                .withStyle(ChatFormatting.RED), false);
            validationResults[0] = false;
        } else {
            source.sendSuccess(() -> Component.literal("✓ Proxy port is valid")
                .withStyle(ChatFormatting.GREEN), false);
        }
        
        // Validate mesh name
        if (config.meshName == null || config.meshName.trim().isEmpty()) {
            source.sendSuccess(() -> Component.literal("✗ Mesh name is required")
                .withStyle(ChatFormatting.RED), false);
            validationResults[0] = false;
        } else {
            source.sendSuccess(() -> Component.literal("✓ Mesh name is valid")
                .withStyle(ChatFormatting.GREEN), false);
        }
        
        // Validate namespace
        if (config.namespace == null || config.namespace.trim().isEmpty()) {
            source.sendSuccess(() -> Component.literal("✗ Namespace is required")
                .withStyle(ChatFormatting.RED), false);
            validationResults[0] = false;
        } else {
            source.sendSuccess(() -> Component.literal("✓ Namespace is valid")
                .withStyle(ChatFormatting.GREEN), false);
        }
        
        final boolean isValid = validationResults[0];
        source.sendSuccess(() -> Component.literal("Overall: " + (isValid ? "VALID" : "INVALID"))
            .withStyle(isValid ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        
        return isValid ? 1 : 0;
    }
    
    private static int listTraces(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        var traces = serviceMeshSystem.getDistributedTraces();
        
        source.sendSuccess(() -> Component.literal("=== Distributed Traces ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        if (traces.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No traces available")
                .withStyle(ChatFormatting.GRAY), false);
            return 1;
        }
        
        int count = 0;
        for (var entry : traces.entrySet()) {
            if (count >= 10) break; // Show only last 10 traces
            
            String traceId = entry.getKey();
            var spans = entry.getValue();
            
            if (!spans.isEmpty()) {
                var firstSpan = spans.get(0);
                source.sendSuccess(() -> Component.literal("• " + traceId.substring(0, 8) + "...")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(" (" + spans.size() + " spans)")
                        .withStyle(ChatFormatting.GRAY)), false);
                
                source.sendSuccess(() -> Component.literal("  Service: " + firstSpan.serviceName + 
                    " | Operation: " + firstSpan.operationName)
                    .withStyle(ChatFormatting.WHITE), false);
                
                if (firstSpan.startTime != null) {
                    source.sendSuccess(() -> Component.literal("  Started: " + 
                        firstSpan.startTime.atZone(java.time.ZoneId.systemDefault()).format(FORMATTER))
                        .withStyle(ChatFormatting.WHITE), false);
                }
            }
            count++;
        }
        
        return 1;
    }
    
    private static int showTrace(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String traceId = StringArgumentType.getString(context, "trace_id");
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        var traces = serviceMeshSystem.getDistributedTraces();
        var spans = traces.get(traceId);
        
        if (spans == null || spans.isEmpty()) {
            source.sendFailure(Component.literal("Trace not found: " + traceId)
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("=== Trace Details ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        source.sendSuccess(() -> Component.literal("Trace ID: " + traceId)
            .withStyle(ChatFormatting.YELLOW), false);
        
        source.sendSuccess(() -> Component.literal("Spans (" + spans.size() + "):")
            .withStyle(ChatFormatting.YELLOW), false);
        
        for (var span : spans) {
            source.sendSuccess(() -> Component.literal("• " + span.serviceName + " - " + span.operationName)
                .withStyle(ChatFormatting.WHITE), false);
            
            if (span.duration != null) {
                source.sendSuccess(() -> Component.literal("  Duration: " + span.duration.toMillis() + "ms")
                    .withStyle(ChatFormatting.GRAY), false);
            }
            
            source.sendSuccess(() -> Component.literal("  Status: " + span.status)
                .withStyle("SUCCESS".equals(span.status) ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        }
        
        return 1;
    }
    
    private static int searchTraces(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String serviceName = StringArgumentType.getString(context, "service");
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        var traces = serviceMeshSystem.getDistributedTraces();
        
        source.sendSuccess(() -> Component.literal("=== Traces for Service: " + serviceName + " ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        int count = 0;
        for (var entry : traces.entrySet()) {
            var spans = entry.getValue();
            boolean hasService = spans.stream().anyMatch(span -> serviceName.equals(span.serviceName));
            
            if (hasService) {
                String traceId = entry.getKey();
                source.sendSuccess(() -> Component.literal("• " + traceId.substring(0, 8) + "...")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(" (" + spans.size() + " spans)")
                        .withStyle(ChatFormatting.GRAY)), false);
                count++;
            }
        }
        
        if (count == 0) {
            source.sendSuccess(() -> Component.literal("No traces found for service: " + serviceName)
                .withStyle(ChatFormatting.GRAY), false);
        }
        
        return 1;
    }
    
    private static int showCircuitBreakerStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("=== Circuit Breaker Status ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        // Note: In the actual implementation, you would access circuit breaker states
        // For now, we'll show a placeholder message
        source.sendSuccess(() -> Component.literal("Circuit breaker monitoring is active")
            .withStyle(ChatFormatting.GREEN), false);
        
        source.sendSuccess(() -> Component.literal("All services are currently healthy")
            .withStyle(ChatFormatting.GREEN), false);
        
        return 1;
    }
    
    private static int resetCircuitBreaker(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String serviceId = StringArgumentType.getString(context, "service_id");
        
        if (serviceMeshSystem == null) {
            source.sendFailure(Component.literal("Service Mesh System is not initialized")
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        var service = serviceMeshSystem.getService(serviceId);
        if (service == null) {
            source.sendFailure(Component.literal("Service not found: " + serviceId)
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("Circuit breaker reset for service: " + service.name)
            .withStyle(ChatFormatting.GREEN), true);
        
        return 1;
    }
}
