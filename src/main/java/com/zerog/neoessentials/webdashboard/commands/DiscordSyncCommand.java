package com.zerog.neoessentials.webdashboard.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import com.zerog.neoessentials.util.PermissionValidator.PermissionResult;
import com.zerog.neoessentials.webdashboard.security.DiscordPermissionSync;
import com.zerog.neoessentials.webdashboard.security.DiscordAuthConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/**
 * Command to manage Discord permission synchronization
 * Usage: /discord syncperms [player|all]
 */
public class DiscordSyncCommand {
    
    private static final String PERM_SYNC = "neoessentials.discord.syncperms";
    private static final String PERM_SYNC_OTHERS = "neoessentials.discord.syncperms.others";
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("discord")
            .then(Commands.literal("syncperms")
                // Sync self
                .executes(DiscordSyncCommand::syncSelf)
                // Sync specific player
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(DiscordSyncCommand::syncPlayer))
                // Sync all online players
                .then(Commands.literal("all")
                    .executes(DiscordSyncCommand::syncAll)))
        );
    }
    
    /**
     * Sync permissions for the command executor
     */
    private static int syncSelf(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        // Check permission
        PermissionResult permResult = PermissionValidator.validatePermission(source, PERM_SYNC);
        if (!permResult.hasPermission()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.no_permission"));
            return 0;
        }
        
        // Must be a player
        try {
            ServerPlayer player = source.getPlayerOrException();
            return performSync(source, player);
        } catch (Exception e) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.discord.sync.console_not_allowed"));
            return 0;
        }
    }
    
    /**
     * Sync permissions for a specific player
     */
    private static int syncPlayer(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        // Check permission
        PermissionResult permResult = PermissionValidator.validatePermission(source, PERM_SYNC_OTHERS);
        if (!permResult.hasPermission()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.no_permission"));
            return 0;
        }
        
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            return performSync(source, targetPlayer);
        } catch (Exception e) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.discord.sync.player_not_found"));
            return 0;
        }
    }
    
    /**
     * Sync permissions for all online players
     */
    private static int syncAll(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        // Check permission
        PermissionResult permResult = PermissionValidator.validatePermission(source, PERM_SYNC_OTHERS);
        if (!permResult.hasPermission()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.no_permission"));
            return 0;
        }
        
        // Check if sync is enabled
        DiscordAuthConfig config = DiscordAuthConfig.load();
        if (!config.isPermissionSyncEnabled()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.discord.sync.disabled"));
            return 0;
        }
        
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.discord.sync.starting_all"), true);
        
        // Run sync asynchronously
        new Thread(() -> {
            try {
                DiscordPermissionSync syncService = DiscordPermissionSync.getInstance();
                Map<String, DiscordPermissionSync.SyncResult> results = syncService.syncAllOnlinePlayers(source.getServer());
                
                int successCount = 0;
                int totalGranted = 0;
                
                for (Map.Entry<String, DiscordPermissionSync.SyncResult> entry : results.entrySet()) {
                    if (entry.getValue().isSuccess()) {
                        successCount++;
                        totalGranted += entry.getValue().getPermissionsGranted();
                    }
                }
                
                int finalSuccessCount = successCount;
                int finalTotalGranted = totalGranted;
                int totalPlayers = results.size();
                
                source.sendSuccess(() -> Component.literal("╔════════════════════════════════════════╗\n")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal("║  Discord Permission Sync Complete     ║\n")
                        .withStyle(ChatFormatting.GREEN))
                    .append(Component.literal("╠════════════════════════════════════════╣\n")
                        .withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(String.format("║  Players Synced: %d/%d                  ║\n", finalSuccessCount, totalPlayers))
                        .withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(String.format("║  Permissions Granted: %d               ║\n", finalTotalGranted))
                        .withStyle(ChatFormatting.AQUA))
                    .append(Component.literal("╚════════════════════════════════════════╝")
                        .withStyle(ChatFormatting.GREEN)), false);
                
            } catch (Exception e) {
                source.sendFailure(MessageUtil.error("commands.neoessentials.discord.sync.error", e.getMessage()));
            }
        }, "DiscordSyncAll").start();
        
        return 1;
    }
    
    /**
     * Perform sync for a single player
     */
    private static int performSync(CommandSourceStack source, ServerPlayer player) {
        String playerName = player.getName().getString();
        
        // Check if sync is enabled
        DiscordAuthConfig config = DiscordAuthConfig.load();
        if (!config.isPermissionSyncEnabled()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.discord.sync.disabled"));
            return 0;
        }
        
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.discord.sync.starting", playerName), false);
        
        // Run sync
        DiscordPermissionSync syncService = DiscordPermissionSync.getInstance();
        DiscordPermissionSync.SyncResult result = syncService.syncPlayerPermissions(player);
        
        if (result.isSuccess()) {
            source.sendSuccess(() -> Component.literal("✓ ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(MessageUtil.localize("commands.neoessentials.discord.sync.success", 
                    playerName, result.getPermissionsGranted()))
                    .withStyle(ChatFormatting.GRAY)), false);
            
            // Notify the player if it's not themselves
            try {
                ServerPlayer executor = source.getPlayerOrException();
                if (!executor.getUUID().equals(player.getUUID())) {
                    player.sendSystemMessage(
                        Component.literal("✓ ")
                            .withStyle(ChatFormatting.GREEN)
                            .append(Component.literal("Your permissions have been synced from Discord.")
                                .withStyle(ChatFormatting.GRAY))
                    );
                }
            } catch (Exception ignored) {
                // Console executed, just notify the target
                player.sendSystemMessage(
                    Component.literal("✓ ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.literal("Your permissions have been synced from Discord.")
                            .withStyle(ChatFormatting.GRAY))
                );
            }
            
            return 1;
        } else {
            source.sendFailure(MessageUtil.error("commands.neoessentials.discord.sync.failed", playerName, result.getMessage()));
            return 0;
        }
    }
}
