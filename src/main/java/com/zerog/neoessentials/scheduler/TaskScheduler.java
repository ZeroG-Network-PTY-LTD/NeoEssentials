package com.zerog.neoessentials.scheduler;

import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

import java.time.ZoneId;
import java.util.List;

/**
 * Task scheduler that executes scheduled tasks based on cron expressions
 * Runs every second and checks if any tasks need to be executed
 */
@EventBusSubscriber(modid = "neoessentials")
public class TaskScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskScheduler.class);
    private static TaskScheduler INSTANCE;
    
    private static MinecraftServer server;
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20; // Check every second (20 ticks)
    
    private TaskScheduler() {
    }
    
    public static TaskScheduler getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TaskScheduler();
        }
        return INSTANCE;
    }
    
    /**
     * Set the Minecraft server instance
     */
    public void setServer(MinecraftServer server) {
        TaskScheduler.server = server;
    }
    
    /**
     * Server tick event handler
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (server == null) {
            server = event.getServer();
        }
        
        tickCounter++;
        if (tickCounter >= CHECK_INTERVAL) {
            tickCounter = 0;
            checkAndExecuteTasks();
        }
    }
    
    /**
     * Check all enabled tasks and execute if due
     */
    private static void checkAndExecuteTasks() {
        if (server == null) {
            return;
        }
        
        TaskManager manager = TaskManager.getInstance();
        List<ScheduledTask> enabledTasks = manager.getEnabledTasks();
        
        long currentTime = System.currentTimeMillis();
        
        for (ScheduledTask task : enabledTasks) {
            try {
                // Check if task is due for execution
                if (task.getNextExecutionTime() > 0 && currentTime >= task.getNextExecutionTime()) {
                    // Check conditions
                    if (manager.checkConditions(task, server)) {
                        executeTask(task);
                    } else {
                        NeoLog.debug(LOGGER, LogCategory.GENERAL, "Task conditions not met, skipping: {}", task.getName());
                        // Still update next execution time even if conditions not met
                        CronParser parser = new CronParser(task.getCronExpression());
                        ZoneId tz = ZoneId.of(task.getTimezone());
                        long nextExecution = parser.getNextExecutionTime(tz);
                        task.setNextExecutionTime(nextExecution);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error checking task: {}", task.getName(), e);
            }
        }
    }
    
    /**
     * Execute a scheduled task
     */
    public static void executeTask(ScheduledTask task) {
        NeoLog.info(LOGGER, LogCategory.GENERAL, "Executing scheduled task: {}", task.getName());
        long startTime = System.currentTimeMillis();
        
        try {
            switch (task.getTaskType()) {
                case COMMAND:
                    executeCommands(task);
                    break;
                case BACKUP:
                    executeBackup(task);
                    break;
                case RESTART:
                    executeRestart(task);
                    break;
                case BROADCAST:
                    executeBroadcast(task);
                    break;
                case CUSTOM:
                    executeCustom(task);
                    break;
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            TaskManager.getInstance().recordExecution(
                task.getId(), 
                true, 
                "Task executed successfully", 
                executionTime
            );
            
            NeoLog.info(LOGGER, LogCategory.GENERAL, "Task executed successfully: {} ({}ms)", task.getName(), executionTime);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            TaskManager.getInstance().recordExecution(
                task.getId(), 
                false, 
                "Error: " + e.getMessage(), 
                executionTime
            );
            
            LOGGER.error("Failed to execute task: {}", task.getName(), e);
        }
    }
    
    /**
     * Execute server commands
     */
    private static void executeCommands(ScheduledTask task) {
        if (server == null) {
            throw new RuntimeException("Server not initialized");
        }
        
        List<String> commands = task.getCommands();
        if (commands == null || commands.isEmpty()) {
            throw new RuntimeException("No commands specified");
        }
        
        for (String command : commands) {
            try {
                // Remove leading slash if present
                String cmd = command.startsWith("/") ? command.substring(1) : command;
                
                // Execute command on server thread
                server.execute(() -> {
                    try {
                        server.getCommands().performPrefixedCommand(
                            server.createCommandSourceStack(),
                            cmd
                        );
                        NeoLog.debug(LOGGER, LogCategory.GENERAL, "Executed command: {}", cmd);
                    } catch (Exception e) {
                        LOGGER.error("Failed to execute command: {}", cmd, e);
                    }
                });
                
            } catch (Exception e) {
                LOGGER.error("Error executing command: {}", command, e);
            }
        }
    }
    
    /**
     * Execute backup task
     */
    private static void executeBackup(ScheduledTask task) {
        if (server == null) {
            throw new RuntimeException("Server not initialized");
        }
        
        NeoLog.info(LOGGER, LogCategory.GENERAL, "Executing backup task: {}", task.getName());
        
        // Execute backup command on server thread
        server.execute(() -> {
            try {
                // Use save-all command
                server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack(),
                    "save-all"
                );
                NeoLog.info(LOGGER, LogCategory.GENERAL, "Backup completed: {}", task.getName());
            } catch (Exception e) {
                LOGGER.error("Failed to execute backup", e);
            }
        });
    }
    
    /**
     * Execute server restart
     */
    private static void executeRestart(ScheduledTask task) {
        if (server == null) {
            throw new RuntimeException("Server not initialized");
        }
        
        LOGGER.warn("Executing server restart task: {}", task.getName());
        
        // Send warning to all players
        Component message = MessageUtil.component("commands.neoessentials.scheduler.restart_warning", task.getName());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
        }
        
        // Stop server on server thread
        server.execute(() -> {
            try {
                server.halt(false);
            } catch (Exception e) {
                LOGGER.error("Failed to restart server", e);
            }
        });
    }
    
    /**
     * Execute broadcast message
     */
    private static void executeBroadcast(ScheduledTask task) {
        if (server == null) {
            throw new RuntimeException("Server not initialized");
        }
        
        List<String> commands = task.getCommands();
        if (commands == null || commands.isEmpty()) {
            throw new RuntimeException("No message specified");
        }
        
        String message = commands.getFirst();
        Component chatMessage = Component.literal(message);
        
        server.execute(() -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(chatMessage);
            }
            NeoLog.info(LOGGER, LogCategory.GENERAL, "Broadcast message sent: {}", message);
        });
    }
    
    /**
     * Execute custom task
     */
    private static void executeCustom(ScheduledTask task) {
        NeoLog.info(LOGGER, LogCategory.GENERAL, "Executing custom task: {}", task.getName());
        // Custom tasks can be extended by other modules
        executeCommands(task);
    }
}
