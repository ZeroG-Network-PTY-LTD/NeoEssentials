package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * Implements the /nick command - Allows players to set custom display names/nicknames
 * Supports color codes and nickname management
 */
public class NickCommand {
    private static final Map<UUID, String> NICKNAMES = new ConcurrentHashMap<>();
    private static final Path NICK_DATA_FILE = Paths.get("config", "neoessentials", "nickname_data.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    // Updated to allow hex color codes like &#5d6a2c
    private static final Pattern VALID_NICK_PATTERN = Pattern.compile("^[a-zA-Z0-9_&§#]{1,32}$");
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("&[0-9a-fk-or]|&#[0-9a-fA-F]{6}");
    
    /**
     * Register the /nick command
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("nick")) return;
        
        // Load nickname data on registration
        loadNicknameData();
        
        dispatcher.register(
            Commands.literal("nick")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                // /nick <nickname> - Set nickname
                .then(Commands.argument("nickname", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        PermissionValidator.PermissionResult permResult = 
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.nick");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        ServerPlayer player = permResult.getPlayer();
                        String nickname = StringArgumentType.getString(ctx, "nickname");
                        return setNickname(player, nickname);
                    })
                )
                // /nick reset - Reset nickname
                .then(Commands.literal("reset")
                    .executes(ctx -> {
                        PermissionValidator.PermissionResult permResult = 
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.nick");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        ServerPlayer player = permResult.getPlayer();
                        return resetNickname(player);
                    })
                )
                // /nick off - Remove nickname (alias for reset)
                .then(Commands.literal("off")
                    .executes(ctx -> {
                        PermissionValidator.PermissionResult permResult = 
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.nick");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        ServerPlayer player = permResult.getPlayer();
                        return resetNickname(player);
                    })
                )
                // /nick - Show current nickname
                .executes(ctx -> {
                    PermissionValidator.PermissionResult permResult = 
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.nick");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    
                    ServerPlayer player = permResult.getPlayer();
                    return showCurrentNickname(player);
                })
        );
        
        // Admin command to set other players' nicknames
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
    
    /**
     * Set a player's nickname
     */
    private static int setNickname(ServerPlayer player, String nickname) {
        // Check if nickname is "off" or "reset"
        if (nickname.equalsIgnoreCase("off") || nickname.equalsIgnoreCase("reset")) {
            return resetNickname(player);
        }
        
        // Validate nickname
        if (!isValidNickname(nickname)) {
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
        if (!isValidNickname(nickname)) {
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
     * Update player's display name based on nickname
     */
    private static void updatePlayerDisplayName(ServerPlayer player) {
        String nickname = NICKNAMES.get(player.getUUID());

        if (nickname != null) {
            String formattedNick = nickname.replace("&", "§");
            player.setCustomName(com.zerog.neoessentials.util.MessageUtil.coloredText(formattedNick));
            player.setCustomNameVisible(true);
        } else {
            player.setCustomName(null);
            player.setCustomNameVisible(false);
        }
    }
    
    /**
     * Check if nickname is valid format
     */
    private static boolean isValidNickname(String nickname) {
        return VALID_NICK_PATTERN.matcher(nickname).matches();
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
     * Apply nicknames to all online players (call on server start)
     */
    public static void applyNicknamesToOnlinePlayers(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updatePlayerDisplayName(player);
        }
    }
}