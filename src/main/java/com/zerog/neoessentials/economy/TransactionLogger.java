package com.zerog.neoessentials.economy;

import com.zerog.neoessentials.NeoEssentials;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
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
        
        LogEntry entry = new LogEntry(
            LocalDateTime.now().format(LOG_DATE_FORMAT),
            transaction.getId().toString(),
            transaction.getType().name(),
            transaction.getFromAccount() != null ? transaction.getFromAccount().toString() : null,
            transaction.getToAccount() != null ? transaction.getToAccount().toString() : null,
            "", // playerName - not available in transaction
            transaction.getAmount().toString(),
            transaction.getCurrency().getSymbol(),
            transaction.getDescription(),
            transaction.getMetadata(),
            "", // oldBalance - not available in transaction
            ""  // newBalance - not available in transaction
        );
        
        logQueue.offer(entry);
    }
    
    /**
     * Logs a balance change
     */
    public void logBalanceChange(UUID playerId, String playerName, Currency currency, 
                                java.math.BigDecimal oldBalance, java.math.BigDecimal newBalance, 
                                String reason) {
        if (!running) return;
        
        LogEntry entry = new LogEntry(
            LocalDateTime.now().format(LOG_DATE_FORMAT),
            UUID.randomUUID().toString(),
            "BALANCE_CHANGE",
            playerId.toString(),
            null, // toAccount
            playerName,
            newBalance.subtract(oldBalance).toString(),
            currency.getSymbol(),
            reason,
            "", // metadata
            oldBalance.toString(),
            newBalance.toString()
        );
        
        logQueue.offer(entry);
    }
    
    /**
     * Logs an economy operation (shop, auction, etc.)
     */
    public void logOperation(String operation, UUID playerId, String playerName, 
                           String details, Object... metadata) {
        if (!running) return;
        
        StringBuilder metaBuilder = new StringBuilder();
        if (metadata.length > 0) {
            for (int i = 0; i < metadata.length; i += 2) {
                if (i + 1 < metadata.length) {
                    if (metaBuilder.length() > 0) metaBuilder.append(", ");
                    metaBuilder.append(metadata[i]).append("=").append(metadata[i + 1]);
                }
            }
        }
        
        LogEntry entry = new LogEntry(
            LocalDateTime.now().format(LOG_DATE_FORMAT),
            UUID.randomUUID().toString(),
            "OPERATION",
            playerId.toString(),
            null, // toAccount
            playerName,
            "", // amount
            "", // currency
            operation + ": " + details,
            metaBuilder.toString(),
            "", // oldBalance
            ""  // newBalance
        );
        
        logQueue.offer(entry);
    }
    
    /**
     * Logs a formatted transaction with additional context
     */
    public void logTransactionWithContext(Transaction transaction, String context) {
        if (!running) return;
        
        String description = transaction.getDescription();
        if (context != null && !context.isEmpty()) {
            description += " [Context: " + context + "]";
        }
        
        LogEntry entry = new LogEntry(
            LocalDateTime.now().format(LOG_DATE_FORMAT),
            transaction.getId().toString(),
            transaction.getType().name(),
            transaction.getFromAccount() != null ? transaction.getFromAccount().toString() : null,
            transaction.getToAccount() != null ? transaction.getToAccount().toString() : null,
            "", // playerName - not available in transaction
            transaction.getAmount().toString(),
            transaction.getCurrency().getSymbol(),
            description,
            transaction.getMetadata() + (context != null ? " | Context: " + context : ""),
            "", // oldBalance - not available in transaction
            ""  // newBalance - not available in transaction
        );
        
        logQueue.offer(entry);
    }
    
    /**
     * Logs a shop transaction with detailed information
     */
    public void logShopTransaction(String type, UUID playerId, String playerName, 
                                  String itemName, int quantity, BigDecimal amount, 
                                  Currency currency, String shopOwner) {
        if (!running) return;
        
        String description = String.format("%s: %dx %s", type, quantity, itemName);
        String metadata = String.format("quantity=%d, item=%s, shop_owner=%s", 
                                      quantity, itemName, shopOwner != null ? shopOwner : "admin");
        
        LogEntry entry = new LogEntry(
            LocalDateTime.now().format(LOG_DATE_FORMAT),
            UUID.randomUUID().toString(),
            "SHOP_TRANSACTION",
            playerId.toString(),
            null, // toAccount
            playerName,
            amount.toString(),
            currency.getSymbol(),
            description,
            metadata,
            "", // oldBalance - not tracked here
            ""  // newBalance - not tracked here
        );
        
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
        final String timestamp;
        final String id;
        final String type;
        final String fromAccount;
        final String toAccount;
        final String playerName;
        final String amount;
        final String currency;
        final String description;
        final String metadata;
        final String oldBalance;
        final String newBalance;
        
        LogEntry(String timestamp, String id, String type, String fromAccount, String toAccount, 
                String playerName, String amount, String currency, String description, 
                String metadata, String oldBalance, String newBalance) {
            this.timestamp = timestamp;
            this.id = id;
            this.type = type;
            this.fromAccount = fromAccount;
            this.toAccount = toAccount;
            this.playerName = playerName;
            this.amount = amount;
            this.currency = currency;
            this.description = description;
            this.metadata = metadata;
            this.oldBalance = oldBalance;
            this.newBalance = newBalance;
        }
        
        // Getters for accessing log entry data
        public String getTimestamp() { return timestamp; }
        public String getId() { return id; }
        public String getType() { return type; }
        public String getFromAccount() { return fromAccount; }
        public String getToAccount() { return toAccount; }
        public String getPlayerName() { return playerName; }
        public String getAmount() { return amount; }
        public String getCurrency() { return currency; }
        public String getDescription() { return description; }
        public String getMetadata() { return metadata; }
        public String getOldBalance() { return oldBalance; }
        public String getNewBalance() { return newBalance; }
    }
}
