package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.api.PlaceholderAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import com.zerog.neoessentials.util.motd.MotdManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implements the /motd command family.
 * <pre>
 * /motd                                         – show active MOTD
 * /motd set <message>                           – set active profile's MOTD
 * /motd clear                                   – clear active profile's MOTD
 * /motd reload                                  – reload profiles from disk
 * /motd broadcast                               – broadcast to all players
 * /motd profile list                            – list all profiles
 * /motd profile create <name> <message>         – create / overwrite a profile
 * /motd profile delete <name>                   – delete a profile
 * /motd profile switch <name>                   – set active profile
 * /motd profile info [name]                     – show profile details
 * /motd rotation enable <intervalMinutes>       – enable auto-rotation
 * /motd rotation disable                        – disable rotation
 * /motd rotation next                           – rotate immediately
 * </pre>
 */
public class MotdCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("motd")) return;
        // Ensure manager is initialised (loads data)
        MotdManager.getInstance();

        dispatcher.register(
            Commands.literal("motd")
                .executes(ctx -> showMotd(ctx.getSource()))

                .then(Commands.literal("set")
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            if (!checkPerm(ctx.getSource(), "neoessentials.motd.set")) return 0;
                            return setMotd(ctx.getSource(), StringArgumentType.getString(ctx, "message"));
                        })
                    )
                )

                .then(Commands.literal("clear")
                    .executes(ctx -> {
                        if (!checkPerm(ctx.getSource(), "neoessentials.motd.set")) return 0;
                        return clearMotd(ctx.getSource());
                    })
                )

                .then(Commands.literal("reload")
                    .executes(ctx -> {
                        if (!checkPerm(ctx.getSource(), "neoessentials.motd.reload")) return 0;
                        return reloadMotd(ctx.getSource());
                    })
                )

                .then(Commands.literal("broadcast")
                    .executes(ctx -> {
                        if (!checkPerm(ctx.getSource(), "neoessentials.motd.broadcast")) return 0;
                        return broadcastMotd(ctx.getSource());
                    })
                )

                // ── /motd profile … ──────────────────────────────────────────
                .then(Commands.literal("profile")
                    .then(Commands.literal("list")
                        .executes(ctx -> {
                            if (!checkPerm(ctx.getSource(), "neoessentials.motd.profile")) return 0;
                            return listProfiles(ctx.getSource());
                        })
                    )
                    .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    if (!checkPerm(ctx.getSource(), "neoessentials.motd.profile")) return 0;
                                    return createProfile(ctx.getSource(),
                                            StringArgumentType.getString(ctx, "name"),
                                            StringArgumentType.getString(ctx, "message"));
                                })
                            )
                        )
                    )
                    .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                MotdManager.getInstance().getProfiles().keySet(), b))
                            .executes(ctx -> {
                                if (!checkPerm(ctx.getSource(), "neoessentials.motd.profile")) return 0;
                                return deleteProfile(ctx.getSource(), StringArgumentType.getString(ctx, "name"));
                            })
                        )
                    )
                    .then(Commands.literal("switch")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                MotdManager.getInstance().getProfiles().keySet(), b))
                            .executes(ctx -> {
                                if (!checkPerm(ctx.getSource(), "neoessentials.motd.profile")) return 0;
                                return switchProfile(ctx.getSource(), StringArgumentType.getString(ctx, "name"));
                            })
                        )
                    )
                    .then(Commands.literal("info")
                        .executes(ctx -> {
                            if (!checkPerm(ctx.getSource(), "neoessentials.motd.profile")) return 0;
                            return profileInfo(ctx.getSource(), null);
                        })
                        .then(Commands.argument("name", StringArgumentType.word())
                            .suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                MotdManager.getInstance().getProfiles().keySet(), b))
                            .executes(ctx -> {
                                if (!checkPerm(ctx.getSource(), "neoessentials.motd.profile")) return 0;
                                return profileInfo(ctx.getSource(), StringArgumentType.getString(ctx, "name"));
                            })
                        )
                    )
                )

                // ── /motd rotation … ─────────────────────────────────────────
                .then(Commands.literal("rotation")
                    .then(Commands.literal("enable")
                        .then(Commands.argument("intervalMinutes", IntegerArgumentType.integer(1, 10080))
                            .executes(ctx -> {
                                if (!checkPerm(ctx.getSource(), "neoessentials.motd.rotation")) return 0;
                                return setRotation(ctx.getSource(), true,
                                        IntegerArgumentType.getInteger(ctx, "intervalMinutes"));
                            })
                        )
                    )
                    .then(Commands.literal("disable")
                        .executes(ctx -> {
                            if (!checkPerm(ctx.getSource(), "neoessentials.motd.rotation")) return 0;
                            return setRotation(ctx.getSource(), false, 60);
                        })
                    )
                    .then(Commands.literal("next")
                        .executes(ctx -> {
                            if (!checkPerm(ctx.getSource(), "neoessentials.motd.rotation")) return 0;
                            return rotateNext(ctx.getSource());
                        })
                    )
                )
        );
    }

    // ── Permission helper ──────────────────────────────────────────────────────

    private static boolean checkPerm(CommandSourceStack source, String node) {
        PermissionValidator.PermissionResult r = PermissionValidator.validatePermission(source, node);
        if (!r.hasPermission()) {
            source.sendFailure(MessageUtil.error(r.getErrorMessage()));
            return false;
        }
        return true;
    }

    private static String senderName(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer p ? p.getName().getString() : "Console";
    }

    // ── Commands ───────────────────────────────────────────────────────────────

    private static int showMotd(CommandSourceStack source) {
        if (!checkPerm(source, "neoessentials.motd")) return 0;
        MotdManager mgr = MotdManager.getInstance();
        if (!mgr.hasMotd()) {
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.motd.no_motd"), false);
            return 1;
        }
        MotdManager.MotdProfile p = mgr.getActiveProfile();
        ServerPlayer player = source.getEntity() instanceof ServerPlayer sp ? sp : null;
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.motd.header"), false);
        for (Component line : buildMotdLines(p.motd, player)) {
            source.sendSuccess(() -> line, false);
        }
        if (!p.timestamp.isEmpty()) {
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.motd.footer", p.author, p.timestamp), false);
        }
        return 1;
    }

    private static int setMotd(CommandSourceStack source, String message) {
        MotdManager mgr = MotdManager.getInstance();
        String error = mgr.setProfile(mgr.getActiveProfileName(), message, senderName(source));
        if (error != null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.motd.save_error", error));
            return 0;
        }
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.motd.set"), false);
        return 1;
    }

    private static int clearMotd(CommandSourceStack source) {
        MotdManager mgr = MotdManager.getInstance();
        String error = mgr.setProfile(mgr.getActiveProfileName(), "", senderName(source));
        if (error != null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.motd.save_error", error));
            return 0;
        }
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.motd.cleared"), false);
        return 1;
    }

    private static int reloadMotd(CommandSourceStack source) {
        String error = MotdManager.getInstance().load();
        if (error != null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.motd.load_error", error));
            return 0;
        }
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.motd.reloaded"), false);
        return 1;
    }

    private static int broadcastMotd(CommandSourceStack source) {
        MotdManager mgr = MotdManager.getInstance();
        if (!mgr.hasMotd()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.motd.no_motd_to_broadcast"));
            return 0;
        }
        List<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();
        if (players.isEmpty()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.motd.no_players_online"));
            return 0;
        }
        MotdManager.MotdProfile p = mgr.getActiveProfile();
        int sent = 0;
        for (ServerPlayer player : players) {
            PermissionValidator.PermissionResult pr =
                    PermissionValidator.validatePermission(player.createCommandSourceStack(), "neoessentials.motd");
            if (pr.hasPermission()) {
                player.sendSystemMessage(MessageUtil.success("commands.neoessentials.motd.broadcast_header"));
                for (Component line : buildMotdLines(p.motd, player)) {
                    player.sendSystemMessage(line);
                }
                if (!p.timestamp.isEmpty()) {
                    player.sendSystemMessage(MessageUtil.info("commands.neoessentials.motd.footer", p.author, p.timestamp));
                }
                sent++;
            }
        }
        final int finalSent = sent;
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.motd.broadcasted", finalSent), false);
        return 1;
    }

    // ── Profile subcommands ────────────────────────────────────────────────────

    private static int listProfiles(CommandSourceStack source) {
        MotdManager mgr = MotdManager.getInstance();
        String active = mgr.getActiveProfileName();
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.motd.profile.list_header"), false);
        for (String name : mgr.getProfiles().keySet()) {
            boolean isActive = name.equals(active);
            source.sendSuccess(() -> isActive
                    ? MessageUtil.component("commands.neoessentials.motd.profile.entry_active", name)
                    : MessageUtil.component("commands.neoessentials.motd.profile.entry_inactive", name), false);
        }
        return 1;
    }

    private static int createProfile(CommandSourceStack source, String name, String message) {
        String error = MotdManager.getInstance().setProfile(name, message, senderName(source));
        if (error != null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.motd.save_error", error));
            return 0;
        }
        final String n = name.toLowerCase();
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.motd.profile.created", n), false);
        return 1;
    }

    private static int deleteProfile(CommandSourceStack source, String name) {
        String error = MotdManager.getInstance().deleteProfile(name);
        if (error != null) {
            source.sendFailure(MessageUtil.error(error));
            return 0;
        }
        final String n = name.toLowerCase();
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.motd.profile.deleted", n), false);
        return 1;
    }

    private static int switchProfile(CommandSourceStack source, String name) {
        String error = MotdManager.getInstance().switchProfile(name);
        if (error != null) {
            source.sendFailure(MessageUtil.error(error));
            return 0;
        }
        final String n = name.toLowerCase();
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.motd.profile.switched", n), false);
        return 1;
    }

    private static int profileInfo(CommandSourceStack source, String name) {
        MotdManager mgr = MotdManager.getInstance();
        String target = (name == null) ? mgr.getActiveProfileName() : name.toLowerCase();
        if (!mgr.getProfiles().containsKey(target)) {
            source.sendFailure(MessageUtil.error("Profile '" + target + "' does not exist"));
            return 0;
        }
        MotdManager.MotdProfile p = mgr.getProfiles().get(target);
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.motd.profile.info_header", target), false);
        String motdDisplay = p.motd.isEmpty() ? MessageUtil.localize("commands.neoessentials.motd.profile.not_set") : p.motd;
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.motd.profile.info_motd", motdDisplay), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.motd.profile.info_author", p.author), false);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.motd.profile.info_saved", p.timestamp), false);
        return 1;
    }

    // ── Rotation subcommands ───────────────────────────────────────────────────

    private static int setRotation(CommandSourceStack source, boolean enabled, int intervalMinutes) {
        String error = MotdManager.getInstance().setRotation(enabled, intervalMinutes);
        if (error != null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.motd.save_error", error));
            return 0;
        }
        if (enabled) {
            final int iv = intervalMinutes;
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.motd.rotation.enabled", iv), false);
        } else {
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.motd.rotation.disabled"), false);
        }
        return 1;
    }

    private static int rotateNext(CommandSourceStack source) {
        MotdManager.getInstance().rotateNext();
        String now = MotdManager.getInstance().getActiveProfileName();
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.motd.rotation.rotated", now), false);
        return 1;
    }

    // ── Public helpers ─────────────────────────────────────────────────────────

    /** @return active MOTD text (may be empty). */
    public static String getCurrentMotd() {
        return MotdManager.getInstance().getActiveMotd();
    }

    /** @return {@code true} if there is a non-empty active MOTD. */
    public static boolean hasMotd() {
        return MotdManager.getInstance().hasMotd();
    }

    /** Show MOTD to a player on join. */
    public static void showMotdToPlayer(ServerPlayer player) {
        MotdManager mgr = MotdManager.getInstance();
        if (!mgr.hasMotd()) return;
        MotdManager.MotdProfile p = mgr.getActiveProfile();
        player.sendSystemMessage(MessageUtil.success("commands.neoessentials.motd.join_header"));
        for (Component line : buildMotdLines(p.motd, player)) {
            player.sendSystemMessage(line);
        }
        if (!p.timestamp.isEmpty()) {
            player.sendSystemMessage(MessageUtil.info("commands.neoessentials.motd.footer", p.author, p.timestamp));
        }
    }

    // ── MOTD rendering ─────────────────────────────────────────────────────────

    /**
     * Resolve and split a MOTD string into display-ready {@link Component}s,
     * one per visual line.
     *
     * <p>Processing pipeline:
     * <ol>
     *   <li>Short-form placeholder aliases:
     *       {@code {player}} / {@code {name}} → player name,
     *       {@code {online}} / {@code {players}} → online count,
     *       {@code {max}} → max players,
     *       {@code {time}} → current wall-clock time (12 h)</li>
     *   <li>Full {@link PlaceholderAPI} resolution
     *       ({@code {neoessentials_...}}, {@code {luckperms_...}}, etc.)</li>
     *   <li>{@code &}-codes → {@code §} Minecraft formatting codes</li>
     *   <li>Literal {@code \n} sequences (e.g. typed in a command) and real
     *       newline characters are both used to split into individual lines.</li>
     * </ol>
     *
     * @param motd   raw MOTD text from the active profile
     * @param player player context; {@code null} when called by console / server
     * @return one {@link Component} per visual line (never {@code null})
     */
    public static List<Component> buildMotdLines(String motd, @Nullable ServerPlayer player) {
        if (motd == null || motd.isEmpty()) return List.of();

        String text = motd;

        // 1. Short-form aliases — resolved before PlaceholderAPI so they work
        //    without requiring the full neoessentials_ prefix.
        if (player != null) {
            text = text
                .replace("{player}", player.getName().getString())
                .replace("{name}",   player.getName().getString());
            if (player.level().getServer() != null) {
                text = text
                    .replace("{online}",  String.valueOf(player.level().getServer().getPlayerCount()))
                    .replace("{players}", String.valueOf(player.level().getServer().getPlayerCount()))
                    .replace("{max}",     String.valueOf(player.level().getServer().getMaxPlayers()));
            }
        }
        // Wall-clock time alias (usable even without a player context)
        text = text.replace("{time}", java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a")));

        // 2. Full PlaceholderAPI resolution ({neoessentials_name}, {luckperms_prefix}, …)
        text = PlaceholderAPI.setPlaceholders(player, text);

        // 3. & → § Minecraft color/formatting codes
        text = text.replace("&", "§");

        // 4. Normalise line separators:
        //    • "\\n" = literal backslash-n (from /motd set … or direct JSON editing)
        //    • "\n"  = real newline (Gson-parsed JSON or previously stored text)
        //    Both are converted to a real newline so a single split() covers all cases.
        text = text.replace("\\n", "\n");

        String[] lines = text.split("\n", -1);
        List<Component> result = new ArrayList<>(lines.length);
        for (String line : lines) {
            result.add(Component.literal(line));
        }
        return result;
    }
}