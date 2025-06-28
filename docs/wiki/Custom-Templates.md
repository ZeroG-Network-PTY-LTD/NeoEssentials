# Custom Templates

This guide explains how to create and use custom templates with NeoEssentials. Templates can be used for tablist headers/footers, welcome messages, announcements, mail formats, and more.

## Template Basics

Templates are stored in the `neoessentials` directory as either JSON or YAML files. They allow you to create reusable, formatted text components that can include placeholders, colors, hover events, click events, and more.

## File Locations

Templates can be stored in the following locations, listed in order of priority:

1. `neoessentials/templates.json` (primary JSON file)
2. `neoessentials/templates.yml` (primary YAML file)
3. `config/neoessentials/templates.toml` (legacy format, deprecated)

## Template Format

### JSON Format (Recommended)

```json
{
  "templates": {
    "welcome": {
      "lines": [
        "&6&l=== Welcome to the Server ===",
        "&eHello, &b{player}&e! We're glad to have you here!",
        "",
        "&7» &aPlayers online: &f{online_players}/{max_players}",
        "&7» &aServer TPS: &f{tps}",
        "",
        "{center}&6Type &f/help &6for a list of commands{/center}"
      ]
    },
    "rules": {
      "lines": [
        "&l&9Server Rules",
        "&r&c1. No griefing",
        "&c2. Be respectful",
        "&c3. No cheating",
        "&c4. Have fun!"
      ]
    }
  }
}
```

### YAML Format

```yaml
templates:
  welcome:
    lines:
      - "&6&l=== Welcome to the Server ==="
      - "&eHello, &b{player}&e! We're glad to have you here!"
      - ""
      - "&7» &aPlayers online: &f{online_players}/{max_players}"
      - "&7» &aServer TPS: &f{tps}"
      - ""
      - "{center}&6Type &f/help &6for a list of commands{/center}"
  rules:
    lines:
      - "&l&9Server Rules"
      - "&r&c1. No griefing"
      - "&c2. Be respectful"
      - "&c3. No cheating"
      - "&c4. Have fun!"
```

### Legacy TOML Format (Deprecated)

```toml
[templates]
[templates.welcome]
lines = [
  "&6&l=== Welcome to the Server ===",
  "&eHello, &b{player}&e! We're glad to have you here!",
  "",
  "&7» &aPlayers online: &f{online_players}/{max_players}",
  "&7» &aServer TPS: &f{tps}",
  "",
  "{center}&6Type &f/help &6for a list of commands{/center}"
]

[templates.rules]
lines = [
  "&l&9Server Rules",
  "&r&c1. No griefing",
  "&c2. Be respectful",
  "&c3. No cheating",
  "&c4. Have fun!"
]
```

## Organization with Sections

For larger servers, you can organize templates into sections:

### JSON Format

```json
{
  "templates": {
    "messages": {
      "welcome": {
        "lines": ["&6Welcome to the server, &b{player}&6!"]
      },
      "goodbye": {
        "lines": ["&cGoodbye, &e{player}&c! Hope to see you again soon!"]
      }
    },
    "tablist": {
      "header": {
        "lines": [
          "&6&l=== YOUR SERVER NAME ===",
          "&eWelcome, &b{player}&e!"
        ]
      },
      "footer": {
        "lines": [
          "&7Players online: &a{online_players}&7/&a{max_players}",
          "&7Website: &b&nwww.yourserver.com"
        ]
      }
    }
  }
}
```

### YAML Format

```yaml
templates:
  messages:
    welcome:
      lines:
        - "&6Welcome to the server, &b{player}&6!"
    goodbye:
      lines:
        - "&cGoodbye, &e{player}&c! Hope to see you again soon!"
  tablist:
    header:
      lines:
        - "&6&l=== YOUR SERVER NAME ==="
        - "&eWelcome, &b{player}&e!"
    footer:
      lines:
        - "&7Players online: &a{online_players}&7/&a{max_players}"
        - "&7Website: &b&nwww.yourserver.com"
```

## Advanced Template Features

### Variables and Conditionals

You can add variables and conditionals within templates:

```json
{
  "templates": {
    "rank_message": {
      "lines": [
        "{if:permission:neoessentials.vip}&6You are a VIP member!{else}&7You are a regular member.{/if}"
      ]
    },
    "staff_greeting": {
      "variables": {
        "staff_color": "&5",
        "regular_color": "&7"
      },
      "lines": [
        "{if:group:admin}{staff_color}Welcome Admin {player}!{else}{regular_color}Hello {player}!{/if}"
      ]
    }
  }
}
```

### Template Variables

You can define variables that can be used throughout the template:

```json
{
  "templates": {
    "server_info": {
      "variables": {
        "primary_color": "&6",
        "secondary_color": "&e",
        "highlight_color": "&b",
        "server_name": "YourServer"
      },
      "lines": [
        "{primary_color}&l=== {server_name} ===",
        "{secondary_color}Welcome, {highlight_color}{player}{secondary_color}!",
        "{secondary_color}There are {highlight_color}{online_players}{secondary_color} players online."
      ]
    }
  }
}
```

### Inheritance

Templates can inherit from other templates:

```json
{
  "templates": {
    "base_message": {
      "variables": {
        "primary_color": "&6",
        "secondary_color": "&e"
      }
    },
    "welcome_message": {
      "inherit": "base_message",
      "lines": [
        "{primary_color}Welcome, {secondary_color}{player}!"
      ]
    }
  }
}
```

## Using Templates

### In Configuration Files

Reference templates in your configuration files:

```toml
[tablist]
useHeader = true
useFooter = true
headerTemplate = "tablist.header"
footerTemplate = "tablist.footer"
```

### In Commands

Some commands accept templates:

```
/broadcast template:announcement "Server will restart in 5 minutes"
/mail send playerName template:mail_format "Hello there!"
```

### Programmatically (For Developers)

Templates can be accessed programmatically:

```java
TemplateManager templateManager = NeoEssentials.getInstance().getTemplateManager();
Template template = templateManager.getTemplate("welcome");
String processed = template.process(player);
player.sendMessage(Component.text(processed));
```

## Multi-page Templates

For longer content like rule books:

```json
{
  "templates": {
    "rules_book": {
      "pages": [
        {
          "title": "Server Rules",
          "lines": [
            "&lServer Rules",
            "&r&c1. No griefing",
            "&c2. Be respectful"
          ]
        },
        {
          "title": "Page 2",
          "lines": [
            "&c3. No cheating",
            "&c4. Have fun!",
            "&c5. Follow staff instructions"
          ]
        }
      ]
    }
  }
}
```

## Template Management Commands

NeoEssentials provides commands for managing templates:

```
/neoessentials:template list
/neoessentials:template view <name>
/neoessentials:template reload
/neoessentials:template test <name> [player]
```

## Directory Organization

For advanced users, templates can be organized in subdirectories:

```
neoessentials/
├─ templates/
│  ├─ tablist/
│  │  ├─ headers.json
│  │  ├─ footers.json
│  ├─ messages/
│  │  ├─ welcome.json
│  │  ├─ rules.json
```

Accessing templates in subdirectories:

```
tablist/headers.default
messages/welcome.vip
```

## Best Practices

1. **Migration from TOML**: Move from TOML to JSON or YAML format for better support and features
2. **Template Organization**: Use sections to organize templates by purpose
3. **Variables**: Define common variables (colors, server name) at the template level
4. **Template Validation**: Use the test command to validate templates before deploying
5. **Template Reuse**: Leverage inheritance for consistent styling across templates
6. **Regular Backups**: Back up your templates regularly

## Troubleshooting

### Common Issues

- **Template Not Found**: Verify the template name and path
- **Formatting Issues**: Check for syntax errors in your JSON or YAML
- **Placeholders Not Working**: Confirm placeholders are spelled correctly
- **Inheritance Issues**: Ensure parent templates exist and are loaded

## Examples

### Tablist Header

```json
{
  "templates": {
    "tablist": {
      "header": {
        "lines": [
          "{gradient:#ff0000:#ff7700:#ffff00}&l=== YOUR SERVER NAME ==={/gradient}",
          "&eWelcome, &b{player}&e!",
          "&7Today is &f{date:dd/MM/yyyy}&7, it's &f{time:HH:mm}&7 server time",
          ""
        ]
      }
    }
  }
}
```

### Welcome Message

```json
{
  "templates": {
    "welcome": {
      "variables": {
        "border": "&6&l======================="
      },
      "lines": [
        "{border}",
        "&r",
        "{center}&b&lWELCOME TO THE SERVER{/center}",
        "&r",
        "{center}&eHello, &b{player}&e!{/center}",
        "{center}&eWe're glad to have you here!{/center}",
        "&r",
        "{center}&7Players online: &f{online_players}/{max_players}{/center}",
        "{center}&7Use &f/help &7for commands{/center}",
        "&r",
        "{border}"
      ]
    }
  }
}
```

## Additional Resources

- [Text Formatting Guide](Text-Formatting)
- [Placeholders](Placeholders)
- [JSON Templates Guide](JSON-Templates-Guide)
- [Animation System](Animation-System)
- [NeoEssentials Discord](https://discord.gg/dUGAQF2Mga) for template support
