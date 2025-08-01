package com.zerog.neoessentials.systems.automation;

import com.zerog.neoessentials.systems.analytics.DataAnalyticsSystem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

/**
 * Advanced command scheduling and automation system for NeoEssentials
 * Provides cron-like scheduling, conditional execution, and automation workflows
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class CommandScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandScheduler.class);
    
    // Singleton instance
    private static CommandScheduler instance;
    
    // Scheduler infrastructure
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final Map<String, ScheduledTask> activeTasks = new ConcurrentHashMap<>();
    private final Map<String, TaskTemplate> taskTemplates = new ConcurrentHashMap<>();
    private final Queue<TaskExecution> executionHistory = new ConcurrentLinkedQueue<>();
    
    // Analytics integration
    private final DataAnalyticsSystem analytics = DataAnalyticsSystem.getInstance();
    
    // Server reference
    private MinecraftServer server;
    
    private CommandScheduler() {
        initializeDefaultTemplates();
        LOGGER.info("Command Scheduler initialized");
    }
    
    public static CommandScheduler getInstance() {
        if (instance == null) {
            instance = new CommandScheduler();
        }
        return instance;
    }
    
    /**
     * Set server instance for command execution
     */
    public void setServer(MinecraftServer server) {
        this.server = server;
    }
    
    /**
     * Schedule a command to run at fixed intervals
     */
    public String scheduleRepeating(String name, String command, long intervalSeconds, 
                                   Map<String, Object> conditions) {
        
        String taskId = UUID.randomUUID().toString();
        
        ScheduledTask task = new ScheduledTask(
            taskId, name, command, TaskType.REPEATING, 
            intervalSeconds, conditions
        );
        
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
            () -> executeTask(task), 
            intervalSeconds, intervalSeconds, TimeUnit.SECONDS
        );
        
        task.setFuture(future);
        activeTasks.put(taskId, task);
        
        LOGGER.info("Scheduled repeating task '{}' every {} seconds", name, intervalSeconds);
        return taskId;
    }
    
    /**
     * Schedule a command to run once after a delay
     */
    public String scheduleOnce(String name, String command, long delaySeconds, 
                              Map<String, Object> conditions) {
        
        String taskId = UUID.randomUUID().toString();
        
        ScheduledTask task = new ScheduledTask(
            taskId, name, command, TaskType.ONCE, 
            delaySeconds, conditions
        );
        
        ScheduledFuture<?> future = scheduler.schedule(
            () -> {
                executeTask(task);
                activeTasks.remove(taskId);
            }, 
            delaySeconds, TimeUnit.SECONDS
        );
        
        task.setFuture(future);
        activeTasks.put(taskId, task);
        
        LOGGER.info("Scheduled one-time task '{}' in {} seconds", name, delaySeconds);
        return taskId;
    }
    
    /**
     * Schedule a cron-like task
     */
    public String scheduleCron(String name, String command, String cronExpression, 
                              Map<String, Object> conditions) {
        
        String taskId = UUID.randomUUID().toString();
        
        ScheduledTask task = new ScheduledTask(
            taskId, name, command, TaskType.CRON, 
            0, conditions
        );
        
        task.setCronExpression(cronExpression);
        
        // Calculate next execution time
        long nextExecution = calculateNextCronExecution(cronExpression);
        long delay = Math.max(0, nextExecution - System.currentTimeMillis());
        
        ScheduledFuture<?> future = scheduler.schedule(
            () -> {
                executeTask(task);
                // Reschedule for next cron execution
                rescheduleCronTask(task);
            }, 
            delay, TimeUnit.MILLISECONDS
        );
        
        task.setFuture(future);
        activeTasks.put(taskId, task);
        
        LOGGER.info("Scheduled cron task '{}' with expression '{}'", name, cronExpression);
        return taskId;
    }
    
    /**
     * Schedule a conditional task that runs when conditions are met
     */
    public String scheduleConditional(String name, String command, Supplier<Boolean> condition, 
                                    long checkIntervalSeconds) {
        
        String taskId = UUID.randomUUID().toString();
        
        ScheduledTask task = new ScheduledTask(
            taskId, name, command, TaskType.CONDITIONAL, 
            checkIntervalSeconds, new HashMap<>()
        );
        
        task.setConditionSupplier(condition);
        
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
            () -> {
                if (condition.get()) {
                    executeTask(task);
                    // Remove after execution for conditional tasks
                    cancelTask(taskId);
                }
            }, 
            checkIntervalSeconds, checkIntervalSeconds, TimeUnit.SECONDS
        );
        
        task.setFuture(future);
        activeTasks.put(taskId, task);
        
        LOGGER.info("Scheduled conditional task '{}' checking every {} seconds", name, checkIntervalSeconds);
        return taskId;
    }
    
    /**
     * Create task from template
     */
    public String scheduleFromTemplate(String templateName, Map<String, Object> parameters) {
        TaskTemplate template = taskTemplates.get(templateName);
        if (template == null) {
            throw new IllegalArgumentException("Unknown task template: " + templateName);
        }
        
        String command = replacePlaceholders(template.getCommand(), parameters);
        Map<String, Object> conditions = new HashMap<>(template.getConditions());
        conditions.putAll(parameters);
        
        switch (template.getType()) {
            case REPEATING:
                return scheduleRepeating(template.getName(), command, template.getInterval(), conditions);
            case ONCE:
                return scheduleOnce(template.getName(), command, template.getInterval(), conditions);
            case CRON:
                return scheduleCron(template.getName(), command, template.getCronExpression(), conditions);
            default:
                throw new IllegalArgumentException("Unsupported template type: " + template.getType());
        }
    }
    
    /**
     * Cancel a scheduled task
     */
    public boolean cancelTask(String taskId) {
        ScheduledTask task = activeTasks.remove(taskId);
        if (task != null && task.getFuture() != null) {
            task.getFuture().cancel(false);
            LOGGER.info("Cancelled task: {}", task.getName());
            return true;
        }
        return false;
    }
    
    /**
     * Get all active tasks
     */
    public List<ScheduledTask> getActiveTasks() {
        return new ArrayList<>(activeTasks.values());
    }
    
    /**
     * Get task execution history
     */
    public List<TaskExecution> getExecutionHistory(int limit) {
        return executionHistory.stream()
                              .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
                              .limit(limit)
                              .collect(ArrayList::new, (list, item) -> list.add(item), (list1, list2) -> list1.addAll(list2));
    }
    
    /**
     * Execute a scheduled task
     */
    private void executeTask(ScheduledTask task) {
        long startTime = System.currentTimeMillis();
        boolean success = false;
        String error = null;
        
        try {
            // Check conditions
            if (!checkConditions(task.getConditions())) {
                LOGGER.debug("Task '{}' conditions not met, skipping execution", task.getName());
                return;
            }
            
            // Execute command
            success = executeCommand(task.getCommand());
            
            // Record execution
            TaskExecution execution = new TaskExecution(
                task.getTaskId(), task.getName(), task.getCommand(),
                startTime, System.currentTimeMillis() - startTime, success, error
            );
            
            executionHistory.offer(execution);
            
            // Clean old executions (keep last 1000)
            while (executionHistory.size() > 1000) {
                executionHistory.poll();
            }
            
            // Analytics
            analytics.trackFeatureUsage("automation", "task_execution", Map.of(
                "task_id", task.getTaskId(),
                "task_name", task.getName(),
                "task_type", task.getType().toString(),
                "success", success,
                "execution_time", System.currentTimeMillis() - startTime
            ));
            
            LOGGER.debug("Executed task '{}' - Success: {}, Time: {}ms", 
                task.getName(), success, System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            error = e.getMessage();
            LOGGER.error("Failed to execute task '{}': {}", task.getName(), error, e);
            
            analytics.trackError("command_scheduler", "task_execution_failed", Map.of(
                "task_id", task.getTaskId(),
                "task_name", task.getName(),
                "error", error
            ));
        }
    }
    
    /**
     * Execute a command on the server
     */
    private boolean executeCommand(String command) {
        if (server == null) {
            LOGGER.warn("Server not available for command execution: {}", command);
            return false;
        }
        
        try {
            CommandSourceStack source = server.createCommandSourceStack();
            server.getCommands().performPrefixedCommand(source, command);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to execute command '{}': {}", command, e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if conditions are met for task execution
     */
    private boolean checkConditions(Map<String, Object> conditions) {
        if (conditions.isEmpty()) {
            return true;
        }
        
        // Check online player count condition
        if (conditions.containsKey("min_players")) {
            int minPlayers = (Integer) conditions.get("min_players");
            int onlinePlayers = server != null ? server.getPlayerCount() : 0;
            if (onlinePlayers < minPlayers) {
                return false;
            }
        }
        
        if (conditions.containsKey("max_players")) {
            int maxPlayers = (Integer) conditions.get("max_players");
            int onlinePlayers = server != null ? server.getPlayerCount() : 0;
            if (onlinePlayers > maxPlayers) {
                return false;
            }
        }
        
        // Check time-based conditions
        if (conditions.containsKey("time_range")) {
            String timeRange = (String) conditions.get("time_range");
            if (!isInTimeRange(timeRange)) {
                return false;
            }
        }
        
        // Check day of week condition
        if (conditions.containsKey("days_of_week")) {
            @SuppressWarnings("unchecked")
            List<String> allowedDays = (List<String>) conditions.get("days_of_week");
            String currentDay = LocalDateTime.now().getDayOfWeek().toString();
            if (!allowedDays.contains(currentDay)) {
                return false;
            }
        }
        
        // Check TPS condition
        if (conditions.containsKey("min_tps")) {
            double minTps = (Double) conditions.get("min_tps");
            double currentTps = getCurrentTPS();
            if (currentTps < minTps) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if current time is in specified range
     */
    private boolean isInTimeRange(String timeRange) {
        // Format: "HH:MM-HH:MM" (e.g., "09:00-17:00")
        try {
            String[] parts = timeRange.split("-");
            if (parts.length != 2) return true;
            
            LocalDateTime now = LocalDateTime.now();
            int currentHour = now.getHour();
            int currentMinute = now.getMinute();
            
            String[] startParts = parts[0].split(":");
            int startHour = Integer.parseInt(startParts[0]);
            int startMinute = Integer.parseInt(startParts[1]);
            
            String[] endParts = parts[1].split(":");
            int endHour = Integer.parseInt(endParts[0]);
            int endMinute = Integer.parseInt(endParts[1]);
            
            int currentTotalMinutes = currentHour * 60 + currentMinute;
            int startTotalMinutes = startHour * 60 + startMinute;
            int endTotalMinutes = endHour * 60 + endMinute;
            
            if (startTotalMinutes <= endTotalMinutes) {
                return currentTotalMinutes >= startTotalMinutes && currentTotalMinutes <= endTotalMinutes;
            } else {
                // Spans midnight
                return currentTotalMinutes >= startTotalMinutes || currentTotalMinutes <= endTotalMinutes;
            }
        } catch (Exception e) {
            LOGGER.warn("Invalid time range format: {}", timeRange);
            return true;
        }
    }
    
    /**
     * Get current server TPS
     */
    private double getCurrentTPS() {
        if (server == null) return 20.0;
        
        try {
            // This would need proper TPS calculation
            return 20.0; // Placeholder
        } catch (Exception e) {
            return 20.0;
        }
    }
    
    /**
     * Calculate next execution time for cron expression
     */
    private long calculateNextCronExecution(String cronExpression) {
        // Simplified cron parser - supports basic expressions
        // Format: "minute hour day month dayOfWeek"
        // Examples: "0 12 * * *" (daily at noon), "*/15 * * * *" (every 15 minutes)
        
        try {
            String[] parts = cronExpression.split("\\s+");
            if (parts.length != 5) {
                throw new IllegalArgumentException("Invalid cron expression format");
            }
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime next = now.plusMinutes(1); // Start from next minute
            
            // For simplicity, calculate next hour execution
            // A full cron implementation would be more complex
            return next.plusHours(1).toEpochSecond(java.time.ZoneOffset.UTC) * 1000;
            
        } catch (Exception e) {
            LOGGER.error("Failed to parse cron expression '{}': {}", cronExpression, e.getMessage());
            // Default to 1 hour from now
            return System.currentTimeMillis() + 3600000;
        }
    }
    
    /**
     * Reschedule cron task for next execution
     */
    private void rescheduleCronTask(ScheduledTask task) {
        if (task.getType() != TaskType.CRON) return;
        
        long nextExecution = calculateNextCronExecution(task.getCronExpression());
        long delay = Math.max(0, nextExecution - System.currentTimeMillis());
        
        ScheduledFuture<?> future = scheduler.schedule(
            () -> {
                executeTask(task);
                rescheduleCronTask(task);
            }, 
            delay, TimeUnit.MILLISECONDS
        );
        
        task.setFuture(future);
    }
    
    /**
     * Replace placeholders in command string
     */
    private String replacePlaceholders(String command, Map<String, Object> parameters) {
        String result = command;
        
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            result = result.replace(placeholder, entry.getValue().toString());
        }
        
        // Built-in placeholders
        result = result.replace("{timestamp}", String.valueOf(System.currentTimeMillis()));
        result = result.replace("{datetime}", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        if (server != null) {
            result = result.replace("{online_players}", String.valueOf(server.getPlayerCount()));
            result = result.replace("{max_players}", String.valueOf(server.getMaxPlayers()));
        }
        
        return result;
    }
    
    /**
     * Initialize default task templates
     */
    private void initializeDefaultTemplates() {
        // Server maintenance
        taskTemplates.put("server_restart_warning", new TaskTemplate(
            "server_restart_warning",
            "Server Restart Warning",
            "broadcast &cServer will restart in {minutes} minutes!",
            TaskType.ONCE,
            0,
            new HashMap<>(),
            null
        ));
        
        // Player notifications
        taskTemplates.put("daily_announcement", new TaskTemplate(
            "daily_announcement",
            "Daily Announcement",
            "broadcast &6Daily reminder: {message}",
            TaskType.CRON,
            0,
            new HashMap<>(),
            "0 12 * * *" // Daily at noon
        ));
        
        // Economy management
        taskTemplates.put("economy_bonus", new TaskTemplate(
            "economy_bonus",
            "Economy Bonus",
            "eco give {player} {amount}",
            TaskType.REPEATING,
            3600, // Every hour
            Map.of("min_players", 5),
            null
        ));
        
        // Cleanup tasks
        taskTemplates.put("cleanup_entities", new TaskTemplate(
            "cleanup_entities",
            "Entity Cleanup",
            "killall monsters",
            TaskType.REPEATING,
            1800, // Every 30 minutes
            Map.of("time_range", "02:00-06:00"), // During low activity hours
            null
        ));
        
        LOGGER.info("Initialized {} default task templates", taskTemplates.size());
    }
    
    /**
     * Add custom task template
     */
    public void addTaskTemplate(TaskTemplate template) {
        taskTemplates.put(template.getId(), template);
        LOGGER.info("Added task template: {}", template.getName());
    }
    
    /**
     * Get all available task templates
     */
    public List<TaskTemplate> getTaskTemplates() {
        return new ArrayList<>(taskTemplates.values());
    }
    
    /**
     * Shutdown scheduler
     */
    public void shutdown() {
        // Cancel all active tasks
        activeTasks.values().forEach(task -> {
            if (task.getFuture() != null) {
                task.getFuture().cancel(false);
            }
        });
        
        activeTasks.clear();
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        LOGGER.info("Command Scheduler shutdown");
    }
    
    // Data classes
    
    public static class ScheduledTask {
        private final String taskId;
        private final String name;
        private final String command;
        private final TaskType type;
        private final long interval;
        private final Map<String, Object> conditions;
        
        private ScheduledFuture<?> future;
        private String cronExpression;
        private Supplier<Boolean> conditionSupplier;
        private final long createdAt;
        
        public ScheduledTask(String taskId, String name, String command, TaskType type, 
                           long interval, Map<String, Object> conditions) {
            this.taskId = taskId;
            this.name = name;
            this.command = command;
            this.type = type;
            this.interval = interval;
            this.conditions = conditions;
            this.createdAt = System.currentTimeMillis();
        }
        
        // Getters and setters
        public String getTaskId() { return taskId; }
        public String getName() { return name; }
        public String getCommand() { return command; }
        public TaskType getType() { return type; }
        public long getInterval() { return interval; }
        public Map<String, Object> getConditions() { return conditions; }
        public ScheduledFuture<?> getFuture() { return future; }
        public void setFuture(ScheduledFuture<?> future) { this.future = future; }
        public String getCronExpression() { return cronExpression; }
        public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
        public Supplier<Boolean> getConditionSupplier() { return conditionSupplier; }
        public void setConditionSupplier(Supplier<Boolean> conditionSupplier) { this.conditionSupplier = conditionSupplier; }
        public long getCreatedAt() { return createdAt; }
    }
    
    public static class TaskTemplate {
        private final String id;
        private final String name;
        private final String command;
        private final TaskType type;
        private final long interval;
        private final Map<String, Object> conditions;
        private final String cronExpression;
        
        public TaskTemplate(String id, String name, String command, TaskType type, 
                          long interval, Map<String, Object> conditions, String cronExpression) {
            this.id = id;
            this.name = name;
            this.command = command;
            this.type = type;
            this.interval = interval;
            this.conditions = conditions;
            this.cronExpression = cronExpression;
        }
        
        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public String getCommand() { return command; }
        public TaskType getType() { return type; }
        public long getInterval() { return interval; }
        public Map<String, Object> getConditions() { return conditions; }
        public String getCronExpression() { return cronExpression; }
    }
    
    public static class TaskExecution {
        private final String taskId;
        private final String taskName;
        private final String command;
        private final long timestamp;
        private final long executionTime;
        private final boolean success;
        private final String error;
        
        public TaskExecution(String taskId, String taskName, String command, 
                           long timestamp, long executionTime, boolean success, String error) {
            this.taskId = taskId;
            this.taskName = taskName;
            this.command = command;
            this.timestamp = timestamp;
            this.executionTime = executionTime;
            this.success = success;
            this.error = error;
        }
        
        // Getters
        public String getTaskId() { return taskId; }
        public String getTaskName() { return taskName; }
        public String getCommand() { return command; }
        public long getTimestamp() { return timestamp; }
        public long getExecutionTime() { return executionTime; }
        public boolean isSuccess() { return success; }
        public String getError() { return error; }
    }
    
    public enum TaskType {
        ONCE, REPEATING, CRON, CONDITIONAL
    }
}
