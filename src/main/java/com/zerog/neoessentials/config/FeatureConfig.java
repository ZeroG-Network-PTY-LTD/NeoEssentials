package com.zerog.neoessentials.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Feature Configuration - feature-config.json
 * Contains all gameplay feature configurations (homes, warps, kits, economy, chat)
 */
public class FeatureConfig {
    
    public Economy economy = new Economy();
    public Homes homes = new Homes();
    public Warps warps = new Warps();
    public Kits kits = new Kits();
    public Chat chat = new Chat();
    public Spawn spawn = new Spawn();
    public Mail mail = new Mail();
    public Teleportation teleportation = new Teleportation();
    
    public static class Economy {
        public boolean enabled = true;
        public String currencySymbol = "$";
        public double startingBalance = 100.0;
        public double transactionFeePercent = 1.0;
        public double maxTransferAmount = 10000.0;
        public boolean enableBanking = true;
        public double interestRate = 0.5;
        public boolean enableShops = true;
        public double dailyBonus = 50.0;
        public boolean enablePayCommand = true;
    }
    
    public static class Homes {
        public boolean enabled = true;
        public int maxHomes = 5;
        public int homeCooldown = 30;
        public double setHomeCost = 25.0;
        public double teleportCost = 10.0;
        public boolean allowCrossWorld = false;
        public List<String> restrictedWorlds = new ArrayList<>();
        public boolean useSetHomeCost = true;
        public double teleportHomeCost = 10.0;
        public boolean requireSafeLocation = true;
        public int teleportWarmup = 3;
        public int maxHomesAdmin = 20;
        public int maxHomesVip = 10;
        public int teleportHomeCooldown = 60;
    }
    
    public static class Warps {
        public boolean enabled = true;
        public int maxWarpsPerPlayer = 5;
        public boolean allowCrossWorld = true;
        public double setWarpCost = 100.0;
        public double teleportCost = 10.0;
        public int warpCooldown = 30;
        public boolean requireSafeLocation = true;
        public List<String> restrictedWorlds = new ArrayList<>();
        public boolean allowPublicWarps = true;
        public boolean allowPrivateWarps = true;
        public boolean allowAdminWarps = true;
        public int maxWarpNameLength = 16;
        public boolean allowSpacesInNames = false;
        public List<String> bannedWarpNames = new ArrayList<>();
        public int teleportWarmup = 0;
        public boolean checkForVoid = true;
    }
    
    public static class Kits {
        public boolean enabled = true;
        public boolean giveKitOnFirstJoin = true;
        public String firstJoinKit = "starter";
        public boolean enableCooldowns = true;
        public boolean autoEquip = false;
        public List<String> commands = new ArrayList<>();
        public int kitCooldown = 300;
        public int maxKitsPerPlayer = 10;
        public boolean allowPreview = true;
        public boolean usePermissions = true;
    }
    
    public static class Chat {
        public boolean isEnabled = true;
        public String chatFormat = "{MESSAGE}";
        public String chatname = "{prefix} {player_name}";
        public boolean enableColors = true;
        public String pmFormatSender = "&7[&6PM&7] &7To {target}: {MESSAGE}";
        public String pmFormatReceiver = "&7[&6PM&7] &7From {sender}: {MESSAGE}";
        public boolean enablePrivateMessages = true;
        public boolean enableChatFormatting = true;
        public Map<String, String> groupFormats = new HashMap<>();
        
        public AntiSpam antiSpam = new AntiSpam();
        public static class AntiSpam {
            public boolean enabled = true;
            public int maxMessagesPerSecond = 2;
            public int maxDuplicateMessages = 3;
        }
        
        public Filter filter = new Filter();
        public static class Filter {
            public boolean enabled = true;
            public boolean caseSensitive = false;
            public boolean censorMode = true;
            public String censorReplacement = "*";
            public List<String> blockedWords = new ArrayList<>();
        }
        
        public Nicknames nicknames = new Nicknames();
        public static class Nicknames {
            public boolean enabled = true;
            public int maxNicknameLength = 16;
            public boolean allowColors = true;
        }
        
        public PrefixSuffix prefixSuffix = new PrefixSuffix();
        public static class PrefixSuffix {
            public boolean enabled = true;
            public String defaultPrefix = "&7[Player]&r";
            public String defaultSuffix = "";
        }
    }
    
    public static class Spawn {
        public boolean enabled = true;
        public boolean setSpawnOnFirstJoin = true;
        public boolean setSpawnOnRespawn = true;
        public boolean setSpawnOnDeath = false;
        public MainSpawn mainSpawn = new MainSpawn();
        
        public static class MainSpawn {
            public String world = "minecraft:overworld";
            public double x = 0.0;
            public double y = 64.0;
            public double z = 0.0;
            public float yaw = 0.0f;
            public float pitch = 0.0f;
        }
        
        public NewPlayer newPlayer = new NewPlayer();
        public static class NewPlayer {
            public boolean giveWelcomeMessage = true;
        }
        
        public Safety safety = new Safety();
        public static class Safety {
            public boolean enabled = true;
            public double safetySearchRadius = 16.0;
        }
        
        public Respawn respawn = new Respawn();
        public static class Respawn {
            public boolean respectBedSpawns = true;
        }
    }
    
    public static class Mail {
        public boolean enabled = true;
        public int maxMailsPerPlayer = 50;
        public int mailRetentionDays = 30;
        public boolean allowAttachments = false;
        public double mailCost = 5.0;
    }
    
    public static class Teleportation {
        public boolean enabled = true;
        public int tpaExpireTime = 30;
        public boolean tpaHereEnabled = true;
        public int teleportWarmup = 3;
        public int teleportCooldown = 5;
        public double teleportCost = 10.0;
        public boolean allowCrossWorld = true;
        public boolean cancelOnMove = true;
        public boolean cancelOnDamage = true;
    }
}
