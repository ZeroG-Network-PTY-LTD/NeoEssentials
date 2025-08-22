package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
import com.zerog.neoessentials.storage.JsonStorage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public class MailCommand {
    /**
     * Send mail to a player
     */
    private static int sendMail(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer sender)) {
            source.sendFailure(Component.literal("§cOnly players can send mail"));
            return 0;
        }
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            String message = StringArgumentType.getString(context, "message");
            if (message.length() > 255) {
                sender.sendSystemMessage(Component.literal("§cMail message is too long! Maximum 255 characters."));
                return 0;
            }
            if (target.getUUID().equals(sender.getUUID())) {
                sender.sendSystemMessage(Component.literal("§cYou cannot send mail to yourself!"));
                return 0;
            }
            JsonStorage.MailEntry entry = new JsonStorage.MailEntry(sender.getName().getString(), message, System.currentTimeMillis());
            mailData.computeIfAbsent(target.getUUID(), k -> new ArrayList<>()).add(entry);
            JsonStorage.saveMail(mailData);
            target.sendSystemMessage(Component.literal("You have new mail from " + sender.getName().getString()));
            sender.sendSystemMessage(Component.literal("Mail sent to " + target.getName().getString()));
            return 1;
        } catch (Exception e) {
            sender.sendSystemMessage(Component.literal("§cError sending mail: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Read mail (default to page 1)
     */
    private static int readMail(CommandContext<CommandSourceStack> context) {
        return readMailPage(context, 1);
    }

    /**
     * Read mail for a specific page
     */
    private static int readMailPage(CommandContext<CommandSourceStack> context) {
        int page = 1;
        try {
            String pageStr = StringArgumentType.getString(context, "page");
            page = Integer.parseInt(pageStr);
        } catch (Exception e) {
            // Use default page 1
        }
        return readMailPage(context, page);
    }

    
    // Mail storage - in production this would be persistent
    private static final Map<UUID, List<JsonStorage.MailEntry>> mailData = new ConcurrentHashMap<>(JsonStorage.loadMail());
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MM/dd HH:mm");
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mail")
            // Send mail
            .then(Commands.literal("send")
                .then(Commands.argument("player", StringArgumentType.string())
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(MailCommand::sendMail))))
            // Read mail
            .then(Commands.literal("read")
                .executes(MailCommand::readMail)
                .then(Commands.argument("page", StringArgumentType.string())
                    .executes(MailCommand::readMailPage)))
            // Clear all mail
            .then(Commands.literal("clear")
                .executes(MailCommand::clearMail))
            // Delete specific mail
            .then(Commands.literal("delete")
                .then(Commands.argument("id", StringArgumentType.string())
                    .executes(MailCommand::deleteMail)))
            // Admin commands
            .then(Commands.literal("clearall")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(MailCommand::clearAllMail))));
    }
    
    
    
    // Internal pagination logic
    private static int readMailPage(CommandContext<CommandSourceStack> context, int page) {
        CommandSourceStack source = context.getSource();
        
        // Only players can read mail
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§cOnly players can read mail"));
            return 0;
        }
        
        List<JsonStorage.MailEntry> mail = mailData.getOrDefault(player.getUUID(), new ArrayList<>());
        
        if (mail.isEmpty()) {
            MessageUtil.sendMessage(player, "§eYou have no mail.");
            return 1;
        }
        
        // Pagination
        int messagesPerPage = 5;
        int totalPages = (int) Math.ceil((double) mail.size() / messagesPerPage);
        
        if (page < 1 || page > totalPages) {
            MessageUtil.sendMessage(player, "§cInvalid page number. Available pages: 1-" + totalPages);
            return 0;
        }
        
        // Header
        MessageUtil.sendMessage(player, "§6===== §eMail (Page " + page + "/" + totalPages + ") §6=====");
        
        // Show messages for this page
        int startIndex = (page - 1) * messagesPerPage;
        int endIndex = Math.min(startIndex + messagesPerPage, mail.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            JsonStorage.MailEntry msg = mail.get(i);
            String timeStr = LocalDateTime.ofEpochSecond(msg.timestamp(), 0, ZoneOffset.UTC).format(TIME_FORMAT);
            MessageUtil.sendMessage(player, "§7[" + (i + 1) + "] §a" + msg.from() + " §7(" + timeStr + "):");
            MessageUtil.sendMessage(player, "§f  " + msg.message());
        }
        
        // Footer
        if (totalPages > 1) {
            MessageUtil.sendMessage(player, "§7Use /mail read <page> to view other pages");
        }
        MessageUtil.sendMessage(player, "§7Use /mail clear to delete all mail");
        
        return 1;
    }
    
    /**
     * Clear all mail for the player
     */
    private static int clearMail(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        // Only players can clear mail
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§cOnly players can clear mail"));
            return 0;
        }
        
        List<JsonStorage.MailEntry> mail = mailData.get(player.getUUID());
        if (mail == null || mail.isEmpty()) {
            MessageUtil.sendMessage(player, "§eYou have no mail to clear.");
            return 1;
        }
        
        int mailCount = mail.size();
        mailData.remove(player.getUUID());
        
        MessageUtil.sendMessage(player, "§aCleared " + mailCount + " mail messages.");
        
        return 1;
    }
    
    /**
     * Delete specific mail by ID
     */
    private static int deleteMail(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        // Only players can delete mail
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§cOnly players can delete mail"));
            return 0;
        }
        
        try {
            String mailId = StringArgumentType.getString(context, "id");
            
            List<JsonStorage.MailEntry> mail = mailData.get(player.getUUID());
            if (mail == null || mail.isEmpty()) {
                MessageUtil.sendMessage(player, "§eYou have no mail.");
                return 1;
            }
            
            boolean found = mail.removeIf(msg -> String.valueOf(msg.timestamp()).equals(mailId));
            
            if (found) {
                MessageUtil.sendMessage(player, "§aDeleted mail with ID: " + mailId);
            } else {
                MessageUtil.sendMessage(player, "§cMail with ID '" + mailId + "' not found.");
            }
            
            return 1;
            
        } catch (Exception e) {
            MessageUtil.sendMessage(player, "§cError deleting mail: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Admin command to clear all mail for a specific player
     */
    private static int clearAllMail(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            
            List<JsonStorage.MailEntry> mail = mailData.get(target.getUUID());
            if (mail == null || mail.isEmpty()) {
                sendMessage(source, "§e" + target.getName().getString() + " has no mail.");
                return 1;
            }
            
            int mailCount = mail.size();
            mailData.remove(target.getUUID());
            
            sendMessage(source, "§aCleared " + mailCount + " mail messages for " + target.getName().getString());
            
            // Notify the target if online
            MessageUtil.sendMessage(target, "§eYour mail was cleared by an admin.");
            
            // Log the action
            source.getServer().sendSystemMessage(Component.literal(
                "§7[Mail] " + getSourceName(source) + " cleared all mail for " + target.getName().getString()));
            
            return 1;
            
        } catch (Exception e) {
            sendMessage(source, "§cError clearing mail: " + e.getMessage());
            return 0;
        }
    }
    
    
    /**
     * Get unread mail count for a player (for join notifications)
     */
    public static int getUnreadMailCount(UUID playerUUID) {
        List<JsonStorage.MailEntry> mail = mailData.get(playerUUID);
        return mail != null ? mail.size() : 0;
    }
    
    /**
     * Clear mail data when a player is removed (cleanup)
     */
    public static void clearPlayerData(UUID playerUUID) {
        mailData.remove(playerUUID);
    }
    
    private static void sendMessage(CommandSourceStack source, String message) {
        if (source.getEntity() instanceof ServerPlayer player) {
            MessageUtil.sendMessage(player, message);
        } else {
            source.sendSuccess(() -> Component.literal(message), false);
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
