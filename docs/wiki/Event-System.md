# Event System

NeoEssentials provides a comprehensive event system that allows developers to respond to various actions and changes within the mod. This guide explains how the event system works, how to create and listen to events, and provides a complete reference of available events.

## Understanding the Event System

The event system in NeoEssentials follows the observer pattern, allowing code to react to various actions without direct coupling. Events are fired by NeoEssentials when something happens (like a player teleporting home), and listener methods can be registered to handle these events.

## Event Listener Basics

### Creating an Event Listener

To create an event listener, implement the `EventListener` interface and add methods with the `@Subscribe` annotation:

```java
import com.zerog.neoessentials.api.event.EventListener;
import com.zerog.neoessentials.api.event.Subscribe;
import com.zerog.neoessentials.api.event.events.PlayerBalanceChangeEvent;

public class MyEventListener implements EventListener {
    
    @Subscribe
    public void onBalanceChange(PlayerBalanceChangeEvent event) {
        // This method will be called when a player's balance changes
        ServerPlayer player = event.getPlayer();
        double oldBalance = event.getOldBalance();
        double newBalance = event.getNewBalance();
        
        player.sendSystemMessage(Component.literal(
            "Your balance changed from " + oldBalance + " to " + newBalance
        ));
    }
}
```

### Registering Event Listeners

To register your event listener with NeoEssentials:

```java
import com.zerog.neoessentials.api.NeoEssentialsAPI;

public class MyMod {
    public void init() {
        NeoEssentialsAPI api = NeoEssentialsAPI.getInstance();
        if (api != null) {
            api.getEventManager().registerListener(new MyEventListener());
        }
    }
}
```

### Event Priorities

You can specify the priority of your event handler using the `priority` parameter:

```java
@Subscribe(priority = EventPriority.HIGH)
public void onBalanceChange(PlayerBalanceChangeEvent event) {
    // This handler will run before NORMAL priority handlers
}

@Subscribe(priority = EventPriority.LOWEST)
public void onBalanceChangeLast(PlayerBalanceChangeEvent event) {
    // This handler will run last
}
```

Available priorities (in execution order):
- `HIGHEST` - Runs first
- `HIGH` - Runs after HIGHEST
- `NORMAL` - Default priority
- `LOW` - Runs after NORMAL
- `LOWEST` - Runs last

### Cancellable Events

Many events in NeoEssentials can be cancelled to prevent the default action from occurring:

```java
@Subscribe
public void onPlayerHomeTeleport(PlayerHomeTeleportEvent event) {
    ServerPlayer player = event.getPlayer();
    Home home = event.getHome();
    
    // Prevent teleporting to homes in combat
    if (isPlayerInCombat(player)) {
        event.setCancelled(true);
        player.sendSystemMessage(Component.literal("You cannot teleport while in combat!"));
    }
}
```

## Core Event Categories

NeoEssentials events are grouped into several categories:

### Economy Events

Events related to the economy system:

- `PlayerBalanceChangeEvent` - Fired when a player's balance changes
- `TransactionEvent` - Fired when a transaction occurs
- `InsufficientFundsEvent` - Fired when a transaction fails due to insufficient funds

### Home Events

Events related to the home system:

- `PlayerHomeCreateEvent` - Fired when a player creates a home
- `PlayerHomeDeleteEvent` - Fired when a player deletes a home
- `PlayerHomeTeleportEvent` - Fired when a player teleports to a home
- `PlayerHomeUpdateEvent` - Fired when a player updates a home's location

### Warp Events

Events related to the warp system:

- `WarpCreateEvent` - Fired when a warp is created
- `WarpDeleteEvent` - Fired when a warp is deleted
- `WarpTeleportEvent` - Fired when a player teleports to a warp
- `WarpUpdateEvent` - Fired when a warp's location is updated

### Kit Events

Events related to the kit system:

- `KitUseEvent` - Fired when a player uses a kit
- `KitCooldownEvent` - Fired when a kit cooldown is applied/expired
- `KitCreateEvent` - Fired when a kit is created/registered
- `KitDeleteEvent` - Fired when a kit is deleted

### Player Events

Events related to player actions:

- `PlayerFirstJoinEvent` - Fired when a player joins for the first time
- `PlayerTeleportRequestEvent` - Fired when a teleport request is sent
- `PlayerCommandEvent` - Fired when a player uses a NeoEssentials command
- `PlayerMessageEvent` - Fired when a player sends a message processed by NeoEssentials

### Tablist Events

Events related to the tablist system:

- `TablistUpdateEvent` - Fired when the tablist is updated
- `TablistHeaderFooterChangeEvent` - Fired when the header/footer changes
- `BossBarUpdateEvent` - Fired when a bossbar is updated

### Config Events

Events related to configuration changes:

- `ConfigLoadEvent` - Fired when a config is loaded
- `ConfigSaveEvent` - Fired when a config is saved
- `ConfigReloadEvent` - Fired when configs are reloaded

## Complete Event Reference

### Economy Events

#### PlayerBalanceChangeEvent

Fired when a player's balance changes.

```java
@Subscribe
public void onBalanceChange(PlayerBalanceChangeEvent event) {
    ServerPlayer player = event.getPlayer();
    double oldBalance = event.getOldBalance();
    double newBalance = event.getNewBalance();
    double difference = event.getDifference();
    TransactionType type = event.getTransactionType();
    
    if (type == TransactionType.DEPOSIT) {
        // Handle deposit
    } else if (type == TransactionType.WITHDRAW) {
        // Handle withdrawal
    }
}
```

Properties:
- `getPlayer()` - The player whose balance changed
- `getOldBalance()` - The player's balance before the change
- `getNewBalance()` - The player's balance after the change
- `getDifference()` - The amount by which the balance changed
- `getTransactionType()` - The type of transaction (DEPOSIT, WITHDRAW, TRANSFER, SET)
- `getReason()` - The reason for the balance change
- `isCancelled()` - Whether the event is cancelled
- `setCancelled(boolean)` - Set whether the event is cancelled

#### TransactionEvent

Fired when a transaction occurs between players or with the server.

```java
@Subscribe
public void onTransaction(TransactionEvent event) {
    UUID source = event.getSource();
    UUID target = event.getTarget();
    double amount = event.getAmount();
    String reason = event.getReason();
    
    // Log the transaction
    getLogger().info(
        String.format(
            "Transaction of %s from %s to %s for %s", 
            amount, 
            source.toString(),
            target.toString(),
            reason
        )
    );
}
```

Properties:
- `getSource()` - The UUID of the transaction source (can be a player or the server)
- `getTarget()` - The UUID of the transaction target (can be a player or the server)
- `getAmount()` - The transaction amount
- `getReason()` - The reason for the transaction
- `isCancelled()` - Whether the event is cancelled
- `setCancelled(boolean)` - Set whether the event is cancelled

#### InsufficientFundsEvent

Fired when a transaction fails due to insufficient funds.

```java
@Subscribe
public void onInsufficientFunds(InsufficientFundsEvent event) {
    ServerPlayer player = event.getPlayer();
    double required = event.getRequiredAmount();
    double available = event.getAvailableAmount();
    double shortfall = event.getShortfall();
    
    // Notify player
    player.sendSystemMessage(Component.literal(
        "You need " + shortfall + " more to complete this transaction."
    ));
}
```

Properties:
- `getPlayer()` - The player who has insufficient funds
- `getRequiredAmount()` - The amount required for the transaction
- `getAvailableAmount()` - The amount the player has available
- `getShortfall()` - The difference between required and available
- `getTransactionType()` - The type of transaction that failed

### Home Events

#### PlayerHomeCreateEvent

Fired when a player creates a home.

```java
@Subscribe
public void onHomeCreate(PlayerHomeCreateEvent event) {
    ServerPlayer player = event.getPlayer();
    String homeName = event.getHomeName();
    Location location = event.getLocation();
    
    // Broadcast to admins
    broadcastToAdmins(
        player.getName().getString() + " created a home named " + 
        homeName + " at " + formatLocation(location)
    );
}
```

Properties:
- `getPlayer()` - The player creating the home
- `getHomeName()` - The name of the home
- `getLocation()` - The location of the home
- `isCancelled()` - Whether the event is cancelled
- `setCancelled(boolean)` - Set whether the event is cancelled

#### PlayerHomeDeleteEvent

Fired when a player deletes a home.

```java
@Subscribe
public void onHomeDelete(PlayerHomeDeleteEvent event) {
    ServerPlayer player = event.getPlayer();
    String homeName = event.getHomeName();
    Home home = event.getHome();
    
    // Log deletion
    getLogger().info(
        player.getName().getString() + " deleted home " + homeName
    );
}
```

Properties:
- `getPlayer()` - The player deleting the home
- `getHomeName()` - The name of the home
- `getHome()` - The home being deleted
- `isCancelled()` - Whether the event is cancelled
- `setCancelled(boolean)` - Set whether the event is cancelled

#### PlayerHomeTeleportEvent

Fired when a player teleports to a home.

```java
@Subscribe
public void onHomeTeleport(PlayerHomeTeleportEvent event) {
    ServerPlayer player = event.getPlayer();
    Home home = event.getHome();
    Location destination = event.getDestination();
    
    // Check for unsafe destination
    if (isLocationUnsafe(destination)) {
        event.setCancelled(true);
        player.sendSystemMessage(Component.literal(
            "Teleport cancelled: Destination appears to be unsafe!"
        ));
    }
}
```

Properties:
- `getPlayer()` - The player teleporting
- `getHome()` - The home being teleported to
- `getDestination()` - The destination location
- `getDelay()` - The teleport delay in seconds
- `setDelay(int)` - Set the teleport delay
- `isCancelled()` - Whether the event is cancelled
- `setCancelled(boolean)` - Set whether the event is cancelled

## Creating Custom Events

You can create custom events that integrate with the NeoEssentials event system:

```java
import com.zerog.neoessentials.api.event.Event;
import com.zerog.neoessentials.api.event.Cancellable;

public class MyCustomEvent extends Event implements Cancellable {
    
    private final ServerPlayer player;
    private final String customData;
    private boolean cancelled = false;
    
    public MyCustomEvent(ServerPlayer player, String customData) {
        this.player = player;
        this.customData = customData;
    }
    
    public ServerPlayer getPlayer() {
        return player;
    }
    
    public String getCustomData() {
        return customData;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
```

### Firing Custom Events

To fire your custom event:

```java
// Create the event
MyCustomEvent event = new MyCustomEvent(player, "Some custom data");

// Fire the event
NeoEssentialsAPI api = NeoEssentialsAPI.getInstance();
api.getEventManager().callEvent(event);

// Check if the event was cancelled
if (!event.isCancelled()) {
    // Proceed with action
} else {
    // Event was cancelled
}
```

## Advanced Event Techniques

### Event Filtering

You can filter events using the `ignoreCancelled` parameter:

```java
@Subscribe(ignoreCancelled = true)
public void onHomeTeleport(PlayerHomeTeleportEvent event) {
    // This handler will only be called for non-cancelled events
}
```

### Async Events

Some events might be fired from asynchronous threads. Handle them safely:

```java
@Subscribe
public void onAsyncEvent(AsyncDatabaseQueryEvent event) {
    // Make sure not to access game state directly from async events
    
    // Use the scheduler to run code on the main thread
    NeoEssentialsAPI.getInstance().getScheduler().runSync(() -> {
        // Safe to access game state here
    });
}
```

### Event Result Handling

Some events allow modifying the result:

```java
@Subscribe
public void onPermissionCheck(PermissionCheckEvent event) {
    // Override permission check for certain cases
    if (event.getPermission().equals("neoessentials.specialaccess") && 
            isSpecialDay()) {
        event.setResult(PermissionCheckEvent.Result.ALLOW);
    }
}
```

## Best Practices

### Performance Considerations

1. **Keep Handlers Light**: Event handlers should execute quickly
2. **Avoid Blocking Operations**: Don't perform blocking I/O in event handlers
3. **Be Mindful of Priority**: Higher priority handlers run first but should be used sparingly
4. **Release Resources**: Clean up any resources used in event handlers

### Thread Safety

1. **Respect Thread Context**: Async events must not manipulate game state directly
2. **Use Synchronous Tasks**: Schedule synchronous tasks for game state manipulation
3. **Avoid Deadlocks**: Don't wait for locks in event handlers

### Code Organization

1. **Group Related Handlers**: Keep related event handlers in the same class
2. **Clear Handler Names**: Use descriptive method names for event handlers
3. **Single Responsibility**: Each handler should have a clear purpose
4. **Documentation**: Document what your handlers are doing, especially if cancelling events

## Debugging Events

### Logging Event Flow

You can enable event logging to debug event flow:

```java
@Subscribe
public void onAnyEvent(Event event) {
    getLogger().debug("Event fired: " + event.getClass().getSimpleName());
}
```

### Event Debugging Commands

Use these commands to debug events:

```
/neoessentials:debug events on
/neoessentials:debug events off
/neoessentials:events list
/neoessentials:events info <eventName>
```

## Common Use Cases

### Anti-Griefing Protection

```java
@Subscribe
public void onWarpCreate(WarpCreateEvent event) {
    ServerPlayer player = event.getPlayer();
    Location location = event.getLocation();
    
    // Check if the location is in a protected region
    if (isProtectedRegion(location)) {
        event.setCancelled(true);
        player.sendSystemMessage(Component.literal(
            "You cannot create warps in protected regions."
        ));
    }
}
```

### Custom Welcome Messages

```java
@Subscribe
public void onPlayerFirstJoin(PlayerFirstJoinEvent event) {
    ServerPlayer player = event.getPlayer();
    
    // Send welcome message to the player
    player.sendSystemMessage(Component.literal("Welcome to the server!"));
    
    // Broadcast to other players
    for (ServerPlayer otherPlayer : server.getPlayerList().getPlayers()) {
        if (otherPlayer != player) {
            otherPlayer.sendSystemMessage(Component.literal(
                player.getName().getString() + " joined for the first time!"
            ));
        }
    }
    
    // Give starter items
    player.addItem(new ItemStack(Items.BREAD, 16));
    player.addItem(new ItemStack(Items.STONE_SWORD));
}
```

### Transaction Logging

```java
@Subscribe
public void onTransaction(TransactionEvent event) {
    // Format: [timestamp] source -> target: amount (reason)
    String log = String.format(
        "[%s] %s -> %s: %.2f (%s)",
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
        getPlayerName(event.getSource()),
        getPlayerName(event.getTarget()),
        event.getAmount(),
        event.getReason()
    );
    
    // Log to file
    try (FileWriter fw = new FileWriter("economy-log.txt", true)) {
        fw.write(log + "\n");
    } catch (IOException e) {
        getLogger().error("Failed to write to economy log", e);
    }
}
```

## Event Testing

To properly test your event listeners, you should:

1. Create a test environment
2. Manually trigger the events you want to test
3. Verify that your handlers respond appropriately
4. Test both successful and cancelled cases

Here's a simple test framework:

```java
public void testEventHandlers() {
    // Create a test player
    ServerPlayer testPlayer = createMockPlayer();
    
    // Create and fire a test event
    PlayerHomeTeleportEvent event = new PlayerHomeTeleportEvent(
        testPlayer, 
        new Home("test", new Location(world, 0, 64, 0)),
        new Location(world, 0, 64, 0),
        5
    );
    
    api.getEventManager().callEvent(event);
    
    // Assert expected outcomes
    assert event.getDelay() == 10; // If your handler modifies the delay
    assert !event.isCancelled(); // If your handler shouldn't cancel this case
}
```

## Additional Resources

- [NeoEssentials API Documentation](API-Documentation)
- [Creating Extensions](Creating-Extensions) guide
- [Custom Placeholders](Custom-Placeholders) guide
- [JavaDoc Documentation](https://docs.zerog.network/neoessentials/api/event/package-summary.html)
- [Example Event Listeners](https://github.com/ZeroG-Network/NeoEssentialsExamples/tree/main/src/main/java/com/example/events)
- [NeoEssentials Discord](https://discord.gg/dUGAQF2Mga) for developer support
