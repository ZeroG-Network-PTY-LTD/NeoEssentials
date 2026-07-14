package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.CommandSourceHelper;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import com.zerog.neoessentials.util.ResourceUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Implements the /nick command - Allows players to set custom display names/nicknames
 * Supports color codes and nickname management
 */
public class NickCommand {
    private static final Map<UUID, String> NICKNAMES = new ConcurrentHashMap<>();
    private static final Path NICK_DATA_FILE = ResourceUtil.getConfigPath("nickname_data.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    // Updated to allow hex color codes like &#5d6a2c
    private static final Pattern VALID_NICK_PATTERN = Pattern.compile("^[a-zA-Z0-9_&§#]{1,32}$");
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("&[0-9a-fk-or]|&#[0-9a-fA-F]{6}");
    
    /**
     * Register the /nick command
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Load nickname data on registration
        loadNicknameData();

        boolean nickEnabled = ConfigManager.getInstance().isCommandEnabled("nick");
        if (nickEnabled) {
        dispatcher.register(
            Commands.literal("nick")
                // /nick <nickname> - Set nickname
                .then(Commands.argument("nickname", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        ServerPlayer player = CommandSourceHelper.requirePlayer(ctx.getSource(), "commands.neoessentials.nick.player_only");
                        if (player == null) return 0;

                        PermissionValidator.PermissionResult permResult =
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.nick");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        String nickname = StringArgumentType.getString(ctx, "nickname");
                        return setNickname(player, nickname);
                    })
                )
                // /nick reset - Reset nickname
                .then(Commands.literal("reset")
                    .executes(ctx -> {
                        ServerPlayer player = CommandSourceHelper.requirePlayer(ctx.getSource(), "commands.neoessentials.nick.player_only");
                        if (player == null) return 0;

                        PermissionValidator.PermissionResult permResult =
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.nick");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        return resetNickname(player);
                    })
                )
                // /nick off - Remove nickname (alias for reset)
                .then(Commands.literal("off")
                    .executes(ctx -> {
                        ServerPlayer player = CommandSourceHelper.requirePlayer(ctx.getSource(), "commands.neoessentials.nick.player_only");
                        if (player == null) return 0;

                        PermissionValidator.PermissionResult permResult =
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.nick");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        return resetNickname(player);
                    })
                )
                // /nick - Show current nickname
                .executes(ctx -> {
                    ServerPlayer player = CommandSourceHelper.requirePlayer(ctx.getSource(), "commands.neoessentials.nick.player_only");
                    if (player == null) return 0;

                    PermissionValidator.PermissionResult permResult =
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.nick");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    
                    return showCurrentNickname(player);
                })
        );
        }

        // Admin command to set other players' nicknames
        if (ConfigManager.getInstance().isCommandEnabled("setnick")) {
        dispatcher.register(
            Commands.literal("setnick")
                .then(Commands.argument("player", StringArgumentType.word())
                    .then(Commands.argument("nickname", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            PermissionValidator.PermissionResult permResult = 
                                PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.nick.others");
                            if (!permResult.hasPermission()) {
                                ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                                return 0;
                            }
                            
                            String playerName = StringArgumentType.getString(ctx, "player");
                            String nickname = StringArgumentType.getString(ctx, "nickname");
                            return setOtherPlayerNickname(ctx.getSource(), playerName, nickname);
                        })
                    )
                    .then(Commands.literal("reset")
                        .executes(ctx -> {
                            PermissionValidator.PermissionResult permResult = 
                                PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.nick.others");
                            if (!permResult.hasPermission()) {
                                ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                                return 0;
                            }
                            
                            String playerName = StringArgumentType.getString(ctx, "player");
                            return resetOtherPlayerNickname(ctx.getSource(), playerName);
                        })
                    )
                )
        );
        }

        // /nickname alias — mirrors /nick exactly (requires /nick itself to be registered)
        if (nickEnabled && ConfigManager.getInstance().isCommandEnabled("nickname")) {
            dispatcher.register(Commands.literal("nickname").redirect(dispatcher.getRoot().getChild("nick")));
        }
    }
    
    /**
     * Set a player's nickname
     */
    private static int setNickname(ServerPlayer player, String nickname) {
        // Check if nickname is "off" or "reset"
        if (nickname.equalsIgnoreCase("off") || nickname.equalsIgnoreCase("reset")) {
            return resetNickname(player);
        }
        
        // Validate nickname
        if (isInvalidNickname(nickname)) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.nick.invalid_format"));
            return 0;
        }
        
        // Check length without color codes
        String withoutColors = removeColorCodes(nickname);
        if (withoutColors.length() > 16) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.nick.too_long"));
            return 0;
        }
        
        if (withoutColors.length() < 3) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.nick.too_short"));
            return 0;
        }
        
        // Check for color permission
        if (hasColorCodes(nickname) && 
            !PermissionValidator.validatePermission(player.createCommandSourceStack(), "neoessentials.nick.color").hasPermission()) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.nick.no_color_permission"));
            return 0;
        }
        
        // Check if nickname is already taken
        if (isNicknameTaken(nickname, player.getUUID())) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.nick.already_taken"));
            return 0;
        }
        
        // Set nickname
        NICKNAMES.put(player.getUUID(), nickname);
        saveNicknameData();
        
        // Apply nickname to player's display name
        updatePlayerDisplayName(player);
        
        String formattedNick = nickname.replace("&", "§");
        player.sendSystemMessage(MessageUtil.success("commands.neoessentials.nick.set", formattedNick));
        
        return 1;
    }
    
    /**
     * Reset a player's nickname
     */
    private static int resetNickname(ServerPlayer player) {
        if (!NICKNAMES.containsKey(player.getUUID())) {
            player.sendSystemMessage(MessageUtil.info("commands.neoessentials.nick.no_nickname"));
            return 0;
        }
        
        NICKNAMES.remove(player.getUUID());
        saveNicknameData();
        
        // Reset player's display name
        updatePlayerDisplayName(player);
        
        player.sendSystemMessage(MessageUtil.success("commands.neoessentials.nick.reset"));
        return 1;
    }
    
    /**
     * Show current nickname
     */
    private static int showCurrentNickname(ServerPlayer player) {
        String nickname = NICKNAMES.get(player.getUUID());
        
        if (nickname == null) {
            player.sendSystemMessage(MessageUtil.info("commands.neoessentials.nick.no_nickname"));
        } else {
            String formattedNick = nickname.replace("&", "§");
            player.sendSystemMessage(MessageUtil.info("commands.neoessentials.nick.current", formattedNick));
        }
        
        return 1;
    }
    
    /**
     * Set another player's nickname (admin command)
     */
    private static int setOtherPlayerNickname(CommandSourceStack source, String playerName, String nickname) {
        ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(playerName);
        if (target == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.nick.player_not_found", playerName));
            return 0;
        }
        
        // Check if nickname is "off" or "reset"
        if (nickname.equalsIgnoreCase("off") || nickname.equalsIgnoreCase("reset")) {
            return resetOtherPlayerNickname(source, playerName);
        }
        
        // Validate nickname
        if (isInvalidNickname(nickname)) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.nick.invalid_format"));
            return 0;
        }
        
        // Check length
        String withoutColors = removeColorCodes(nickname);
        if (withoutColors.length() > 16 || withoutColors.length() < 3) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.nick.invalid_length"));
            return 0;
        }
        
        // Check if nickname is already taken
        if (isNicknameTaken(nickname, target.getUUID())) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.nick.already_taken"));
            return 0;
        }
        
        // Set nickname
        NICKNAMES.put(target.getUUID(), nickname);
        saveNicknameData();
        
        // Apply nickname
        updatePlayerDisplayName(target);
        
        String formattedNick = nickname.replace("&", "§");
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.nick.set_other", target.getName().getString(), formattedNick), false);
        target.sendSystemMessage(MessageUtil.info("commands.neoessentials.nick.set_by_admin", formattedNick));
        
        return 1;
    }
    
    /**
     * Reset another player's nickname
     */
    private static int resetOtherPlayerNickname(CommandSourceStack source, String playerName) {
        ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(playerName);
        if (target == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.nick.player_not_found", playerName));
            return 0;
        }
        
        if (!NICKNAMES.containsKey(target.getUUID())) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.nick.player_no_nickname", playerName));
            return 0;
        }
        
        NICKNAMES.remove(target.getUUID());
        saveNicknameData();
        
        // Reset display name
        updatePlayerDisplayName(target);
        
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.nick.reset_other", target.getName().getString()), false);
        target.sendSystemMessage(MessageUtil.info("commands.neoessentials.nick.reset_by_admin"));
        
        return 1;
    }
    
    /**
     * Update player's visible name everywhere:
     *   1. Tab-list display name — sent via ClientboundPlayerInfoUpdatePacket to all online players.
     *   2. Chat & placeholder resolution — handled by DefaultPlaceholderExpansion reading NICKNAMES.
     * Note: setCustomName() is intentionally NOT used here.  On players it only adds a floating
     * second label above the real name, has no effect on the tab list, and is invisible in chat.
     */
    private static void updatePlayerDisplayName(ServerPlayer player) {
        String nickname = NICKNAMES.get(player.getUUID());
        net.minecraft.server.MinecraftServer server = player.getServer();
        if (server == null) return;

        // Build the tab-list display name: formatted nickname, or null to revert to real username
        Component tabDisplayName = (nickname != null)
            ? MessageUtil.coloredText(nickname.replace("&", "§"))
            : null;

        // Broadcast UPDATE_DISPLAY_NAME to every connected player (including the nick owner)
        broadcastTabListDisplayName(player, tabDisplayName, server);
    }

    /**
     * Sends a {@code ClientboundPlayerInfoUpdatePacket} that overwrites the tab-list
     * display name for {@code subject} on every connected client.
     *
     * @param subject     the player whose tab entry should be updated
     * @param displayName the new name to show, or {@code null} to revert to the game-profile name
     * @param server      the running server instance
     */
    private static void broadcastTabListDisplayName(ServerPlayer subject,
                                                     @Nullable Component displayName,
                                                     net.minecraft.server.MinecraftServer server) {
        try {
            ClientboundPlayerInfoUpdatePacket.Entry entry = new ClientboundPlayerInfoUpdatePacket.Entry(
                subject.getUUID(),
                subject.getGameProfile(),
                true,
                subject.connection.latency(),
                subject.gameMode.getGameModeForPlayer(),
                displayName,   // null → client falls back to the profile name
                null           // no chat session
            );

            EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions =
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME);

            ClientboundPlayerInfoUpdatePacket packet = buildNickPacket(actions, List.of(entry));
            if (packet == null) return;

            for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
                viewer.connection.send(packet);
            }
        } catch (Exception e) {
            System.err.println("[NeoEssentials] Failed to broadcast tab display name for "
                + subject.getName().getString() + ": " + e.getMessage());
        }
    }

    /**
     * Builds a {@link ClientboundPlayerInfoUpdatePacket} with custom entries via reflection,
     * using the same technique as {@code FakePlayerManager}.
     */
    private static ClientboundPlayerInfoUpdatePacket buildNickPacket(
            EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions,
            List<ClientboundPlayerInfoUpdatePacket.Entry> entries) {
        try {
            ClientboundPlayerInfoUpdatePacket packet =
                new ClientboundPlayerInfoUpdatePacket(actions, Collections.emptyList());
            for (java.lang.reflect.Field f : ClientboundPlayerInfoUpdatePacket.class.getDeclaredFields()) {
                if (List.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    f.set(packet, List.copyOf(entries));
                    return packet;
                }
            }
        } catch (Exception e) {
            System.err.println("[NeoEssentials] buildNickPacket reflection error: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Check if nickname is invalid format
     */
    private static boolean isInvalidNickname(String nickname) {
        return !VALID_NICK_PATTERN.matcher(nickname).matches();
    }
    
    /**
     * Check if nickname contains color codes
     */
    private static boolean hasColorCodes(String nickname) {
        return COLOR_CODE_PATTERN.matcher(nickname).find();
    }
    
    /**
     * Remove color codes from nickname
     */
    private static String removeColorCodes(String nickname) {
        return COLOR_CODE_PATTERN.matcher(nickname).replaceAll("");
    }
    
    /**
     * Check if nickname is already taken by another player
     */
    private static boolean isNicknameTaken(String nickname, UUID excludePlayer) {
        String cleanNickname = removeColorCodes(nickname).toLowerCase();
        
        return NICKNAMES.entrySet().stream()
            .filter(entry -> !entry.getKey().equals(excludePlayer))
            .anyMatch(entry -> removeColorCodes(entry.getValue()).toLowerCase().equals(cleanNickname));
    }
    
    /**
     * Get player's nickname
     */
    public static String getNickname(UUID playerId) {
        return NICKNAMES.get(playerId);
    }
    
    /**
     * Get player's display name (nickname or real name)
     */
    public static String getDisplayName(ServerPlayer player) {
        String nickname = NICKNAMES.get(player.getUUID());
        if (nickname != null) {
            return nickname.replace("&", "§");
        }
        return player.getName().getString();
    }
    
    /**
     * Load nickname data from file
     */
    private static void loadNicknameData() {
        try {
            if (!Files.exists(NICK_DATA_FILE)) {
                Files.createDirectories(NICK_DATA_FILE.getParent());
                return;
            }
            
            String json = Files.readString(NICK_DATA_FILE);
            JsonObject data = JsonParser.parseString(json).getAsJsonObject();
            
            for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
                try {
                    UUID playerId = UUID.fromString(entry.getKey());
                    String nickname = entry.getValue().getAsString();
                    NICKNAMES.put(playerId, nickname);
                } catch (Exception e) {
                    // Skip invalid entries
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load nickname data: " + e.getMessage());
        }
    }
    
    /**
     * Save nickname data to file
     */
    private static void saveNicknameData() {
        try {
            JsonObject data = new JsonObject();
            
            for (Map.Entry<UUID, String> entry : NICKNAMES.entrySet()) {
                data.addProperty(entry.getKey().toString(), entry.getValue());
            }
            
            Files.createDirectories(NICK_DATA_FILE.getParent());
            Files.writeString(NICK_DATA_FILE, GSON.toJson(data));
            
        } catch (Exception e) {
            System.err.println("Failed to save nickname data: " + e.getMessage());
        }
    }
    
    /**
     * Apply nicknames to all online players (call on server start / reload).
     * Sends tab-list display-name packets so every viewer sees the correct nickname immediately.
     */
    public static void applyNicknamesToOnlinePlayers(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updatePlayerDisplayName(player);
        }
    }

    /**
     * Called when a player joins the server.
     * Restores their tab-list display name if they had a nickname before logging out.
     */
    public static void onPlayerJoin(ServerPlayer player) {
        if (NICKNAMES.containsKey(player.getUUID())) {
            updatePlayerDisplayName(player);
        }
    }
}