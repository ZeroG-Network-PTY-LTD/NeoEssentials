package com.zerog.neoessentials.listeners;

import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import com.zerog.neoessentials.commands.essentials.MessageCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandOverrideListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandOverrideListener.class);

    @net.neoforged.bus.api.SubscribeEvent
    public static void onCommand(CommandEvent event) {
        String input = event.getParseResults().getReader().getString();
        String[] parts = input.trim().split(" ");
        String cmd = parts[0].replace("/", "").toLowerCase();
        if (cmd.equals("msg") || cmd.equals("tell") || cmd.equals("w") || cmd.equals("whisper")) {
            event.setCanceled(true);
            CommandSourceStack source = event.getParseResults().getContext().getSource();
            if (source.getEntity() instanceof ServerPlayer sender && parts.length >= 3) {
                String targetName = parts[1];
                String message = input.substring(input.indexOf(targetName) + targetName.length()).trim();
                // Find target player by name
                ServerPlayer target = sender.getServer().getPlayerList().getPlayerByName(targetName);
                if (target == null) {
                    source.sendSystemMessage(Component.literal("Player not found: " + targetName));
                    return;
                }
                // Use your custom message logic
                com.zerog.neoessentials.messaging.IMessageRecipient senderRecipient = new com.zerog.neoessentials.messaging.SimpleMessageRecipient(sender);
                com.zerog.neoessentials.messaging.IMessageRecipient targetRecipient = new com.zerog.neoessentials.messaging.SimpleMessageRecipient(target);
                String senderName = senderRecipient.getName();
                String targetDisplay = targetRecipient.getName();
                senderRecipient.sendMessage(Component.literal("[PM to " + targetDisplay + "] " + message));
                targetRecipient.sendMessage(Component.literal("[PM from " + senderName + "] " + message));
                senderRecipient.setReplyRecipient(targetRecipient);
                targetRecipient.setReplyRecipient(senderRecipient);
            } else {
                source.sendSystemMessage(Component.literal("Usage: /msg <player> <message>"));
            }
            LOGGER.info("Vanilla whisper command '{}' was intercepted and replaced with NeoEssentials PM.", cmd);
        }
    }
}
