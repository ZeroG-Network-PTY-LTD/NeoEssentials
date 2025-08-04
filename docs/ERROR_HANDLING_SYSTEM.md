# Enhanced Error Handling System
## NeoEssentials Comprehensive Error Management

### Overview
The Enhanced Error Handling system provides a robust, user-friendly approach to error management throughout NeoEssentials. It includes structured error handling, input validation, custom exceptions, and seamless integration utilities.

### System Components

#### 1. ErrorHandler (`com.zerog.neoessentials.error.ErrorHandler`)
**Core error handling engine with multiple severity levels and categories.**

**Error Levels:**
- `INFO` - Informational messages (blue 💙)
- `WARNING` - Warnings that don't break functionality (yellow ⚠️)
- `ERROR` - Errors that affect functionality (red ❌)
- `CRITICAL` - Critical system errors requiring immediate attention (red 🔴)

**Error Categories:**
- `COMMAND` - Command execution errors
- `ECONOMY` - Economy system issues
- `TELEPORTATION` - Teleportation failures
- `PERMISSION` - Permission-related problems
- `CONFIGURATION` - Config file issues
- `DATABASE` - Data storage problems
- `NETWORK` - Network/API communication errors
- `VALIDATION` - Input validation failures
- `SYSTEM` - General system errors

**Key Features:**
- User-friendly error messages with helpful suggestions
- Admin notification system for serious errors
- Comprehensive logging with stack traces
- Recovery mechanisms and fallback handling
- Color-coded messages with emojis for better UX

#### 2. CommandValidator (`com.zerog.neoessentials.validation.CommandValidator`)
**Advanced input validation system for all command parameters.**

**Validation Types:**
- **Player Names:** Format validation with Minecraft username rules
- **Monetary Amounts:** Number format with min/max bounds checking
- **Time Durations:** Parse time strings (30s, 5m, 2h, 1d) with bounds
- **Coordinates:** World coordinate validation with boundary checks
- **Item Names:** Minecraft item ID/name validation
- **Speed Values:** Movement speed validation with safe limits
- **Permissions:** Permission node format validation

**Features:**
- Pattern-based validation with regex
- User-friendly error messages with specific suggestions
- Quick validation helpers for common checks
- Integration with ErrorHandler for consistent messaging

#### 3. NeoEssentialsExceptions (`com.zerog.neoessentials.exception.NeoEssentialsExceptions`)
**Structured exception hierarchy for type-safe error handling.**

**Exception Types:**
- `CommandException` - Command execution failures
- `PermissionException` - Access control violations
- `EconomyException` - Economy system errors
- `TeleportException` - Teleportation problems
- `HomeException` - Home management issues
- `ConfigException` - Configuration errors
- `DataException` - Database/storage failures
- `NetworkException` - External service issues

**Factory Methods:**
- `invalidCommand(command, reason)` - Command validation failures
- `noPermission(permission, action)` - Permission denied scenarios
- `insufficientFunds(required, available)` - Economy insufficient funds
- `unsafeLocation(reason)` - Teleportation safety blocks
- `homeNotFound(homeName)` - Missing home references
- `configLoadError(configName, cause)` - Config loading failures
- `saveError(dataType, cause)` - Data persistence errors
- `connectionTimeout(service)` - Network timeout scenarios

#### 4. ErrorHandlingIntegration (`com.zerog.neoessentials.integration.ErrorHandlingIntegration`)
**Integration utilities for applying error handling to existing commands.**

**Integration Features:**
- Wrap existing command logic with comprehensive error handling
- Permission validation with error handling
- Safe parameter parsing (players, amounts, coordinates)
- Economy operation wrappers with specific error handling
- Teleportation safety checks with user-friendly feedback
- Configuration value retrieval with fallback handling

### Usage Examples

#### Basic Error Handling
```java
// Simple command with error handling
public static int balanceCommand(CommandSourceStack source, String playerName) {
    return ErrorHandlingIntegration.executeCommand(source, "balance", (src) -> {
        ServerPlayer target = ErrorHandlingIntegration.getPlayerSafely(src, playerName);
        double balance = EconomyManager.getBalance(target.getUUID());
        src.sendSuccess(() -> Component.literal(
            String.format("§e%s's balance: §a$%.2f", target.getName().getString(), balance)), false);
        return 1;
    });
}
```

#### Permission-Based Command
```java
// Command with permission check and error handling
public static int payCommand(CommandSourceStack source, String targetName, String amountStr) {
    return ErrorHandlingIntegration.executeWithPermission(source, "pay", "neoessentials.economy.pay", (src) -> {
        ServerPlayer target = ErrorHandlingIntegration.getPlayerSafely(src, targetName);
        double amount = ErrorHandlingIntegration.parseAmountSafely(src, amountStr);
        
        ErrorHandlingIntegration.performEconomyOperation(src, "pay", () -> {
            EconomyManager.transfer(src.getPlayerOrException().getUUID(), target.getUUID(), amount);
            src.sendSuccess(() -> Component.literal(
                String.format("§aPaid §e$%.2f §ato §e%s", amount, target.getName().getString())), false);
        });
        
        return 1;
    });
}
```

#### Custom Exception Handling
```java
// Throwing custom exceptions
try {
    if (balance < amount) {
        throw NeoEssentialsExceptions.Factory.insufficientFunds(amount, balance);
    }
    // Perform operation
} catch (NeoEssentialsExceptions.EconomyException e) {
    NeoEssentialsExceptions.Handler.handle(source, e);
}
```

#### Input Validation
```java
// Validate input before processing
CommandValidator.ValidationResult result = CommandValidator.validateAmount(source, "100.50", 1.0, 10000.0);
if (!result.isValid()) {
    source.sendFailure(Component.literal(result.getErrorMessage() + " " + result.getSuggestion()));
    return 0;
}
```

### Error Message Examples

#### User Messages
- **Invalid Amount:** `❌ Invalid amount format - Amount must be a positive number (e.g., 100, 50.25)`
- **Permission Denied:** `✋ You don't have permission to do that - Ask an admin for the required permissions`
- **Player Not Found:** `❌ Player 'InvalidName' is not online - Check the player name and make sure they're online`
- **Insufficient Funds:** `💰 You need $500.00 but only have $250.00 - Earn more money or reduce the amount`

#### Admin Notifications
- **Critical Database Error:** `🔴 Critical database error: Operation: save_player_data Error: Connection timeout`
- **Configuration Error:** `⚙️ Config issue detected: File: economy.toml Issue: Invalid currency format`
- **System Error:** `🔧 System error in teleportation: Player attempted unsafe teleport to void`

### Integration Guide

#### Migrating Existing Commands
1. **Wrap command logic** with `ErrorHandlingIntegration.executeCommand()`
2. **Replace parameter parsing** with safe parsing methods
3. **Add permission checks** using `executeWithPermission()`
4. **Handle specific operations** with dedicated wrappers (economy, teleportation, etc.)

#### Adding Custom Error Types
1. **Extend NeoEssentialsException** for new error categories
2. **Add factory methods** for common scenarios
3. **Register with ErrorHandler** for proper categorization
4. **Update documentation** with usage examples

### Configuration Options

Error handling behavior can be customized through the main configuration:
- **Log Level:** Control which errors are logged
- **Admin Notifications:** Enable/disable admin alerts
- **User Message Detail:** Control verbosity of user-facing errors
- **Error Recovery:** Enable automatic retry mechanisms
- **Debug Mode:** Enhanced error reporting for development

### Best Practices

1. **Always validate input** before processing commands
2. **Use specific exception types** rather than generic exceptions
3. **Provide helpful suggestions** in error messages
4. **Log errors appropriately** based on severity
5. **Notify admins** of system-critical issues
6. **Handle errors gracefully** without crashing systems
7. **Test error scenarios** to ensure proper handling
8. **Keep user messages friendly** and actionable

### Performance Considerations

- Error handling adds minimal overhead to command execution
- Validation is performed before expensive operations
- Logging is asynchronous to prevent blocking
- Admin notifications are throttled to prevent spam
- Error recovery mechanisms include backoff strategies

### Future Enhancements

- **Metrics Collection:** Track error frequencies and patterns
- **Auto-Recovery:** Enhanced automatic error recovery
- **Error Analytics:** Dashboard for error analysis
- **Custom Error Actions:** User-defined error handling scripts
- **Multi-Language Support:** Localized error messages

---

**Author:** ZeroG  
**Version:** 2.0.0  
**Last Updated:** January 2025
