package com.zerog.neoessentials.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.zerog.neoessentials.economy.managers.PayToggleManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PayToggleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(PayToggleCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            net.minecraft.commands.Commands.literal("paytoggle")
                .requires(src -> com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(src, 2) || // Allow ops
                    (src.getPlayer() != null && com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.economy.paytoggle")))
                .executes(ctx -> execute(ctx))
        );
        
        // Register "pt" alias for paytoggle
        dispatcher.register(
            net.minecraft.commands.Commands.literal("pt")
                .requires(src -> com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(src, 2) || // Allow ops
                    (src.getPlayer() != null && com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.economy.paytoggle")))
                .executes(ctx -> execute(ctx))
        );
    }

    private static int execute(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            java.util.UUID uuid = player.getUUID();
            
            LOGGER.debug("PayToggle command executed by player: {}", player.getName().getString());
            
            boolean current = PayToggleManager.getInstance().getPayToggle(uuid);
            boolean newState = !current;
            PayToggleManager.getInstance().setPayToggle(uuid, newState);
            
            LOGGER.debug("PayToggle state changed from {} to {} for player {}", current, newState, player.getName().getString());
            
            if (newState) {
                ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.paytoggle.enabled"), false);
            } else {
                ctx.getSource().sendSuccess(() -> MessageUtil.info("commands.neoessentials.paytoggle.disabled"), false);
            }
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error executing paytoggle command", e);
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.error"));
            return 0;
        }
    }
}
