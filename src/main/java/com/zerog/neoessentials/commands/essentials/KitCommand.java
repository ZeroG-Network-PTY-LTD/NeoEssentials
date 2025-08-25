
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

        // List each available kit (details should be fetched from a KitRegistry or KitManager, not config)
        for (String kitName : availableKits) {
            // Example: You would fetch kit details from a KitRegistry or KitManager here
            // For now, just show the kit name
            MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, "neoessentials.kit.list_entry", kitName));
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
