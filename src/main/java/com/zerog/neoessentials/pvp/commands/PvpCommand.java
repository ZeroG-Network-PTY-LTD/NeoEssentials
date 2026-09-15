package com.zerog.neoessentials.pvp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.pvp.PvpManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionLevelCompat;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code /pvp} — server-wide and per-player PvP control.
 *
 * <pre>
 *   /pvp                — show global PvP status and your own opt-out/protection state
 *   /pvp on             — enable PvP server-wide (neoessentials.pvp.admin)
 *   /pvp off            — disable PvP server-wide (neoessentials.pvp.admin)
 *   /pvp toggle         — opt yourself in/out of PvP (neoessentials.pvp.toggle)
 * </pre>
 */
public class PvpCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(PvpCommand.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.isPvpModuleEnabled()) return;
        if (!ConfigManager.getInstance().isCommandEnabled("pvp")) return;

        dispatcher.register(Commands.literal("pvp")
            .requires(src -> PermissionLevelCompat.hasPermission(src, 2) ||
                (src.getPlayer() != null && PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.pvp.status")))
            .executes(ctx -> executeStatus(ctx.getSource()))
            .then(Commands.literal("on")
                .requires(src -> PermissionLevelCompat.hasPermission(src, 2) ||
                    (src.getPlayer() != null && PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.pvp.admin")))
                .executes(ctx -> executeSetGlobal(ctx.getSource(), true)))
            .then(Commands.literal("off")
                .requires(src -> PermissionLevelCompat.hasPermission(src, 2) ||
                    (src.getPlayer() != null && PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.pvp.admin")))
                .executes(ctx -> executeSetGlobal(ctx.getSource(), false)))
            .then(Commands.literal("toggle")
                .requires(src -> PermissionLevelCompat.hasPermission(src, 2) ||
                    (src.getPlayer() != null && PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.pvp.toggle")))
                .executes(ctx -> executeToggleSelf(ctx.getSource())))
        );
    }

    private static int executeStatus(CommandSourceStack src) {
        PvpManager manager = PvpManager.getInstance();
        boolean global = manager.isGlobalPvpEnabled();

        src.sendSuccess(() -> MessageUtil.info("commands.neoessentials.pvp.status_global",
            MessageUtil.localize(global ? "commands.neoessentials.pvp.state_enabled" : "commands.neoessentials.pvp.state_disabled")), false);

        if (src.getEntity() instanceof ServerPlayer player) {
            boolean optedOut = manager.isOptedOut(player.getUUID());
            src.sendSuccess(() -> MessageUtil.info("commands.neoessentials.pvp.status_self",
                MessageUtil.localize(optedOut ? "commands.neoessentials.pvp.state_disabled" : "commands.neoessentials.pvp.state_enabled")), false);

            int protectedSeconds = manager.getNewbieProtectionRemainingSeconds(player.getUUID());
            if (protectedSeconds > 0) {
                src.sendSuccess(() -> MessageUtil.info("commands.neoessentials.pvp.status_newbie_protection", protectedSeconds), false);
            }
        }
        return 1;
    }

    private static int executeSetGlobal(CommandSourceStack src, boolean enabled) {
        PvpManager.getInstance().setGlobalPvpEnabled(enabled);

        String sourceName = src.getEntity() instanceof ServerPlayer player ? player.getName().getString() : "Console";
        NeoLog.info(LOGGER, LogCategory.GENERAL, "[PvP] {} set global PvP to {}", sourceName, enabled);

        src.sendSuccess(() -> MessageUtil.success(enabled
            ? "commands.neoessentials.pvp.global_enabled"
            : "commands.neoessentials.pvp.global_disabled"), true);

        // Broadcast so players in the middle of a fight aren't left guessing why hits suddenly stopped.
        String broadcastKey = enabled ? "commands.neoessentials.pvp.broadcast_enabled" : "commands.neoessentials.pvp.broadcast_disabled";
        for (ServerPlayer p : src.getServer().getPlayerList().getPlayers()) {
            p.sendSystemMessage(MessageUtil.info(broadcastKey));
        }
        return 1;
    }

    private static int executeToggleSelf(CommandSourceStack src) {
        ServerPlayer player;
        try {
            player = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only"));
            return 0;
        }

        PvpManager manager = PvpManager.getInstance();
        boolean newOptedOut = !manager.isOptedOut(player.getUUID());
        if (!manager.setOptedOut(player.getUUID(), newOptedOut)) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.pvp.per_player_toggle_disabled"));
            return 0;
        }

        src.sendSuccess(() -> MessageUtil.success(newOptedOut
            ? "commands.neoessentials.pvp.self_disabled"
            : "commands.neoessentials.pvp.self_enabled"), false);
        return 1;
    }
}
