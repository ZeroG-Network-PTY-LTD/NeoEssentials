package com.zerog.neoessentials.config;

public class MainConfig {
    public final HomeSettings homeSettings = new HomeSettings();
    public final EconomySettings economySettings = new EconomySettings();
    public final WarpConfig warpConfig = new WarpConfig();
    public final SpawnConfig spawnConfig = new SpawnConfig();
    public final KitSettings kitSettings = new KitSettings();
    public final ChatSettings chatSettings = new ChatSettings();
    public final ItemManagementConfig itemManagement = new ItemManagementConfig();
    public final InvseeConfig invseeConfig = new InvseeConfig();
    public final ColorPermissionsConfig colorPermissionsConfig = new ColorPermissionsConfig();
    public final Modules modules = new Modules();
    public int maxGiveAmount = 64;
    public boolean allowGiveEnchantments = true;
    public boolean debugMode = false;

    public static class Modules {
        public boolean homes = true;
        public boolean economy = true;
        public boolean warps = true;
        public boolean kits = true;
        public boolean chat = true;
        public boolean spawn = true;
        public boolean moderation = true;
    }

    public static class HomeSettings {
        public int maxHomes = 5;
        public double setHomeCost = 0.0;
        public boolean enabled = true;
        public java.util.List<String> restrictedWorlds = new java.util.ArrayList<>();
        public boolean useSetHomeCost = true;
        public double teleportHomeCost = 10.0;
        public boolean requireSafeLocation = true;
        public int teleportWarmup = 3;
        public int maxHomesAdmin = 20;
        public int maxHomesVip = 10;
        public int teleportHomeCooldown = 60;
    }
    
    public static class EconomySettings {
        public boolean enabled = true;
        public double startingBalance = 100.0;
        public String currencySymbol = "$";
        public double maxBalance = 100000.0;
        public boolean cleanupInactiveAccounts = true;
        public double transactionFeePercent = 1.0;
        public double maxTransferAmount = 10000.0;
    }
    
    public static class WarpConfig {
        public boolean enabled = true;
        public int maxWarpsPerPlayer = 5;
        public double setWarpCost = 100.0;
        public int maxWarpNameLength = 16;
        public boolean allowSpacesInNames = false;
        public java.util.List<String> bannedWarpNames = new java.util.ArrayList<>();
        public java.util.List<String> restrictedWorlds = new java.util.ArrayList<>();
        public Double createWarpCost = 0.0;
        public Double teleportWarpCost = 0.0;
        public boolean requireSafeLocation = true;
        public java.util.List<String> noTeleportWorlds = new java.util.ArrayList<>();
        public int teleportWarmup = 0;
        public int teleportWarpCooldown = 0;
        public boolean checkForVoid = true;
    }
    
    public static class SpawnConfig {
        public boolean enabled = true;
        public boolean setSpawnOnFirstJoin = true;
        public boolean setSpawnOnRespawn = true;
        public boolean setSpawnOnDeath = false;
        public SpawnLocation mainSpawn = new SpawnLocation();
        public NewPlayer newPlayer = new NewPlayer();
        public Safety safety = new Safety();
        public Respawn respawn = new Respawn();

        public static class SpawnLocation {
            public String world = "minecraft:overworld";
            public double x = 0;
            public double y = 64;
            public double z = 0;
            public float yaw = 0;
            public float pitch = 0;
        }

        public static class NewPlayer {
            public boolean giveWelcomeMessage = true;
        }

        public static class Safety {
            public boolean enabled = true;
            public double safetySearchRadius = 16.0;
        }

        public static class Respawn {
            public boolean respectBedSpawns = true;
        }
    }
    
    public static class KitSettings {
        public boolean enabled = true;
        public int kitCooldown = 300;
        public boolean giveKitOnFirstJoin = true;
        public String firstJoinKit = "starter";
    }
    
    public static class ChatSettings {
        public boolean enableChatFormatting = true;
        public boolean enableSpamFilter = true;
        public int spamThreshold = 3;
        public int spamTimeWindow = 5000;
        public java.util.List<String> blockedWords = new java.util.ArrayList<>();
        public String chatFormat = "[{group}] {player_name}: {message}";
        public boolean isEnabled = true;
        public String chatname = "[{group}] {player_name}";
        public AntiSpamSettings antiSpam = new AntiSpamSettings();
        public FilterSettings filter = new FilterSettings();
        public java.util.Map<String, String> groupFormats = new java.util.HashMap<>();

        public static class AntiSpamSettings {
            public boolean enabled = true;
            public int maxMessagesPerSecond = 2;
            public int maxDuplicateMessages = 3;
        }

        public static class FilterSettings {
            public boolean enabled = true;
            public java.util.List<String> blockedWords = new java.util.ArrayList<>();
            public boolean caseSensitive = false;
            public boolean censorMode = true;
            public String censorReplacement = "***";
        }
    }
    
    public static class ItemManagementConfig {
        public boolean enabled = true;
        public int maxStackSize = 64;
        public boolean allowEnchantments = true;
    }

    public static class InvseeConfig {
        public boolean allowEdit = true;
    }

    public static class ColorPermissionsConfig {
        public boolean chat = true;
        public boolean rgb = true;
    }
}
