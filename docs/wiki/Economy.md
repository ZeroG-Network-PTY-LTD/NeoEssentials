
# NeoEssentials Economy System

The NeoEssentials 2.1.0 Economy System provides a robust virtual economy for your Minecraft server, featuring a streamlined architecture with balance management, player transactions, and administrative tools.

---

## 💰 Economy Features

NeoEssentials includes a unified economy system that provides:

### Core Features
- **Player Balances** - BigDecimal precision for accurate calculations
- **Player-to-Player Payments** - Secure money transfers with validation
- **Balance Management** - Administrative commands for balance control
- **Economy Leaderboard** - Rich formatting for top player balances
- **Transaction Logging** - Comprehensive tracking of all economic activities
- **Vault Integration** - Compatibility with other economy-dependent plugins
- **Cache Optimization** - High-performance for busy servers

### System Architecture
- **EconomyManager** (`managers.EconomyManager`) - Main economy system with Vault compatibility
- **EconomyCommands** (`commands.EconomyCommands`) - Basic commands (/bal, /pay, /eco)
- **EconomyCommand** (`economy.EconomyCommand`) - Advanced economy command system (documentation only)

---

## ⚙️ Configuration

The economy system is configured through the main `config/neoessentials/config.json` file in the economy settings section:

```json
{
  "economySettings": {
    "enabled": true,
    "startingBalance": 100.0,
    "currencySymbol": "$",
    "maxBalance": 100000.0,
    "cleanupInactiveAccounts": true,
    "transactionFeePercent": 1.0,
    "maxTransferAmount": 10000.0
  }
}
```

### Configuration Options

- `enabled` - Enable/disable the entire economy system
- `startingBalance` - Default balance for new players (BigDecimal precision)
- `currencySymbol` - Symbol used for currency display (e.g., "$", "€", "¥")
- `maxBalance` - Maximum balance a player can have
- `cleanupInactiveAccounts` - Remove inactive player accounts automatically
- `transactionFeePercent` - Percentage fee for transactions (1.0 = 1%)
- `maxTransferAmount` - Maximum amount allowed in a single transfer

---

## 💵 Basic Commands

The economy system provides essential commands for players and administrators:

### Player Commands

**Check Balance:**
```bash
/balance           # Check your own balance
/bal              # Short form
/balance <player>  # Check another player's balance (requires permission)
```

**Send Money:**
```bash
/pay <player> <amount>    # Send money to another player
```

**Economy Leaderboard:**
```bash
/baltop           # Show top 10 richest players
/baltop <number>  # Show top X players (max 50)
```

### Administrative Commands

**Give Money:**
```bash
/eco give <player> <amount>    # Give money to a player
```

**Take Money:**
```bash
/eco take <player> <amount>    # Remove money from a player
```

**Set Balance:**
```bash
/eco set <player> <amount>     # Set a player's balance
```

---

## � System Features

### Balance Management
- **BigDecimal Precision** - Accurate financial calculations without rounding errors
- **Automatic Validation** - Prevents negative balances and overflow
- **Transaction Logging** - All balance changes are logged with reasons
- **Balance Limits** - Configurable maximum balances per player

### Payment System
- **Secure Transfers** - Validates sender has sufficient funds
- **Anti-Self-Payment** - Prevents players from paying themselves
- **Transaction Fees** - Optional percentage-based fees
- **Transfer Limits** - Maximum transfer amounts to prevent abuse

### Leaderboard System
- **Top Balances** - Shows richest players on the server
- **Rich Formatting** - Color-coded rankings and formatted amounts
- **Game Profile Integration** - Shows player names even when offline
- **Configurable Limits** - Customizable leaderboard size

---

## 📊 Economy Management

### Administrative Features
- **Balance Control** - Set, add, or remove player balances
- **Transaction Monitoring** - Track all economic activities
- **Inactive Account Cleanup** - Automatic removal of dormant accounts
- **Performance Optimization** - Efficient caching and data management

### Integration Features
- **Vault Compatibility** - Works with other economy-dependent plugins
- **Permission Integration** - Respects server permission systems
- **Localization Support** - Multi-language message support
- **Command Cooldowns** - Configurable cooldowns to prevent spam

---

## 🎯 Usage Examples

### Basic Player Usage
```bash
# Check your balance
/balance
# Output: Your balance: $1,000.00

# Pay another player
/pay Steve 250
# Output: Sent $250.00 to Steve

# Check the leaderboard
/baltop 5
# Shows top 5 richest players
```

### Administrative Usage
```bash
# Give starting funds to a new player
/eco give NewPlayer 500
# Output: Gave $500.00 to NewPlayer

# Remove money from a player (punishment)
/eco take Griefer 1000
# Output: Removed $1,000.00 from Griefer

# Set exact balance for testing
/eco set TestPlayer 5000
# Output: Set TestPlayer's balance to $5,000.00
```

---

## 🔒 Permissions

### Player Permissions
- `neoessentials.balance` - Check your own balance
- `neoessentials.balance.others` - Check other players' balances
- `neoessentials.pay` - Send money to other players
- `neoessentials.baltop` - View economy leaderboard

### Administrative Permissions
- `neoessentials.economy.admin.give` - Give money to players
- `neoessentials.economy.admin.take` - Remove money from players
- `neoessentials.economy.admin.set` - Set player balances
- `neoessentials.economy.admin.bypass` - Bypass transfer limits and fees

---

## ⚡ Performance Features

### Optimization
- **Efficient Caching** - Balance data cached for fast access
- **Lazy Loading** - Player data loaded only when needed
- **Background Processing** - Non-critical operations run asynchronously
- **Memory Management** - Automatic cleanup of unused data

### Scalability
- **High Concurrency** - Thread-safe operations for busy servers
- **Batch Operations** - Efficient bulk operations for administrative tasks
- **Resource Monitoring** - Tracks memory and performance metrics

---

## 🔍 Troubleshooting

### Common Issues

#### Economy Not Working
- Check that `economySettings.enabled` is `true` in config
- Verify Vault is installed if using economy-dependent plugins
- Check server logs for economy initialization errors

#### Balance Issues
- Ensure `startingBalance` is set appropriately
- Check `maxBalance` limits aren't too restrictive
- Verify player has necessary permissions

#### Performance Problems
- Reduce `maxTransferAmount` if needed
- Enable `cleanupInactiveAccounts` for automatic cleanup
- Monitor server logs for economy-related errors

---

## 📚 Related Documentation

- [Commands Guide](Commands.md) - Complete command documentation
- [Permissions Guide](Permissions.md) - Permission system setup
- [Configuration Guide](Configuration.md) - Configuration management
- [API Documentation](API_DOCUMENTATION.md) - Developer integration

---

*Last Updated: September 2025 - NeoEssentials 2.1.0*

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
- [Commands](Commands.md) - All available commands
