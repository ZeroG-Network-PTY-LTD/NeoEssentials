

# NeoEssentials Bossbar System

This document describes the actual Bossbar system as implemented in NeoEssentials. All features, commands, permissions, configuration, and API details below are strictly based on the mod's codebase.

---

## Features
- Show/hide bossbars for individual players
- Broadcast bossbars to all players
- Update bossbar text and progress
- List available bossbar templates
- Create simple custom bossbars (runtime only)
- All bossbar messages are lang-managed and support color codes

---

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/bossbar show <template> [player] [duration]` | Show bossbar to self, other player, or for a set duration | `neoessentials.bossbar.show`, `neoessentials.bossbar.show.others` |
| `/bossbar hide [player]` | Hide bossbar for self or other player | `neoessentials.bossbar.hide` |
| `/bossbar update <text> <progress> [player]` | Update bossbar text/progress for self or other player | `neoessentials.bossbar.update` |
| `/bossbar broadcast <template> <duration>` | Broadcast bossbar to all players | `neoessentials.bossbar.broadcast` |
| `/bossbar announce <template> <duration>` | Alias for broadcast | `neoessentials.bossbar.broadcast` |
| `/bossbar templates` | List available bossbar templates | `neoessentials.bossbar.templates` |
| `/bossbar create <text>` | Create a simple custom bossbar for yourself | `neoessentials.bossbar.create` |

---

## Permissions

All bossbar features require `neoessentials.moderation.basic` as a base permission. Specific permissions:

- `neoessentials.bossbar.show` — Show bossbar to self
- `neoessentials.bossbar.show.others` — Show bossbar to other players
- `neoessentials.bossbar.hide` — Hide bossbar
- `neoessentials.bossbar.broadcast` — Broadcast bossbar to all
- `neoessentials.bossbar.create` — Create custom bossbar
- `neoessentials.bossbar.update` — Update bossbar text/progress
- `neoessentials.bossbar.templates` — List templates
- `neoessentials.bossbar.*` — All bossbar features

---

## Bossbar Templates

- Templates are managed in-memory and referenced by name (e.g. "welcome", "event").
- Template names are listed via `/bossbar templates`.
- Each template has: name, text, color (int), style (int).
- Colors and styles use Minecraft's BossBarColor and BossBarOverlay enums:
	- Colors: `PINK`, `BLUE`, `RED`, `GREEN`, `YELLOW`, `PURPLE`, `WHITE`
	- Styles: `PROGRESS`, `NOTCHED_6`, `NOTCHED_10`, `NOTCHED_12`, `NOTCHED_20`
- Templates are not currently loaded from config files; they are managed by the mod at runtime.
- Custom templates can be created at runtime via `/bossbar create <text>` (uses default color/style).

---

## API Usage

```java
CustomBossbarManager bossbars = CustomBossbarManager.getInstance();
bossbars.showBossbar(player, "welcome", 10); // Show template bossbar for 10 seconds
bossbars.updateBossbar(player, "Server restarting soon!", 0.5f); // Update text/progress
bossbars.removeBossbar(player); // Hide bossbar
bossbars.broadcastBossbar("event", 30); // Broadcast template bossbar to all
```

---

## Placeholders

Bossbar text supports placeholders via the PlaceholderManager. Supported placeholders include:

- Player: `{player}`, `{displayname}`, `{uuid}`, `{ping}`
- Location: `{world}`, `{x}`, `{y}`, `{z}`
- Server: `{server_name}`, `{server_players}`, `{server_max_players}`
- Time: `{time}`, `{date}`
- Economy, homes, kits, warps: `{balance}`, `{homes_count}`, `{kits_available}`, `{warps_count}`

Placeholders use `{name}` format and are replaced at runtime.

---

## Configuration

- Bossbar system is enabled/disabled via `main.json` (`bossbarSystem: true/false`).
- Templates are managed in-memory; config file loading is not currently implemented.
- All bossbar messages are lang-managed and support color codes (&a, &#FFFFFF, etc.).

---

## Troubleshooting

- Bossbar not showing: check permissions, template name, and if bossbar system is enabled in config.
- Placeholders not working: check placeholder syntax and mod version.
- Bossbar not removed: use `/bossbar hide` or wait for duration to expire.
- Template not found: use `/bossbar templates` to list available names.

---

## Usage Examples

```bash
# Show welcome bossbar to self
/bossbar show welcome

# Show event bossbar to another player for 30 seconds
/bossbar show event PlayerName 30

# Broadcast warning bossbar to all for 60 seconds
/bossbar broadcast warning 60

# Update bossbar text and progress
/bossbar update "Server restarting soon!" 0.5

# Hide bossbar
/bossbar hide

# List available templates
/bossbar templates

# Create a custom bossbar
/bossbar create "Custom message here"
```

---

## Best Practices

- Use concise text and color codes for clarity
- Limit bossbar duration to avoid spam
- Use placeholders for dynamic info
- Always check available templates before using

---

**Related Docs:** [Placeholders](Placeholders.md) | [Configuration](Configuration.md) | [Essential Commands](Essential-Commands.md)

*Last Updated: August 22, 2025*
