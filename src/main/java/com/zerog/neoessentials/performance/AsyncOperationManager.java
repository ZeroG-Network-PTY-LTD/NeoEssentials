package com.zerog.neoessentials.performance;

import com.zerog.neoessentials.error.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Asynchronous operation manager for performance-critical tasks
 * Handles database operations, file I/O, and network requests asynchronously
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class AsyncOperationManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncOperationManager.class);
    private static AsyncOperationManager instance;
    
    private final ExecutorService databaseExecutor;
    private final ExecutorService fileIOExecutor;
    private final ExecutorService networkExecutor;
    private final ScheduledExecutorService scheduledExecutor;
    
    private AsyncOperationManager() {
        // Database operations (CPU-intensive, moderate concurrency)
        this.databaseExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "NeoEssentials-Database");
            t.setDaemon(true);
            return t;
        });
        
        // File I/O operations (I/O-bound, higher concurrency)
        this.fileIOExecutor = Executors.newFixedThreadPool(6, r -> {
            Thread t = new Thread(r, "NeoEssentials-FileIO");
            t.setDaemon(true);
            return t;
        });
        
        // Network operations (I/O-bound, moderate concurrency)
        this.networkExecutor = Executors.newFixedThreadPool(3, r -> {
            Thread t = new Thread(r, "NeoEssentials-Network");
            t.setDaemon(true);
            return t;
        });
        
        // Scheduled operations (periodic tasks)
        this.scheduledExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "NeoEssentials-Scheduled");
            t.setDaemon(true);
            return t;
        });
    }
    
    public static AsyncOperationManager getInstance() {
        if (instance == null) {
            instance = new AsyncOperationManager();
        }
        return instance;
    }
    
    /**
     * Execute database operation asynchronously
     */
    public <T> CompletableFuture<T> executeDatabaseOperation(Supplier<T> operation) {
        return CompletableFuture.supplyAsync(operation, databaseExecutor)
            .exceptionally(throwable -> {
                ErrorHandler.handleSystemError("Async Database Operation", 
                    "database operation failed", new Exception(throwable));
                return null;
            });
    }
    
    /**
     * Execute database operation asynchronously with callback
     */
    public <T> void executeDatabaseOperation(Supplier<T> operation, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        CompletableFuture.supplyAsync(operation, databaseExecutor)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    if (onError != null) {
                        onError.accept(throwable);
                    } else {
                        ErrorHandler.handleSystemError("Async Database Operation", 
                            "database operation failed", new Exception(throwable));
                    }
                } else if (onSuccess != null) {
                    onSuccess.accept(result);
                }
            });
    }
    
    /**
     * Execute file I/O operation asynchronously
     */
    public <T> CompletableFuture<T> executeFileOperation(Supplier<T> operation) {
        return CompletableFuture.supplyAsync(operation, fileIOExecutor)
            .exceptionally(throwable -> {
                ErrorHandler.handleSystemError("Async File Operation", 
                    "file operation failed", new Exception(throwable));
                return null;
            });
    }
    
    /**
     * Execute file I/O operation asynchronously with callback
     */
    public <T> void executeFileOperation(Supplier<T> operation, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        CompletableFuture.supplyAsync(operation, fileIOExecutor)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    if (onError != null) {
                        onError.accept(throwable);
                    } else {
                        ErrorHandler.handleSystemError("Async File Operation", 
                            "file operation failed", new Exception(throwable));
                    }
                } else if (onSuccess != null) {
                    onSuccess.accept(result);
                }
            });
    }
    
    /**
     * Execute network operation asynchronously
     */
    public <T> CompletableFuture<T> executeNetworkOperation(Supplier<T> operation) {
        return CompletableFuture.supplyAsync(operation, networkExecutor)
            .exceptionally(throwable -> {
                ErrorHandler.handleSystemError("Async Network Operation", 
                    "network operation failed", new Exception(throwable));
                return null;
            });
    }
    
    /**
     * Execute network operation asynchronously with timeout
     */
    public <T> CompletableFuture<T> executeNetworkOperation(Supplier<T> operation, long timeoutSeconds) {
        CompletableFuture<T> future = CompletableFuture.supplyAsync(operation, networkExecutor);
        
        return future.orTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .exceptionally(throwable -> {
                if (throwable instanceof TimeoutException) {
                    ErrorHandler.handleSystemError("Async Network Operation", 
                        "network operation timed out", new Exception(throwable));
                } else {
                    ErrorHandler.handleSystemError("Async Network Operation", 
                        "network operation failed", new Exception(throwable));
                }
                return null;
            });
    }
    
    /**
     * Schedule periodic task
     */
    public ScheduledFuture<?> scheduleRepeating(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                task.run();
            } catch (Exception e) {
                ErrorHandler.handleSystemError("Scheduled Task", 
                    "scheduled task failed", e);
            }
        }, initialDelay, period, unit);
    }
    
    /**
     * Schedule one-time delayed task
     */
    public ScheduledFuture<?> scheduleDelayed(Runnable task, long delay, TimeUnit unit) {
        return scheduledExecutor.schedule(() -> {
            try {
                task.run();
            } catch (Exception e) {
                ErrorHandler.handleSystemError("Scheduled Task", 
                    "delayed task failed", e);
            }
        }, delay, unit);
    }
    
    /**
     * Execute runnable asynchronously on appropriate executor
     */
    public void executeAsync(Runnable task, AsyncTaskType type) {
        ExecutorService executor = switch (type) {
            case DATABASE -> databaseExecutor;
            case FILE_IO -> fileIOExecutor;
            case NETWORK -> networkExecutor;
        };
        
        executor.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                ErrorHandler.handleSystemError("Async Task", 
                    "async task failed", e);
            }
        });
    }
    
    /**
     * Get executor statistics
     */
    public AsyncStats getAsyncStats() {
        return new AsyncStats(
            getExecutorStats("Database", databaseExecutor),
            getExecutorStats("FileIO", fileIOExecutor),
            getExecutorStats("Network", networkExecutor),
            getExecutorStats("Scheduled", scheduledExecutor)
        );
    }
    
    /**
     * Get statistics for specific executor
     */
    private ExecutorStats getExecutorStats(String name, ExecutorService executor) {
        if (executor instanceof ThreadPoolExecutor tpe) {
            return new ExecutorStats(
                name,
                tpe.getActiveCount(),
                tpe.getCorePoolSize(),
                tpe.getMaximumPoolSize(),
                tpe.getCompletedTaskCount(),
                tpe.getTaskCount(),
                tpe.getQueue().size()
            );
        }
        
        return new ExecutorStats(name, 0, 0, 0, 0, 0, 0);
    }
    
    /**
     * Graceful shutdown of all executors
     */
    public void shutdown() {
        LOGGER.info("Shutting down async operation manager...");
        
        shutdownExecutor("Database", databaseExecutor);
        shutdownExecutor("FileIO", fileIOExecutor);
        shutdownExecutor("Network", networkExecutor);
        shutdownExecutor("Scheduled", scheduledExecutor);
        
        LOGGER.info("Async operation manager shutdown complete");
    }
    
    /**
     * Shutdown individual executor gracefully
     */
    private void shutdownExecutor(String name, ExecutorService executor) {
        try {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.warn("{} executor did not terminate gracefully, forcing shutdown", name);
                executor.shutdownNow();
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    LOGGER.error("{} executor did not terminate after forced shutdown", name);
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Task type enumeration
     */
    public enum AsyncTaskType {
        DATABASE,
        FILE_IO,
        NETWORK
    }
    
    /**
     * Executor statistics container
     */
    public static class ExecutorStats {
        private final String name;
        private final int activeThreads;
        private final int corePoolSize;
        private final int maxPoolSize;
        private final long completedTasks;
        private final long totalTasks;
        private final int queueSize;
        
        public ExecutorStats(String name, int activeThreads, int corePoolSize, int maxPoolSize,
                long completedTasks, long totalTasks, int queueSize) {
            this.name = name;
            this.activeThreads = activeThreads;
            this.corePoolSize = corePoolSize;
            this.maxPoolSize = maxPoolSize;
            this.completedTasks = completedTasks;
            this.totalTasks = totalTasks;
            this.queueSize = queueSize;
        }
        
        // Getters
        public String getName() { return name; }
        public int getActiveThreads() { return activeThreads; }
        public int getCorePoolSize() { return corePoolSize; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public long getCompletedTasks() { return completedTasks; }
        public long getTotalTasks() { return totalTasks; }
        public int getQueueSize() { return queueSize; }
    }
    
    /**
     * Async statistics container
     */
    public static class AsyncStats {
        private final ExecutorStats databaseStats;
        private final ExecutorStats fileIOStats;
        private final ExecutorStats networkStats;
        private final ExecutorStats scheduledStats;
        
        public AsyncStats(ExecutorStats databaseStats, ExecutorStats fileIOStats,
                ExecutorStats networkStats, ExecutorStats scheduledStats) {
            this.databaseStats = databaseStats;
            this.fileIOStats = fileIOStats;
            this.networkStats = networkStats;
            this.scheduledStats = scheduledStats;
        }
        
        // Getters
        public ExecutorStats getDatabaseStats() { return databaseStats; }
        public ExecutorStats getFileIOStats() { return fileIOStats; }
        public ExecutorStats getNetworkStats() { return networkStats; }
        public ExecutorStats getScheduledStats() { return scheduledStats; }
    }
}
