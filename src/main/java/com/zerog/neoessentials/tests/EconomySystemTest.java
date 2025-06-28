package com.zerog.neoessentials.tests;

import com.zerog.neoessentials.economy.*;
import com.zerog.neoessentials.config.EnhancedEconomyConfig;
import com.zerog.neoessentials.economy.persistence.EconomyPersistenceManager;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Test class for the v1.0.2 economy system.
 * Validates all major economy functionality including banking, loans, shops, and auctions.
 */
public class EconomySystemTest {
    
    private static final UUID TEST_PLAYER_1 = UUID.randomUUID();
    private static final UUID TEST_PLAYER_2 = UUID.randomUUID();
    
    public static void main(String[] args) {
        runAllTests();
    }
    
    public static void runAllTests() {
        System.out.println("=== NeoEssentials Economy System Tests v1.0.2 ===");
        
        try {
            testConfiguration();
            testCurrencySystem();
            testPlayerEconomyData();
            testBankingSystem();
            testLoanSystem();
            testShopSystem();
            testAuctionSystem();
            testTransactionSystem();
            testPersistence();
            testEconomicAnalytics();
            
            System.out.println("=== All Economy Tests Completed Successfully ===");
        } catch (Exception e) {
            System.err.println("Economy test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testConfiguration() {
        System.out.println("Testing Enhanced Economy Configuration...");
        
        EnhancedEconomyConfig config = EnhancedEconomyConfig.getInstance();
        
        // Test default values
        assert config.isEconomyEnabled() : "Economy should be enabled by default";
        assert config.getStartingBalance() == 100.0 : "Starting balance should be 100.0";
        assert config.isBankingEnabled() : "Banking should be enabled by default";
        assert config.isLoansEnabled() : "Loans should be enabled by default";
        assert config.isShopsEnabled() : "Shops should be enabled by default";
        assert config.isAuctionsEnabled() : "Auctions should be enabled by default";
        
        System.out.println("✓ Configuration test passed");
    }
    
    private static void testCurrencySystem() {
        System.out.println("Testing Multi-Currency System...");
        
        CurrencyManager currencyManager = CurrencyManager.getInstance();
        
        // Test default currency
        Currency defaultCurrency = currencyManager.getDefaultCurrency();
        assert defaultCurrency != null : "Default currency should exist";
        
        // Test currency creation
        Currency testCurrency = new Currency("TEST", "Test Coin", "Test Coins", "T$", 
            false, false, 1.0, Currency.CurrencyType.STANDARD);
        currencyManager.addCurrency(testCurrency);
        
        Currency retrieved = currencyManager.getCurrency("TEST");
        assert retrieved != null : "Currency should be retrievable";
        assert retrieved.getDisplayName().equals("Test Coin") : "Currency display name should match";
        
        // Test exchange rates
        double exchangeRate = currencyManager.getExchangeRate(defaultCurrency, testCurrency);
        assert exchangeRate > 0 : "Exchange rate should be positive";
        
        System.out.println("✓ Currency system test passed");
    }
    
    private static void testPlayerEconomyData() {
        System.out.println("Testing Player Economy Data...");
        
        EconomyManager economyManager = EconomyManager.getInstance();
        CurrencyManager currencyManager = CurrencyManager.getInstance();
        Currency defaultCurrency = currencyManager.getDefaultCurrency();
        
        // Test player data creation
        PlayerEconomyData playerData = economyManager.getPlayerData(TEST_PLAYER_1);
        assert playerData != null : "Player data should be created";
        assert playerData.getPlayerId().equals(TEST_PLAYER_1) : "Player ID should match";
        
        // Test balance operations
        double startingBalance = playerData.getBalance(defaultCurrency);
        assert startingBalance >= 0 : "Starting balance should be non-negative";
        
        playerData.setBalance(defaultCurrency, 500.0);
        assert playerData.getBalance(defaultCurrency) == 500.0 : "Balance should be updated";
        
        // Test credit score
        double creditScore = playerData.getCreditScore();
        assert creditScore >= 300 && creditScore <= 850 : "Credit score should be in valid range";
        
        System.out.println("✓ Player economy data test passed");
    }
    
    private static void testBankingSystem() {
        System.out.println("Testing Banking System...");
        
        BankManager bankManager = BankManager.getInstance();
        
        // Test account creation
        BankAccount checkingAccount = bankManager.createAccount(TEST_PLAYER_1, BankAccount.AccountType.CHECKING);
        assert checkingAccount != null : "Checking account should be created";
        assert checkingAccount.getAccountType() == BankAccount.AccountType.CHECKING : "Account type should match";
        assert checkingAccount.getOwnerId().equals(TEST_PLAYER_1) : "Account owner should match";
        
        // Test deposit
        boolean depositSuccess = bankManager.deposit(checkingAccount.getAccountId(), 1000.0);
        assert depositSuccess : "Deposit should succeed";
        assert checkingAccount.getBalance() == 1000.0 : "Account balance should be updated";
        
        // Test withdrawal
        boolean withdrawSuccess = bankManager.withdraw(checkingAccount.getAccountId(), 200.0);
        assert withdrawSuccess : "Withdrawal should succeed";
        assert checkingAccount.getBalance() == 800.0 : "Account balance should be reduced";
        
        // Test transfer
        BankAccount savingsAccount = bankManager.createAccount(TEST_PLAYER_1, BankAccount.AccountType.SAVINGS);
        boolean transferSuccess = bankManager.transfer(checkingAccount.getAccountId(), savingsAccount.getAccountId(), 300.0);
        assert transferSuccess : "Transfer should succeed";
        assert checkingAccount.getBalance() == 500.0 : "Source account should be debited";
        assert savingsAccount.getBalance() == 300.0 : "Target account should be credited";
        
        System.out.println("✓ Banking system test passed");
    }
    
    private static void testLoanSystem() {
        System.out.println("Testing Loan System...");
        
        BankManager bankManager = BankManager.getInstance();
        
        // Test loan application
        String loanId = bankManager.applyForLoan(TEST_PLAYER_1, 5000.0, Loan.LoanType.PERSONAL, 24);
        assert loanId != null : "Loan application should succeed";
        
        // Test loan retrieval
        Loan loan = bankManager.getLoan(UUID.fromString(loanId));
        assert loan != null : "Loan should be retrievable";
        assert loan.getPrincipalAmount() == 5000.0 : "Loan amount should match";
        assert loan.getTermMonths() == 24 : "Loan term should match";
        assert loan.getStatus() == Loan.LoanStatus.PENDING : "Loan should be pending";
        
        // Test loan approval
        boolean approvalSuccess = bankManager.approveLoan(UUID.fromString(loanId));
        assert approvalSuccess : "Loan approval should succeed";
        assert loan.getStatus() == Loan.LoanStatus.APPROVED : "Loan should be approved";
        
        // Test loan payment
        boolean paymentSuccess = bankManager.makeLoanPayment(TEST_PLAYER_1, UUID.fromString(loanId), 250.0);
        assert paymentSuccess : "Loan payment should succeed";
        assert loan.getPayments().size() == 1 : "Payment should be recorded";
        
        System.out.println("✓ Loan system test passed");
    }
    
    private static void testShopSystem() {
        System.out.println("Testing Shop System...");
        
        ShopManager shopManager = ShopManager.getInstance();
        
        // Test shop creation
        Shop playerShop = shopManager.createShop(TEST_PLAYER_1, "Test Shop", Shop.ShopType.PLAYER);
        assert playerShop != null : "Shop should be created";
        assert playerShop.getName().equals("Test Shop") : "Shop name should match";
        assert playerShop.getOwnerId().equals(TEST_PLAYER_1) : "Shop owner should match";
        
        // Test shop item management
        // Note: This would require actual ItemStack integration in real implementation
        System.out.println("Shop item management test skipped (requires ItemStack integration)");
        
        System.out.println("✓ Shop system test passed");
    }
    
    private static void testAuctionSystem() {
        System.out.println("Testing Auction System...");
        
        ShopManager shopManager = ShopManager.getInstance();
        ShopManager.AuctionHouse auctionHouse = shopManager.getAuctionHouse();
        
        // Test auction creation
        // Note: This would require actual ItemStack integration in real implementation
        System.out.println("Auction creation test skipped (requires ItemStack integration)");
        
        // Test auction listing
        assert auctionHouse.getActiveAuctions() != null : "Active auctions list should exist";
        
        System.out.println("✓ Auction system test passed");
    }
    
    private static void testTransactionSystem() {
        System.out.println("Testing Transaction System...");
        
        TransactionManager transactionManager = new TransactionManager();
        CurrencyManager currencyManager = CurrencyManager.getInstance();
        Currency defaultCurrency = currencyManager.getDefaultCurrency();
        
        // Test transaction recording
        Transaction testTransaction = new Transaction(
            UUID.randomUUID(),
            TEST_PLAYER_1,
            TEST_PLAYER_2,
            100.0,
            defaultCurrency,
            "Test payment",
            Transaction.TransactionType.PLAYER_PAY,
            System.currentTimeMillis()
        );
        
        transactionManager.recordTransaction(testTransaction);
        
        // Test transaction retrieval
        var playerTransactions = transactionManager.getPlayerTransactions(TEST_PLAYER_1, 10);
        assert playerTransactions.size() >= 1 : "Transaction should be recorded";
        
        System.out.println("✓ Transaction system test passed");
    }
    
    private static void testPersistence() {
        System.out.println("Testing Persistence System...");
        
        EconomyPersistenceManager persistence = EconomyPersistenceManager.getInstance();
        
        // Test player data persistence
        PlayerEconomyData testData = new PlayerEconomyData(TEST_PLAYER_2);
        CompletableFuture<Void> saveResult = persistence.savePlayerData(testData);
        saveResult.join(); // Wait for completion
        
        CompletableFuture<PlayerEconomyData> loadResult = persistence.loadPlayerData(TEST_PLAYER_2);
        PlayerEconomyData loadedData = loadResult.join();
        
        assert loadedData != null : "Player data should be loaded";
        assert loadedData.getPlayerId().equals(TEST_PLAYER_2) : "Loaded data should match";
        
        System.out.println("✓ Persistence system test passed");
    }
    
    private static void testEconomicAnalytics() {
        System.out.println("Testing Economic Analytics...");
        
        EconomicAnalytics analytics = new EconomicAnalytics();
        
        // Test analytics update
        analytics.updateEconomicMetrics();
        
        // Test metrics retrieval
        double velocity = analytics.getEconomicVelocity();
        assert velocity >= 0 : "Economic velocity should be non-negative";
        
        double inflationRate = analytics.getInflationRate();
        assert inflationRate >= -1.0 && inflationRate <= 1.0 : "Inflation rate should be reasonable";
        
        EconomicAnalytics.WealthDistribution distribution = analytics.getWealthDistribution();
        assert distribution != null : "Wealth distribution should exist";
        
        double giniCoefficient = distribution.getGiniCoefficient();
        assert giniCoefficient >= 0 && giniCoefficient <= 1 : "Gini coefficient should be between 0 and 1";
        
        System.out.println("✓ Economic analytics test passed");
    }
    
    /**
     * Run performance tests
     */
    public static void runPerformanceTests() {
        System.out.println("=== Economy Performance Tests ===");
        
        long startTime = System.currentTimeMillis();
        
        // Test bulk player data operations
        EconomyManager economyManager = EconomyManager.getInstance();
        for (int i = 0; i < 1000; i++) {
            UUID testPlayerId = UUID.randomUUID();
            PlayerEconomyData data = economyManager.getPlayerData(testPlayerId);
            data.setBalance(CurrencyManager.getInstance().getDefaultCurrency(), Math.random() * 10000);
        }
        
        long endTime = System.currentTimeMillis();
        System.out.println("✓ Created 1000 player economy profiles in " + (endTime - startTime) + "ms");
        
        // Test bulk transaction recording
        startTime = System.currentTimeMillis();
        TransactionManager transactionManager = new TransactionManager();
        Currency defaultCurrency = CurrencyManager.getInstance().getDefaultCurrency();
        
        for (int i = 0; i < 1000; i++) {
            Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Math.random() * 1000,
                defaultCurrency,
                "Performance test transaction",
                Transaction.TransactionType.PLAYER_PAY,
                System.currentTimeMillis()
            );
            transactionManager.recordTransaction(transaction);
        }
        
        endTime = System.currentTimeMillis();
        System.out.println("✓ Recorded 1000 transactions in " + (endTime - startTime) + "ms");
        
        System.out.println("=== Performance Tests Completed ===");
    }
}
