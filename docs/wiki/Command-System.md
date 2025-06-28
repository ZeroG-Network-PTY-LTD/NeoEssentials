# NeoEssentials Command System

The Command System in NeoEssentials provides a comprehensive framework for managing and executing commands on your Minecraft server.

![Commands Icon](../images/icons/commands.png)

## Overview

NeoEssentials offers a robust command system that allows server administrators to:

- Execute essential server management operations
- Provide useful utilities for players
- Customize command behavior and permissions
- Create command aliases and shortcuts
- Integrate with other mod systems

## Key Features

### Command Categories

NeoEssentials organizes commands into the following categories:

| Category | Description | Example Commands |
|----------|-------------|-----------------|
| **Administrative** | Server management commands | `/ne reload`, `/ne debug` |
| **Economy** | Money and transaction commands | `/money`, `/pay`, `/baltop` |
| **Teleportation** | Movement and location commands | `/home`, `/warp`, `/spawn`, `/tpa` |
| **Player** | Player-focused utility commands | `/nick`, `/msg`, `/help` |
| **World** | World management commands | `/time`, `/weather`, `/gamemode` |
| **Utility** | Miscellaneous helpful commands | `/afk`, `/hat`, `/ping` |

### Command Architecture

The NeoEssentials command system uses a hierarchical structure:

1. **Base Commands**: Root-level commands (e.g., `/ne`, `/money`)
2. **Subcommands**: Commands under a base command (e.g., `/ne reload`, `/money pay`)
3. **Arguments**: Parameters that modify command behavior (e.g., `/warp create <name>`)

### Custom Command Support

Server administrators can create custom commands through configuration:

```json
{
  "commands": {
    "customcmd": {
      "permission": "neoessentials.command.custom",
      "aliases": ["cc", "customcommand"],
      "action": "execute_commands",
      "commands": [
        "say Custom command executed!",
        "give %player% minecraft:diamond 1"
      ]
    }
  }
}
```

## Command Configuration

### Global Command Settings

You can configure global command settings in `config/neoessentials/commands.json`:

```json
{
  "settings": {
    "enable_command_cooldowns": true,
    "default_cooldown": 3,
    "enable_command_costs": false,
    "command_prefix": "ne"
  }
}
```

### Command-Specific Configuration

Individual commands can be configured with:

- **Aliases**: Alternative command names
- **Permissions**: Required permissions to use the command
- **Cooldowns**: Time between command uses
- **Cost**: Economy cost to use the command
- **Description**: Help text for the command

## Command Registration API

Developers can register custom commands using the API:

```java
import com.zerog.neoessentials.api.command.CommandAPI;

public class MyMod {
    public void registerCommands() {
        CommandAPI.register("mycommand", new MyCustomCommand());
    }
}
```

## Common Command Issues

| Issue | Solution |
|-------|----------|
| Command not found | Ensure the command is enabled in configuration |
| Permission denied | Check player has the required permission |
| Command cooldown | Wait for cooldown to expire |
| Command cost error | Ensure player has sufficient balance |

## Related Documentation

- [Commands Reference](Commands-Reference) - Full list of all available commands
- [Permissions Guide](Permissions-Guide) - Command permission setup
- [API Documentation](API-Documentation) - Developing with the command API

---

*For additional help with the command system, join our [Discord server](https://discord.gg/dUGAQF2Mga) or check the [GitHub repository](https://github.com/ZeroG-Network/NeoEssentials).*
