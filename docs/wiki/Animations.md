
# NeoEssentials Animation System

## Overview
Create dynamic, animated placeholders for tablist, scoreboard, and bossbar using config-driven, EssentialsX-style setup. All messages and labels are managed via the lang file for full localization support.

## Features
- Multiple animation types: text cycle, color cycle, progress bar, conditional, health bar, weather, typewriter, gradient, wave, blink, and more
- Works in tablist, scoreboard, bossbar, and any theme supporting placeholders
- Fully configurable via `config/neoessentials/animations.json`
- All messages, descriptions, and command outputs are lang-managed

## Configuration
**File:** `config/neoessentials/animations.json` (auto-generated on first run)

Edit this file to define custom animations, then use `/neoanimations reload` to apply changes instantly.

### Example Animation
```json
{
  "animations": {
    "rainbow_text": {
      "type": "color_cycle",
      "frames": ["&c{text}", "&6{text}", "&e{text}", "&a{text}", "&b{text}", "&9{text}", "&d{text}"],
      "speed": 500,
      "description": "Cycles through rainbow colors"
    }
  }
}
```

### Placeholder Mapping
```json
{
  "placeholder_mappings": {
    "{animated_server_name}": "rainbow_text",
    "{loading}": "loading_bar"
  }
}
```

## Usage in Themes
Reference animated placeholders in your tablist, scoreboard, or bossbar theme configs:

```yaml
tablist_themes:
  animated_theme:
    headers:
      - "&6{animated_server_name}"
      - "&e{status} {dots}"
    footers:
      - "&7Players: &f{online}&8/&f{max}"
      - "&7Health: {health} &8| &7Weather: {weather}"
```

## Built-in Placeholders
All standard NeoEssentials placeholders are supported:
- `{player}` `{health}` `{max_health}` `{food}` `{level}` `{exp}` `{ping}`
- `{online}` `{max}` `{tps}` `{time}` `{uptime}` `{world}` `{ram_used}` `{ram_max}`
- `{weather}` `{session_time}` `{balance}`

## Global Settings
Configure animation system behavior in `animations.json`:
```json
{
  "global_settings": {
    "enable_animations": true,
    "max_fps": 20,
    "smooth_transitions": true,
    "cache_animations": true,
    "debug_mode": false
  }
}
```

## Commands
All output/messages are lang-managed and permission-checked.

| Command | Description | Permission |
|---------|-------------|------------|
| `/neoanimations reload` | Reload animation config | `neoessentials.admin` |
| `/neoanimations stats` | Show animation stats | `neoessentials.admin` |
| `/neoanimations list` | List all animations | `neoessentials.admin` |
| `/neoanimations test <animation>` | Test animation | `neoessentials.admin` |
| `/neoanimations help` | Show help | `neoessentials.admin` |

Server operators (level 3+) have access by default.

## Troubleshooting
**Animations not showing:**
- Ensure `enable_animations` is true
- Check placeholder mappings and animation names
- Use `/neoanimations reload` after changes

**Performance issues:**
- Lower `max_fps` or reduce frame count
- Disable caching if memory is limited

**Config errors:**
- Validate JSON syntax
- Check console for errors

Enable debug mode for detailed logs:
```json
{
  "global_settings": { "debug_mode": true }
}
```

## Advanced & API Usage
Extend with custom animation types, integrate with economy, permissions, weather, and player stats. All output/messages are lang-managed.

**Java Example:**
```java
AnimationManager manager = TablistScoreboardManager.getInstance().getAnimationManager();
String result = manager.processAnimatedText("{animated_placeholder}", player);
Set<String> animations = manager.getAnimationNames();
```

## Example Configs
See `docs/Example Configs/animations_prefix_examples.json` and the auto-generated `animations.json` for more templates.

---
The NeoEssentials animation system is fully config-driven, permission-checked, and lang-managed for maximum flexibility and localization.
