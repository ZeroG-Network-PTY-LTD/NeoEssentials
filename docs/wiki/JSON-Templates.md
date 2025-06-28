# JSON Templates Guide

Complete JSON template configuration guide for NeoEssentials v1.0.1.89+ including multiple animation placeholders and the three-tier update system.

## 🎯 Overview

JSON templates provide a powerful way to configure tablist headers, footers, boss bars, and animations. While **YAML is recommended** for its readability, JSON remains fully supported for advanced configurations.

### When to Use JSON
- ✅ **Legacy compatibility** with existing configurations
- ✅ **Advanced nested structures** requiring precise syntax  
- ✅ **Programmatic generation** of configuration files
- ✅ **Integration with external tools** that output JSON

## 📁 File Structure

JSON template files are located in the server root `neoessentials/` directory:

| File | Purpose | Priority |
|------|---------|----------|
| `templates.json` | **Tablist headers, footers, boss bars** | Secondary (YAML preferred) |
| `animations.json` | **Animation frame definitions** | Secondary (YAML preferred) |

## 🚀 Core Configuration: `templates.json`

### Basic Structure with Multiple Animations
```json
{
  "settings": {
    "update_interval": 3000,
    "placeholder_update_interval": 250,
    "animation_frame_interval": 25,
    "enable_animations": true,
    "enable_headers": true,
    "enable_footers": true,
    "enable_group_specific": true
  },
  "templates": {
    "headers": [
      "&6&l✦ &b&lNeoEssentials Server &6&l✦",
      "<anim:welcome>",
      "&a%player%&e! <anim:server_name>",
      "&eOnline: &a%online%&e/&a%max% <anim:pulse>"
    ],
    "footers": [
      "&eBalance: &a%balance% coins",
      "<anim:example> &7| <anim:rainbow>", 
      "&eServer TPS: &a%tps% <anim:clock>"
    ]
  }
### Group-Specific Templates
```json
{
  "groups": {
    "admin": {
      "headers": [
        "&4&l★ &c&lAdmin Panel &4&l★",
        "&a%player% &7| &cAdmin Mode",
        "<anim:admin_status> &7| &eOnline: &a%online%"
      ],
      "footers": [
        "&cAdmin Commands: &f/neoessentials help",
        "&cServer uptime: &f%uptime%"
      ]
    },
    "vip": {
      "headers": [
        "&6&l⚜ &e&lVIP Member &6&l⚜",
        "&eWelcome back, &6%player%&e!",
        "<anim:vip_benefits> &7| <anim:pulse>"
      ],
      "footers": [
        "&6VIP Balance: &e%balance% coins",
        "&6Use &e/vip help &6for perks list"
      ]
    },
    "moderator": {
      "headers": [
        "&2&l⚡ &a&lModerator &2&l⚡",
        "&a%player% &7| &2Staff Online",
        "<anim:mod_tools>"
      ],
      "footers": [
        "&2Mod Tools: &a/modhelp",
        "&2Reports: &a%pending_reports%"
      ]
    }
  }
}
```

### Boss Bars with Animations
```json
{
  "bossbars": {
    "global": [
      "{color:red}{style:progress}{progress:1.0}Server TPS: %tps%",
      "{color:green}{style:notched_6}{progress:0.8}<anim:welcome>"
    ],
    "groups": {
      "admin": [
        "{color:purple}{style:progress}{progress:1.0}<anim:admin_status>"
      ],
      "vip": [
        "{color:gold}{style:progress}{progress:1.0}<anim:vip_benefits>"
      ]
    }
  }
}
```

## 🎨 Animation Configuration: `animations.json`

### Ultra-Smooth Animation Structure  
```json
{
  "metadata": {
    "version": "1.0.1.89",
    "description": "NeoEssentials Ultra-Smooth Animations",
    "author": "NeoEssentials Team"
  },
  "animations": {
    "welcome": {
      "interval": 100,
      "frames": [
        "&eW",
        "&eWe", 
        "&eWel",
        "&eWelc",
        "&eWelco",
        "&eWelcom",
        "&eWelcome",
        "&eWelcome!"
      ]
    },
    "pulse": {
      "interval": 25,
      "frames": [
        "&f●",
        "&7●",
        "&8●", 
        "&7●"
      ]
    },
    "rainbow": {
      "interval": 50,
      "frames": [
        "&#FF0000R&#FF3300a&#FF6600i&#FF9900n&#FFCC00b&#FFFF00o&#CCFF00w",
        "&#FF3300R&#FF6600a&#FF9900i&#FFCC00n&#FFFF00b&#CCFF00o&#FF0000w",
        "&#FF6600R&#FF9900a&#FFCC00i&#FFFF00n&#CCFF00b&#FF0000o&#FF3300w"
      ]
    },
    "server_name": {
      "interval": 80,
      "frames": [
        "&b&lM&f&lyServer",
        "&f&lM&b&ly&f&lServer", 
        "&f&lMy&b&lS&f&lerver",
        "&f&lMyS&b&le&f&lrver",
        "&f&lMySe&b&lr&f&lver",
        "&f&lMyServer"
      ]
    },
    "clock": {
      "interval": 120,
      "frames": ["🕐", "🕑", "🕒", "🕓", "🕔", "🕕", "🕖", "🕗", "🕘", "🕙", "🕚", "🕛"]
    }
  }
}
```

### Gradient Effects
```json
{
  "gradients": {
    "ocean_wave": {
      "colors": ["#001F3F", "#002A55", "#004080", "#0066CC", "#0080FF"],
      "steps": 12,
      "text": "Ocean Wave"
    },
    "sunset": {
      "colors": ["#FF6B35", "#F7931E", "#FFD23F", "#FF6B35"],
      "steps": 15,
      "text": "Sunset Glow"
    }
  }
}
```

## 📝 JSON Syntax Guide

### Essential Rules
1. **Quotes**: All strings must be in double quotes `"text"`
2. **Commas**: Required between array items and object properties
3. **Braces**: `{}` for objects, `[]` for arrays
4. **Escaping**: Use `\"` for quotes inside strings

### Common Patterns
```json
// Simple array
"headers": [
  "Header 1",
  "Header 2"
]

// Nested object
"groups": {
  "admin": {
    "headers": ["Admin header"]
  }
}

// Animation with frames
"animations": {
  "my_anim": {
    "interval": 50,
    "frames": ["Frame 1", "Frame 2"]
  }
}
```
## ⚠️ Common JSON Mistakes

| Issue | Problem | Solution |
|-------|---------|----------|
| **Missing Quotes** | `headers: [Hello]` | Use `"headers": ["Hello"]` |
| **Missing Commas** | `"header1" "header2"` | Use `"header1", "header2"` |
| **Trailing Commas** | `["item1", "item2",]` | Remove trailing comma |
| **Escaped Characters** | `"&"text""` | Use `"&\"text\""` |

## 🔄 JSON vs YAML Comparison

| Feature | JSON | YAML |
|---------|------|------|
| **Readability** | ❌ More verbose | ✅ Clean and readable |
| **Comments** | ❌ Not supported | ✅ `# comments` |
| **Syntax Errors** | ⚠️ Common (commas, quotes) | ✅ Less error-prone |
| **Tool Support** | ✅ Universal | ✅ Growing support |
| **Performance** | ✅ Faster parsing | ⚠️ Slightly slower |

**Recommendation**: Use **YAML** for new configurations, JSON for legacy compatibility.

## 🔧 Advanced Features

### Boss Bar Animation Integration
```json
{
  "bossbars": {
    "global": [
      "{color:red}{style:progress}{progress:1.0}<anim:server_status>",
      "{color:green}{style:notched_6}{progress:0.8}<anim:welcome>"
    ]
  }
}
```

### Boss Bar Properties
- **Colors**: `pink`, `blue`, `red`, `green`, `yellow`, `purple`, `white`
- **Styles**: `progress`, `notched_6`, `notched_10`, `notched_12`, `notched_20`  
- **Progress**: `0.0` to `1.0` or placeholders like `%tps_percent%`

### Multiple Animation Usage
```json
{
  "templates": {
    "headers": [
      "<anim:welcome> &a%player% <anim:pulse>",
      "Online: <anim:counter> &7| Time: <anim:clock>",
      "<anim:rainbow> Server Status <anim:rainbow>"
    ]
  }
}
```

## 🛠️ Validation & Testing

### JSON Validation Tools
- [JSONLint](https://jsonlint.com/) - Online syntax validator
- [JSON Formatter](https://jsonformatter.org/) - Pretty-print and validate

### Debug Commands
```bash
/tablist reload     # Reload JSON configuration  
/tablist debug      # Show parsing status
/tablist test <anim> # Test specific animation
```

### Common Validation Errors
```json
// ❌ Wrong: Missing quotes on keys
{
  templates: {
    headers: ["text"]
  }
}

// ✅ Correct: Quoted keys
{
  "templates": {
    "headers": ["text"]
  }
}
```

## 📁 File Migration

### From Legacy TOML
Use the built-in migration system:
```bash
/neoessentials migrate toml-to-json
```

### Manual Conversion
1. **Backup existing files**
2. **Use online converters** for complex structures
3. **Test with `/tablist reload`**
4. **Validate with debug commands**

## 🔗 Integration Examples

### External Tool Generation
```javascript
// Example: Generate JSON config programmatically
const config = {
  templates: {
    headers: [
      `&6&l✦ &b&l${serverName} &6&l✦`,
      `<anim:welcome>`,
      `&eOnline: &a%online%&e/&a%max%`
    ]
  }
};
```

### API Integration
```java
// Java example: Programmatic template creation
JsonObject template = new JsonObject();
JsonArray headers = new JsonArray();
headers.add("&6Server Header");
headers.add("<anim:welcome>");
template.add("headers", headers);
```

---

*JSON templates provide powerful configuration capabilities for advanced NeoEssentials setups. For easier management, consider using [YAML Configuration](YML-Configuration) instead.*
```

## Loading Priority

Templates and animations are loaded in this order:

1. JSON files (`neoessentials/templates.json`, `neoessentials/animations.json`)
2. YML files (`neoessentials/templates.yml`, `neoessentials/animations.yml`) if JSON not found
3. Legacy files in the config directory (for backward compatibility)
4. Default templates from mod resources if no custom files are found

## Validation and Troubleshooting

If your JSON is invalid:

1. Check the server logs for parsing errors
2. Validate your JSON using an online tool like [JSONLint](https://jsonlint.com/)
3. Fix any syntax issues (missing commas, unbalanced brackets, etc.)
4. Reload with `/neoessentials tablist reload`

## Examples

### Simple Server Info Template

```json
{
  "templates": {
    "headers": [
      "&6&l✦ &b&lNeoEssentials Server &6&l✦",
      "&eWelcome, &a%player%&e!"
    ],
    "footers": [
      "&eOnline: &a%online%/%max% &7| &eUptime: &a%uptime%"
    ]
  }
}
```

### Advanced Template with Groups and Boss Bars

```json
{
  "templates": {
    "headers": ["&eWelcome to the server!"],
    "footers": ["&eVisit our website: &awww.example.com"]
  },
  "groups": {
    "admin": {
      "headers": [
        "&4&l★ &c&lAdmin Panel &4&l★",
        "&cServer TPS: &f%tps%"
      ],
      "footers": [
        "&cMemory: &f%memory_used%&c/&f%memory_max% MB"
      ]
    }
  },
  "bossbars": {
    "global": [
      "{color:green}{style:progress}{progress:1.0}Welcome to NeoEssentials!"
    ],
    "groups": {
      "admin": [
        "{color:red}{style:progress}{progress:%tps/20%}TPS: %tps%"
      ]
    }
  }
}
```

For more detailed information, see the [JSON Templates Guide](https://github.com/ZeroG-Network/NeoEssentials/wiki/JSON-Templates-Guide) and [TOML to JSON Migration](https://github.com/ZeroG-Network/NeoEssentials/wiki/TOML-to-JSON-Migration) pages.
