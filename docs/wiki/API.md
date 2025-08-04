# API Documentation

The NeoEssentials API provides developers with comprehensive access to the mod's functionality for creating extensions, integrations, and custom features. This documentation covers all public APIs, events, and integration methods.

## 🎯 Overview

The NeoEssentials API offers:

- **Command System Integration**: Register custom commands with full feature parity
- **Permission System Access**: Advanced permission management and queries
- **Event System**: Comprehensive event handling for all mod activities
- **Placeholder System**: Register custom placeholders and data providers
- **Bossbar Management**: Programmatic bossbar creation and management
- **Security Integration**: Hook into security monitoring and response systems
- **Teleportation Services**: Advanced teleportation with safety and validation
- **Configuration Access**: Read and modify configuration settings

## 📚 Core API Components

### NeoEssentials Main API

#### Getting the API Instance

```java
// Get the main API instance
NeoEssentialsAPI api = NeoEssentialsAPI.getInstance();

// Check if NeoEssentials is available
if (NeoEssentialsAPI.isAvailable()) {
    // Safe to use API
}
```

#### API Availability

```java
// Event fired when API becomes available
@EventHandler
public void onNeoEssentialsLoad(NeoEssentialsLoadEvent event) {
    NeoEssentialsAPI api = event.getAPI();
    // Initialize your integration here
}

// Event fired when API is disabled
@EventHandler
public void onNeoEssentialsUnload(NeoEssentialsUnloadEvent event) {
    // Clean up your integration here
}
```

### Version and Compatibility

```java
// Get API version
String apiVersion = NeoEssentialsAPI.getAPIVersion();

// Check minimum API version
if (NeoEssentialsAPI.isAPIVersionAtLeast("1.0.0")) {
    // Use newer API features
}

// Get mod version
String modVersion = NeoEssentialsAPI.getModVersion();
```

## 🎮 Command System API

### Custom Command Registration

#### Basic Command Registration

```java
public class MyCustomCommand implements Command {
    
    @Override
    public String getName() {
        return "mycustom";
    }
    
    @Override
    public String getDescription() {
        return "My custom command description";
    }
    
    @Override
    public String getUsage() {
        return "/mycustom [player]";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("mc", "custom");
    }
    
    @Override
    public String getPermission() {
        return "essentials.mycustom";
    }
    
    @Override
    public boolean execute(CommandContext context) {
        Player player = context.getPlayer();
        String[] args = context.getArgs();
        
        // Command implementation
        player.sendMessage("Custom command executed!");
        return true;
    }
    
    @Override
    public List<String> tabComplete(CommandContext context) {
        // Tab completion implementation
        return Arrays.asList("option1", "option2", "option3");
    }
}

// Register the command
CommandRegistry registry = api.getCommandRegistry();
registry.registerCommand(new MyCustomCommand());
```

#### Advanced Command Features

```java
public class AdvancedCommand implements Command {
    
    @Override
    public boolean execute(CommandContext context) {
        Player player = context.getPlayer();
        String[] args = context.getArgs();
        
        // Permission checking
        if (!context.hasPermission("essentials.advanced.use")) {
            context.sendMessage("&cYou don't have permission!");
            return false;
        }
        
        // Cooldown checking
        CooldownManager cooldowns = api.getCooldownManager();
        if (cooldowns.hasCooldown(player, "advanced_command")) {
            long remaining = cooldowns.getRemainingCooldown(player, "advanced_command");
            context.sendMessage("&cCooldown active: " + remaining + "s remaining");
            return false;
        }
        
        // Set cooldown
        cooldowns.setCooldown(player, "advanced_command", Duration.ofMinutes(5));
        
        // Placeholder support
        PlaceholderManager placeholders = api.getPlaceholderManager();
        String message = placeholders.parsePlaceholders(player, 
            "Hello {player_name}! Server TPS: {server_tps}");
        context.sendMessage(message);
        
        return true;
    }
}
```

#### Sub-command System

```java
public class MainCommand implements Command {
    
    private final Map<String, SubCommand> subCommands = new HashMap<>();
    
    public MainCommand() {
        subCommands.put("info", new InfoSubCommand());
        subCommands.put("reload", new ReloadSubCommand());
        subCommands.put("debug", new DebugSubCommand());
    }
    
    @Override
    public boolean execute(CommandContext context) {
        String[] args = context.getArgs();
        
        if (args.length == 0) {
            showHelp(context);
            return true;
        }
        
        SubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand == null) {
            context.sendMessage("&cUnknown subcommand: " + args[0]);
            return false;
        }
        
        // Create sub-command context
        CommandContext subContext = context.createSubContext(1);
        return subCommand.execute(subContext);
    }
    
    @Override
    public List<String> tabComplete(CommandContext context) {
        String[] args = context.getArgs();
        
        if (args.length <= 1) {
            return subCommands.keySet().stream()
                .filter(cmd -> cmd.startsWith(args.length > 0 ? args[0] : ""))
                .collect(Collectors.toList());
        }
        
        SubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand != null) {
            return subCommand.tabComplete(context.createSubContext(1));
        }
        
        return Collections.emptyList();
    }
}
```

## 🔐 Permission System API

### Permission Management

#### Basic Permission Operations

```java
PermissionManager permissions = api.getPermissionManager();

// Check if player has permission
boolean hasPermission = permissions.hasPermission(player, "essentials.fly");

// Check with context
PermissionContext context = PermissionContext.builder()
    .player(player)
    .world(player.getWorld())
    .build();
boolean hasWorldPermission = permissions.hasPermission(context, "essentials.build");

// Get player's permission group
Optional<PermissionGroup> group = permissions.getPlayerGroup(player);

// Get all player permissions
Set<String> playerPermissions = permissions.getPlayerPermissions(player);
```

#### Advanced Permission Queries

```java
// Get effective permissions (including inherited)
Set<String> effectivePermissions = permissions.getEffectivePermissions(player);

// Check wildcard permissions
boolean hasWildcard = permissions.hasWildcard(player, "essentials.*");

// Get permission value with meta
PermissionResult result = permissions.checkPermissionWithMeta(player, "essentials.homes.limit");
if (result.hasValue()) {
    int homeLimit = result.getIntValue(1); // Default to 1
}

// Temporary permissions
permissions.addTemporaryPermission(player, "essentials.fly", Duration.ofHours(1));
boolean hasTemporary = permissions.hasTemporaryPermission(player, "essentials.fly");
```

#### Group Management

```java
// Create new permission group
PermissionGroup.Builder builder = PermissionGroup.builder()
    .name("custom_vip")
    .displayName("&6Custom VIP")
    .priority(20)
    .addPermission("essentials.fly")
    .addPermission("essentials.heal")
    .setInheritance("default");

PermissionGroup customGroup = builder.build();
permissions.createGroup(customGroup);

// Modify existing group
Optional<PermissionGroup> vipGroup = permissions.getGroup("vip");
if (vipGroup.isPresent()) {
    permissions.addGroupPermission("vip", "essentials.speed");
    permissions.setGroupInheritance("vip", "default");
}

// Set player group
permissions.setPlayerGroup(player, "custom_vip");
```

### Permission Events

```java
// Listen for permission changes
@EventHandler
public void onPlayerPermissionChange(PlayerPermissionChangeEvent event) {
    Player player = event.getPlayer();
    String permission = event.getPermission();
    boolean hasPermission = event.hasPermission();
    
    // React to permission changes
    if (permission.equals("essentials.fly") && !hasPermission) {
        // Remove flight if permission removed
        player.setFlying(false);
    }
}

// Listen for group changes
@EventHandler
public void onPlayerGroupChange(PlayerGroupChangeEvent event) {
    Player player = event.getPlayer();
    String oldGroup = event.getOldGroup();
    String newGroup = event.getNewGroup();
    
    // Handle group promotion/demotion
    sendGroupChangeMessage(player, oldGroup, newGroup);
}
```

## 📊 Placeholder System API

### Custom Placeholder Registration

#### Simple Placeholder

```java
PlaceholderManager placeholders = api.getPlaceholderManager();

// Register simple placeholder
placeholders.registerPlaceholder("server_uptime", (player, params) -> {
    long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
    return formatUptime(uptime);
});

// Register player-specific placeholder
placeholders.registerPlaceholder("player_play_time", (player, params) -> {
    if (player == null) return "Unknown";
    
    long playTime = getPlayerPlayTime(player);
    return formatTime(playTime);
});
```

#### Advanced Placeholder with Parameters

```java
// Register placeholder with parameter support
placeholders.registerPlaceholder("player_stat", (player, params) -> {
    if (player == null || params.isEmpty()) return "0";
    
    String statName = params.get(0);
    PlayerStatistics stats = getPlayerStats(player);
    
    switch (statName.toLowerCase()) {
        case "deaths":
            return String.valueOf(stats.getDeaths());
        case "kills":
            return String.valueOf(stats.getKills());
        case "blocks_broken":
            return String.valueOf(stats.getBlocksBroken());
        default:
            return "Unknown stat";
    }
});

// Usage: {player_stat_deaths}, {player_stat_kills}
```

#### Placeholder Expansion

```java
public class CustomPlaceholderExpansion implements PlaceholderExpansion {
    
    @Override
    public String getIdentifier() {
        return "mymod";
    }
    
    @Override
    public String getDescription() {
        return "My custom mod placeholders";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String onRequest(Player player, String params) {
        if (params.equals("custom_data")) {
            return getCustomData(player);
        }
        
        if (params.startsWith("formatted_")) {
            String value = params.substring(10);
            return formatValue(value);
        }
        
        return null; // Unknown placeholder
    }
}

// Register expansion
placeholders.registerExpansion(new CustomPlaceholderExpansion());
// Usage: {mymod_custom_data}, {mymod_formatted_value}
```

#### Cached Placeholders

```java
// Register cached placeholder for expensive operations
placeholders.registerCachedPlaceholder("server_tps_detailed", 
    Duration.ofSeconds(5), // Cache for 5 seconds
    (player, params) -> {
        // Expensive TPS calculation
        double[] tps = calculateDetailedTPS();
        return String.format("1m: %.2f, 5m: %.2f, 15m: %.2f", 
            tps[0], tps[1], tps[2]);
    });
```

### Placeholder Usage

```java
// Parse placeholders in text
String rawText = "Hello {player_name}! Server TPS: {server_tps}";
String parsed = placeholders.parsePlaceholders(player, rawText);

// Parse with custom context
PlaceholderContext context = PlaceholderContext.builder()
    .player(player)
    .world(player.getWorld())
    .location(player.getLocation())
    .addCustomData("extra_info", "custom_value")
    .build();

String contextParsed = placeholders.parsePlaceholders(context, rawText);

// Check if placeholder exists
if (placeholders.hasPlaceholder("custom_placeholder")) {
    // Safe to use
}
```

## 📢 Bossbar System API

### Bossbar Management

#### Creating Bossbars

```java
BossbarManager bossbars = api.getBossbarManager();

// Create simple bossbar
Bossbar bossbar = bossbars.createBossbar("welcome", 
    "Welcome to {server_name}!", 
    BossbarColor.BLUE, 
    BossbarStyle.SOLID);

// Show to player
bossbars.showBossbar(player, bossbar);

// Create advanced bossbar
Bossbar advancedBar = BossbarBuilder.create("progress")
    .text("Progress: {progress}%")
    .color(BossbarColor.GREEN)
    .style(BossbarStyle.SEGMENTED_10)
    .progress(0.0f)
    .visible(true)
    .placeholder("progress", () -> String.valueOf(getCurrentProgress()))
    .build();

bossbars.registerBossbar(advancedBar);
```

#### Dynamic Bossbar Updates

```java
// Update bossbar properties
bossbars.updateBossbarText("progress", "Loading: {progress}%");
bossbars.updateBossbarProgress("progress", 0.75f);
bossbars.updateBossbarColor("progress", BossbarColor.YELLOW);

// Conditional bossbar display
bossbars.showBossbarIf("server_info", player, () -> {
    // Only show if player has permission
    return player.hasPermission("essentials.bossbar.info");
});

// Timed bossbar
bossbars.showBossbarTemporary(player, "temp_message", 
    "Temporary notification!", Duration.ofSeconds(10));
```

#### Bossbar Templates

```java
// Register bossbar template
BossbarTemplate template = BossbarTemplate.builder()
    .name("event_announcement")
    .text("&6Event: {event_name} starting in {event_time}!")
    .color(BossbarColor.YELLOW)
    .style(BossbarStyle.SOLID)
    .duration(Duration.ofSeconds(30))
    .sound("BLOCK_NOTE_BLOCK_PLING")
    .build();

bossbars.registerTemplate(template);

// Use template
Map<String, String> variables = Map.of(
    "event_name", "PvP Tournament",
    "event_time", "5 minutes"
);

bossbars.showTemplate(player, "event_announcement", variables);
```

### Bossbar Events

```java
// Listen for bossbar events
@EventHandler
public void onBossbarShow(BossbarShowEvent event) {
    Player player = event.getPlayer();
    Bossbar bossbar = event.getBossbar();
    
    // Log bossbar displays
    logBossbarActivity(player, bossbar, "SHOW");
}

@EventHandler
public void onBossbarHide(BossbarHideEvent event) {
    Player player = event.getPlayer();
    String bossbarId = event.getBossbarId();
    
    // Cleanup when bossbar is hidden
    cleanupBossbarData(player, bossbarId);
}
```

## 🔒 Security System API

### Security Monitoring

#### Custom Threat Detection

```java
SecurityManager security = api.getSecurityManager();

// Register custom threat detector
security.registerThreatDetector("custom_griefing", new ThreatDetector() {
    @Override
    public ThreatLevel analyzeThreat(SecurityContext context) {
        Player player = context.getPlayer();
        SecurityData data = context.getSecurityData();
        
        // Custom threat analysis
        int blocksPerMinute = data.getBlocksPlacedLastMinute();
        if (blocksPerMinute > 500) {
            return ThreatLevel.HIGH;
        } else if (blocksPerMinute > 200) {
            return ThreatLevel.MEDIUM;
        }
        
        return ThreatLevel.NONE;
    }
    
    @Override
    public String getDescription() {
        return "Detects rapid block placement (potential grief)";
    }
});

// Register custom response action
security.registerResponseAction("custom_restriction", new ResponseAction() {
    @Override
    public void execute(SecurityIncident incident) {
        Player player = incident.getPlayer();
        
        // Apply custom restriction
        applyBuildingRestriction(player, Duration.ofMinutes(10));
        
        // Notify staff
        notifyStaff("Player " + player.getName() + " restricted for griefing");
    }
});
```

#### Security Event Handling

```java
// Listen for security events
@EventHandler
public void onThreatDetected(ThreatDetectedEvent event) {
    Player player = event.getPlayer();
    ThreatLevel level = event.getThreatLevel();
    String detectorName = event.getDetectorName();
    
    // Custom threat response
    if (level == ThreatLevel.HIGH && detectorName.equals("custom_griefing")) {
        // Take immediate action
        teleportToSafety(player);
        alertAdministrators(player, "High threat detected");
    }
}

@EventHandler
public void onSecurityIncident(SecurityIncidentEvent event) {
    SecurityIncident incident = event.getIncident();
    
    // Log to external system
    logToExternalSecuritySystem(incident);
    
    // Update player risk score
    updatePlayerRiskScore(incident.getPlayer(), incident.getThreatLevel());
}
```

#### Player Security Analysis

```java
// Get player security status
SecurityStatus status = security.getPlayerSecurityStatus(player);
int riskScore = status.getRiskScore();
List<SecurityFlag> flags = status.getActiveFlags();
boolean isUnderInvestigation = status.isUnderInvestigation();

// Security history
List<SecurityIncident> history = security.getPlayerSecurityHistory(player);
SecurityIncident lastIncident = security.getLastIncident(player);

// Add security flag
security.addSecurityFlag(player, SecurityFlag.SUSPICIOUS_MOVEMENT, 
    "Detected impossible movement speed", Duration.ofHours(1));

// Remove security flag
security.removeSecurityFlag(player, SecurityFlag.COMMAND_ABUSE);
```

## 🌐 Teleportation API

### Teleportation Services

#### Basic Teleportation

```java
TeleportationManager teleport = api.getTeleportationManager();

// Simple teleportation
Location destination = new Location(world, x, y, z);
TeleportResult result = teleport.teleport(player, destination);

if (result.isSuccess()) {
    player.sendMessage("Teleported successfully!");
} else {
    player.sendMessage("Teleportation failed: " + result.getFailureReason());
}

// Teleportation with options
TeleportOptions options = TeleportOptions.builder()
    .cause(TeleportCause.PLUGIN)
    .delay(Duration.ofSeconds(3))
    .cancelOnMove(true)
    .cancelOnDamage(true)
    .safetyCheck(true)
    .bypassRestrictions(false)
    .build();

teleport.teleport(player, destination, options);
```

#### Advanced Teleportation Features

```java
// Safe teleportation (finds safe location)
Optional<Location> safeLocation = teleport.findSafeLocation(destination);
if (safeLocation.isPresent()) {
    teleport.teleport(player, safeLocation.get());
}

// Cross-dimensional teleportation
CrossDimensionalTeleport crossTeleport = teleport.createCrossDimensionalTeleport()
    .from(player.getLocation())
    .to(netherLocation)
    .convertCoordinates(true)
    .createPlatform(true)
    .build();

crossTeleport.execute(player);

// Batch teleportation
List<Player> players = Arrays.asList(player1, player2, player3);
teleport.teleportGroup(players, destination, options);
```

#### Home and Warp Management

```java
HomeManager homes = api.getHomeManager();
WarpManager warps = api.getWarpManager();

// Home management
homes.setHome(player, "base", player.getLocation());
Optional<Location> home = homes.getHome(player, "base");
List<String> homeNames = homes.getHomeNames(player);
int homeLimit = homes.getHomeLimit(player);

// Warp management
warps.createWarp("custom_warp", location, "Custom teleport point");
Optional<Warp> warp = warps.getWarp("custom_warp");
List<Warp> allWarps = warps.getAllWarps();
warps.deleteWarp("old_warp");
```

### Teleportation Events

```java
// Pre-teleport event (cancellable)
@EventHandler
public void onPreTeleport(PreTeleportEvent event) {
    Player player = event.getPlayer();
    Location destination = event.getDestination();
    TeleportCause cause = event.getCause();
    
    // Custom validation
    if (isRestrictedArea(destination)) {
        event.setCancelled(true);
        event.setCancelReason("Destination is restricted!");
    }
}

// Post-teleport event
@EventHandler
public void onTeleport(TeleportEvent event) {
    Player player = event.getPlayer();
    Location from = event.getFrom();
    Location to = event.getTo();
    
    // Log teleportation
    logTeleportation(player, from, to);
    
    // Apply arrival effects
    player.spawnParticle(Particle.PORTAL, to, 20);
}
```

## ⚙️ Configuration API

### Configuration Access

#### Reading Configuration

```java
ConfigurationManager config = api.getConfigurationManager();

// Get configuration sections
ConfigurationSection commandsConfig = config.getSection("commands");
ConfigurationSection securityConfig = config.getSection("security");

// Read values with defaults
boolean enableSecurity = config.getBoolean("security.enabled", true);
int maxHomes = config.getInt("teleportation.homes.defaultLimit", 1);
String serverName = config.getString("general.serverName", "Minecraft Server");

// Get complex objects
List<String> blockedCommands = config.getStringList("security.blockedCommands");
Map<String, Object> permissions = config.getConfigurationSection("permissions").getValues(false);
```

#### Modifying Configuration

```java
// Update configuration values
config.set("general.serverName", "My Awesome Server");
config.set("security.rateLimiting.enabled", true);

// Save configuration
config.save();

// Reload configuration
config.reload();

// Listen for configuration changes
@EventHandler
public void onConfigReload(ConfigurationReloadEvent event) {
    // React to configuration changes
    updateInternalSettings();
}
```

#### Custom Configuration Files

```java
// Create custom configuration file
CustomConfiguration myConfig = config.createCustomConfiguration("myintegration.toml");

// Set default values
myConfig.setDefault("integration.enabled", true);
myConfig.setDefault("integration.apiKey", "");
myConfig.setDefault("integration.endpoints", Arrays.asList("https://api.example.com"));

// Load with defaults
myConfig.loadWithDefaults();

// Access values
boolean enabled = myConfig.getBoolean("integration.enabled");
String apiKey = myConfig.getString("integration.apiKey");
```

## 🔌 Event System

### Core Events

#### Mod Lifecycle Events

```java
// Mod initialization
@EventHandler
public void onNeoEssentialsInit(NeoEssentialsInitEvent event) {
    // Called when NeoEssentials initializes
    registerIntegrations();
}

// Mod shutdown
@EventHandler
public void onNeoEssentialsShutdown(NeoEssentialsShutdownEvent event) {
    // Called when NeoEssentials shuts down
    cleanupIntegrations();
}

// Configuration reload
@EventHandler
public void onConfigReload(ConfigurationReloadEvent event) {
    // Called when configuration is reloaded
    updateSettings();
}
```

#### Command Events

```java
// Command execution
@EventHandler
public void onCommandExecute(CommandExecuteEvent event) {
    Player player = event.getPlayer();
    String command = event.getCommand();
    String[] args = event.getArgs();
    
    // Log command usage
    logCommandUsage(player, command, args);
}

// Command registration
@EventHandler
public void onCommandRegister(CommandRegisterEvent event) {
    Command command = event.getCommand();
    
    // React to new commands being registered
    updateCommandHelp(command);
}
```

### Custom Events

#### Creating Custom Events

```java
public class CustomIntegrationEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final String integrationType;
    private final Map<String, Object> data;
    
    public CustomIntegrationEvent(Player player, String integrationType, Map<String, Object> data) {
        this.player = player;
        this.integrationType = integrationType;
        this.data = data;
    }
    
    public Player getPlayer() { return player; }
    public String getIntegrationType() { return integrationType; }
    public Map<String, Object> getData() { return data; }
    
    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    
    public static HandlerList getHandlerList() { return HANDLERS; }
}

// Fire custom event
CustomIntegrationEvent event = new CustomIntegrationEvent(player, "discord", dataMap);
Bukkit.getPluginManager().callEvent(event);
```

## 🔧 Integration Examples

### Discord Integration

```java
public class DiscordIntegration {
    
    private final NeoEssentialsAPI api;
    private final DiscordBot bot;
    
    public DiscordIntegration() {
        this.api = NeoEssentialsAPI.getInstance();
        this.bot = new DiscordBot();
        
        setupIntegration();
    }
    
    private void setupIntegration() {
        // Listen for security events
        api.getEventBus().subscribe(ThreatDetectedEvent.class, this::onThreatDetected);
        
        // Register Discord commands
        bot.registerCommand("!online", this::handleOnlineCommand);
        bot.registerCommand("!ban", this::handleBanCommand);
        
        // Register Discord placeholder
        api.getPlaceholderManager().registerPlaceholder("discord_members", 
            (player, params) -> String.valueOf(bot.getMemberCount()));
    }
    
    private void onThreatDetected(ThreatDetectedEvent event) {
        if (event.getThreatLevel() == ThreatLevel.HIGH) {
            String message = String.format("🚨 High threat detected: %s (%s)", 
                event.getPlayer().getName(), event.getDetectorName());
            bot.sendAdminAlert(message);
        }
    }
    
    private void handleOnlineCommand(DiscordMessage message) {
        List<Player> players = Bukkit.getOnlinePlayers().stream()
            .collect(Collectors.toList());
        
        String response = String.format("Online players (%d): %s", 
            players.size(), 
            players.stream().map(Player::getName).collect(Collectors.joining(", ")));
        
        message.reply(response);
    }
}
```

### Economy Integration

```java
public class EconomyIntegration {
    
    private final NeoEssentialsAPI api;
    private final Economy economy;
    
    public EconomyIntegration(Economy economy) {
        this.api = NeoEssentialsAPI.getInstance();
        this.economy = economy;
        
        setupIntegration();
    }
    
    private void setupIntegration() {
        // Add economy cost to teleportation
        api.getEventBus().subscribe(PreTeleportEvent.class, this::checkTeleportCost);
        
        // Register economy placeholders
        api.getPlaceholderManager().registerPlaceholder("player_balance", 
            (player, params) -> economy.format(economy.getBalance(player)));
        
        // Register economy commands
        api.getCommandRegistry().registerCommand(new BalanceCommand(economy));
        api.getCommandRegistry().registerCommand(new PayCommand(economy));
    }
    
    private void checkTeleportCost(PreTeleportEvent event) {
        Player player = event.getPlayer();
        TeleportCause cause = event.getCause();
        
        // Only charge for certain teleport types
        if (cause == TeleportCause.COMMAND || cause == TeleportCause.WARP) {
            double cost = getTeleportCost(cause);
            
            if (economy.getBalance(player) < cost) {
                event.setCancelled(true);
                event.setCancelReason("Insufficient funds! Cost: " + economy.format(cost));
            } else {
                economy.withdrawPlayer(player, cost);
                player.sendMessage("Teleportation cost: " + economy.format(cost));
            }
        }
    }
}
```

### Database Integration

```java
public class DatabaseIntegration {
    
    private final NeoEssentialsAPI api;
    private final Database database;
    
    public DatabaseIntegration() {
        this.api = NeoEssentialsAPI.getInstance();
        this.database = connectToDatabase();
        
        setupIntegration();
    }
    
    private void setupIntegration() {
        // Log all teleportations
        api.getEventBus().subscribe(TeleportEvent.class, this::logTeleportation);
        
        // Log security incidents
        api.getEventBus().subscribe(SecurityIncidentEvent.class, this::logSecurityIncident);
        
        // Register database placeholders
        api.getPlaceholderManager().registerPlaceholder("player_total_teleports", 
            (player, params) -> String.valueOf(getTeleportCount(player)));
    }
    
    private void logTeleportation(TeleportEvent event) {
        CompletableFuture.runAsync(() -> {
            try {
                database.execute(
                    "INSERT INTO teleportations (player_id, from_world, from_x, from_y, from_z, " +
                    "to_world, to_x, to_y, to_z, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    event.getPlayer().getUniqueId().toString(),
                    event.getFrom().getWorld().getName(),
                    event.getFrom().getX(),
                    event.getFrom().getY(),
                    event.getFrom().getZ(),
                    event.getTo().getWorld().getName(),
                    event.getTo().getX(),
                    event.getTo().getY(),
                    event.getTo().getZ(),
                    System.currentTimeMillis()
                );
            } catch (SQLException e) {
                api.getLogger().error("Failed to log teleportation", e);
            }
        });
    }
}
```

## 🐛 Debugging & Testing

### Debug API

```java
DebugManager debug = api.getDebugManager();

// Enable debug mode
debug.setDebugMode(true);

// Log debug information
debug.logDebug("Custom integration", "Processing player data for " + player.getName());

// Debug timing
DebugTimer timer = debug.startTimer("custom_operation");
// ... perform operation ...
timer.stop(); // Automatically logs execution time

// Debug player state
debug.dumpPlayerState(player); // Logs comprehensive player information

// Debug system state
debug.dumpSystemState(); // Logs system performance and statistics
```

### Testing Utilities

```java
TestingManager testing = api.getTestingManager();

// Create test player
TestPlayer testPlayer = testing.createTestPlayer("TestUser");

// Simulate events
testing.simulateCommand(testPlayer, "heal");
testing.simulateTeleport(testPlayer, testLocation);
testing.simulateSecurityEvent(testPlayer, ThreatLevel.MEDIUM);

// Assert expected behavior
testing.assertPlayerHealth(testPlayer, 20.0);
testing.assertPlayerLocation(testPlayer, testLocation);
testing.assertPermission(testPlayer, "essentials.heal");

// Cleanup
testing.removeTestPlayer(testPlayer);
```

## 📝 Best Practices

### API Usage Guidelines

1. **Always check API availability** before using:
   ```java
   if (NeoEssentialsAPI.isAvailable()) {
       // Use API
   }
   ```

2. **Handle API events properly**:
   ```java
   @EventHandler
   public void onAPILoad(NeoEssentialsLoadEvent event) {
       // Initialize integration
   }
   
   @EventHandler
   public void onAPIUnload(NeoEssentialsUnloadEvent event) {
       // Cleanup integration
   }
   ```

3. **Use async operations for heavy tasks**:
   ```java
   CompletableFuture.runAsync(() -> {
       // Heavy database operation
   }).thenRun(() -> {
       // Update UI on main thread
   });
   ```

4. **Implement proper error handling**:
   ```java
   try {
       api.getPermissionManager().setPlayerGroup(player, "vip");
   } catch (PermissionException e) {
       logger.warn("Failed to set player group", e);
       player.sendMessage("Group change failed: " + e.getMessage());
   }
   ```

5. **Clean up resources**:
   ```java
   @EventHandler
   public void onDisable(PluginDisableEvent event) {
       // Unregister placeholders
       api.getPlaceholderManager().unregisterExpansion(myExpansion);
       
       // Cancel scheduled tasks
       scheduler.cancelAllTasks();
       
       // Close database connections
       database.close();
   }
   ```

### Performance Considerations

- **Cache expensive operations**: Use the caching API for expensive calculations
- **Minimize event handling**: Only listen for events you actually need
- **Use bulk operations**: When possible, use batch operations for multiple players
- **Implement rate limiting**: Prevent abuse of your custom features
- **Monitor resource usage**: Use the performance monitoring tools

---

**Related Documentation**: [Installation](Installation.md) | [Configuration](Configuration.md) | [Essential Commands](Essential-Commands.md)

*Last Updated: August 3, 2025*
