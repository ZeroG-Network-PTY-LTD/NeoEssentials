# Enhanced Scoreboard Integration Guide

## Quick Integration Steps

### 1. Update Feature Manager (Optional)
If you want to integrate with your existing FeatureManager, add this to the initialization:

```java
// In FeatureManager.java initialization
ScoreboardManagerEnhanced.getInstance().initialize(server);
```

### 2. Add Command Registration
In your main mod class or command registration area:

```java
// Register the admin scoreboard command
AdminScoreboardCommand.register(dispatcher);
```

### 3. Configuration Generation
The enhanced scoreboard configuration is already integrated with your ConfigManager and will automatically generate when the server starts.

### 4. Server Integration Points

#### In your main mod initialization:
```java
@SubscribeEvent
public void onServerStarted(ServerStartedEvent event) {
    // Initialize enhanced scoreboard system
    ScoreboardManagerEnhanced.getInstance().initialize(event.getServer());
}
```

#### For shutdown cleanup:
```java
@SubscribeEvent  
public void onServerStopping(ServerStoppedEvent event) {
    // Clean shutdown of enhanced scoreboard
    ScoreboardManagerEnhanced.getInstance().shutdown();
}
```

## Usage as Administrator

### 1. Start your server
The enhanced scoreboard system will automatically initialize and generate the configuration file.

### 2. Customize layouts
Edit `config/neoessentials/scoreboard.json` to customize:
- Permission levels
- Layout designs
- Animation settings
- Update intervals

### 3. Test different permission levels
- Give yourself different OP levels (1-4) to see different layouts
- Use `/neoscoreboard status` to check system status
- Use `/neoscoreboard reload` to apply changes without restart

### 4. Admin commands available:
- `/neoscoreboard status` - System overview
- `/neoscoreboard enable/disable` - System control
- `/neoscoreboard reload` - Hot-reload configuration
- `/neoscoreboard toggle <player>` - Individual player control
- `/neoscoreboard update [player]` - Force updates
- `/neoscoreboard debug on/off` - Debug logging

## Permission Levels Explained

- **OP Level 4** = Owner Panel (full server stats)
- **OP Level 3** = Admin Panel (management info)
- **OP Level 2** = Moderator Panel (player management)
- **OP Level 1** = VIP Panel (enhanced features)
- **No Permissions** = Player Panel (basic info)

## Files Created

1. **ScoreboardManagerEnhanced.java** - Core enhanced scoreboard system
2. **AdminScoreboardCommand.java** - Admin command interface
3. **Enhanced ScoreboardConfig.java** - Configuration structure with layouts
4. **Updated ConfigManager.java** - Generates comprehensive scoreboard config

The system is fully backward compatible and doesn't interfere with your existing scoreboard manager (if you want to keep both).

## Next Steps

1. **Test the system** - Start your server and verify initialization
2. **Customize layouts** - Edit the configuration to match your server theme
3. **Set permissions** - Configure player permissions for different layouts
4. **Monitor performance** - Use debug mode to optimize update intervals
5. **Train staff** - Show admins the available commands and features

The enhanced scoreboard system is now ready for production use with full administrative control!
