package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.text.DecimalFormat;
import java.util.Optional;

public class WhoisCommand {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("whois")) return;
        
        dispatcher.register(Commands.literal("whois")
            .requires(source -> {
                PermissionValidator.PermissionResult result = PermissionValidator.validatePermission(source, "neoessentials.whois");
                return result.hasPermission();
            })
            .then(Commands.argument("player", EntityArgument.player())
                .executes(WhoisCommand::whoisPlayer)
            )
            .then(Commands.argument("playername", StringArgumentType.word())
                .executes(WhoisCommand::whoisPlayerByName)
            )
        );
    }

    private static int whoisPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        return showPlayerInfo(context, targetPlayer);
    }

    private static int whoisPlayerByName(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String playerName = StringArgumentType.getString(context, "playername");
        MinecraftServer server = context.getSource().getServer();
        
        // Try to find online player first
        ServerPlayer targetPlayer = server.getPlayerList().getPlayerByName(playerName);
        
        if (targetPlayer != null) {
            return showPlayerInfo(context, targetPlayer);
        }
        
        // If not online, try to find offline player data
        return showOfflinePlayerInfo(context, playerName);
    }

    private static int showPlayerInfo(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
        CommandSourceStack source = context.getSource();
        PermissionValidator.PermissionResult detailedResult = PermissionValidator.validatePermission(source, "neoessentials.whois.detailed");
        boolean canSeeDetailed = detailedResult.hasPermission();
        
        // Header
        MutableComponent header = Component.literal("§6§l┌─ Player Information: " + targetPlayer.getName().getString() + " ─┐");
        source.sendSuccess(() -> header, false);
        
        // Basic Information
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.whois.separator"), false);

        // Display Name (if different from username)
        String displayName = targetPlayer.getDisplayName().getString();
        String realName = targetPlayer.getName().getString();
        if (!displayName.equals(realName)) {
            MutableComponent nickInfo = Component.literal("§aNickname: §f" + displayName + " §7(Real: " + realName + ")");
            source.sendSuccess(() -> nickInfo, false);
        } else {
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.whois.username", realName), false);
        }
        
        // UUID (for admins)
        if (canSeeDetailed) {
            MutableComponent uuidComponent = Component.literal("§bUUID: §7" + targetPlayer.getUUID().toString())
                .withStyle(style -> style
                    .withClickEvent(com.zerog.neoessentials.util.ClickEventCompat.create(ClickEvent.Action.COPY_TO_CLIPBOARD, targetPlayer.getUUID().toString()))
                    .withHoverEvent(com.zerog.neoessentials.util.HoverEventCompat.create(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy UUID")))
                );
            source.sendSuccess(() -> uuidComponent, false);
        }
        
        // Status
        String status = "§aOnline";
        // Note: Vanish detection would be added here if VanishHandler exists
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.whois.status", status), false);

        // Game Mode
        GameType gameType = targetPlayer.gameMode.getGameModeForPlayer();
        String gameModeName = switch (gameType) {
            case SURVIVAL -> "§aSurvival";
            case CREATIVE -> "§6Creative";
            case ADVENTURE -> "§9Adventure";
            case SPECTATOR -> "§7Spectator";
        };
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.whois.gamemode", gameModeName), false);

        // Health and Food (for admins or self)
        try {
            ServerPlayer viewer = source.getPlayerOrException();
            if (canSeeDetailed || viewer.equals(targetPlayer)) {
                float health = targetPlayer.getHealth();
                float maxHealth = targetPlayer.getMaxHealth();
                int foodLevel = targetPlayer.getFoodData().getFoodLevel();
                
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.whois.health_online",
                    DECIMAL_FORMAT.format(health), DECIMAL_FORMAT.format(maxHealth)), false);
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.whois.food", foodLevel), false);
            }
        } catch (CommandSyntaxException ignored) {
            // Console execution, show detailed info
            if (canSeeDetailed) {
                float health = targetPlayer.getHealth();
                float maxHealth = targetPlayer.getMaxHealth();
                int foodLevel = targetPlayer.getFoodData().getFoodLevel();
                
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.whois.health_offline",
                    DECIMAL_FORMAT.format(health), DECIMAL_FORMAT.format(maxHealth)), false);
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.whois.food", foodLevel), false);
            }
        }
        
        // Location (for admins)
        if (canSeeDetailed) {
            double x = targetPlayer.getX();
            double y = targetPlayer.getY();
            double z = targetPlayer.getZ();
            String dimension = targetPlayer.level().dimension().location().toString();
            
            MutableComponent locationComponent = Component.literal("§dLocation: §f" + 
                DECIMAL_FORMAT.format(x) + ", " + DECIMAL_FORMAT.format(y) + ", " + DECIMAL_FORMAT.format(z) + 
                " §7in §f" + dimension)
                .withStyle(style -> style
                    .withClickEvent(com.zerog.neoessentials.util.ClickEventCompat.create(ClickEvent.Action.SUGGEST_COMMAND, 
                        "/tp " + DECIMAL_FORMAT.format(x) + " " + DECIMAL_FORMAT.format(y) + " " + DECIMAL_FORMAT.format(z)))
                    .withHoverEvent(com.zerog.neoessentials.util.HoverEventCompat.create(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to get teleport command")))
                );
            source.sendSuccess(() -> locationComponent, false);
        }
        
        // Experience Level
        int expLevel = targetPlayer.experienceLevel;
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.whois.experience_level", expLevel), false);

        // IP Address (for admins only)
        if (canSeeDetailed && targetPlayer.connection != null) {
            String ipAddress = targetPlayer.connection.getRemoteAddress().toString();
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.whois.ip_address", ipAddress), false);
        }
        
        // Play time (if available through SeenCommand data)
        showPlayTimeInfo(source, targetPlayer);
        
        // Footer
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.whois.separator"), false);

        return 1;
    }

    private static int showOfflinePlayerInfo(CommandContext<CommandSourceStack> context, String playerName) {
        CommandSourceStack source = context.getSource();
        PermissionValidator.PermissionResult detailedResult = PermissionValidator.validatePermission(source, "neoessentials.whois.detailed");
        boolean canSeeDetailed = detailedResult.hasPermission();
        
        // Try to get offline player data from SeenCommand
        if (canSeeDetailed) {
            // Check if we have data from SeenCommand
            Optional<Component> seenInfo = getOfflinePlayerSeenInfo(playerName);
            if (seenInfo.isPresent()) {
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.whois.offline_header", playerName), false);
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.whois.separator"), false);
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.whois.status_offline"), false);
                source.sendSuccess(() -> seenInfo.get(), false);
                source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.whois.separator"), false);
                return 1;
            }
        }
        
        // Player not found
        source.sendFailure(Component.translatable("commands.neoessentials.whois.player_not_found", playerName));
        return 0;
    }

    private static void showPlayTimeInfo(CommandSourceStack source, ServerPlayer player) {
        // This would integrate with SeenCommand data if available
        // For now, we'll show current session time
        try {
            // This is a placeholder - in a real implementation, you'd track when the player joined
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.whois.session_time"), false);
        } catch (Exception e) {
            // Skip if we can't get play time info
        }
    }

    private static Optional<Component> getOfflinePlayerSeenInfo(String playerName) {
        // This would integrate with SeenCommand data storage
        // For now, return empty - in a real implementation, you'd check the seen data file
        return Optional.empty();
    }
}