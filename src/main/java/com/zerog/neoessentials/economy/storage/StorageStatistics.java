package com.zerog.neoessentials.economy.storage;

/**
 * Contains statistics about economy storage usage and performance
 */
public class StorageStatistics {
    private final long totalAccounts;
    private final long totalTransactions;
    private final long storageSize;
    private final long lastBackupTime;
    private final String storageType;
    private final boolean healthy;
    
    public StorageStatistics(long totalAccounts, long totalTransactions, long storageSize, 
                           long lastBackupTime, String storageType, boolean healthy) {
        this.totalAccounts = totalAccounts;
        this.totalTransactions = totalTransactions;
        this.storageSize = storageSize;
        this.lastBackupTime = lastBackupTime;
        this.storageType = storageType;
        this.healthy = healthy;
    }
    
    public long getTotalAccounts() {
        return totalAccounts;
    }
    
    public long getTotalTransactions() {
        return totalTransactions;
    }
    
    public long getStorageSize() {
        return storageSize;
    }
    
    public long getLastBackupTime() {
        return lastBackupTime;
    }
    
    public String getStorageType() {
        return storageType;
    }
    
    public boolean isHealthy() {
        return healthy;
    }
    
    @Override
    public String toString() {
        return String.format("StorageStatistics{accounts=%d, transactions=%d, size=%d bytes, type=%s, healthy=%s}",
                totalAccounts, totalTransactions, storageSize, storageType, healthy);
    }
}
