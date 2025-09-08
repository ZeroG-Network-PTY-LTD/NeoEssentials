# Events System

NeoEssentials provides a custom event system built on NeoForge's event bus, offering integration points for other mods and server functionality. The system includes custom event classes for economy transactions, teleportation, moderation, and player activities.

## 🎯 Event Architecture

### Core Event Classes
NeoEssentials implements a streamlined event hierarchy:

#### Base Event Classes
- **`NeoEssentialsEvent`** - Base class for all custom events, provides player context and UUID access
- **`CancellableNeoEssentialsEvent`** - Base class for cancellable events implementing ICancellableEvent interface
- **Event Categories**: Economy, Home, Warp, Kit, Messaging, Teleport, Moderation, Player, and Placeholder events

#### Event Handlers
- **`NeoEssentialsEventHandler`** - Main event handler containing all custom event definitions (events.NeoEssentialsEventHandler)
- **`NeoEssentialsEventHandler`** (main package) - Core Minecraft event handling for block interactions, shop protection, and jail restrictions
- **`UIEventHandler`** - UI events and player join handling for tablist integration
- **`ShopEventHandler`** - Shop creation and transaction event handling

### Event Integration
Events integrate with:
- **NeoForge Event Bus** - Native Minecraft/NeoForge event system using @EventBusSubscriber annotation
- **Custom Event Broadcasting** - Internal event system for mod integration points
- **Shop System** - Block interaction events for shop sign and chest protection
- **Moderation System** - Events for jail restrictions and player moderation
- **Teleportation System** - Events for home, warp, and teleport requests

---

## 💰 Economy Events

### Balance Events
| Event Class | Description | Cancellable |
|-------------|-------------|-------------|
| `EconomyBalanceChangeEvent` | Fired when a player's balance changes | ❌ |
| `EconomyTransactionEvent` | Fired before a transaction occurs | ✅ |

#### EconomyBalanceChangeEvent
```java
public class EconomyBalanceChangeEvent extends NeoEssentialsEvent {
    // Fields: oldBalance, newBalance, reason, transactionType
    public BigDecimal getOldBalance();
    public BigDecimal getNewBalance(); 
    public BigDecimal getChange();        // Calculated: newBalance - oldBalance
    public String getReason();
    public TransactionType getType();
}
```

**Transaction Types:**
- `DEPOSIT` - Money added to account
- `WITHDRAW` - Money removed from account
- `SET` - Balance set to specific amount
- `TRANSFER_SEND` - Money sent to another player
- `TRANSFER_RECEIVE` - Money received from another player

#### EconomyTransactionEvent
```java
public class EconomyTransactionEvent extends CancellableNeoEssentialsEvent {
    // Fields: amount, reason, type, otherPlayer (for transfers)
    public BigDecimal getAmount();
    public String getReason();
    public TransactionType getType();
    public ServerPlayer getOtherPlayer(); // For transfers, otherwise null
}
```

### Usage Example
```java
@SubscribeEvent
public void onBalanceChange(EconomyBalanceChangeEvent event) {
    ServerPlayer player = event.getPlayer();
    BigDecimal change = event.getChange();
    String reason = event.getReason();
    // Handle balance change logic
}

@SubscribeEvent  
public void onTransaction(EconomyTransactionEvent event) {
    if (event.getAmount().compareTo(BigDecimal.valueOf(1000)) > 0) {
        event.setCanceled(true); // Cancel large transactions
    }
}
```

---

## 🏠 Home & Warp Events

### Home Events
| Event Class | Description | Cancellable |
|-------------|-------------|-------------|
| `HomeSetEvent` | Fired when a player sets a home | ✅ |
| `HomeTeleportEvent` | Fired when a player teleports to a home | ✅ |
| `HomeDeleteEvent` | Fired when a player deletes a home | ✅ |

#### HomeSetEvent
```java
public class HomeSetEvent extends CancellableNeoEssentialsEvent {
    public String getHomeName();
    public double getX();
    public double getY(); 
    public double getZ();
    public String getWorld();
}
```

#### HomeTeleportEvent
```java
public class HomeTeleportEvent extends CancellableNeoEssentialsEvent {
    public String getHomeName();
    public double getX();
    public double getY();
    public double getZ();
    public String getWorld();
}
```

#### HomeDeleteEvent
```java
public class HomeDeleteEvent extends CancellableNeoEssentialsEvent {
    public String getHomeName();
}
```

### Warp Events
| Event Class | Description | Cancellable |
|-------------|-------------|-------------|
| `WarpTeleportEvent` | Fired when a player teleports to a warp | ✅ |

#### WarpTeleportEvent
```java
public class WarpTeleportEvent extends CancellableNeoEssentialsEvent {
    public String getWarpName();
    public double getX();
    public double getY();
    public double getZ();
    public String getWorld();
}
```

### Usage Example
```java
@SubscribeEvent
public void onHomeSet(HomeSetEvent event) {
    ServerPlayer player = event.getPlayer();
    String homeName = event.getHomeName();
    
    // Custom logic - e.g., limit homes in certain worlds
    if (event.getWorld().equals("nether")) {
        event.setCanceled(true);
        player.sendSystemMessage(Component.literal("Cannot set homes in the Nether!"));
    }
}

@SubscribeEvent
public void onHomeTeleport(HomeTeleportEvent event) {
    // Log home teleportation for analytics
    logTeleportEvent(event.getPlayer(), "HOME", event.getHomeName());
}
```

---

## � Teleportation Events

### Teleport Request Events
| Event Class | Description | Cancellable |
|-------------|-------------|-------------|
| `TeleportRequestEvent` | Fired when a teleport request is sent | ✅ |
| `SpawnTeleportEvent` | Fired when a player teleports to spawn | ✅ |

#### TeleportRequestEvent
```java
public class TeleportRequestEvent extends CancellableNeoEssentialsEvent {
    public ServerPlayer getRequester();
    public ServerPlayer getTarget();
    public RequestType getType();
    
    public enum RequestType {
        TPA,      // Teleport to target
        TPAHERE   // Target teleports to requester  
    }
}
```

#### SpawnTeleportEvent
```java
public class SpawnTeleportEvent extends CancellableNeoEssentialsEvent {
    public double getX();
    public double getY();
    public double getZ();
    public String getWorld();
}
```

### Usage Example
```java
@SubscribeEvent
public void onTeleportRequest(TeleportRequestEvent event) {
    ServerPlayer requester = event.getRequester();
    ServerPlayer target = event.getTarget();
    
    // Custom logic - e.g., check if players are in same team
    if (!sameTeam(requester, target)) {
        event.setCanceled(true);
        requester.sendSystemMessage(Component.literal("Cannot teleport to players outside your team!"));
    }
}

@SubscribeEvent
public void onSpawnTeleport(SpawnTeleportEvent event) {
    // Log spawn teleportation
    logTeleportEvent(event.getPlayer(), "SPAWN", "spawn");
}
```

---

## �️ Moderation Events

### Player Moderation Events
| Event Class | Description | Cancellable |
|-------------|-------------|-------------|
| `PlayerMuteEvent` | Fired when a player is muted | ✅ |
| `PlayerUnmuteEvent` | Fired when a player is unmuted | ❌ |
| `PlayerKickEvent` | Fired when a player is kicked | ✅ |

#### PlayerMuteEvent
```java
public class PlayerMuteEvent extends CancellableNeoEssentialsEvent {
    public ServerPlayer getModerator();
    public String getReason();
    public long getDuration();          // 0 for permanent
    public boolean isPermanent();       // Helper method for duration == 0
}
```

#### PlayerUnmuteEvent
```java
public class PlayerUnmuteEvent extends NeoEssentialsEvent {
    public ServerPlayer getModerator();
    public String getReason();
}
```

#### PlayerKickEvent
```java
public class PlayerKickEvent extends CancellableNeoEssentialsEvent {
    public ServerPlayer getModerator();
    public String getReason();
    public void setReason(String reason);   // Allows modifying kick reason
}
```

### Usage Example
```java
@SubscribeEvent
public void onPlayerMute(PlayerMuteEvent event) {
    // Log moderation action
    logModerationAction(event.getPlayer(), "MUTE", event.getReason(), event.getDuration());
    
    // Notify other staff members
    notifyModerators(event.getPlayer(), event.getModerator(), "muted", event.getReason());
}

@SubscribeEvent
public void onPlayerKick(PlayerKickEvent event) {
    // Custom kick reason modification
    if (event.getReason().contains("spam")) {
        event.setReason("Spamming - Please read server rules");
    }
}
```

---

## � Player Events

### Player Status Events
| Event Class | Description | Cancellable |
|-------------|-------------|-------------|
| `PlayerAFKEvent` | Fired when AFK status changes | ❌ |
| `PlayerNicknameChangeEvent` | Fired when nickname changes | ✅ |

#### PlayerAFKEvent
```java
public class PlayerAFKEvent extends NeoEssentialsEvent {
    public boolean isAFK();
    public long getTimestamp();
}
```

#### PlayerNicknameChangeEvent
```java
public class PlayerNicknameChangeEvent extends CancellableNeoEssentialsEvent {
    public String getOldNickname();
    public String getNewNickname();
    public void setNewNickname(String nickname);   // Allows modifying the new nickname
}
```

### Usage Example
```java
@SubscribeEvent
public void onAFKChange(PlayerAFKEvent event) {
    ServerPlayer player = event.getPlayer();
    if (event.isAFK()) {
        // Player went AFK - update displays
        updatePlayerStatus(player, "AFK");
    } else {
        // Player returned from AFK
        updatePlayerStatus(player, "Active");
    }
}

@SubscribeEvent
public void onNicknameChange(PlayerNicknameChangeEvent event) {
    // Filter inappropriate nicknames
    String newNickname = event.getNewNickname();
    if (containsInappropriateContent(newNickname)) {
        event.setCanceled(true);
        event.getPlayer().sendSystemMessage(Component.literal("Inappropriate nickname rejected"));
    }
}
```

---

## �📦 Kit Events

### Kit System Events
| Event Class | Description | Cancellable |
|-------------|-------------|-------------|
| `KitGiveEvent` | Fired when a player receives a kit | ✅ |

#### KitGiveEvent
```java
public class KitGiveEvent extends CancellableNeoEssentialsEvent {
    public String getKitName();
    public boolean hasCooldown();
    public long getCooldownTime();
}
```

### Usage Example
```java
@SubscribeEvent
public void onKitGive(KitGiveEvent event) {
    // Custom kit restrictions
    if (event.getKitName().equals("admin") && !hasPermission(event.getPlayer(), "kit.admin")) {
        event.setCanceled(true);
        event.getPlayer().sendSystemMessage(Component.literal("You don't have permission for this kit"));
        return;
    }
    
    // Log kit usage
    logKitUsage(event.getPlayer(), event.getKitName());
}
```

---

## 💬 Messaging Events

### Communication Events
| Event Class | Description | Cancellable |
|-------------|-------------|-------------|
| `PrivateMessageEvent` | Fired when a player sends a private message | ✅ |
| `MailSendEvent` | Fired when a player sends mail | ✅ |

#### PrivateMessageEvent
```java
public class PrivateMessageEvent extends CancellableNeoEssentialsEvent {
    public ServerPlayer getSender();
    public ServerPlayer getRecipient();
    public String getMessage();
    public void setMessage(String message);     // Allows modifying the message
}
```

#### MailSendEvent
```java
public class MailSendEvent extends CancellableNeoEssentialsEvent {
    public ServerPlayer getSender();
    public String getRecipientName();
    public String getMessage();
    public void setMessage(String message);     // Allows modifying the message
}
```

### Usage Example
```java
@SubscribeEvent
public void onPrivateMessage(PrivateMessageEvent event) {
    // Filter spam or inappropriate content
    String message = event.getMessage();
    if (isSpam(message) || containsInappropriateContent(message)) {
        event.setCanceled(true);
        event.getSender().sendSystemMessage(Component.literal("Message blocked"));
        return;
    }
    
    // Log private messages for moderation
    logPrivateMessage(event.getSender(), event.getRecipient(), message);
}

@SubscribeEvent
public void onMailSend(MailSendEvent event) {
    // Rate limiting for mail
    if (isPlayerSpamming(event.getSender())) {
        event.setCanceled(true);
        event.getSender().sendSystemMessage(Component.literal("You're sending mail too quickly"));
    }
}
```

---

## 🏪 Shop Events

### Minecraft Event Handling
The shop system utilizes standard Minecraft events for interaction and protection:

#### Block Interaction Events
- **Right-Click on Signs** - `PlayerInteractEvent.RightClickBlock` for shop transactions
- **Right-Click on Chests** - Shop chest access protection and validation
- **Block Breaking** - `BlockEvent.BreakEvent` for shop sign and chest protection
- **Block Placement** - `BlockEvent.EntityPlaceEvent` for jail restriction checks

#### Shop Protection System
```java
@SubscribeEvent
public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
    // Handle shop sign interaction
    if (isShopSign(level, pos)) {
        handleShopSignInteraction(player, level, pos, event);
        return;
    }
    
    // Handle shop chest protection
    if (isChest(level, pos)) {
        handleChestAccess(event);
    }
}
```

### Shop Transaction Flow
1. **Player Interaction** - Right-click on shop sign triggers `PlayerInteractEvent.RightClickBlock`
2. **Shop Validation** - Verify shop exists and is valid using ShopManager
3. **Transaction Processing** - SignShopHandler processes buy/sell operations
4. **Economy Integration** - Balance changes through EconomyManager
5. **Transaction Logging** - All transactions recorded for analytics
6. **Protection Enforcement** - Shop chest and sign protection prevents unauthorized access

### Shop Creation Events
Shop creation happens through sign placement and text detection:

#### Sign-Based Shop Creation
```java
@SubscribeEvent
public void onSignChanged(BlockEvent.EntityPlaceEvent event) {
    // Detect shop creation patterns:
    // [buy] / [sell] / [admin buy] / [admin sell]
    // Amount on line 2, price on line 3
    // Auto-link to adjacent chests
}
```

**Shop Creation Requirements:**
- **Valid Sign Text** - First line must be [buy], [sell], [admin buy], or [admin sell]
- **Amount and Price** - Lines 2 and 3 must contain valid numbers
- **Adjacent Chest** - Chest must be adjacent to sign for player shops
- **Permissions** - Player must have appropriate shop creation permissions

---

## 🎯 Placeholder Events

### Placeholder System Events
| Event Class | Description | Cancellable |
|-------------|-------------|-------------|
| `PlaceholderRegisterEvent` | Fired when custom placeholders are registered | ❌ |
| `PlaceholderProcessEvent` | Fired when placeholders are processed | ❌ |

#### PlaceholderRegisterEvent
```java
public class PlaceholderRegisterEvent extends Event {
    public String getIdentifier();      // Placeholder identifier (e.g., "custom_placeholder")
    public String getProviderName();    // Name of the provider registering the placeholder
}
```

#### PlaceholderProcessEvent
```java
public class PlaceholderProcessEvent extends Event {
    public String getOriginalText();        // Text before placeholder processing
    public String getProcessedText();       // Text after placeholder processing
    public void setProcessedText(String processedText);   // Allows modifying the processed result
    public ServerPlayer getPlayer();        // Player context for processing
}
```

### Usage Example
```java
@SubscribeEvent
public void onPlaceholderRegister(PlaceholderRegisterEvent event) {
    // Log placeholder registration for debugging
    LOGGER.info("Registered placeholder: {} by provider: {}", 
               event.getIdentifier(), event.getProviderName());
}

@SubscribeEvent
public void onPlaceholderProcess(PlaceholderProcessEvent event) {
    // Post-process placeholder results
    String processed = event.getProcessedText();
    
    // Apply custom formatting or modifications
    if (processed.contains("admin")) {
        processed = "§c" + processed + "§r"; // Add color codes
        event.setProcessedText(processed);
    }
}
```

---

## 🔧 Event System Integration

### Core Minecraft Events
The main `NeoEssentialsEventHandler` class handles core Minecraft events:

#### Block Events
```java
@EventBusSubscriber(modid = "neoessentials")
public class NeoEssentialsEventHandler {
    
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        // Jail restriction checks
        // Shop sign protection
        // Shop chest protection
    }
    
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        // Jail restriction checks
    }
    
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // Shop sign interactions
        // Shop chest access protection
    }
}
```

#### UI Events
```java
@EventBusSubscriber
public class UIEventHandler {
    
    @SubscribeEvent
    private void onPlayerJoin(PlayerLoggedInEvent event) {
        // TabList integration
        // Player UI setup
    }
    
    @SubscribeEvent
    public void onPermissionUpdate(PermissionUpdateEvent event) {
        // Permission-based UI updates
    }
}
```

### Custom Event Broadcasting
Fire NeoEssentials custom events:

```java
// Fire an economy balance change event
EconomyBalanceChangeEvent balanceEvent = new EconomyBalanceChangeEvent(
    player, oldBalance, newBalance, "Shop purchase", TransactionType.WITHDRAW
);
NeoForge.EVENT_BUS.post(balanceEvent);

// Fire a cancellable transaction event
EconomyTransactionEvent transactionEvent = new EconomyTransactionEvent(
    player, amount, reason, TransactionType.WITHDRAW
);
if (!NeoForge.EVENT_BUS.post(transactionEvent)) {
    // Event was not cancelled, proceed with transaction
    processTransaction(player, amount);
}
```

---

## 🛠️ Developer Integration

### Listening to Events
```java
@EventBusSubscriber(modid = "your_mod_id")
public class YourEventHandler {
    
    @SubscribeEvent
    public static void onEconomyTransaction(EconomyTransactionEvent event) {
        // Custom transaction logic
        if (event.getAmount().compareTo(BigDecimal.valueOf(10000)) > 0) {
            // Log large transactions
            logLargeTransaction(event.getPlayer(), event.getAmount());
        }
    }
    
    @SubscribeEvent 
    public static void onHomeTeleport(HomeTeleportEvent event) {
        // Custom home teleportation handling
        logTeleportUsage(event.getPlayer(), event.getHomeName());
    }
    
    @SubscribeEvent
    public static void onPlayerMute(PlayerMuteEvent event) {
        // Custom moderation integration
        notifyDiscord(event.getPlayer(), "muted", event.getReason());
    }
}
```

### Event Priority
```java
@SubscribeEvent(priority = EventPriority.HIGH)
public void onHighPriorityEvent(EconomyTransactionEvent event) {
    // Runs before normal priority handlers
    // Use for critical checks or validation
}

@SubscribeEvent(priority = EventPriority.LOW) 
public void onLowPriorityEvent(EconomyTransactionEvent event) {
    // Runs after normal priority handlers
    // Use for logging or cleanup tasks
}
```

### Firing Custom Events
```java
// Fire a home set event
HomeSetEvent homeEvent = new HomeSetEvent(player, homeName, x, y, z, worldName);
if (!NeoForge.EVENT_BUS.post(homeEvent)) {
    // Event was not cancelled, proceed with setting home
    savePlayerHome(player, homeName, x, y, z, worldName);
}

// Fire a kit give event
KitGiveEvent kitEvent = new KitGiveEvent(player, kitName, hasCooldown, cooldownTime);
if (!NeoForge.EVENT_BUS.post(kitEvent)) {
    // Event was not cancelled, give the kit
    giveKitToPlayer(player, kitName);
}
```

---

## 🔍 Event Usage Examples

### Economy Event Integration
```java
@SubscribeEvent
public static void onBalanceChange(EconomyBalanceChangeEvent event) {
    BigDecimal change = event.getChange();
    ServerPlayer player = event.getPlayer();
    
    // Log significant balance changes
    if (change.abs().compareTo(BigDecimal.valueOf(1000)) >= 0) {
        LOGGER.info("Large balance change for {}: {} ({})", 
                   player.getName().getString(), change, event.getReason());
    }
    
    // Update player displays
    updatePlayerBalanceDisplay(player, event.getNewBalance());
}

@SubscribeEvent
public static void onTransaction(EconomyTransactionEvent event) {
    // Fraud prevention
    if (event.getType() == TransactionType.TRANSFER_SEND) {
        if (event.getAmount().compareTo(BigDecimal.valueOf(50000)) > 0) {
            // Cancel extremely large transfers
            event.setCanceled(true);
            event.getPlayer().sendSystemMessage(Component.literal(
                "§cTransfer amount too large! Contact an administrator for large transfers."));
        }
    }
}
```

### Teleportation Event Integration
```java
@SubscribeEvent
public static void onTeleportRequest(TeleportRequestEvent event) {
    ServerPlayer requester = event.getRequester();
    ServerPlayer target = event.getTarget();
    
    // Check if target allows teleport requests
    if (!allowsTeleportRequests(target)) {
        event.setCanceled(true);
        requester.sendSystemMessage(Component.literal(
            "§c" + target.getName().getString() + " is not accepting teleport requests."));
        return;
    }
    
    // Log teleport requests for moderation
    logTeleportRequest(requester, target, event.getType());
}

@SubscribeEvent
public static void onHomeSet(HomeSetEvent event) {
    // Limit homes in certain dimensions
    if (event.getWorld().equals("minecraft:the_end")) {
        event.setCanceled(true);
        event.getPlayer().sendSystemMessage(Component.literal(
            "§cCannot set homes in The End!"));
    }
}
```

### Moderation Event Integration
```java
@SubscribeEvent
public static void onPlayerMute(PlayerMuteEvent event) {
    ServerPlayer player = event.getPlayer();
    ServerPlayer moderator = event.getModerator();
    
    // Log to moderation system
    logModerationAction("MUTE", player, moderator, event.getReason(), event.getDuration());
    
    // Notify other staff
    notifyStaffMembers(moderator.getName().getString() + " muted " + 
                      player.getName().getString() + " for: " + event.getReason());
    
    // Update player status displays
    updatePlayerMuteStatus(player, true, event.getDuration());
}
```

---

## 📚 Related Documentation

- [Configuration Guide](Configuration.md) - System configuration options
- [Commands Guide](Commands.md) - Complete command documentation
- [API Documentation](API_DOCUMENTATION.md) - Developer integration guide
- [Permissions Guide](Permissions.md) - Permission system setup

---
