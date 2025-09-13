package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.api.NeoEssentialsAPI;
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
        sendTranslatedMessage(source, "neoessentials.info.header");

        // Server basics
        sendTranslatedMessage(source, "neoessentials.info.server_version", server.getServerVersion());
        // Clarify that this is the modded server name, not the vanilla Minecraft version
        sendTranslatedMessage(source, "neoessentials.info.minecraft_version", server.getServerModName() + " (modded server name)");
        sendTranslatedMessage(source, "neoessentials.info.motd", server.getMotd());

        // Player information
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        int maxPlayers = server.getMaxPlayers();
        sendTranslatedMessage(source, "neoessentials.info.players_online", players.size(), maxPlayers);

        // Memory information
        sendTranslatedMessage(source, "neoessentials.info.memory_usage", formatBytes(usedMemory), formatBytes(maxMemory));
        double memoryPercent = (double) usedMemory / maxMemory * 100;
        sendTranslatedMessage(source, "neoessentials.info.memory_percent", df.format(memoryPercent));

        // TPS information (not available)
        sendTranslatedMessage(source, "neoessentials.info.tps_unavailable");

        // World information
        var overworld = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
        if (overworld != null) {
            GameRules gameRules = overworld.getGameRules();
            long worldTime = overworld.getDayTime();
            int day = (int) (worldTime / 24000L);

            sendTranslatedMessage(source, "neoessentials.info.current_day", day + 1);
            sendTranslatedMessage(source, "neoessentials.info.keep_inventory", gameRules.getBoolean(GameRules.RULE_KEEPINVENTORY));
            sendTranslatedMessage(source, "neoessentials.info.mob_griefing", gameRules.getBoolean(GameRules.RULE_MOBGRIEFING));
            sendTranslatedMessage(source, "neoessentials.info.fire_spread", gameRules.getBoolean(GameRules.RULE_DOFIRETICK));
            sendTranslatedMessage(source, "neoessentials.info.difficulty", overworld.getDifficulty().getDisplayName().getString());
        }

        // Server tick count
        sendTranslatedMessage(source, "neoessentials.info.server_ticks", server.getTickCount());

        // NeoEssentials version (use API)
        sendTranslatedMessage(source, "neoessentials.info.mod_version", NeoEssentialsAPI.getModVersion());

        sendTranslatedMessage(source, "neoessentials.info.footer");
        
        return 1;
    }
    
    private static void sendTranslatedMessage(CommandSourceStack source, String key, Object... args) {
        if (source.getEntity() instanceof ServerPlayer player) {
            MessageUtil.sendTranslatedMessage(player, key, args);
        } else {
            source.sendSuccess(() -> MessageUtil.translatable(key, args), false);
        }
    }
    
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), units[exp]);
    }
}