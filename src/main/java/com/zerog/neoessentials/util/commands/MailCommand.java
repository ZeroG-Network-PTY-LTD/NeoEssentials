package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Implements the /mail command - Player-to-player mail system for offline messaging
 * Supports sending, reading, and managing mail messages
 */
public class MailCommand {
    private static final Map<UUID, List<MailMessage>> MAIL_BOX = new ConcurrentHashMap<>();
    private static final Path MAIL_DATA_FILE = Paths.get("config", "neoessentials", "mail_data.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
    
    /**
     * Mail message data class
     */
    private static class MailMessage {
        String sender;
        String message;
        String timestamp;
        boolean read;
        String id;
        
        MailMessage(String sender, String message) {
            this.sender = sender;
            this.message = message;
            this.timestamp = LocalDateTime.now().format(TIME_FORMAT);
            this.read = false;
            this.id = UUID.randomUUID().toString().substring(0, 8);
        }
    }
    
    /**
     * Register the /mail command
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("mail")) return;
        
        // Load mail data on registration
        loadMailData();
        
        dispatcher.register(
            Commands.literal("mail")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                // /mail read [page] - Read mail
                .then(Commands.literal("read")
                    .executes(ctx -> {
                        PermissionValidator.PermissionResult permResult = 
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.mail");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        ServerPlayer player = permResult.getPlayer();
                        return readMail(player, 1);
                    })
                    .then(Commands.argument("page", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            PermissionValidator.PermissionResult permResult = 
                                PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.mail");
                            if (!permResult.hasPermission()) {
                                ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                                return 0;
                            }
                            
                            ServerPlayer player = permResult.getPlayer();
                            int page = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "page");
                            return readMail(player, page);
                        })
                    )
                )
                // /mail send <player> <message> - Send mail
                .then(Commands.literal("send")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                PermissionValidator.PermissionResult permResult = 
                                    PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.mail.send");
                                if (!permResult.hasPermission()) {
                                    ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                                    return 0;
                                }
                                
                                ServerPlayer player = permResult.getPlayer();
                                String targetName = StringArgumentType.getString(ctx, "player");
                                String message = StringArgumentType.getString(ctx, "message");
                                return sendMail(player, targetName, message);
                            })
                        )
                    )
                )
                // /mail delete <id> - Delete mail by ID
                .then(Commands.literal("delete")
                    .then(Commands.argument("id", StringArgumentType.word())
                        .executes(ctx -> {
                            PermissionValidator.PermissionResult permResult = 
                                PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.mail");
                            if (!permResult.hasPermission()) {
                                ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                                return 0;
                            }
                            
                            ServerPlayer player = permResult.getPlayer();
                            String id = StringArgumentType.getString(ctx, "id");
                            return deleteMail(player, id);
                        })
                    )
                )
                // /mail clear - Clear all mail
                .then(Commands.literal("clear")
                    .executes(ctx -> {
                        PermissionValidator.PermissionResult permResult = 
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.mail.clear");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        ServerPlayer player = permResult.getPlayer();
                        return clearMail(player);
                    })
                )
                // /mail - Show usage or unread count
                .executes(ctx -> {
                    PermissionValidator.PermissionResult permResult = 
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.mail");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    
                    ServerPlayer player = permResult.getPlayer();
                    return showMailStatus(player);
                })
        );
    }
    
    /**
     * Show mail status and usage
     */
    private static int showMailStatus(ServerPlayer player) {
        List<MailMessage> messages = MAIL_BOX.get(player.getUUID());
        
        if (messages == null || messages.isEmpty()) {
            player.sendSystemMessage(MessageUtil.info("commands.neoessentials.mail.no_mail"));
            return 1;
        }
        
        long unreadCount = messages.stream().filter(m -> !m.read).count();
        
        player.sendSystemMessage(MessageUtil.info("commands.neoessentials.mail.status", 
            messages.size(), unreadCount));
        player.sendSystemMessage(MessageUtil.info("commands.neoessentials.mail.usage"));
        
        return 1;
    }
    
    /**
     * Read mail messages
     */
    private static int readMail(ServerPlayer player, int page) {
        List<MailMessage> messages = MAIL_BOX.get(player.getUUID());
        
        if (messages == null || messages.isEmpty()) {
            player.sendSystemMessage(MessageUtil.info("commands.neoessentials.mail.no_mail"));
            return 1;
        }
        
        // Sort by timestamp (newest first)
        messages.sort((a, b) -> b.timestamp.compareTo(a.timestamp));
        
        int itemsPerPage = 5;
        int totalPages = (int) Math.ceil((double) messages.size() / itemsPerPage);
        
        if (page > totalPages) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.mail.invalid_page", page, totalPages));
            return 0;
        }
        
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, messages.size());
        
        // Header
        player.sendSystemMessage(MessageUtil.success("commands.neoessentials.mail.header", page, totalPages));
        
        // Messages
        for (int i = startIndex; i < endIndex; i++) {
            MailMessage mail = messages.get(i);
            mail.read = true; // Mark as read
            
            MutableComponent message = Component.literal(
                String.format("§7[§f%s§7] %s§f%s: §7%s", 
                    mail.id,
                    mail.read ? "" : "§e● ",
                    mail.sender,
                    mail.message
                )
            );
            
            // Add hover text with timestamp and delete option
            MutableComponent hoverText = Component.literal("")
                .append(Component.literal("§6Sent: §f" + mail.timestamp + "\n"))
                .append(Component.literal("§6ID: §f" + mail.id + "\n"))
                .append(Component.literal("§7Click to delete this message"));
            
            message = message.withStyle(style -> style
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText))
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/mail delete " + mail.id))
            );
            
            player.sendSystemMessage(message);
        }
        
        // Footer with navigation
        if (totalPages > 1) {
            MutableComponent footer = Component.literal("§7Page " + page + "/" + totalPages + " ");
            
            if (page > 1) {
                footer.append(Component.literal("§7[§a◀ Prev§7]")
                    .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mail read " + (page - 1)))));
                footer.append(Component.literal(" "));
            }
            
            if (page < totalPages) {
                footer.append(Component.literal("§7[§aNext ▶§7]")
                    .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mail read " + (page + 1)))));
            }
            
            player.sendSystemMessage(footer);
        }
        
        // Save changes
        saveMailData();
        
        return 1;
    }
    
    /**
     * Send mail to a player
     */
    private static int sendMail(ServerPlayer sender, String targetName, String message) {
        // Check message length
        if (message.length() > 200) {
            sender.sendSystemMessage(MessageUtil.error("commands.neoessentials.mail.message_too_long"));
            return 0;
        }
        
        // Get target UUID (from online players or offline data)
        UUID targetUUID = getPlayerUUID(sender.getServer(), targetName);
        if (targetUUID == null) {
            sender.sendSystemMessage(MessageUtil.error("commands.neoessentials.mail.player_not_found", targetName));
            return 0;
        }
        
        // Prevent self-mail
        if (targetUUID.equals(sender.getUUID())) {
            sender.sendSystemMessage(MessageUtil.error("commands.neoessentials.mail.cannot_mail_self"));
            return 0;
        }
        
        // Create mail message
        MailMessage mail = new MailMessage(sender.getName().getString(), message);
        
        // Add to target's mailbox
        MAIL_BOX.computeIfAbsent(targetUUID, k -> new ArrayList<>()).add(mail);
        
        // Limit mailbox size (prevent spam)
        List<MailMessage> targetMail = MAIL_BOX.get(targetUUID);
        if (targetMail.size() > 50) {
            targetMail.remove(0); // Remove oldest
        }
        
        // Save data
        saveMailData();
        
        sender.sendSystemMessage(MessageUtil.success("commands.neoessentials.mail.sent", targetName));
        
        // Notify target if online
        ServerPlayer target = sender.getServer().getPlayerList().getPlayer(targetUUID);
        if (target != null) {
            target.sendSystemMessage(MessageUtil.info("commands.neoessentials.mail.received", sender.getName().getString()));
        }
        
        return 1;
    }
    
    /**
     * Delete a mail message by ID
     */
    private static int deleteMail(ServerPlayer player, String id) {
        List<MailMessage> messages = MAIL_BOX.get(player.getUUID());
        
        if (messages == null || messages.isEmpty()) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.mail.no_mail"));
            return 0;
        }
        
        boolean removed = messages.removeIf(mail -> mail.id.equals(id));
        
        if (!removed) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.mail.invalid_id", id));
            return 0;
        }
        
        // Clean up empty mailboxes
        if (messages.isEmpty()) {
            MAIL_BOX.remove(player.getUUID());
        }
        
        saveMailData();
        
        player.sendSystemMessage(MessageUtil.success("commands.neoessentials.mail.deleted", id));
        return 1;
    }
    
    /**
     * Clear all mail
     */
    private static int clearMail(ServerPlayer player) {
        List<MailMessage> messages = MAIL_BOX.get(player.getUUID());
        
        if (messages == null || messages.isEmpty()) {
            player.sendSystemMessage(MessageUtil.info("commands.neoessentials.mail.no_mail"));
            return 0;
        }
        
        int count = messages.size();
        MAIL_BOX.remove(player.getUUID());
        
        saveMailData();
        
        player.sendSystemMessage(MessageUtil.success("commands.neoessentials.mail.cleared", count));
        return 1;
    }
    
    /**
     * Get player UUID by name (online or offline)
     */
    private static UUID getPlayerUUID(net.minecraft.server.MinecraftServer server, String playerName) {
        // Try online players first
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayerByName(playerName);
        if (onlinePlayer != null) {
            return onlinePlayer.getUUID();
        }
        
        // Try to get from user cache
        try {
            com.mojang.authlib.GameProfile profile = server.getProfileCache().get(playerName).orElse(null);
            if (profile != null) {
                return profile.getId();
            }
        } catch (Exception e) {
            // Ignore
        }
        
        return null;
    }
    
    /**
     * Load mail data from file
     */
    private static void loadMailData() {
        try {
            if (!Files.exists(MAIL_DATA_FILE)) {
                Files.createDirectories(MAIL_DATA_FILE.getParent());
                return;
            }
            
            String json = Files.readString(MAIL_DATA_FILE);
            JsonObject data = JsonParser.parseString(json).getAsJsonObject();
            
            for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
                try {
                    UUID playerId = UUID.fromString(entry.getKey());
                    JsonArray mailArray = entry.getValue().getAsJsonArray();
                    
                    List<MailMessage> messages = new ArrayList<>();
                    for (JsonElement element : mailArray) {
                        JsonObject mailObj = element.getAsJsonObject();
                        MailMessage mail = new MailMessage(
                            mailObj.get("sender").getAsString(),
                            mailObj.get("message").getAsString()
                        );
                        mail.timestamp = mailObj.get("timestamp").getAsString();
                        mail.read = mailObj.get("read").getAsBoolean();
                        mail.id = mailObj.get("id").getAsString();
                        messages.add(mail);
                    }
                    
                    if (!messages.isEmpty()) {
                        MAIL_BOX.put(playerId, messages);
                    }
                } catch (Exception e) {
                    // Skip invalid entries
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load mail data: " + e.getMessage());
        }
    }
    
    /**
     * Save mail data to file
     */
    private static void saveMailData() {
        try {
            JsonObject data = new JsonObject();
            
            for (Map.Entry<UUID, List<MailMessage>> entry : MAIL_BOX.entrySet()) {
                JsonArray mailArray = new JsonArray();
                for (MailMessage mail : entry.getValue()) {
                    JsonObject mailObj = new JsonObject();
                    mailObj.addProperty("sender", mail.sender);
                    mailObj.addProperty("message", mail.message);
                    mailObj.addProperty("timestamp", mail.timestamp);
                    mailObj.addProperty("read", mail.read);
                    mailObj.addProperty("id", mail.id);
                    mailArray.add(mailObj);
                }
                data.add(entry.getKey().toString(), mailArray);
            }
            
            Files.createDirectories(MAIL_DATA_FILE.getParent());
            Files.writeString(MAIL_DATA_FILE, GSON.toJson(data));
            
        } catch (Exception e) {
            System.err.println("Failed to save mail data: " + e.getMessage());
        }
    }
    
    /**
     * Check if a player has unread mail (for login notifications)
     */
    public static boolean hasUnreadMail(UUID playerId) {
        List<MailMessage> messages = MAIL_BOX.get(playerId);
        return messages != null && messages.stream().anyMatch(mail -> !mail.read);
    }
    
    /**
     * Get unread mail count for a player
     */
    public static int getUnreadMailCount(UUID playerId) {
        List<MailMessage> messages = MAIL_BOX.get(playerId);
        return messages == null ? 0 : (int) messages.stream().filter(mail -> !mail.read).count();
    }
}