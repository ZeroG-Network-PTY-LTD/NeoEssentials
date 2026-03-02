package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player information and admin tool commands ported from EssentialsX:
 *
 *  /seen <player>               — last seen / online info (Commandseen)
 *  /near [radius]               — list nearby players (Commandnear)
 *  /ping [player]               — show network latency (Commandping)
 *  /playtime [player]           — show total play time (Commandplaytime)
 *  /whois <player>              — detailed player info (Commandwhois)
 *  /realname <nickname>         — look up real name from nick (Commandrealname)
 *  /sudo <player> <command>     — force a player to run a command (Commandsudo)
 *  /suicide                     — kill yourself (Commandsuicide)
 *  /msgtoggle [on|off] [player] — block all incoming private messages (Commandmsgtoggle)
 *  /rtoggle [on|off] [player]   — toggle /r replying to last sender (Commandrtoggle)
 *  /motd                        — show the message of the day (Commandmotd)
 *  /rules                       — show server rules (Commandrules)
 */
public class PlayerInfoCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerInfoCommands.class);
    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault());

    // msgtoggle shadow map (UUID-keyed, kept in sync with MsgToggleManager)
    private static final Map<UUID, Boolean> msgToggleBlocked = new ConcurrentHashMap<>();
    // rtoggle state: UUID → reply-to-last-sender enabled (default true)
    private static final Map<UUID, Boolean> rtoggleEnabled = new ConcurrentHashMap<>();

    /** Returns true if the player has blocked incoming private messages.
     *  Delegates to existing MsgToggleManager so both /msgtoggle and MsgCommand share state. */
    public static boolean isMsgBlocked(UUID uuid) {
        // MsgToggleManager uses player name — look up from online list is not available here,
        // so we keep a UUID-keyed shadow map that /msgtoggle keeps in sync.
        return msgToggleBlocked.getOrDefault(uuid, false);
    }

    /** Returns true if the player has /r replying to last sender enabled (default on). */
    public static boolean isRtoggleEnabled(UUID uuid) {
        return rtoggleEnabled.getOrDefault(uuid, true);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (ConfigManager.getInstance().isCommandEnabled("seen"))      registerSeen(dispatcher);
        if (ConfigManager.getInstance().isCommandEnabled("near"))      registerNear(dispatcher);
        if (ConfigManager.getInstance().isCommandEnabled("ping"))      registerPing(dispatcher);
        if (ConfigManager.getInstance().isCommandEnabled("playtime"))  registerPlaytime(dispatcher);
        if (ConfigManager.getInstance().isCommandEnabled("whois"))     registerWhois(dispatcher);
        if (ConfigManager.getInstance().isCommandEnabled("realname"))  registerRealname(dispatcher);
        if (ConfigManager.getInstance().isCommandEnabled("sudo"))      registerSudo(dispatcher);
        if (ConfigManager.getInstance().isCommandEnabled("suicide"))   registerSuicide(dispatcher);
        if (ConfigManager.getInstance().isCommandEnabled("msgtoggle")) registerMsgToggle(dispatcher);
        if (ConfigManager.getInstance().isCommandEnabled("rtoggle"))   registerRtoggle(dispatcher);
        if (ConfigManager.getInstance().isCommandEnabled("motd"))      registerMotd(dispatcher);
        if (ConfigManager.getInstance().isCommandEnabled("rules"))     registerRules(dispatcher);
    }

    // ── /seen <player> ────────────────────────────────────────────────────────
    private static void registerSeen(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("seen")
            .requires(src -> src.getPlayer() == null
                || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.seen"))
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), b))
                .executes(ctx -> executeSeen(ctx, StringArgumentType.getString(ctx, "player")))
            )
        );
    }

    private static int executeSeen(CommandContext<CommandSourceStack> ctx, String name) {
        var src = ctx.getSource();
        // Check online first
        ServerPlayer online = src.getServer().getPlayerList().getPlayerByName(name);
        if (online != null) {
            long ping = online.latency;
            src.sendSuccess(() -> MessageUtil.info("commands.neoessentials.seen.online",
                online.getName().getString(),
                online.serverLevel().dimension().location().getPath(),
                String.format("%.0f, %.0f, %.0f", online.getX(), online.getY(), online.getZ()),
                ping), false);
            return 1;
        }
        // Try profile cache for offline
        var cache = src.getServer().getProfileCache();
        Optional<com.mojang.authlib.GameProfile> profile = cache != null ? cache.get(name) : Optional.empty();
        if (profile.isEmpty()) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", name));
            return 0;
        }
        src.sendSuccess(() -> MessageUtil.info("commands.neoessentials.seen.offline",
            profile.get().getName()), false);
        return 1;
    }

    // ── /near [radius] ────────────────────────────────────────────────────────
    private static void registerNear(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("near")
            .requires(src -> src.getPlayer() == null
                || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.near"))
            .executes(ctx -> executeNear(ctx, 200))
            .then(Commands.argument("radius", IntegerArgumentType.integer(1, 10000))
                .executes(ctx -> executeNear(ctx, IntegerArgumentType.getInteger(ctx, "radius")))
            )
        );
    }

    private static int executeNear(CommandContext<CommandSourceStack> ctx, int radius) {
        var src = ctx.getSource();
        var player = src.getPlayer();
        if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }

        double r2 = (double) radius * radius;
        List<String> nearby = new ArrayList<>();
        for (ServerPlayer other : src.getServer().getPlayerList().getPlayers()) {
            if (other.getUUID().equals(player.getUUID())) continue;
            if (!other.serverLevel().equals(player.serverLevel())) continue;
            double dist = other.distanceToSqr(player.getX(), player.getY(), player.getZ());
            if (dist <= r2) {
                nearby.add(other.getName().getString() + " §7(" + (int) Math.sqrt(dist) + "m)");
            }
        }
        nearby.sort(Comparator.naturalOrder());
        final String list = nearby.isEmpty() ? "§7none" : String.join("§r, §e", nearby);
        final int fr = radius;
        src.sendSuccess(() -> MessageUtil.info("commands.neoessentials.near.result", fr, list), false);
        return 1;
    }

    // ── /ping [player] ────────────────────────────────────────────────────────
    private static void registerPing(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("ping")
            .requires(src -> src.getPlayer() == null
                || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.ping"))
            .executes(ctx -> executePing(ctx, null))
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), b))
                .requires(src -> src.getPlayer() == null
                    || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.ping.others"))
                .executes(ctx -> executePing(ctx, StringArgumentType.getString(ctx, "target")))
            )
        );
    }

    private static int executePing(CommandContext<CommandSourceStack> ctx, String targetName) {
        var src = ctx.getSource();
        ServerPlayer target = targetName != null
            ? src.getServer().getPlayerList().getPlayerByName(targetName)
            : src.getPlayer();
        if (target == null) {
            if (targetName != null) src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", targetName));
            else src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only"));
            return 0;
        }
        int ping = target.latency;
        String color = ping < 80 ? "§a" : ping < 200 ? "§e" : "§c";
        final String name = target.getName().getString();
        src.sendSuccess(() -> MessageUtil.info("commands.neoessentials.ping.result",
            name, color + ping + "§r"), false);
        return 1;
    }

    // ── /playtime [player] ────────────────────────────────────────────────────
    private static void registerPlaytime(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("playtime")
            .requires(src -> src.getPlayer() == null
                || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.playtime"))
            .executes(ctx -> executePlaytime(ctx, null))
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), b))
                .requires(src -> src.getPlayer() == null
                    || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.playtime.others"))
                .executes(ctx -> executePlaytime(ctx, StringArgumentType.getString(ctx, "target")))
            )
        );
    }

    private static int executePlaytime(CommandContext<CommandSourceStack> ctx, String targetName) {
        var src = ctx.getSource();
        ServerPlayer target = targetName != null
            ? src.getServer().getPlayerList().getPlayerByName(targetName)
            : src.getPlayer();
        if (target == null) {
            if (targetName != null) src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", targetName));
            else src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only"));
            return 0;
        }
        // Stats.PLAY_TIME is in ticks (20 ticks = 1 second)
        int ticks = target.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
        long seconds = ticks / 20L;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        final String name = target.getName().getString();
        final String time = String.format("%dh %dm %ds", hours, minutes, secs);
        src.sendSuccess(() -> MessageUtil.info("commands.neoessentials.playtime.result", name, time), false);
        return 1;
    }

    // ── /whois <player> ───────────────────────────────────────────────────────
    private static void registerWhois(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("whois")
            .requires(src -> src.getPlayer() == null
                || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.whois"))
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), b))
                .executes(ctx -> executeWhois(ctx, StringArgumentType.getString(ctx, "target")))
            )
        );
    }

    private static int executeWhois(CommandContext<CommandSourceStack> ctx, String name) {
        var src = ctx.getSource();
        ServerPlayer target = src.getServer().getPlayerList().getPlayerByName(name);
        if (target == null) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", name));
            return 0;
        }
        String uuid = target.getUUID().toString();
        String world = target.serverLevel().dimension().location().getPath();
        String pos = String.format("%.1f, %.1f, %.1f", target.getX(), target.getY(), target.getZ());
        String gm = target.gameMode.getGameModeForPlayer().getName();
        int ping = target.latency;
        int health = (int) target.getHealth();
        int food = target.getFoodData().getFoodLevel();
        src.sendSuccess(() -> Component.literal(
            "§e--- §fWhois: §b" + target.getName().getString() + " §e---\n" +
            "§7UUID: §f" + uuid + "\n" +
            "§7World: §f" + world + " §7@ §f" + pos + "\n" +
            "§7Gamemode: §f" + gm + "  §7Ping: §f" + ping + "ms\n" +
            "§7Health: §f" + health + "/20  §7Food: §f" + food + "/20"
        ), false);
        return 1;
    }

    // ── /realname <nickname> ──────────────────────────────────────────────────
    private static void registerRealname(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("realname")
            .requires(src -> src.getPlayer() == null
                || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.realname"))
            .then(Commands.argument("nickname", StringArgumentType.word())
                .executes(ctx -> {
                    var src = ctx.getSource();
                    String nick = StringArgumentType.getString(ctx, "nickname");
                    // Search online players by display name / custom name
                    for (ServerPlayer p : src.getServer().getPlayerList().getPlayers()) {
                        String displayName = p.getDisplayName().getString();
                        if (displayName.equalsIgnoreCase(nick)
                                || net.minecraft.ChatFormatting.stripFormatting(displayName).equalsIgnoreCase(nick)) {
                            final String real = p.getName().getString();
                            final String dn = displayName;
                            src.sendSuccess(() -> MessageUtil.info("commands.neoessentials.realname.result",
                                nick, real), false);
                            return 1;
                        }
                    }
                    src.sendFailure(MessageUtil.error("commands.neoessentials.realname.not_found", nick));
                    return 0;
                })
            )
        );
    }

    // ── /sudo <player> <command> ──────────────────────────────────────────────
    private static void registerSudo(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("sudo")
            .requires(src -> src.getPlayer() == null
                || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.sudo"))
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), b))
                .then(Commands.argument("command", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        var src = ctx.getSource();
                        String targetName = StringArgumentType.getString(ctx, "target");
                        String command = StringArgumentType.getString(ctx, "command");
                        ServerPlayer target = src.getServer().getPlayerList().getPlayerByName(targetName);
                        if (target == null) {
                            src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", targetName));
                            return 0;
                        }
                        // Exempt check
                        if (PermissionAPI.hasPermission(target.getUUID(), "neoessentials.sudo.exempt")
                                && src.getPlayer() != null) {
                            src.sendFailure(MessageUtil.error("commands.neoessentials.sudo.exempt", targetName));
                            return 0;
                        }
                        // Chat message (c:prefix) vs command
                        if (command.toLowerCase().startsWith("c:")) {
                            target.chat(command.substring(2));
                        } else {
                            String stripped = command.startsWith("/") ? command.substring(1) : command;
                            src.getServer().getCommands().performPrefixedCommand(
                                target.createCommandSourceStack(), stripped);
                        }
                        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.sudo.ran",
                            targetName, command), true);
                        LOGGER.info("{} sudoed {} to run: {}", src.getTextName(), targetName, command);
                        return 1;
                    })
                )
            )
        );
    }

    // ── /suicide ──────────────────────────────────────────────────────────────
    private static void registerSuicide(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("suicide")
            .requires(src -> src.getPlayer() == null
                || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.suicide"))
            .executes(ctx -> {
                var src = ctx.getSource();
                var player = src.getPlayer();
                if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }
                player.hurt(player.damageSources().magic(), Float.MAX_VALUE);
                // Broadcast to others
                final String name = player.getName().getString();
                for (ServerPlayer p : src.getServer().getPlayerList().getPlayers()) {
                    if (!p.getUUID().equals(player.getUUID())) {
                        p.sendSystemMessage(MessageUtil.info("commands.neoessentials.suicide.broadcast", name));
                    }
                }
                return 1;
            })
        );
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

    // ── /rtoggle [on|off] [player] ────────────────────────────────────────────
    private static void registerRtoggle(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("rtoggle")
            .requires(src -> src.getPlayer() == null
                || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.rtoggle"))
            .executes(ctx -> executeRtoggle(ctx, null, null))
            .then(Commands.literal("on").executes(ctx -> executeRtoggle(ctx, null, true)))
            .then(Commands.literal("off").executes(ctx -> executeRtoggle(ctx, null, false)))
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), b))
                .requires(src -> src.getPlayer() == null
                    || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.rtoggle.others"))
                .executes(ctx -> executeRtoggle(ctx, StringArgumentType.getString(ctx, "target"), null))
                .then(Commands.literal("on").executes(ctx -> executeRtoggle(ctx, StringArgumentType.getString(ctx, "target"), true)))
                .then(Commands.literal("off").executes(ctx -> executeRtoggle(ctx, StringArgumentType.getString(ctx, "target"), false)))
            )
        );
    }

    private static int executeRtoggle(CommandContext<CommandSourceStack> ctx, String targetName, Boolean enable) {
        var src = ctx.getSource();
        ServerPlayer target = targetName != null
            ? src.getServer().getPlayerList().getPlayerByName(targetName)
            : src.getPlayer();
        if (target == null) {
            if (targetName != null) src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", targetName));
            else src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only"));
            return 0;
        }
        boolean cur = rtoggleEnabled.getOrDefault(target.getUUID(), true);
        boolean newState = enable != null ? enable : !cur;
        rtoggleEnabled.put(target.getUUID(), newState);
        String label = newState ? "§aenabled" : "§cdisabled";
        boolean isOther = src.getPlayer() == null || !src.getPlayer().getUUID().equals(target.getUUID());
        if (isOther) {
            target.sendSystemMessage(MessageUtil.info("commands.neoessentials.rtoggle.self", label));
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.rtoggle.other",
                target.getName().getString(), label), false);
        } else {
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.rtoggle.self", label), false);
        }
        return 1;
    }

    // ── /motd ─────────────────────────────────────────────────────────────────
    private static void registerMotd(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("motd")
            .requires(src -> src.getPlayer() == null
                || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.motd"))
            .executes(ctx -> {
                var src = ctx.getSource();
                String motd = ConfigManager.getInstance().getMotd();
                if (motd == null || motd.isBlank()) {
                    src.sendSuccess(() -> MessageUtil.info("commands.neoessentials.motd.empty"), false);
                } else {
                    // Replace colour codes and {player} placeholder
                    String playerName = src.getPlayer() != null ? src.getPlayer().getName().getString() : "Console";
                    String formatted = motd.replace("{player}", playerName);
                    src.sendSuccess(() -> MessageUtil.coloredText(formatted), false);
                }
                return 1;
            })
        );
    }

    // ── /rules ────────────────────────────────────────────────────────────────
    private static void registerRules(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("rules")
            .requires(src -> src.getPlayer() == null
                || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.rules"))
            .executes(ctx -> {
                var src = ctx.getSource();
                String rules = ConfigManager.getInstance().getRules();
                if (rules == null || rules.isBlank()) {
                    src.sendSuccess(() -> MessageUtil.info("commands.neoessentials.rules.empty"), false);
                } else {
                    src.sendSuccess(() -> MessageUtil.coloredText(rules), false);
                }
                return 1;
            })
        );
    }
}

