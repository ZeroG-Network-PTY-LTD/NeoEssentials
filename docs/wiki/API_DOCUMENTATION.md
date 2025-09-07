# NeoEssentials API Documentation

## Overview

The NeoEssentials API provides comprehensive access to mod features for integration with other mods. The API uses a streamlined provider interface system for better developer experience and maintainability.

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

// Check API version compatibility
if (NeoEssentialsAPI.isAPIVersionAtLeast("2.1.0")) {
    // Use current API features
}
```

### 2. NeoEssentialsAPIFactory (Provider Management)

```java
// Get provider instances (may be empty if not implemented)
Optional<IEconomyProvider> economy = NeoEssentialsAPIFactory.getEconomyProvider();
Optional<IPlayerDataProvider> playerData = NeoEssentialsAPIFactory.getPlayerDataProvider();
Optional<IPlaceholderProvider> placeholders = NeoEssentialsAPIFactory.getPlaceholderProvider();

// Check if NeoEssentials is fully loaded
if (NeoEssentialsAPIFactory.isNeoEssentialsReady()) {
    // Safe to use API
}

// Get API status information
Map<String, Object> status = NeoEssentialsAPIFactory.getAPIStatus();
```

## Interface System

### IEconomyProvider

Economy management interface (simplified):

```java
public interface IEconomyProvider {
    // Provider information
    String getProviderName();
    String getProviderVersion();
    boolean isEnabled();
    
    // Currency information
    String getCurrencyNameSingular();
    String getCurrencyNamePlural();
    String getCurrencySymbol();
    boolean supportsFractionalCurrency();
    String formatCurrency(BigDecimal amount);
    
    // Basic balance operations
    BigDecimal getBalance(UUID playerUuid);
    boolean hasBalance(UUID playerUuid, BigDecimal amount);
    boolean withdraw(UUID playerUuid, BigDecimal amount, String reason);
    boolean deposit(UUID playerUuid, BigDecimal amount, String reason);
    boolean transfer(UUID fromUuid, UUID toUuid, BigDecimal amount, String reason);
    boolean setBalance(UUID playerUuid, BigDecimal amount, String reason);
    
    // Account management
    boolean hasAccount(UUID playerUuid);
    boolean createAccount(UUID playerUuid, String playerName);
    boolean deleteAccount(UUID playerUuid);
    
    // History and statistics
    List<TransactionRecord> getTransactionHistory(UUID playerUuid, int limit);
    List<BalanceRecord> getTopBalances(int limit);
    
    // Record classes
    record TransactionRecord(long timestamp, String type, BigDecimal amount, 
                           BigDecimal balanceBefore, BigDecimal balanceAfter, 
                           String reason, UUID relatedPlayer) {}
    
    record BalanceRecord(UUID playerUuid, String playerName, 
                        BigDecimal balance, int rank) {}
}
```

### IPlayerDataProvider

Player data management interface:

```java
public interface IPlayerDataProvider {
    // Basic player data access
    Optional<PlayerData> getPlayerData(UUID playerUuid);
    Optional<PlayerData> getPlayerData(String playerName);
    boolean savePlayerData(PlayerData playerData);
    boolean hasPlayerData(UUID playerUuid);
    PlayerData createPlayerData(ServerPlayer player);
    boolean deletePlayerData(UUID playerUuid);
    
    // Player queries
    List<UUID> getAllPlayerUUIDs();
    int getOnlinePlayersCount();
    int getOfflinePlayersCount();
    
    // PlayerData interface
    interface PlayerData {
        UUID getUUID();
        String getName();
        String getDisplayName();
        void setDisplayName(String displayName);
        
        // Time tracking
        long getFirstLogin();
        long getLastLogin();
        void setLastLogin(long timestamp);
        long getLastLogout();
        void setLastLogout(long timestamp);
        long getTotalPlaytime();
        void addPlaytime(long playtime);
        
        // Status
        boolean isOnline();
        void setOnline(boolean online);
        boolean isAFK();
        void setAFK(boolean afk);
        long getAFKTime();
        void setAFKTime(long timestamp);
        
        // Location and world
        String getCurrentWorld();
        void setCurrentWorld(String world);
        String getIpAddress();
        void setIpAddress(String ipAddress);
        
        // Moderation
        boolean isMuted();
        void setMuted(boolean muted);
        long getMuteExpiration();
        void setMuteExpiration(long expiration);
        String getMuteReason();
        void setMuteReason(String reason);
        
        // Custom data
        Object getCustomData(String key);
        void setCustomData(String key, Object value);
        Object removeCustomData(String key);
        boolean hasCustomData(String key);
        List<String> getCustomDataKeys();
    }
}
```

### IPlaceholderProvider

Placeholder management interface:

```java
public interface IPlaceholderProvider {
    // Provider information
    String getProviderName();
    String getProviderVersion();
    
    // Placeholder registration
    boolean registerPlaceholder(String identifier, Function<PlaceholderContext, String> resolver);
    boolean unregisterPlaceholder(String identifier);
    boolean isPlaceholderRegistered(String identifier);
    String[] getRegisteredPlaceholders();
    
    // Placeholder processing
    String processPlaceholders(String text, PlaceholderContext context);
    String processPlaceholders(String text, ServerPlayer player);
    
    // Special placeholder types
    boolean registerAnimatedPlaceholder(String identifier, String[] frames, double intervalSeconds);
    boolean registerConditionalPlaceholder(String identifier, String condition, String trueValue, String falseValue);
    
    // PlaceholderContext interface
    interface PlaceholderContext {
        ServerPlayer getPlayer();
        Object getContextData(String key);
        void setContextData(String key, Object value);
        boolean hasContextData(String key);
        ServerPlayer getViewer();
        void setViewer(ServerPlayer viewer);
        long getCurrentTime();
        PlaceholderContext withPlayer(ServerPlayer player);
        PlaceholderContext withData(String key, Object value);
    }
}
```

## Event System

### Current Event Structure

The API provides basic events for core operations through the NeoEssentialsEventHandler:

#### Economy Events
```java
// Listen for balance changes
@SubscribeEvent
public void onBalanceChange(NeoEssentialsEventHandler.EconomyBalanceChangeEvent event) {
    ServerPlayer player = event.getPlayer();
    BigDecimal oldBalance = event.getOldBalance();
    BigDecimal newBalance = event.getNewBalance();
    String reason = event.getReason();
    NeoEssentialsEventHandler.EconomyBalanceChangeEvent.TransactionType type = event.getType();
    // Handle balance change (not cancellable)
}

// Listen for transactions (before they occur)
@SubscribeEvent
public void onTransaction(NeoEssentialsEventHandler.EconomyTransactionEvent event) {
    ServerPlayer player = event.getPlayer();
    BigDecimal amount = event.getAmount();
    String reason = event.getReason();
    
    // Event is cancellable
    if (someCondition) {
        event.setCanceled(true);
    }
}
```

#### Home and Location Events
```java
// Listen for home operations
@SubscribeEvent
public void onHomeSet(NeoEssentialsEventHandler.HomeSetEvent event) {
    ServerPlayer player = event.getPlayer();
    String homeName = event.getHomeName();
    double x = event.getX();
    double y = event.getY();
    double z = event.getZ();
    String world = event.getWorld();
    
    // Event is cancellable
    if (!allowHomeHere) {
        event.setCanceled(true);
    }
}
```

### Event Registration

Register your event handlers with NeoForge:

```java
@Mod.EventBusSubscriber(modid = "yourmod")
public class YourEventHandler {
    
    @SubscribeEvent
    public static void onBalanceChange(NeoEssentialsEventHandler.EconomyBalanceChangeEvent event) {
        // Handle event
    }
    
    @SubscribeEvent
    public static void onTransaction(NeoEssentialsEventHandler.EconomyTransactionEvent event) {
        // Handle transaction - can be cancelled
    }
}
```

## Usage Examples

### Economy Integration

```java
// Get economy provider (may not be available)
Optional<IEconomyProvider> economyOpt = NeoEssentialsAPIFactory.getEconomyProvider();
if (economyOpt.isPresent()) {
    IEconomyProvider economy = economyOpt.get();
    
    // Check if provider is ready
    if (economy.isEnabled()) {
        // Check player balance
        BigDecimal balance = economy.getBalance(playerId);
        
        // Make a transaction
        if (economy.hasBalance(playerId, new BigDecimal("100.00"))) {
            boolean success = economy.withdraw(playerId, new BigDecimal("100.00"), "Item purchase");
            
            if (success) {
                // Transaction successful
            }
        }
        
        // Format currency for display
        String formatted = economy.formatCurrency(balance);
    }
} else {
    // Economy provider not available - use alternative approach
    // Access economy directly via API
    NeoEssentialsAPI api = NeoEssentialsAPI.getInstance();
    if (api.isFeatureAvailable("economy")) {
        BigDecimal balance = api.getPlayerBalance(playerId);
        // Use direct API methods
    }
}
```

### Player Data Integration

```java
// Get player data provider (may not be available)
Optional<IPlayerDataProvider> playerDataOpt = NeoEssentialsAPIFactory.getPlayerDataProvider();
if (playerDataOpt.isPresent()) {
    IPlayerDataProvider playerData = playerDataOpt.get();
    
    // Get player information
    Optional<IPlayerDataProvider.PlayerData> dataOpt = playerData.getPlayerData(playerId);
    if (dataOpt.isPresent()) {
        IPlayerDataProvider.PlayerData data = dataOpt.get();
        String username = data.getName();
        boolean isOnline = data.isOnline();
        long playTime = data.getTotalPlaytime();
        
        // Set custom metadata
        data.setCustomData("custom_score", 1000);
        playerData.savePlayerData(data);
    }
} else {
    // Player data provider not available - use direct API access
    NeoEssentialsAPI api = NeoEssentialsAPI.getInstance();
    // Use direct manager access for advanced features
    // Note: This bypasses the provider interface
}
```

### Placeholder Integration

```java
// Get placeholder provider (may not be available)
Optional<IPlaceholderProvider> placeholderOpt = NeoEssentialsAPIFactory.getPlaceholderProvider();
if (placeholderOpt.isPresent()) {
    IPlaceholderProvider placeholders = placeholderOpt.get();
    
    // Register custom placeholder
    placeholders.registerPlaceholder("mymod_custom", context -> {
        ServerPlayer player = context.getPlayer();
        return "Custom value for " + player.getName().getString();
    });
    
    // Process placeholders in text
    String processed = placeholders.processPlaceholders(
        "Hello %mymod_custom%!", player);
    
    // Register animated placeholder
    String[] frames = {"&cAnimated", "&eAnimated", "&aAnimated"};
    placeholders.registerAnimatedPlaceholder("mymod_animated", frames, 1.0);
    
} else {
    // Placeholder provider not available - use direct API access
    NeoEssentialsAPI api = NeoEssentialsAPI.getInstance();
    
    // Register via direct API
    api.registerPlaceholder("mymod_custom", context -> {
        ServerPlayer player = context.getPlayer();
        return "Custom value for " + player.getName().getString();
    });
    
    // Process via direct API
    String processed = api.processPlaceholders(player, "Hello %mymod_custom%!");
}
```

## Error Handling

All API methods include proper error handling and null safety:

```java
try {
    // API operations
    NeoEssentialsAPI api = NeoEssentialsAPI.getInstance();
    
    // Always check if NeoEssentials is available
    if (!NeoEssentialsAPI.isAvailable()) {
        // NeoEssentials not loaded
        return;
    }
    
    // Check if NeoEssentials is fully ready
    if (!NeoEssentialsAPIFactory.isNeoEssentialsReady()) {
        // NeoEssentials not fully initialized
        return;
    }
    
    // Use providers with null checks
    Optional<IEconomyProvider> economy = NeoEssentialsAPIFactory.getEconomyProvider();
    if (economy.isPresent() && economy.get().isEnabled()) {
        // Use provider safely
        BigDecimal balance = economy.get().getBalance(playerId);
    } else {
        // Provider not available - use direct API or fallback
        if (api.isFeatureAvailable("economy")) {
            BigDecimal balance = api.getPlayerBalance(playerId);
        }
    }
    
} catch (Exception e) {
    // Handle errors gracefully
    logger.error("Failed to use NeoEssentials API", e);
}
```

## Thread Safety and Async Operations

- The main API is designed to be thread-safe for basic operations
- Most operations are synchronous, not asynchronous (no CompletableFuture)
- Provider interfaces follow simple synchronous patterns
- Always check availability before use in async contexts

## Version Compatibility

```java
// Check API version compatibility
if (NeoEssentialsAPI.isAPIVersionAtLeast("2.1.0")) {
    // Use current API features
} else {
    // Use fallback methods or warn about compatibility
}

// Get version information
String apiVersion = NeoEssentialsAPI.getAPIVersion();  // "2.1.0"
String modVersion = NeoEssentialsAPI.getModVersion();  // "1.0.2"
```

## Direct API Access

For advanced integrations, you can access managers directly:

```java
NeoEssentialsAPI api = NeoEssentialsAPI.getInstance();

// Direct manager access (bypasses provider interfaces)
HomeManager homeManager = api.getHomeManager();
EconomyManager economyManager = api.getEconomyManager(); 
WarpManager warpManager = api.getWarpManager();
PlaceholderManager placeholderManager = api.getPlaceholderManager();

// Use manager methods directly
List<String> homes = homeManager.getPlayerHomes(playerId);
boolean hasHome = homeManager.hasHome(playerId, "home");
```

## Best Practices

1. **Always check availability**: Use `isAvailable()` and `isNeoEssentialsReady()` before API calls
2. **Handle provider optionals**: Provider interfaces may not be implemented - use `Optional.isPresent()`
3. **Use direct API as fallback**: If providers aren't available, use direct manager access
4. **Initialize events early**: Call `initializeEventSystem()` during mod setup
5. **Handle errors gracefully**: Include proper error handling and logging
6. **Check versions**: Ensure API compatibility before using features
7. **Prefer synchronous operations**: Most API operations are synchronous, not async

## Current Implementation Status

### ✅ Implemented
- Main NeoEssentialsAPI class with version checking
- Basic event system for economy and home operations
- API factory with provider management framework
- Interface definitions for economy, player data, and placeholders
- Direct manager access for advanced features

### ⚠️ Partially Implemented
- Provider interfaces exist but may not have implementations
- Event system covers basic operations but not all features
- Some advanced features require direct manager access

### ❌ Not Implemented
- Full provider implementations (providers may be empty)
- Comprehensive async operations
- All events mentioned in older documentation

## Migration from v2.0.0

The v2.1.0 API maintains backward compatibility with v2.0.0. Key differences:

- **Provider pattern**: New provider interfaces (may be empty - use as fallback)
- **Simplified events**: Focused on core operations rather than comprehensive coverage  
- **Direct access maintained**: All features still accessible via direct API methods
- **Version methods**: Enhanced version checking and compatibility methods

Existing code using direct API methods will continue to work. The provider interfaces offer a cleaner integration path when implemented.
