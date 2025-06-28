# Creating Extensions for NeoEssentials

This guide provides developers with the information needed to create extensions for the NeoEssentials mod, allowing for custom functionality and integrations.

![Development Icon](../images/icons/development.png)

## Extension Architecture

NeoEssentials provides an extension framework that allows developers to create add-ons that integrate seamlessly with the core functionality.

### Extension Basics

An extension is a separate mod that:
- Depends on the NeoEssentials mod
- Uses the NeoEssentials API
- Registers itself as an extension
- Provides additional features or integrates with other mods

## Creating Your First Extension

### Setup Development Environment

1. Set up a standard NeoForge mod development environment
2. Add NeoEssentials as a dependency in your `build.gradle`:

```gradle
dependencies {
    implementation fg.deobf("com.zerog:neoessentials:1.0.0:api")
    // Other dependencies...
}
```

### Basic Extension Structure

```java
package com.example.myextension;

import com.zerog.neoessentials.api.extension.NeoExtension;
import com.zerog.neoessentials.api.extension.ExtensionInfo;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod("myextension")
public class MyExtension implements NeoExtension {
    
    private static MyExtension instance;
    
    public MyExtension(IEventBus modEventBus) {
        instance = this;
        
        // Register with NeoEssentials
        NeoExtensionRegistry.register(this);
    }
    
    @Override
    public ExtensionInfo getExtensionInfo() {
        return new ExtensionInfo(
            "My Extension",
            "1.0.0",
            "A simple extension for NeoEssentials",
            "YourUsername"
        );
    }
    
    @Override
    public void onInit() {
        // Called when NeoEssentials initializes this extension
        // Initialize your extension features here
    }
    
    @Override
    public void onEnable() {
        // Called when the extension is enabled
        // Start your extension's services here
    }
    
    @Override
    public void onDisable() {
        // Called when the extension is disabled
        // Clean up resources here
    }
    
    public static MyExtension getInstance() {
        return instance;
    }
}
```

## Integration Points

### Command Registration

Add custom commands to NeoEssentials:

```java
import com.zerog.neoessentials.api.command.CommandAPI;
import com.zerog.neoessentials.api.command.CommandBase;

public class MyCustomCommand extends CommandBase {
    public MyCustomCommand() {
        super("mycommand");
        setDescription("A custom command from my extension");
        setPermission("myextension.command.mycommand");
    }
    
    @Override
    public void execute(CommandContext context) {
        // Command logic here
    }
}

// In your extension class:
@Override
public void onInit() {
    CommandAPI.register(new MyCustomCommand());
}
```

### Placeholder Registration

Add custom placeholders:

```java
import com.zerog.neoessentials.api.placeholder.PlaceholderAPI;

// In your extension class:
@Override
public void onInit() {
    PlaceholderAPI.register("myextension_playercount", (player) -> {
        return String.valueOf(ServerLifecycleHooks.getCurrentServer().getPlayerCount());
    });
}
```

### Event Listening

Listen to NeoEssentials events:

```java
import com.zerog.neoessentials.api.events.EconomyTransactionEvent;
import net.neoforged.bus.api.SubscribeEvent;

public class MyEventListener {
    @SubscribeEvent
    public void onEconomyTransaction(EconomyTransactionEvent event) {
        // React to economy transactions
    }
}

// Register your event listener:
@Override
public void onInit() {
    NeoEventBus.register(new MyEventListener());
}
```

### Config Integration

Create custom configurations:

```java
import com.zerog.neoessentials.api.config.ConfigManager;
import com.zerog.neoessentials.api.config.JsonConfig;

public class MyExtensionConfig extends JsonConfig {
    public boolean enableFeature = true;
    public int cooldownTime = 60;
    public List<String> enabledWorlds = new ArrayList<>();
    
    public MyExtensionConfig() {
        super("myextension");
    }
}

// In your extension class:
private MyExtensionConfig config;

@Override
public void onInit() {
    config = ConfigManager.register(new MyExtensionConfig());
}
```

## Advanced Extension Features

### Custom UIs

Create in-game GUIs that integrate with NeoEssentials:

```java
import com.zerog.neoessentials.api.ui.UIManager;

public void openMyCustomUI(Player player) {
    UIManager.openCustomMenu(player, "myextension.menu", (container) -> {
        // Build your custom menu here
    });
}
```

### Database Integration

Use the NeoEssentials database system:

```java
import com.zerog.neoessentials.api.storage.DatabaseManager;
import com.zerog.neoessentials.api.storage.StorageTable;

// Define your data model
StorageTable myTable = new StorageTable("myextension_data")
    .addColumn("player_uuid", "VARCHAR(36) PRIMARY KEY")
    .addColumn("custom_data", "TEXT");
    
// In your extension class:
@Override
public void onInit() {
    DatabaseManager.registerTable(myTable);
}

// Save data
DatabaseManager.executeUpdate(
    "INSERT INTO myextension_data (player_uuid, custom_data) VALUES (?, ?)",
    playerUUID.toString(),
    jsonData
);

// Query data
DatabaseManager.executeQuery(
    "SELECT custom_data FROM myextension_data WHERE player_uuid = ?",
    playerUUID.toString(),
    (resultSet) -> {
        if (resultSet.next()) {
            String data = resultSet.getString("custom_data");
            // Process data...
        }
    }
);
```

## Testing Your Extension

1. Build your extension using Gradle
2. Place the resulting JAR in the `mods` directory alongside NeoEssentials
3. Start the server and verify your extension loads correctly
4. Check the logs for any initialization messages or errors

## Distribution

When ready to distribute your extension:

1. Create clear documentation for users
2. Specify the required NeoEssentials version
3. Provide installation instructions
4. Consider publishing on platforms like CurseForge or Modrinth

## Best Practices

- Follow NeoEssentials API conventions and patterns
- Don't modify core NeoEssentials functionality directly
- Keep your extension focused on specific functionality
- Handle errors gracefully without crashing the server
- Use proper versioning (Semantic Versioning recommended)
- Test thoroughly with different configurations

## Related Documentation

- [API Documentation](API-Documentation)
- [Event System](Event-System)
- [Custom Placeholders](Custom-Placeholders)
- [Contributing Guide](Contributing-Guide)

---

*If you need further assistance with extension development, join our [Discord server](https://discord.gg/dUGAQF2Mga) or check the [GitHub repository](https://github.com/ZeroG-Network/NeoEssentials).*
