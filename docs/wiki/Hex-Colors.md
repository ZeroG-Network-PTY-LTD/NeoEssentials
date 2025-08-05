# Hex Color Support Documentation

## Overview

Yes! NeoEssentials **fully supports** hex colors (#FFFFFF format) in custom placeholders, animations, tablist, scoreboard, and bossbar displays. You can use hex colors anywhere that accepts color codes.

## Supported Color Formats

### 1. Legacy Color Codes (Traditional)
```
&0-9, &a-f    - Standard Minecraft colors
&k-o, &r      - Formatting codes (bold, italic, etc.)
```

### 2. Hex Colors (Modern - RGB)
```
#FFFFFF       - Direct hex format
&#FFFFFF      - Ampersand + hex format  
&{#FFFFFF}    - Bracketed hex format
```

## Hex Color Examples

### Basic Hex Colors
```json
"#FF0000"     - Pure Red
"#00FF00"     - Pure Green  
"#0000FF"     - Pure Blue
"#FFFFFF"     - Pure White
"#000000"     - Pure Black
"#808080"     - Gray
```

### Popular Hex Colors
```json
"#FF6B35"     - Orange Red
"#F7931E"     - Orange
"#FFD23F"     - Gold
"#06FFA5"     - Mint Green
"#4ECDC4"     - Teal
"#45B7D1"     - Sky Blue
"#96CEB4"     - Sage Green
"#FFEAA7"     - Light Yellow
"#DDA0DD"     - Plum
"#FF69B4"     - Hot Pink
```

## Usage in Animations

### Hex Color Animation Example
```json
"hex_rainbow": {
  "type": "color_cycle",
  "frames": [
    "#FF0000{text}",
    "#FF7F00{text}",
    "#FFFF00{text}",
    "#00FF00{text}",
    "#0000FF{text}",
    "#4B0082{text}",
    "#9400D3{text}"
  ],
  "speed": 500,
  "description": "True RGB rainbow animation"
}
```

### Neon Glow Effect
```json
"neon_glow": {
  "type": "color_cycle",
  "frames": [
    "#00FFFF{text}",
    "#00DDFF{text}",
    "#00BBFF{text}",
    "#0099FF{text}",
    "#0077FF{text}",
    "#0099FF{text}",
    "#00BBFF{text}",
    "#00DDFF{text}"
  ],
  "speed": 300,
  "description": "Neon blue glow effect"
}
```

### Fire Effect
```json
"fire_effect": {
  "type": "color_cycle", 
  "frames": [
    "#FF0000{text}",
    "#FF3300{text}",
    "#FF6600{text}",
    "#FF9900{text}",
    "#FFCC00{text}",
    "#FFFF00{text}",
    "#FFCC00{text}",
    "#FF9900{text}",
    "#FF6600{text}",
    "#FF3300{text}"
  ],
  "speed": 200,
  "description": "Fire color animation"
}
```

## Usage in Themes

### Tablist with Hex Colors
```yaml
tablist_themes:
  hex_theme:
    name: "Hex Color Theme"
    headers:
      - "#FF6B35&lAWESOME SERVER"
      - "#F7931E{hex_server_name}"
    footers:
      - "#45B7D1Players: &f{online}#808080/&f{max}"
      - "#06FFA5Health: {health} #4ECDC4| #96CEB4TPS: #FFEAA7{tps}"
    rotation_interval: 3000
```

### Scoreboard with Hex Colors
```yaml
scoreboard_themes:
  hex_stats:
    name: "Hex Stats Theme"
    title: "#FF6B35&l⚡ #F7931E&lSERVER #FFD23F&lSTATS"
    lines:
      - "#808080━━━━━━━━━━━━━━━━"
      - "#45B7D1Player: #FFFFFF{player}"
      - "#06FFA5Health: {health}"
      - "#4ECDC4World: #FFFFFF{world}"
      - "#96CEB4Ping: #FFEAA7{ping}ms"
      - "#808080━━━━━━━━━━━━━━━━"
      - "#FF69B4Status: {neon_text}"
      - "#DDA0DD{fire_text}"
      - "#808080━━━━━━━━━━━━━━━━"
```

### Bossbar with Hex Colors
```yaml
bossbar_templates:
  hex_welcome:
    title: "#FF6B35{fire_text}"
    text: "#06FFA5Welcome #FFFFFF{player}#FF69B4! {heart}"
    color: "BLUE"
    overlay: "PROGRESS"
    progress: 1.0
    duration: 10
```

## Advanced Hex Features

### Gradient Creation
Use the built-in gradient utility:
```java
// Create 5-step gradient from red to blue
String[] gradient = ColorUtil.createGradient("#FF0000", "#0000FF", 5);
// Result: ["#FF0000", "#BF003F", "#7F007F", "#3F00BF", "#0000FF"]
```

### RGB Conversion
```java
// Convert RGB to hex
String hex = ColorUtil.rgbToHex(255, 100, 50);  // "#FF6432"

// Convert hex to RGB
int[] rgb = ColorUtil.hexToRgb("#FF6432");  // [255, 100, 50]
```

## Available Hex Animations

The default configuration includes these hex-powered animations:

### Color Animations
- `{hex_server_name}` - True RGB rainbow
- `{neon_text}` - Neon blue glow  
- `{fire_text}` - Fire effect
- `{sunset_text}` - Sunset glow
- `{ocean_text}` - Ocean wave gradient

### Usage Examples
```yaml
# In tablist header
header: "#FF6B35Server: {hex_server_name}"

# In scoreboard line  
line: "#06FFA5Status: {neon_text}"

# In bossbar text
text: "{fire_text} #FFFFFF{player}! {sunset_text}"
```

## Color Tools & Resources

### Online Color Pickers
- [HTML Color Codes](https://htmlcolorcodes.com/)
- [Adobe Color](https://color.adobe.com/)
- [Coolors.co](https://coolors.co/)

### Minecraft-Specific Tools
- [Minecraft Color Code Generator](https://minecraft.tools/en/color-code.php)
- [RGB to Hex Converter](https://www.rapidtables.com/convert/color/rgb-to-hex.html)

### Color Palette Ideas
```json
// Gaming Theme
"#00FF41"  - Matrix Green
"#FF0080"  - Electric Pink  
"#8A2BE2"  - Blue Violet
"#FFD700"  - Gold

// Ocean Theme  
"#006994"  - Deep Blue
"#47B5D8"  - Sky Blue
"#B8E6B8"  - Sea Foam
"#E0F6FF"  - Ice Blue

// Fire Theme
"#FF4500"  - Orange Red
"#FF6347"  - Tomato  
"#FFD700"  - Gold
"#FFFF00"  - Yellow

// Pastel Theme
"#FFB6C1"  - Light Pink
"#E6E6FA"  - Lavender
"#F0FFFF"  - Azure  
"#F5FFFA"  - Mint Cream
```

## Performance Notes

### Hex Color Performance
- **Excellent**: Hex colors are processed efficiently
- **No Lag**: Modern Minecraft clients handle RGB colors natively
- **Memory Efficient**: Hex colors use the same memory as legacy colors

### Best Practices
1. **Consistent Format**: Use `#RRGGBB` format consistently
2. **Valid Colors**: Ensure hex codes are 6 characters (A-F, 0-9)
3. **Contrast**: Maintain good contrast for readability
4. **Testing**: Test colors on different client settings

## Troubleshooting

### Common Issues
1. **Invalid Hex**: Ensure format is exactly `#RRGGBB`
2. **Case Sensitivity**: Hex codes are case-insensitive (`#FF0000` = `#ff0000`)
3. **Missing #**: Hash symbol is required for direct hex format

### Debug Commands
```
/neoanimations test hex_rainbow    - Test hex rainbow animation
/neoanimations list               - Show all available animations
/neoanimations stats              - Show animation system stats
```

## Configuration Examples

### Full Hex Theme Configuration
See `docs/Example Configs/hex_colors_examples.yml` for complete working examples including:
- Animated hex tablist themes
- Dynamic hex scoreboard configurations  
- Interactive hex bossbar templates
- Complex hex gradient animations

## Summary

**✅ YES** - You can absolutely use `#FFFFFF` type coloring in:
- **Custom placeholders** - All animation frames
- **Tablist headers/footers** - Full hex support
- **Scoreboard titles/lines** - Complete RGB control
- **Bossbar text/titles** - Modern color support
- **Theme configurations** - Mix legacy and hex colors

**Enhanced Features:**
- **True RGB Colors** - 16.7 million color possibilities
- **Gradient Support** - Smooth color transitions
- **Animation Integration** - Hex colors in all animation types
- **Performance Optimized** - No performance penalty
- **Easy Migration** - Mix with existing legacy colors

The color system is fully modern and supports both traditional Minecraft color codes and modern hex RGB colors for maximum flexibility and visual appeal!
