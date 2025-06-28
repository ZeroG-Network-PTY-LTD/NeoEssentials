# Animation System

The NeoEssentials Animation System provides ultra-smooth, customizable text animations for tablist headers, footers, and other UI elements.

## 🎯 Overview

The animation system features:
- **Ultra-fast updates**: 25ms frame intervals for smooth animations
- **Multiple simultaneous animations**: Use multiple `<anim:name>` placeholders
- **Independent animation timing**: Each animation runs at its own speed
- **Hex color support**: Full RGB animations with gradients
- **Easy configuration**: Simple YAML-based animation definitions

## 🚀 Key Features

### Multiple Animation Placeholders
Use multiple animations in the same template:
```yaml
headers:
  - "<anim:welcome> &a%player%&e! <anim:server_name>"
  - "<anim:rainbow> &7| <anim:pulse> &7| <anim:clock>"
```

### Independent Timing
Each animation can have its own update interval:
```yaml
animations:
  fast_pulse:
    interval: 10  # Very fast (10ms)
    frames: ["&a●", "&7●"]
  
  slow_wave:
    interval: 200  # Slower (200ms)  
    frames: ["～", "~", "～"]
```

### Regex Pattern Fix
The system uses an optimized regex pattern that properly extracts animation names:
```
Pattern: (\{animation:|<anim:)([^}>]+)(}|>)
```
This ensures multiple placeholders work correctly without interference.

**Critical Fix Details:**
- **Problem**: Previous pattern `([^}]+)` captured too much text
- **Solution**: Changed to `([^}>]+)` to stop at both `}` and `>` characters  
- **Result**: Perfect extraction enabling unlimited simultaneous animations

## ⚙️ Configuration

### Animation Definition Format

```yaml
animations:
  animation_name:
    interval: 30  # milliseconds between frames
    frames:
      - "Frame 1 text"
      - "Frame 2 text"
      - "Frame 3 text"
```

### Complete Example

```yaml
animations:
  # Welcome animation with typewriter effect
  welcome:
    interval: 100
    frames:
      - "&eW"
      - "&eWe"
      - "&eWel"
      - "&eWelc"
      - "&eWelco"
      - "&eWelcom"
      - "&eWelcome"
      - "&eWelcome!"
  
  # Rainbow color cycling
  rainbow:
    interval: 50
    frames:
      - "&#FF0000R&#FF3300a&#FF6600i&#FF9900n&#FFCC00b&#FFFF00o&#CCFF00w"
      - "&#FF3300R&#FF6600a&#FF9900i&#FFCC00n&#FFFF00b&#CCFF00o&#FF0000w"
      - "&#FF6600R&#FF9900a&#FFCC00i&#FFFF00n&#CCFF00b&#FF0000o&#FF3300w"
  
  # Pulsing effect
  pulse:
    interval: 25
    frames:
      - "&f●"
      - "&7●"
      - "&8●"
      - "&7●"
  
  # Server name animation
  server_name:
    interval: 80
    frames:
      - "&b&lM&f&lyServer"
      - "&f&lM&b&ly&f&lServer"
      - "&f&lMy&b&lS&f&lerver"
      - "&f&lMyS&b&le&f&lrver"
      - "&f&lMySe&b&lr&f&lver"
      - "&f&lMyServ&b&le&f&lr"
      - "&f&lMyServe&b&lr"
      - "&f&lMyServer"
  
  # Clock animation
  clock:
    interval: 120
    frames:
      - "🕐"
      - "🕑"
      - "🕒"
      - "🕓"
      - "🕔"
      - "🕕"
      - "🕖"
      - "🕗"
      - "🕘"
      - "🕙"
      - "🕚"
      - "🕛"
```

## 🎨 Animation Techniques

### Color Gradients
Create smooth color transitions:
```yaml
gradient_blue:
  interval: 40
  frames:
    - "&#0066CC"
    - "&#1A75D6" 
    - "&#3384E0"
    - "&#4D93EA"
    - "&#66A2F4"
    - "&#80B1FE"
    - "&#99C0FF"
```

### Text Effects
Various text animation styles:
```yaml
# Typewriter effect
typing:
  interval: 80
  frames:
    - "H"
    - "He"
    - "Hel"
    - "Hell"
    - "Hello"

# Wave effect  
wave:
  interval: 60
  frames:
    - "Hello"
    - "HEllo"
    - "HeLLo"
    - "HellO"
    - "Hello"

# Blinking
blink:
  interval: 500
  frames:
    - "&aTEXT"
    - "&f    "
```

### Loading Animations
```yaml
loading:
  interval: 150
  frames:
    - "&7Loading"
    - "&7Loading."
    - "&7Loading.."
    - "&7Loading..."
    
dots:
  interval: 200
  frames:
    - "&a●&7●●"
    - "&7●&a●&7●"  
    - "&7●●&a●"
```

## 🔧 Using Animations

### In Tablist Templates
```yaml
templates:
  headers:
    - "&6&l✦ &b&lNeoEssentials Server &6&l✦"
    - "<anim:welcome>"
    - "&a%player%&e! <anim:server_name>"
    - "&eOnline: &a%online%&e/&a%max% <anim:pulse>"
  
  footers:
    - "&eBalance: &a%balance% coins <anim:dots>"
    - "<anim:rainbow> Welcome to our server! <anim:rainbow>"
    - "&eServer TPS: &a%tps% <anim:clock>"
```

### Placeholder Formats
Both formats are supported:
- `<anim:name>` - Modern format (recommended)
- `{animation:name}` - Legacy format

### Multiple Animations
You can use as many animations as needed:
```yaml
- "<anim:welcome> &a%player% <anim:pulse> Online: <anim:counter> <anim:clock>"
```

## 📊 Performance Considerations

### Optimization Tips

1. **Choose appropriate intervals**:
   - Fast effects: 10-50ms for smooth visuals
   - Normal animations: 50-150ms for good balance
   - Slow effects: 200-500ms for subtle changes

2. **Frame count guidelines**:
   - Simple effects: 2-5 frames
   - Complex animations: 5-15 frames
   - Detailed sequences: 10-30 frames

3. **Server performance**:
   - Monitor TPS when using many animations
   - Reduce update frequency if needed
   - Limit simultaneous animations per player

### Performance Monitoring
```yaml
# Debug settings in general.toml
debug_mode = true
tablist_debug = true
animation_debug = true
```

## 🎯 Animation Ideas

### Server Branding
```yaml
server_logo:
  interval: 100
  frames:
    - "&6▄&e▄ &6&lMYSERVER &e▄&6▄"
    - "&e▄&6▄ &e&lMYSERVER &6▄&e▄"
```

### Status Indicators
```yaml
online_status:
  interval: 800
  frames:
    - "&a● &fOnline"
    - "&2● &fOnline"

tps_indicator:
  interval: 50
  frames:
    - "&a▁▂▃▄▅▆▇█"
    - "&a▂▃▄▅▆▇█▁"
    - "&a▃▄▅▆▇█▁▂"
```

### Seasonal Themes
```yaml
christmas:
  interval: 200
  frames:
    - "&c🎄 &aChristmas Special &c🎄"
    - "&a🎄 &cChristmas Special &a🎄"

halloween:
  interval: 300
  frames:
    - "&6🎃 &8Spooky Server &6🎃"
    - "&8🎃 &6Spooky Server &8🎃"
```

## 🛠️ Commands

### Animation Commands
- `/tablist test <animation>` - Test a specific animation
- `/tablist animations` - List all available animations
- `/tablist reload` - Reload animation configurations

## 🔍 Troubleshooting

### Common Issues

**Animation not working:**
1. Check animation exists in `animations.yml`
2. Verify syntax: `<anim:name>` or `{animation:name}`
3. Ensure animations are enabled in settings
4. Check for YAML syntax errors

**Performance problems:**
1. Reduce animation intervals (increase delay)
2. Decrease frame counts
3. Limit simultaneous animations
4. Monitor server TPS

**Colors not displaying:**
1. Verify hex color format: `&#RRGGBB`
2. Check client version compatibility
3. Test with standard color codes first

### Debug Information
Enable animation debugging:
```yaml
# In general.toml
[debug]
animation_logging = true
animation_frame_logging = true
```

## 🔗 Related Documentation

- [Tablist System](Tablist-System) - Main tablist configuration
- [Text Formatting](Text-Formatting) - Color codes and formatting
- [Custom Placeholders](Custom-Placeholders) - Creating custom placeholders
- [Performance Optimization](Performance-Optimization) - Server optimization

---

*The animation system is designed for maximum flexibility and performance. Experiment with different combinations to create unique server experiences!*
