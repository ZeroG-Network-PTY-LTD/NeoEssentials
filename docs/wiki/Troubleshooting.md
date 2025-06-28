# Troubleshooting

Complete troubleshooting guide for NeoEssentials v1.0.1.89+ including solutions for animation issues and performance problems.

## 🚨 Critical Issues (v1.0.1.89+)

### Animation & Tablist Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| **Animations not showing** | Missing animation definitions | Check `animations.yml` exists and contains your animations |
| **Multiple animations conflicting** | Old version or config issue | Update to v1.0.1.89+, regex pattern is fixed |
| **Animation placeholders showing as text** | Wrong format or typo | Use `<anim:name>` format, check spelling exactly |
| **Tablist performance issues** | Too many/fast animations | Increase `animation_frame_interval` from 25ms to 50ms+ |
| **Animation freezing** | Server TPS drop | Check `/tps` and adjust intervals in `tablist.yml` |

### Performance Optimization
| Issue | Cause | Solution |
|-------|-------|----------|
| **Server lag with animations** | Too aggressive intervals | Use recommended: 25ms animation, 250ms placeholders, 3000ms templates |
| **High memory usage** | Animation cache buildup | Enable cache cleanup: `/neoessentials reload` |
| **TPS drops** | Multiple heavy animations | Limit to 3-5 simultaneous animations per player |

## 🔧 Common Installation Issues

### Mod Loading Problems
| Issue | Solution |
|-------|----------|
| **Mod won't load** | Verify NeoForge 52.1.1+ and Minecraft 1.21.1+ compatibility |
| **Missing dependencies** | NeoEssentials is standalone - check for conflicting tablist mods |
| **Class not found errors** | Delete old versions from `mods/` folder completely |
| **Crash on startup** | Check `latest.log` for specific errors, ensure Java 17+ |

### Configuration Problems  
| Issue | Solution |
|-------|----------|
| **Config files not generating** | Check server write permissions to `config/` and `neoessentials/` directories |
| **Changes not applying** | Use `/neoessentials reload` or restart server |
| **YAML parse errors** | Validate syntax at [yamllint.com](http://www.yamllint.com) - check indentation |
| **Animation definitions not loading** | Ensure `animations.yml` is in `neoessentials/` directory (server root) |

## 🎯 Animation System Debugging

### Debug Commands
```bash
/tablist debug           # Show performance metrics
/tablist test <name>     # Test specific animation  
/tablist animations      # List all loaded animations
/neoessentials debug     # Enable debug logging
```

### Common Animation Issues
| Issue | Fix |
|-------|-----|
| **Animation name 'xxx>' not found** | Fixed in v1.0.1.89 - update mod |
| **Only first animation works** | Multiple placeholder support fixed in v1.0.1.89 |
| **Animations too slow/fast** | Adjust `interval` value in `animations.yml` |
| **Animation frames corrupted** | Check YAML indentation and special characters |

### Home/Warp Issues

| Issue | Solution |
|-------|----------|
| **Homes not saving** | Check storage configuration and file permissions. Try setting a home with `/sethome` again. |
| **Warps not working** | Ensure warp points are properly set up and that players have the correct permissions. |
| **Teleport failures** | Some teleports may fail due to safety checks. Configure teleport safety options in the config. |

## Server Log Analysis

When troubleshooting, always check the server logs in `logs/latest.log` for error messages related to NeoEssentials. Look for lines containing:

- `[NeoEssentials]`
- `ERROR` or `WARN`
- Java exceptions or stack traces

## Diagnostic Commands

Use these commands to diagnose issues:

```
/neoessentials debug info    - Shows system information
/neoessentials debug config  - Displays configuration status
/neoessentials debug perms   - Shows permission information
/neoessentials debug storage - Tests storage systems
```

## Configuration Validation

Run the validation tool to check your configuration files:

```
/neoessentials validate
```

This will scan all configuration files and report any issues.

## Resetting to Defaults

If you're experiencing persistent issues, you can reset to default configurations:

1. Stop the server
2. Rename or delete the affected config files
3. Start the server - new default files will be generated

## Common File Locations

| Issue Type | Files to Check |
|------------|----------------|
| **Basic Settings** | `config/neoessentials/*.toml` |
| **Templates** | `neoessentials/templates.json` or `templates.yml` |
| **Animations** | `neoessentials/animations.json` or `animations.yml` |
| **Player Data** | `data/neoessentials/players/` |
| **Economy Data** | `data/neoessentials/economy/` |
| **Home Data** | `data/neoessentials/homes/` |
| **Warp Data** | `data/neoessentials/warps/` |

## Getting Help

If you're still experiencing issues:

1. **Discord Support**: Join our [Discord server](https://discord.gg/dUGAQF2Mga) for live support.
2. **GitHub Issues**: Create an issue on our [GitHub repository](https://github.com/ZeroG-Network/NeoEssentials/issues) with detailed information about your problem.
3. **Server Logs**: Always include relevant parts of your server logs when seeking help.

## Advanced Troubleshooting

For advanced users:

1. **Debug Mode**: Enable debug logging in `config/neoessentials/general.toml`:
   ```toml
   [logging]
   enableDebugLogging = true
   ```

2. **Profiling**: Enable performance profiling:
   ```toml
   [performance]
   enableProfiling = true
   ```

3. **Test Server**: Set up a separate test server to isolate issues before applying changes to your main server.
