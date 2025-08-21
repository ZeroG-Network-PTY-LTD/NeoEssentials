package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.util.MessageUtil;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MessageCommand {
    
    // Store last messaged player for /reply command
    private static final Map<UUID, UUID> lastMessaged = new HashMap<>();
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    // Register custom commands to override vanilla whisper commands
        // /msg command with EssentialsX-like argument parsing and tab completion
        dispatcher.register(Commands.literal("msg")
            .then(Commands.argument("recipient", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    // Suggest online player names for tab completion
                    for (ServerPlayer player : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                        builder.suggest(player.getName().getString());
                    }
                    builder.suggest("console");
                    return builder.buildFuture();
                })
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(MessageCommand::sendMessageEssentialsStyle))));

        // /tell command (alias)
        dispatcher.register(Commands.literal("tell")
            .then(Commands.argument("recipient", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    for (ServerPlayer player : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                        builder.suggest(player.getName().getString());
                    }
                    builder.suggest("console");
                    return builder.buildFuture();
                })
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(MessageCommand::sendMessageEssentialsStyle))));
        // Removed /w and /whisper aliases for strict privacy


        dispatcher.register(Commands.literal("w")
            .executes(ctx -> 0)); // Cancel vanilla

        dispatcher.register(Commands.literal("whisper")
            .executes(ctx -> 0)); // Cancel vanilla
    }
    
    /**
     * Send a private message between players
     */
    // EssentialsX-style /msg command handler
    private static int sendMessageEssentialsStyle(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String recipientArg = StringArgumentType.getString(context, "recipient");
        String messageArg = StringArgumentType.getString(context, "message");

        // Only players can send messages (for now)
        if (!(source.getEntity() instanceof ServerPlayer sender)) {
            source.sendFailure(Component.literal("Only players can send private messages."));
            return 0;
        }

        // Argument check
            if (recipientArg == null || recipientArg.isEmpty() || messageArg == null || messageArg.isEmpty()) {
                com.zerog.neoessentials.util.MessageUtil.sendTranslatedMessage(sender, "neoessentials.msg.usage");
            return 0;
        }

        // Mute check (stub)
        // TODO: Integrate with your mute system
        // if (isMuted(sender)) { sender.sendSystemMessage(Component.literal("You are muted.")); return 0; }

        // Console recipient
        if (recipientArg.equalsIgnoreCase("console")) {
            // Send to console only
            source.getServer().sendSystemMessage(Component.literal("[PM from " + sender.getName().getString() + "] " + messageArg));
            sender.sendSystemMessage(Component.literal("[PM to Console] " + messageArg));
            return 1;
        }

        // Find target player
        ServerPlayer target = null;
        for (ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
            if (p.getName().getString().equalsIgnoreCase(recipientArg)) {
                target = p;
                break;
            }
        }
        if (target == null) {
            sender.sendSystemMessage(Component.literal("Player not found: " + recipientArg));
            return 0;
        }

        // Self check
        if (target.getUUID().equals(sender.getUUID())) {
            sender.sendSystemMessage(Component.literal("You cannot message yourself."));
            return 0;
        }

        // TODO: Wildcard/multiple recipients support

        // TODO: Ignore/AFK checks


    // Use configurable format strings from MainConfig
    var mainConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getMainConfig();
    var chatSettings = mainConfig.chatSettings;
    String senderFormat = chatSettings.pmFormatSender != null ? chatSettings.pmFormatSender : "[PM to {0}] {1}";
    String receiverFormat = chatSettings.pmFormatReceiver != null ? chatSettings.pmFormatReceiver : "[PM from {0}] {1}";
    MessageUtil.sendMessage(sender, senderFormat, target.getName().getString(), messageArg);
    MessageUtil.sendMessage(target, receiverFormat, sender.getName().getString(), messageArg);

        // Reply tracking
        lastMessaged.put(sender.getUUID(), target.getUUID());
        lastMessaged.put(target.getUUID(), sender.getUUID());

        // Log to console for admin monitoring
        source.getServer().sendSystemMessage(Component.literal(
            "§7[Private] " + sender.getName().getString() + " → " + target.getName().getString() + ": " + messageArg));

        return 1;
    }
    
    /**
     * Get the last player that this player messaged (for reply functionality)
     */
    public static UUID getLastMessaged(UUID playerUUID) {
        return lastMessaged.get(playerUUID);
    }
    
    /**
     * Clear last messaged data for a player (called when they leave)
     */
    public static void clearLastMessaged(UUID playerUUID) {
        lastMessaged.remove(playerUUID);
        // Also clear any references to this player in others' last messaged
        lastMessaged.entrySet().removeIf(entry -> entry.getValue().equals(playerUUID));
    }
}
