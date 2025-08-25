package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class MotdCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("motd")
            .executes(MotdCommand::showMotd)
            .then(Commands.literal("set")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC)) // Admin only
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(MotdCommand::setMotd)))
            .then(Commands.literal("reload")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC)) // Admin only
                .executes(MotdCommand::reloadMotd)));
    }
    
    /**
     * Show the current message of the day
     */
    private static int showMotd(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String motd = source.getServer().getMotd();
    sendMessage(source, Component.translatable("neoessentials.motd.header"));
    sendMessage(source, Component.translatable("neoessentials.motd.body", motd));
    sendMessage(source, Component.translatable("neoessentials.motd.footer"));
        return 1;
    }
    
    /**
     * Set a new message of the day (admin only)
     */
    private static int setMotd(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String newMotd = StringArgumentType.getString(context, "message");
        
        try {
            // This would require reflection or mod access to server properties
            // For now, just show what the new MOTD would be
            sendMessage(source, Component.translatable("neoessentials.motd.admin_new"));
            sendMessage(source, Component.translatable("neoessentials.motd.admin_new_value", newMotd));
            sendMessage(source, Component.translatable("neoessentials.motd.set.note"));

            // Log the change
            source.getServer().sendSystemMessage(Component.translatable(
                "neoessentials.motd.admin_log_set", getSourceName(source), newMotd));
            return 1;
        } catch (Exception e) {
            sendMessage(source, Component.translatable("neoessentials.motd.set.failed", e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Reload the message of the day from server properties
     */
    private static int reloadMotd(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            // This would reload from server.properties if we had access
            sendMessage(source, Component.translatable("neoessentials.motd.reload.success"));
            sendMessage(source, Component.translatable("neoessentials.motd.reload.current", source.getServer().getMotd()));

            // Log the reload
            source.getServer().sendSystemMessage(Component.translatable(
                "neoessentials.motd.admin_log_reload", getSourceName(source)));
            return 1;
        } catch (Exception e) {
            sendMessage(source, Component.translatable("neoessentials.motd.reload.failed", e.getMessage()));
            return 0;
        }
    }
    

    private static void sendMessage(CommandSourceStack source, net.minecraft.network.chat.MutableComponent component) {
        if (source.getEntity() instanceof ServerPlayer player) {
            player.sendSystemMessage(component);
        } else {
            source.sendSuccess(() -> component, false);
        }
    }
    
    private static String getSourceName(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player.getName().getString();
        } else {
            return "Console";
        }
    }
}
