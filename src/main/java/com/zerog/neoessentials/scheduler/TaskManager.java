package com.zerog.neoessentials.scheduler;

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.zerog.neoessentials.util.ResourceUtil;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Task manager for CRUD operations and persistence
 * Manages scheduled tasks, stores them to disk, and provides task lifecycle management
 */
public class TaskManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static TaskManager INSTANCE;
    
    private static final Path TASKS_DIR = ResourceUtil.getDataPath("scheduler");
    private static final Path TASKS_FILE = ResourceUtil.getDataPath("scheduler/tasks.json");
    private static final Path HISTORY_FILE = ResourceUtil.getDataPath("scheduler/execution_history.json");
    
    // Tasks storage: ID -> ScheduledTask
    private final Map<String, ScheduledTask> tasks = new ConcurrentHashMap<>();
    
    // Execution history: Task ID -> List of executions
    private final Map<String, List<TaskExecution>> executionHistory = new ConcurrentHashMap<>();
    
    // Max history entries per task
    private static final int MAX_HISTORY_PER_TASK = 100;
    
    private TaskManager() {
        try {
            if (!Files.exists(TASKS_DIR)) {
                Files.createDirectories(TASKS_DIR);
            }
            loadTasks();
            loadExecutionHistory();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize tasks directory", e);
        }
    }
    
    public static TaskManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TaskManager();
        }
        return INSTANCE;
    }
    
    /**
     * Create a new scheduled task
     */
    public ScheduledTask createTask(String name, String cronExpression, ScheduledTask.TaskType taskType,
                                    List<String> commands, String timezone, String createdBy) {
        // Validate cron expression
        if (!CronParser.isValid(cronExpression)) {
            throw new IllegalArgumentException("Invalid cron expression: " + cronExpression);
        }
        
        ScheduledTask task = new ScheduledTask(name, cronExpression, taskType);
        task.setCommands(commands);
        task.setTimezone(timezone != null ? timezone : ZoneId.systemDefault().getId());
        task.setCreatedBy(createdBy);
        
        // Calculate next execution time
        try {
            CronParser parser = new CronParser(cronExpression);
            ZoneId tz = ZoneId.of(task.getTimezone());
            long nextExecution = parser.getNextExecutionTime(tz);
            task.setNextExecutionTime(nextExecution);
        } catch (Exception e) {
            LOGGER.error("Failed to calculate next execution time", e);
        }
        
        tasks.put(task.getId(), task);
        saveTasks();
        
        LOGGER.info("Created scheduled task: {} ({})", name, task.getId());
        return task;
    }
    
    /**
     * Update existing task
     */
    public boolean updateTask(String id, String name, String cronExpression, Boolean enabled,
                             List<String> commands, String timezone, String description,
                             ScheduledTask.TaskConditions conditions) {
        ScheduledTask task = tasks.get(id);
        if (task == null) {
            return false;
        }
        
        if (name != null) task.setName(name);
        if (description != null) task.setDescription(description);
        if (enabled != null) task.setEnabled(enabled);
        if (commands != null) task.setCommands(commands);
        if (timezone != null) task.setTimezone(timezone);
        if (conditions != null) task.setConditions(conditions);
        
        if (cronExpression != null && !cronExpression.equals(task.getCronExpression())) {
            if (!CronParser.isValid(cronExpression)) {
                throw new IllegalArgumentException("Invalid cron expression: " + cronExpression);
            }
            task.setCronExpression(cronExpression);
            
            // Recalculate next execution time
            try {
                CronParser parser = new CronParser(cronExpression);
                ZoneId tz = ZoneId.of(task.getTimezone());
                long nextExecution = parser.getNextExecutionTime(tz);
                task.setNextExecutionTime(nextExecution);
            } catch (Exception e) {
                LOGGER.error("Failed to recalculate next execution time", e);
            }
        }
        
        task.setUpdatedAt(System.currentTimeMillis());
        saveTasks();
        
        LOGGER.info("Updated scheduled task: {} ({})", task.getName(), id);
        return true;
    }
    
    /**
     * Delete task
     */
    public boolean deleteTask(String id) {
        ScheduledTask removed = tasks.remove(id);
        if (removed != null) {
            executionHistory.remove(id);
            saveTasks();
            saveExecutionHistory();
            LOGGER.info("Deleted scheduled task: {} ({})", removed.getName(), id);
            return true;
        }
        return false;
    }
    
    /**
     * Get task by ID
     */
    public ScheduledTask getTask(String id) {
        return tasks.get(id);
    }
    
    /**
     * Get all tasks
     */
    public Collection<ScheduledTask> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }
    
    /**
     * Get enabled tasks only
     */
    public List<ScheduledTask> getEnabledTasks() {
        List<ScheduledTask> enabled = new ArrayList<>();
        for (ScheduledTask task : tasks.values()) {
            if (task.isEnabled()) {
                enabled.add(task);
            }
        }
        return enabled;
    }
    
    /**
     * Record task execution
     */
    public void recordExecution(String taskId, boolean success, String message, long executionTime) {
        TaskExecution execution = new TaskExecution();
        execution.taskId = taskId;
        execution.timestamp = System.currentTimeMillis();
        execution.success = success;
        execution.message = message;
        execution.executionTime = executionTime;
        
        List<TaskExecution> history = executionHistory.computeIfAbsent(taskId, k -> new ArrayList<>());
        history.add(0, execution); // Add to beginning
        
        // Limit history size
        if (history.size() > MAX_HISTORY_PER_TASK) {
            history.remove(history.size() - 1);
        }
        
        // Update task statistics
        ScheduledTask task = tasks.get(taskId);
        if (task != null) {
            task.setLastExecutionTime(execution.timestamp);
            task.incrementExecutionCount();
            
            // Calculate next execution time
            try {
                CronParser parser = new CronParser(task.getCronExpression());
                ZoneId tz = ZoneId.of(task.getTimezone());
                long nextExecution = parser.getNextExecutionTime(tz);
                task.setNextExecutionTime(nextExecution);
            } catch (Exception e) {
                LOGGER.error("Failed to calculate next execution time", e);
            }
        }
        
        saveExecutionHistory();
        saveTasks();
    }
    
    /**
     * Get execution history for a task
     */
    public List<TaskExecution> getExecutionHistory(String taskId) {
        return executionHistory.getOrDefault(taskId, new ArrayList<>());
    }
    
    /**
     * Check if task conditions are met
     */
    public boolean checkConditions(ScheduledTask task, MinecraftServer server) {
        ScheduledTask.TaskConditions conditions = task.getConditions();
        if (conditions == null) {
            return true;
        }
        
        // Check player count
        int playerCount = server.getPlayerCount();
        if (conditions.getMinPlayers() != null && playerCount < conditions.getMinPlayers()) {
            return false;
        }
        if (conditions.getMaxPlayers() != null && playerCount > conditions.getMaxPlayers()) {
            return false;
        }
        
        // Check server load (TPS-based)
        double avgTickTime = server.getAverageTickTimeNanos() / 1_000_000.0; // Convert to ms
        double tps = Math.min(20.0, 1000.0 / Math.max(50.0, avgTickTime));
        double load = (20.0 - tps) / 20.0 * 100.0; // Convert to load percentage
        if (conditions.getMaxServerLoad() != null && load > conditions.getMaxServerLoad()) {
            return false;
        }
        
        // Check time range
        if (conditions.getStartTime() != null && conditions.getEndTime() != null) {
            ZoneId tz = ZoneId.of(task.getTimezone());
            ZonedDateTime now = ZonedDateTime.now(tz);
            long currentTime = now.getHour() * 3600000L + now.getMinute() * 60000L + now.getSecond() * 1000L;
            
            if (currentTime < conditions.getStartTime() || currentTime > conditions.getEndTime()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Load tasks from disk
     */
    private void loadTasks() {
        try {
            if (Files.exists(TASKS_FILE)) {
                String json = Files.readString(TASKS_FILE, StandardCharsets.UTF_8);
                JsonObject data = JsonParser.parseString(json).getAsJsonObject();
                
                if (data.has("tasks")) {
                    JsonArray tasksArray = data.getAsJsonArray("tasks");
                    for (JsonElement element : tasksArray) {
                        try {
                            ScheduledTask task = parseTask(element.getAsJsonObject());
                            tasks.put(task.getId(), task);
                        } catch (Exception e) {
                            LOGGER.error("Failed to parse task", e);
                        }
                    }
                }
                
                LOGGER.info("Loaded {} scheduled tasks from disk", tasks.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load tasks", e);
        }
    }
    
    /**
     * Save tasks to disk
     */
    private void saveTasks() {
        try {
            JsonObject data = new JsonObject();
            data.addProperty("lastUpdated", System.currentTimeMillis());
            data.addProperty("version", "1.0");
            
            JsonArray tasksArray = new JsonArray();
            for (ScheduledTask task : tasks.values()) {
                tasksArray.add(taskToJson(task));
            }
            data.add("tasks", tasksArray);
            
            Files.writeString(TASKS_FILE, GSON.toJson(data), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                
        } catch (Exception e) {
            LOGGER.error("Failed to save tasks", e);
        }
    }
    
    /**
     * Load execution history from disk
     */
    private void loadExecutionHistory() {
        try {
            if (Files.exists(HISTORY_FILE)) {
                String json = Files.readString(HISTORY_FILE, StandardCharsets.UTF_8);
                JsonObject data = JsonParser.parseString(json).getAsJsonObject();
                
                if (data.has("history")) {
                    JsonObject historyObj = data.getAsJsonObject("history");
                    for (String taskId : historyObj.keySet()) {
                        JsonArray executions = historyObj.getAsJsonArray(taskId);
                        List<TaskExecution> execList = new ArrayList<>();
                        for (JsonElement element : executions) {
                            execList.add(GSON.fromJson(element, TaskExecution.class));
                        }
                        executionHistory.put(taskId, execList);
                    }
                }
                
                LOGGER.info("Loaded execution history for {} tasks", executionHistory.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load execution history", e);
        }
    }
    
    /**
     * Save execution history to disk
     */
    private void saveExecutionHistory() {
        try {
            JsonObject data = new JsonObject();
            data.addProperty("lastUpdated", System.currentTimeMillis());
            
            JsonObject historyObj = new JsonObject();
            for (Map.Entry<String, List<TaskExecution>> entry : executionHistory.entrySet()) {
                JsonArray executions = new JsonArray();
                for (TaskExecution exec : entry.getValue()) {
                    executions.add(GSON.toJsonTree(exec));
                }
                historyObj.add(entry.getKey(), executions);
            }
            data.add("history", historyObj);
            
            Files.writeString(HISTORY_FILE, GSON.toJson(data), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                
        } catch (Exception e) {
            LOGGER.error("Failed to save execution history", e);
        }
    }
    
    /**
     * Convert task to JSON
     */
    private JsonObject taskToJson(ScheduledTask task) {
        return GSON.toJsonTree(task).getAsJsonObject();
    }
    
    /**
     * Parse task from JSON
     */
    private ScheduledTask parseTask(JsonObject obj) {
        return GSON.fromJson(obj, ScheduledTask.class);
    }
    
    /**
     * Task execution record
     */
    public static class TaskExecution {
        public String taskId;
        public long timestamp;
        public boolean success;
        public String message;
        public long executionTime;
    }
}
