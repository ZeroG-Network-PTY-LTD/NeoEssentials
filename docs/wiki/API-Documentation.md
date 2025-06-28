# API Documentation

NeoEssentials provides a comprehensive API that allows developers to integrate with and extend its functionality. This documentation covers the core API components, usage examples, and best practices.

## Getting Started

### Adding NeoEssentials as a Dependency

#### Gradle

```gradle
repositories {
    maven { url = "https://maven.zerog.network/releases" }
}

dependencies {
    implementation fg.deobf("com.zerog:neoessentials:1.0.1:api")
}
```

#### Maven

```xml
<repositories>
    <repository>
        <id>zerog-maven</id>
        <url>https://maven.zerog.network/releases</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.zerog</groupId>
        <artifactId>neoessentials</artifactId>
        <version>1.0.1</version>
        <classifier>api</classifier>
    </dependency>
</dependencies>
```

### Accessing the API

The main entry point to the API is through the `NeoEssentials` class:

```java
import com.zerog.neoessentials.api.NeoEssentialsAPI;

public class MyMod {
    public void example() {
        NeoEssentialsAPI api = NeoEssentialsAPI.getInstance();
        
        if (api != null) {
            // NeoEssentials is loaded, use the API
        } else {
            // NeoEssentials is not loaded
        }
    }
}
```

## Core API Components

### Economy API

```java
// Get the economy manager
EconomyManager economyManager = api.getEconomyManager();

// Get player balance
double balance = economyManager.getBalance(playerUUID);

// Set player balance
economyManager.setBalance(playerUUID, 1000.0);

// Add to player balance
boolean success = economyManager.deposit(playerUUID, 100.0);

// Remove from player balance
boolean success = economyManager.withdraw(playerUUID, 50.0);

// Transfer money between players
boolean success = economyManager.transfer(senderUUID, receiverUUID, 100.0);
```

### Homes API

```java
// Get the homes manager
HomeManager homeManager = api.getHomeManager();

// Get player homes
List<Home> homes = homeManager.getHomes(playerUUID);

// Get specific home
Optional<Home> home = homeManager.getHome(playerUUID, "home");

// Create a new home
Location location = new Location(world, x, y, z, yaw, pitch);
boolean success = homeManager.createHome(playerUUID, "home", location);

// Update home location
boolean success = homeManager.updateHome(playerUUID, "home", newLocation);

// Delete a home
boolean success = homeManager.deleteHome(playerUUID, "home");
```

### Warps API

```java
// Get the warps manager
WarpManager warpManager = api.getWarpManager();

// Get all warps
List<Warp> warps = warpManager.getWarps();

// Get specific warp
Optional<Warp> warp = warpManager.getWarp("spawn");

// Create a warp
Location location = new Location(world, x, y, z, yaw, pitch);
boolean success = warpManager.createWarp("spawn", location);

// Update warp location
boolean success = warpManager.updateWarp("spawn", newLocation);

// Delete a warp
boolean success = warpManager.deleteWarp("spawn");
```

### Kits API

```java
// Get the kits manager
KitManager kitManager = api.getKitManager();

// Get available kits
List<Kit> kits = kitManager.getKits();

// Get specific kit
Optional<Kit> kit = kitManager.getKit("starter");

// Give kit to player
boolean success = kitManager.giveKit(player, "starter");

// Check if player can use kit (considering cooldowns)
boolean canUse = kitManager.canUseKit(player, "starter");

// Create a kit
Kit newKit = new Kit.Builder("vip")
    .setCooldown(86400) // 24 hours in seconds
    .addItem(new ItemStack(Items.DIAMOND_SWORD))
    .addItem(new ItemStack(Items.DIAMOND_PICKAXE))
    .setPermission("neoessentials.kit.vip")
    .build();
kitManager.registerKit(newKit);
```

### Tablist API

```java
// Get the tablist manager
TabManager tabManager = api.getTabManager();

// Update player's tab list
tabManager.updateTabList(player);

// Set custom header and footer for a player
Component header = Component.text("Custom Header");
Component footer = Component.text("Custom Footer");
tabManager.setHeaderAndFooter(player, header, footer);

// Get template manager for working with templates
TemplateManager templateManager = tabManager.getTemplateManager();
```

### Template API

```java
// Get the template manager
TemplateManager templateManager = api.getTemplateManager();

// Get a specific template
Optional<Template> template = templateManager.getTemplate("welcome");

// Process a template for a player
String processed = templateManager.processTemplate("welcome", player);

// Register a custom template
Template customTemplate = new Template.Builder("custom")
    .addLine("&6Welcome to the server!")
    .addLine("&eThis is a custom template.")
    .build();
templateManager.registerTemplate("custom", customTemplate);

// Create a template from string content
String content = "&6Line 1\n&eLine 2\n&aLine 3";
Template template = templateManager.createFromString(content);
```

### Animation API

```java
// Get the animation manager
AnimationManager animationManager = api.getAnimationManager();

// Get a specific animation
Optional<Animation> animation = animationManager.getAnimation("server_name");

// Process the current frame of an animation
String currentFrame = animationManager.getCurrentFrame("server_name");

// Register a custom animation
List<String> frames = Arrays.asList(
    "&cFrame 1",
    "&6Frame 2",
    "&eFrame 3",
    "&aFrame 4"
);
Animation customAnimation = new Animation("custom", frames, 20);
animationManager.registerAnimation("custom", customAnimation);
```

### Permission API

```java
// Get the permission manager
PermissionManager permissionManager = api.getPermissionManager();

// Check if player has permission
boolean hasPermission = permissionManager.hasPermission(player, "neoessentials.command.home");

// Check if player is in group
boolean isInGroup = permissionManager.isInGroup(player, "vip");

// Get player's prefix
String prefix = permissionManager.getPrefix(player);

// Get player's suffix
String suffix = permissionManager.getSuffix(player);

// Get player's groups
List<String> groups = permissionManager.getGroups(player);
```

### Command API

```java
// Get the command manager
CommandManager commandManager = api.getCommandManager();

// Register a custom command
commandManager.registerCommand(new MyCustomCommand());

// Check if player can use a command
boolean canUse = commandManager.canUseCommand(player, "home");

// Execute a command as a player
boolean success = commandManager.executeCommand(player, "home set main");
```

### Placeholder API

```java
// Get the placeholder manager
PlaceholderManager placeholderManager = api.getPlaceholderManager();

// Register a custom placeholder
placeholderManager.registerPlaceholder("custom_placeholder", player -> {
    return "Custom value for " + player.getName();
});

// Process placeholders in a string
String processed = placeholderManager.process(player, "Hello {player}, your custom value is {custom_placeholder}");
```

### Event API

```java
// Get the event manager
EventManager eventManager = api.getEventManager();

// Register a custom event listener
eventManager.registerListener(new MyCustomListener());

// Call a custom event
CustomEvent event = new CustomEvent(player);
eventManager.callEvent(event);
```

## Common Development Tasks

### Creating a Custom Command

```java
import com.zerog.neoessentials.api.command.CommandBase;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;

public class MyCustomCommand extends CommandBase {
    
    public MyCustomCommand() {
        super("mycommand", "neoessentials.command.mycommand");
    }
    
    @Override
    public int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (source.getEntity() instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.literal("Custom command executed!"));
            return Command.SINGLE_SUCCESS;
        }
        
        return 0;
    }
    
    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal(getName())
                .requires(source -> checkPermission(source))
                .executes(this)
        );
    }
}
```

### Creating a Custom Event Listener

```java
import com.zerog.neoessentials.api.event.EventListener;
import com.zerog.neoessentials.api.event.events.PlayerBalanceChangeEvent;

public class MyCustomListener implements EventListener {
    
    @Subscribe
    public void onBalanceChange(PlayerBalanceChangeEvent event) {
        ServerPlayer player = event.getPlayer();
        double oldBalance = event.getOldBalance();
        double newBalance = event.getNewBalance();
        
        // Custom logic when a player's balance changes
        if (newBalance > oldBalance) {
            player.sendSystemMessage(Component.literal("You earned money!"));
        } else {
            player.sendSystemMessage(Component.literal("You spent money!"));
        }
    }
}
```

### Creating a Custom Template Processor

```java
import com.zerog.neoessentials.api.template.TemplateProcessor;
import com.zerog.neoessentials.api.template.ProcessorContext;

public class MyCustomProcessor implements TemplateProcessor {
    
    @Override
    public String process(String content, ProcessorContext context) {
        // Replace custom placeholders in content
        content = content.replaceAll("\\{my_custom_placeholder\\}", "Custom Value");
        
        // Return processed content
        return content;
    }
}

// Register the custom processor
templateManager.registerProcessor(new MyCustomProcessor());
```

## Working with Config Files

### Accessing Configuration

```java
// Get the config manager
ConfigManager configManager = api.getConfigManager();

// Get a specific config
Optional<Config> config = configManager.getConfig("economy");

// Read values from a config
double startingBalance = config.get().getDouble("startingBalance", 100.0);
String currencyName = config.get().getString("currencyName", "Coins");
boolean enabled = config.get().getBoolean("enabled", true);

// Modify and save config
config.get().set("startingBalance", 200.0);
configManager.saveConfig("economy");
```

### Creating a Custom Config

```java
// Create a new config file
Config customConfig = configManager.createConfig("mycustom");

// Set default values
customConfig.setDefaults(Map.of(
    "enabled", true,
    "interval", 60,
    "message", "Hello World"
));

// Save the config
configManager.saveConfig("mycustom");
```

## Database Integration

### Working with Database

```java
// Get the database manager
DatabaseManager databaseManager = api.getDatabaseManager();

// Execute a query
databaseManager.executeQuery(
    "SELECT * FROM players WHERE last_login > ?", 
    preparedStatement -> {
        preparedStatement.setLong(1, System.currentTimeMillis() - (86400000 * 7));
    },
    resultSet -> {
        List<String> recentPlayers = new ArrayList<>();
        while (resultSet.next()) {
            recentPlayers.add(resultSet.getString("name"));
        }
        return recentPlayers;
    }
);

// Execute an update
databaseManager.executeUpdate(
    "UPDATE players SET balance = ? WHERE uuid = ?",
    preparedStatement -> {
        preparedStatement.setDouble(1, 1000.0);
        preparedStatement.setString(2, playerUUID.toString());
    }
);
```

## Advanced API Usage

### Creating a Custom Module

```java
import com.zerog.neoessentials.api.module.ModuleBase;

public class MyCustomModule extends ModuleBase {
    
    public MyCustomModule() {
        super("mycustom", "My Custom Module");
    }
    
    @Override
    public void onEnable() {
        // Module initialization code
        getLogger().info("My custom module enabled!");
        
        // Register commands
        getCommandManager().registerCommand(new MyCustomCommand());
        
        // Register events
        getEventManager().registerListener(new MyCustomListener());
    }
    
    @Override
    public void onDisable() {
        // Clean up resources
        getLogger().info("My custom module disabled!");
    }
}

// Register the module
api.getModuleManager().registerModule(new MyCustomModule());
```

### Integration with External Mods

```java
// Check if another mod is loaded
boolean isLuckPermsLoaded = api.isModLoaded("luckperms");

if (isLuckPermsLoaded) {
    // Integrate with LuckPerms
    // ...
} else {
    // Fall back to built-in permission system
    // ...
}
```

### Working with Player Data

```java
// Get the player manager
PlayerManager playerManager = api.getPlayerManager();

// Get player data
PlayerData playerData = playerManager.getPlayerData(playerUUID);

// Access player data
long firstJoin = playerData.getFirstJoin();
long lastJoin = playerData.getLastJoin();
long playtime = playerData.getPlaytime();

// Update player data
playerData.setCustomData("faction", "Warriors");
playerManager.savePlayerData(playerUUID);
```

## Best Practices

### Thread Safety

NeoEssentials API methods should be called from the server thread unless explicitly stated otherwise. For async operations:

```java
api.getScheduler().runAsync(() -> {
    // Perform async database operations
    // ...
    
    // Run follow-up code on the main thread
    api.getScheduler().runSync(() -> {
        // Update player on the main thread
    });
});
```

### Error Handling

Always handle exceptions when using the API:

```java
try {
    api.getEconomyManager().withdraw(playerUUID, amount);
} catch (InsufficientFundsException e) {
    player.sendSystemMessage(Component.literal("You don't have enough money!"));
} catch (Exception e) {
    player.sendSystemMessage(Component.literal("An error occurred: " + e.getMessage()));
    api.getLogger().error("Error in economy transaction", e);
}
```

### Version Compatibility

Check API version compatibility:

```java
String apiVersion = api.getVersion();
if (apiVersion.startsWith("1.0")) {
    // Compatible with API v1.0.x
} else {
    getLogger().warn("Unsupported NeoEssentials API version: " + apiVersion);
}
```

### Resource Management

Close resources properly:

```java
FileHandle fileHandle = api.getFileManager().openFile("data.bin");
try {
    // Use the file
    // ...
} finally {
    fileHandle.close();
}
```

## Event System Reference

### Core Events

| Event | Description |
|-------|-------------|
| `PlayerBalanceChangeEvent` | Fired when a player's balance changes |
| `PlayerHomeCreateEvent` | Fired when a player creates a home |
| `PlayerHomeDeleteEvent` | Fired when a player deletes a home |
| `PlayerHomeTeleportEvent` | Fired when a player teleports to a home |
| `WarpCreateEvent` | Fired when a warp is created |
| `WarpDeleteEvent` | Fired when a warp is deleted |
| `WarpTeleportEvent` | Fired when a player teleports to a warp |
| `PlayerFirstJoinEvent` | Fired when a player joins for the first time |
| `KitUseEvent` | Fired when a player uses a kit |

### Listening to Events

```java
@Subscribe
public void onPlayerFirstJoin(PlayerFirstJoinEvent event) {
    ServerPlayer player = event.getPlayer();
    player.sendSystemMessage(Component.literal("Welcome to the server!"));
    
    // Give starter items
    player.addItem(new ItemStack(Items.BREAD, 16));
}
```

### Cancellable Events

Many events can be cancelled to prevent the default action:

```java
@Subscribe
public void onHomeTeleport(PlayerHomeTeleportEvent event) {
    ServerPlayer player = event.getPlayer();
    Home home = event.getHome();
    
    // Check if player is in a restricted area
    if (isInRestrictedArea(player)) {
        event.setCancelled(true);
        player.sendSystemMessage(Component.literal("You cannot teleport from restricted areas!"));
    }
}
```

## Placeholder System Reference

### Registering Custom Placeholders

```java
// Simple placeholder
api.getPlaceholderManager().registerPlaceholder("server_name", player -> "My Awesome Server");

// Player-specific placeholder
api.getPlaceholderManager().registerPlaceholder("player_rank", player -> {
    // Custom logic to determine player rank
    return getPlayerRank(player);
});

// Placeholders with arguments
api.getPlaceholderManager().registerPlaceholder("top_player", (player, args) -> {
    if (args.length > 0) {
        try {
            int position = Integer.parseInt(args[0]);
            return getTopPlayer(position);
        } catch (NumberFormatException e) {
            return "Invalid position";
        }
    }
    return getTopPlayer(1);
});
```

## API Versions and Compatibility

| NeoEssentials Version | API Version | NeoForge Version | Java Version |
|-----------------------|-------------|------------------|--------------|
| 1.0.0 | 1.0 | NF 1.20.1+ | Java 17+ |
| 1.0.1 | 1.0 | NF 1.21.1+ | Java 17+ |

## Additional Resources

- [GitHub Repository](https://github.com/ZeroG-Network/NeoEssentials)
- [JavaDoc Documentation](https://docs.zerog.network/neoessentials)
- [Example Projects](https://github.com/ZeroG-Network/NeoEssentialsExamples)
- [Creating Extensions](Creating-Extensions) guide for addon development
- [Event System](Event-System) detailed documentation
- [Custom Placeholders](Custom-Placeholders) guide
- [NeoEssentials Discord](https://discord.gg/dUGAQF2Mga) for developer support
