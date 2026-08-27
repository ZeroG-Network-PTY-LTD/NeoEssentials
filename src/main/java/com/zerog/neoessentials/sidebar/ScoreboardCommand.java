package com.zerog.neoessentials.sidebar;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * /scoreboard — Admin + player command for the NeoEssentials sidebar scoreboard.
 *
 * <pre>
 * /scoreboard                                — help
 * /scoreboard toggle                         — player self-toggle (persisted)
 * /scoreboard reload                         — reload scoreboard.json and push to all players
 * /scoreboard enable | disable               — enable/disable the whole system
 * /scoreboard info                           — status summary
 * /scoreboard preview                        — re-send your own board now
 * /scoreboard set title &lt;board&gt; &lt;text&gt;
 * /scoreboard set line &lt;board&gt; &lt;index&gt; &lt;text&gt;
 * /scoreboard board list
 * /scoreboard player &lt;player&gt; title|line|reset
 * /scoreboard group &lt;group&gt; title|line|reset
 * </pre>
 */
public class ScoreboardCommand {

    private static final String PERM_ADMIN = "neoessentials.scoreboard.admin";
    private static final String PERM_TOGGLE = "neoessentials.scoreboard.toggle";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.isScoreboardModuleEnabled()) return;
        if (!ConfigManager.getInstance().isCommandEnabled("scoreboard")) return;

        dispatcher.register(Commands.literal("scoreboard")
            .executes(ctx -> { showHelp(ctx.getSource()); return 1; })

            // ── /scoreboard toggle ────────────────────────────────────────────
            .then(Commands.literal("toggle")
                .requires(src -> {
                    var p = src.getPlayer();
                    return p != null && PermissionAPI.hasPermission(p.getUUID(), PERM_TOGGLE);
                })
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayer();
                    if (player == null) {
                        ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.general.player_only"));
                        return 0;
                    }
                    boolean newState = !ScoreboardToggleManager.getInstance().isEnabled(player.getUUID());
                    ScoreboardToggleManager.getInstance().setEnabled(player.getUUID(), newState);
                    var server = ServerLifecycleHooks.getCurrentServer();
                    if (server != null) ScoreboardManager.getInstance().updatePlayer(player, server);
                    ctx.getSource().sendSuccess(() -> MessageUtil.component(
                        newState ? "commands.neoessentials.scoreboard.toggle_on" : "commands.neoessentials.scoreboard.toggle_off"
                    ), false);
                    return 1;
                })
            )

            // ── Admin subtree ─────────────────────────────────────────────────
            .then(Commands.literal("reload")
                .requires(adminCheck())
                .executes(ctx -> {
                    // See the identical fix in /tablist reload's handler — clearCache() MUST
                    // run before loadConfig(), otherwise the on-disk scoreboard.json edit is
                    // never actually re-read and this command silently no-ops.
                    ConfigManager.getInstance().clearCache();
                    ScoreboardManager.getInstance().loadConfig();
                    var server = ServerLifecycleHooks.getCurrentServer();
                    if (server != null) ScoreboardManager.getInstance().updateAll(server);
                    ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.scoreboard.reloaded"), false);
                    return 1;
                })
            )
            .then(Commands.literal("enable")
                .requires(adminCheck())
                .executes(ctx -> {
                    ScoreboardManager.getInstance().setEnabled(true);
                    var server = ServerLifecycleHooks.getCurrentServer();
                    if (server != null) ScoreboardManager.getInstance().updateAll(server);
                    ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.scoreboard.enabled"), false);
                    return 1;
                })
            )
            .then(Commands.literal("disable")
                .requires(adminCheck())
                .executes(ctx -> {
                    ScoreboardManager.getInstance().setEnabled(false);
                    var server = ServerLifecycleHooks.getCurrentServer();
                    if (server != null) ScoreboardManager.getInstance().hideAll(server);
                    ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.scoreboard.disabled"), false);
                    return 1;
                })
            )
            .then(Commands.literal("info")
                .requires(adminCheck())
                .executes(ctx -> {
                    ScoreboardManager mgr = ScoreboardManager.getInstance();
                    final String fStatus = mgr.isEnabled() ? "aEnabled" : "cDisabled";
                    String boards = mgr.getBoardNames().isEmpty() ? "§7(none)" : "§e" + String.join(", ", mgr.getBoardNames());
                    ctx.getSource().sendSuccess(() -> MessageUtil.component("commands.neoessentials.scoreboard.info",
                        fStatus, mgr.getBoardCount(), mgr.getRefreshIntervalTicks(), boards
                    ), false);
                    return 1;
                })
            )
            .then(Commands.literal("preview")
                .requires(adminCheck())
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayer();
                    if (player == null) {
                        ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.general.player_only"));
                        return 0;
                    }
                    var server = player.getServer();
                    if (server != null) ScoreboardManager.getInstance().updatePlayer(player, server);
                    ctx.getSource().sendSuccess(() -> MessageUtil.component("commands.neoessentials.scoreboard.preview_active"), false);
                    return 1;
                })
            )
            .then(Commands.literal("board")
                .requires(adminCheck())
                .then(Commands.literal("list")
                    .executes(ctx -> {
                        var names = ScoreboardManager.getInstance().getBoardNames();
                        String msg = names.isEmpty() ? "§7(none)" : "§e" + String.join("§7, §e", names);
                        ctx.getSource().sendSuccess(() -> MessageUtil.component("commands.neoessentials.scoreboard.board_list", msg), false);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("set")
                .requires(adminCheck())
                .then(Commands.literal("title")
                    .then(Commands.argument("board", StringArgumentType.word())
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String boardName = StringArgumentType.getString(ctx, "board");
                                String text = StringArgumentType.getString(ctx, "text");
                                if (!setBoardTitle(ctx.getSource(), boardName, text)) return 0;
                                ctx.getSource().sendSuccess(() -> MessageUtil.component("commands.neoessentials.scoreboard.title_set", boardName), false);
                                return 1;
                            })
                        )
                    )
                )
                .then(Commands.literal("line")
                    .then(Commands.argument("board", StringArgumentType.word())
                        .then(Commands.argument("index", IntegerArgumentType.integer(0, 14))
                            .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String boardName = StringArgumentType.getString(ctx, "board");
                                    int index = IntegerArgumentType.getInteger(ctx, "index");
                                    String text = StringArgumentType.getString(ctx, "text");
                                    ScoreboardBoard board = ScoreboardManager.getInstance().findBoard(boardName);
                                    if (board == null) {
                                        ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.scoreboard.board_not_found", boardName));
                                        return 0;
                                    }
                                    if (index >= board.getLines().size()) {
                                        ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.scoreboard.line_index_invalid", index, board.getLines().size()));
                                        return 0;
                                    }
                                    board.getLines().set(index, new ScoreboardLine(java.util.List.of(text), board.getLines().get(index).getCondition()));
                                    var server = ServerLifecycleHooks.getCurrentServer();
                                    if (server != null) ScoreboardManager.getInstance().updateAll(server);
                                    ctx.getSource().sendSuccess(() -> MessageUtil.component("commands.neoessentials.scoreboard.line_set", index, boardName), false);
                                    return 1;
                                })
                            )
                        )
                    )
                )
            )
            .then(Commands.literal("player")
                .requires(adminCheck())
                .then(Commands.argument("target", EntityArgument.player())
                    .then(Commands.literal("title")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                String text = StringArgumentType.getString(ctx, "text");
                                ScoreboardManager.getInstance().setPlayerTitleOverride(target.getUUID(), text);
                                var server = ServerLifecycleHooks.getCurrentServer();
                                if (server != null) ScoreboardManager.getInstance().updatePlayer(target, server);
                                ctx.getSource().sendSuccess(() -> MessageUtil.component(
                                    "commands.neoessentials.scoreboard.player_title_set", target.getName().getString()
                                ), false);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("line")
                        .then(Commands.argument("index", IntegerArgumentType.integer(0, 14))
                            .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                    int index = IntegerArgumentType.getInteger(ctx, "index");
                                    String text = StringArgumentType.getString(ctx, "text");
                                    ScoreboardManager.getInstance().setPlayerLineOverride(target.getUUID(), index, text);
                                    var server = ServerLifecycleHooks.getCurrentServer();
                                    if (server != null) ScoreboardManager.getInstance().updatePlayer(target, server);
                                    ctx.getSource().sendSuccess(() -> MessageUtil.component(
                                        "commands.neoessentials.scoreboard.player_line_set", index, target.getName().getString()
                                    ), false);
                                    return 1;
                                })
                            )
                        )
                    )
                    .then(Commands.literal("reset")
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                            ScoreboardManager.getInstance().clearPlayerOverrides(target.getUUID());
                            var server = ServerLifecycleHooks.getCurrentServer();
                            if (server != null) ScoreboardManager.getInstance().updatePlayer(target, server);
                            ctx.getSource().sendSuccess(() -> MessageUtil.component(
                                "commands.neoessentials.scoreboard.player_reset", target.getName().getString()
                            ), false);
                            return 1;
                        })
                    )
                )
            )
            .then(Commands.literal("group")
                .requires(adminCheck())
                .then(Commands.argument("group", StringArgumentType.word())
                    .then(Commands.literal("title")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String group = StringArgumentType.getString(ctx, "group");
                                String text = StringArgumentType.getString(ctx, "text");
                                ScoreboardManager.getInstance().setGroupTitleOverride(group, text);
                                var server = ServerLifecycleHooks.getCurrentServer();
                                if (server != null) ScoreboardManager.getInstance().updateAll(server);
                                ctx.getSource().sendSuccess(() -> MessageUtil.component(
                                    "commands.neoessentials.scoreboard.group_title_set", group
                                ), false);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("line")
                        .then(Commands.argument("index", IntegerArgumentType.integer(0, 14))
                            .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String group = StringArgumentType.getString(ctx, "group");
                                    int index = IntegerArgumentType.getInteger(ctx, "index");
                                    String text = StringArgumentType.getString(ctx, "text");
                                    ScoreboardManager.getInstance().setGroupLineOverride(group, index, text);
                                    var server = ServerLifecycleHooks.getCurrentServer();
                                    if (server != null) ScoreboardManager.getInstance().updateAll(server);
                                    ctx.getSource().sendSuccess(() -> MessageUtil.component(
                                        "commands.neoessentials.scoreboard.group_line_set", index, group
                                    ), false);
                                    return 1;
                                })
                            )
                        )
                    )
                    .then(Commands.literal("reset")
                        .executes(ctx -> {
                            String group = StringArgumentType.getString(ctx, "group");
                            ScoreboardManager.getInstance().clearGroupOverrides(group);
                            var server = ServerLifecycleHooks.getCurrentServer();
                            if (server != null) ScoreboardManager.getInstance().updateAll(server);
                            ctx.getSource().sendSuccess(() -> MessageUtil.component(
                                "commands.neoessentials.scoreboard.group_reset", group
                            ), false);
                            return 1;
                        })
                    )
                )
            )
        );
    }

    private static java.util.function.Predicate<CommandSourceStack> adminCheck() {
        return src -> {
            var p = src.getPlayer();
            return p == null || PermissionAPI.hasPermission(p.getUUID(), PERM_ADMIN);
        };
    }

    private static boolean setBoardTitle(CommandSourceStack source, String boardName, String text) {
        ScoreboardBoard board = ScoreboardManager.getInstance().findBoard(boardName);
        if (board == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.scoreboard.board_not_found", boardName));
            return false;
        }
        // Animated multi-frame titles stay config-file-only in v1 — this replaces the whole
        // frame list with a single static frame.
        board.setTitleFrames(java.util.List.of(text));
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) ScoreboardManager.getInstance().updateAll(server);
        return true;
    }

    private static void showHelp(CommandSourceStack src) {
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.scoreboard.help"), false);
    }
}
