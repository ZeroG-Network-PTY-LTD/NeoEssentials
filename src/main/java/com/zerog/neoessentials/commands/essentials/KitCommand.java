
package com.zerog.neoessentials.commands.essentials;
import com.zerog.neoessentials.config.MainConfig;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zerog.neoessentials.managers.KitManager;
import com.zerog.neoessentials.managers.EconomyManager;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Kit command implementation for NeoEssentials
 * Provides kit distribution functionality with cooldowns, costs, and permissions
 * 
 * Commands:
 * - /kit <name> - Claim a specific kit
 * - /kit - List available kits
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class KitCommand {
    
    private static final SuggestionProvider<CommandSourceStack> KIT_SUGGESTIONS = (context, builder) -> {
        CommandSourceStack source = context.getSource();
        if (source.getEntity() instanceof ServerPlayer player) {
            KitManager kitManager = KitManager.getInstance();
            List<String> availableKits = kitManager.getAvailableKits(player);
            return SharedSuggestionProvider.suggest(availableKits, builder);
        }
        return builder.buildFuture();
    };
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kit")
            .requires(source -> source.getEntity() instanceof ServerPlayer)
            .executes(KitCommand::listKits)
            .then(Commands.argument("name", StringArgumentType.word())
                .suggests(KIT_SUGGESTIONS)
                .executes(KitCommand::giveKit)));
    }
    
    /**
     * Execute /kit command to list available kits
     */
    private static int listKits(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        KitManager kitManager = KitManager.getInstance();
    MainConfig.KitSettings config = ConfigManager.getInstance().getMainConfig().kitSettings;
        
        if (!config.enabled) {
            MessageUtil.sendMessage(player, "&cKit system is disabled.");
            return 0;
        }
        
        List<String> availableKits = kitManager.getAvailableKits(player);
        
        if (availableKits.isEmpty()) {
            MessageUtil.sendMessage(player, config.messages.kitListEmpty);
            return 0;
        }
        
        // Send header
        MessageUtil.sendMessage(player, config.messages.kitListHeader);
        
        // List each available kit with details
        for (String kitName : availableKits) {
            MainConfig.KitSettings.KitDefinition kit = config.kits.get(kitName);
            if (kit != null) {
                String delayText = kit.hasDelay() ? 
                    MessageUtil.formatTime(kit.delay * 1000L) : "None";
                String costText = kit.hasCost() ? 
                    EconomyManager.getInstance().formatCurrency(kit.cost) : "Free";
                
                // Check if on cooldown
                String status = "";
                if (kit.hasDelay() && kitManager.isOnCooldown(player, kitName)) {
                    long remaining = kitManager.getRemainingCooldown(player, kitName);
                    status = " &c(Cooldown: " + MessageUtil.formatTime(remaining) + ")";
                }
                
                String message = MessageUtil.replacePlaceholders(config.messages.kitListEntry,
                    kit.displayName, kitName, delayText, costText) + status;
                MessageUtil.sendMessage(player, message);
                
                // Show description if available
                if (!kit.description.isEmpty()) {
                    for (String desc : kit.description) {
                        MessageUtil.sendMessage(player, "  " + desc);
                    }
                }
            }
        }
        
        return 1;
    }
    
    /**
     * Execute /kit <name> command to give a specific kit
     */
    private static int giveKit(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
        String kitName = StringArgumentType.getString(context, "name");
        
        KitManager kitManager = KitManager.getInstance();
        
        // Attempt to give the kit
        boolean success = kitManager.giveKit(player, kitName);
        
        return success ? 1 : 0;
    }
}
