package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Fly command implementation - /fly [player]
 * Toggles flight mode for players
 * 
 * @author ZeroG
 * @since 2.0.0
 */
@SuppressWarnings("deprecation")
public class FlyCommand {
    
    // Persistent fly states
    private static final java.util.Map<UUID, Boolean> flyStates = new java.util.HashMap<>();
    private static final String FLY_DATA_FILE = "config/neoessentials_fly.json";
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fly")
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.FLY_SELF))
            .executes(ctx -> toggleSelf(ctx.getSource()))
            .then(Commands.argument("mode", com.mojang.brigadier.arguments.StringArgumentType.word())
                .suggests((c,b) -> { b.suggest("on"); b.suggest("off"); b.suggest("toggle"); return b.buildFuture(); })
                .executes(ctx -> setSelf(ctx.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "mode"))))
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.FLY_OTHERS))
                .executes(ctx -> toggleOther(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))
            )
        );
        // Alias: /fl
        dispatcher.register(Commands.literal("fl")
            .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.FLY_SELF))
            .executes(ctx -> toggleSelf(ctx.getSource()))
            .then(Commands.argument("mode", com.mojang.brigadier.arguments.StringArgumentType.word())
                .suggests((c,b) -> { b.suggest("on"); b.suggest("off"); b.suggest("toggle"); return b.buildFuture(); })
                .executes(ctx -> setSelf(ctx.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "mode"))))
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> PermissionUtil.hasPermission(source, PermissionNodes.FLY_OTHERS))
                .executes(ctx -> toggleOther(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))
            )
        );
    }
    
    /**
     * Toggle fly mode for the command executor
     */
    private static int toggleSelf(CommandSourceStack source) {
        if (source.getPlayer() instanceof ServerPlayer player) {
            return toggleFlight(player);
        }
        return 0;
    }

    private static int setSelf(CommandSourceStack source, String mode) {
        if (source.getPlayer() instanceof ServerPlayer player) {
            return setFlight(player, mode);
        }
        return 0;
    }

    private static int toggleOther(CommandSourceStack source, ServerPlayer target) {
        boolean enabled = !flyStates.getOrDefault(target.getUUID(), false);
        setFlight(target, enabled ? "on" : "off");
        String msgKey = enabled ? "neoessentials.fly.enabled_other" : "neoessentials.fly.disabled_other";
        String message = com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(source.getPlayer(), msgKey, target.getName().getString());
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(message), true);
        return 1;
    }
    
    /**
     * Toggle fly mode for another player
     */
    
    /**
     * Toggle fly mode for a player
     * @return true if fly is now enabled, false if disabled
     */
    private static int toggleFlight(ServerPlayer player) {
        boolean enabled = !flyStates.getOrDefault(player.getUUID(), false);
        return setFlight(player, enabled ? "on" : "off");
    }

    private static int setFlight(ServerPlayer player, String mode) {
        boolean enabled = switch (mode.toLowerCase()) {
            case "on" -> true;
            case "off" -> false;
            default -> !flyStates.getOrDefault(player.getUUID(), false);
        };
        player.getAbilities().mayfly = enabled;
        player.onUpdateAbilities();
        flyStates.put(player.getUUID(), enabled);
        saveFlyStates();
        String msgKey = enabled ? "neoessentials.fly.enabled_self" : "neoessentials.fly.disabled_self";
        String message = com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, msgKey);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(message), true);
        return 1;
    }
    
    /**
     * Check if a player has fly enabled
     */
    public static boolean canFly(ServerPlayer player) {
        return flyStates.getOrDefault(player.getUUID(), false);
    }
    
    /**
     * Remove player from fly mode (called when player leaves or changes gamemode)
     */
    public static void removePlayer(UUID playerId) {
        flyStates.remove(playerId);
        saveFlyStates();
    }
    
    /**
     * Get all players with fly enabled
     */
    public static Set<UUID> getFlyingPlayers() {
        Set<UUID> enabled = new HashSet<>();
        for (var entry : flyStates.entrySet()) {
            if (entry.getValue()) enabled.add(entry.getKey());
        }
        return enabled;
    }
    
    /**
     * Handle gamemode change - preserve fly status if appropriate
     */
    public static void onGameModeChange(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        if (flyStates.getOrDefault(player.getUUID(), false)) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
    }

    // Persistent storage
    private static void saveFlyStates() {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(FLY_DATA_FILE);
            java.util.Map<String, Boolean> data = new java.util.HashMap<>();
            for (var entry : flyStates.entrySet()) {
                data.put(entry.getKey().toString(), entry.getValue());
            }
            String json = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(java.util.Map.of("players", data));
            java.nio.file.Files.createDirectories(path.getParent());
            java.nio.file.Files.writeString(path, json);
        } catch (Exception e) {
            // Ignore
        }
    }

    public static void loadFlyStates() {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(FLY_DATA_FILE);
            if (!java.nio.file.Files.exists(path)) return;
            String json = java.nio.file.Files.readString(path);
            var obj = new com.google.gson.Gson().fromJson(json, java.util.Map.class);
            var players = (java.util.Map<?,?>) obj.get("players");
            flyStates.clear();
            for (var entry : players.entrySet()) {
                flyStates.put(UUID.fromString(entry.getKey().toString()), Boolean.parseBoolean(entry.getValue().toString()));
            }
        } catch (Exception e) {
            // Ignore
        }
    }
}