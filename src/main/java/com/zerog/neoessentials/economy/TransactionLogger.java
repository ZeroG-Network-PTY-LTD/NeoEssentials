package com.zerog.neoessentials.economy;

import com.zerog.neoessentials.NeoEssentials;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles logging of economy transactions for audit trails and debugging
 */
public class TransactionLogger {
    
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter LOG_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final File logDirectory;
    private final Gson gson;
    private final BlockingQueue<LogEntry> logQueue;
    private final ExecutorService logExecutor;
    private volatile boolean running;
    
    public TransactionLogger(File configDirectory) {
        this.logDirectory = new File(configDirectory, "economy_logs");
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        this.logQueue = new LinkedBlockingQueue<>();
        this.logExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Economy-Logger");
            t.setDaemon(true);
            return t;
        });
        this.running = true;
        
        // Create log directory if it doesn't exist
        if (!logDirectory.exists()) {
            logDirectory.mkdirs();
        }
        
        // Start logging thread
        logExecutor.submit(this::processLogs);
        
        NeoEssentials.LOGGER.info("Transaction logger initialized at: {}", logDirectory.getAbsolutePath());
    }
    
    /**
     * Logs a transaction
     */
    public void logTransaction(Transaction transaction) {
        if (!running) return;
        
        LogEntry entry = new LogEntry();
        entry.timestamp = LocalDateTime.now().format(LOG_DATE_FORMAT);
        entry.id = transaction.getId().toString();
        entry.type = transaction.getType().name();
        entry.fromAccount = transaction.getFromAccount() != null ? transaction.getFromAccount().toString() : null;
        entry.toAccount = transaction.getToAccount() != null ? transaction.getToAccount().toString() : null;
        entry.amount = transaction.getAmount().toString();
        entry.currency = transaction.getCurrency().getSymbol();
        entry.description = transaction.getDescription();
        entry.metadata = transaction.getMetadata();
        
        logQueue.offer(entry);
    }
    
    /**
     * Logs a balance change
     */
    public void logBalanceChange(UUID playerId, String playerName, Currency currency, 
                                java.math.BigDecimal oldBalance, java.math.BigDecimal newBalance, 
                                String reason) {
        if (!running) return;
        
        LogEntry entry = new LogEntry();
        entry.timestamp = LocalDateTime.now().format(LOG_DATE_FORMAT);
        entry.id = UUID.randomUUID().toString();
        entry.type = "BALANCE_CHANGE";
        entry.fromAccount = playerId.toString();
        entry.playerName = playerName;
        entry.amount = newBalance.subtract(oldBalance).toString();
        entry.currency = currency.getSymbol();
        entry.description = reason;
        entry.oldBalance = oldBalance.toString();
        entry.newBalance = newBalance.toString();
        
        logQueue.offer(entry);
    }
    
    /**
     * Logs an economy operation (shop, auction, etc.)
     */
    public void logOperation(String operation, UUID playerId, String playerName, 
                           String details, Object... metadata) {
        if (!running) return;
        
        LogEntry entry = new LogEntry();
        entry.timestamp = LocalDateTime.now().format(LOG_DATE_FORMAT);
        entry.id = UUID.randomUUID().toString();
        entry.type = "OPERATION";
        entry.fromAccount = playerId.toString();
        entry.playerName = playerName;
        entry.description = operation + ": " + details;
        
        if (metadata.length > 0) {
            StringBuilder meta = new StringBuilder();
            for (int i = 0; i < metadata.length; i += 2) {
                if (i + 1 < metadata.length) {
                    if (meta.length() > 0) meta.append(", ");
                    meta.append(metadata[i]).append("=").append(metadata[i + 1]);
                }
            }
            entry.metadata = meta.toString();
        }
        
        logQueue.offer(entry);
    }
    
    /**
     * Processes log entries in the background
     */
    private void processLogs() {
        while (running || !logQueue.isEmpty()) {
            try {
                LogEntry entry = logQueue.take();
                writeLogEntry(entry);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error processing log entry", e);
            }
        }
    }
    
    /**
     * Writes a log entry to the appropriate daily log file
     */
    private void writeLogEntry(LogEntry entry) {
        String dateStr = LocalDateTime.now().format(FILE_DATE_FORMAT);
        File logFile = new File(logDirectory, "economy_" + dateStr + ".log");
        
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write(gson.toJson(entry));
            writer.write("\n");
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to write log entry to {}", logFile.getAbsolutePath(), e);
        }
    }
    
    /**
     * Shuts down the transaction logger
     */
    public void shutdown() {
        running = false;
        logExecutor.shutdown();
        
        // Process remaining entries
        while (!logQueue.isEmpty()) {
            try {
                LogEntry entry = logQueue.poll();
                if (entry != null) {
                    writeLogEntry(entry);
                }
            } catch (Exception e) {
                NeoEssentials.LOGGER.error("Error processing final log entries", e);
                break;
            }
        }
        
        NeoEssentials.LOGGER.info("Transaction logger shut down");
    }
    
    /**
     * Internal class for log entries
     */
    private static class LogEntry {
        String timestamp;
        String id;
        String type;
        String fromAccount;
        String toAccount;
        String playerName;
        String amount;
        String currency;
        String description;
        String metadata;
        String oldBalance;
        String newBalance;
    }
}
