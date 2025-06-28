# NeoEssentials Economy System - Complete Documentation

## Table of Contents
1. [System Overview](#system-overview)
2. [Architecture](#architecture)
3. [Currency System](#currency-system)
4. [Banking System](#banking-system)
5. [Loan System](#loan-system)
6. [Shop System](#shop-system)
7. [Auction House](#auction-house)
8. [Transaction Management](#transaction-management)
9. [Economic Analytics](#economic-analytics)
10. [Performance & Configuration](#performance--configuration)
11. [Database Schema](#database-schema)
12. [API Reference](#api-reference)
13. [Administrative Guide](#administrative-guide)
14. [Troubleshooting](#troubleshooting)

---

## System Overview

The NeoEssentials Economy System is a comprehensive, multi-faceted economic framework designed for Minecraft servers running NeoForge 1.21.1. It provides a realistic economic simulation with multiple currencies, banking operations, loans, shops, auctions, and detailed analytics.

### Key Features
- **Multi-Currency Support**: Multiple currency types (standard, resource-backed, tokens)
- **Advanced Banking**: Multiple account types with different interest rates and features
- **Loan System**: Personal, mortgage, and business loans with credit scoring
- **Dynamic Shops**: Player-owned shops with dynamic pricing
- **Auction House**: Competitive bidding system
- **Economic Analytics**: Real-time monitoring and reporting
- **Database Persistence**: SQLite-based data storage with backup systems

### System Requirements
- NeoForge 1.21.1
- Java 21+
- Minimum 2GB RAM allocated to server
- SQLite database support

---

## Architecture

### Core Components

```
EconomyManager (Central Controller)
├── CurrencyManager (Currency operations)
├── BankManager (Banking operations)
├── TransactionManager (Transaction processing)
├── ShopManager (Shop operations)
├── AuctionHouse (Auction operations)
├── LoanManager (Loan processing)
├── EconomicAnalytics (Data analysis)
└── EconomyPersistenceManager (Database operations)
```

### Data Flow

1. **Player Action** → Command System
2. **Command Processing** → EconomyManager
3. **Business Logic** → Specific Manager (Bank/Shop/etc.)
4. **Data Validation** → TransactionManager
5. **Persistence** → EconomyPersistenceManager
6. **Analytics Update** → EconomicAnalytics
7. **Response** → Player

### Thread Safety
- All economy operations are thread-safe using `ConcurrentHashMap`
- Database operations use connection pooling
- Background tasks run on dedicated thread pools

---

## Currency System

### Currency Types

#### 1. Standard Currency
- **Purpose**: Primary server currency
- **Backing**: Virtual (no physical items)
- **Example**: Coins, Credits
- **Configuration**:
```yaml
coins:
  display_name: "Coin"
  plural_name: "Coins"
  symbol: "$"
  is_default: true
  is_physical: false
  exchange_rate: 1.0
  type: "STANDARD"
```

#### 2. Resource-Backed Currency
- **Purpose**: Tied to Minecraft items
- **Backing**: Physical Minecraft items
- **Example**: Gold Ingots, Diamonds
- **Configuration**:
```yaml
gold_ingots:
  display_name: "Gold Ingot"
  plural_name: "Gold Ingots"
  symbol: "⚆"
  is_default: false
  is_physical: true
  exchange_rate: 10.0
  type: "RESOURCE"
```

#### 3. Token Currency
- **Purpose**: Event/special rewards
- **Backing**: Virtual tokens
- **Example**: Event Tokens, Loyalty Points
- **Configuration**:
```yaml
event_tokens:
  display_name: "Event Token"
  plural_name: "Event Tokens"
  symbol: "✦"
  is_default: false
  is_physical: false
  exchange_rate: 5.0
  type: "TOKEN"
```

### Currency Exchange

The system supports automatic currency conversion based on exchange rates:

```java
// Example: Converting 100 coins to gold ingots
double coinsAmount = 100.0;
double exchangeRate = 10.0; // 1 gold ingot = 10 coins
double goldAmount = coinsAmount / exchangeRate; // 10 gold ingots
```

### Exchange Rate Management
- **Static Rates**: Fixed conversion rates
- **Dynamic Rates**: Market-based fluctuations (future feature)
- **Fees**: Configurable conversion fees
- **Update Interval**: Automatic rate updates

---

## Banking System

### Account Types

#### 1. Checking Account
- **Purpose**: Daily transactions
- **Interest Rate**: 1% annually
- **Withdrawal Limits**: Unlimited
- **Transaction Fees**: 0.5%
- **Features**:
  - Instant transfers
  - No minimum balance
  - Overdraft protection (optional)

#### 2. Savings Account
- **Purpose**: Long-term savings
- **Interest Rate**: 5% annually
- **Withdrawal Limits**: $500/month
- **Minimum Term**: 6 months
- **Transaction Fees**: None
- **Features**:
  - Higher interest rates
  - Limited withdrawals
  - Penalty for early withdrawal

#### 3. Business Account
- **Purpose**: Business operations
- **Interest Rate**: 2% annually
- **Withdrawal Limits**: Unlimited
- **Transaction Fees**: 1%
- **Features**:
  - Multi-user access (future)
  - Business analytics
  - Merchant services

#### 4. Joint Account
- **Purpose**: Shared accounts
- **Interest Rate**: 1% annually
- **Withdrawal Limits**: Unlimited
- **Transaction Fees**: 0.5%
- **Features**:
  - Multiple authorized users
  - Shared access controls
  - Individual transaction limits

#### 5. Investment Account
- **Purpose**: High-yield investments
- **Interest Rate**: 8% annually
- **Withdrawal Limits**: $1000/month
- **Minimum Term**: 12 months
- **Transaction Fees**: 2%
- **Features**:
  - Highest interest rates
  - Risk-based returns
  - Investment tracking

### Banking Operations

#### Account Creation
```java
// Create a new savings account
BankAccount account = bankManager.createAccount(playerId, BankAccount.AccountType.SAVINGS);
```

#### Deposits
```java
// Deposit $500 into account
boolean success = bankManager.deposit(accountNumber, 500.0, Currency.getDefault());
```

#### Withdrawals
```java
// Withdraw $200 from account
boolean success = bankManager.withdraw(accountNumber, 200.0, Currency.getDefault());
```

#### Transfers
```java
// Transfer $100 between accounts
boolean success = bankManager.transfer(fromAccount, toAccount, 100.0, Currency.getDefault());
```

### Interest Calculation

Interest is calculated using the compound interest formula:
```
A = P(1 + r/n)^(nt)
```
Where:
- A = Final amount
- P = Principal amount
- r = Annual interest rate (decimal)
- n = Number of times interest is compounded per year
- t = Time in years

#### Daily Compounding
```java
// Interest calculated daily
double dailyRate = annualRate / 365.0;
double newBalance = principal * Math.pow(1 + dailyRate, days);
```

### Account Security
- **Account Numbers**: 10-digit unique identifiers
- **Access Control**: Owner-only access (unless joint account)
- **Transaction Logging**: All operations logged
- **Fraud Detection**: Unusual transaction patterns flagged

---

## Loan System

### Loan Types

#### 1. Personal Loans
- **Amount Range**: $500 - $50,000
- **Term**: Up to 60 months
- **Interest Rate**: 8% annually
- **Collateral**: Required
- **Purpose**: General personal use

#### 2. Mortgages
- **Amount Range**: $10,000 - $1,000,000
- **Term**: Up to 360 months (30 years)
- **Interest Rate**: 5% annually
- **Collateral**: Property-based
- **Purpose**: Property purchases

#### 3. Business Loans
- **Amount Range**: $1,000 - $500,000
- **Term**: Up to 120 months (10 years)
- **Interest Rate**: 6% annually
- **Collateral**: Required
- **Purpose**: Business operations

### Credit Scoring System

#### Credit Score Calculation
```java
public double calculateCreditScore(UUID playerId) {
    double baseScore = 750.0; // Starting score
    
    // Payment history (35% weight)
    double paymentHistory = getPaymentHistoryScore(playerId) * 0.35;
    
    // Credit utilization (30% weight)
    double creditUtilization = getCreditUtilizationScore(playerId) * 0.30;
    
    // Length of credit history (15% weight)
    double creditHistory = getCreditHistoryLength(playerId) * 0.15;
    
    // Types of credit (10% weight)
    double creditTypes = getCreditTypesScore(playerId) * 0.10;
    
    // New credit inquiries (10% weight)
    double newCredit = getNewCreditScore(playerId) * 0.10;
    
    return Math.max(300, Math.min(850, 
        baseScore + paymentHistory + creditUtilization + 
        creditHistory + creditTypes + newCredit));
}
```

#### Credit Score Factors
1. **Payment History (35%)**
   - On-time payments: +5 points
   - Late payments: -10 points
   - Defaults: -50 points

2. **Credit Utilization (30%)**
   - Low utilization (<30%): +10 points
   - High utilization (>70%): -15 points

3. **Credit History Length (15%)**
   - Longer history: +5 points per year
   - New accounts: No penalty

4. **Credit Mix (10%)**
   - Multiple loan types: +5 points
   - Single type: No penalty

5. **New Credit (10%)**
   - Recent inquiries: -2 points each
   - No recent activity: +0 points

### Loan Processing

#### Application Process
1. **Eligibility Check**
   - Credit score verification
   - Income verification (future)
   - Existing debt analysis

2. **Risk Assessment**
   - Credit score impact
   - Collateral evaluation
   - Payment capacity

3. **Approval/Denial**
   - Automatic for good credit
   - Manual review for borderline cases
   - Instant denial for poor credit

#### Payment Processing
```java
// Monthly payment calculation
double monthlyPayment = calculateMonthlyPayment(principal, annualRate, termMonths);

// Monthly payment formula: M = P * [r(1+r)^n] / [(1+r)^n - 1]
public double calculateMonthlyPayment(double principal, double annualRate, int termMonths) {
    double monthlyRate = annualRate / 12.0;
    double numerator = principal * monthlyRate * Math.pow(1 + monthlyRate, termMonths);
    double denominator = Math.pow(1 + monthlyRate, termMonths) - 1;
    return numerator / denominator;
}
```

### Collateral Management
- **Item-based Collateral**: Minecraft items held in escrow
- **Property Collateral**: Land/building claims
- **Cross-collateralization**: Multiple items as security
- **Automatic Seizure**: Upon default, collateral automatically transferred

---

## Shop System

### Shop Types

#### 1. Player Shops
- **Ownership**: Individual players
- **Location**: Player-designated areas
- **Rent**: Daily rental fee
- **Inventory**: Player-managed stock

#### 2. Market Stalls
- **Ownership**: Rented spaces
- **Location**: Designated market areas
- **Rent**: Higher daily fee, prime locations
- **Features**: Increased visibility

#### 3. Admin Shops
- **Ownership**: Server administrators
- **Purpose**: Market stabilization
- **Stock**: Unlimited (configurable)
- **Pricing**: Fixed or dynamic

### Dynamic Pricing Engine

The pricing engine adjusts prices based on supply and demand:

```java
public double calculateDynamicPrice(String itemId, double basePrice) {
    // Get supply and demand metrics
    int supply = getItemSupply(itemId);
    int demand = getItemDemand(itemId);
    
    // Calculate supply/demand ratio
    double ratio = (double) demand / Math.max(supply, 1);
    
    // Apply pricing formula
    double multiplier = 1.0 + (ratio - 1.0) * demandMultiplier;
    
    // Apply limits
    multiplier = Math.max(1.0 - maxPriceDecrease, 
                 Math.min(1.0 + maxPriceIncrease, multiplier));
    
    return basePrice * multiplier;
}
```

#### Pricing Factors
1. **Supply**: Number of items available across all shops
2. **Demand**: Recent purchase frequency
3. **Competition**: Number of shops selling same item
4. **Seasonality**: Time-based demand variations (future)
5. **Quality**: Item condition/enchantments

### Shop Operations

#### Shop Creation
```java
// Create a new shop
Shop shop = shopManager.createShop(ownerId, location, shopType);
```

#### Item Listing
```java
// Add item to shop
shopManager.addItem(shopId, itemStack, price, currency);
```

#### Purchase Processing
```java
// Process purchase
PurchaseResult result = shopManager.purchaseItem(buyerId, shopId, itemId, quantity);
```

### Taxation System
- **Sales Tax**: 5% on all transactions
- **Shop Licenses**: Annual licensing fees
- **Import/Export Duties**: Cross-region trade taxes (future)
- **Progressive Taxation**: Higher rates for luxury items

---

## Auction House

### Auction Types

#### 1. Standard Auctions
- **Format**: English auction (ascending bids)
- **Duration**: 1-168 hours
- **Minimum Bid**: Set by seller
- **Bid Increment**: 5% minimum

#### 2. Reserve Auctions
- **Reserve Price**: Hidden minimum price
- **Format**: Auction only succeeds if reserve met
- **Disclosure**: Reserve amount not shown to bidders

#### 3. Buy-It-Now Auctions
- **Instant Purchase**: Fixed price option
- **Dual Format**: Auction + instant buy
- **Early Termination**: Auction ends if item bought instantly

### Bidding System

#### Bid Processing
```java
public BidResult placeBid(UUID bidderId, UUID auctionId, double bidAmount) {
    Auction auction = getAuction(auctionId);
    
    // Validate bid
    if (bidAmount < auction.getCurrentBid() * 1.05) {
        return BidResult.INSUFFICIENT_AMOUNT;
    }
    
    // Check bidder balance
    if (!economyManager.hasBalance(bidderId, bidAmount)) {
        return BidResult.INSUFFICIENT_FUNDS;
    }
    
    // Place bid
    auction.placeBid(bidderId, bidAmount);
    
    // Hold funds
    economyManager.holdFunds(bidderId, bidAmount);
    
    return BidResult.SUCCESS;
}
```

#### Automatic Bidding (Proxy Bidding)
- **Max Bid**: Bidders set maximum bid amount
- **Auto-increment**: System bids incrementally up to max
- **Outbid Protection**: Automatic re-bidding when outbid
- **Snipe Protection**: Auction extension if bid in final minutes

### Auction Fees
- **Listing Fee**: 2% of starting bid
- **Success Fee**: 5% of final sale price
- **Reserve Fee**: Additional 1% for reserve auctions
- **Featured Listing**: Premium placement fees

---

## Transaction Management

### Transaction Types

#### 1. Direct Transfers
- **Player-to-Player**: Direct balance transfers
- **Instant**: Immediate processing
- **Fees**: Minimal transaction fees

#### 2. Shop Transactions
- **Purchase**: Item buying from shops
- **Sale**: Item selling to shops
- **Commission**: Shop owner commissions

#### 3. Banking Transactions
- **Deposits**: Adding money to accounts
- **Withdrawals**: Removing money from accounts
- **Transfers**: Inter-account transfers
- **Interest**: Automatic interest payments

#### 4. Service Transactions
- **Fees**: System service charges
- **Taxes**: Government taxation
- **Penalties**: Late payment penalties

### Transaction Processing

#### Validation Pipeline
```java
public TransactionResult processTransaction(Transaction transaction) {
    // 1. Validate transaction
    ValidationResult validation = validateTransaction(transaction);
    if (!validation.isValid()) {
        return TransactionResult.failure(validation.getError());
    }
    
    // 2. Check balances
    if (!checkSufficientFunds(transaction)) {
        return TransactionResult.INSUFFICIENT_FUNDS;
    }
    
    // 3. Apply holds
    applyFundsHold(transaction);
    
    // 4. Execute transaction
    try {
        executeTransaction(transaction);
        releaseFundsHold(transaction);
        return TransactionResult.SUCCESS;
    } catch (Exception e) {
        rollbackTransaction(transaction);
        return TransactionResult.failure(e.getMessage());
    }
}
```

#### Transaction States
1. **PENDING**: Transaction created, awaiting processing
2. **PROCESSING**: Currently being executed
3. **COMPLETED**: Successfully finished
4. **FAILED**: Failed to complete
5. **CANCELLED**: Manually cancelled
6. **REFUNDED**: Amount returned to sender

### Anti-Fraud Measures

#### Suspicious Activity Detection
- **Velocity Checks**: Rapid transaction patterns
- **Amount Thresholds**: Unusually large transactions
- **Geographic Patterns**: Impossible location changes
- **Time Patterns**: Transactions at unusual hours

#### Security Features
- **Transaction Limits**: Daily/monthly limits
- **Cooling Periods**: Delays for large transactions
- **Two-Factor Authentication**: Additional verification (future)
- **Audit Trails**: Complete transaction logging

---

## Economic Analytics

### Real-Time Metrics

#### Economic Health Indicators
1. **Economic Velocity**: Rate of money circulation
```java
double velocity = totalTransactionValue / totalMoneySupply;
```

2. **Wealth Distribution**: Gini coefficient calculation
```java
double giniCoefficient = calculateGiniCoefficient(playerBalances);
```

3. **Inflation Rate**: Price level changes over time
```java
double inflationRate = (currentPriceLevel - previousPriceLevel) / previousPriceLevel;
```

4. **Market Liquidity**: Available cash in circulation
```java
double liquidity = totalCashBalances / totalAssetValue;
```

### Data Collection

#### Automatic Data Points
- **Transaction Volume**: Amount and frequency
- **Price Movements**: Item price changes
- **Account Activity**: Banking operations
- **Market Participation**: Active traders count
- **Currency Flows**: Inter-currency exchanges

#### Sampling Intervals
- **Real-time**: Critical alerts
- **Hourly**: Detailed metrics
- **Daily**: Summary reports
- **Weekly**: Trend analysis
- **Monthly**: Comprehensive reviews

### Reporting System

#### Daily Reports
```
=== NeoEssentials Economy Daily Report ===
Date: 2025-06-28

SUMMARY METRICS:
- Total Money Supply: $2,450,000
- Transaction Volume: $125,000
- Active Players: 45
- Economic Velocity: 0.85

MARKET ACTIVITY:
- Shop Sales: $35,000
- Auction Volume: $15,000
- Banking Activity: $75,000

ALERTS:
- High inflation detected (8.5%)
- Wealth inequality above threshold
```

#### Market Analysis
- **Supply/Demand Analysis**: Item availability vs. demand
- **Price Trend Analysis**: Historical price movements
- **Competition Analysis**: Market concentration
- **Seasonal Patterns**: Time-based variations

### Economic Interventions

#### Automatic Stabilization
1. **Inflation Control**: Money supply adjustments
2. **Market Making**: Admin shop price setting
3. **Liquidity Injection**: Emergency cash infusions
4. **Tax Adjustments**: Dynamic tax rate changes

#### Manual Interventions
1. **Interest Rate Changes**: Central bank style adjustments
2. **Fiscal Policy**: Government spending/saving
3. **Market Regulations**: Trading restrictions
4. **Emergency Measures**: Crisis response protocols

---

## Performance & Configuration

### Performance Optimization

#### Database Optimization
- **Connection Pooling**: Reuse database connections
- **Prepared Statements**: Optimized SQL queries
- **Batch Operations**: Group multiple operations
- **Indexing**: Optimized database indexes

#### Memory Management
- **Object Pooling**: Reuse expensive objects
- **Caching**: Store frequently accessed data
- **Garbage Collection**: Minimize object creation
- **Memory Monitoring**: Track memory usage

#### Concurrent Processing
- **Thread Pools**: Dedicated task executors
- **Asynchronous Operations**: Non-blocking operations
- **Lock-Free Algorithms**: Minimize synchronization
- **Load Balancing**: Distribute processing load

### Configuration Management

#### Environment-Specific Settings
```yaml
# Development Environment
performance:
  background_tasks:
    thread_pool_size: 1
    queue_size: 100
  caching:
    player_data_cache_size: 100
    cache_expiry: 5

# Production Environment
performance:
  background_tasks:
    thread_pool_size: 4
    queue_size: 2000
  caching:
    player_data_cache_size: 5000
    cache_expiry: 60
```

#### Scaling Considerations
- **Horizontal Scaling**: Multiple server support (future)
- **Vertical Scaling**: Resource allocation optimization
- **Database Sharding**: Data distribution strategies
- **Cache Distribution**: Shared cache systems

### Monitoring & Alerting

#### Performance Metrics
- **Response Times**: Transaction processing speed
- **Throughput**: Transactions per second
- **Error Rates**: Failed operation percentage
- **Resource Usage**: CPU, memory, disk utilization

#### Alert Thresholds
```yaml
monitoring:
  alerts:
    response_time_warning: 500ms
    response_time_critical: 2000ms
    error_rate_warning: 1%
    error_rate_critical: 5%
    memory_usage_warning: 80%
    memory_usage_critical: 95%
```

---

## Database Schema

### Core Tables

#### players
```sql
CREATE TABLE players (
    player_id TEXT PRIMARY KEY,
    username TEXT NOT NULL,
    balance REAL DEFAULT 0.0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

#### currencies
```sql
CREATE TABLE currencies (
    currency_id TEXT PRIMARY KEY,
    display_name TEXT NOT NULL,
    symbol TEXT NOT NULL,
    exchange_rate REAL NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    is_physical BOOLEAN DEFAULT FALSE,
    currency_type TEXT NOT NULL
);
```

#### bank_accounts
```sql
CREATE TABLE bank_accounts (
    account_id TEXT PRIMARY KEY,
    player_id TEXT NOT NULL,
    account_type TEXT NOT NULL,
    balance REAL DEFAULT 0.0,
    interest_rate REAL DEFAULT 0.0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (player_id) REFERENCES players(player_id)
);
```

#### transactions
```sql
CREATE TABLE transactions (
    transaction_id TEXT PRIMARY KEY,
    from_player_id TEXT,
    to_player_id TEXT,
    amount REAL NOT NULL,
    currency_id TEXT NOT NULL,
    transaction_type TEXT NOT NULL,
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (currency_id) REFERENCES currencies(currency_id)
);
```

#### loans
```sql
CREATE TABLE loans (
    loan_id TEXT PRIMARY KEY,
    borrower_id TEXT NOT NULL,
    principal_amount REAL NOT NULL,
    interest_rate REAL NOT NULL,
    term_months INTEGER NOT NULL,
    monthly_payment REAL NOT NULL,
    remaining_balance REAL NOT NULL,
    status TEXT DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (borrower_id) REFERENCES players(player_id)
);
```

#### shops
```sql
CREATE TABLE shops (
    shop_id TEXT PRIMARY KEY,
    owner_id TEXT NOT NULL,
    shop_name TEXT NOT NULL,
    location TEXT,
    shop_type TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES players(player_id)
);
```

#### auctions
```sql
CREATE TABLE auctions (
    auction_id TEXT PRIMARY KEY,
    seller_id TEXT NOT NULL,
    item_data TEXT NOT NULL,
    starting_bid REAL NOT NULL,
    current_bid REAL,
    current_bidder_id TEXT,
    end_time DATETIME NOT NULL,
    status TEXT DEFAULT 'ACTIVE',
    FOREIGN KEY (seller_id) REFERENCES players(player_id)
);
```

### Indexing Strategy

#### Primary Indexes (Automatic)
- All PRIMARY KEY constraints create unique indexes
- Essential for entity lookups

#### Performance Indexes
```sql
-- Transaction lookups by player
CREATE INDEX idx_transactions_from_player ON transactions(from_player_id);
CREATE INDEX idx_transactions_to_player ON transactions(to_player_id);
CREATE INDEX idx_transactions_timestamp ON transactions(created_at);

-- Bank account lookups
CREATE INDEX idx_bank_accounts_player ON bank_accounts(player_id);
CREATE INDEX idx_bank_accounts_type ON bank_accounts(account_type);

-- Auction queries
CREATE INDEX idx_auctions_status ON auctions(status);
CREATE INDEX idx_auctions_end_time ON auctions(end_time);

-- Shop searches
CREATE INDEX idx_shops_owner ON shops(owner_id);
CREATE INDEX idx_shops_type ON shops(shop_type);
```

### Data Backup Strategy

#### Automated Backups
- **Full Backup**: Daily complete database backup
- **Incremental Backup**: Hourly transaction log backup
- **Retention Policy**: 30 days full, 7 days incremental

#### Backup Verification
- **Integrity Checks**: Verify backup completeness
- **Restoration Testing**: Regular restore tests
- **Corruption Detection**: Automated scan for data corruption

---

## API Reference

### EconomyManager API

#### Balance Operations
```java
// Get player balance
double getBalance(UUID playerId, Currency currency)

// Set player balance
boolean setBalance(UUID playerId, double amount, Currency currency)

// Add to balance
boolean addToBalance(UUID playerId, double amount, Currency currency)

// Subtract from balance
boolean subtractFromBalance(UUID playerId, double amount, Currency currency)

// Transfer between players
boolean transfer(UUID fromPlayer, UUID toPlayer, double amount, Currency currency)
```

#### Account Management
```java
// Create bank account
BankAccount createBankAccount(UUID playerId, AccountType type)

// Get player accounts
List<BankAccount> getPlayerAccounts(UUID playerId)

// Deposit to account
boolean deposit(String accountNumber, double amount, Currency currency)

// Withdraw from account
boolean withdraw(String accountNumber, double amount, Currency currency)
```

### BankManager API

#### Account Operations
```java
// Account creation
BankAccount createAccount(UUID playerId, BankAccount.AccountType accountType)

// Account lookup
BankAccount getAccount(String accountNumber)
List<BankAccount> getPlayerAccounts(UUID playerId)

// Balance operations
boolean deposit(String accountNumber, double amount, Currency currency)
boolean withdraw(String accountNumber, double amount, Currency currency)
boolean transfer(String fromAccount, String toAccount, double amount, Currency currency)

// Interest calculations
void calculateInterest(String accountNumber)
void calculateAllInterest()
```

### ShopManager API

#### Shop Management
```java
// Shop creation
Shop createShop(UUID ownerId, Location location, Shop.ShopType type)

// Shop operations
boolean addItem(UUID shopId, ItemStack item, double price, Currency currency)
boolean removeItem(UUID shopId, String itemId)
boolean updatePrice(UUID shopId, String itemId, double newPrice)

// Purchase operations
PurchaseResult purchaseItem(UUID buyerId, UUID shopId, String itemId, int quantity)
```

### AuctionHouse API

#### Auction Management
```java
// Create auction
Auction createAuction(UUID sellerId, ItemStack item, double startingBid, long duration)

// Bidding operations
BidResult placeBid(UUID bidderId, UUID auctionId, double bidAmount)
List<Auction> getActiveAuctions()
List<Auction> getPlayerAuctions(UUID playerId)

// Auction completion
void endAuction(UUID auctionId)
void processAuctionEnd(Auction auction)
```

### Event System

#### Custom Events
```java
// Economy events
@SubscribeEvent
public void onBalanceChange(PlayerBalanceChangeEvent event) {
    UUID playerId = event.getPlayerId();
    double oldBalance = event.getOldBalance();
    double newBalance = event.getNewBalance();
    // Handle balance change
}

@SubscribeEvent
public void onTransaction(TransactionEvent event) {
    Transaction transaction = event.getTransaction();
    // Handle transaction
}

@SubscribeEvent
public void onShopPurchase(ShopPurchaseEvent event) {
    UUID buyerId = event.getBuyerId();
    UUID shopId = event.getShopId();
    ItemStack item = event.getItem();
    double price = event.getPrice();
    // Handle shop purchase
}
```

---

## Administrative Guide

### Setup & Installation

#### Initial Configuration
1. **Enable Economy System**
```yaml
economy:
  enabled: true
  starting_balance: 100.0
  max_balance: 1000000.0
```

2. **Configure Currencies**
```yaml
currencies:
  coins:
    display_name: "Coin"
    is_default: true
    exchange_rate: 1.0
```

3. **Set Up Banking**
```yaml
banking:
  enabled: true
  account_creation:
    creation_fee: 100.0
    auto_create_checking: true
```

#### Database Setup
1. **Automatic Initialization**: Database tables created automatically
2. **Manual Setup**: SQL scripts available in `/sql/` directory
3. **Data Migration**: Import tools for existing economy data

### Administrative Commands

#### Player Management
```
/economy balance <player> [currency] - Check player balance
/economy set <player> <amount> [currency] - Set player balance
/economy give <player> <amount> [currency] - Give money to player
/economy take <player> <amount> [currency] - Take money from player
```

#### Bank Administration
```
/bank admin create <player> <type> - Create account for player
/bank admin close <account> - Close bank account
/bank admin interest - Trigger interest calculation
/bank admin statement <account> - View account statement
```

#### Shop Management
```
/shop admin list - List all shops
/shop admin inspect <shop> - Inspect shop details
/shop admin close <shop> - Close shop
/shop admin tax collect - Collect shop taxes
```

#### System Maintenance
```
/economy reload - Reload configuration
/economy backup - Create manual backup
/economy cleanup - Clean old transaction data
/economy status - View system status
```

### Monitoring & Analytics

#### Economic Health Dashboard
- **Real-time Metrics**: Live economic indicators
- **Alert System**: Automated problem detection
- **Trend Analysis**: Historical data visualization
- **Player Activity**: User engagement metrics

#### Performance Monitoring
```
/economy performance - View performance metrics
/economy debug - Enable debug logging
/economy profile start - Start performance profiling
/economy profile stop - Stop performance profiling
```

### Troubleshooting Common Issues

#### Performance Issues
1. **Slow Transactions**
   - Check database connection pool
   - Review transaction limits
   - Monitor memory usage

2. **High Memory Usage**
   - Reduce cache sizes
   - Increase garbage collection frequency
   - Review data retention policies

#### Data Issues
1. **Balance Discrepancies**
   - Run balance verification check
   - Review transaction logs
   - Restore from backup if necessary

2. **Missing Transactions**
   - Check transaction queue
   - Review error logs
   - Verify database integrity

### Backup & Recovery

#### Automated Backup Schedule
```yaml
backup:
  enabled: true
  schedule: "0 2 * * *"  # Daily at 2 AM
  retention_days: 30
  compression: true
```

#### Manual Backup Commands
```
/economy backup create - Create immediate backup
/economy backup list - List available backups
/economy backup restore <backup_id> - Restore from backup
```

#### Disaster Recovery
1. **Data Corruption**: Restore from last known good backup
2. **Server Crash**: Automatic recovery on restart
3. **Hardware Failure**: Backup restoration procedures

---

## Troubleshooting

### Common Issues & Solutions

#### Issue: Economy System Not Loading
**Symptoms**: Error messages during server startup
**Causes**:
- Missing dependencies
- Configuration errors
- Database connection issues

**Solutions**:
1. Verify all required dependencies are installed
2. Check configuration file syntax
3. Test database connectivity
4. Review server logs for specific errors

#### Issue: Transaction Failures
**Symptoms**: Failed balance transfers, error messages
**Causes**:
- Insufficient funds
- Invalid currency
- Database errors
- Permission issues

**Solutions**:
1. Verify sufficient player balance
2. Check currency configuration
3. Test database operations
4. Review permission settings

#### Issue: Performance Degradation
**Symptoms**: Slow response times, timeouts
**Causes**:
- High transaction volume
- Database bottlenecks
- Memory limitations
- Configuration issues

**Solutions**:
1. Increase thread pool sizes
2. Optimize database queries
3. Increase memory allocation
4. Review performance settings

### Debug Tools

#### Logging Configuration
```yaml
logging:
  level: DEBUG
  categories:
    - economy.transactions
    - economy.banking
    - economy.performance
```

#### Debug Commands
```
/economy debug enable - Enable debug mode
/economy debug transactions - View transaction queue
/economy debug cache - Display cache statistics
/economy debug threads - Show thread pool status
```

### Performance Tuning

#### Optimization Guidelines
1. **Database Tuning**
   - Optimize connection pool size
   - Add appropriate indexes
   - Regular maintenance tasks

2. **Memory Management**
   - Adjust cache sizes
   - Monitor garbage collection
   - Profile memory usage

3. **Threading Configuration**
   - Balance thread pool sizes
   - Minimize lock contention
   - Use async operations

#### Monitoring Tools
- **JVM Monitoring**: Memory and CPU usage
- **Database Monitoring**: Query performance
- **Application Monitoring**: Transaction metrics
- **Alert Systems**: Automated problem detection

---

## Appendices

### Appendix A: Configuration Reference

Complete configuration file with all available options:

```yaml
# NeoEssentials Economy Configuration
economy:
  enabled: true
  starting_balance: 100.0
  max_balance: 1000000.0
  allow_negative_balances: false
  inflation_rate: 0.02

currencies:
  coins:
    display_name: "Coin"
    plural_name: "Coins"
    symbol: "$"
    is_default: true
    is_physical: false
    exchange_rate: 1.0
    type: "STANDARD"

banking:
  enabled: true
  account_creation:
    creation_fee: 100.0
    max_accounts_per_player: 5
    auto_create_checking: true
  account_types:
    checking:
      display_name: "Checking Account"
      base_interest_rate: 0.01
      monthly_withdrawal_limit: -1
      minimum_months_before_withdrawal: 0
      transaction_fee_rate: 0.005
  interest:
    calculation_interval: 24
    compound_daily: true

loans:
  enabled: true
  loan_types:
    personal:
      display_name: "Personal Loan"
      min_amount: 500.0
      max_amount: 50000.0
      max_term_months: 60
      base_interest_rate: 0.08
      requires_collateral: true
  credit_scoring:
    starting_score: 750.0
    min_score: 300.0
    max_score: 850.0
    update_interval: 24

shops:
  enabled: true
  creation:
    creation_fee: 500.0
    max_shops_per_player: 5
    rental_fee: 50.0
  taxation:
    sales_tax_rate: 0.05
    collection_interval: 24
  dynamic_pricing:
    enabled: true
    demand_multiplier: 0.1
    max_price_increase: 0.5
    max_price_decrease: 0.3

auctions:
  enabled: true
  settings:
    min_duration: 1
    max_duration: 168
    default_duration: 24
    min_bid_increment: 0.05
  fees:
    listing_fee_rate: 0.02
    success_fee_rate: 0.05

transactions:
  history_retention: 365
  limits:
    max_transaction_amount: 100000.0
    daily_limit: 500000.0
    cooldown: 1
  cleanup:
    interval: 24
    archive_old_transactions: true

analytics:
  enabled: true
  collection:
    update_interval: 1
    retention_period: 365
  health_monitoring:
    velocity_warning_low: 0.1
    velocity_warning_high: 2.0
    inequality_warning: 0.7
    inflation_warning: 0.1
  reporting:
    daily_reports: true
    weekly_reports: true
    monthly_reports: true

exchange:
  enabled: true
  rates:
    update_interval: 24
    volatility: 0.05
  fees:
    base_fee_rate: 0.02
    cross_type_fee_rate: 0.01

performance:
  background_tasks:
    thread_pool_size: 2
    queue_size: 1000
  caching:
    player_data_cache_size: 1000
    cache_expiry: 30
  database:
    connection_pool_size: 10
    query_timeout: 30
```

### Appendix B: SQL Schema

Complete database schema with all tables and indexes:

```sql
-- Players table
CREATE TABLE players (
    player_id TEXT PRIMARY KEY,
    username TEXT NOT NULL,
    balance REAL DEFAULT 0.0,
    credit_score REAL DEFAULT 750.0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Currencies table
CREATE TABLE currencies (
    currency_id TEXT PRIMARY KEY,
    display_name TEXT NOT NULL,
    plural_name TEXT NOT NULL,
    symbol TEXT NOT NULL,
    exchange_rate REAL NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    is_physical BOOLEAN DEFAULT FALSE,
    currency_type TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Bank accounts table
CREATE TABLE bank_accounts (
    account_id TEXT PRIMARY KEY,
    account_number TEXT UNIQUE NOT NULL,
    player_id TEXT NOT NULL,
    account_type TEXT NOT NULL,
    balance REAL DEFAULT 0.0,
    interest_rate REAL DEFAULT 0.0,
    status TEXT DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (player_id) REFERENCES players(player_id)
);

-- Transactions table
CREATE TABLE transactions (
    transaction_id TEXT PRIMARY KEY,
    from_player_id TEXT,
    to_player_id TEXT,
    from_account_id TEXT,
    to_account_id TEXT,
    amount REAL NOT NULL,
    currency_id TEXT NOT NULL,
    transaction_type TEXT NOT NULL,
    description TEXT,
    status TEXT DEFAULT 'COMPLETED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (currency_id) REFERENCES currencies(currency_id),
    FOREIGN KEY (from_account_id) REFERENCES bank_accounts(account_id),
    FOREIGN KEY (to_account_id) REFERENCES bank_accounts(account_id)
);

-- Loans table
CREATE TABLE loans (
    loan_id TEXT PRIMARY KEY,
    borrower_id TEXT NOT NULL,
    loan_type TEXT NOT NULL,
    principal_amount REAL NOT NULL,
    interest_rate REAL NOT NULL,
    term_months INTEGER NOT NULL,
    monthly_payment REAL NOT NULL,
    remaining_balance REAL NOT NULL,
    next_payment_date DATETIME,
    status TEXT DEFAULT 'ACTIVE',
    collateral_data TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (borrower_id) REFERENCES players(player_id)
);

-- Shops table
CREATE TABLE shops (
    shop_id TEXT PRIMARY KEY,
    owner_id TEXT NOT NULL,
    shop_name TEXT NOT NULL,
    location TEXT,
    shop_type TEXT NOT NULL,
    status TEXT DEFAULT 'ACTIVE',
    creation_fee_paid REAL DEFAULT 0.0,
    monthly_rent REAL DEFAULT 0.0,
    last_rent_payment DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES players(player_id)
);

-- Shop items table
CREATE TABLE shop_items (
    item_id TEXT PRIMARY KEY,
    shop_id TEXT NOT NULL,
    item_data TEXT NOT NULL,
    price REAL NOT NULL,
    currency_id TEXT NOT NULL,
    quantity INTEGER DEFAULT 1,
    status TEXT DEFAULT 'AVAILABLE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (shop_id) REFERENCES shops(shop_id),
    FOREIGN KEY (currency_id) REFERENCES currencies(currency_id)
);

-- Auctions table
CREATE TABLE auctions (
    auction_id TEXT PRIMARY KEY,
    seller_id TEXT NOT NULL,
    item_data TEXT NOT NULL,
    starting_bid REAL NOT NULL,
    current_bid REAL,
    current_bidder_id TEXT,
    reserve_price REAL,
    buy_now_price REAL,
    end_time DATETIME NOT NULL,
    status TEXT DEFAULT 'ACTIVE',
    listing_fee REAL DEFAULT 0.0,
    success_fee REAL DEFAULT 0.0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES players(player_id),
    FOREIGN KEY (current_bidder_id) REFERENCES players(player_id)
);

-- Auction bids table
CREATE TABLE auction_bids (
    bid_id TEXT PRIMARY KEY,
    auction_id TEXT NOT NULL,
    bidder_id TEXT NOT NULL,
    bid_amount REAL NOT NULL,
    is_auto_bid BOOLEAN DEFAULT FALSE,
    max_bid_amount REAL,
    bid_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auctions(auction_id),
    FOREIGN KEY (bidder_id) REFERENCES players(player_id)
);

-- Economic metrics table
CREATE TABLE economic_metrics (
    metric_id TEXT PRIMARY KEY,
    metric_type TEXT NOT NULL,
    metric_value REAL NOT NULL,
    metric_data TEXT,
    recorded_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Performance indexes
CREATE INDEX idx_transactions_from_player ON transactions(from_player_id);
CREATE INDEX idx_transactions_to_player ON transactions(to_player_id);
CREATE INDEX idx_transactions_timestamp ON transactions(created_at);
CREATE INDEX idx_transactions_type ON transactions(transaction_type);

CREATE INDEX idx_bank_accounts_player ON bank_accounts(player_id);
CREATE INDEX idx_bank_accounts_type ON bank_accounts(account_type);
CREATE INDEX idx_bank_accounts_status ON bank_accounts(status);

CREATE INDEX idx_loans_borrower ON loans(borrower_id);
CREATE INDEX idx_loans_status ON loans(status);
CREATE INDEX idx_loans_payment_date ON loans(next_payment_date);

CREATE INDEX idx_shops_owner ON shops(owner_id);
CREATE INDEX idx_shops_type ON shops(shop_type);
CREATE INDEX idx_shops_status ON shops(status);

CREATE INDEX idx_shop_items_shop ON shop_items(shop_id);
CREATE INDEX idx_shop_items_status ON shop_items(status);

CREATE INDEX idx_auctions_seller ON auctions(seller_id);
CREATE INDEX idx_auctions_status ON auctions(status);
CREATE INDEX idx_auctions_end_time ON auctions(end_time);

CREATE INDEX idx_auction_bids_auction ON auction_bids(auction_id);
CREATE INDEX idx_auction_bids_bidder ON auction_bids(bidder_id);
CREATE INDEX idx_auction_bids_time ON auction_bids(bid_time);

CREATE INDEX idx_economic_metrics_type ON economic_metrics(metric_type);
CREATE INDEX idx_economic_metrics_time ON economic_metrics(recorded_at);
```

### Appendix C: API Examples

Common usage examples for developers:

```java
// Basic economy operations
EconomyManager economy = EconomyManager.getInstance();
UUID playerId = player.getUUID();

// Check balance
double balance = economy.getBalance(playerId, Currency.getDefault());

// Transfer money
boolean success = economy.transfer(fromPlayer, toPlayer, 100.0, Currency.getDefault());

// Banking operations
BankManager bank = BankManager.getInstance();

// Create savings account
BankAccount account = bank.createAccount(playerId, BankAccount.AccountType.SAVINGS);

// Deposit money
boolean deposited = bank.deposit(account.getAccountNumber(), 500.0, Currency.getDefault());

// Shop operations
ShopManager shops = ShopManager.getInstance();

// Create shop
Shop shop = shops.createShop(playerId, location, Shop.ShopType.PLAYER);

// Add item to shop
ItemStack item = new ItemStack(Items.DIAMOND, 1);
shops.addItem(shop.getShopId(), item, 50.0, Currency.getDefault());

// Auction operations
AuctionHouse auctions = AuctionHouse.getInstance();

// Create auction
Auction auction = auctions.createAuction(playerId, item, 10.0, 24 * 60 * 60); // 24 hours

// Place bid
BidResult result = auctions.placeBid(bidderId, auction.getAuctionId(), 15.0);

// Loan operations
LoanManager loans = LoanManager.getInstance();

// Apply for loan
LoanApplication application = new LoanApplication(playerId, 5000.0, 12, LoanType.PERSONAL);
LoanResult result = loans.processLoanApplication(application);
```

---

This comprehensive documentation covers all aspects of the NeoEssentials Economy System, from basic concepts to advanced configuration and troubleshooting. The system provides a robust, scalable economic framework suitable for servers of all sizes.
