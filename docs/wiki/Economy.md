# Economy System

NeoEssentials includes a comprehensive economy system for managing server currency, transactions, and player balances. This system integrates seamlessly with the GUI interfaces and provides both command-line and graphical management tools.

## 💰 Basic Economy Features

### Currency Management
```bash
/balance [player]             # Check balance
/bal [player]                 # Alias for balance
/money [player]               # Alternative balance command
/eco balance <player>         # Admin balance check
```

### Basic Transactions
```bash
/pay <player> <amount>        # Pay another player
/eco give <player> <amount>   # Give money (admin)
/eco take <player> <amount>   # Take money (admin)  
/eco set <player> <amount>    # Set exact balance (admin)
/eco reset <player>           # Reset balance to default
```

**Examples:**
```bash
/pay John 500                 # Pay John 500 coins
/eco give Steve 1000          # Give Steve 1000 coins
/eco set Alice 5000           # Set Alice's balance to 5000
```

## 🏪 Shop System Integration

### GUI Shop Interface
```bash
/shop                         # Open main shop GUI
/shop <category>              # Open specific category
/sell                         # Open selling interface
/sell hand                    # Quick sell item in hand
/sell all                     # Sell all sellable items
```

### Shop Categories
The shop system organizes items into logical categories:

- **🏗️ Building** - Construction materials, decorative blocks
- **⚔️ Combat** - Weapons, armor, combat supplies  
- **🍖 Food** - Food items, agricultural products
- **🔧 Tools** - Tools, utility items, equipment
- **🧪 Brewing** - Potions, brewing supplies, enchanting
- **📦 Misc** - Miscellaneous items, rare materials

### Shop Configuration
Items and prices are configured in `config/gui/shop_gui.json`:

```json
{
  "items": {
    "diamond_sword": {
      "item": "minecraft:diamond_sword",
      "name": "§b💎 Diamond Sword",
      "price": 500,
      "sellPrice": 250,
      "category": "combat",
      "lore": [
        "§7A powerful diamond sword",
        "§7Perfect for combat",
        "",
        "§6Buy Price: §f500 coins",
        "§eSell Price: §f250 coins",
        "§eClick to purchase!"
      ]
    }
  }
}
```

## 💳 Advanced Economy Features

### Banking System
```bash
/bank                         # Open banking interface
/bank deposit <amount>        # Deposit money to bank
/bank withdraw <amount>       # Withdraw money from bank  
/bank interest                # Check interest rates
/bank transfer <player> <amount> # Bank transfer
```

**Banking Features:**
- **Interest System** - Earn interest on banked money
- **Safe Storage** - Protected money storage
- **Transfer System** - Secure player-to-player transfers
- **Transaction History** - Complete transaction logs

### Auction House
```bash
/auction                      # Open auction house GUI
/auction sell <price>         # Auction item in hand
/auction list                 # List your auctions
/auction cancel <id>          # Cancel auction
/auction collect              # Collect auction earnings
```

**Auction Features:**
- **Timed Auctions** - Items sell after set duration
- **Bidding System** - Players can bid on items
- **Reserve Prices** - Minimum selling prices
- **Auto-collection** - Automatic earning collection

### Player Shops
```bash
/playershop                   # Open player shop browser
/playershop create            # Create your shop
/playershop manage            # Manage your shop
/playershop visit <player>    # Visit player's shop
```

## 🎯 Economy GUI System

### Economy Management Interface
```bash
/economy                      # Open economy management (admin)
```

**Management Features:**
- **Server Balance** - Total server economy overview
- **Player Rankings** - Richest players leaderboard
- **Transaction Logs** - Recent economy activity
- **Price Management** - Adjust shop prices
- **Currency Control** - Mint/remove currency

### Transaction History
```bash
/transactions                 # View your transaction history
/transactions <player>        # View player transactions (admin)
```

**Transaction Types:**
- **Purchases** - Shop item purchases
- **Sales** - Item sales to shop/players
- **Payments** - Player-to-player payments
- **Admin Actions** - Admin money adjustments
- **Interest** - Bank interest payments

## 💱 Currency Configuration

### Base Currency Settings
Configure in `config/neoessentials/economy.toml`:

```toml
[currency]
# Currency name and symbol
name = "Coins"
symbol = "$"
plural = "Coins"
singular = "Coin"

# Starting balance for new players
startingBalance = 1000.0

# Decimal places for currency display
decimalPlaces = 2

[limits]
# Maximum balance a player can have
maxBalance = 999999999.0

# Minimum balance (can be negative for debt)
minBalance = -10000.0

# Maximum transaction amount
maxTransaction = 100000.0

[bank]
# Enable banking system
enabled = true

# Interest rate (percentage per day)
interestRate = 0.5

# Maximum bank balance
maxBankBalance = 10000000.0
```

### Shop Pricing Configuration
```json
{
  "pricing": {
    "multipliers": {
      "vip": 0.9,        // VIP players get 10% discount
      "staff": 0.8,      // Staff get 20% discount
      "default": 1.0     // Regular pricing
    },
    "sellMultiplier": 0.5,  // Sell for 50% of buy price
    "taxRate": 0.05         // 5% transaction tax
  }
}
```

## 📊 Economy Statistics & Analytics

### Server Economy Dashboard
```bash
/eco stats                    # Economy statistics overview
/eco top [amount]             # Richest players list
/eco activity                 # Recent economy activity
/eco trends                   # Economy trend analysis
```

**Statistics Include:**
- **Total Currency** - Money in circulation
- **Transaction Volume** - Daily/weekly transaction amounts
- **Player Wealth Distribution** - Rich vs poor analysis
- **Shop Activity** - Most purchased/sold items
- **Price Trends** - Item price changes over time

### Player Economy Stats
```bash
/eco playerstats [player]     # Individual player economy stats
```

**Player Stats:**
- **Total Earnings** - All-time money earned
- **Total Spending** - All-time money spent
- **Net Worth** - Current total wealth
- **Transaction Count** - Number of transactions
- **Favorite Purchases** - Most bought items

## 🛡️ Economy Security & Anti-Cheat

### Transaction Monitoring
```bash
/eco monitor                  # Monitor suspicious transactions
/eco investigate <player>     # Investigate player's economy
/eco freeze <player>          # Freeze player's economy
/eco unfreeze <player>        # Unfreeze player's economy
```

**Security Features:**
- **Large Transaction Alerts** - Notifications for big transactions
- **Rapid Transaction Detection** - Identify potential duping
- **Balance Change Monitoring** - Track unusual balance changes
- **Admin Transaction Logging** - Log all admin economy actions

### Economy Protection
```toml
[security]
# Enable economy security monitoring
enabled = true

# Alert threshold for large transactions
largeTransactionThreshold = 10000.0

# Maximum transactions per hour per player
maxTransactionsPerHour = 100

# Enable automatic fraud detection
fraudDetection = true

# Freeze account on suspicious activity
autoFreeze = false
```

## 🎁 Rewards & Incentives

### Daily Rewards
```bash
/daily                        # Claim daily reward
/daily streak                 # Check daily streak
/weekly                       # Claim weekly reward
/monthly                      # Claim monthly reward
```

**Reward Configuration:**
```toml
[rewards.daily]
enabled = true
baseAmount = 100.0
streakMultiplier = 1.1
maxStreak = 30

[rewards.weekly]
enabled = true
amount = 1000.0

[rewards.monthly]
enabled = true
amount = 5000.0
```

### Activity Rewards
- **Playtime Rewards** - Money for time spent online
- **Achievement Rewards** - Money for completing achievements
- **Voting Rewards** - Money for voting for the server
- **Event Rewards** - Special event bonuses

## 🔧 Economy Commands for Admins

### Administrative Tools
```bash
/eco info                     # Economy system information
/eco reload                   # Reload economy configuration
/eco backup                   # Backup economy data
/eco restore <backup>         # Restore from backup
/eco migrate                  # Migrate economy data
```

### Bulk Operations
```bash
/eco givall <amount>          # Give money to all online players
/eco takeall <amount>         # Take money from all players
/eco setall <amount>          # Set balance for all players
/eco reset all                # Reset all player balances
```

### Economy Management
```bash
/eco inflation <percentage>   # Apply inflation to all balances
/eco deflation <percentage>   # Apply deflation to all balances
/eco tax <percentage>         # Apply tax to all players
/eco bonus <amount>           # Give bonus to all players
```

## 🎯 Integration Features

### Permission-Based Pricing
```yaml
# Different pricing for different groups
shop.prices.vip: 0.9         # 10% discount for VIP
shop.prices.staff: 0.8       # 20% discount for staff
shop.sell.premium: 1.2       # 20% bonus sell price for premium
```

### External Plugin Integration
- **Job Plugin Integration** - Earn money from jobs
- **Land Claiming** - Economy for land purchases
- **Auction House Plugins** - Extended auction features
- **Vault API** - Compatible with other economy plugins

### API Integration
```java
// Example API usage for developers
EconomyAPI api = NeoEssentials.getEconomyAPI();
double balance = api.getBalance(player);
boolean success = api.transferMoney(fromPlayer, toPlayer, amount);
```

## 📈 Economy Balancing

### Price Management
```bash
/eco prices                   # View all item prices
/eco price set <item> <price> # Set item price
/eco price adjust <item> <percent> # Adjust price by percentage
/eco market analysis          # Market price analysis
```

### Supply & Demand
```toml
[market]
# Enable dynamic pricing based on supply/demand
dynamicPricing = true

# Price adjustment factors
supplyFactor = 0.1
demandFactor = 0.15

# Maximum price change per day
maxPriceChange = 0.2
```

## 🛠️ Troubleshooting Economy Issues

### Common Problems

#### Negative Balances
- Check minimum balance settings
- Verify transaction logs for errors
- Use `/eco set <player> <amount>` to fix

#### Transaction Failures
- Check player permissions
- Verify sufficient balance
- Review transaction limits

#### Shop Not Working
- Verify GUI system is enabled
- Check shop configuration files
- Reload GUI configurations

### Debug Commands
```bash
/eco debug <player>           # Debug player's economy
/eco verify                   # Verify economy data integrity
/eco test transaction         # Test transaction system
```

---

## 📚 Related Documentation

- **[GUI System](GUI-System.md)** - Shop interface customization
- **[Player Management](Player-Management.md)** - Managing player economics
- **[Configuration](Configuration.md)** - Economy configuration options
- **[Permissions](Permissions.md)** - Economy permission setup

*Last Updated: August 6, 2025*
