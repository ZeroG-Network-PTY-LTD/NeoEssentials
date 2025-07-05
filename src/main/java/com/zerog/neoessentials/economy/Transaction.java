package com.zerog.neoessentials.economy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an economic transaction in the system.
 * Immutable class for audit trail purposes.
 */
public class Transaction {
    
    public enum Type {
        PAYMENT,        // Player to player payment
        SHOP_PURCHASE,  // Buying from a shop
        SHOP_SALE,      // Selling to a shop
        AUCTION_BID,    // Placing a bid
        AUCTION_WIN,    // Winning an auction
        ADMIN_GIVE,     // Admin giving money
        ADMIN_TAKE,     // Admin taking money
        ADMIN_SET,      // Admin setting balance
        INTEREST,       // Interest payment
        TAX,           // Tax deduction
        LOAN,          // Loan received
        LOAN_PAYMENT,  // Loan repayment
        DEPOSIT,       // Bank deposit
        WITHDRAWAL,    // Bank withdrawal
        SYSTEM        // System-generated transaction
    }
    
    public enum Status {
        PENDING,    // Transaction is being processed
        COMPLETED,  // Transaction completed successfully
        FAILED,     // Transaction failed
        CANCELLED,  // Transaction was cancelled
        REVERSED    // Transaction was reversed/rolled back
    }
    
    private final UUID id;
    private final UUID fromAccount;
    private final UUID toAccount;
    private final BigDecimal amount;
    private final Currency currency;
    private final Type type;
    private final Status status;
    private final String description;
    private final String metadata; // JSON string for additional data
    private final LocalDateTime timestamp;
    private final String processorId; // Who/what processed this transaction
    
    private Transaction(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID();
        this.fromAccount = builder.fromAccount;
        this.toAccount = builder.toAccount;
        this.amount = Objects.requireNonNull(builder.amount, "Amount cannot be null");
        this.currency = Objects.requireNonNull(builder.currency, "Currency cannot be null");
        this.type = Objects.requireNonNull(builder.type, "Type cannot be null");
        this.status = builder.status != null ? builder.status : Status.PENDING;
        this.description = builder.description != null ? builder.description : "";
        this.metadata = builder.metadata != null ? builder.metadata : "{}";
        this.timestamp = builder.timestamp != null ? builder.timestamp : LocalDateTime.now();
        this.processorId = builder.processorId;
        
        // Validation
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Transaction amount cannot be negative");
        }
        
        // Some transaction types require specific accounts
        if (type == Type.PAYMENT && (fromAccount == null || toAccount == null)) {
            throw new IllegalArgumentException("Payment transactions require both from and to accounts");
        }
    }
    
    /**
     * Creates a new transaction builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates a copy of this transaction with a new status
     */
    public Transaction withStatus(Status newStatus) {
        return new Builder()
                .id(this.id)
                .fromAccount(this.fromAccount)
                .toAccount(this.toAccount)
                .amount(this.amount)
                .currency(this.currency)
                .type(this.type)
                .status(newStatus)
                .description(this.description)
                .metadata(this.metadata)
                .timestamp(this.timestamp)
                .processorId(this.processorId)
                .build();
    }
    
    // Getters
    public UUID getId() { return id; }
    public UUID getFromAccount() { return fromAccount; }
    public UUID getToAccount() { return toAccount; }
    public BigDecimal getAmount() { return amount; }
    public Currency getCurrency() { return currency; }
    public Type getType() { return type; }
    public Status getStatus() { return status; }
    public String getDescription() { return description; }
    public String getMetadata() { return metadata; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getProcessorId() { return processorId; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Transaction that = (Transaction) obj;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("Transaction{id=%s, type=%s, amount=%s, status=%s}", 
                id, type, currency.format(amount), status);
    }
    
    /**
     * Builder pattern for creating transactions
     */
    public static class Builder {
        private UUID id;
        private UUID fromAccount;
        private UUID toAccount;
        private BigDecimal amount;
        private Currency currency;
        private Type type;
        private Status status;
        private String description;
        private String metadata;
        private LocalDateTime timestamp;
        private String processorId;
        
        public Builder id(UUID id) { this.id = id; return this; }
        public Builder fromAccount(UUID fromAccount) { this.fromAccount = fromAccount; return this; }
        public Builder toAccount(UUID toAccount) { this.toAccount = toAccount; return this; }
        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
        public Builder amount(double amount) { this.amount = BigDecimal.valueOf(amount); return this; }
        public Builder currency(Currency currency) { this.currency = currency; return this; }
        public Builder type(Type type) { this.type = type; return this; }
        public Builder status(Status status) { this.status = status; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder metadata(String metadata) { this.metadata = metadata; return this; }
        public Builder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }
        public Builder processorId(String processorId) { this.processorId = processorId; return this; }
        
        public Transaction build() {
            return new Transaction(this);
        }
    }
}
