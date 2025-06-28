# Economy System Quick Reference

This page provides quick access to the most commonly needed information about the NeoEssentials Economy System. For complete documentation, see the [Full Economy System Documentation](Economy-System).

## 📋 Quick Start

### Enable Economy System
```yaml
# config/neoessentials/economy.yml
economy:
  enabled: true
  starting_balance: 100.0
  max_balance: 1000000.0
```

### Essential Commands
```
/balance [player] - Check balance
/pay <player> <amount> - Send money
/bank account create <type> - Create bank account
/shop create <name> - Create a shop
```

## 🏦 Banking Quick Reference

### Account Types
| Type | Interest Rate | Features |
|------|---------------|----------|
| **Checking** | 1% | Daily transactions, no limits |
| **Savings** | 5% | Higher interest, withdrawal limits |
| **Business** | 2% | Business operations, analytics |
| **Investment** | 8% | Highest interest, 12-month minimum |

### Banking Commands
```
/bank account create <type> - Create account
/bank deposit <account> <amount> - Deposit money
/bank withdraw <account> <amount> - Withdraw money
/bank transfer <from> <to> <amount> - Transfer funds
/bank balance <account> - Check account balance
```

## 💰 Currency System

### Currency Types
- **Standard** (🪙): Virtual server currency (Coins)
- **Resource** (⚆): Backed by items (Gold Ingots, Diamonds)
- **Token** (✦): Special event currencies

### Currency Commands
```
/currency list - List all currencies
/currency exchange <from> <to> <amount> - Exchange currencies
/currency rates - View exchange rates
```

## 🏪 Shop System

### Shop Management
```
/shop create <name> - Create new shop
/shop list - List your shops
/shop info <shop> - View shop details
/shop add <item> <price> - Add item to shop
/shop remove <item> - Remove item from shop
/shop price <item> <price> - Update item price
```

### Shop Features
- **Dynamic Pricing**: Prices adjust based on supply/demand
- **Taxation**: 5% sales tax on transactions
- **Rental System**: Daily rental fees for shop locations

## 🏛️ Auction House

### Auction Commands
```
/auction create <item> <starting_bid> [duration] - Create auction
/auction bid <auction> <amount> - Place bid
/auction list - View active auctions
/auction search <item> - Search for auctions
```

### Auction Features
- **Duration**: 1-168 hours (1 hour to 7 days)
- **Fees**: 2% listing fee, 5% success fee
- **Bidding**: 5% minimum bid increment

## 💳 Loan System

### Loan Types
| Type | Amount Range | Term | Interest | Collateral |
|------|-------------|------|----------|------------|
| **Personal** | $500-$50K | 60 months | 8% | Required |
| **Mortgage** | $10K-$1M | 360 months | 5% | Property |
| **Business** | $1K-$500K | 120 months | 6% | Required |

### Loan Commands
```
/loan apply <type> <amount> <term> - Apply for loan
/loan list - View your loans
/loan payment <loan> - Make payment
/loan balance <loan> - Check remaining balance
```

### Credit Score Factors
- **Payment History** (35%): On-time payments
- **Credit Utilization** (30%): Debt-to-limit ratio
- **Credit History** (15%): Length of credit history
- **Credit Mix** (10%): Types of credit accounts
- **New Credit** (10%): Recent credit inquiries

## 📊 Economic Analytics

### Health Indicators
- **Economic Velocity**: Money circulation rate
- **Wealth Distribution**: Gini coefficient (inequality measure)
- **Inflation Rate**: Price level changes
- **Market Liquidity**: Available cash circulation

### Analytics Commands
```
/economy stats - View economic statistics
/economy health - Check economic health
/economy report daily - Generate daily report
/economy analytics - View detailed analytics
```

## ⚙️ Admin Commands

### Player Management
```
/economy balance <player> [currency] - Check player balance
/economy set <player> <amount> [currency] - Set player balance
/economy give <player> <amount> [currency] - Give money
/economy take <player> <amount> [currency] - Take money
```

### System Administration
```
/economy reload - Reload configuration
/economy backup - Create manual backup
/economy cleanup - Clean old transaction data
/economy status - View system status
```

### Banking Administration
```
/bank admin create <player> <type> - Create account for player
/bank admin close <account> - Close bank account
/bank admin interest - Trigger interest calculation
/bank admin statement <account> - View account statement
```

## 🔧 Configuration Quick Settings

### Performance Tuning
```yaml
performance:
  background_tasks:
    thread_pool_size: 2    # Increase for busy servers
    queue_size: 1000       # Increase for high transaction volume
  caching:
    player_data_cache_size: 1000  # Increase for more players
    cache_expiry: 30       # Minutes
```

### Economic Settings
```yaml
economy:
  starting_balance: 100.0
  max_balance: 1000000.0
  allow_negative_balances: false
  inflation_rate: 0.02
```

### Banking Settings
```yaml
banking:
  enabled: true
  account_creation:
    creation_fee: 100.0
    max_accounts_per_player: 5
    auto_create_checking: true
```

## 🚨 Common Issues & Solutions

### Issue: Economy Not Loading
- Check `economy.enabled: true` in config
- Verify database connectivity
- Review server logs for errors

### Issue: Transaction Failures
- Verify sufficient player balance
- Check currency configuration
- Test database operations

### Issue: Performance Problems
- Increase thread pool sizes
- Optimize database queries
- Increase memory allocation

## 📚 Additional Resources

- [Full Economy System Documentation](Economy-System) - Complete technical documentation
- [Configuration Guide](Configuration-Guide) - General configuration help
- [Commands Reference](Commands-Reference) - All available commands
- [Troubleshooting](Troubleshooting) - Common issues and solutions
- [API Documentation](API-Documentation) - Developer integration

---

*For complete technical details, formulas, and advanced configuration, see the [Full Economy System Documentation](Economy-System).*
