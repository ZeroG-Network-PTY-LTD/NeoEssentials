# Command System

NeoEssentials provides a comprehensive command system with over 100 built-in commands for server administration, player management, economy, teleportation, and more.

## 🚀 Command System Overview

### Built-in Commands
NeoEssentials includes a wide range of pre-built commands organized into categories:

#### Essential Utility Commands
- `/heal [player]` - Heal yourself or another player
- `/feed [player]` - Feed yourself or another player
- `/god [player]` - Toggle god mode
- `/vanish [player]` - Toggle visibility
- `/fly [player]` - Toggle flight mode
- `/speed <walk|fly> <speed> [player]` - Set movement speed
- `/gamemode <mode> [player]` - Change game mode

#### Teleportation Commands
- `/tp <player>` - Teleport to a player
- `/tpa <player>` - Request teleportation to a player
- `/tpaccept` - Accept teleport request
- `/tpdeny` - Deny teleport request
- `/back` - Return to previous location
- `/spawn` - Teleport to spawn
- `/home [name]` - Teleport to home
- `/sethome [name]` - Set home location
- `/warp <name>` - Teleport to warp
- `/setwarp <name>` - Create warp location

#### Economy Commands
- `/balance [player]` - Check balance
- `/pay <player> <amount>` - Pay another player
- `/economy <give|take|set> <player> <amount>` - Admin economy management
- `/shop` - Access shop system

#### Communication Commands
- `/msg <player> <message>` - Private message
- `/reply <message>` - Reply to last message
- `/mail <send|read|clear>` - Mail system
- `/broadcast <message>` - Server-wide message

#### Moderation Commands
- `/kick <player> [reason]` - Kick player
- `/ban <player> [reason]` - Ban player
- `/tempban <player> <duration> [reason]` - Temporary ban
- `/mute <player> [duration] [reason]` - Mute player
- `/whois <player>` - Player information
- `/seen <player>` - Last seen information

### Command Registration
Commands are registered through the centralized `CommandRegistry` system during server startup.

## 📋 Complete Command List

### Administrative Commands
- `/neoessentials <reload|version|status>` - Main admin command
- `/permissions <info|group|user|reload|stats>` - Permission management
- `/config <reload|get|set>` - Configuration management
- `/theme <tablist|scoreboard|bossbar> <theme>` - Theme management
- `/security <status|audit|config>` - Security management

### Player Management Commands
- `/whois <player>` - Detailed player information
- `/seen <player>` - Last seen information
- `/list` - List online players
- `/playtime [player]` - Check playtime
- `/nick <nickname>` - Set nickname
- `/preferences <setting> <value>` - Player preferences

### Utility Commands
- `/workbench` - Open crafting table
- `/anvil` - Open anvil interface
- `/enderchest` - Open ender chest
- `/invsee <player>` - View player inventory
- `/repair [all]` - Repair items
- `/give <item> [amount] [player]` - Give items
- `/time <set|add> <value>` - Time management
- `/weather <clear|rain|thunder>` - Weather control

### Information Commands
- `/help [command]` - Command help
- `/info` - Server information
- `/motd` - Message of the day
- `/rules` - Server rules

### Bossbar Commands
- `/bossbar show <template> [player] [duration]` - Show bossbar
- `/bossbar hide [player]` - Hide bossbar
- `/bossbar broadcast <template> <duration>` - Broadcast bossbar
- `/bossbar templates` - List templates
- `/bossbar update <text> <progress> [player]` - Update bossbar

### Kit System
- `/kit <name>` - Get a kit
- `/kit list` - List available kits
- `/kit preview <name>` - Preview kit contents

### Animation Commands
- `/animation <play|stop|list> [name] [player]` - Animation management
- `/animation reload` - Reload animations
- `/animation stats` - Animation statistics

## 🛠️ Command Permissions

### Permission Structure
Commands use a hierarchical permission system:
```
neoessentials.<category>.<command>[.<subcategory>]
```

### Basic Permission Groups
- `neoessentials.user.*` - Basic user commands
- `neoessentials.moderator.*` - Moderation commands  
- `neoessentials.admin.*` - Administrative commands
- `neoessentials.*` - All permissions

### Specific Permissions
- `neoessentials.heal.self` - Heal yourself
- `neoessentials.heal.others` - Heal other players
- `neoessentials.teleport.tp` - Basic teleportation
- `neoessentials.economy.admin` - Economy administration
- `neoessentials.moderation.basic` - Basic moderation
- `neoessentials.bossbar.show` - Show bossbars

## 🔧 Command Usage Examples

### Basic Player Commands
```bash
# Heal yourself
/heal

# Heal another player  
/heal Steve

# Set your nickname
/nick "§6Golden§fPlayer"

# Check your balance
/balance

# Send a private message
/msg Steve Hello there!
```

### Administrative Commands
```bash
# Give items to a player
/give Steve minecraft:diamond 64

# Set server time to day
/time set day

# Teleport to a player
/tp Steve

# Create a warp point
/setwarp mall

# Show server-wide bossbar
/bossbar broadcast welcome 30
```

### Moderation Commands
```bash
# Check player information
/whois Steve

# Temporary ban for 1 hour
/tempban Steve 1h Griefing

# Mute player for 30 minutes
/mute Steve 30m Spam

# View player's last login
/seen Steve
```

## 📊 Command Categories

### Essential Commands (20+ commands)
Core server functionality including heal, feed, god mode, vanish, fly, speed, and gamemode commands.

### Teleportation Commands (15+ commands)  
Complete teleportation system with homes, warps, spawn, TPA requests, and back functionality.

### Economy Commands (10+ commands)
Full economy system with balance management, payments, shops, and administrative tools.

### Moderation Commands (15+ commands)
Comprehensive moderation tools including bans, kicks, mutes, and player information commands.

### Communication Commands (8+ commands)
Messaging system with private messages, mail, replies, and server-wide broadcasts.

### Utility Commands (25+ commands)
Various utility commands for inventory management, crafting interfaces, item giving, and server management.

### Information Commands (10+ commands)
Commands for displaying server information, rules, help, and player statistics.

### Administrative Commands (20+ commands)
Advanced administrative tools for server configuration, permissions, themes, and system management.

## 🎯 Command Aliases

Many commands include convenient aliases:
- `/gm` = `/gamemode`
- `/gmc` = `/gamemode creative`
- `/gms` = `/gamemode survival`
- `/gma` = `/gamemode adventure` 
- `/gmsp` = `/gamemode spectator`
- `/tpa` = teleport request
- `/tpaccept` = accept teleport
- `/tpdeny` = deny teleport
- `/msg` = `/message`
- `/r` = `/reply`

## 🔍 Command Help System

### Getting Help
- `/help` - List all available commands
- `/help <command>` - Get specific command help
- `/help <category>` - Get category-specific help

### Command Information
Each command provides:
- Usage syntax
- Parameter descriptions
- Permission requirements
- Example usage
- Related commands

## ⚙️ Command Configuration

Commands are managed through the NeoEssentials configuration system:
- **Command enabling/disabling**: Control which commands are available
- **Permission requirements**: Set required permissions for commands
- **Cooldowns**: Configure command cooldowns
- **Aliases**: Define custom command aliases

**Configuration Location**: `config/neoessentials/main.json`

## 🚫 Disabled Commands

Some commands may be temporarily disabled:
- Commands requiring missing dependencies
- Commands with compilation issues
- Experimental or unstable commands

Check the console logs during server startup for information about disabled commands.

---

**Related Documentation**: [Permissions](Permissions.md) | [Configuration](Configuration.md) | [Essential Commands](Essential-Commands.md)

*Last Updated: August 8, 2025*
- `/feed [player]` - Feed yourself or another player
- `/god [player]` - Toggle god mode
- `/vanish [player]` - Toggle visibility
- `/fly [player]` - Toggle flight mode
- `/speed <walk|fly> <speed> [player]` - Set movement speed
- `/gamemode <mode> [player]` - Change game mode

#### Teleportation Commands
- `/tp <player>` - Teleport to a player
- `/tpa <player>` - Request teleportation to a player
- `/tpaccept` - Accept teleport request
- `/tpdeny` - Deny teleport request
- `/back` - Return to previous location
- `/spawn` - Teleport to spawn
- `/home [name]` - Teleport to home
- `/sethome [name]` - Set home location
- `/warp <name>` - Teleport to warp
- `/setwarp <name>` - Create warp location

#### Economy Commands
- `/balance [player]` - Check balance
- `/pay <player> <amount>` - Pay another player
- `/economy <give|take|set> <player> <amount>` - Admin economy management
- `/shop` - Access shop system

#### Communication Commands
- `/msg <player> <message>` - Private message
- `/reply <message>` - Reply to last message
- `/mail <send|read|clear>` - Mail system
- `/broadcast <message>` - Server-wide message

#### Moderation Commands
- `/kick <player> [reason]` - Kick player
- `/ban <player> [reason]` - Ban player
- `/tempban <player> <duration> [reason]` - Temporary ban
- `/mute <player> [duration] [reason]` - Mute player
- `/whois <player>` - Player information
- `/seen <player>` - Last seen information

### Command Registration
Commands are registered through the centralized `CommandRegistry` system during server startup.
      description: "The player to give the kit to"
      
    - name: "kit_name"
      type: "string"
      required: true
      description: "The name of the kit"
      validation: "^[a-zA-Z0-9_]+$"
  
  actions:
    - type: "validate_kit"
      kit: "{kit_name}"
      
    - type: "give_kit"
      target: "{target_player}"
      kit: "{kit_name}"
      
    - type: "message"
      target: "{player}"
      message: "§aGave kit '{kit_name}' to {target_player}!"
      
    - type: "log"
      message: "{player} gave kit {kit_name} to {target_player}"
```

## 🔧 Advanced Command Features

### Conditional Logic
Implement if/else logic in commands:

```yaml
# smart_heal.yml
command:
  name: "smartheal"
  description: "Intelligent healing based on player status"
  permission: "essentials.smartheal"
  
  actions:
    - type: "conditional"
      condition: "{player_health} < 10"
      then:
        - type: "heal"
          target: "{player}"
          amount: "full"
        - type: "message"
          target: "{player}"
          message: "§cYou were critically injured! Fully healed."
      else_if:
        - condition: "{player_health} < 15"
          then:
            - type: "heal"
              target: "{player}"
              amount: 10
            - type: "message"
              target: "{player}"
              message: "§eHealed 10 health points."
      else:
        - type: "message"
          target: "{player}"
          message: "§aYou're already healthy!"
```

### Loop Actions
Repeat actions multiple times:

```yaml
# rain_diamonds.yml
command:
  name: "raindiamonds"
  description: "Rain diamonds from the sky!"
  permission: "admin.raindiamonds"
  
  actions:
    - type: "loop"
      iterations: 10
      delay: 500  # milliseconds between iterations
      actions:
        - type: "spawn_item"
          location: "{player_location_above_10}"
          item: "minecraft:diamond"
          random_offset: 5
          
        - type: "particle"
          location: "{player_location_above_10}"
          effect: "firework"
          count: 5
```

### GUI Integration
Create commands that open custom GUIs:

```yaml
# player_manager.yml
command:
  name: "playermgr"
  description: "Open player management interface"
  permission: "admin.playermanager"
  
  actions:
    - type: "open_custom_gui"
      target: "{player}"
      gui_config:
        title: "§cPlayer Management"
        size: 54
        items:
          - slot: 10
            item: "minecraft:player_head"
            name: "§6Online Players"
            lore:
              - "§7Click to view online players"
            action:
              type: "open_gui"
              gui: "online_players"
              
          - slot: 12
            item: "minecraft:book"
            name: "§eBan Management" 
            lore:
              - "§7Manage banned players"
            action:
              type: "open_gui"
              gui: "ban_management"
```

## 📜 Scripted Commands

### JavaScript Commands
Use JavaScript for complex logic:

```yaml
# advanced_economy.yml
command:
  name: "economystats"
  description: "Show advanced economy statistics"
  permission: "admin.economystats"
  
  script:
    language: "javascript"
    code: |
      // Get economy data
      var totalMoney = getServerTotalMoney();
      var averageBalance = getAveragePlayerBalance();
      var richestPlayer = getRichestPlayer();
      var transactions = getTodayTransactions();
      
      // Format messages
      var messages = [
        "§6=== Economy Statistics ===",
        "§eTotalMoney in circulation: §f$" + formatNumber(totalMoney),
        "§eAverage player balance: §f$" + formatNumber(averageBalance),
        "§eRichest player: §f" + richestPlayer.name + " §7($" + formatNumber(richestPlayer.balance) + ")",
        "§eTransactions today: §f" + transactions
      ];
      
      // Send to player
      for (var i = 0; i < messages.length; i++) {
        sendMessage(player, messages[i]);
      }
```

### Lua Commands
Alternative scripting with Lua:

```yaml
# world_info.yml
command:
  name: "worldinfo"
  description: "Display detailed world information"
  permission: "essentials.worldinfo"
  
  script:
    language: "lua"
    code: |
      -- Get world data
      local world_name = getPlayerWorld(player)
      local world_time = getWorldTime(world_name)
      local weather = getWorldWeather(world_name)
      local player_count = getWorldPlayerCount(world_name)
      
      -- Format time
      local formatted_time = ""
      if world_time >= 0 and world_time < 6000 then
        formatted_time = "Morning"
      elseif world_time >= 6000 and world_time < 12000 then
        formatted_time = "Day"
      elseif world_time >= 12000 and world_time < 18000 then
        formatted_time = "Evening"
      else
        formatted_time = "Night"
      end
      
      -- Send information
      sendMessage(player, "§6=== World Information ===")
      sendMessage(player, "§eWorld: §f" .. world_name)
      sendMessage(player, "§eTime: §f" .. formatted_time .. " §7(" .. world_time .. ")")
      sendMessage(player, "§eWeather: §f" .. weather)
      sendMessage(player, "§ePlayers: §f" .. player_count)
```

## 🎯 Command Action Types

### Core Actions
Built-in action types available for custom commands:

#### Player Actions
```yaml
# Player-related actions
- type: "heal"
  target: "{player}"
  amount: 20  # or "full"

- type: "feed"
  target: "{player}"

- type: "teleport"
  target: "{player}"
  location: "spawn"  # or coordinates

- type: "give_item"
  target: "{player}"
  item: "minecraft:diamond_sword"
  amount: 1
  nbt: "{Enchantments:[{id:sharpness,lvl:5}]}"

- type: "set_gamemode"
  target: "{player}"
  gamemode: "creative"
```

#### Economy Actions
```yaml
# Economy-related actions
- type: "give_money"
  target: "{player}"
  amount: 1000

- type: "take_money"
  target: "{player}"
  amount: 500

- type: "set_balance"
  target: "{player}"
  amount: 10000

- type: "shop_transaction"
  target: "{player}"
  item: "diamond"
  amount: 10
  type: "buy"  # or "sell"
```

#### Communication Actions
```yaml
# Communication actions
- type: "message"
  target: "{player}"
  message: "Hello, {player}!"

- type: "broadcast"
  message: "Server announcement!"

- type: "title"
  target: "{player}"
  title: "Welcome!"
  subtitle: "Enjoy your stay"

- type: "actionbar"
  target: "{player}"
  message: "Action bar message"

- type: "sound"
  target: "{player}"
  sound: "entity.player.levelup"
  volume: 1.0
  pitch: 1.0
```

#### Server Actions
```yaml
# Server management actions
- type: "command"
  command: "time set day"
  as_console: true

- type: "log"
  message: "Custom command executed by {player}"
  level: "INFO"

- type: "delay"
  duration: 3000  # milliseconds

- type: "conditional"
  condition: "{player_balance} > 1000"
  then: [...]
  else: [...]
```

## 🔧 Command Parameters and Validation

### Parameter Types
Define and validate command parameters:

```yaml
parameters:
  - name: "target_player"
    type: "player"
    required: true
    description: "The target player"
    validation:
      online_only: true
      
  - name: "amount"
    type: "integer"
    required: true
    description: "Amount of money"
    validation:
      min: 1
      max: 100000
      
  - name: "item_name"
    type: "string"
    required: false
    default: "diamond"
    description: "Item to give"
    validation:
      pattern: "^[a-z_]+$"
      allowed_values: ["diamond", "emerald", "gold_ingot"]
      
  - name: "coordinates"
    type: "location"
    required: false
    description: "Target coordinates"
    validation:
      world_only: "world"
      y_min: 0
      y_max: 256
```

### Advanced Validation
Complex parameter validation:

```yaml
parameters:
  - name: "player_or_group"
    type: "mixed"
    required: true
    description: "Player name or group (@group_name)"
    validation:
      custom_validator: |
        if (value.startsWith("@")) {
          var group = value.substring(1);
          return isValidGroup(group);
        } else {
          return isValidPlayer(value);
        }
```

## 🎮 Interactive Commands

### Multi-step Commands
Commands that require multiple inputs:

```yaml
# setup_home.yml
command:
  name: "setuphome"
  description: "Interactive home setup"
  permission: "essentials.sethome"
  
  steps:
    - step: "name_input"
      message: "§eEnter a name for your home:"
      input_type: "chat"
      validation: "^[a-zA-Z0-9_]{1,20}$"
      
    - step: "confirm_location"
      message: "§eSet home at your current location? §a[Yes] §c[No]"
      input_type: "chat_confirmation"
      
    - step: "execute"
      actions:
        - type: "set_home"
          target: "{player}"
          name: "{name_input}"
          location: "{player_location}"
        - type: "message"
          target: "{player}"
          message: "§aHome '{name_input}' set successfully!"
```

### Conversation Commands
Commands that maintain context across multiple interactions:

```yaml
# support_ticket.yml
command:
  name: "support"
  description: "Create a support ticket"
  permission: "essentials.support"
  
  conversation:
    - prompt: "§eWhat type of issue are you experiencing?"
      options:
        - "Technical Problem"
        - "Player Report" 
        - "General Question"
        - "Bug Report"
      variable: "issue_type"
      
    - prompt: "§ePlease describe your issue in detail:"
      input_type: "chat"
      max_length: 500
      variable: "description"
      
    - prompt: "§eHow urgent is this issue?"
      options:
        - "Low - Can wait"
        - "Medium - Soon as possible"
        - "High - Urgent"
        - "Critical - Game breaking"
      variable: "priority"
      
    - execute:
        - type: "create_ticket"
          player: "{player}"
          type: "{issue_type}"
          description: "{description}"
          priority: "{priority}"
        - type: "message"
          target: "{player}"
          message: "§aSupport ticket created! ID: {ticket_id}"
```

## 📊 Command Management

### Command Administration
Manage custom commands from in-game:

```bash
/customcommand list                    # List all custom commands
/customcommand info <command>          # Show command details
/customcommand reload                  # Reload all custom commands
/customcommand reload <command>        # Reload specific command
/customcommand enable <command>        # Enable command
/customcommand disable <command>       # Disable command
/customcommand test <command> [args]   # Test command execution
```

### Command Analytics
Track command usage and performance:

```bash
/customcommand stats                   # Command usage statistics
/customcommand performance             # Performance metrics
/customcommand errors                  # Recent command errors
/customcommand logs <command>          # Command execution logs
```

### Command Sharing
Share commands with other servers:

```bash
/customcommand export <command>        # Export command definition
/customcommand import <file>           # Import command from file
/customcommand publish <command>       # Publish to command repository
/customcommand browse                  # Browse command repository
```

## 🔒 Security and Permissions

### Permission System
Secure custom commands with permissions:

```yaml
command:
  name: "dangerouscommand"
  description: "A potentially dangerous command"
  
  permissions:
    base: "admin.dangerous"              # Basic permission
    parameters:
      target_player: "admin.target"     # Permission to target others
      amount: "admin.money"              # Permission for money amounts
      
  restrictions:
    max_uses_per_hour: 5                # Rate limiting
    require_confirmation: true          # Require confirmation
    admin_notification: true           # Notify admins when used
    
  security:
    validate_inputs: true               # Validate all inputs
    prevent_exploits: true              # Prevent known exploits
    audit_log: true                     # Log all executions
```

### Safe Execution
Protect against malicious commands:

```yaml
security:
  sandbox_mode: true                    # Run in sandboxed environment
  timeout: 30000                        # Maximum execution time (ms)
  max_actions: 100                      # Maximum actions per command
  prevent_recursion: true               # Prevent infinite loops
  whitelist_only: false                 # Only allow whitelisted actions
  
  allowed_actions:                      # Whitelist specific actions
    - "message"
    - "heal"
    - "feed"
    - "teleport"
  
  blocked_actions:                      # Blacklist dangerous actions
    - "command:op"
    - "command:stop"
    - "file_access"
```

## 🛠️ Debugging Custom Commands

### Debug Tools
Debug and troubleshoot custom commands:

```bash
/customcommand debug <command>         # Debug command execution
/customcommand trace <command>         # Trace command flow
/customcommand validate <command>      # Validate command syntax
/customcommand test dry-run <command>  # Dry run without execution
```

### Error Handling
Handle errors gracefully in commands:

```yaml
command:
  name: "robust_command"
  description: "Command with error handling"
  
  error_handling:
    on_error: "continue"                # continue, stop, rollback
    log_errors: true
    notify_player: true
    fallback_actions:
      - type: "message"
        target: "{player}"
        message: "§cCommand failed, but don't worry!"
        
  actions:
    - type: "try"
      actions:
        - type: "risky_action"
          target: "{player}"
      catch:
        - type: "message"
          target: "{player}"
          message: "§eRisky action failed, trying alternative..."
        - type: "safe_alternative"
          target: "{player}"
```

---

## 📚 Related Documentation

- **[Essential Commands](Essential-Commands.md)** - Built-in command reference
- **[Permissions](Permissions.md)** - Permission system setup
- **[GUI System](GUI-System.md)** - GUI integration for commands
- **[Placeholders](Placeholders.md)** - Using placeholders in commands

*Last Updated: August 6, 2025*
