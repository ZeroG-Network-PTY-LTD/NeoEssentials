package com.zerog.neoessentials.scheduler;

import java.time.ZoneId;
import java.util.*;

/**
 * Represents a scheduled task with cron expression, commands, and execution conditions
 */
public class ScheduledTask {
    private String id;
    private String name;
    private String description;
    private String cronExpression;
    private TaskType taskType;
    private List<String> commands;
    private boolean enabled;
    private String timezone;
    private TaskConditions conditions;
    private String createdBy;
    private long createdAt;
    private long updatedAt;
    private long lastExecutionTime;
    private long nextExecutionTime;
    private int executionCount;
    
    public ScheduledTask() {
        this.id = UUID.randomUUID().toString();
        this.enabled = true;
        this.timezone = ZoneId.systemDefault().getId();
        this.commands = new ArrayList<>();
        this.conditions = new TaskConditions();
        this.createdAt = System.currentTimeMillis();
        this.executionCount = 0;
    }
    
    public ScheduledTask(String name, String cronExpression, TaskType taskType) {
        this();
        this.name = name;
        this.cronExpression = cronExpression;
        this.taskType = taskType;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
    
    public TaskType getTaskType() { return taskType; }
    public void setTaskType(TaskType taskType) { this.taskType = taskType; }
    
    public List<String> getCommands() { return commands; }
    public void setCommands(List<String> commands) { this.commands = commands; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    
    public TaskConditions getConditions() { return conditions; }
    public void setConditions(TaskConditions conditions) { this.conditions = conditions; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    
    public long getLastExecutionTime() { return lastExecutionTime; }
    public void setLastExecutionTime(long lastExecutionTime) { this.lastExecutionTime = lastExecutionTime; }
    
    public long getNextExecutionTime() { return nextExecutionTime; }
    public void setNextExecutionTime(long nextExecutionTime) { this.nextExecutionTime = nextExecutionTime; }
    
    public int getExecutionCount() { return executionCount; }
    public void setExecutionCount(int executionCount) { this.executionCount = executionCount; }
    
    public void incrementExecutionCount() { this.executionCount++; }
    
    /**
     * Task types supported by the scheduler
     */
    public enum TaskType {
        COMMAND,        // Execute server commands
        BACKUP,         // Create server backup
        RESTART,        // Restart server
        BROADCAST,      // Send message to players
        CUSTOM          // Custom action
    }
    
    /**
     * Execution conditions for the task
     */
    public static class TaskConditions {
        private Integer minPlayers;
        private Integer maxPlayers;
        private Double maxServerLoad;
        private Long startTime;  // Time of day in milliseconds
        private Long endTime;    // Time of day in milliseconds
        private List<String> requiredDimensions;
        
        public TaskConditions() {
            this.requiredDimensions = new ArrayList<>();
        }
        
        public Integer getMinPlayers() { return minPlayers; }
        public void setMinPlayers(Integer minPlayers) { this.minPlayers = minPlayers; }
        
        public Integer getMaxPlayers() { return maxPlayers; }
        public void setMaxPlayers(Integer maxPlayers) { this.maxPlayers = maxPlayers; }
        
        public Double getMaxServerLoad() { return maxServerLoad; }
        public void setMaxServerLoad(Double maxServerLoad) { this.maxServerLoad = maxServerLoad; }
        
        public Long getStartTime() { return startTime; }
        public void setStartTime(Long startTime) { this.startTime = startTime; }
        
        public Long getEndTime() { return endTime; }
        public void setEndTime(Long endTime) { this.endTime = endTime; }
        
        public List<String> getRequiredDimensions() { return requiredDimensions; }
        public void setRequiredDimensions(List<String> requiredDimensions) { 
            this.requiredDimensions = requiredDimensions; 
        }
    }
}
