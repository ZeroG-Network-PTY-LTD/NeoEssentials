# Animation System Documentation

## Overview

The NeoEssentials Animation System provides powerful animated placeholders for tablist, scoreboard, and bossbar displays. Create dynamic, eye-catching server information with customizable animations that update in real-time.

## Features

### Animation Types
- **Text Cycle**: Cycle through predefined text frames
- **Color Cycle**: Animate through different color codes
- **Progress Bar**: Loading bar animations
- **Conditional**: Dynamic content based on server state
- **Health Bar**: Visual health representation
- **Weather**: Dynamic weather icons
- **Typewriter**: Text appearing letter by letter
- **Gradient**: Shifting color gradients
- **Wave**: Wave motion effects
- **Blink**: Blinking text effect

### Supported Displays
- **Tablist Headers/Footers**: Animated player list information
- **Scoreboard**: Dynamic sidebar content
- **Bossbar**: Animated boss bar messages

## Configuration

### animations.json Location
```
config/neoessentials/animations.json
```

### Basic Animation Structure
```json
{
  "animations": {
    "animation_name": {
      "type": "animation_type",
      "frames": ["frame1", "frame2", "frame3"],
      "speed": 500,
      "description": "Animation description"
    }
  }
}
```

## Animation Examples

### 1. Rainbow Text Animation
```json
"rainbow_text": {
  "type": "color_cycle",
  "frames": [
    "&c{text}",
    "&6{text}",
    "&e{text}",
    "&a{text}",
    "&b{text}",
    "&9{text}",
    "&d{text}"
  ],
  "speed": 500,
  "description": "Cycles through rainbow colors"
}
```

### 2. Loading Bar Animation
```json
"loading_bar": {
  "type": "progress_bar",
  "frames": [
    "&7[&c■&7□□□□□□□□□]",
    "&7[&c■■&7□□□□□□□□]",
    "&7[&c■■■&7□□□□□□□]",
    "&7[&c■■■■&7□□□□□□]",
    "&7[&c■■■■■&7□□□□□]",
    "&7[&c■■■■■■&7□□□□]",
    "&7[&c■■■■■■■&7□□□]",
    "&7[&c■■■■■■■■&7□□]",
    "&7[&c■■■■■■■■■&7□]",
    "&7[&a■■■■■■■■■■&7]"
  ],
  "speed": 200,
  "description": "Loading progress bar"
}
```

### 3. Dynamic Server Status
```json
"server_status": {
  "type": "conditional",
  "conditions": [
    {
      "placeholder": "{online}",
      "operator": ">",
      "value": "50",
      "frame": "&a● BUSY"
    },
    {
      "placeholder": "{online}",
      "operator": ">",
      "value": "20",
      "frame": "&e● ACTIVE"
    },
    {
      "placeholder": "{online}",
      "operator": ">=",
      "value": "1",
      "frame": "&6● ONLINE"
    }
  ],
  "default_frame": "&7● EMPTY",
  "description": "Dynamic server status based on player count"
}
```

### 4. Health Bar Visualization
```json
"health_bar": {
  "type": "health_bar",
  "full_char": "❤",
  "half_char": "♡",
  "empty_char": "♢",
  "full_color": "&c",
  "half_color": "&6",
  "empty_color": "&7",
  "max_hearts": 10,
  "description": "Player health visualization"
}
```

### 5. Weather Icons
```json
"weather_icon": {
  "type": "weather",
  "clear": "&e☀",
  "rain": "&9☔",
  "storm": "&8⚡",
  "snow": "&f❄",
  "description": "Dynamic weather icon"
}
```

## Placeholder Mappings

Map placeholders to animations in your configuration:

```json
"placeholder_mappings": {
  "{animated_server_name}": "rainbow_text",
  "{loading}": "loading_bar",
  "{status}": "server_status",
  "{health}": "health_bar",
  "{weather}": "weather_icon",
  "{dots}": "bouncing_dots",
  "{heart}": "pulse_heart",
  "{compass}": "spinning_compass"
}
```

## Usage in Themes

### Tablist Theme Example
```yaml
tablist_themes:
  animated_theme:
    name: "Animated Theme"
    headers:
      - "&6{animated_server_name}"
      - "&e{status} {dots}"
    footers:
      - "&7Players: &f{online}&8/&f{max}"
      - "&7Health: {health} &8| &7Weather: {weather}"
    rotation_interval: 3000
```

### Scoreboard Theme Example
```yaml
scoreboard_themes:
  animated_stats:
    name: "Animated Stats"
    title: "&6&l{animated_server_name}"
    lines:
      - "&7━━━━━━━━━━━━━━━━"
      - "&ePlayer: &f{player}"
      - "&eHealth: {health}"
      - "&eStatus: {status}"
      - "&7Weather: {weather}"
      - "&6Loading: {loading}"
      - "&7━━━━━━━━━━━━━━━━"
```

### Bossbar Template Example
```yaml
bossbar_templates:
  animated_welcome:
    title: "{animated_server_name}"
    text: "&aWelcome {player}! {heart}"
    color: "BLUE"
    overlay: "PROGRESS"
    progress: 1.0
    duration: 10
```

## Commands

### Animation Management Commands
```
/neoanimations reload          - Reload animation configurations
/neoanimations stats           - Show animation system statistics
/neoanimations list            - List all available animations
/neoanimations test <animation> - Test an animation
/neoanimations help            - Show help message
```

### Permission Requirements
- `neoessentials.admin` - Required for all animation commands
- Server operators (level 3+) have access by default

## Global Settings

Configure animation system behavior:

```json
"global_settings": {
  "enable_animations": true,
  "max_fps": 20,
  "smooth_transitions": true,
  "cache_animations": true,
  "debug_mode": false
}
```

### Settings Explained
- **enable_animations**: Master toggle for animation system
- **max_fps**: Maximum animation update rate (1-60)
- **smooth_transitions**: Enable smooth animation transitions
- **cache_animations**: Cache player-specific animation states
- **debug_mode**: Enable debug logging for animations

## Built-in Placeholders

The animation system supports all standard NeoEssentials placeholders:

### Player Information
- `{player}` - Player display name
- `{health}` - Current health
- `{max_health}` - Maximum health
- `{food}` - Food level
- `{level}` - Experience level
- `{exp}` - Experience percentage
- `{ping}` - Connection latency

### Server Information
- `{online}` - Online player count
- `{max}` - Maximum player count
- `{tps}` - Server TPS
- `{time}` - Current time
- `{uptime}` - Server uptime
- `{world}` - Current world
- `{ram_used}` - Used RAM
- `{ram_max}` - Maximum RAM

### Dynamic Placeholders
- `{weather}` - Current weather
- `{session_time}` - Player session time
- `{balance}` - Player balance (if economy enabled)

## Animation Performance

### Optimization Tips
1. **Reasonable Frame Counts**: Keep animation frames under 20 for smooth performance
2. **Appropriate Speed**: Use speeds between 100-1000ms for best results
3. **Cache Settings**: Enable caching for better performance with many players
4. **FPS Limits**: Keep max_fps at 20 or lower for servers with many players

### Memory Usage
- Each animation frame uses minimal memory (~50 bytes)
- Player-specific caching adds ~1KB per player per animation
- Total memory impact is typically under 1MB for most configurations

## Troubleshooting

### Common Issues

1. **Animations Not Showing**
   - Check if `enable_animations` is true
   - Verify placeholder mappings are correct
   - Ensure animation names match exactly

2. **Performance Issues**
   - Reduce max_fps setting
   - Decrease number of animation frames
   - Disable caching if memory is limited

3. **Configuration Errors**
   - Use `/neoanimations reload` to test changes
   - Check console for JSON parsing errors
   - Validate JSON syntax

### Debug Mode
Enable debug mode for detailed logging:
```json
"global_settings": {
  "debug_mode": true
}
```

## Advanced Features

### Custom Animation Types
The system supports extending with custom animation types for specific server needs.

### Integration with Other Systems
- **Economy**: Balance-based conditional animations
- **Permissions**: Permission-based animation visibility
- **Weather**: Real-time weather integration
- **Player Stats**: Health, hunger, experience animations

### Performance Monitoring
Use `/neoanimations stats` to monitor:
- Active animations count
- Cached player states
- System performance metrics

## Examples and Templates

See the `docs/Example Configs/animations_examples.yml` file for complete working examples of:
- Animated tablist themes
- Dynamic scoreboard configurations  
- Interactive bossbar templates
- Complex conditional animations

## API Integration

For developers wanting to create custom animations programmatically:

```java
// Get animation manager
AnimationManager manager = TablistScoreboardManager.getInstance().getAnimationManager();

// Process animated text
String result = manager.processAnimatedText("{animated_placeholder}", player);

// Check available animations
Set<String> animations = manager.getAnimationNames();
```

This animation system provides endless possibilities for creating engaging, dynamic server displays that keep players informed and entertained!
