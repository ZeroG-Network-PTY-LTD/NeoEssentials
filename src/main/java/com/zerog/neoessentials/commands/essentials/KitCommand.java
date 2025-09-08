
package com.zerog.neoessentials.commands.essentials;
import com.zerog.neoessentials.config.MainConfig;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zerog.neoessentials.managers.KitManager;
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
            MessageUtil.sendTranslatedMessage(player, "neoessentials.kit.disabled");
            return 0;
        }
        
        List<String> availableKits = kitManager.getAvailableKits(player);
        
        if (availableKits.isEmpty()) {
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.kit.list_empty"));
            return 0;
        }

        // Send header
        MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.kit.list_header"));

        // List each available kit with details
        for (String kitName : availableKits) {
            com.zerog.neoessentials.data.Kit kit = kitManager.getKit(kitName);
            if (kit != null) {
                StringBuilder kitInfo = new StringBuilder();
                kitInfo.append("§e").append(kit.getDisplayName());
                
                if (kit.hasCost()) {
                    kitInfo.append(" §7(Cost: §c").append(kit.getCost()).append("§7)");
                }
                
                if (kit.getCooldown() > 0) {
                    long remainingCooldown = kitManager.getRemainingCooldown(player, kitName);
                    if (remainingCooldown > 0) {
                        kitInfo.append(" §7(Cooldown: §c").append(formatTime(remainingCooldown)).append("§7)");
                    } else {
                        kitInfo.append(" §7(Cooldown: §a").append(kit.getCooldown()).append("s§7)");
                    }
                }
                
                kitInfo.append(" §7- ").append(kit.getDescription());
                
                MessageUtil.sendMessage(player, kitInfo.toString());
            } else {
                MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.kit.list_entry", kitName));
            }
        }

        return 1;
    }
    
    /**
     * Format time for display
     */
    private static String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        if (seconds < 60) {
            return seconds + "s";
        }
        
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "m " + (seconds % 60) + "s";
        }
        
        long hours = minutes / 60;
        return hours + "h " + (minutes % 60) + "m";
    }    /**
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
