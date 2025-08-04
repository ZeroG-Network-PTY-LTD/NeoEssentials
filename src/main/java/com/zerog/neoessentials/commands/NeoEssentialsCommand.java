package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NeoEssentials Summary Command - Shows all available features
 * 
 * Commands:
 * - /neoessentials - Shows main information and feature list
 * - /ne - Alias for /neoessentials
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class NeoEssentialsCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(NeoEssentialsCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Main command
        dispatcher.register(Commands.literal("neoessentials")
            .executes(NeoEssentialsCommand::executeMainCommand)
        );
        
        // Alias
        dispatcher.register(Commands.literal("ne")
            .executes(NeoEssentialsCommand::executeMainCommand)
        );
        
        // Version command
        dispatcher.register(Commands.literal("neoessentials")
            .then(Commands.literal("version")
                .executes(NeoEssentialsCommand::executeVersionCommand))
        );
        
        // Features command
        dispatcher.register(Commands.literal("neoessentials")
            .then(Commands.literal("features")
                .executes(NeoEssentialsCommand::executeFeaturesCommand))
        );
        
        // Commands list
        dispatcher.register(Commands.literal("neoessentials")
            .then(Commands.literal("commands")
                .executes(NeoEssentialsCommand::executeCommandsCommand))
        );
    }
    
    private static int executeMainCommand(CommandContext<CommandSourceStack> context) {
        try {
            var source = context.getSource();
            
            source.sendSuccess(() -> Component.literal("§6§l◆━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━◆"), false);
            source.sendSuccess(() -> Component.literal("§6§l                           NeoEssentials v2.0.0"), false);
            source.sendSuccess(() -> Component.literal("§7           Essential server administration tools for NeoForge"), false);
            source.sendSuccess(() -> Component.literal("§6§l◆━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━◆"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal("§a§lAvailable Commands:"), false);
            source.sendSuccess(() -> Component.literal("§b/ne features §7- View all available features"), false);
            source.sendSuccess(() -> Component.literal("§b/ne commands §7- View all available commands"), false);
            source.sendSuccess(() -> Component.literal("§b/ne version §7- View version information"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal("§e§lQuick Access:"), false);
            source.sendSuccess(() -> Component.literal("§6/shop §7- Open server shop"), false);
            source.sendSuccess(() -> Component.literal("§6/menu §7- Open main menu"), false);
            source.sendSuccess(() -> Component.literal("§6/stats §7- View your statistics"), false);
            source.sendSuccess(() -> Component.literal("§6/warps §7- View server warps"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal("§7Author: §fZeroG §7| §7Version: §f2.0.0"), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing NeoEssentials main command", e);
            return 0;
        }
    }
    
    private static int executeVersionCommand(CommandContext<CommandSourceStack> context) {
        try {
            var source = context.getSource();
            
            source.sendSuccess(() -> Component.literal("§6§l◆━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━◆"), false);
            source.sendSuccess(() -> Component.literal("§6§l                        NeoEssentials Version Info"), false);
            source.sendSuccess(() -> Component.literal("§6§l◆━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━◆"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal("§a§lMod Information:"), false);
            source.sendSuccess(() -> Component.literal("§7Name: §fNeoEssentials"), false);
            source.sendSuccess(() -> Component.literal("§7Version: §f2.0.0"), false);
            source.sendSuccess(() -> Component.literal("§7Author: §fZeroG"), false);
            source.sendSuccess(() -> Component.literal("§7Target: §fNeoForge Server-Side"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal("§b§lFeature Status:"), false);
            source.sendSuccess(() -> Component.literal("§7Essential Commands: §a✓ Implemented"), false);
            source.sendSuccess(() -> Component.literal("§7Discord Integration: §a✓ Implemented"), false);
            source.sendSuccess(() -> Component.literal("§7Custom GUIs: §a✓ Implemented"), false);
            source.sendSuccess(() -> Component.literal("§7Tablist & Scoreboards: §a✓ Implemented"), false);
            source.sendSuccess(() -> Component.literal("§7Economy System: §e⚠ Partial"), false);
            source.sendSuccess(() -> Component.literal("§7Home System: §a✓ Implemented"), false);
            source.sendSuccess(() -> Component.literal("§7Warp System: §e⚠ Partial"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal("§d§lCompatibility: §fServer-side only, no client required"), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing version command", e);
            return 0;
        }
    }
    
    private static int executeFeaturesCommand(CommandContext<CommandSourceStack> context) {
        try {
            var source = context.getSource();
            
            source.sendSuccess(() -> Component.literal("§6§l◆━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━◆"), false);
            source.sendSuccess(() -> Component.literal("§6§l                          NeoEssentials Features"), false);
            source.sendSuccess(() -> Component.literal("§6§l◆━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━◆"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            
            source.sendSuccess(() -> Component.literal("§a§l1. Essential Utility Commands"), false);
            source.sendSuccess(() -> Component.literal("§7   • Player management: heal, feed, god, vanish"), false);
            source.sendSuccess(() -> Component.literal("§7   • Movement: fly, speed"), false);
            source.sendSuccess(() -> Component.literal("§7   • Items: give, workbench, anvil, enderchest, invsee"), false);
            source.sendSuccess(() -> Component.literal("§7   • World: time, weather"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            
            source.sendSuccess(() -> Component.literal("§b§l2. Discord Integration"), false);
            source.sendSuccess(() -> Component.literal("§7   • Webhook notifications"), false);
            source.sendSuccess(() -> Component.literal("§7   • Account linking"), false);
            source.sendSuccess(() -> Component.literal("§7   • Server event broadcasts"), false);
            source.sendSuccess(() -> Component.literal("§7   • Rich embed messages"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            
            source.sendSuccess(() -> Component.literal("§c§l3. Custom GUI System"), false);
            source.sendSuccess(() -> Component.literal("§7   • Interactive shop interface"), false);
            source.sendSuccess(() -> Component.literal("§7   • Player statistics viewer"), false);
            source.sendSuccess(() -> Component.literal("§7   • Server information display"), false);
            source.sendSuccess(() -> Component.literal("§7   • Kit and warp selectors"), false);
            source.sendSuccess(() -> Component.literal("§7   • Economy management"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            
            source.sendSuccess(() -> Component.literal("§d§l4. Enhanced Tablist & Scoreboards"), false);
            source.sendSuccess(() -> Component.literal("§7   • Custom player list headers/footers"), false);
            source.sendSuccess(() -> Component.literal("§7   • Real-time server information"), false);
            source.sendSuccess(() -> Component.literal("§7   • Player statistics display"), false);
            source.sendSuccess(() -> Component.literal("§7   • Session time tracking"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            
            source.sendSuccess(() -> Component.literal("§e§l5. Additional Systems"), false);
            source.sendSuccess(() -> Component.literal("§7   • Home management system"), false);
            source.sendSuccess(() -> Component.literal("§7   • Multi-language support"), false);
            source.sendSuccess(() -> Component.literal("§7   • Configuration management"), false);
            source.sendSuccess(() -> Component.literal("§7   • Data persistence"), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing features command", e);
            return 0;
        }
    }
    
    private static int executeCommandsCommand(CommandContext<CommandSourceStack> context) {
        try {
            var source = context.getSource();
            
            source.sendSuccess(() -> Component.literal("§6§l◆━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━◆"), false);
            source.sendSuccess(() -> Component.literal("§6§l                         NeoEssentials Commands"), false);
            source.sendSuccess(() -> Component.literal("§6§l◆━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━◆"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            
            source.sendSuccess(() -> Component.literal("§a§lUtility Commands:"), false);
            source.sendSuccess(() -> Component.literal("§7/heal [player] §f- Restore health"), false);
            source.sendSuccess(() -> Component.literal("§7/feed [player] §f- Restore hunger"), false);
            source.sendSuccess(() -> Component.literal("§7/god [player] §f- Toggle invincibility"), false);
            source.sendSuccess(() -> Component.literal("§7/vanish [player] §f- Toggle invisibility"), false);
            source.sendSuccess(() -> Component.literal("§7/fly [player] §f- Toggle flight"), false);
            source.sendSuccess(() -> Component.literal("§7/speed <type> <value> [player] §f- Set movement speed"), false);
            source.sendSuccess(() -> Component.literal("§7/give <player> <item> [amount] §f- Give items"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            
            source.sendSuccess(() -> Component.literal("§b§lWorld Commands:"), false);
            source.sendSuccess(() -> Component.literal("§7/time <set|add> <value> §f- Manage time"), false);
            source.sendSuccess(() -> Component.literal("§7/weather <clear|rain|thunder> §f- Control weather"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            
            source.sendSuccess(() -> Component.literal("§c§lInterface Commands:"), false);
            source.sendSuccess(() -> Component.literal("§7/workbench §f- Open crafting table"), false);
            source.sendSuccess(() -> Component.literal("§7/anvil §f- Open anvil interface"), false);
            source.sendSuccess(() -> Component.literal("§7/enderchest [player] §f- Open ender chest"), false);
            source.sendSuccess(() -> Component.literal("§7/invsee <player> §f- View player inventory"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            
            source.sendSuccess(() -> Component.literal("§d§lGUI Commands:"), false);
            source.sendSuccess(() -> Component.literal("§7/shop [category] §f- Open server shop"), false);
            source.sendSuccess(() -> Component.literal("§7/menu §f- Open main menu"), false);
            source.sendSuccess(() -> Component.literal("§7/stats [player] §f- View statistics"), false);
            source.sendSuccess(() -> Component.literal("§7/kits §f- Open kit selector"), false);
            source.sendSuccess(() -> Component.literal("§7/warps §f- Open warp selector"), false);
            source.sendSuccess(() -> Component.literal("§7/economy §f- Economy management"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            
            source.sendSuccess(() -> Component.literal("§e§lDiscord Commands:"), false);
            source.sendSuccess(() -> Component.literal("§7/discord §f- Discord integration menu"), false);
            source.sendSuccess(() -> Component.literal("§7/discord link <username> §f- Link Discord account"), false);
            source.sendSuccess(() -> Component.literal("§7/discord broadcast <message> §f- Send to Discord"), false);
            
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing commands command", e);
            return 0;
        }
    }
}
