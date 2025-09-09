package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.util.MessageUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;

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
            source.sendFailure(MessageUtil.translatable("neoessentials.msg.only_players"));
            return 0;
        }

        // Argument check
            if (recipientArg == null || recipientArg.isEmpty() || messageArg == null || messageArg.isEmpty()) {
                com.zerog.neoessentials.util.MessageUtil.sendTranslatedMessage(sender, "neoessentials.msg.usage");
            return 0;
        }

        // Mute check (example integration)
        if (isMuted(sender)) {
            sender.sendSystemMessage(MessageUtil.translatable("neoessentials.msg.muted"));
            return 0;
        }

        // Console recipient
        if (recipientArg.equalsIgnoreCase("console")) {
            // Send to console only
            source.getServer().sendSystemMessage(MessageUtil.translatable("neoessentials.msg.console_from", sender.getName().getString(), messageArg));
            sender.sendSystemMessage(MessageUtil.translatable("neoessentials.msg.console_to", messageArg));
            return 1;
        }

        // Wildcard/multiple recipients support
        List<ServerPlayer> recipients = new java.util.ArrayList<>();
        if (recipientArg.equals("*")) {
            // Send to all online players except sender
            for (ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
                if (!p.getUUID().equals(sender.getUUID())) {
                    recipients.add(p);
                }
            }
        } else if (recipientArg.contains(",")) {
            // Multiple recipients separated by comma
            String[] names = recipientArg.split(",");
            for (String name : names) {
                for (ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
                    if (p.getName().getString().equalsIgnoreCase(name.trim()) && !p.getUUID().equals(sender.getUUID())) {
                        recipients.add(p);
                    }
                }
            }
        } else {
            // Single recipient
            for (ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
                if (p.getName().getString().equalsIgnoreCase(recipientArg) && !p.getUUID().equals(sender.getUUID())) {
                    recipients.add(p);
                    break;
                }
            }
        }
        if (recipients.isEmpty() && !recipientArg.equalsIgnoreCase("console")) {
            sender.sendSystemMessage(MessageUtil.translatable("neoessentials.msg.player_not_found", recipientArg));
            return 0;
        }

        // Ignore/AFK checks (stub)
        recipients.removeIf(p -> isIgnored(sender, p) || isAFK(p));
        if (recipients.isEmpty() && !recipientArg.equalsIgnoreCase("console")) {
            sender.sendSystemMessage(MessageUtil.translatable("neoessentials.msg.no_available_recipients"));
            return 0;
        }


        // Send messages
        for (ServerPlayer target : recipients) {
            sender.sendSystemMessage(MessageUtil.translatable("neoessentials.message.format", sender.getName().getString(), target.getName().getString(), messageArg));
            target.sendSystemMessage(MessageUtil.translatable("neoessentials.message.reply_format", sender.getName().getString(), target.getName().getString(), messageArg));
            lastMessaged.put(sender.getUUID(), target.getUUID());
            lastMessaged.put(target.getUUID(), sender.getUUID());
            source.getServer().sendSystemMessage(MessageUtil.translatable("neoessentials.msg.log", sender.getName().getString(), target.getName().getString(), messageArg));
        }

        // Console recipient
        if (recipientArg.equalsIgnoreCase("console")) {
            source.getServer().sendSystemMessage(MessageUtil.translatable("neoessentials.msg.console_from", sender.getName().getString(), messageArg));
            sender.sendSystemMessage(MessageUtil.translatable("neoessentials.msg.console_to", messageArg));
        }

        return 1;
    }

    // Stub mute check (replace with actual mute system)
    private static boolean isMuted(ServerPlayer player) {
    // Integrated with actual mute system
    return com.zerog.neoessentials.managers.ModerationManager.getInstance().isPlayerMuted(player.getUUID());
    }

    // Stub ignore check (replace with actual ignore system)
    private static boolean isIgnored(ServerPlayer sender, ServerPlayer recipient) {
    // Integrated with IgnoreManager
    return com.zerog.neoessentials.managers.IgnoreManager.getInstance().isIgnored(sender.getUUID(), recipient.getUUID());
    }

    // Stub AFK check (replace with actual AFK system)
    private static boolean isAFK(ServerPlayer player) {
    // Integrated with AFKManager
    return com.zerog.neoessentials.managers.AFKManager.getInstance().isAFK(player.getUUID());
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
