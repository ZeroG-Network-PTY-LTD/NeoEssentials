package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.MailManager;
import com.zerog.neoessentials.data.UserManager;
import com.zerog.neoessentials.utils.TextUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/**
 * Implements mail-related commands.
 */
public class MailCommands {
    
    /**
     * Registers all mail-related commands with the dispatcher.
     *
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerMailCommand(dispatcher);
    }
    
    /**
     * Registers the mail command.
     *
     * @param dispatcher The command dispatcher
     */
    private void registerMailCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /mail                - Shows mail summary
        // /mail read          - Reads all mail
        // /mail read <index>  - Reads specific mail
        // /mail clear         - Clears all mail
        // /mail delete <index> - Deletes specific mail
        // /mail send <player> <message> - Sends mail to player
        LiteralArgumentBuilder<CommandSourceStack> mailCommand = Commands.literal("mail")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.mail"))
                .executes(this::executeMailSummary)
                .then(Commands.literal("read")
                        .executes(this::executeMailRead)
                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                .executes(this::executeMailReadIndex)))
                .then(Commands.literal("clear")
                        .executes(this::executeMailClear))
                .then(Commands.literal("delete")
                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                .executes(this::executeMailDelete)))
                .then(Commands.literal("send")
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.mail.send"))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                        .executes(this::executeMailSend))));

        // Register aliases
        dispatcher.register(mailCommand);
    }
    
    /**
     * Executes the mail summary command.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private int executeMailSummary(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        
        MailManager mailManager = NeoEssentials.getInstance().getDataManager().getMailManager();
        List<MailManager.MailMessage> messages = mailManager.getMail(player);
        
        int unreadCount = 0;
        for (MailManager.MailMessage message : messages) {
            if (!message.isRead()) {
                unreadCount++;
            }
        }
        
        if (messages.isEmpty()) {
            source.sendSuccess(() -> Component.literal(TextUtil.formatText("&aYou have no mail.")), false);
            return 1;
        }
        
<<<<<<< HEAD
        final int finalUnreadCount = unreadCount;
        final int messageCount = messages.size();
        
        source.sendSuccess(() -> Component.literal(TextUtil.formatText(
                "&aYou have &6" + messageCount + " &amail message" + (messageCount == 1 ? "" : "s") + 
                " (&6" + finalUnreadCount + " &aunread).")), false);
=======
        source.sendSuccess(() -> Component.literal(TextUtil.formatText(
                "&aYou have &6" + messages.size() + " &amail message" + (messages.size() == 1 ? "" : "s") + 
                " (&6" + unreadCount + " &aunread).")), false);
>>>>>>> 907bd69 (feat: Add MailManager and MailCommands for player mail functionality)
        source.sendSuccess(() -> Component.literal(TextUtil.formatText(
                "&aUse &6/mail read &ato read all messages, or &6/mail read <number> &ato read a specific message.")), false);
        
        return 1;
    }
    
    /**
     * Executes the mail read command.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private int executeMailRead(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        
        MailManager mailManager = NeoEssentials.getInstance().getDataManager().getMailManager();
        List<MailManager.MailMessage> messages = mailManager.getMail(player);
        
        if (messages.isEmpty()) {
            source.sendSuccess(() -> Component.literal(TextUtil.formatText("&aYou have no mail.")), false);
            return 1;
        }
        
<<<<<<< HEAD
        final int messageCount = messages.size();
        source.sendSuccess(() -> Component.literal(TextUtil.formatText("&a=== &6Your Mail &a=== &7(" + messageCount + " messages)")), false);
        
        // We need to use a slightly different approach for the index
        // since it changes in the loop but needs to be effectively final for the lambda
        for (int i = 0; i < messages.size(); i++) {
            final int index = i + 1; // Make a final copy for the lambda
            final MailManager.MailMessage message = messages.get(i);
            
=======
        source.sendSuccess(() -> Component.literal(TextUtil.formatText("&a=== &6Your Mail &a=== &7(" + messages.size() + " messages)")), false);
        
        int index = 1;
        for (MailManager.MailMessage message : messages) {
>>>>>>> 907bd69 (feat: Add MailManager and MailCommands for player mail functionality)
            source.sendSuccess(() -> Component.literal(TextUtil.formatText(
                    "&a" + index + ". &6From: &e" + message.getSender() + " &6Date: &e" + message.getFormattedDate())), false);
            source.sendSuccess(() -> Component.literal(TextUtil.formatText("&f" + message.getMessage())), false);
            source.sendSuccess(() -> Component.literal(TextUtil.formatText("&7---")), false);
            
            // Mark as read
            if (!message.isRead()) {
                message.markAsRead();
            }
<<<<<<< HEAD
=======
            
            index++;
>>>>>>> 907bd69 (feat: Add MailManager and MailCommands for player mail functionality)
        }
        
        mailManager.saveMail();
        
        return 1;
    }
    
    /**
     * Executes the mail read index command.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private int executeMailReadIndex(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        int index = IntegerArgumentType.getInteger(context, "index");
        
        MailManager mailManager = NeoEssentials.getInstance().getDataManager().getMailManager();
        List<MailManager.MailMessage> messages = mailManager.getMail(player);
        
        if (messages.isEmpty()) {
            source.sendSuccess(() -> Component.literal(TextUtil.formatText("&aYou have no mail.")), false);
            return 1;
        }
        
        if (index > messages.size()) {
            source.sendFailure(Component.literal(TextUtil.formatText(
                    "&cYou only have " + messages.size() + " mail message" + (messages.size() == 1 ? "" : "s") + ".")));
            return 0;
        }
        
        MailManager.MailMessage message = messages.get(index - 1);
        
        source.sendSuccess(() -> Component.literal(TextUtil.formatText("&a=== &6Message #" + index + " &a===")), false);
        source.sendSuccess(() -> Component.literal(TextUtil.formatText("&6From: &e" + message.getSender())), false);
        source.sendSuccess(() -> Component.literal(TextUtil.formatText("&6Date: &e" + message.getFormattedDate())), false);
        source.sendSuccess(() -> Component.literal(TextUtil.formatText("&f" + message.getMessage())), false);
        
        // Mark as read
        if (!message.isRead()) {
            message.markAsRead();
            mailManager.saveMail();
        }
        
        return 1;
    }
    
    /**
     * Executes the mail clear command.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private int executeMailClear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        
        MailManager mailManager = NeoEssentials.getInstance().getDataManager().getMailManager();
        int count = mailManager.clearMail(player);
        
        if (count > 0) {
            source.sendSuccess(() -> Component.literal(TextUtil.formatText(
                    "&aCleared &6" + count + " &amail message" + (count == 1 ? "" : "s") + ".")), true);
        } else {
            source.sendSuccess(() -> Component.literal(TextUtil.formatText("&aYou had no mail to clear.")), false);
        }
        
        return 1;
    }
    
    /**
     * Executes the mail delete command.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private int executeMailDelete(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        int index = IntegerArgumentType.getInteger(context, "index");
        
        MailManager mailManager = NeoEssentials.getInstance().getDataManager().getMailManager();
        List<MailManager.MailMessage> messages = mailManager.getMail(player);
        
        if (messages.isEmpty()) {
            source.sendSuccess(() -> Component.literal(TextUtil.formatText("&aYou have no mail.")), false);
            return 1;
        }
        
        if (index > messages.size()) {
            source.sendFailure(Component.literal(TextUtil.formatText(
                    "&cYou only have " + messages.size() + " mail message" + (messages.size() == 1 ? "" : "s") + ".")));
            return 0;
        }
        
        MailManager.MailMessage message = messages.get(index - 1);
        boolean deleted = mailManager.deleteMail(player, message.getId());
        
        if (deleted) {
            source.sendSuccess(() -> Component.literal(TextUtil.formatText(
                    "&aDeleted mail message #" + index + ".")), true);
        } else {
            source.sendFailure(Component.literal(TextUtil.formatText(
                    "&cFailed to delete mail message #" + index + ".")));
        }
        
        return deleted ? 1 : 0;
    }
    
    /**
     * Executes the mail send command.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private int executeMailSend(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String targetName = StringArgumentType.getString(context, "player");
        String message = StringArgumentType.getString(context, "message");
        
        UserManager userManager = NeoEssentials.getInstance().getDataManager().getUserManager();
        UUID targetUUID = userManager.getPlayerUUID(targetName);
        
        if (targetUUID == null) {
            source.sendFailure(Component.literal(TextUtil.formatText("&cPlayer not found.")));
            return 0;
        }
        
        // Get the mail manager
        MailManager mailManager = NeoEssentials.getInstance().getDataManager().getMailManager();
        
        // Get the sender's name
        String sender;
        try {
            ServerPlayer player = source.getPlayerOrException();
            sender = player.getScoreboardName();
        } catch (CommandSyntaxException e) {
            // Command block, console, etc.
            sender = source.getTextName();
        }
        
        // Send the mail (30 days expiry by default)
        boolean sent = mailManager.sendMail(targetUUID, sender, message, 30);
        
        if (sent) {
            source.sendSuccess(() -> Component.literal(TextUtil.formatText(
                    "&aMail sent to &6" + targetName + "&a.")), true);
            
            // If the recipient is online, notify them
            ServerPlayer targetPlayer = source.getServer().getPlayerList().getPlayerByName(targetName);
            if (targetPlayer != null) {
                Component notification = Component.literal(TextUtil.formatText(
                        "&aYou have received new mail from &6" + sender + "&a. Type &6/mail read &ato view it."));
                targetPlayer.sendSystemMessage(notification);
            }
        } else {
            source.sendFailure(Component.literal(TextUtil.formatText(
                    "&cFailed to send mail to " + targetName + ".")));
        }
        
        return sent ? 1 : 0;
    }
}
