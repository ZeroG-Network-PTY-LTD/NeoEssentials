package com.zerog.neoessentials.economy;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Manages all player wallets and cash transactions.
 * This handles the "cash on hand" system separate from banking.
 */
public class WalletManager {
    private static WalletManager instance;
    private final Map<UUID, PlayerWallet> wallets;
    
    private WalletManager() {
        this.wallets = new ConcurrentHashMap<>();
    }
    
    public static WalletManager getInstance() {
        if (instance == null) {
            instance = new WalletManager();
        }
        return instance;
    }
    
    /**
     * Get or create a player's wallet
     * 
     * @param playerId The player UUID
     * @return The player's wallet
     */
    public PlayerWallet getWallet(UUID playerId) {
        return wallets.computeIfAbsent(playerId, PlayerWallet::new);
    }
    
    /**
     * Get cash balance for a player in the default currency
     * 
     * @param playerId The player UUID
     * @return The cash balance
     */
    public double getCashBalance(UUID playerId) {
        Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
        if (defaultCurrency == null) return 0.0;
        
        return getCashBalance(playerId, defaultCurrency);
    }
    
    /**
     * Get cash balance for a player in a specific currency
     * 
     * @param playerId The player UUID
     * @param currency The currency
     * @return The cash balance
     */
    public double getCashBalance(UUID playerId, Currency currency) {
        PlayerWallet wallet = getWallet(playerId);
        return wallet.getCashBalance(currency);
    }
    
    /**
     * Set cash balance for a player in the default currency
     * 
     * @param playerId The player UUID
     * @param amount The new balance
     * @return true if successful
     */
    public boolean setCashBalance(UUID playerId, double amount) {
        Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
        if (defaultCurrency == null) return false;
        
        return setCashBalance(playerId, defaultCurrency, amount);
    }
    
    /**
     * Set cash balance for a player in a specific currency
     * 
     * @param playerId The player UUID
     * @param currency The currency
     * @param amount The new balance
     * @return true if successful
     */
    public boolean setCashBalance(UUID playerId, Currency currency, double amount) {
        PlayerWallet wallet = getWallet(playerId);
        return wallet.setCashBalance(currency, amount);
    }
    
    /**
     * Add cash to a player's wallet in the default currency
     * 
     * @param playerId The player UUID
     * @param amount The amount to add
     * @return true if successful
     */
    public boolean addCash(UUID playerId, double amount) {
        Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
        if (defaultCurrency == null) return false;
        
        return addCash(playerId, defaultCurrency, amount);
    }
    
    /**
     * Add cash to a player's wallet in a specific currency
     * 
     * @param playerId The player UUID
     * @param currency The currency
     * @param amount The amount to add
     * @return true if successful
     */
    public boolean addCash(UUID playerId, Currency currency, double amount) {
        PlayerWallet wallet = getWallet(playerId);
        return wallet.addCash(currency, amount);
    }
    
    /**
     * Subtract cash from a player's wallet in the default currency
     * 
     * @param playerId The player UUID
     * @param amount The amount to subtract
     * @return true if successful
     */
    public boolean subtractCash(UUID playerId, double amount) {
        Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
        if (defaultCurrency == null) return false;
        
        return subtractCash(playerId, defaultCurrency, amount);
    }
    
    /**
     * Subtract cash from a player's wallet in a specific currency
     * 
     * @param playerId The player UUID
     * @param currency The currency
     * @param amount The amount to subtract
     * @return true if successful
     */
    public boolean subtractCash(UUID playerId, Currency currency, double amount) {
        PlayerWallet wallet = getWallet(playerId);
        return wallet.subtractCash(currency, amount);
    }
    
    /**
     * Check if a player has sufficient cash in the default currency
     * 
     * @param playerId The player UUID
     * @param amount The amount needed
     * @return true if player has sufficient cash
     */
    public boolean hasCash(UUID playerId, double amount) {
        Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
        if (defaultCurrency == null) return false;
        
        return hasCash(playerId, defaultCurrency, amount);
    }
    
    /**
     * Check if a player has sufficient cash in a specific currency
     * 
     * @param playerId The player UUID
     * @param currency The currency
     * @param amount The amount needed
     * @return true if player has sufficient cash
     */
    public boolean hasCash(UUID playerId, Currency currency, double amount) {
        PlayerWallet wallet = getWallet(playerId);
        return wallet.hasCash(currency, amount);
    }
    
    /**
     * Transfer cash between two players in the default currency
     * 
     * @param fromPlayerId The sender player UUID
     * @param toPlayerId The receiver player UUID
     * @param amount The amount to transfer
     * @return true if successful
     */
    public boolean transferCash(UUID fromPlayerId, UUID toPlayerId, double amount) {
        Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
        if (defaultCurrency == null) return false;
        
        return transferCash(fromPlayerId, toPlayerId, defaultCurrency, amount);
    }
    
    /**
     * Transfer cash between two players in a specific currency
     * 
     * @param fromPlayerId The sender player UUID
     * @param toPlayerId The receiver player UUID
     * @param currency The currency
     * @param amount The amount to transfer
     * @return true if successful
     */
    public boolean transferCash(UUID fromPlayerId, UUID toPlayerId, Currency currency, double amount) {
        PlayerWallet fromWallet = getWallet(fromPlayerId);
        PlayerWallet toWallet = getWallet(toPlayerId);
        
        return fromWallet.transferCash(toWallet, currency, amount);
    }
    
    /**
     * Deposit cash from wallet to bank account
     * 
     * @param playerId The player UUID
     * @param bankAccount The target bank account
     * @param currency The currency
     * @param amount The amount to deposit
     * @return true if successful
     */
    public boolean depositToBank(UUID playerId, BankAccount bankAccount, Currency currency, double amount) {
        PlayerWallet wallet = getWallet(playerId);
        
        if (!wallet.hasCash(currency, amount)) {
            return false; // Insufficient cash
        }
        
        // Perform the transaction
        if (wallet.subtractCash(currency, amount)) {
            if (bankAccount.deposit(currency, amount)) {
                return true;
            } else {
                // Rollback if bank deposit failed
                wallet.addCash(currency, amount);
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Withdraw cash from bank account to wallet
     * 
     * @param playerId The player UUID
     * @param bankAccount The source bank account
     * @param currency The currency
     * @param amount The amount to withdraw
     * @return true if successful
     */
    public boolean withdrawFromBank(UUID playerId, BankAccount bankAccount, Currency currency, double amount) {
        PlayerWallet wallet = getWallet(playerId);
        
        // Check if bank account has sufficient balance
        if (bankAccount.getBalance(currency) < amount) {
            return false; // Insufficient bank balance
        }
        
        // Check if wallet can accept this amount
        double currentCash = wallet.getCashBalance(currency);
        double maxBalance = getMaxCashBalance();
        if (currentCash + amount > maxBalance) {
            return false; // Would exceed maximum cash balance
        }
        
        // Perform the transaction
        if (bankAccount.withdraw(currency, amount)) {
            if (wallet.addCash(currency, amount)) {
                return true;
            } else {
                // Rollback if wallet addition failed
                bankAccount.deposit(currency, amount);
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Get the maximum cash balance allowed
     * 
     * @return Maximum cash balance
     */
    private double getMaxCashBalance() {
        try {
            // This would normally come from economy config
            return 1000000.0;
        } catch (Exception e) {
            return 1000000.0;
        }
    }
    
    /**
     * Initialize wallet for a new player
     * 
     * @param playerId The player UUID
     */
    public void initializePlayerWallet(UUID playerId) {
        getWallet(playerId); // This will create the wallet with starting balance
    }
    
    /**
     * Get total number of wallets
     * 
     * @return Number of player wallets
     */
    public int getTotalWallets() {
        return wallets.size();
    }
    
    /**
     * Remove a player's wallet (for cleanup)
     * 
     * @param playerId The player UUID
     */
    public void removeWallet(UUID playerId) {
        wallets.remove(playerId);
    }
}
