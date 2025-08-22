package com.zerog.neoessentials.commands.permissions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.animation.AnimationManager;
import com.zerog.neoessentials.permissions.CustomPermissionsManager;
import com.zerog.neoessentials.permissions.PermissionNodes;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Enhanced Prefix Command with Animation Support
 * Allows setting animated prefixes for permission groups
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class AnimatedPrefixCommand {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AnimatedPrefixCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("aprefix")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.PERMISSIONS_GROUP))
            .then(Commands.literal("set")
                .then(Commands.argument("group", StringArgumentType.word())
                    .then(Commands.argument("prefix", StringArgumentType.greedyString())
                        .executes(AnimatedPrefixCommand::setGroupPrefix)
                    )
                )
            )
            .then(Commands.literal("preview")
                .then(Commands.argument("prefix", StringArgumentType.greedyString())
                    .executes(AnimatedPrefixCommand::previewPrefix)
                )
            )
            .then(Commands.literal("test")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(AnimatedPrefixCommand::testPlayerPrefix)
                )
            )
            .then(Commands.literal("animations")
                .executes(AnimatedPrefixCommand::listAnimations)
            )
            .then(Commands.literal("help")
                .executes(AnimatedPrefixCommand::showHelp)
            )
        );
    }
    
    /**
     * Set an animated prefix for a group
     */
    private static int setGroupPrefix(CommandContext<CommandSourceStack> context) {
        try {
            String groupName = StringArgumentType.getString(context, "group");
            String prefix = StringArgumentType.getString(context, "prefix");
            
            CustomPermissionsManager permManager = CustomPermissionsManager.getInstance();
            
            // Validate group exists
            if (permManager.getGroup(groupName) == null) {
                context.getSource().sendFailure(Component.literal("§cGroup '" + groupName + "' does not exist!"));
                return 0;
            }
            
            // Set the prefix (with animation support)
            permManager.setGroupPrefix(groupName, prefix);
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§a✅ Set animated prefix for group §e" + groupName + "§a:\n" +
                "§7Preview: " + prefix + "§r\n" +
                "§7Note: Animations will be processed when displayed to players."
            ), true);
            
            LOGGER.info("Set animated prefix '{}' for group '{}' by {}", 
                prefix, groupName, context.getSource().getTextName());
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError setting prefix: " + e.getMessage()));
            LOGGER.error("Error setting animated prefix", e);
            return 0;
        }
    }
    
    /**
     * Preview how a prefix would look with animations
     */
    private static int previewPrefix(CommandContext<CommandSourceStack> context) {
        try {
            String prefix = StringArgumentType.getString(context, "prefix");
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            // Try to process the prefix with animations
            String processedPrefix;
            try {
                // Get animation manager config directory
                File configDir = new File("config/neoessentials");
                AnimationManager animManager = AnimationManager.getInstance(configDir);
                
                if (animManager.isEnabled()) {
                    processedPrefix = animManager.processAnimatedText(prefix, player);
                } else {
                    processedPrefix = prefix;
                }
            } catch (Exception e) {
                LOGGER.debug("Animation manager not available for preview: {}", e.getMessage());
                processedPrefix = prefix;
            }
            
            final String finalProcessedPrefix = processedPrefix;
            context.getSource().sendSuccess(() -> Component.literal(
                "§b🎬 Prefix Preview:\n" +
                "§7Raw: §f" + prefix + "\n" +
                "§7Processed: " + finalProcessedPrefix + "§r\n" +
                "§7Note: Animations update dynamically in-game."
            ), false);
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError previewing prefix: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Test a player's current prefix with animations
     */
    private static int testPlayerPrefix(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            
            CustomPermissionsManager permManager = CustomPermissionsManager.getInstance();
            String animatedPrefix = permManager.getPlayerPrefix(targetPlayer.getUUID());
            String groupName = permManager.getPlayerGroup(targetPlayer.getUUID());
            
            context.getSource().sendSuccess(() -> Component.literal(
                "§b🎭 Player Prefix Test:\n" +
                "§7Player: §e" + targetPlayer.getName().getString() + "\n" +
                "§7Group: §e" + groupName + "\n" +
                "§7Animated Prefix: " + animatedPrefix + "§r\n" +
                "§7Full Display: " + animatedPrefix + targetPlayer.getDisplayName().getString()
            ), false);
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError testing prefix: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * List available animations for prefixes
     */
    private static int listAnimations(CommandContext<CommandSourceStack> context) {
        try {
            context.getSource().sendSuccess(() -> Component.literal(
                "§b🎨 Available Animations for Prefixes:\n" +
                "§7Basic placeholders:\n" +
                "§e{animated_server} §7- Animated server name\n" +
                "§e{rainbow_server} §7- Rainbow text effect\n" +
                "§e{loading} §7- Loading bar animation\n" +
                "§e{players_online} §7- Online player count\n" +
                "§e{server_time} §7- Server time display\n" +
                "§e{server_tps} §7- Server TPS indicator\n\n" +
                "§7Example animated prefixes:\n" +
                "§e/aprefix set admin \"§c[{rainbow_server}]§r \"\n" +
                "§e/aprefix set vip \"§b[{animated_server}]§r \"\n" +
                "§e/aprefix set mod \"§6[{loading}]§r \"\n\n" +
                "§7Create custom animations in §econfig/neoessentials/animations.json"
            ), false);
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError listing animations: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Show help for animated prefix commands
     */
    private static int showHelp(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal(
            "§b🎬 Animated Prefix Commands Help:\n\n" +
            "§e/aprefix set <group> <prefix> §7- Set animated prefix for group\n" +
            "§e/aprefix preview <prefix> §7- Preview how prefix will look\n" +
            "§e/aprefix test <player> §7- Test player's current prefix\n" +
            "§e/aprefix animations §7- List available animations\n" +
            "§e/aprefix help §7- Show this help\n\n" +
            "§b💡 Tips:\n" +
            "§7• Use {placeholders} for dynamic content\n" +
            "§7• Animations update automatically in-game\n" +
            "§7• Custom animations can be created in animations.json\n" +
            "§7• Use color codes (§c&c§7, §a&a§7) for styling\n" +
            "§7• Test prefixes before applying to groups"
        ), false);
        
        return 1;
    }
}
