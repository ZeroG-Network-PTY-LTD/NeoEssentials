package com.zerog.neoessentials.economy.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.EconomyAccount;
import com.zerog.neoessentials.economy.Transaction;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * JSON file-based storage implementation for economy data
 */
public class JsonEconomyStorage implements EconomyStorage {
    private final Path dataDirectory;
    private final Path accountsFile;
    private final Path transactionsFile;
    private final Path backupDirectory;
    private final Gson gson;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    private Map<UUID, EconomyAccount> accountCache = new HashMap<>();
    private List<Transaction> transactionHistory = new ArrayList<>();
    private boolean initialized = false;
    
    public JsonEconomyStorage(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.accountsFile = dataDirectory.resolve("accounts.json");
        this.transactionsFile = dataDirectory.resolve("transactions.json");
        this.backupDirectory = dataDirectory.resolve("backups");
        
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }
    
    @Override
    public boolean initialize() {
        try {
            // Create directories if they don't exist
            Files.createDirectories(dataDirectory);
            Files.createDirectories(backupDirectory);
            
            // Load existing data
            loadAccounts();
            loadTransactions();
            
            initialized = true;
            NeoEssentials.LOGGER.info("JSON Economy Storage initialized successfully");
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to initialize JSON Economy Storage", e);
            return false;
        }
    }
    
    @Override
    public void close() {
        if (initialized) {
            // Perform final save
            saveAccounts();
            saveTransactions();
            initialized = false;
            NeoEssentials.LOGGER.info("JSON Economy Storage closed");
        }
    }
    
    @Override
    public boolean saveAccount(EconomyAccount account) {
        if (!initialized) return false;
        
        lock.writeLock().lock();
        try {
            accountCache.put(account.getPlayerId(), account);
            return saveAccounts();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public Optional<EconomyAccount> loadAccount(UUID playerId) {
        if (!initialized) return Optional.empty();
        
        lock.readLock().lock();
        try {
            return Optional.ofNullable(accountCache.get(playerId));
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public boolean deleteAccount(UUID playerId) {
        if (!initialized) return false;
        
        lock.writeLock().lock();
        try {
            accountCache.remove(playerId);
            return saveAccounts();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public boolean accountExists(UUID playerId) {
        if (!initialized) return false;
        
        lock.readLock().lock();
        try {
            return accountCache.containsKey(playerId);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<EconomyAccount> getAllAccounts() {
        if (!initialized) return new ArrayList<>();
        
        lock.readLock().lock();
        try {
            return new ArrayList<>(accountCache.values());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public boolean logTransaction(Transaction transaction) {
        if (!initialized) return false;
        
        lock.writeLock().lock();
        try {
            transactionHistory.add(transaction);
            // Keep only last 10000 transactions in memory for performance
            if (transactionHistory.size() > 10000) {
                transactionHistory = new ArrayList<>(transactionHistory.subList(transactionHistory.size() - 10000, transactionHistory.size()));
            }
            return saveTransactions();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public List<Transaction> getTransactionHistory(UUID playerId, int limit) {
        if (!initialized) return new ArrayList<>();
        
        lock.readLock().lock();
        try {
            return transactionHistory.stream()
                    .filter(t -> t.getFromAccount().equals(playerId) || t.getToAccount().equals(playerId))
                    .sorted((t1, t2) -> Long.compare(t2.getTimestamp(), t1.getTimestamp()))
                    .limit(limit)
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<Transaction> getTransactionHistory(UUID playerId, long fromTimestamp, long toTimestamp, int limit) {
        if (!initialized) return new ArrayList<>();
        
        lock.readLock().lock();
        try {
            return transactionHistory.stream()
                    .filter(t -> t.getFromAccount().equals(playerId) || t.getToAccount().equals(playerId))
                    .filter(t -> t.getTimestamp() >= fromTimestamp && t.getTimestamp() <= toTimestamp)
                    .sorted((t1, t2) -> Long.compare(t2.getTimestamp(), t1.getTimestamp()))
                    .limit(limit)
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public Optional<Transaction> getTransaction(UUID transactionId) {
        if (!initialized) return Optional.empty();
        
        lock.readLock().lock();
        try {
            return transactionHistory.stream()
                    .filter(t -> t.getId().equals(transactionId))
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<Transaction> getAllTransactions(int limit) {
        if (!initialized) return new ArrayList<>();
        
        lock.readLock().lock();
        try {
            return transactionHistory.stream()
                    .sorted((t1, t2) -> Long.compare(t2.getTimestamp(), t1.getTimestamp()))
                    .limit(limit)
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public boolean backup() {
        if (!initialized) return false;
        
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            Path backupPath = backupDirectory.resolve("backup_" + timestamp);
            Files.createDirectories(backupPath);
            
            // Copy accounts file
            Files.copy(accountsFile, backupPath.resolve("accounts.json"), StandardCopyOption.REPLACE_EXISTING);
            
            // Copy transactions file
            Files.copy(transactionsFile, backupPath.resolve("transactions.json"), StandardCopyOption.REPLACE_EXISTING);
            
            NeoEssentials.LOGGER.info("Economy backup created: {}", backupPath);
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to create economy backup", e);
            return false;
        }
    }
    
    @Override
    public boolean restore(String backupName) {
        if (!initialized) return false;
        
        try {
            Path backupPath = backupDirectory.resolve(backupName);
            if (!Files.exists(backupPath)) {
                NeoEssentials.LOGGER.error("Backup not found: {}", backupName);
                return false;
            }
            
            // Restore accounts
            Path backupAccountsFile = backupPath.resolve("accounts.json");
            if (Files.exists(backupAccountsFile)) {
                Files.copy(backupAccountsFile, accountsFile, StandardCopyOption.REPLACE_EXISTING);
            }
            
            // Restore transactions
            Path backupTransactionsFile = backupPath.resolve("transactions.json");
            if (Files.exists(backupTransactionsFile)) {
                Files.copy(backupTransactionsFile, transactionsFile, StandardCopyOption.REPLACE_EXISTING);
            }
            
            // Reload data
            loadAccounts();
            loadTransactions();
            
            NeoEssentials.LOGGER.info("Economy data restored from backup: {}", backupName);
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to restore from backup: " + backupName, e);
            return false;
        }
    }
    
    @Override
    public List<String> getAvailableBackups() {
        try {
            if (!Files.exists(backupDirectory)) {
                return new ArrayList<>();
            }
            
            return Files.list(backupDirectory)
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted(Collections.reverseOrder())
                    .toList();
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to list backups", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public boolean performMaintenance() {
        if (!initialized) return false;
        
        try {
            // Remove old backups (keep only last 10)
            List<String> backups = getAvailableBackups();
            if (backups.size() > 10) {
                for (int i = 10; i < backups.size(); i++) {
                    Path oldBackup = backupDirectory.resolve(backups.get(i));
                    deleteDirectory(oldBackup);
                }
            }
            
            // Clean up old transactions (keep only last 30 days)
            long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
            transactionHistory.removeIf(t -> t.getTimestamp() < thirtyDaysAgo);
            saveTransactions();
            
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to perform maintenance", e);
            return false;
        }
    }
    
    @Override
    public StorageStatistics getStatistics() {
        if (!initialized) {
            return new StorageStatistics(0, 0, 0, 0, "JSON", false);
        }
        
        lock.readLock().lock();
        try {
            long totalAccounts = accountCache.size();
            long totalTransactions = transactionHistory.size();
            long storageSize = getDirectorySize(dataDirectory);
            
            long lastBackupTime = 0;
            List<String> backups = getAvailableBackups();
            if (!backups.isEmpty()) {
                // Parse timestamp from backup name (backup_yyyy-MM-dd_HH-mm-ss)
                String latestBackup = backups.get(0);
                // This is a simplified approach; in practice you might store backup metadata
            }
            
            return new StorageStatistics(totalAccounts, totalTransactions, storageSize, 
                    lastBackupTime, "JSON", true);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public StorageValidationReport validateData() {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (!initialized) {
            errors.add("Storage not initialized");
            return new StorageValidationReport(false, errors, warnings);
        }
        
        lock.readLock().lock();
        try {
            // Validate accounts
            for (EconomyAccount account : accountCache.values()) {
                if (account.getPlayerId() == null) {
                    errors.add("Account with null player ID found");
                }
                if (account.getBalance().compareTo(java.math.BigDecimal.ZERO) < 0) {
                    warnings.add("Account with negative balance: " + account.getPlayerId());
                }
            }
            
            // Validate transactions
            for (Transaction transaction : transactionHistory) {
                if (transaction.getId() == null) {
                    errors.add("Transaction with null ID found");
                }
                if (transaction.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                    errors.add("Transaction with invalid amount: " + transaction.getId());
                }
            }
            
            boolean valid = errors.isEmpty();
            return new StorageValidationReport(valid, errors, warnings);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    private boolean saveAccounts() {
        try {
            JsonObject root = new JsonObject();
            root.add("accounts", gson.toJsonTree(accountCache));
            
            try (FileWriter writer = new FileWriter(accountsFile.toFile())) {
                gson.toJson(root, writer);
            }
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to save accounts", e);
            return false;
        }
    }
    
    private void loadAccounts() throws IOException {
        if (!Files.exists(accountsFile)) {
            accountCache = new HashMap<>();
            return;
        }
        
        try (FileReader reader = new FileReader(accountsFile.toFile())) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            
            Type mapType = new TypeToken<Map<UUID, EconomyAccount>>(){}.getType();
            accountCache = gson.fromJson(root.get("accounts"), mapType);
            
            if (accountCache == null) {
                accountCache = new HashMap<>();
            }
        }
    }
    
    private boolean saveTransactions() {
        try {
            JsonObject root = new JsonObject();
            root.add("transactions", gson.toJsonTree(transactionHistory));
            
            try (FileWriter writer = new FileWriter(transactionsFile.toFile())) {
                gson.toJson(root, writer);
            }
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to save transactions", e);
            return false;
        }
    }
    
    private void loadTransactions() throws IOException {
        if (!Files.exists(transactionsFile)) {
            transactionHistory = new ArrayList<>();
            return;
        }
        
        try (FileReader reader = new FileReader(transactionsFile.toFile())) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            
            Type listType = new TypeToken<List<Transaction>>(){}.getType();
            transactionHistory = gson.fromJson(root.get("transactions"), listType);
            
            if (transactionHistory == null) {
                transactionHistory = new ArrayList<>();
            }
        }
    }
    
    private long getDirectorySize(Path directory) {
        try {
            return Files.walk(directory)
                    .filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            return 0;
        }
    }
    
    private void deleteDirectory(Path directory) throws IOException {
        Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
}
