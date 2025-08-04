package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;

import java.text.DecimalFormat;
import java.util.List;

public class InfoCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("info")
            .executes(InfoCommand::showServerInfo));
        
        // Also register /serverinfo as an alias
        dispatcher.register(Commands.literal("serverinfo")
            .executes(InfoCommand::showServerInfo));
    }
    
    /**
     * Execute /info command to display server information
     */
    private static int showServerInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        
        // Get system information
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        DecimalFormat df = new DecimalFormat("#.##");
        
        // Send header
        sendMessage(source, "§6========== §eServer Information §6==========");
        
        // Server basics
        sendMessage(source, "§7Server Version: §a" + server.getServerVersion());
        sendMessage(source, "§7Minecraft Version: §a" + server.getServerModName());
        sendMessage(source, "§7MOTD: §a" + server.getMotd());
        
        // Player information
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        int maxPlayers = server.getMaxPlayers();
        sendMessage(source, "§7Players Online: §a" + players.size() + "/" + maxPlayers);
        
        // Memory information
        sendMessage(source, "§7Memory Usage: §a" + formatBytes(usedMemory) + " / " + formatBytes(maxMemory));
        double memoryPercent = (double) usedMemory / maxMemory * 100;
        sendMessage(source, "§7Memory Percent: §a" + df.format(memoryPercent) + "%");
        
        // TPS information (simplified)
        sendMessage(source, "§7Server Running: §aYes");
        
        // World information
        var overworld = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
        if (overworld != null) {
            GameRules gameRules = overworld.getGameRules();
            long worldTime = overworld.getDayTime();
            int day = (int) (worldTime / 24000L);
            
            sendMessage(source, "§7Current Day: §a" + (day + 1));
            sendMessage(source, "§7Keep Inventory: §a" + gameRules.getBoolean(GameRules.RULE_KEEPINVENTORY));
            sendMessage(source, "§7Mob Griefing: §a" + gameRules.getBoolean(GameRules.RULE_MOBGRIEFING));
            sendMessage(source, "§7Fire Spread: §a" + gameRules.getBoolean(GameRules.RULE_DOFIRETICK));
            sendMessage(source, "§7Difficulty: §a" + overworld.getDifficulty().getDisplayName().getString());
        }
        
        // Server tick count
        sendMessage(source, "§7Server Ticks: §a" + server.getTickCount());
        
        // NeoEssentials version
        sendMessage(source, "§7NeoEssentials: §aVersion 1.0.2");
        
        sendMessage(source, "§6==========================================");
        
        return 1;
    }
    
    private static void sendMessage(CommandSourceStack source, String message) {
        if (source.getEntity() instanceof ServerPlayer player) {
            MessageUtil.sendMessage(player, message);
        } else {
            source.sendSuccess(() -> Component.literal(message), false);
        }
    }
    
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), units[exp]);
    }
}
