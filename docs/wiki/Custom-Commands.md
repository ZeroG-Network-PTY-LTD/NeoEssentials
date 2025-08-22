## NeoEssentials Custom Command System

NeoEssentials provides a powerful, config-driven command system supporting over 100 built-in and custom commands for server administration, player management, economy, teleportation, and more.

---

### 🗂️ Command Categories & Examples

#### Essentials & Utility
- `/heal [player]` — Heal yourself or another player
- `/feed [player]` — Feed yourself or another player
- `/god [player]` — Toggle god mode
- `/vanish [player]` — Toggle invisibility
- `/fly [player]` — Toggle flight mode
- `/speed <walk|fly> <speed> [player]` — Set movement speed
- `/gamemode <mode> [player]` — Change game mode
- `/give <item> [amount] [player]` — Give items
- `/workbench` — Open crafting table
- `/anvil` — Open anvil interface
- `/enderchest` — Open ender chest
- `/invsee <player>` — View player inventory
- `/repair [all]` — Repair items

#### Teleportation & Movement
- `/tp <player>` — Teleport to a player
- `/tpa <player>` — Request teleportation to a player
- `/tpaccept` — Accept teleport request
- `/tpdeny` — Deny teleport request
- `/back` — Return to previous location
- `/spawn` — Teleport to spawn
- `/home [name]` — Teleport to home
- `/sethome [name]` — Set home location
- `/delhome [name]` — Delete home
- `/warp <name>` — Teleport to warp
- `/setwarp <name>` — Create warp location
- `/delwarp <name>` — Delete warp

#### Economy & Shops
- `/balance [player]` — Check balance
- `/pay <player> <amount>` — Pay another player
- `/economy <give|take|set> <player> <amount>` — Admin economy management
- `/shop` — Access shop system
- `/createshop` — Create a shop

#### Messaging & Social
- `/msg <player> <message>` — Private message
- `/reply <message>` — Reply to last message
- `/mail <send|read|clear>` — Mail system
- `/ignore <player>` — Ignore player
- `/mute <player> [duration] [reason]` — Mute player
- `/unmute <player>` — Unmute player
- `/socialspy` — Monitor private messages

#### Moderation & Admin
- `/kick <player> [reason]` — Kick player
- `/ban <player> [reason]` — Ban player
- `/tempban <player> <duration> [reason]` — Temporary ban
- `/whois <player>` — Player information
- `/seen <player>` — Last seen information
- `/list` — List online players
- `/notification` — Admin notifications
- `/config <reload|get|set>` — Configuration management
- `/reload` — Reload config
- `/performance` — Show performance info
- `/placeholders` — List available placeholders

#### Kits & Items
- `/kit <name>` — Get a kit
- `/kits` — List available kits
- `/item` — Admin item command

#### World & Time
- `/time <set|add> <value>` — Time management
- `/weather <clear|rain|thunder>` — Weather control

#### Interface & GUI
- `/menu` — Open main menu
- `/stats` — Show player stats
- `/smithing` — Open smithing table
- `/stonecutter` — Open stonecutter

#### Text & Info
- `/motd` — Message of the day
- `/rules` — Server rules
- `/help [command]` — Command help

#### Discord Integration
- `/discord` — Discord info/menu
- `/discord link` — Link account
- `/discord broadcast` — Broadcast to Discord

#### FTB Integration (if mods installed)
- `/teaminfo` — Show FTB team info
- `/checkrank <permission>` — Check FTB rank permission

---

### 🛠️ Custom Command Features

- **Config-driven**: All commands, permissions, cooldowns, and aliases are managed via `config/neoessentials/main.json`.
- **Localization**: All messages are managed by the lang file for full translation support.
- **Permission System**: Hierarchical permission nodes (e.g., `neoessentials.<category>.<command>`) for granular control.
- **Aliases**: Many commands support aliases (e.g., `/gm` for `/gamemode`).
- **Cooldowns**: Per-command cooldowns configurable in the main config.
- **Enable/Disable**: Commands can be enabled or disabled via config.
- **Soft Dependencies**: Optional integration with mods like FTB Teams/Ranks and SDLink.

---

### 🔧 Usage Examples

```bash
# Heal yourself
/heal

# Teleport to a player
/tp Steve

# Pay another player
/pay Steve 100

# Send a private message
/msg Steve Hello!

# Set home
/sethome base

# Open shop menu
/shop
```

---

### 📚 Related Documentation

- [Essential Commands](Essential-Commands.md)
- [Permissions](Permissions.md)
- [Configuration](Configuration.md)
- [GUI System](GUI-System.md)
- [Placeholders](Placeholders.md)

---

*Last Updated: August 22, 2025*
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
