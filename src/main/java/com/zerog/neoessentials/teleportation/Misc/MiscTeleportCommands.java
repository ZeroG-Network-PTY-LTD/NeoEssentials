package com.zerog.neoessentials.teleportation.Misc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Commands for miscellaneous teleportation: /back, /tpauto
 * Note: /top, /jump, /jumpto, /tpr are implemented in DirectTeleportCommands
 */
public class MiscTeleportCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(MiscTeleportCommands.class);

    // Permission nodes
    private static final String PERMISSION_BACK    = "neoessentials.teleport.back";
    private static final String PERMISSION_TPAUTO  = "neoessentials.tpauto";
    private static final String PERMISSION_TPAUTO_OTHERS = "neoessentials.tpauto.others";

    // tpauto state: UUID → auto-accept all incoming tp requests
    private static final Map<UUID, Boolean> tpAutoState = new ConcurrentHashMap<>();

    /** Returns true if the player has tpauto enabled. Used by TeleportRequestManager. */
    public static boolean isTpAutoEnabled(UUID uuid) {
        return tpAutoState.getOrDefault(uuid, false);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ConfigManager config = ConfigManager.getInstance();

        if (config.isTeleportationEnabled() && config.isCommandEnabled("back")) {
            dispatcher.register(
                Commands.literal("back")
                    .requires(source -> {
                        if (source.getEntity() instanceof ServerPlayer player) {
                            return PermissionAPI.hasPermission(player.getUUID(), PERMISSION_BACK);
                        }
                        return false;
                    })
                    .executes(context -> executeBack(context))
            );
            LOGGER.info("Registered misc teleport commands: /back");
        }

        if (config.isCommandEnabled("tpauto")) {
            registerTpAuto(dispatcher);
            LOGGER.info("Registered misc teleport commands: /tpauto");
        }
    }

    // ── /tpauto [on|off] [player] ─────────────────────────────────────────────
    // Essentials: Commandtpauto — automatically accept all incoming tp requests.
    private static void registerTpAuto(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpauto")
            .requires(src -> src.getPlayer() == null
                || PermissionAPI.hasPermission(src.getPlayer().getUUID(), PERMISSION_TPAUTO))
            .executes(ctx -> executeTpAuto(ctx, null, null))
            .then(Commands.literal("on").executes(ctx -> executeTpAuto(ctx, null, true)))
            .then(Commands.literal("off").executes(ctx -> executeTpAuto(ctx, null, false)))
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), b))
                .requires(src -> src.getPlayer() == null
                    || PermissionAPI.hasPermission(src.getPlayer().getUUID(), PERMISSION_TPAUTO_OTHERS))
                .executes(ctx -> executeTpAuto(ctx, StringArgumentType.getString(ctx, "target"), null))
                .then(Commands.literal("on")
                    .executes(ctx -> executeTpAuto(ctx, StringArgumentType.getString(ctx, "target"), true)))
                .then(Commands.literal("off")
                    .executes(ctx -> executeTpAuto(ctx, StringArgumentType.getString(ctx, "target"), false)))
            )
        );
    }

    private static int executeTpAuto(CommandContext<CommandSourceStack> ctx, String targetName, Boolean enable) {
        var src = ctx.getSource();
        ServerPlayer target = targetName != null
            ? src.getServer().getPlayerList().getPlayerByName(targetName)
            : src.getPlayer();
        if (target == null) {
            if (targetName != null)
                src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", targetName));
            else
                src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only"));
            return 0;
        }
        boolean cur = tpAutoState.getOrDefault(target.getUUID(), false);
        boolean newState = enable != null ? enable : !cur;
        tpAutoState.put(target.getUUID(), newState);
        String label = newState ? "§aenabled" : "§cdisabled";
        boolean isOther = src.getPlayer() == null || !src.getPlayer().getUUID().equals(target.getUUID());
        if (isOther) {
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.tpauto.other",
                target.getName().getString(), label), false);
            target.sendSystemMessage(MessageUtil.info("commands.neoessentials.tpauto.self", label));
        } else {
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.tpauto.self", label), false);
        }
        // Warn if tptoggle is off while enabling tpauto
        if (newState && !com.zerog.neoessentials.util.commands.ItemCustomisationCommands.isTpToggleAllowed(target.getUUID())) {
            target.sendSystemMessage(MessageUtil.warning("commands.neoessentials.tpauto.toggle_warning"));
        }
        return 1;
    }

    private static int executeBack(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            MiscTeleportManager manager = MiscTeleportManager.getInstance();
            boolean success = manager.teleportBack(player);
            return success ? 1 : 0;
        } catch (CommandSyntaxException e) {
            LOGGER.error("Command syntax error in /back", e);
            return 0;
        } catch (Exception e) {
            LOGGER.error("Error executing /back command", e);
            return 0;
        }
    }
}