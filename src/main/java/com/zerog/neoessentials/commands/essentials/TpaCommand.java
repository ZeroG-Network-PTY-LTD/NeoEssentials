package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.managers.TeleportRequestManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

/**
 * TPA command implementation for NeoEssentials
 * Handles teleport request functionality including /tpa, /tpaccept, /tpdeny, /tpahere
 * 
 * Commands:
 * - /tpa <player> - Request to teleport to another player
 * - /tpaccept [player] - Accept a teleport request
 * - /tpdeny [player] - Deny a teleport request  
 * - /tpahere <player> - Request a player to teleport to you
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class TpaCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /tpa <player> - Request to teleport to another player
        dispatcher.register(Commands.literal("tpa")
            .requires(source -> source.getEntity() instanceof ServerPlayer)
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> sendTpaRequest(ctx, TeleportRequestManager.RequestType.TPA))
            )
        );
        
        // /tpahere <player> - Request a player to teleport to you
        dispatcher.register(Commands.literal("tpahere")
            .requires(source -> source.getEntity() instanceof ServerPlayer)
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> sendTpaRequest(ctx, TeleportRequestManager.RequestType.TPAHERE))
            )
        );
        
        // /tpaccept [player] - Accept a teleport request
        dispatcher.register(Commands.literal("tpaccept")
            .requires(source -> source.getEntity() instanceof ServerPlayer)
            .executes(ctx -> acceptTpaRequest(ctx, null))
            .then(Commands.argument("player", StringArgumentType.word())
                .executes(ctx -> acceptTpaRequest(ctx, StringArgumentType.getString(ctx, "player")))
            )
        );
        
        // /tpdeny [player] - Deny a teleport request
        dispatcher.register(Commands.literal("tpdeny")
            .requires(source -> source.getEntity() instanceof ServerPlayer)
            .executes(ctx -> denyTpaRequest(ctx, null))
            .then(Commands.argument("player", StringArgumentType.word())
                .executes(ctx -> denyTpaRequest(ctx, StringArgumentType.getString(ctx, "player")))
            )
        );
    }

    /**
     * Send a TPA or TPAHERE request
     */
    private static int sendTpaRequest(CommandContext<CommandSourceStack> context, TeleportRequestManager.RequestType type) throws CommandSyntaxException {
        ServerPlayer requester = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        
        TeleportRequestManager requestManager = TeleportRequestManager.getInstance();
        boolean success = requestManager.sendRequest(requester, target, type);
        
        return success ? 1 : 0;
    }

    /**
     * Accept a teleport request
     */
    private static int acceptTpaRequest(CommandContext<CommandSourceStack> context, String requesterName) throws CommandSyntaxException {
        ServerPlayer target = context.getSource().getPlayerOrException();
        
        TeleportRequestManager requestManager = TeleportRequestManager.getInstance();
        boolean success = requestManager.acceptRequest(target, requesterName);
        
        return success ? 1 : 0;
    }

    /**
     * Deny a teleport request
     */
    private static int denyTpaRequest(CommandContext<CommandSourceStack> context, String requesterName) throws CommandSyntaxException {
        ServerPlayer target = context.getSource().getPlayerOrException();
        
        TeleportRequestManager requestManager = TeleportRequestManager.getInstance();
        boolean success = requestManager.denyRequest(target, requesterName);
        
        return success ? 1 : 0;
    }
}
