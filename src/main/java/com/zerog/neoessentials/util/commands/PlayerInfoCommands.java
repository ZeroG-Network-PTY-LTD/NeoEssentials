package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registers commands that do not have their own dedicated command class.
 * Commands that have dedicated classes (NearCommand, PingCommand, SeenCommand,
 * WhoisCommand, RealnameCommand, SuicideCommand, MotdCommand, RulesCommand, etc.)
 * are registered by those classes directly and must NOT be registered here to avoid
 * Brigadier command-tree conflicts where the last registration silently overrides
 * the dedicated file's execution handler.
 *
 * <p>Currently owns: /msgtoggle (with on/off subcommands and admin targeting).
 * The /togglemsg and /mt aliases are still provided by MsgToggleCommand in
 * chat.command. The /rtoggle command is owned by MiscItemCommands.</p>
 */
@SuppressWarnings({"unused", "resource"}) // Public API methods used externally; ServerLevel is not AutoCloseable
public class PlayerInfoCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerInfoCommands.class);

    // msgtoggle shadow map (UUID-keyed, kept in sync with MsgToggleManager)
    private static final Map<UUID, Boolean> msgToggleBlocked = new ConcurrentHashMap<>();

    /** Returns true if the player has blocked incoming private messages. */
    public static boolean isMsgBlocked(UUID uuid) {
        return msgToggleBlocked.getOrDefault(uuid, false);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /msgtoggle owns on/off subcommands and admin targeting.
        // MsgToggleCommand in chat.command still provides the /togglemsg and /mt aliases.
        if (ConfigManager.getInstance().isCommandEnabled("msgtoggle")) registerMsgToggle(dispatcher);
    }

    // ── /msgtoggle [on|off] [player] ─────────────────────────────────────────
    private static void registerMsgToggle(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("msgtoggle")
            .requires(src -> src.getPlayer() == null
                || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.msgtoggle"))
            .executes(ctx -> executeMsgToggle(ctx, null, null))
            .then(Commands.literal("on").executes(ctx -> executeMsgToggle(ctx, null, false)))   // "on" = allow messages
            .then(Commands.literal("off").executes(ctx -> executeMsgToggle(ctx, null, true)))   // "off" = block messages
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), b))
                .requires(src -> src.getPlayer() == null
                    || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.msgtoggle.others"))
                .executes(ctx -> executeMsgToggle(ctx, StringArgumentType.getString(ctx, "target"), null))
                .then(Commands.literal("on").executes(ctx -> executeMsgToggle(ctx, StringArgumentType.getString(ctx, "target"), false)))
                .then(Commands.literal("off").executes(ctx -> executeMsgToggle(ctx, StringArgumentType.getString(ctx, "target"), true)))
            )
        );
    }

    private static int executeMsgToggle(CommandContext<CommandSourceStack> ctx, String targetName, Boolean block) {
        var src = ctx.getSource();
        ServerPlayer target = targetName != null
            ? src.getServer().getPlayerList().getPlayerByName(targetName)
            : src.getPlayer();
        if (target == null) {
            if (targetName != null) src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", targetName));
            else src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only"));
            return 0;
        }
        boolean cur = msgToggleBlocked.getOrDefault(target.getUUID(), false);
        boolean newBlocked = block != null ? block : !cur;
        msgToggleBlocked.put(target.getUUID(), newBlocked);
        // Also sync with the existing MsgToggleManager (name-based, used by MsgCommand)
        boolean currentMsgToggle = com.zerog.neoessentials.chat.MsgToggleManager.isMsgToggled(target);
        if (currentMsgToggle != newBlocked) {
            com.zerog.neoessentials.chat.MsgToggleManager.toggleMsg(target);
        }
        String label = newBlocked ? "§cdisabled" : "§aenabled";
        boolean isOther = src.getPlayer() == null || !src.getPlayer().getUUID().equals(target.getUUID());
        if (isOther) {
            target.sendSystemMessage(MessageUtil.info("commands.neoessentials.msgtoggle.self", label));
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.msgtoggle.other",
                target.getName().getString(), label), false);
        } else {
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.msgtoggle.self", label), false);
        }
        return 1;
    }
}
