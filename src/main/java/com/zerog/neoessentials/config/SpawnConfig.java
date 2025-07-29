package com.zerog.neoessentials.config;

import java.util.Arrays;
import java.util.List;

/**
 * Spawn management configuration for NeoEssentials
 * Compatible with EssentialsX spawn features
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class SpawnConfig {
    
    // Basic spawn settings
    public boolean enabled = true;
    public boolean setSpawnOnFirstJoin = true;
    public boolean setSpawnOnDeath = true;
    public boolean setSpawnOnRespawn = true;
    public boolean setSpawnOnJoin = false;
    public boolean setSpawnOnWorldChange = false;
    
    // Main spawn settings
    public MainSpawnConfig mainSpawn = new MainSpawnConfig();
    
    // New player settings
    public NewPlayerConfig newPlayer = new NewPlayerConfig();
    
    // Respawn settings
    public RespawnConfig respawn = new RespawnConfig();
    
    // World-specific spawns
    public WorldSpawnConfig worldSpawns = new WorldSpawnConfig();
    
    // Group-specific spawns
    public GroupSpawnConfig groupSpawns = new GroupSpawnConfig();
    
    // Random spawn settings
    public RandomSpawnConfig randomSpawn = new RandomSpawnConfig();
    
    // Safety settings
    public SafetyConfig safety = new SafetyConfig();
    
    // Messages
    public MessagesConfig messages = new MessagesConfig();
    
    public static class MainSpawnConfig {
        public boolean enabled = true;
        public String world = "world";
        public double x = 0.0;
        public double y = 64.0;
        public double z = 0.0;
        public float yaw = 0.0f;
        public float pitch = 0.0f;
        public boolean exactCoordinates = false; // If false, find safe spot nearby
        public int priority = 1; // Lower = higher priority
    }
    
    public static class NewPlayerConfig {
        public boolean enabled = true;
        public boolean useWelcomeKit = true;
        public String welcomeKitName = "starter";
        public boolean giveWelcomeMessage = true;
        public boolean protectFromPvp = true;
        public int pvpProtectionSeconds = 300; // 5 minutes
        public boolean flyToSpawn = false;
        public boolean invulnerableOnSpawn = true;
        public int invulnerabilitySeconds = 10;
        public boolean broadcastFirstJoin = true;
        public List<String> firstJoinCommands = Arrays.asList(
            "kit starter {PLAYER}",
            "msg {PLAYER} Welcome to our server!",
            "broadcast &e{PLAYER} joined for the first time!"
        );
    }
    
    public static class RespawnConfig {
        public boolean enabled = true;
        public boolean respectWorldSpawns = true;
        public boolean respectBedSpawns = true;
        public boolean respectAnchorSpawns = true;
        public String respawnPriority = "BED,ANCHOR,WORLD,MAIN"; // Order of preference
        public boolean useRandomSpawnOnDeath = false;
        public boolean keepInventoryOnSpawn = false;
        public boolean keepExperienceOnSpawn = false;
        public boolean clearInventoryOnSpawn = false;
        public int respawnDelay = 0; // Seconds to wait before respawning
        public boolean healOnRespawn = true;
        public boolean feedOnRespawn = true;
    }
    
    public static class WorldSpawnConfig {
        public boolean enabled = true;
        public boolean autoSetWorldSpawn = true;
        public List<WorldSpawnDefinition> worldSpawns = Arrays.asList(
            new WorldSpawnDefinition("world", 0.0, 64.0, 0.0, 0.0f, 0.0f, 1),
            new WorldSpawnDefinition("world_nether", 0.0, 64.0, 0.0, 0.0f, 0.0f, 2),
            new WorldSpawnDefinition("world_the_end", 100.0, 64.0, 0.0, 0.0f, 0.0f, 3)
        );
        
        public static class WorldSpawnDefinition {
            public String worldName;
            public double x;
            public double y;
            public double z;
            public float yaw;
            public float pitch;
            public int priority;
            
            public WorldSpawnDefinition() {}
            
            public WorldSpawnDefinition(String worldName, double x, double y, double z, float yaw, float pitch, int priority) {
                this.worldName = worldName;
                this.x = x;
                this.y = y;
                this.z = z;
                this.yaw = yaw;
                this.pitch = pitch;
                this.priority = priority;
            }
        }
    }
    
    public static class GroupSpawnConfig {
        public boolean enabled = false;
        public List<GroupSpawnDefinition> groupSpawns = Arrays.asList(
            new GroupSpawnDefinition("vip", "world", 50.0, 64.0, 50.0, 0.0f, 0.0f, 1),
            new GroupSpawnDefinition("premium", "world", 25.0, 64.0, 25.0, 0.0f, 0.0f, 2),
            new GroupSpawnDefinition("default", "world", 0.0, 64.0, 0.0, 0.0f, 0.0f, 3)
        );
        
        public static class GroupSpawnDefinition {
            public String groupName;
            public String world;
            public double x;
            public double y;
            public double z;
            public float yaw;
            public float pitch;
            public int priority;
            
            public GroupSpawnDefinition() {}
            
            public GroupSpawnDefinition(String groupName, String world, double x, double y, double z, float yaw, float pitch, int priority) {
                this.groupName = groupName;
                this.world = world;
                this.x = x;
                this.y = y;
                this.z = z;
                this.yaw = yaw;
                this.pitch = pitch;
                this.priority = priority;
            }
        }
    }
    
    public static class RandomSpawnConfig {
        public boolean enabled = false;
        public boolean useOnFirstJoin = false;
        public boolean useOnDeath = false;
        public boolean useOnCommand = true;
        public String world = "world";
        public int minRadius = 100;
        public int maxRadius = 1000;
        public int maxAttempts = 10;
        public int minY = 60;
        public int maxY = 120;
        public boolean avoidWater = true;
        public boolean avoidLava = true;
        public boolean avoidVoid = true;
        public boolean requireSolidGround = true;
        public List<String> forbiddenBiomes = Arrays.asList("ocean", "deep_ocean");
        public List<String> preferredBiomes = Arrays.asList("plains", "forest");
        public boolean checkForStructures = true;
        public List<String> avoidStructures = Arrays.asList("village", "pillager_outpost");
    }
    
    public static class SafetyConfig {
        public boolean enabled = true;
        public boolean enableSafetyChecks = true;
        public boolean checkForSuffocation = true;
        public boolean checkForFall = true;
        public boolean checkForLava = true;
        public boolean checkForVoid = true;
        public boolean checkForHostileMobs = false;
        public int safetyCheckRadius = 3;
        public int maxSafetyAttempts = 5;
        public double safetySearchRadius = 10.0;
        public boolean createSafePlatform = false;
        public String platformMaterial = "stone";
        public boolean lightUpSpawn = false;
        public int lightRadius = 5;
    }
    
    public static class MessagesConfig {
        // Spawn messages
        public String teleportingToSpawn = "&aTeleporting to spawn...";
        public String teleportedToSpawn = "&aYou have been teleported to spawn!";
        public String spawnSet = "&aSpawn has been set to your current location!";
        public String spawnNotSet = "&cSpawn is not set! Use /setspawn to set it.";
        public String spawnNotFound = "&cSpawn location not found or is unsafe!";
        public String unsafeSpawn = "&cSpawn location is unsafe! Finding a safe location...";
        public String safeLocationFound = "&aSafe spawn location found!";
        public String safeLocationNotFound = "&cCould not find a safe spawn location!";
        
        // World spawn messages
        public String worldSpawnSet = "&aWorld spawn for {0} has been set!";
        public String worldSpawnNotSet = "&cWorld spawn for {0} is not set!";
        public String worldSpawnDeleted = "&cWorld spawn for {0} has been deleted!";
        public String worldNotFound = "&cWorld {0} not found!";
        
        // Group spawn messages
        public String groupSpawnSet = "&aGroup spawn for {0} has been set!";
        public String groupSpawnNotSet = "&cGroup spawn for {0} is not set!";
        public String groupSpawnDeleted = "&cGroup spawn for {0} has been deleted!";
        public String groupNotFound = "&cGroup {0} not found!";
        
        // Random spawn messages
        public String randomSpawnTeleporting = "&aFinding a random spawn location...";
        public String randomSpawnFound = "&aRandom spawn location found!";
        public String randomSpawnFailed = "&cCould not find a suitable random spawn location!";
        public String randomSpawnCooldown = "&cYou must wait {0} before using random spawn again!";
        
        // First join messages
        public String welcomeMessage = "&eWelcome to our server, {0}!";
        public String firstJoinBroadcast = "&e{0} joined the server for the first time! Welcome!";
        public String kitGiven = "&aYou have been given the starter kit!";
        public String pvpProtection = "&aYou are protected from PvP for {0}!";
        public String invulnerability = "&aYou are invulnerable for {0}!";
        
        // Error messages
        public String noPermission = "&cYou don't have permission to do that!";
        public String invalidUsage = "&cInvalid usage! Use: {0}";
        public String playerNotFound = "&cPlayer {0} not found!";
        public String playerOffline = "&cPlayer {0} is offline!";
        public String teleportCooldown = "&cYou must wait {0} before teleporting again!";
        public String cannotTeleportHere = "&cYou cannot teleport here!";
        public String teleportBlocked = "&cTeleportation is blocked in this area!";
        
        // Cost messages
        public String spawnCost = "&aTeleporting to spawn costs {COST}!";
        public String insufficientFunds = "&cYou don't have enough money! Cost: {COST}";
        public String chargedForSpawn = "&aYou were charged {COST} for teleporting to spawn!";
        
        // Cooldown messages
        public String spawnCooldown = "&cYou must wait {TIME} before using spawn again!";
        public String cooldownRemaining = "&cCooldown remaining: {TIME}";
        public String cooldownExpired = "&aYour spawn cooldown has expired!";
        
        // Status messages
        public String respawning = "&7Respawning...";
        public String respawned = "&aYou have respawned!";
        public String forcedSpawn = "&cYou were forced to spawn!";
        public String spawnOnJoin = "&aWelcome back! You have been teleported to spawn.";
        public String spawnOnWorldChange = "&aTeleported to this world's spawn!";
    }
    
    /**
     * Check if spawn system is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Check if main spawn is configured
     */
    public boolean isMainSpawnConfigured() {
        return enabled && mainSpawn.enabled && mainSpawn.world != null && !mainSpawn.world.isEmpty();
    }
    
    /**
     * Check if world spawns are enabled
     */
    public boolean areWorldSpawnsEnabled() {
        return enabled && worldSpawns.enabled;
    }
    
    /**
     * Check if group spawns are enabled
     */
    public boolean areGroupSpawnsEnabled() {
        return enabled && groupSpawns.enabled;
    }
    
    /**
     * Check if random spawn is enabled
     */
    public boolean isRandomSpawnEnabled() {
        return enabled && randomSpawn.enabled;
    }
    
    /**
     * Get world spawn for specific world
     */
    public WorldSpawnConfig.WorldSpawnDefinition getWorldSpawn(String worldName) {
        return worldSpawns.worldSpawns.stream()
            .filter(spawn -> spawn.worldName.equalsIgnoreCase(worldName))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get group spawn for specific group
     */
    public GroupSpawnConfig.GroupSpawnDefinition getGroupSpawn(String groupName) {
        return groupSpawns.groupSpawns.stream()
            .filter(spawn -> spawn.groupName.equalsIgnoreCase(groupName))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Check if safety checks are enabled
     */
    public boolean areSafetyChecksEnabled() {
        return enabled && safety.enabled && safety.enableSafetyChecks;
    }
}
