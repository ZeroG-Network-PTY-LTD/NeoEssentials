# NeoEssentials Dedicated Configuration Files

This document provides an overview of all dedicated configuration files available in NeoEssentials for fine-tuned customization of every feature.

## Available Configuration Files

### 1. TabListConfig.java
**Purpose:** Comprehensive player list (Tab) customization
**Location:** `com.zerog.neoessentials.config.dedicated.TabListConfig`

**Key Features:**
- Custom header and footer messages with placeholder support
- Player display formatting with rank, ping, health, balance display options
- Color schemes for different player types (admin, VIP, regular)
- Update intervals and animation settings
- Advanced sorting, filtering, and display options

**Placeholders Supported:**
- `{server_name}`, `{online_players}`, `{max_players}`, `{motd}` (header/footer)
- `{player_name}`, `{ping}`, `{health}`, `{balance}`, `{rank}` (player format)

### 2. EssentialsConfig.java
**Purpose:** Core command customization and behavior settings
**Location:** `com.zerog.neoessentials.config.dedicated.EssentialsConfig`

**Key Features:**
- Command aliases for all essential commands (heal, feed, fly, god, vanish, etc.)
- Default values and limits (fly speed, walk speed, item amounts)
- Feature enable/disable toggles for each command
- Individual cooldowns and costs per command
- Advanced behavior settings (heal removes effects, feed gives saturation, etc.)
- Permission exemptions for bypassing cooldowns and costs

### 3. GuiConfig.java
**Purpose:** Complete GUI system customization
**Location:** `com.zerog.neoessentials.config.dedicated.GuiConfig`

**Key Features:**
- Menu layout settings (inventory rows, title formats, sounds)
- Color schemes and styling for all GUI elements
- Shop GUI settings (categories, pricing display, items per page)
- Kit GUI configuration (previews, cooldowns, cost display)
- Warp GUI options (categories, descriptions, pagination)
- Player stats GUI customization
- Navigation settings (back buttons, page controls)
- Animation and effect controls

### 4. DiscordConfig.java
**Purpose:** Comprehensive Discord webhook integration
**Location:** `com.zerog.neoessentials.config.dedicated.DiscordConfig`

**Key Features:**
- Main integration settings (webhook URL, bot name, avatar)
- Customizable message templates for all events
- Moderation action notification templates
- Event notification enable/disable toggles
- Rich embed customization (colors, footers, thumbnails)
- Server statistics and economy reporting
- Security settings for command execution
- Role-based access controls

**Supported Events:**
- Server start/stop, player join/leave, deaths, advancements
- Ban, unban, kick, mute, unmute, tempban actions
- Admin command logging and custom notifications

### 5. EconomyConfig.java
**Purpose:** Complete economy system configuration
**Location:** `com.zerog.neoessentials.config.dedicated.EconomyConfig`

**Key Features:**
- Main economy settings (starting balance, limits, negative balance)
- Currency display customization (symbol, names, decimal places)
- Transaction settings (limits, fees, percentage/flat rates)
- Banking system (interest rates, intervals, minimums)
- Shop integration (tax settings, server accounts)
- Pay command configuration (limits, cooldowns, confirmations)
- Baltop settings (size, caching, real-time updates)
- Comprehensive logging and analytics options
- Admin tool configurations and bypass settings
- Integration controls for warps, kits, commands, shops

### 6. TeleportationConfig.java
**Purpose:** All teleportation system settings
**Location:** `com.zerog.neoessentials.config.dedicated.TeleportationConfig`

**Key Features:**
- Home system (max homes, cooldowns, costs, delays)
- Warp system (cooldowns, costs, categories, delays)
- TPA system (timeouts, cooldowns, costs, concurrent requests)
- Spawn system (cooldowns, costs, first join/death behavior)
- Back system (cooldowns, costs, location history)
- Cross-dimension settings (allowances, cost multipliers)
- Safety and validation (safe teleport, combat prevention)
- Movement cancellation (thresholds, damage cancellation)
- Permission integration (limits, bypass permissions)
- Visual and audio effects (particles, sounds, warmup effects)

## Configuration Categories

### Core Systems
- **TabListConfig:** Player list appearance and behavior
- **EssentialsConfig:** Essential command behavior and limits

### User Interface
- **GuiConfig:** All graphical interface customization

### External Integration
- **DiscordConfig:** Discord webhook integration and notifications

### Game Systems
- **EconomyConfig:** Financial system and transaction management
- **TeleportationConfig:** All movement and teleportation features

## Implementation Notes

- All config files use NeoForge's `ModConfigSpec` system for validation
- Each config includes comprehensive comments explaining all options
- Default values are set for production-ready deployment
- All configs support enable/disable toggles for features
- Permission integration available where applicable
- Placeholder support for dynamic content where relevant

## Usage

These configuration files provide server administrators with granular control over every aspect of NeoEssentials. Each file focuses on a specific system, making it easy to customize individual features without affecting others.

To use these configs, server administrators can modify the generated TOML files in the config directory, and changes will be automatically loaded by the NeoEssentials configuration system.
