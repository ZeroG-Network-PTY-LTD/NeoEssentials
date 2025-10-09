package com.zerog.neoessentials.kits.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
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
                .executes(KitCommand::useKit)
            )
        );
    }
    
    private static final SuggestionProvider<CommandSourceStack> suggestKits = 
        (context, builder) -> suggestKits(context, builder);
    
    private static CompletableFuture<Suggestions> suggestKits(CommandContext<CommandSourceStack> context, 
                                                             SuggestionsBuilder builder) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return builder.buildFuture();
        }
        
        try {
            KitManager kitManager = KitManager.getInstance();
            for (Kit kit : kitManager.getAvailableKits(player)) {
                builder.suggest(kit.getName());
            }
        } catch (Exception e) {
            LOGGER.error("Error suggesting kits: {}", e.getMessage(), e);
        }
        
        return builder.buildFuture();
    }
    
    private static int listAvailableKits(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = (ServerPlayer) source.getEntity();
        
        try {
            KitManager kitManager = KitManager.getInstance();
            var availableKits = kitManager.getAvailableKits(player);
            
            if (availableKits.isEmpty()) {
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.kit.no_kits_available"), false);
                return 1;
            }
            
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.kit.available_kits_header"), false);
            
            for (Kit kit : availableKits) {
                var canUse = kitManager.canUseKit(player, kit.getName());
                String status = canUse.isAllowed() ? "§aReady" : "§c" + canUse.getMessage();
                
                source.sendSuccess(() -> MessageUtil.component(
                    "commands.neoessentials.kit.kit_list_entry",
                    kit.getName(),
                    kit.getDisplayName(),
                    kit.getDescription(),
                    kit.getItems().size(),
                    kit.getCooldownDisplay(),
                    status
                ), false);
            }
            
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.kit.use_kit_hint"), false);
            return 1;
            
        } catch (Exception e) {
            LOGGER.error("Error listing kits for player {}: {}", player.getName().getString(), e.getMessage(), e);
            source.sendFailure(MessageUtil.error("commands.neoessentials.kit.error_listing"));
            return 0;
        }
    }
    
    private static int useKit(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = (ServerPlayer) source.getEntity();
        String kitName = StringArgumentType.getString(context, "kitname");
        
        try {
            KitManager kitManager = KitManager.getInstance();
            Kit kit = kitManager.getKit(kitName);
            
            if (kit == null) {
                source.sendFailure(MessageUtil.error("commands.neoessentials.kit.not_found", kitName));
                return 0;
            }
            
            // Check if player can use this kit
            var result = kitManager.canUseKit(player, kitName);
            if (!result.isAllowed()) {
                source.sendFailure(MessageUtil.error("commands.neoessentials.kit.cannot_use", result.getMessage()));
                return 0;
            }
            
            // Give the kit
            var giveResult = kitManager.giveKit(player, kitName);
            if (giveResult.isAllowed()) {
                source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.kit.given", 
                    kit.getDisplayName(), giveResult.getMessage()), false);
                LOGGER.info("Player {} used kit '{}'", player.getName().getString(), kitName);
                return 1;
            } else {
                source.sendFailure(MessageUtil.error("commands.neoessentials.kit.give_failed", 
                    giveResult.getMessage()));
                return 0;
            }
            
        } catch (Exception e) {
            LOGGER.error("Error giving kit '{}' to player {}: {}", 
                        kitName, player.getName().getString(), e.getMessage(), e);
            source.sendFailure(MessageUtil.error("commands.neoessentials.kit.error_using"));
            return 0;
        }
    }
}