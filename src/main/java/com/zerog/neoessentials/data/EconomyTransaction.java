package com.zerog.neoessentials.data;

import java.util.Date;
import java.util.UUID;

/**
 * Represents a single economic transaction in the NeoEssentials economy system.
 */
public class EconomyTransaction {
    // Transaction type constants
    public static final String TYPE_DEPOSIT = "deposit";
    public static final String TYPE_WITHDRAW = "withdraw";
    public static final String TYPE_TRANSFER_SEND = "transfer_send";
    public static final String TYPE_TRANSFER_RECEIVE = "transfer_receive";
    public static final String TYPE_ADMIN = "admin";
    public static final String TYPE_COMMAND = "command";

    private final long id;
    private final UUID playerUUID;
    private final UUID otherPlayerUUID; // For transfers
    private final String type;
    private final double amount;
    private final double balanceAfter;
    private final String description;
    private final long timestamp;
    
    /**
     * Create a new economy transaction
     * 
     * @param id The transaction ID
     * @param playerUUID The UUID of the player involved
     * @param otherPlayerUUID The UUID of the other player (for transfers)
     * @param type The transaction type
     * @param amount The transaction amount
     * @param balanceAfter The balance after the transaction
     * @param description A description of the transaction
     * @param timestamp The timestamp of the transaction
     */
    public EconomyTransaction(long id, UUID playerUUID, UUID otherPlayerUUID, String type, 
                             double amount, double balanceAfter, String description, long timestamp) {
        this.id = id;
        this.playerUUID = playerUUID;
        this.otherPlayerUUID = otherPlayerUUID;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.timestamp = timestamp;
    }
    
    /**
     * Create a new economy transaction with the current timestamp
     * 
     * @param id The transaction ID
     * @param playerUUID The UUID of the player involved
     * @param otherPlayerUUID The UUID of the other player (for transfers)
     * @param type The transaction type
     * @param amount The transaction amount
     * @param balanceAfter The balance after the transaction
     * @param description A description of the transaction
     */
    public EconomyTransaction(long id, UUID playerUUID, UUID otherPlayerUUID, String type, 
                             double amount, double balanceAfter, String description) {
        this(id, playerUUID, otherPlayerUUID, type, amount, balanceAfter, description, System.currentTimeMillis());
    }
    
    /**
     * Create a new economy transaction with generated ID and current timestamp
     * 
     * @param playerUUID The UUID of the player involved
     * @param otherPlayerUUID The UUID of the other player (for transfers)
     * @param type The transaction type
     * @param amount The transaction amount
     * @param balanceAfter The balance after the transaction
     * @param description A description of the transaction
     */
    public EconomyTransaction(UUID playerUUID, UUID otherPlayerUUID, String type, 
                             double amount, double balanceAfter, String description) {
        this(-1, playerUUID, otherPlayerUUID, type, amount, balanceAfter, description, System.currentTimeMillis());
    }
    
    /**
     * Get the transaction ID
     * 
     * @return The transaction ID
     */
    public long getId() {
        return id;
    }
    
    /**
     * Get the UUID of the player involved
     * 
     * @return The player's UUID
     */
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    /**
     * Get the UUID of the other player involved (for transfers)
     * 
     * @return The other player's UUID, or null if not applicable
     */
    public UUID getOtherPlayerUUID() {
        return otherPlayerUUID;
    }
    
    /**
     * Get the transaction type
     * 
     * @return The transaction type
     */
    public String getType() {
        return type;
    }
    
    /**
     * Get the transaction amount
     * 
     * @return The transaction amount
     */
    public double getAmount() {
        return amount;
    }
    
    /**
     * Get the balance after the transaction
     * 
     * @return The balance after the transaction
     */
    public double getBalanceAfter() {
        return balanceAfter;
    }
    
    /**
     * Get the transaction description
     * 
     * @return The transaction description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the transaction timestamp
     * 
     * @return The transaction timestamp (milliseconds since epoch)
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Get the transaction date
     * 
     * @return The transaction date
     */
    public Date getDate() {
        return new Date(timestamp);
    }
    
    /**
     * Check if this is a deposit transaction
     * 
     * @return true if this is a deposit, false otherwise
     */
    public boolean isDeposit() {
        return TYPE_DEPOSIT.equals(type) || TYPE_TRANSFER_RECEIVE.equals(type);
    }
    
    /**
     * Check if this is a withdrawal transaction
     * 
     * @return true if this is a withdrawal, false otherwise
     */
    public boolean isWithdrawal() {
        return TYPE_WITHDRAW.equals(type) || TYPE_TRANSFER_SEND.equals(type);
    }
    
    /**
     * Check if this is a transfer transaction
     * 
     * @return true if this is a transfer, false otherwise
     */
    public boolean isTransfer() {
        return TYPE_TRANSFER_SEND.equals(type) || TYPE_TRANSFER_RECEIVE.equals(type);
    }
    
    /**
     * Check if this is an admin transaction
     * 
     * @return true if this is an admin transaction, false otherwise
     */
    public boolean isAdmin() {
        return TYPE_ADMIN.equals(type);
    }
    
    /**
     * Convert the transaction to a string
     * 
     * @return A string representation of the transaction
     */
    @Override
    public String toString() {
        return "EconomyTransaction{" +
                "id=" + id +
                ", playerUUID=" + playerUUID +
                ", otherPlayerUUID=" + otherPlayerUUID +
                ", type='" + type + '\'' +
                ", amount=" + amount +
                ", balanceAfter=" + balanceAfter +
                ", description='" + description + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
