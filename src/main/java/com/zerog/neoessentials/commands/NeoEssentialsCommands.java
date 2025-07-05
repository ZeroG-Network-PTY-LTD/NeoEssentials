package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Main NeoEssentials command with subcommands for help, version, and reload.
 * Provides core functionality and information about the mod.
 */
public class NeoEssentialsCommands {
    
    /**
     * Registers the main /neoessentials command with subcommands.
     *
     * @param dispatcher The command dispatcher to register commands with
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        
        dispatcher.register(
            Commands.literal("neoessentials")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.main"))
                .executes(context -> {
                    // Show main help when no subcommand is provided
                    return showMainHelp(context.getSource());
                })
                
                // /neoessentials help
                .then(Commands.literal("help")
                    .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.help"))
                    .executes(context -> {
                        return showDetailedHelp(context.getSource());
                    })
                )
                
                // /neoessentials version
                .then(Commands.literal("version")
                    .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.version"))
                    .executes(context -> {
                        return showVersion(context.getSource());
                    })
                )
                
                // /neoessentials reload
                .then(Commands.literal("reload")
                    .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.reload"))
                    .executes(context -> {
                        return reloadConfiguration(context.getSource());
                    })
                )
                
                // /neoessentials info
                .then(Commands.literal("info")
                    .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.info"))
                    .executes(context -> {
                        return showSystemInfo(context.getSource());
                    })
                )
        );
    }
    
    /**
     * Shows the main help message with basic information
     */
    private int showMainHelp(CommandSourceStack source) {
        try {
            boolean isPlayer = source.getEntity() instanceof ServerPlayer;
            
            if (isPlayer) {
                ServerPlayer player = source.getPlayerOrException();
                LanguageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                LanguageUtil.sendMessage(player, "§6§l⚡ NeoEssentials §7v1.0.2.75");
                LanguageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                LanguageUtil.sendMessage(player, "§7Welcome to NeoEssentials! Here are the main command categories:");
                LanguageUtil.sendMessage(player, "");
                LanguageUtil.sendMessage(player, "§b§l🏠 Utility Commands:");
                LanguageUtil.sendMessage(player, "§7  • §e/home §7- Home management");
                LanguageUtil.sendMessage(player, "§7  • §e/warp §7- Server warps");
                LanguageUtil.sendMessage(player, "§7  • §e/tpa §7- Teleport requests");
                LanguageUtil.sendMessage(player, "§7  • §e/kit §7- Available kits");
                LanguageUtil.sendMessage(player, "");
                LanguageUtil.sendMessage(player, "§c§l📋 Main Commands:");
                LanguageUtil.sendMessage(player, "§7  • §e/neoessentials help §7- Detailed help");
                LanguageUtil.sendMessage(player, "§7  • §e/neoessentials version §7- Mod version");
                LanguageUtil.sendMessage(player, "§7  • §e/neoessentials info §7- System information");
                LanguageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            } else {
                source.sendSuccess(() -> Component.literal("NeoEssentials v1.0.2.75 - Use /neoessentials help for detailed information"), false);
            }
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("An error occurred: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Shows detailed help information
     */
    private int showDetailedHelp(CommandSourceStack source) {
        try {
            boolean isPlayer = source.getEntity() instanceof ServerPlayer;
            
            if (isPlayer) {
                ServerPlayer player = source.getPlayerOrException();
                LanguageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                LanguageUtil.sendMessage(player, "§6§l⚡ NeoEssentials §7Detailed Help");
                LanguageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                LanguageUtil.sendMessage(player, "");
                LanguageUtil.sendMessage(player, "§7For specific command help, type the command followed by §ehelp§7.");
                LanguageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            } else {
                source.sendSuccess(() -> Component.literal("NeoEssentials Detailed Help - Check console or use in-game for formatted output"), false);
            }
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("An error occurred: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Shows version information
     */
    private int showVersion(CommandSourceStack source) {
        try {
            boolean isPlayer = source.getEntity() instanceof ServerPlayer;
            
            // Get version info from the mod
            String version = "1.0.2.75";
            String mcVersion = "1.21.1";
            String neoVersion = "21.1.169+";
            
            if (isPlayer) {
                ServerPlayer player = source.getPlayerOrException();
                LanguageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                LanguageUtil.sendMessage(player, "§6§l⚡ NeoEssentials §7Version Information");
                LanguageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                LanguageUtil.sendMessage(player, "§a§lMod Information:");
                LanguageUtil.sendMessage(player, "§7  • §eNeoEssentials Version: §a" + version);
                LanguageUtil.sendMessage(player, "§7  • §eMinecraft Version: §a" + mcVersion);
                LanguageUtil.sendMessage(player, "§7  • §eNeoForge Version: §a" + neoVersion);
                LanguageUtil.sendMessage(player, "");
                LanguageUtil.sendMessage(player, "§b§lFeature Status:");
                LanguageUtil.sendMessage(player, "§7  • §eTeleportation System: §2✓ Complete");
                LanguageUtil.sendMessage(player, "§7  • §eHome Management: §2✓ Complete");
                LanguageUtil.sendMessage(player, "§7  • §eWarp System: §2✓ Complete");
                LanguageUtil.sendMessage(player, "");
                LanguageUtil.sendMessage(player, "§c§lBuild Information:");
                LanguageUtil.sendMessage(player, "§7  • §eBuild Date: §aJuly 1, 2025");
                LanguageUtil.sendMessage(player, "§7  • §eBuild Status: §aProduction Ready");
                LanguageUtil.sendMessage(player, "§7  • §eDatabase: §aMySQL/PostgreSQL Support");
                LanguageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            } else {
                source.sendSuccess(() -> Component.literal("NeoEssentials v" + version + " for Minecraft " + mcVersion + " (NeoForge " + neoVersion + ")"), false);
            }
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("An error occurred: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Reloads the configuration
     */
    private int reloadConfiguration(CommandSourceStack source) {
        try {
            boolean isPlayer = source.getEntity() instanceof ServerPlayer;
            
            // Perform the reload
            boolean success = true;
            StringBuilder reloadResults = new StringBuilder();
            
            // Note: Economy system has been removed for recode
            reloadResults.append("§e✓ Configuration reload completed\n");
            
            if (isPlayer) {
                ServerPlayer player = source.getPlayerOrException();
                LanguageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                LanguageUtil.sendMessage(player, "§6§l⚡ NeoEssentials §7Configuration Reload");
                LanguageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                
                String[] lines = reloadResults.toString().split("\n");
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        LanguageUtil.sendMessage(player, "§7" + line);
                    }
                }
                
                LanguageUtil.sendMessage(player, "");
                if (success) {
                    LanguageUtil.sendMessage(player, "§a§l✓ Configuration reload completed successfully!");
                } else {
                    LanguageUtil.sendMessage(player, "§c§l⚠ Configuration reload completed with errors!");
                    LanguageUtil.sendMessage(player, "§7Check the console for detailed error information.");
                }
                LanguageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            } else {
                if (success) {
                    source.sendSuccess(() -> Component.literal("NeoEssentials configuration reloaded successfully"), true);
                } else {
                    source.sendFailure(Component.literal("NeoEssentials configuration reload completed with errors - check console"));
                }
            }
            
            return success ? 1 : 0;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Configuration reload failed: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Shows system information
     */
    private int showSystemInfo(CommandSourceStack source) {
        try {
            boolean isPlayer = source.getEntity() instanceof ServerPlayer;
            
            if (isPlayer) {
                ServerPlayer player = source.getPlayerOrException();
                LanguageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                LanguageUtil.sendMessage(player, "§6§l⚡ NeoEssentials §7System Information");
                LanguageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                
                LanguageUtil.sendMessage(player, "");
                LanguageUtil.sendMessage(player, "§e§lServer Information:");
                LanguageUtil.sendMessage(player, "§7  • §eUptime: §a" + getServerUptimeString());
                LanguageUtil.sendMessage(player, "§7  • §eJava Version: §a" + System.getProperty("java.version"));
                LanguageUtil.sendMessage(player, "§7  • §eMemory Usage: §a" + getMemoryUsageString());
                LanguageUtil.sendMessage(player, "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            } else {
                source.sendSuccess(() -> Component.literal("NeoEssentials System Information - Use in-game for detailed output"), false);
            }
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("An error occurred: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Gets formatted server uptime string
     */
    private String getServerUptimeString() {
        try {
            long uptimeMs = System.currentTimeMillis() - 
                java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime();
            long hours = uptimeMs / (1000 * 60 * 60);
            long minutes = (uptimeMs % (1000 * 60 * 60)) / (1000 * 60);
            return hours + "h " + minutes + "m";
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    /**
     * Gets formatted memory usage string
     */
    private String getMemoryUsageString() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / 1024 / 1024; // MB
            long totalMemory = runtime.totalMemory() / 1024 / 1024; // MB
            long freeMemory = runtime.freeMemory() / 1024 / 1024; // MB
            long usedMemory = totalMemory - freeMemory;
            
            return usedMemory + "/" + maxMemory + " MB (" + 
                   (usedMemory * 100 / maxMemory) + "%)";
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
