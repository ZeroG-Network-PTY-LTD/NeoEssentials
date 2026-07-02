package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.core.BlockPos;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.CommandSourceHelper;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Implements the /helpop command - Sends help requests to online staff
 * Notifies all staff members with proper formatting and click actions
 */
public class HelpopCommand {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    /**
     * Register the /helpop command with aliases
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("helpop")) return;
        
        // Register main command and aliases matching the registry entries
        registerHelpopCommand(dispatcher, "helpop");
        registerHelpopCommand(dispatcher, "ac");    // admin chat alias
        registerHelpopCommand(dispatcher, "amsg");  // admin message alias
    }
    
    private static void registerHelpopCommand(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(
            Commands.literal(commandName)
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        ServerPlayer player = CommandSourceHelper.requirePlayer(ctx.getSource(), "commands.neoessentials.helpop.player_only");
                        if (player == null) return 0;

                        PermissionValidator.PermissionResult permResult =
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.helpop");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        String message = StringArgumentType.getString(ctx, "message");
                        
                        return sendHelpRequest(player, message);
                    })
                )
                .executes(ctx -> {
                    ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.helpop.usage"));
                    return 0;
                })
        );
    }
    
    /**
     * Send a help request to all online staff members
     */
    private static int sendHelpRequest(ServerPlayer sender, String message) {
        // Get sender information
        String playerName = sender.getName().getString();
        BlockPos pos = sender.blockPosition();
        String worldName = CommandUtil.getWorldName(sender.level());
        String timeStamp = LocalDateTime.now().format(TIME_FORMAT);
        String location = String.format("%s (%d, %d, %d)", worldName, pos.getX(), pos.getY(), pos.getZ());
        
        // Get all online players
        List<ServerPlayer> onlinePlayers = sender.getServer().getPlayerList().getPlayers();
        
        // Count staff members who will receive the message
        int staffCount = 0;
        
        // Send to all staff members
        for (ServerPlayer staff : onlinePlayers) {
            PermissionValidator.PermissionResult staffPermResult = 
                PermissionValidator.validatePermission(staff.createCommandSourceStack(), "neoessentials.helpop.receive");
            
            if (staffPermResult.hasPermission()) {
                staffCount++;
                sendHelpopToStaff(staff, sender, playerName, message, location, timeStamp);
            }
        }
        
        // Send confirmation to sender
        if (staffCount > 0) {
            sender.sendSystemMessage(MessageUtil.success("commands.neoessentials.helpop.sent", staffCount));
            
            // Log the helpop request
            sender.getServer().sendSystemMessage(
                MessageUtil.info("commands.neoessentials.helpop.log", playerName, location, message)
            );
        } else {
            sender.sendSystemMessage(MessageUtil.warning("commands.neoessentials.helpop.no_staff"));
        }
        
        return 1;
    }
    
    /**
     * Send formatted helpop message to a staff member
     */
    private static void sendHelpopToStaff(ServerPlayer staff, ServerPlayer sender, String playerName, 
                                        String message, String location, String timeStamp) {
        
        // Create header with time and player info
        Component header = MessageUtil.warning("commands.neoessentials.helpop.staff.header", timeStamp, playerName);
        
        // Create location component with click-to-teleport (if staff has tp permission)
        Component locationComponent;
        PermissionValidator.PermissionResult tpPermResult = 
            PermissionValidator.validatePermission(staff.createCommandSourceStack(), "neoessentials.teleport.admin.tp");
        
        if (tpPermResult.hasPermission()) {
            BlockPos pos = sender.blockPosition();
            String tpCommand = String.format("/tp %d %d %d", pos.getX(), pos.getY(), pos.getZ());
            
            locationComponent = Component.literal("§e" + location)
                .withStyle(style -> style
                    .withClickEvent(com.zerog.neoessentials.util.ClickEventCompat.create(ClickEvent.Action.SUGGEST_COMMAND, tpCommand))
                    .withHoverEvent(com.zerog.neoessentials.util.HoverEventCompat.create(HoverEvent.Action.SHOW_TEXT, 
                        Component.literal("§7Click to teleport to " + playerName)))
                );
        } else {
            locationComponent = Component.literal("§e" + location);
        }
        
        // Create message component
        Component messageComponent = Component.literal("§f" + message);
        
        // Create reply component with click-to-reply
        String replyCommand = "/msg " + playerName + " ";
        Component replyComponent = Component.literal("§a[Reply]")
            .withStyle(style -> style
                .withClickEvent(com.zerog.neoessentials.util.ClickEventCompat.create(ClickEvent.Action.SUGGEST_COMMAND, replyCommand))
                .withHoverEvent(com.zerog.neoessentials.util.HoverEventCompat.create(HoverEvent.Action.SHOW_TEXT, 
                    Component.literal("§7Click to reply to " + playerName)))
            );
        
        // Send all components to staff member
        staff.sendSystemMessage(MessageUtil.component("commands.neoessentials.helpop.header"));
        staff.sendSystemMessage(header);
        staff.sendSystemMessage(Component.literal(MessageUtil.localize("commands.neoessentials.helpop.location")).append(locationComponent));
        staff.sendSystemMessage(Component.literal(MessageUtil.localize("commands.neoessentials.helpop.message")).append(messageComponent));
        staff.sendSystemMessage(Component.literal(MessageUtil.localize("commands.neoessentials.helpop.actions")).append(replyComponent));
        staff.sendSystemMessage(MessageUtil.component("commands.neoessentials.helpop.footer"));
    }
}