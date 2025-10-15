package com.zerog.neoessentials.docs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
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
        LOGGER.info("Initializing NeoEssentials Documentation System...");
        
        // Create docs directory
        try {
            Files.createDirectories(docsDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create documentation directory", e);
        }
        
        // Load or create default documentation
        loadDocumentation();
        
        LOGGER.info("Documentation system initialized with {} sections, {} API endpoints, {} tutorials, {} FAQs",
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
        
        LOGGER.info("Loaded {} documentation sections", sections.size());
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
        
        LOGGER.info("Loaded {} API endpoint documentations", apiEndpoints.size());
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
        
        LOGGER.info("Loaded {} tutorials", tutorials.size());
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
        
        LOGGER.info("Loaded {} FAQ items", faqItems.size());
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
        
        LOGGER.info("Loaded {} video tutorials", videoTutorials.size());
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
