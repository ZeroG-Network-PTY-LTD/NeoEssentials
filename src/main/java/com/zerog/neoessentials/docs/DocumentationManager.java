package com.zerog.neoessentials.docs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages built-in documentation for the NeoEssentials dashboard.
 * Provides comprehensive documentation including API references, tutorials, FAQs, and guides.
 */
public class DocumentationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentationManager.class);
    private static DocumentationManager instance;
    @SuppressWarnings("unused") // Reserved for future JSON serialization features
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    // Documentation storage
    private final Map<String, DocumentationSection> sections = new LinkedHashMap<>();
    private final Map<String, ApiEndpoint> apiEndpoints = new LinkedHashMap<>();
    private final List<Tutorial> tutorials = new ArrayList<>();
    private final List<FaqItem> faqItems = new ArrayList<>();
    private final List<VideoTutorial> videoTutorials = new ArrayList<>();
    
    // Documentation directory
    private final Path docsDir = Paths.get("config", "neoessentials", "webdashboard", "docs");
    
    private DocumentationManager() {}
    
    public static synchronized DocumentationManager getInstance() {
        if (instance == null) {
            instance = new DocumentationManager();
        }
        return instance;
    }
    
    /**
     * Initialize the documentation system
     */
    public void initialize() {
        NeoLog.info(LOGGER, LogCategory.GENERAL, "Initializing NeoEssentials Documentation System...");
        
        // Create docs directory
        try {
            Files.createDirectories(docsDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create documentation directory", e);
        }
        
        // Load or create default documentation
        loadDocumentation();
        
        NeoLog.info(LOGGER, LogCategory.GENERAL, "Documentation system initialized with {} sections, {} API endpoints, {} tutorials, {} FAQs",
                sections.size(), apiEndpoints.size(), tutorials.size(), faqItems.size());
    }
    
    /**
     * Load documentation from files or create defaults
     */
    private void loadDocumentation() {
        loadOrCreateSections();
        loadOrCreateApiDocumentation();
        loadOrCreateTutorials();
        loadOrCreateFaqItems();
        loadOrCreateVideoTutorials();
    }
    
    /**
     * Load or create documentation sections
     */
    private void loadOrCreateSections() {
        sections.put("getting-started", new DocumentationSection(
                "getting-started",
                "Getting Started",
                "Quick start guide to using the NeoEssentials dashboard",
                """
                # Getting Started with NeoEssentials Dashboard
                
                Welcome to the NeoEssentials Web Dashboard! This comprehensive administration panel allows you to manage your Minecraft server from anywhere.
                
                ## Accessing the Dashboard
                
                1. Start your Minecraft server with NeoEssentials installed
                2. Open your web browser and navigate to: `http://localhost:8080`
                3. Log in with your administrator credentials
                
                ## Dashboard Overview
                
                The dashboard provides access to:
                - **Player Management**: View online players, manage inventories, and moderate users
                - **Server Control**: Monitor performance, view logs, execute commands
                - **Configuration**: Edit settings, manage permissions, configure features
                - **Database Tools**: Browse and query SQLite databases
                - **World Management**: Control world settings and properties
                
                ## First Steps
                
                1. **Change Default Password**: Navigate to Settings → Security
                2. **Configure Permissions**: Set up user roles and permissions
                3. **Review Settings**: Check configuration in Settings → General
                4. **Explore Features**: Browse through the navigation menu to discover available tools
                
                ## Need Help?
                
                - Check the FAQ section for common questions
                - Browse API Documentation for integration details
                - Watch video tutorials for step-by-step guides
                - Review feature tutorials for detailed instructions
                """,
                1
        ));
        
        sections.put("features", new DocumentationSection(
                "features",
                "Features Overview",
                "Complete list of dashboard features and capabilities",
                """
                # NeoEssentials Dashboard Features
                
                ## Player Management
                - **User Management**: Create, edit, and delete user accounts
                - **Permission Editor**: Node-based permission system with inheritance
                - **Inventory Viewer**: View and modify player inventories
                - **Player Statistics**: Track player activity and achievements
                - **Online Status**: Monitor active players and sessions
                
                ## Server Administration
                - **Server Console**: Execute commands and view live logs
                - **Performance Metrics**: Monitor TPS, memory, CPU usage
                - **Log Viewer**: Search and download server logs
                - **World Management**: Control world settings and dimensions
                - **Database Browser**: Query SQLite databases
                
                ## Economy & Items
                - **Economy Overview**: Track transactions and balances
                - **Kit Configuration**: Create and manage item kits
                - **Resource Pack Manager**: Deploy and enforce resource packs
                
                ## Communication
                - **Announcement System**: Broadcast messages to players
                - **Chat Moderation**: Monitor and moderate chat messages
                - **Event Calendar**: Schedule and manage server events
                
                ## Teleportation
                - **Home & Warp Manager**: Manage player homes and warps
                - **TPA Management**: Control teleport requests
                - **Spawn Management**: Configure spawn points
                
                ## Automation
                - **Scheduled Tasks**: Automate server maintenance
                - **Automated Backups**: Schedule world backups
                
                ## Moderation
                - **Whitelist/Blacklist**: Control server access
                - **Ban Management**: Manage player bans and appeals
                - **Nickname Manager**: Control player display names
                
                ## Advanced
                - **Map Viewer**: Interactive world map with player tracking
                - **Plugin Configuration**: Edit config files with live reload
                - **Multi-Language Support**: Localized dashboard in 11 languages
                """,
                2
        ));
        
        sections.put("security", new DocumentationSection(
                "security",
                "Security Best Practices",
                "Important security guidelines for dashboard administrators",
                """
                # Security Best Practices
                
                ## Authentication
                
                1. **Change Default Credentials**: Immediately change the default admin password
                2. **Use Strong Passwords**: Require complex passwords for all accounts
                3. **Enable Two-Factor**: Consider implementing 2FA for admin accounts
                4. **Regular Password Rotation**: Change passwords periodically
                
                ## Network Security
                
                1. **Firewall Configuration**: Restrict dashboard access to trusted IPs
                2. **HTTPS/SSL**: Use reverse proxy (nginx/Apache) with SSL certificates
                3. **Port Management**: Change default port 8080 if exposed to internet
                4. **VPN Access**: Consider requiring VPN for remote administration
                
                ## Permission Management
                
                1. **Principle of Least Privilege**: Grant minimum necessary permissions
                2. **Role-Based Access**: Use roles instead of individual permissions
                3. **Regular Audits**: Review permission assignments regularly
                4. **Session Timeouts**: Configure appropriate session expiration
                
                ## Data Protection
                
                1. **Regular Backups**: Enable automated backup system
                2. **Database Security**: Protect SQLite database files
                3. **Log Retention**: Configure appropriate log rotation
                4. **Sensitive Data**: Avoid storing sensitive information in configs
                
                ## Monitoring
                
                1. **Audit Logs**: Review command execution logs regularly
                2. **Failed Login Attempts**: Monitor authentication failures
                3. **Unusual Activity**: Watch for suspicious API requests
                4. **Performance Alerts**: Set up alerts for resource abuse
                
                ## Updates
                
                1. **Keep Updated**: Install security patches promptly
                2. **Dependency Management**: Update NeoForge and dependencies
                3. **Changelog Review**: Read update notes for security fixes
                """,
                3
        ));
        
        sections.put("troubleshooting", new DocumentationSection(
                "troubleshooting",
                "Troubleshooting",
                "Common issues and solutions",
                """
                # Troubleshooting Guide
                
                ## Dashboard Won't Start
                
                **Problem**: Dashboard doesn't start on server launch
                
                **Solutions**:
                1. Check if port 8080 is already in use: `netstat -ano | grep 8080`
                2. Review server logs for error messages
                3. Verify NeoEssentials is properly installed in mods folder
                4. Check if Java has network permissions
                5. Try changing port in config file
                
                ## Cannot Connect to Dashboard
                
                **Problem**: Browser shows "Connection refused" or timeout
                
                **Solutions**:
                1. Verify server is running and dashboard is started
                2. Check firewall rules allow connections to port 8080
                3. Try accessing from server: `http://localhost:8080`
                4. Verify correct IP address (use server's LAN IP)
                5. Check if reverse proxy (if used) is configured correctly
                
                ## Login Issues
                
                **Problem**: Cannot log in with credentials
                
                **Solutions**:
                1. Verify username/password are correct (case-sensitive)
                2. Reset password using console command: `/neoessentials resetpassword`
                3. Check authentication logs in server console
                4. Clear browser cache and cookies
                5. Try incognito/private browsing mode
                
                ## Features Not Loading
                
                **Problem**: Dashboard loads but features show errors
                
                **Solutions**:
                1. Check browser console (F12) for JavaScript errors
                2. Clear browser cache completely
                3. Try different browser (Chrome, Firefox, Edge)
                4. Verify API endpoints are responding: check Network tab
                5. Review server logs for backend errors
                
                ## Performance Issues
                
                **Problem**: Dashboard is slow or unresponsive
                
                **Solutions**:
                1. Check server resources (CPU, RAM)
                2. Reduce log tail length in settings
                3. Limit database query result size
                4. Close unused browser tabs
                5. Check network latency to server
                
                ## Permission Errors
                
                **Problem**: "Access denied" or "Insufficient permissions"
                
                **Solutions**:
                1. Verify user has correct role assigned
                2. Check permission nodes in Permission Editor
                3. Review role inheritance configuration
                4. Clear permission cache: `/neoessentials reloadperms`
                5. Check for permission negation entries
                
                ## Database Issues
                
                **Problem**: Database browser shows errors
                
                **Solutions**:
                1. Verify database file exists and is readable
                2. Check file permissions on database
                3. Ensure database is not corrupted: use SQLite tools
                4. Refresh database list in dashboard
                5. Check if database is locked by another process
                
                ## Map Viewer Issues
                
                **Problem**: Map not rendering or showing players
                
                **Solutions**:
                1. Verify world is loaded on server
                2. Check if players are in same dimension
                3. Clear map cache in browser
                4. Verify WebSocket connection is active
                5. Check console for map rendering errors
                
                ## Getting More Help
                
                If issues persist:
                1. Check server logs: `logs/latest.log`
                2. Enable debug logging in config
                3. Review GitHub Issues for similar problems
                4. Join Discord server for community support
                5. Submit bug report with logs and reproduction steps
                """,
                4
        ));
        
        // ── Placeholder API ──────────────────────────────────────────────────
        sections.put("placeholder-api", new DocumentationSection(
                "placeholder-api",
                "Placeholder API",
                "Register custom placeholders from your mod or plugin",
                """
                # Placeholder API
                
                NeoEssentials provides a thread-safe placeholder system that any mod can integrate with.
                Placeholders are resolved in chat format strings, MOTD, join/quit messages, tablist
                headers/footers, and any config value that passes through `PlaceholderManager.setPlaceholders()`.
                
                ## Syntax
                
                Placeholders use curly-brace syntax: `{identifier}` or `{identifier:params}`.
                
                External mods are encouraged to prefix identifiers with their mod id, e.g. `{mymod_kills}`.
                
                ## Registering a single placeholder
                
                Call during your mod's init event or `ServerStartingEvent`:
                
                ```java
                import com.zerog.neoessentials.api.NeoEssentialsAPI;
                import com.zerog.neoessentials.api.PlaceholderManager;
                
                PlaceholderManager pm = NeoEssentialsAPI.getPlaceholderManager();
                
                pm.registerPlaceholder("mymod_kills", (player, params) ->
                    player != null ? String.valueOf(MyStats.getKills(player.getUUID())) : "0"
                );
                ```
                
                Or via the static façade:
                
                ```java
                import com.zerog.neoessentials.api.PlaceholderAPI;
                
                PlaceholderAPI.registerPlaceholder("mymod_kills", (player, params) -> "42");
                ```
                
                ## Registering a PlaceholderExpansion (multiple placeholders)
                
                ```java
                import com.zerog.neoessentials.api.PlaceholderExpansion;
                import com.zerog.neoessentials.api.PlaceholderAPI;
                
                public class MyModExpansion extends PlaceholderExpansion {
                
                    @Override public String getIdentifier() { return "mymod"; }
                    @Override public String getVersion()    { return "1.0.0"; }
                    @Override public String getAuthor()     { return "YourName"; }
                
                    @Override
                    public java.util.Set<String> getPlaceholders() {
                        return java.util.Set.of("kills", "deaths", "playtime");
                    }
                
                    @Override
                    public String onPlaceholderRequest(ServerPlayer player, String id, String params) {
                        if (player == null) return null;
                        return switch (id) {
                            case "kills"    -> String.valueOf(MyStats.getKills(player.getUUID()));
                            case "deaths"   -> String.valueOf(MyStats.getDeaths(player.getUUID()));
                            case "playtime" -> MyStats.getFormattedPlaytime(player.getUUID());
                            default         -> null;
                        };
                    }
                }
                
                // Registration
                PlaceholderAPI.registerExpansion(new MyModExpansion());
                ```
                
                The expansion above registers `{mymod_kills}`, `{mymod_deaths}`, `{mymod_playtime}`.
                
                ## Resolving placeholders
                
                ```java
                PlaceholderManager pm = NeoEssentialsAPI.getPlaceholderManager();
                
                // Resolve all placeholders in a string
                String formatted = pm.setPlaceholders(player, "Hello {neoessentials_name}, you have {mymod_kills} kills!");
                
                // Resolve a single placeholder
                String value = pm.getPlaceholderValue(player, "mymod_kills", null);
                ```
                
                ## Built-in NeoEssentials placeholders
                
                All NeoEssentials placeholders use the `neoessentials` expansion prefix:
                
                | Placeholder | Description |
                |---|---|
                | `{neoessentials_name}` | Player username |
                | `{neoessentials_displayname}` | Player display name / nickname |
                | `{neoessentials_prefix}` | Permission group prefix |
                | `{neoessentials_suffix}` | Permission group suffix |
                | `{neoessentials_group}` | Primary permission group |
                | `{neoessentials_balance}` | Economy balance (raw) |
                | `{neoessentials_balance_formatted}` | Economy balance (formatted) |
                | `{neoessentials_world}` | Current dimension name |
                | `{neoessentials_x}` | Player X coordinate |
                | `{neoessentials_y}` | Player Y coordinate |
                | `{neoessentials_z}` | Player Z coordinate |
                | `{neoessentials_biome}` | Current biome |
                | `{neoessentials_health}` | Current health |
                | `{neoessentials_max_health}` | Max health |
                | `{neoessentials_food}` | Food level |
                | `{neoessentials_level}` | Experience level |
                | `{neoessentials_exp}` | Experience progress (%) |
                | `{neoessentials_gamemode}` | Current gamemode |
                | `{neoessentials_ping}` | Connection latency (ms) |
                | `{neoessentials_online_players}` | Online player count |
                | `{neoessentials_max_players}` | Max player slots |
                | `{neoessentials_server_name}` | Server MOTD / name |
                | `{neoessentials_time}` | Server time (12h) |
                | `{neoessentials_time_24}` | Server time (24h) |
                | `{neoessentials_date}` | Current date (yyyy-MM-dd) |
                | `{neoessentials_afk}` | AFK status ("AFK" or blank) |
                | `{neoessentials_afk_time}` | Time AFK (e.g. "5m 30s") |
                | `{neoessentials_afk_reason}` | AFK reason text |
                
                ## Short-form placeholders (legacy)
                
                The following short-form placeholders (without expansion prefix) also work and map
                to common values for backwards compatibility in chat config strings:
                `{player}`, `{prefix}`, `{suffix}`, `{group}`, `{world}`, `{balance}`, `{ping}`.
                
                ## REST API
                
                The placeholder system is also accessible via REST:
                
                - `GET /api/placeholders/list` — all registered identifiers
                - `GET /api/placeholders/resolve?player=<name>&text=<str>` — server-side resolution
                - `GET /api/placeholders/stats` — registry statistics
                
                ## In-game admin command
                
                ```
                /placeholder list                  — list all registered identifiers
                /placeholder info <id>             — check if an identifier is registered
                /placeholder test <text>           — resolve placeholders live (uses your player context)
                /placeholder stats                 — show registry statistics
                ```
                
                Permission: `neoessentials.admin.placeholders`
                """,
                5
        ));

        // ── Developer API ─────────────────────────────────────────────────────
        sections.put("developer-api", new DocumentationSection(
                "developer-api",
                "Developer API",
                "Extend NeoEssentials from your own NeoForge mod",
                """
                # NeoEssentials Developer API
                
                **API Version:** 1.2.0
                
                NeoEssentials ships a stable public API that other NeoForge 1.21.1 mods can depend on
                to integrate with its economy, permission, and placeholder systems without any transitive
                dependencies beyond the mod jar itself.
                
                ## Adding NeoEssentials as a dependency
                
                ### build.gradle
                
                ```groovy
                dependencies {
                    // NeoEssentials mod jar on the local libs/ path, or via a file dep
                    implementation files('libs/neoessentials-<version>.jar')
                }
                ```
                
                Mark the dependency as `compileOnly` if your mod is designed to work without
                NeoEssentials present and you check `isAvailable()` at runtime.
                
                ## API entry-point
                
                ```java
                import com.zerog.neoessentials.api.NeoEssentialsAPI;
                
                if (NeoEssentialsAPI.isAvailable()) {
                    // safe to call any API method
                }
                
                // API version (SemVer)
                String version = NeoEssentialsAPI.API_VERSION; // "1.2.0"
                ```
                
                ## Economy API
                
                ```java
                import com.zerog.neoessentials.api.economy.EconomyService;
                
                EconomyService eco = NeoEssentialsAPI.getEconomyService();
                
                eco.deposit(uuid, 100.0);
                eco.withdraw(uuid, 50.0);
                double balance = eco.getBalance(uuid);
                boolean has    = eco.has(uuid, 30.0);
                ```
                
                ### Economy Events (NeoForge event bus)
                
                ```java
                @SubscribeEvent
                public void onDeposit(EconomyDepositEvent event) {
                    UUID player      = event.getPlayerUUID();
                    double amount    = event.getAmount();
                    double newBal    = event.getNewBalance();
                    event.setCanceled(true); // optional — cancels the deposit
                }
                
                @SubscribeEvent
                public void onWithdraw(EconomyWithdrawEvent event) { ... }
                ```
                
                ## Permissions API
                
                ```java
                import com.zerog.neoessentials.api.permissions.PermissionsService;
                
                PermissionsService perms = NeoEssentialsAPI.getPermissionsService();
                
                // Check permission
                boolean canFly = perms.hasPermission(playerUUID, "neoessentials.fly");
                
                // Group info
                String group  = perms.getGroup(playerUUID);
                String prefix = perms.getPrefix(playerUUID);
                String suffix = perms.getSuffix(playerUUID);
                
                // Register your mod's permission nodes (shown in /permissions search)
                perms.registerPermission("mymod.feature", "Enables my mod's cool feature");
                
                // Register a legacy alias
                perms.registerAlias("oldmod.fly", "neoessentials.fly");
                ```
                
                ## Placeholder API
                
                See the **Placeholder API** section for full details with code examples.
                
                Quick reference:
                
                ```java
                import com.zerog.neoessentials.api.PlaceholderManager;
                import com.zerog.neoessentials.api.PlaceholderExpansion;
                import com.zerog.neoessentials.api.PlaceholderAPI;
                
                // Single placeholder
                PlaceholderAPI.registerPlaceholder("mymod_value", (player, params) -> "hello");
                
                // Expansion (multiple)
                PlaceholderAPI.registerExpansion(new MyModExpansion());
                
                // Resolve
                String out = PlaceholderManager.getInstance().setPlaceholders(player, "{mymod_value}");
                ```
                
                ## REST API access
                
                All REST endpoints exposed by the dashboard (see API Reference section) are accessible
                from external tools using a Bearer token obtained from `POST /api/auth/login`.
                
                ```bash
                # Login
                curl -X POST http://localhost:8080/api/auth/login \\
                     -H "Content-Type: application/json" \\
                     -d '{"username":"admin","password":"secret"}'
                # Response: {"token":"abc123..."}
                
                # Use token
                curl http://localhost:8080/api/placeholders/list \\
                     -H "Authorization: Bearer abc123..."
                ```
                
                ## Versioning contract
                
                NeoEssentials follows SemVer for API changes:
                - **PATCH** — bug fixes only, fully backward compatible
                - **MINOR** — new methods/classes added, backward compatible
                - **MAJOR** — breaking changes (rare, announced in advance)
                
                Use `NeoEssentialsAPI.API_VERSION` (String, SemVer) to guard version-specific calls:
                
                ```java
                String[] parts = NeoEssentialsAPI.API_VERSION.split("\\\\.");
                int minor = Integer.parseInt(parts[1]);
                if (minor >= 2) {
                    // use getPlaceholderManager(), available since 1.2.0
                }
                ```
                """,
                6
        ));

        NeoLog.info(LOGGER, LogCategory.GENERAL, "Loaded {} documentation sections", sections.size());
    }
    
    /**
     * Load or create API endpoint documentation
     */
    private void loadOrCreateApiDocumentation() {
        // User Management API
        apiEndpoints.put("/api/users", new ApiEndpoint(
                "/api/users",
                "User Management",
                "GET, POST, PUT, DELETE",
                "Manage user accounts, roles, and permissions",
                List.of(
                        new ApiExample("GET", "/api/users", null, "List all users", """
                                {
                                  "success": true,
                                  "users": [
                                    {
                                      "id": "uuid",
                                      "username": "admin",
                                      "role": "ADMIN",
                                      "lastLogin": "2025-10-15T10:30:00Z"
                                    }
                                  ]
                                }"""),
                        new ApiExample("POST", "/api/users", """
                                {
                                  "username": "newuser",
                                  "password": "secure_password",
                                  "role": "MODERATOR"
                                }""", "Create new user", """
                                {
                                  "success": true,
                                  "user": {
                                    "id": "new-uuid",
                                    "username": "newuser",
                                    "role": "MODERATOR"
                                  }
                                }""")
                ),
                "Admin"
        ));
        
        // Performance API
        apiEndpoints.put("/api/performance/current", new ApiEndpoint(
                "/api/performance/current",
                "Performance Metrics",
                "GET",
                "Get current server performance metrics",
                List.of(
                        new ApiExample("GET", "/api/performance/current", null, "Get current metrics", """
                                {
                                  "success": true,
                                  "tps": 20.0,
                                  "memoryUsed": 2048,
                                  "memoryMax": 4096,
                                  "cpuUsage": 15.5,
                                  "playerCount": 10,
                                  "entityCount": 1523,
                                  "chunkCount": 2048
                                }""")
                ),
                "All"
        ));
        
        // Database API
        apiEndpoints.put("/api/database/query", new ApiEndpoint(
                "/api/database/query",
                "Database Query",
                "POST",
                "Execute read-only SQL queries on SQLite databases",
                List.of(
                        new ApiExample("POST", "/api/database/query", """
                                {
                                  "database": "neoessentials.db",
                                  "query": "SELECT * FROM players LIMIT 10",
                                  "page": 1,
                                  "pageSize": 10
                                }""", "Query database", """
                                {
                                  "success": true,
                                  "columns": ["id", "uuid", "username"],
                                  "rows": [
                                    ["1", "uuid-here", "player1"]
                                  ],
                                  "totalRows": 150,
                                  "page": 1,
                                  "pageSize": 10
                                }""")
                ),
                "Admin"
        ));
        
        // ── Placeholder API endpoints ──────────────────────────────────────────
        apiEndpoints.put("/api/placeholders/list", new ApiEndpoint(
                "/api/placeholders/list",
                "List Placeholders",
                "GET",
                "Return all registered placeholder identifiers (sorted alphabetically)",
                List.of(
                        new ApiExample("GET", "/api/placeholders/list", null, "Get all placeholders", """
                                {
                                  "success": true,
                                  "count": 30,
                                  "placeholders": [
                                    "neoessentials_afk",
                                    "neoessentials_balance",
                                    "neoessentials_displayname",
                                    "neoessentials_group",
                                    "..."
                                  ]
                                }""")
                ),
                "Auth"
        ));

        apiEndpoints.put("/api/placeholders/resolve", new ApiEndpoint(
                "/api/placeholders/resolve",
                "Resolve Placeholders",
                "GET",
                "Resolve all placeholders in a text string server-side. "
                + "Optional query param `player` provides a player context (must be online).",
                List.of(
                        new ApiExample("GET",
                                "/api/placeholders/resolve?player=Steve&text=Hello+{neoessentials_name}!",
                                null,
                                "Resolve with player context",
                                """
                                {
                                  "success": true,
                                  "player": "Steve",
                                  "input": "Hello {neoessentials_name}!",
                                  "resolved": "Hello Steve!"
                                }"""),
                        new ApiExample("GET",
                                "/api/placeholders/resolve?text=Server+has+{neoessentials_online_players}+players.",
                                null,
                                "Resolve without player (server-wide only)",
                                """
                                {
                                  "success": true,
                                  "player": null,
                                  "input": "Server has {neoessentials_online_players} players.",
                                  "resolved": "Server has 12 players."
                                }""")
                ),
                "Auth"
        ));

        apiEndpoints.put("/api/placeholders/stats", new ApiEndpoint(
                "/api/placeholders/stats",
                "Placeholder Statistics",
                "GET",
                "Return placeholder registry statistics: total counts, expansion counts, etc.",
                List.of(
                        new ApiExample("GET", "/api/placeholders/stats", null, "Get stats", """
                                {
                                  "success": true,
                                  "stats": {
                                    "total_placeholders": 5,
                                    "total_expansions": 2,
                                    "registered_placeholders": 30
                                  }
                                }""")
                ),
                "Auth"
        ));

        // Internationalization API
        apiEndpoints.put("/api/i18n/languages", new ApiEndpoint(
                "/api/i18n/languages",
                "Available Languages",
                "GET",
                "List all supported dashboard languages",
                List.of(
                        new ApiExample("GET", "/api/i18n/languages", null, "Get languages", """
                                {
                                  "success": true,
                                  "count": 11,
                                  "languages": [
                                    {
                                      "code": "en_us",
                                      "nativeName": "English (United States)",
                                      "englishName": "English",
                                      "countryCode": "US",
                                      "rtl": false
                                    }
                                  ]
                                }""")
                ),
                "All"
        ));
        
        NeoLog.info(LOGGER, LogCategory.GENERAL, "Loaded {} API endpoint documentations", apiEndpoints.size());
    }
    
    /**
     * Load or create tutorials
     */
    private void loadOrCreateTutorials() {
        tutorials.add(new Tutorial(
                "setup-permissions",
                "Setting Up Permissions",
                "Learn how to configure the permission system",
                "beginner",
                15,
                List.of(
                        new TutorialStep(1, "Navigate to Permission Editor", "Click 'Permissions' in the sidebar menu"),
                        new TutorialStep(2, "Create a Role", "Click 'Add Role' button and name it (e.g., 'Moderator')"),
                        new TutorialStep(3, "Add Permission Nodes", "Click the role, then 'Add Permission'. Use wildcards like 'neoessentials.kick.*'"),
                        new TutorialStep(4, "Assign to Users", "Go to User Management, edit a user, and select the role"),
                        new TutorialStep(5, "Test Permissions", "Have the user log in and verify they can access features")
                )
        ));
        
        tutorials.add(new Tutorial(
                "create-backup",
                "Creating Automated Backups",
                "Set up scheduled world backups",
                "intermediate",
                10,
                List.of(
                        new TutorialStep(1, "Open Backup Manager", "Navigate to 'Backups' in sidebar"),
                        new TutorialStep(2, "Create Schedule", "Click 'New Schedule' button"),
                        new TutorialStep(3, "Configure Timing", "Use cron expression or interval (e.g., 'Every 6 hours')"),
                        new TutorialStep(4, "Set Retention Policy", "Configure how many backups to keep"),
                        new TutorialStep(5, "Test Backup", "Click 'Backup Now' to verify configuration")
                )
        ));
        
        tutorials.add(new Tutorial(
                "database-query",
                "Querying Databases",
                "How to use the database browser to query SQLite databases",
                "advanced",
                20,
                List.of(
                        new TutorialStep(1, "Open Database Browser", "Click 'Database' in navigation menu"),
                        new TutorialStep(2, "Select Database", "Choose a database from the list"),
                        new TutorialStep(3, "View Tables", "Click on a table to see its schema"),
                        new TutorialStep(4, "Execute Query", "Use the query editor to write SELECT statements"),
                        new TutorialStep(5, "Export Results", "Download results as CSV or JSON")
                )
        ));
        
        NeoLog.info(LOGGER, LogCategory.GENERAL, "Loaded {} tutorials", tutorials.size());
    }
    
    /**
     * Load or create FAQ items
     */
    private void loadOrCreateFaqItems() {
        faqItems.add(new FaqItem(
                "change-port",
                "How do I change the dashboard port?",
                """
                To change the dashboard port:
                1. Stop your server
                2. Open `config/neoessentials/main.json`
                3. Find the `webDashboard` section
                4. Change `port` value (e.g., from 8080 to 8081)
                5. Save the file and restart your server
                
                Example:
                ```json
                "webDashboard": {
                  "enabled": true,
                  "port": 8081,
                  "bindAddress": "0.0.0.0"
                }
                ```
                """,
                List.of("configuration", "network")
        ));
        
        faqItems.add(new FaqItem(
                "reset-password",
                "How do I reset the admin password?",
                """
                If you've forgotten the admin password:
                1. Open server console
                2. Execute: `/neoessentials resetpassword admin newpassword`
                3. Or edit `config/neoessentials/users.json` directly
                4. Log in with new credentials
                
                For security, change the password again after logging in via the dashboard Settings page.
                """,
                List.of("security", "authentication")
        ));
        
        faqItems.add(new FaqItem(
                "ssl-https",
                "Can I use HTTPS/SSL with the dashboard?",
                """
                Yes! The recommended approach is using a reverse proxy:
                
                **Using Nginx**:
                ```nginx
                server {
                    listen 443 ssl;
                    server_name dashboard.example.com;
                    
                    ssl_certificate /path/to/cert.pem;
                    ssl_certificate_key /path/to/key.pem;
                    
                    location / {
                        proxy_pass http://localhost:8080;
                        proxy_set_header Host $host;
                        proxy_set_header X-Real-IP $remote_addr;
                    }
                }
                ```
                
                **Using Apache**:
                ```apache
                <VirtualHost *:443>
                    ServerName dashboard.example.com
                    SSLEngine on
                    SSLCertificateFile /path/to/cert.pem
                    SSLCertificateKeyFile /path/to/key.pem
                    
                    ProxyPass / http://localhost:8080/
                    ProxyPassReverse / http://localhost:8080/
                </VirtualHost>
                ```
                """,
                List.of("security", "network", "advanced")
        ));
        
        faqItems.add(new FaqItem(
                "performance-impact",
                "Does the dashboard affect server performance?",
                """
                The dashboard has minimal performance impact:
                - **Idle**: Negligible (< 1% CPU, ~50MB RAM)
                - **Active Use**: Moderate (2-5% CPU, ~100MB RAM)
                - **Heavy Queries**: Can spike temporarily
                
                **Tips to minimize impact**:
                1. Limit concurrent users
                2. Reduce log tail length
                3. Use pagination for large datasets
                4. Schedule heavy operations during low-traffic times
                5. Adjust metric collection intervals
                
                The dashboard runs asynchronously and won't block game server threads.
                """,
                List.of("performance", "optimization")
        ));
        
        faqItems.add(new FaqItem(
                "mobile-access",
                "Can I use the dashboard on mobile?",
                """
                Yes! The dashboard is responsive and works on mobile devices:
                - **Tablets**: Full desktop experience
                - **Phones**: Optimized mobile layout
                - **Touch Support**: Touch-friendly controls
                
                **Recommendations**:
                - Use landscape mode for better visibility
                - Some features work better on larger screens
                - Consider using desktop for complex tasks (database queries, bulk operations)
                
                Tested on:
                - iOS Safari
                - Android Chrome
                - Mobile Firefox
                """,
                List.of("mobile", "accessibility")
        ));
        
        faqItems.add(new FaqItem(
                "backup-location",
                "Where are backups stored?",
                """
                Backups are stored in the server's backup directory:
                - **Default Location**: `backups/` folder in server root
                - **Custom Location**: Can be configured in settings
                
                **Backup Structure**:
                ```
                backups/
                  ├── world_2025-10-15_10-30-00.zip
                  ├── world_nether_2025-10-15_10-30-00.zip
                  └── world_the_end_2025-10-15_10-30-00.zip
                ```
                
                **Important Notes**:
                - Backups are compressed (ZIP format)
                - Includes world data, playerdata, and region files
                - Automatic cleanup based on retention policy
                - Manual backups are never auto-deleted
                """,
                List.of("backups", "storage")
        ));
        
        NeoLog.info(LOGGER, LogCategory.GENERAL, "Loaded {} FAQ items", faqItems.size());
    }
    
    /**
     * Load or create video tutorials
     */
    private void loadOrCreateVideoTutorials() {
        videoTutorials.add(new VideoTutorial(
                "dashboard-overview",
                "Dashboard Overview and Features",
                "Complete tour of the NeoEssentials dashboard",
                "https://youtube.com/watch?v=example1",
                480,
                "beginner"
        ));
        
        videoTutorials.add(new VideoTutorial(
                "permission-system",
                "Understanding the Permission System",
                "Deep dive into permission nodes and inheritance",
                "https://youtube.com/watch?v=example2",
                720,
                "intermediate"
        ));
        
        NeoLog.info(LOGGER, LogCategory.GENERAL, "Loaded {} video tutorials", videoTutorials.size());
    }
    
    // ===== Public API Methods =====
    
    public Map<String, DocumentationSection> getAllSections() {
        return new LinkedHashMap<>(sections);
    }
    
    public DocumentationSection getSection(String sectionId) {
        return sections.get(sectionId);
    }
    
    public Map<String, ApiEndpoint> getAllApiEndpoints() {
        return new LinkedHashMap<>(apiEndpoints);
    }
    
    public ApiEndpoint getApiEndpoint(String endpoint) {
        return apiEndpoints.get(endpoint);
    }
    
    public List<Tutorial> getAllTutorials() {
        return new ArrayList<>(tutorials);
    }
    
    public Tutorial getTutorial(String tutorialId) {
        return tutorials.stream()
                .filter(t -> t.id.equals(tutorialId))
                .findFirst()
                .orElse(null);
    }
    
    public List<FaqItem> getAllFaqItems() {
        return new ArrayList<>(faqItems);
    }
    
    public List<FaqItem> searchFaq(String query) {
        String lowerQuery = query.toLowerCase();
        return faqItems.stream()
                .filter(faq -> 
                    faq.question.toLowerCase().contains(lowerQuery) ||
                    faq.answer.toLowerCase().contains(lowerQuery) ||
                    faq.tags.stream().anyMatch(tag -> tag.toLowerCase().contains(lowerQuery))
                )
                .collect(Collectors.toList());
    }
    
    public List<VideoTutorial> getAllVideoTutorials() {
        return new ArrayList<>(videoTutorials);
    }
    
    public VideoTutorial getVideoTutorial(String videoId) {
        return videoTutorials.stream()
                .filter(v -> v.id.equals(videoId))
                .findFirst()
                .orElse(null);
    }
    
    // ===== Data Classes =====
    
    public static class DocumentationSection {
        public final String id;
        public final String title;
        public final String description;
        public final String content;
        public final int order;
        
        public DocumentationSection(String id, String title, String description, String content, int order) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.content = content;
            this.order = order;
        }
    }
    
    public static class ApiEndpoint {
        public final String endpoint;
        public final String name;
        public final String methods;
        public final String description;
        public final List<ApiExample> examples;
        public final String requiredPermission;
        
        public ApiEndpoint(String endpoint, String name, String methods, String description, 
                          List<ApiExample> examples, String requiredPermission) {
            this.endpoint = endpoint;
            this.name = name;
            this.methods = methods;
            this.description = description;
            this.examples = examples;
            this.requiredPermission = requiredPermission;
        }
    }
    
    public static class ApiExample {
        public final String method;
        public final String endpoint;
        public final String requestBody;
        public final String description;
        public final String responseBody;
        
        public ApiExample(String method, String endpoint, String requestBody, 
                         String description, String responseBody) {
            this.method = method;
            this.endpoint = endpoint;
            this.requestBody = requestBody;
            this.description = description;
            this.responseBody = responseBody;
        }
    }
    
    public static class Tutorial {
        public final String id;
        public final String title;
        public final String description;
        public final String difficulty;
        public final int estimatedMinutes;
        public final List<TutorialStep> steps;
        
        public Tutorial(String id, String title, String description, String difficulty, 
                       int estimatedMinutes, List<TutorialStep> steps) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.difficulty = difficulty;
            this.estimatedMinutes = estimatedMinutes;
            this.steps = steps;
        }
    }
    
    public static class TutorialStep {
        public final int stepNumber;
        public final String title;
        public final String instructions;
        
        public TutorialStep(int stepNumber, String title, String instructions) {
            this.stepNumber = stepNumber;
            this.title = title;
            this.instructions = instructions;
        }
    }
    
    public static class FaqItem {
        public final String id;
        public final String question;
        public final String answer;
        public final List<String> tags;
        
        public FaqItem(String id, String question, String answer, List<String> tags) {
            this.id = id;
            this.question = question;
            this.answer = answer;
            this.tags = tags;
        }
    }
    
    public static class VideoTutorial {
        public final String id;
        public final String title;
        public final String description;
        public final String videoUrl;
        public final int durationSeconds;
        public final String difficulty;
        
        public VideoTutorial(String id, String title, String description, String videoUrl, 
                            int durationSeconds, String difficulty) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.videoUrl = videoUrl;
            this.durationSeconds = durationSeconds;
            this.difficulty = difficulty;
        }
    }
}
