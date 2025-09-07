
# NeoEssentials Economy System

The NeoEssentials Economy System provides a robust virtual economy for your Minecraft server, featuring a dual-manager architecture with advanced multi-currency support, comprehensive balance management, banking operations, transaction analytics, and extensive administrative tools.

---

## 💰 Economy Features

NeoEssentials includes two economy managers that work together:

### Dual Manager Architecture
- **Advanced Economy Manager** (`economy.EconomyManager`) - Provides multi-currency support, banking, transaction history, analytics
- **Memory-Optimized Manager** (`managers.EconomyManager`) - Handles basic Vault-compatible operations with performance optimization

### Core Features
- Player balances with BigDecimal precision for accurate calculations
- Player-to-player payments with transaction logging
- Economy leaderboard with rich formatting
- Advanced administrative commands for balance management
- Multi-currency support with exchange rates
- Banking system with account management
- Comprehensive transaction history and analytics
- Background tasks for data cleanup and optimization
- Vault integration for plugin compatibility
- Cache optimization for high-performance servers

---

## ⚙️ Configuration

The economy system is configured through the main `config/config.json` file in the economy settings section. Here's the configuration structure:

```json
{
    "economy": {
        "enabled": true,
        "startingBalance": 500.0,
        "currencySymbol": "$",
        "maxBalance": 999999.0,
        "maxTransferAmount": 50000.0,
        "transactionFeePercent": 0.0,
        "cleanupInactiveAccounts": false
    }
}
```

### Configuration Options

- `enabled` - Enable/disable the entire economy system
- `startingBalance` - Default balance for new players (BigDecimal precision)
- `currencySymbol` - Primary symbol displayed with currency amounts
- `maxBalance` - Maximum balance a player can have
- `maxTransferAmount` - Maximum amount for single transfers
- `transactionFeePercent` - Fee percentage for player-to-player transactions
- `cleanupInactiveAccounts` - Remove balances for inactive players

**Note:** The economy configuration is part of the unified JSON configuration system, not a separate `economy.json` file.
```

---

## 📝 Player Commands

### Balance Commands
```bash
/bal [player]         # Check your own or another player's balance
/balance [player]     # Alias for /bal - Check balance
/baltop [limit]       # View economy leaderboard (default limit: 10, max: 50)
/balancetop [limit]   # Alias for /baltop - View top balances
```

### Payment Commands
```bash
/pay <player> <amount>  # Send money to another player
                        # Minimum amount: 0.01, includes transaction logging
```

---

## 🛠️ Admin Commands

### Advanced Economy Management (`/economy`)
```bash
/economy balance check <player> <currency>    # Check player's balance in specific currency
/economy balance set <player> <currency> <amount>    # Set player's balance
/economy balance add <player> <currency> <amount>    # Add money to player's balance
/economy balance remove <player> <currency> <amount> # Remove money from player's balance
```

### Basic Admin Commands (`/eco`)
```bash
/eco give <player> <amount>  # Give money to a player
/eco take <player> <amount>  # Remove money from a player
/eco set <player> <amount>   # Set a player's balance to exact amount
```

**Permission Requirements:**
- Advanced `/economy` commands require `neoessentials.moderation.basic`
- Basic `/eco` commands require specific permissions (see below)

---

## 🔒 Permission Nodes

### Player Economy Permissions
```
neoessentials.balance           # Use /balance and /bal commands
neoessentials.balance.others    # Check other players' balances
neoessentials.pay               # Send money to other players via /pay
neoessentials.balancetop        # View economy leaderboard via /baltop
```

### Administrative Permissions
```
neoessentials.eco.*             # All basic economy admin permissions
neoessentials.eco.give          # Use /eco give command
neoessentials.eco.take          # Use /eco take command  
neoessentials.eco.set           # Use /eco set command
neoessentials.eco.reset         # Reset economy data (if implemented)
```

### Advanced Economy Features
```
neoessentials.economy.analytics    # Access economy analytics and reports
neoessentials.economy.transactions # View detailed transaction history
neoessentials.economy.history      # Access comprehensive economy history
neoessentials.moderation.basic     # Required for /economy advanced commands
```

---

## 🏗️ Technical Architecture

### Dual Manager System
NeoEssentials uses a sophisticated dual-manager architecture:

#### Advanced Economy Manager (`economy.EconomyManager`)
- **Purpose:** Advanced features with multi-currency support, banking, analytics
- **Features:** 
  - Multi-currency management with exchange rates
  - Banking system with account operations
  - Comprehensive transaction history and analytics
  - Background cleanup and optimization tasks
  - Currency management and exchange operations
- **Integration:** Used for advanced administrative commands and multi-currency operations

#### Memory-Optimized Manager (`managers.EconomyManager`)  
- **Purpose:** High-performance Vault-compatible economy operations
- **Features:**
  - Streamlined balance operations with cache optimization
  - Thread-safe singleton pattern for concurrent access
  - BigDecimal precision for accurate financial calculations
  - Transaction history with detailed logging
  - Bank account support for advanced features
  - Background cleanup tasks for inactive accounts
- **Integration:** Used for player commands, Vault compatibility, and core operations

### Data Precision
- All monetary values use `BigDecimal` for precise decimal calculations
- Prevents floating-point errors common in economy plugins  
- Supports transaction amounts from 0.01 to configured maximums
- Transaction history maintains full precision records

### Performance Optimization
- **Caching:** Balance and transaction data cached for high-performance access
- **Background Tasks:** Automated cleanup and optimization processes
- **Thread Safety:** All operations are thread-safe for concurrent server access
- **Memory Management:** Efficient storage and cleanup of inactive player data

---

## 💡 Usage Examples

### Player Balance Management
```bash
# Check your own balance
/bal

# Check another player's balance (requires permission)
/balance Steve

# View top 10 richest players
/baltop

# View top 25 richest players
/baltop 25
```

### Player Payments
```bash
# Send $100 to player "Alex"
/pay Alex 100

# Send $50.75 to player "Steve" (supports decimals)
/pay Steve 50.75
```

### Administrative Operations
```bash
# Give $500 to new player
/eco give NewPlayer 500

# Remove $25 from player who broke rules
/eco take Griefer 25

# Set player's balance to exactly $1000
/eco set VIP 1000

# Advanced: Set player's euro balance (multi-currency)
/economy balance set Player euros 250
```

---

## 🔧 Troubleshooting

### Common Issues

**"You don't have enough money" Errors:**
- Check player's current balance with `/bal`
- Verify the payment amount doesn't exceed `maxTransferAmount`
- Ensure sender has sufficient funds plus any transaction fees
- Check that economy system is enabled in configuration

**Permission Denied Errors:**
- Verify player has required permission nodes
- Check that permissions plugin is properly configured
- Ensure `neoessentials.balance.others` for checking other players' balances
- Confirm admin permissions for `/eco` and `/economy` commands

**Commands Not Working:**
- Verify economy system is enabled in `config/config.json`
- Check that configuration syntax is valid JSON
- Ensure both economy managers are properly initialized
- Look for startup errors in server console logs

**Balance/Transaction Issues:**
- Check maximum balance limits in configuration
- Verify transaction amounts meet minimum requirements (0.01)
- Ensure BigDecimal precision isn't causing overflow errors
- Review transaction logs for detailed error information

### Debug Information
- Transaction history is automatically logged for troubleshooting
- Administrative commands provide detailed success/failure feedback
- Economy analytics can help identify system-wide issues
- Background task logs show cleanup and optimization status

### Performance Considerations
- Large baltop requests may impact performance - use reasonable limits
- Frequent balance checks are cached for optimal performance  
- Background cleanup tasks run automatically to maintain system health
- Consider `cleanupInactiveAccounts` for servers with many inactive players

---

**Related Documentation:**
- [Configuration Guide](Configuration.md) - Main configuration settings
- [Permissions System](Permissions.md) - Permission node details  
- [API Documentation](API_DOCUMENTATION.md) - Developer integration
- [Custom Commands](Custom-Commands.md) - All available commands

*Documentation updated to reflect actual NeoEssentials 1.0.2.1 implementation*
