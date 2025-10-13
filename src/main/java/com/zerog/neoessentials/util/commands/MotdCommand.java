package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import com.google.gson.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Implements the /motd command - Message of the Day system
 * Shows and manages server message of the day
 */
public class MotdCommand {
    private static String currentMotd = "";
    private static String motdAuthor = "Server";
    private static String motdTimestamp = "";
    private static final Path MOTD_DATA_FILE = Paths.get("config", "neoessentials", "motd_data.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
    
    /**
     * Register the /motd command
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("motd")) return;
        
        // Load MOTD data on registration
        loadMotdData();
        
        dispatcher.register(
            Commands.literal("motd")
                // /motd - Show current MOTD
                .executes(ctx -> {
                    PermissionValidator.PermissionResult permResult = 
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.motd");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    
                    return showMotd(ctx.getSource());
                })
                // /motd reload - Reload MOTD from file
                .then(Commands.literal("reload")
                    .executes(ctx -> {
                        PermissionValidator.PermissionResult permResult = 
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.motd.reload");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        return reloadMotd(ctx.getSource());
                    })
                )
                // /motd set <message> - Set MOTD
                .then(Commands.literal("set")
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            PermissionValidator.PermissionResult permResult = 
                                PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.motd.set");
                            if (!permResult.hasPermission()) {
                                ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                                return 0;
                            }
                            
                            String message = StringArgumentType.getString(ctx, "message");
                            String author = ctx.getSource().getEntity() instanceof ServerPlayer ? 
                                ((ServerPlayer) ctx.getSource().getEntity()).getName().getString() : "Console";
                            return setMotd(ctx.getSource(), message, author);
                        })
                    )
                )
                // /motd clear - Clear MOTD
                .then(Commands.literal("clear")
                    .executes(ctx -> {
                        PermissionValidator.PermissionResult permResult = 
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.motd.set");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        String author = ctx.getSource().getEntity() instanceof ServerPlayer ? 
                            ((ServerPlayer) ctx.getSource().getEntity()).getName().getString() : "Console";
                        return clearMotd(ctx.getSource(), author);
                    })
                )
                // /motd broadcast - Broadcast MOTD to all players
                .then(Commands.literal("broadcast")
                    .executes(ctx -> {
                        PermissionValidator.PermissionResult permResult = 
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.motd.broadcast");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        return broadcastMotd(ctx.getSource());
                    })
                )
        );
    }
    
    /**
     * Show the current MOTD
     */
    private static int showMotd(CommandSourceStack source) {
        if (currentMotd.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.motd.no_motd"), false);
            return 1;
        }
        
        // Header
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.motd.header"), false);
        
        // MOTD content (support color codes)
        String formattedMotd = currentMotd.replace("&", "§");
        Component motdComponent = Component.literal(formattedMotd);
        source.sendSuccess(() -> motdComponent, false);
        
        // Footer with author and timestamp
        if (!motdTimestamp.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.motd.footer", motdAuthor, motdTimestamp), false);
        }
        
        return 1;
    }
    
    /**
     * Set the MOTD
     */
    private static int setMotd(CommandSourceStack source, String message, String author) {
        // Validate message length
        if (message.length() > 500) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.motd.too_long"));
            return 0;
        }
        
        currentMotd = message;
        motdAuthor = author;
        motdTimestamp = LocalDateTime.now().format(TIME_FORMAT);
        
        // Save to file
        saveMotdData();
        
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.motd.set"), false);
        return 1;
    }
    
    /**
     * Clear the MOTD
     */
    private static int clearMotd(CommandSourceStack source, String author) {
        currentMotd = "";
        motdAuthor = author;
        motdTimestamp = LocalDateTime.now().format(TIME_FORMAT);
        
        // Save to file
        saveMotdData();
        
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.motd.cleared"), false);
        return 1;
    }
    
    /**
     * Reload MOTD from file
     */
    private static int reloadMotd(CommandSourceStack source) {
        loadMotdData();
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.motd.reloaded"), false);
        return 1;
    }
    
    /**
     * Broadcast MOTD to all online players
     */
    private static int broadcastMotd(CommandSourceStack source) {
        if (currentMotd.isEmpty()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.motd.no_motd_to_broadcast"));
            return 0;
        }
        
        // Get all online players
        List<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();
        
        if (players.isEmpty()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.motd.no_players_online"));
            return 0;
        }
        
        // Send MOTD to all players
        String formattedMotd = currentMotd.replace("&", "§");
        Component motdComponent = Component.literal(formattedMotd);
        
        for (ServerPlayer player : players) {
            player.sendSystemMessage(MessageUtil.success("commands.neoessentials.motd.broadcast_header"));
            player.sendSystemMessage(motdComponent);
            if (!motdTimestamp.isEmpty()) {
                player.sendSystemMessage(MessageUtil.info("commands.neoessentials.motd.footer", motdAuthor, motdTimestamp));
            }
        }
        
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.motd.broadcasted", players.size()), false);
        return 1;
    }
    
    /**
     * Load MOTD data from file
     */
    private static void loadMotdData() {
        try {
            if (!Files.exists(MOTD_DATA_FILE)) {
                Files.createDirectories(MOTD_DATA_FILE.getParent());
                return;
            }
            
            String json = Files.readString(MOTD_DATA_FILE);
            JsonObject data = JsonParser.parseString(json).getAsJsonObject();
            
            currentMotd = data.has("motd") ? data.get("motd").getAsString() : "";
            motdAuthor = data.has("author") ? data.get("author").getAsString() : "Server";
            motdTimestamp = data.has("timestamp") ? data.get("timestamp").getAsString() : "";
            
        } catch (Exception e) {
            System.err.println("Failed to load MOTD data: " + e.getMessage());
            // Set defaults
            currentMotd = "";
            motdAuthor = "Server";
            motdTimestamp = "";
        }
    }
    
    /**
     * Save MOTD data to file
     */
    private static void saveMotdData() {
        try {
            JsonObject data = new JsonObject();
            data.addProperty("motd", currentMotd);
            data.addProperty("author", motdAuthor);
            data.addProperty("timestamp", motdTimestamp);
            
            Files.createDirectories(MOTD_DATA_FILE.getParent());
            Files.writeString(MOTD_DATA_FILE, GSON.toJson(data));
            
        } catch (Exception e) {
            System.err.println("Failed to save MOTD data: " + e.getMessage());
        }
    }
    
    /**
     * Get current MOTD for external use (like join messages)
     */
    public static String getCurrentMotd() {
        return currentMotd;
    }
    
    /**
     * Check if MOTD is set
     */
    public static boolean hasMotd() {
        return !currentMotd.isEmpty();
    }
    
    /**
     * Show MOTD to a player (for join messages)
     */
    public static void showMotdToPlayer(ServerPlayer player) {
        if (!hasMotd()) return;
        
        player.sendSystemMessage(MessageUtil.success("commands.neoessentials.motd.join_header"));
        
        String formattedMotd = currentMotd.replace("&", "§");
        Component motdComponent = Component.literal(formattedMotd);
        player.sendSystemMessage(motdComponent);
        
        if (!motdTimestamp.isEmpty()) {
            player.sendSystemMessage(MessageUtil.info("commands.neoessentials.motd.footer", motdAuthor, motdTimestamp));
        }
    }
}