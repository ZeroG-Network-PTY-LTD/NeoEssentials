package com.zerog.neoessentials.kits.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.zerog.neoessentials.kits.Kit;
import com.zerog.neoessentials.kits.KitManager;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Handles the /kit command for giving players kits.
 */
public class KitCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(KitCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Check if kit module is enabled
        if (!com.zerog.neoessentials.config.ConfigManager.isKitSystemEnabled()) {
            return; // Don't register kit commands if module is disabled
        }
        
        dispatcher.register(Commands.literal("kit")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return PermissionAPI.hasPermission(player.getUUID(), "neoessentials.kits.use");
                }
                return false; // Console can't use kits
            })
            .executes(KitCommand::listAvailableKits)
            .then(Commands.argument("kitname", StringArgumentType.word())
                .suggests(KitCommand::suggestKits)
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    ServerPlayer player = (ServerPlayer) source.getEntity();
                    if (player == null) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.kits.no_player"));
                        return 0;
                    }
                    String kitName = StringArgumentType.getString(context, "kitname");
                    Kit kit = null;
                    try {
                        kit = KitManager.getInstance().getKit(kitName);
                    } catch (Exception e) {
                        // fallback, handled below
                    }
                    // Strict permission: require both general and per-kit permission
                    if (!PermissionAPI.hasPermission(player.getUUID(), "neoessentials.kits.use")) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.kits.no_permission_general"));
                        return 0;
                    }
                    // Always check per-kit permission node (auto-generated if not set)
                    String kitPerm = kit != null && kit.getPermission() != null && !kit.getPermission().isEmpty()
                        ? kit.getPermission()
                        : ("neoessentials.kits." + kitName.toLowerCase());
                    boolean hasKitPerm = PermissionAPI.hasPermission(player.getUUID(), kitPerm);
                    if (!hasKitPerm) {
                        source.sendFailure(MessageUtil.error("commands.neoessentials.kits.no_permission_kit", kitName));
                        return 0;
                    }
                    // Debug log
                    org.slf4j.LoggerFactory.getLogger(KitCommand.class).debug("Kit permission check: {} for {}: {}", kitPerm, player.getUUID(), hasKitPerm);
                    return useKit(context);
                })
            )
        );
    }
    
    private static CompletableFuture<Suggestions> suggestKits(CommandContext<CommandSourceStack> context, 
                                                             SuggestionsBuilder builder) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return builder.buildFuture();
        }
        try {
            KitManager kitManager = KitManager.getInstance();
            for (Kit kit : kitManager.getAllKits()) {
                String kitPerm = kit.getPermission() != null && !kit.getPermission().isEmpty()
                        ? kit.getPermission()
                        : ("neoessentials.kits." + kit.getName().toLowerCase());
                boolean hasGeneral = PermissionAPI.hasPermission(player.getUUID(), "neoessentials.kits.use");
                boolean hasKitPerm = PermissionAPI.hasPermission(player.getUUID(), kitPerm);
                if (hasGeneral && hasKitPerm) {
                    builder.suggest(kit.getName());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error suggesting kits: {}", e.getMessage(), e);
        }
        return builder.buildFuture();
    }
    
    private static int listAvailableKits(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = (ServerPlayer) source.getEntity();
        if (player == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.kits.no_player"));
            return 0;
        }
        try {
            KitManager kitManager = KitManager.getInstance();
            var availableKits = kitManager.getAvailableKits(player);
            
            if (availableKits.isEmpty()) {
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.listkits.empty"), false);
                return 1;
            }
            
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.listkits.header", 
                1, 1, availableKits.size()), false);
            source.sendSuccess(() -> MessageUtil.coloredText("§7▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"), false);
            
            int index = 1;
            for (Kit kit : availableKits) {
                displayKitInfo(source, kit, index++);
            }
            
            source.sendSuccess(() -> MessageUtil.coloredText("§7▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"), false);
            return 1;
            
        } catch (Exception e) {
            // player is guaranteed non-null here
            LOGGER.error("Error listing kits for player {}: {}", player.getName().getString(), e.getMessage(), e);
            source.sendFailure(MessageUtil.error("commands.neoessentials.listkits.error"));
            return 0;
        }
    }
    
    private static void displayKitInfo(CommandSourceStack source, Kit kit, int index) {
        String name = kit.getName();
        String displayName = kit.getDisplayName();
        int itemCount = kit.getItems().size();
        String cooldown = formatCooldown(kit.getCooldownMillis());
        String permission = kit.getPermission();
        
        // Basic kit info
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.listkits.kit_entry", 
            index, name, displayName, itemCount), false);
        
        // Additional details
        if (!cooldown.equals("none")) {
            source.sendSuccess(() -> MessageUtil.coloredText("§8  └ Cooldown: " + cooldown), false);
        }
        
        if (permission != null && !permission.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.coloredText("§8  └ Permission: " + permission), false);
        }
        
        if (kit.getDescription() != null && !kit.getDescription().isEmpty()) {
            final String desc = kit.getDescription().length() > 60 ? 
                kit.getDescription().substring(0, 57) + "..." : kit.getDescription();
            source.sendSuccess(() -> MessageUtil.coloredText("§8  └ Description: " + desc), false);
        }
    }
    
    private static String formatCooldown(long millis) {
        if (millis == 0) return "none";
        
        long seconds = millis / 1000;
        if (seconds < 60) return seconds + "s";
        
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m";
        
        long hours = minutes / 60;
        if (hours < 24) {
            long remainingMinutes = minutes % 60;
            return remainingMinutes > 0 ? hours + "h " + remainingMinutes + "m" : hours + "h";
        }
        
        long days = hours / 24;
        long remainingHours = hours % 24;
        return remainingHours > 0 ? days + "d " + remainingHours + "h" : days + "d";
    }
    
    private static int useKit(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = (ServerPlayer) source.getEntity();
        if (player == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.kits.no_player"));
            return 0;
        }
        String kitName = StringArgumentType.getString(context, "kitname");
        try {
            // Check and deduct kit command cost if economy is enabled
            int cost = (int) com.zerog.neoessentials.config.ConfigManager.getKitCommandCost("kit");
            if (cost > 0 && com.zerog.neoessentials.economy.managers.EconomyManager.getInstance().isEnabled()) {
                var eco = com.zerog.neoessentials.economy.managers.EconomyManager.getInstance();
                var bal = eco.getBalance(player.getUUID());
                if (bal.doubleValue() < cost) {
                    source.sendFailure(com.zerog.neoessentials.util.MessageUtil.error("commands.neoessentials.listkits.not_enough_money", cost));
                    return 0;
                }
                if (!eco.subtractBalance(player.getUUID(), java.math.BigDecimal.valueOf(cost))) {
                    source.sendFailure(com.zerog.neoessentials.util.MessageUtil.error("commands.neoessentials.listkits.charge_failed"));
                    return 0;
                }
            }
            KitManager kitManager = KitManager.getInstance();
            Kit kit = kitManager.getKit(kitName);
            if (kit == null) {
                source.sendFailure(MessageUtil.error("commands.neoessentials.listkits.not_found", kitName));
                return 0;
            }
            // Check if player can use this kit
            var result = kitManager.canUseKit(player, kitName);
            if (!result.isAllowed()) {
                source.sendFailure(MessageUtil.error("commands.neoessentials.listkits.cannot_use", result.getMessage()));
                return 0;
            }
            // Give the kit
            var giveResult = kitManager.giveKit(player, kitName);
            if (giveResult.isAllowed()) {
                source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.listkits.given", 
                    kit.getDisplayName()), false);
                LOGGER.info("Player {} used kit '{}'", player.getName().getString(), kitName);
                return 1;
            } else {
                source.sendFailure(MessageUtil.error("commands.neoessentials.listkits.give_failed", 
                    giveResult.getMessage()));
                return 0;
            }

        } catch (Exception e) {
            LOGGER.error("Error giving kit '{}' to player {}: {}",
                        kitName, player.getName().getString(), e.getMessage(), e);
            source.sendFailure(MessageUtil.error("commands.neoessentials.listkits.error_using"));
            return 0;
        }
    }
}