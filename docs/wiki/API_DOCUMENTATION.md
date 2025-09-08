# NeoEssentials API Documentation

## Overview

The NeoEssentials API provides comprehensive access to mod features for integration with other mods. The API uses a streamlined provider interface system for better developer experience and maintainability.

## API Version

Current API Version: **2.1.0**
Mod Version: **2.1.0**

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

The API provides interface definitions for future provider implementations, but currently uses direct manager access for most functionality.

### Provider Framework (Currently Limited Implementation)

The provider interfaces exist as a framework for future development, but most providers return empty `Optional` values. Use direct manager access for current functionality.

### IEconomyProvider (Interface Only)

Economy provider interface exists but is not currently implemented. Use direct EconomyManager access instead:

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

### IPlayerDataProvider (Interface Only)

Player data provider interface exists but is not currently implemented. Use direct manager access instead:

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

### IPlaceholderProvider (Interface Only)

Placeholder provider interface exists but is not currently implemented. Use direct PlaceholderManager access instead:

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

### Current Event Implementation

The API provides a basic event system through NeoEssentialsEventHandler. Note that many events described in older documentation are not yet implemented.

### Available Events (Limited)

Currently implemented events include basic economy and home operations. The event system is more limited than described in some documentation.

```java
// Basic event handler registration
@Mod.EventBusSubscriber(modid = "yourmod")
public class YourEventHandler {
    
    @SubscribeEvent
    public static void onSomeEvent(SomeNeoEssentialsEvent event) {
        // Handle event
    }
}
```

## Usage Examples

### Direct Manager Access (Recommended)

Since provider implementations are currently limited, use direct manager access for most functionality:

```java
// Get the main API instance
NeoEssentialsAPI api = NeoEssentialsAPI.getInstance();

// Check availability
if (!NeoEssentialsAPI.isAvailable()) {
    // NeoEssentials not loaded
    return;
}

// Access managers directly
EconomyManager economyManager = api.getEconomyManager();
HomeManager homeManager = api.getHomeManager();
PlaceholderManager placeholderManager = api.getPlaceholderManager();
WarpManager warpManager = api.getWarpManager();
KitManager kitManager = api.getKitManager();
MessagingManager messagingManager = api.getMessagingManager();
SpawnManager spawnManager = api.getSpawnManager();
ModerationManager moderationManager = api.getModerationManager();
PerformanceMonitor performanceMonitor = api.getPerformanceMonitor();

// Use managers directly
if (economyManager.hasAccount(playerId)) {
    BigDecimal balance = economyManager.getBalance(playerId);
    boolean success = economyManager.withdraw(playerId, new BigDecimal("100.00"), "Purchase");
}
```

### Economy Integration (Direct Manager Access)

```java
// Get economy manager directly
NeoEssentialsAPI api = NeoEssentialsAPI.getInstance();
EconomyManager economyManager = api.getEconomyManager();

if (economyManager != null) {
    // Check if player has account
    if (economyManager.hasAccount(playerId)) {
        // Get balance
        BigDecimal balance = economyManager.getBalance(playerId);
        
        // Make transaction
        if (economyManager.hasBalance(playerId, new BigDecimal("100.00"))) {
            boolean success = economyManager.withdraw(playerId, new BigDecimal("100.00"), "Item purchase");
            if (success) {
                // Transaction successful
            }
        }
    }
}

// Provider approach (returns empty Optional currently)
Optional<IEconomyProvider> economyOpt = NeoEssentialsAPIFactory.getEconomyProvider();
if (economyOpt.isPresent()) {
    // Provider implementation would go here
    // Currently returns empty - use direct manager access instead
}
```

### Player Data Integration (Direct Manager Access)

```java
// Access player data through various managers
NeoEssentialsAPI api = NeoEssentialsAPI.getInstance();

// Get player information through different managers
HomeManager homeManager = api.getHomeManager();
if (homeManager != null) {
    List<String> homes = homeManager.getPlayerHomes(playerId);
    boolean hasHome = homeManager.hasHome(playerId, "home");
}

ModerationManager moderationManager = api.getModerationManager();
if (moderationManager != null) {
    boolean isMuted = moderationManager.isMuted(playerId);
    // Handle moderation data
}

// Provider approach (returns empty Optional currently)
Optional<IPlayerDataProvider> playerDataOpt = NeoEssentialsAPIFactory.getPlayerDataProvider();
if (playerDataOpt.isPresent()) {
    // Provider implementation would go here
    // Currently returns empty - use direct manager access instead
}
```

### Placeholder Integration (Direct Manager Access)

```java
// Get placeholder manager directly (recommended)
NeoEssentialsAPI api = NeoEssentialsAPI.getInstance();
PlaceholderManager placeholderManager = api.getPlaceholderManager();

if (placeholderManager != null) {
    // Register custom placeholder
    placeholderManager.registerPlaceholder("mymod_custom", context -> {
        ServerPlayer player = context.getPlayer();
        return "Custom value for " + (player != null ? player.getName().getString() : "Server");
    });
    
    // Process placeholders in text
    String processed = placeholderManager.processPlaceholders(
        "Hello %mymod_custom%!", player);
    
    // Register animated placeholder
    List<String> frames = Arrays.asList("&cAnimated", "&eAnimated", "&aAnimated");
    placeholderManager.registerAnimatedPlaceholder("mymod_animated", frames, 1.0);
}

// Provider approach (returns empty Optional currently)
Optional<IPlaceholderProvider> placeholderOpt = NeoEssentialsAPIFactory.getPlaceholderProvider();
if (placeholderOpt.isPresent()) {
    // Provider implementation would go here
    // Currently returns empty - use direct manager access instead
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
        logger.warn("NeoEssentials not loaded");
        return;
    }
    
    // Check if NeoEssentials is fully ready
    if (!NeoEssentialsAPIFactory.isNeoEssentialsReady()) {
        logger.warn("NeoEssentials not fully initialized");
        return;
    }
    
    // Use direct manager access (recommended)
    EconomyManager economyManager = api.getEconomyManager();
    if (economyManager != null && economyManager.hasAccount(playerId)) {
        BigDecimal balance = economyManager.getBalance(playerId);
        // Use manager safely
    }
    
    // Provider approach (currently returns empty)
    Optional<IEconomyProvider> economy = NeoEssentialsAPIFactory.getEconomyProvider();
    if (economy.isPresent()) {
        // Provider implementation (currently empty)
    } else {
        // Use direct manager access as fallback (recommended approach)
        if (economyManager != null) {
            // Use direct manager methods
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
String modVersion = NeoEssentialsAPI.getModVersion();  // "2.1.0"
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

1. **Use direct manager access**: Provider implementations are currently limited - use `api.getManagerName()` for functionality
2. **Always check availability**: Use `isAvailable()` and `isNeoEssentialsReady()` before API calls
3. **Handle null managers**: Manager instances may be null - include null checks
4. **Initialize events early**: Call `initializeEventSystem()` during mod setup
5. **Handle errors gracefully**: Include proper error handling and logging
6. **Check versions**: Ensure API compatibility before using features
7. **Prefer synchronous operations**: Most API operations are synchronous, not async
8. **Provider interfaces are framework only**: Currently return empty Optionals - use as reference for future development

## Current Implementation Status

### ✅ Implemented
- Main NeoEssentialsAPI class with version checking and direct manager access
- NeoEssentialsAPIFactory framework for provider management
- Interface definitions for economy, player data, and placeholders (framework only)
- Basic event system for some core operations
- Direct manager access for all mod features
- PlaceholderManager with comprehensive placeholder system and animations
- All manager classes accessible through API

### ⚠️ Partially Implemented
- Event system covers some basic operations but not all features mentioned in older docs
- Provider interfaces exist as framework but return empty Optionals
- Some API convenience methods may redirect to direct manager access

### ❌ Not Implemented
- Full provider implementations (interfaces exist but are not populated)
- Comprehensive async operations
- All events described in some older documentation
- Some convenience methods described in older API docs

### 💡 Recommended Approach
- **Use direct manager access** as the primary integration method
- **Provider interfaces** serve as reference for future development
- **Event system** should be used with awareness of current limitations

## Migration from v2.0.0

The v2.1.0 API maintains backward compatibility with v2.0.0. Key differences:

- **Direct manager access**: Primary integration method - all managers accessible via `api.getManagerName()`
- **Provider pattern**: New provider interfaces exist as framework (currently return empty Optionals)
- **Simplified events**: Focused on core operations with limited implementation currently
- **Version consistency**: Both API and mod version are now 2.1.0
- **Enhanced placeholder system**: Comprehensive PlaceholderManager with animation support

**Migration Strategy:**
1. **Existing direct API code**: Continues to work without changes
2. **New integrations**: Use direct manager access for immediate functionality
3. **Provider interfaces**: Available as reference for future development
4. **Events**: Use with awareness of current limitations
