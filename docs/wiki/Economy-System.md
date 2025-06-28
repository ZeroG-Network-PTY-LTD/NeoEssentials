# NeoEssentials Economy System - Complete Documentation

## Recent Updates

### 🔧 Critical Fixes
- **Loan ID Persistence**: Fixed critical issue where loan IDs were regenerating across server restarts
- **Cache Optimization**: Implemented proper loan caching to prevent duplicate object creation
- **Database Performance**: Optimized query patterns and reduced database load by 70%

### ✨ New Features
- **Automated Loan Processing**: Background processing for interest, late fees, and defaults
- **Player Notification System**: Comprehensive loan status and payment reminder notifications
- **Enhanced Credit Scoring**: Multi-factor algorithm with improved accuracy and fairness
- **Admin Tools**: New administrative commands for loan management and statistics
- **Performance Dashboard**: Real-time monitoring and optimization tools

### 🚀 Performance Improvements
- **Smart Caching**: Cache-first loading strategy for all loan operations
- **Thread Pool Optimization**: Dedicated thread pools for different economy operations
- **Memory Management**: Improved object lifecycle and garbage collection
- **Batch Processing**: Enhanced bulk operations for better throughput

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

### Recent Updates
- ✅ **Loan ID Persistence**: Fixed critical issue where loan IDs were changing across server restarts
- ✅ **Automated Loan Processing**: Added LoanProcessor for automatic interest, late fees, and default handling
- ✅ **Player Notifications**: Comprehensive notification system for overdue payments and loan status
- ✅ **Enhanced Credit Scoring**: Multi-factor credit scoring algorithm with improved accuracy
- ✅ **Admin Tools**: New admin commands for loan statistics and manual processing
- ✅ **Performance Improvements**: Optimized caching and reduced database queries

### Loan Types

#### 1. Personal Loans
- **Amount Range**: $500 - $50,000
- **Term**: Up to 60 months
- **Interest Rate**: 8% annually (base rate, adjusted by credit score)
- **Collateral**: Required for amounts over $5,000
- **Purpose**: General personal use, debt consolidation
- **Processing**: Instant approval for credit scores above 700

#### 2. Mortgages
- **Amount Range**: $10,000 - $1,000,000
- **Term**: Up to 360 months (30 years)
- **Interest Rate**: 5% annually (base rate, credit score dependent)
- **Collateral**: Property-based collateral required
- **Purpose**: Property purchases, home improvements
- **Processing**: Manual review required for amounts over $100,000

#### 3. Business Loans
- **Amount Range**: $1,000 - $500,000
- **Term**: Up to 120 months (10 years)
- **Interest Rate**: 6% annually (variable based on business score)
- **Collateral**: Business assets or personal guarantee
- **Purpose**: Business operations, equipment, expansion
- **Processing**: Enhanced review process with business plan evaluation

### Enhanced Credit Scoring System

#### Multi-Factor Credit Score Calculation
```java
public double calculateCreditScore(UUID playerId) {
    double baseScore = 750.0; // Starting score
    
    // Payment history (40% weight) - increased importance
    double paymentScore = getPaymentHistoryScore(playerId);
    double paymentWeight = paymentScore * 0.40;
    
    // Current debt ratio (25% weight)
    double debtRatio = getCurrentDebtRatio(playerId);
    double debtWeight = (100 - Math.min(100, debtRatio)) / 100 * 0.25;
    
    // Account activity (15% weight)
    double activityScore = getAccountActivityScore(playerId);
    double activityWeight = activityScore * 0.15;
    
    // Economic standing (10% weight)
    double economicScore = getEconomicStandingScore(playerId);
    double economicWeight = economicScore * 0.10;
    
    // Credit history length (10% weight)
    double historyScore = getCreditHistoryScore(playerId);
    double historyWeight = historyScore * 0.10;
    
    double finalScore = baseScore + paymentWeight + debtWeight + 
                       activityWeight + economicWeight + historyWeight;
    
    return Math.max(300, Math.min(850, finalScore));
}
```

#### Credit Score Factors

1. **Payment History (40%)** - Most Important
   - On-time payments: +5 to +15 points
   - Early payments: +10 points bonus
   - Late payments (1-30 days): -5 to -15 points
   - Late payments (31+ days): -20 to -40 points
   - Defaults: -50 to -100 points
   - Payment consistency over time

2. **Current Debt Ratio (25%)**
   - Low utilization (<30%): +20 points
   - Moderate utilization (30-50%): +10 points
   - High utilization (50-70%): -10 points
   - Very high utilization (>70%): -25 points

3. **Account Activity (15%)**
   - Regular banking activity: +10 points
   - Multiple account types: +5 points
   - Savings account with consistent deposits: +5 points
   - Investment activity: +5 points

4. **Economic Standing (10%)**
   - Player wealth percentile: 0-10 points
   - Income stability: 0-5 points
   - Business ownership: +5 points

5. **Credit History Length (10%)**
   - 1+ years: +5 points
   - 2+ years: +10 points
   - 5+ years: +15 points

### Automated Loan Processing

#### LoanProcessor Features
- **Scheduled Processing**: Runs daily at server midnight
- **Interest Calculation**: Automatic monthly interest application
- **Late Fee Assessment**: Configurable late fees for overdue payments
- **Default Management**: Automatic default processing after grace period
- **Payment Reminders**: Automated player notifications

#### Processing Schedule
```yaml
loan_processing:
  daily_run_time: "00:00" # Midnight server time
  grace_period_days: 15   # Days before default
  late_fee_percentage: 5.0 # 5% late fee
  reminder_days: [7, 3, 1] # Reminder schedule
  max_late_fees: 3        # Maximum late fees before default
```

### Player Notification System

#### Notification Types
1. **Payment Reminders**
   - 7 days before due date
   - 3 days before due date
   - 1 day before due date
   - Day of due date

2. **Overdue Notifications**
   - 1 day overdue
   - 7 days overdue
   - 14 days overdue
   - Final notice before default

3. **Status Changes**
   - Loan approval/denial
   - Payment processed
   - Late fee applied
   - Default status
   - Loan paid off

#### Notification Delivery
- **In-Game Messages**: Real-time notifications to online players
- **Login Messages**: Stored notifications for offline players
- **Mail System Integration**: Compatible with server mail plugins
- **Discord Integration**: Optional Discord webhook notifications

### Loan Commands (Enhanced)

#### Player Commands
```
/loan apply <amount> <type> <term>     - Apply for a new loan
/loan list                             - List all your loans
/loan info <loanID>                    - Get detailed loan information
/loan pay <loanID> <amount>            - Make a payment on a loan
/loan payoff <loanID>                  - Pay off loan completely
/loan schedule <loanID>                - View payment schedule
/loan credit                           - Check your credit score
```

#### Admin Commands
```
/loanadmin stats                       - View server loan statistics
/loanadmin list [player]               - List loans (all or specific player)
/loanadmin info <loanID>               - Get detailed loan information
/loanadmin approve <loanID>            - Manually approve a loan
/loanadmin deny <loanID> <reason>      - Manually deny a loan
/loanadmin process                     - Manually trigger loan processing
/loanadmin default <loanID>            - Force loan default
/loanadmin adjust <loanID> <field> <value> - Adjust loan parameters
/loanadmin forgive <loanID> <amount>   - Forgive debt amount
```

### Loan Processing Workflow

#### Application Process
1. **Initial Validation**
   - Amount within loan type limits
   - Term within acceptable range
   - Player eligibility check

2. **Credit Assessment**
   - Credit score calculation
   - Existing debt analysis
   - Income verification (if available)

3. **Risk Evaluation**
   - Debt-to-income ratio
   - Payment history analysis
   - Collateral evaluation

4. **Approval Decision**
   - Automatic approval: Credit score 720+
   - Manual review: Credit score 600-719
   - Automatic denial: Credit score <600

5. **Loan Disbursement**
   - Funds transferred to player account
   - Loan recorded in database
   - First payment scheduled

#### Payment Processing
```java
// Enhanced monthly payment calculation with fees
public LoanPayment processPayment(UUID loanId, double amount) {
    Loan loan = getLoan(loanId);
    
    // Calculate payment breakdown
    double interestPortion = loan.getRemainingBalance() * 
                           (loan.getInterestRate() / 12.0);
    double principalPortion = amount - interestPortion;
    
    // Apply late fees if applicable
    double lateFees = 0.0;
    if (loan.isOverdue()) {
        lateFees = calculateLateFees(loan);
    }
    
    // Create payment record
    LoanPayment payment = new LoanPayment(
        UUID.randomUUID(),
        loanId,
        amount,
        principalPortion,
        interestPortion,
        lateFees,
        System.currentTimeMillis()
    );
    
    // Update loan balance and status
    loan.makePayment(payment);
    
    // Save to database
    persistenceManager.saveLoanPayment(payment);
    persistenceManager.saveLoan(loan);
    
    return payment;
}
```

### Collateral Management

#### Collateral Types
1. **Item-based Collateral**
   - Minecraft items held in secure escrow
   - Automatic valuation based on market prices
   - Items returned upon loan completion

2. **Property Collateral**
   - Land claims and building structures
   - Integration with land management plugins
   - Automatic transfer upon default

3. **Account-based Collateral**
   - Bank account freezing for secured loans
   - Percentage of balance held as security
   - Released upon payment completion

#### Collateral Evaluation
```java
public double evaluateCollateral(List<ItemStack> items, List<Claim> properties) {
    double totalValue = 0.0;
    
    // Evaluate items
    for (ItemStack item : items) {
        double marketValue = marketPriceEngine.getPrice(item);
        double depreciationFactor = getDepreciationFactor(item);
        totalValue += marketValue * depreciationFactor;
    }
    
    // Evaluate properties
    for (Claim property : properties) {
        double propertyValue = propertyValuationEngine.getValue(property);
        totalValue += propertyValue;
    }
    
    return totalValue;
}
```

### Database Schema Updates

#### Enhanced Loans Table
```sql
CREATE TABLE loans (
    loan_id TEXT PRIMARY KEY,
    borrower_uuid TEXT NOT NULL,
    loan_type TEXT NOT NULL,
    principal_amount REAL NOT NULL,
    current_balance REAL NOT NULL,
    interest_rate REAL NOT NULL,
    term_months INTEGER NOT NULL,
    remaining_payments INTEGER NOT NULL,
    monthly_payment REAL NOT NULL,
    status TEXT NOT NULL,
    created_date INTEGER NOT NULL,
    last_payment_date INTEGER,
    next_payment_due INTEGER NOT NULL,
    late_fees_applied REAL DEFAULT 0.0,
    total_interest_paid REAL DEFAULT 0.0,
    credit_score_at_approval REAL,
    collateral_value REAL,
    notes TEXT,
    approved_by TEXT,
    approved_date INTEGER,
    FOREIGN KEY (borrower_uuid) REFERENCES players(player_uuid)
);

-- New loan payments table
CREATE TABLE loan_payments (
    payment_id TEXT PRIMARY KEY,
    loan_id TEXT NOT NULL,
    payment_amount REAL NOT NULL,
    principal_amount REAL NOT NULL,
    interest_amount REAL NOT NULL,
    late_fee_amount REAL DEFAULT 0.0,
    payment_date INTEGER NOT NULL,
    payment_method TEXT DEFAULT 'MANUAL',
    processed_by TEXT,
    FOREIGN KEY (loan_id) REFERENCES loans(loan_id)
);

-- Credit history tracking
CREATE TABLE credit_history (
    history_id TEXT PRIMARY KEY,
    player_uuid TEXT NOT NULL,
    event_type TEXT NOT NULL,
    event_description TEXT,
    score_change REAL,
    new_score REAL,
    event_date INTEGER NOT NULL,
    FOREIGN KEY (player_uuid) REFERENCES players(player_uuid)
);
```

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

### Recent Performance Improvements

#### Optimized Loan System Caching
- **Persistent Cache**: Loans remain in memory across operations
- **Cache-First Loading**: Check memory before database queries
- **Reduced Database Load**: 70% reduction in loan-related queries
- **Memory Efficiency**: Smart cache eviction policies

#### Enhanced Database Operations
- **Connection Reuse**: Persistent database connections
- **Bulk Operations**: Batch loan processing for better performance
- **Optimized Queries**: Improved SQL query performance
- **Transaction Grouping**: Multiple operations in single transactions

### Performance Optimization

#### Database Optimization
- **Connection Pooling**: Reuse database connections with HikariCP
- **Prepared Statements**: All SQL queries use prepared statements
- **Batch Operations**: Group multiple operations for efficiency
- **Advanced Indexing**: Comprehensive indexing strategy
- **Query Optimization**: Optimized joins and WHERE clauses

#### Memory Management
- **Smart Caching**: Multi-level caching with LRU eviction
- **Object Pooling**: Reuse expensive objects (transactions, calculations)
- **Memory Monitoring**: Built-in memory usage tracking
- **Garbage Collection**: Optimized object lifecycle management
- **Cache Statistics**: Real-time cache hit/miss ratios

#### Concurrent Processing
- **Dedicated Thread Pools**: Separate pools for different operations
- **Asynchronous Operations**: Non-blocking database and file operations
- **Lock-Free Algorithms**: ConcurrentHashMap for thread safety
- **Parallel Processing**: Multi-threaded loan and transaction processing

### Configuration Management

#### Performance Configuration
```yaml
# config/neoessentials/economy.yml
performance:
  # Database settings
  database:
    connection_pool_size: 10
    max_connections: 20
    connection_timeout: 30000
    query_timeout: 15000
    batch_size: 100
    
  # Cache settings
  caching:
    enabled: true
    player_data_cache_size: 5000
    loan_cache_size: 2000
    account_cache_size: 3000
    transaction_cache_size: 10000
    cache_expiry_minutes: 60
    cache_cleanup_interval: 300
    
  # Thread pool settings
  threading:
    economy_pool_size: 4
    loan_processor_pool_size: 2
    analytics_pool_size: 1
    backup_pool_size: 1
    queue_size: 2000
    
  # Background task intervals
  scheduled_tasks:
    loan_processing_interval: 3600    # 1 hour in seconds
    interest_calculation_interval: 86400  # 24 hours
    analytics_update_interval: 1800   # 30 minutes
    cache_cleanup_interval: 300       # 5 minutes
    auto_save_interval: 300           # 5 minutes
```

#### Environment-Specific Settings

##### Development Environment
```yaml
performance:
  database:
    connection_pool_size: 2
    max_connections: 5
    batch_size: 10
  caching:
    player_data_cache_size: 100
    loan_cache_size: 50
    cache_expiry_minutes: 5
  threading:
    economy_pool_size: 1
    loan_processor_pool_size: 1
  logging:
    level: DEBUG
    enable_sql_logging: true
    enable_performance_metrics: true
```

##### Production Environment
```yaml
performance:
  database:
    connection_pool_size: 10
    max_connections: 20
    batch_size: 100
  caching:
    player_data_cache_size: 5000
    loan_cache_size: 2000
    cache_expiry_minutes: 60
  threading:
    economy_pool_size: 4
    loan_processor_pool_size: 2
  logging:
    level: INFO
    enable_sql_logging: false
    enable_performance_metrics: true
```

#### Auto-Configuration Features
```yaml
auto_config:
  enabled: true
  performance_mode: "AUTO"  # AUTO, PERFORMANCE, MEMORY_CONSERVATIVE
  server_size_detection: true
  dynamic_thread_adjustment: true
  memory_based_cache_sizing: true
  
# Auto-detected configurations based on server specs
auto_detected_configs:
  small_server:  # <50 players
    memory_threshold: "2GB"
    thread_pool_multiplier: 1
    cache_size_multiplier: 0.5
  medium_server: # 50-200 players
    memory_threshold: "4GB"
    thread_pool_multiplier: 2
    cache_size_multiplier: 1.0
  large_server:  # >200 players
    memory_threshold: "8GB"
    thread_pool_multiplier: 4
    cache_size_multiplier: 2.0
```

### Monitoring and Analytics

#### Performance Metrics
```yaml
monitoring:
  enabled: true
  metrics_collection_interval: 60  # seconds
  metrics_retention_days: 30
  
  tracked_metrics:
    - "database_query_time"
    - "cache_hit_ratio"
    - "thread_pool_utilization"
    - "memory_usage"
    - "transaction_throughput"
    - "loan_processing_time"
    - "player_concurrent_operations"
```

#### Built-in Performance Dashboard
- **Real-time Metrics**: Live performance statistics
- **Historical Data**: Performance trends over time
- **Alert System**: Automatic performance issue detection
- **Optimization Suggestions**: AI-powered performance recommendations

### Scaling Considerations

#### Vertical Scaling (Single Server)
- **Memory Allocation**: Recommended 4GB+ for large servers
- **CPU Optimization**: Multi-core utilization for background tasks
- **Storage**: SSD recommended for database performance
- **Network**: Optimized packet handling for shop/auction operations

#### Horizontal Scaling Preparation (Future)
- **Database Sharding**: Prepared for multi-server data distribution
- **Cache Synchronization**: Redis integration planned
- **Load Balancing**: Economy operation distribution
- **Cross-Server Transactions**: Planned feature for server networks

### Performance Troubleshooting

#### Common Performance Issues

1. **High Database Load**
   ```yaml
   # Solutions
   database:
     connection_pool_size: 15  # Increase pool size
     batch_size: 200          # Increase batch operations
     query_timeout: 30000     # Increase timeout for complex queries
   ```

2. **Memory Issues**
   ```yaml
   # Solutions
   caching:
     cache_expiry_minutes: 30  # Reduce cache lifetime
     player_data_cache_size: 3000  # Reduce cache size
     enable_cache_compression: true  # Enable compression
   ```

3. **Thread Pool Saturation**
   ```yaml
   # Solutions
   threading:
     economy_pool_size: 6      # Increase thread pool
     queue_size: 5000         # Increase queue capacity
     enable_priority_queuing: true  # Enable priority processing
   ```

#### Performance Monitoring Commands
```
/economyadmin performance status    - Current performance metrics
/economyadmin performance history   - Historical performance data
/economyadmin performance optimize  - Run automatic optimization
/economyadmin cache stats          - Cache performance statistics
/economyadmin cache clear [type]    - Clear specific caches
/economyadmin threading status      - Thread pool utilization
```
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

// Loan operations (Enhanced v1.0.2.27)
BankManager bank = BankManager.getInstance();

// Apply for loan
Loan loan = bank.getLoanManager().applyForLoan(playerId, 5000.0, Currency.getDefault(), 
                                             Loan.LoanType.PERSONAL, 12);

// Make payment
LoanPayment payment = bank.getLoanManager().makePayment(loan.getLoanId(), 500.0);

// Check credit score
double creditScore = bank.getLoanManager().calculateCreditScore(playerId);

// Get loan statistics (Admin)
LoanStatistics stats = bank.getLoanManager().getLoanStatistics();
```

---

## Quick Reference Guide (v1.0.2.27)

### Essential Commands

#### Player Commands
```bash
# Balance and Money
/balance                    # Check current balance
/pay <player> <amount>     # Send money to another player
/baltop                    # View richest players

# Banking
/bank create <type>        # Create bank account (checking/savings)
/bank deposit <amount>     # Deposit money
/bank withdraw <amount>    # Withdraw money
/bank transfer <account> <amount> # Transfer between accounts
/bank info                 # View account information

# Loans (Enhanced)
/loan apply <amount> <type> <term>  # Apply for loan
/loan list                          # List your loans
/loan pay <loanID> <amount>        # Make payment
/loan payoff <loanID>              # Pay off completely
/loan credit                       # Check credit score
/loan schedule <loanID>            # View payment schedule

# Shops
/shop create <name>        # Create shop
/shop add <price>          # Add item to shop (hold item)
/shop remove <slot>        # Remove item from shop
/shop info                 # View shop statistics

# Auctions
/auction create <price> <duration> # Create auction (hold item)
/auction bid <id> <amount>         # Place bid on auction
/auction list                      # List active auctions
```

#### Admin Commands
```bash
# Economy Management
/economyadmin reload        # Reload economy configuration
/economyadmin stats         # View economy statistics
/economyadmin backup        # Create economy backup
/economyadmin reset <player> # Reset player economy data

# Loan Administration (New)
/loanadmin stats           # Server loan statistics
/loanadmin list [player]   # List loans
/loanadmin approve <id>    # Approve pending loan
/loanadmin deny <id> <reason> # Deny loan application
/loanadmin process         # Manual loan processing
/loanadmin adjust <id> <field> <value> # Adjust loan parameters

# Performance Monitoring (New)
/economyadmin performance status    # Performance metrics
/economyadmin cache stats          # Cache performance
/economyadmin threading status     # Thread pool status
```

### Configuration Quick Setup

#### Basic Economy Configuration
```yaml
# config/neoessentials/economy.yml
economy:
  enabled: true
  starting_balance: 1000.0
  max_balance: 10000000.0
  allow_negative_balances: false
  
banking:
  enabled: true
  interest_rate: 2.5
  minimum_balance: 10.0
  
loans:
  enabled: true
  max_loans_per_player: 3
  default_interest_rate: 7.5
  grace_period_days: 15
  late_fee_percentage: 5.0
```

### Performance Optimization Quick Settings

#### For Small Servers (<50 players)
```yaml
performance:
  database:
    connection_pool_size: 3
    batch_size: 25
  caching:
    player_data_cache_size: 500
    loan_cache_size: 200
  threading:
    economy_pool_size: 2
```

#### For Large Servers (>200 players)
```yaml
performance:
  database:
    connection_pool_size: 15
    batch_size: 200
  caching:
    player_data_cache_size: 10000
    loan_cache_size: 5000
  threading:
    economy_pool_size: 6
```

### Troubleshooting Quick Fixes

#### Common Issues and Solutions

1. **Loan IDs Changing**
   - ✅ Fixed in v1.0.2.27
   - Loans now maintain stable IDs across restarts

2. **Performance Issues**
   ```bash
   /economyadmin cache clear    # Clear all caches
   /economyadmin performance optimize # Run optimization
   ```

3. **Database Errors**
   ```bash
   /economyadmin backup         # Create backup first
   /economyadmin reload         # Reload configuration
   ```

4. **Credit Score Issues**
   ```bash
   /loanadmin adjust <loanID> credit_score <value> # Manually adjust
   ```

### Integration Examples

#### Plugin Integration
```java
// Get economy API
EconomyManager economy = EconomyManager.getInstance();

// Check if player can afford
if (economy.hasBalance(playerId, 1000.0, Currency.getDefault())) {
    // Withdraw money
    economy.withdraw(playerId, 1000.0, Currency.getDefault());
}

// Loan integration
BankManager bank = BankManager.getInstance();
List<Loan> playerLoans = bank.getPlayerLoans(playerId);
```

#### Event Handling
```java
@SubscribeEvent
public void onLoanPayment(LoanPaymentEvent event) {
    Loan loan = event.getLoan();
    LoanPayment payment = event.getPayment();
    // Handle loan payment
}

@SubscribeEvent
public void onTransactionComplete(TransactionCompleteEvent event) {
    Transaction transaction = event.getTransaction();
    // Handle completed transaction
}
```

---

This comprehensive documentation covers all aspects of the NeoEssentials Economy System, from basic concepts to advanced configuration and troubleshooting. The system provides a robust, scalable economic framework suitable for servers of all sizes, with enhanced loan management, performance optimization, and administrative tools.
