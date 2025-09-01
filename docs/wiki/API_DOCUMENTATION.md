# NeoEssentials API Documentation

## Overview

The NeoEssentials API provides comprehensive access to all mod features for integration with other mods. The API has been enhanced with a comprehensive interface system, event system, and factory pattern for better developer experience.

## API Version

Current API Version: **2.1.0**
Mod Version: **1.0.2**

## Core Components

### 1. NeoEssentialsAPI (Main Entry Point)

```java
// Get the main API instance
NeoEssentialsAPI api = NeoEssentialsAPI.getInstance();

// Check if API is available
if (NeoEssentialsAPI.isAvailable()) {
    // API is ready to use
}

// Initialize event system (call during mod initialization)
api.initializeEventSystem();
```

### 2. NeoEssentialsAPIFactory (Provider Management)

```java
// Get provider instances
Optional<IEconomyProvider> economy = NeoEssentialsAPIFactory.getEconomyProvider();
Optional<IPlayerDataProvider> playerData = NeoEssentialsAPIFactory.getPlayerDataProvider();
Optional<IPlaceholderProvider> placeholders = NeoEssentialsAPIFactory.getPlaceholderProvider();

// Register custom providers
NeoEssentialsAPIFactory.registerProvider(IEconomyProvider.class, new MyEconomyProvider());

// Check API status
Map<String, Object> status = NeoEssentialsAPIFactory.getAPIStatus();
```

## Interface System

### IEconomyProvider

Comprehensive economy management interface:

```java
public interface IEconomyProvider {
    // Basic balance operations
    BigDecimal getBalance(UUID playerId);
    TransactionRecord setBalance(UUID playerId, BigDecimal amount, String reason);
    TransactionRecord deposit(UUID playerId, BigDecimal amount, String reason);
    TransactionRecord withdraw(UUID playerId, BigDecimal amount, String reason);
    TransactionRecord transfer(UUID fromPlayer, UUID toPlayer, BigDecimal amount, String reason);
    
    // Balance checking
    boolean hasBalance(UUID playerId, BigDecimal amount);
    
    // Account management
    boolean hasAccount(UUID playerId);
    boolean createAccount(UUID playerId, String playerName);
    boolean deleteAccount(UUID playerId);
    
    // History and statistics
    List<TransactionRecord> getTransactionHistory(UUID playerId, int limit);
    List<BalanceRecord> getTopBalances(int limit);
    
    // Provider information
    boolean isEnabled();
    String getProviderName();
    String getProviderVersion();
    String getCurrencyNameSingular();
    String getCurrencyNamePlural();
    String getCurrencySymbol();
    String formatCurrency(BigDecimal amount);
    boolean supportsFractionalCurrency();
    
    // Record classes
    record TransactionRecord(UUID transactionId, UUID playerId, TransactionType type, 
                           BigDecimal amount, BigDecimal oldBalance, BigDecimal newBalance, 
                           String reason, LocalDateTime timestamp, boolean successful) {}
    
    record BalanceRecord(UUID playerId, BigDecimal balance, String currency, LocalDateTime lastUpdated) {}
    
    enum TransactionType { DEPOSIT, WITHDRAWAL, TRANSFER, SET }
}
```

### IPlayerDataProvider

Comprehensive player data management interface:

```java
public interface IPlayerDataProvider {
    // Player data operations
    CompletableFuture<PlayerData> getPlayerData(UUID playerId);
    CompletableFuture<PlayerData> getPlayerData(String username);
    CompletableFuture<Boolean> updatePlayerData(UUID playerId, PlayerData data);
    CompletableFuture<Boolean> savePlayerData(PlayerData data);
    CompletableFuture<Boolean> deletePlayerData(UUID playerId);
    CompletableFuture<PlayerData> createPlayerData(ServerPlayer player);
    
    // Player queries
    CompletableFuture<List<PlayerData>> getOnlinePlayers();
    CompletableFuture<PlayerData> getPlayerByName(String username);
    CompletableFuture<Boolean> isPlayerOnline(UUID playerId);
    CompletableFuture<List<UUID>> getAllPlayerUUIDs();
    CompletableFuture<Integer> getOfflinePlayersCount();
    
    // Metadata management
    CompletableFuture<Boolean> setPlayerMetadata(UUID playerId, String key, Object value);
    CompletableFuture<Object> getPlayerMetadata(UUID playerId, String key);
    
    // Server player access
    Optional<ServerPlayer> getServerPlayer(UUID playerId);
    
    // Player data record
    record PlayerData(UUID playerId, String username, String displayName, 
                     LocalDateTime joinDate, LocalDateTime lastSeen, long playTime,
                     boolean isOnline, boolean isAfk, boolean isMuted, boolean isBanned,
                     Map<String, Object> homes, Map<String, Object> kits, 
                     Map<String, Object> metadata) {}
}
```

### IPlaceholderProvider

Comprehensive placeholder management interface:

```java
public interface IPlaceholderProvider {
    // Placeholder registration
    boolean registerPlaceholder(String identifier, PlaceholderProcessor processor);
    boolean unregisterPlaceholder(String identifier);
    
    // Placeholder processing
    String processPlaceholder(String text, PlaceholderContext context);
    
    // Placeholder information
    Set<String> getRegisteredPlaceholders();
    boolean isPlaceholderRegistered(String identifier);
    
    // Context creation
    PlaceholderContext createContext(ServerPlayer player);
    PlaceholderContext createContext(ServerPlayer player, Map<String, Object> variables);
    
    // Functional interfaces
    @FunctionalInterface
    interface PlaceholderProcessor {
        String process(PlaceholderContext context);
    }
    
    // Context record
    record PlaceholderContext(ServerPlayer player, Map<String, Object> variables) {
        public ServerPlayer getPlayer() { return player; }
        public Map<String, Object> getVariables() { return variables; }
        public Object getVariable(String key) { return variables.get(key); }
        public void setVariable(String key, Object value) { variables.put(key, value); }
    }
}
```

## Event System

### Comprehensive Event Structure

The API provides extensive events for all major operations:

#### Economy Events
```java
// Listen for balance changes
@SubscribeEvent
public void onBalanceChange(BalanceChangeEvent event) {
    UUID playerId = event.getPlayerId();
    BigDecimal oldBalance = event.getOldBalance();
    BigDecimal newBalance = event.getNewBalance();
    String reason = event.getReason();
    
    // Event is cancellable
    if (someCondition) {
        event.setCanceled(true);
    }
}

// Listen for transactions
@SubscribeEvent
public void onTransaction(TransactionEvent event) {
    IEconomyProvider.TransactionRecord transaction = event.getTransaction();
    // Handle transaction
}
```

#### Player Events
```java
// Listen for AFK status changes
@SubscribeEvent
public void onAfkStatusChange(AfkStatusChangeEvent event) {
    ServerPlayer player = event.getPlayer();
    boolean isAfk = event.isAfk();
    // Handle AFK change
}

// Listen for nickname changes
@SubscribeEvent
public void onNicknameChange(NicknameChangeEvent event) {
    ServerPlayer player = event.getPlayer();
    String oldNickname = event.getOldNickname();
    String newNickname = event.getNewNickname();
    // Handle nickname change
}
```

#### Home and Warp Events
```java
// Listen for home operations
@SubscribeEvent
public void onHomeSet(HomeSetEvent event) {
    ServerPlayer player = event.getPlayer();
    String homeName = event.getHomeName();
    // Handle home set
}

// Listen for warp operations
@SubscribeEvent
public void onWarpCreate(WarpCreateEvent event) {
    String warpName = event.getWarpName();
    // Handle warp creation
}
```

### Event Registration

Register your event handlers with NeoForge:

```java
@Mod.EventBusSubscriber(modid = "yourmod")
public class YourEventHandler {
    
    @SubscribeEvent
    public static void onBalanceChange(BalanceChangeEvent event) {
        // Handle event
    }
}
```

## Usage Examples

### Economy Integration

```java
// Get economy provider
Optional<IEconomyProvider> economyOpt = NeoEssentialsAPIFactory.getEconomyProvider();
if (economyOpt.isPresent()) {
    IEconomyProvider economy = economyOpt.get();
    
    // Check player balance
    BigDecimal balance = economy.getBalance(playerId);
    
    // Make a transaction
    if (economy.hasBalance(playerId, new BigDecimal("100.00"))) {
        IEconomyProvider.TransactionRecord transaction = 
            economy.withdraw(playerId, new BigDecimal("100.00"), "Item purchase");
        
        if (transaction != null && transaction.successful()) {
            // Transaction successful
        }
    }
}
```

### Player Data Integration

```java
// Get player data provider
Optional<IPlayerDataProvider> playerDataOpt = NeoEssentialsAPIFactory.getPlayerDataProvider();
if (playerDataOpt.isPresent()) {
    IPlayerDataProvider playerData = playerDataOpt.get();
    
    // Get player information
    playerData.getPlayerData(playerId).thenAccept(data -> {
        if (data != null) {
            String username = data.username();
            boolean isOnline = data.isOnline();
            long playTime = data.playTime();
            // Use player data
        }
    });
    
    // Set custom metadata
    playerData.setPlayerMetadata(playerId, "custom_score", 1000);
}
```

### Placeholder Integration

```java
// Get placeholder provider
Optional<IPlaceholderProvider> placeholderOpt = NeoEssentialsAPIFactory.getPlaceholderProvider();
if (placeholderOpt.isPresent()) {
    IPlaceholderProvider placeholders = placeholderOpt.get();
    
    // Register custom placeholder
    placeholders.registerPlaceholder("mymod_custom", context -> {
        ServerPlayer player = context.getPlayer();
        return "Custom value for " + player.getName().getString();
    });
    
    // Process placeholders in text
    IPlaceholderProvider.PlaceholderContext context = 
        placeholders.createContext(player);
    String processed = placeholders.processPlaceholder(
        "Hello %mymod_custom%!", context);
}
```

## Error Handling

All API methods include proper error handling:

```java
try {
    // API operations
    NeoEssentialsAPI api = NeoEssentialsAPI.getInstance();
    
    // Check if components are available
    if (!NeoEssentialsAPIFactory.isNeoEssentialsReady()) {
        // NeoEssentials not fully loaded yet
        return;
    }
    
    // Perform operations with null checks
    Optional<IEconomyProvider> economy = NeoEssentialsAPIFactory.getEconomyProvider();
    economy.ifPresent(provider -> {
        // Use provider safely
    });
    
} catch (Exception e) {
    // Handle errors gracefully
    logger.error("Failed to use NeoEssentials API", e);
}
```

## Thread Safety

- All API interfaces are designed to be thread-safe
- CompletableFuture is used for asynchronous operations
- Concurrent data structures are used where appropriate
- Always check if components are available before use

## Version Compatibility

```java
// Check API version compatibility
if (NeoEssentialsAPI.isAPIVersionAtLeast("2.1.0")) {
    // Use new API features
} else {
    // Fallback to older API methods
}
```

## Best Practices

1. **Always check availability**: Use `isAvailable()` and provider optionals
2. **Handle async operations**: Use CompletableFuture properly
3. **Register events early**: Initialize event system during mod setup
4. **Use providers**: Access features through the provider interfaces
5. **Handle errors gracefully**: Include proper error handling and logging
6. **Check versions**: Ensure API compatibility before using features
7. **Clean up resources**: Unregister placeholders and providers when shutting down

## Migration from v2.0.0

The v2.1.0 API is fully backward compatible with v2.0.0. New features:

- **Provider interfaces**: Clean, standardized access to features
- **Enhanced event system**: Comprehensive events for all operations
- **API factory**: Centralized provider management
- **Better documentation**: Comprehensive API documentation
- **Improved error handling**: More robust error handling and logging

Existing code will continue to work, but using the new interfaces is recommended for better maintainability and future compatibility.
