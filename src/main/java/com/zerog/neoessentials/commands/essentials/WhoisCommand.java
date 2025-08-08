package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.text.DecimalFormat;

/**
 * Whois command implementation for NeoEssentials
 * Shows detailed information about a player
 * 
 * Commands:
 * - /whois <player> - Show detailed player information
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class WhoisCommand {
    
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("whois")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ESSENTIALS_USE))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(WhoisCommand::showPlayerInfo)));
    }
    
    /**
     * Execute /whois <player> command to show player information
     */
    private static int showPlayerInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        
        if (targetPlayer == null) {
            context.getSource().sendFailure(Component.literal("§cPlayer not found!"));
            return 0;
        }
        
        String playerName = targetPlayer.getName().getString();
        
        // Header
        context.getSource().sendSuccess(() -> Component.literal("§6=== Player Info: " + playerName + " ==="), false);
        
        // Basic info
        context.getSource().sendSuccess(() -> Component.literal("§7Display Name: §f" + targetPlayer.getDisplayName().getString()), false);
        context.getSource().sendSuccess(() -> Component.literal("§7UUID: §f" + targetPlayer.getUUID().toString()), false);
        
        // Game mode
        GameType gameMode = targetPlayer.gameMode.getGameModeForPlayer();
        String gameModeColor = switch (gameMode) {
            case SURVIVAL -> "§a";
            case CREATIVE -> "§6";
            case ADVENTURE -> "§e";
            case SPECTATOR -> "§7";
        };
        context.getSource().sendSuccess(() -> Component.literal("§7Game Mode: " + gameModeColor + gameMode.getName()), false);
        
        // Health and food
        float health = targetPlayer.getHealth();
        float maxHealth = targetPlayer.getMaxHealth();
        int foodLevel = targetPlayer.getFoodData().getFoodLevel();
        context.getSource().sendSuccess(() -> Component.literal(String.format("§7Health: §c%.1f§7/§c%.1f §7| Food: §6%d§7/§620", health, maxHealth, foodLevel)), false);
        
        // Position
        double x = targetPlayer.getX();
        double y = targetPlayer.getY();
        double z = targetPlayer.getZ();
        String world = targetPlayer.level().dimension().location().toString();
        context.getSource().sendSuccess(() -> Component.literal(String.format("§7Location: §b%s §7at §b%s§7, §b%s§7, §b%s", 
            world, DECIMAL_FORMAT.format(x), DECIMAL_FORMAT.format(y), DECIMAL_FORMAT.format(z))), false);
        
        // Permission level
        int permissionLevel = context.getSource().getServer().getProfilePermissions(targetPlayer.getGameProfile());
        String permissionText = switch (permissionLevel) {
            case 0 -> "§aPlayer";
            case 1 -> "§eModerator";
            case 2 -> "§6Admin";
            case 3 -> "§cOperator";
            case 4 -> "§4Owner";
            default -> "§7Unknown";
        };
        context.getSource().sendSuccess(() -> Component.literal("§7Permission Level: " + permissionText + " §7(" + permissionLevel + ")"), false);
        
        // Special status
        StringBuilder status = new StringBuilder("§7Status: ");
        boolean hasStatus = false;
        
        if (targetPlayer.getAbilities().invulnerable) {
            status.append("§eGod Mode");
            hasStatus = true;
        }
        
        if (targetPlayer.isInvisible()) {
            if (hasStatus) status.append("§7, ");
            status.append("§7Vanished");
            hasStatus = true;
        }
        
        if (targetPlayer.getAbilities().flying) {
            if (hasStatus) status.append("§7, ");
            status.append("§9Flying");
            hasStatus = true;
        }
        
        if (!hasStatus) {
            status.append("§aNormal");
        }
        
        context.getSource().sendSuccess(() -> Component.literal(status.toString()), false);
        
        // Experience info
        int expLevel = targetPlayer.experienceLevel;
        float expProgress = targetPlayer.experienceProgress;
        context.getSource().sendSuccess(() -> Component.literal(String.format("§7Experience: §aLevel %d §7(§a%.1f%% §7to next level)", expLevel, expProgress * 100)), false);
        
        // Connection info (ping)
        int ping = targetPlayer.connection.latency();
        String pingColor = ping < 50 ? "§a" : ping < 100 ? "§e" : ping < 200 ? "§6" : "§c";
        context.getSource().sendSuccess(() -> Component.literal("§7Ping: " + pingColor + ping + "ms"), false);
        
        return 1;
    }
}
