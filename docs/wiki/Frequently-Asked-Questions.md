# Frequently Asked Questions

Common questions and answers about NeoEssentials v1.0.1.89+ including new animation features.

## 🎯 General Questions

### What is NeoEssentials?
NeoEssentials is a comprehensive server enhancement mod for NeoForge that provides essential features including **ultra-smooth tablist animations**, economy systems, teleportation, and advanced server management tools.

### What versions are supported?
- **Minecraft**: 1.21.1+  
- **NeoForge**: 52.1.1+
- **Java**: 17+ required

### Is NeoEssentials compatible with other mods?
Yes! NeoEssentials is designed to work alongside most popular mods including:
- **LuckPerms** (permissions)
- **FTB Ranks** (permissions)  
- **TAB** (can replace or enhance)
- **PlaceholderAPI** mods

### Where can I download it?
- [**CurseForge**](https://www.curseforge.com/minecraft/mc-mods/neoessentials) - Stable releases
- [**Modrinth**](https://modrinth.com/mod/neoessentials) - Alternative platform
- [**GitHub**](https://github.com/ZeroG-Network/NeoEssentials/releases) - Latest builds

## 🎨 Animation & Tablist Questions (NEW)

### Can I use multiple animations simultaneously?
**YES!** v1.0.1.89+ supports unlimited `<anim:name>` placeholders in the same tablist:
```yaml
headers:
  - "<anim:welcome> &a%player% <anim:pulse> Online: <anim:counter>"
```

### How smooth are the animations?
**Ultra-smooth!** Animations update every **25ms** (40 FPS) for professional quality, while maintaining excellent server performance.

### What's the three-tier update system?
NeoEssentials uses separate update intervals for optimal performance:
- **Animations**: 25ms (ultra-smooth frames)
- **Placeholders**: 250ms (dynamic data)  
- **Templates**: 3000ms (variety rotation)

### Can I create custom animations?
Absolutely! Create custom animations in `animations.yml`:
```yaml
my_animation:
  interval: 50
  frames:
    - "&aFrame 1"  
    - "&bFrame 2"
    - "&cFrame 3"
```

### Do animations impact server performance?
**Minimal impact!** The system is optimized for 100+ concurrent players with multiple animations running simultaneously.

## ⚙️ Configuration Questions

## Commands & Permissions

### What commands are available?
NeoEssentials includes over 50 essential commands for both players and administrators. See the [Commands Reference](Commands-Reference) for a complete list.

### How do permissions work?
NeoEssentials integrates with permission mods like LuckPerms and FTB Ranks. All permissions follow the format `neoessentials.command.commandname`. See the [Permissions Guide](Permissions-Guide) for details.

### Can I disable specific commands?
Yes, any command can be enabled or disabled in the `config/neoessentials/commands.toml` file.

## Economy System

### How does the economy system work?
NeoEssentials includes a complete economy system with player balances, transactions, and shop integration. See the [Economy System](Economy-System) guide for details.

### Can NeoEssentials integrate with other economy mods?
Yes, NeoEssentials can integrate with other economy mods through its API. See the [API Documentation](API-Documentation) for implementation details.

### How are player balances stored?
Player balances can be stored in:
- JSON files (default)
- SQLite database
- MySQL/MariaDB database

Configure storage options in `config/neoessentials/economy.toml`.

## Home & Warp System

### How many homes can players have?
The number of homes is permission-based. Configure limits with permissions like `neoessentials.limit.homes.5` for 5 homes or `neoessentials.limit.homes.unlimited` for unlimited homes.

### Can I set cooldowns for teleportation?
Yes, cooldowns can be configured in `config/neoessentials/teleport.toml` for homes, warps, and other teleportation commands.

### How do I create a warp?
Admins can create warps using `/setwarp <name>` and players can teleport to them with `/warp <name>` if they have permission.

## Technical Questions

### Does NeoEssentials work in singleplayer?
Yes, but it's primarily designed for multiplayer servers. Some features may have limited functionality in singleplayer.

### Will NeoEssentials conflict with other mods?
NeoEssentials is designed to be compatible with most mods. However, conflicts may occur with other server utility mods that provide similar functionality.

### Does NeoEssentials impact server performance?
NeoEssentials is optimized for minimal performance impact. Even on large servers with many players, the performance overhead should be negligible.

### Can I use NeoEssentials in a modpack?
Yes, NeoEssentials can be included in modpacks, subject to the terms of the MIT license.

## Support & Development

### How do I report a bug?
Report bugs on our [GitHub Issues](https://github.com/ZeroG-Network/NeoEssentials/issues) page. Please include detailed information about the issue, steps to reproduce, and relevant logs.

### How can I request a feature?
Feature requests can be submitted on our [GitHub Issues](https://github.com/ZeroG-Network/NeoEssentials/issues) page or discussed on our [Discord server](https://discord.gg/dUGAQF2Mga).

### How can I contribute to NeoEssentials?
Contributions are welcome! See our [Contributing Guide](Contributing-Guide) for more information about how to contribute code, documentation, or translations.

### Where can I get support?
Support is available on our [Discord server](https://discord.gg/dUGAQF2Mga) or through [GitHub Issues](https://github.com/ZeroG-Network/NeoEssentials/issues).

## Migration & Compatibility

### Can I migrate from EssentialsX or other essentials mods?
There's no direct migration tool, but many features are similar. See our [Migration Guide](Migration-Guide) for tips on moving from other essentials mods.

### Is NeoEssentials compatible with permission plugins?
Yes, NeoEssentials works with permission mods that implement the Forge/NeoForge permission API, such as LuckPerms and FTB Ranks.

### Will my configuration break when updating NeoEssentials?
We strive to maintain backward compatibility between versions. If breaking changes are necessary, we provide migration guides and tools to help update your configuration.
