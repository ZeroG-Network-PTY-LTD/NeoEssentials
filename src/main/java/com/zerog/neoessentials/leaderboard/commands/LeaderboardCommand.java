package com.zerog.neoessentials.leaderboard.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.leaderboard.LeaderboardCache;
import com.zerog.neoessentials.leaderboard.LeaderboardManager;
import com.zerog.neoessentials.leaderboard.config.LeaderboardConfigLoader;
import com.zerog.neoessentials.hologram.HologramData;
import com.zerog.neoessentials.hologram.HologramLine;
import com.zerog.neoessentials.hologram.HologramManager;
import com.zerog.neoessentials.hologram.HologramRenderer;
import com.zerog.neoessentials.util.LevelCompat;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

/**
 * /leaderboard [board] [page] — generalized ranked-stat command; /baltop keeps working
 * unchanged and independently (it isn't rewired through this — see the leaderboard plan's
 * note on keeping the existing, already-trusted /baltop path untouched).
 */
public class LeaderboardCommand {
    private static final String PERM_VIEW = "neoessentials.leaderboard.view";
    private static final String PERM_ADMIN = "neoessentials.leaderboard.admin";
    private static final int PAGE_SIZE = 10;

    private static final SuggestionProvider<CommandSourceStack> BOARD_SUGGESTIONS = (ctx, builder) ->
        SharedSuggestionProvider.suggest(LeaderboardManager.getInstance().getRegisteredBoardIds(), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String name : new String[]{"leaderboard", "lb"}) {
            dispatcher.register(Commands.literal(name)
                .requires(src -> {
                    var p = src.getPlayer();
                    return p == null || PermissionAPI.hasPermission(p.getUUID(), PERM_VIEW);
                })
                .executes(ctx -> listBoards(ctx.getSource()))
                .then(Commands.argument("board", StringArgumentType.word())
                    .suggests(BOARD_SUGGESTIONS)
                    .executes(ctx -> show(ctx.getSource(), StringArgumentType.getString(ctx, "board"), 1))
                    .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> show(ctx.getSource(), StringArgumentType.getString(ctx, "board"),
                            IntegerArgumentType.getInteger(ctx, "page"))))
                    .then(Commands.literal("gui")
                        .executes(ctx -> openGui(ctx.getSource(), StringArgumentType.getString(ctx, "board")))))

                .then(Commands.literal("reload")
                    .requires(adminCheck())
                    .executes(ctx -> {
                        ConfigManager.getInstance().clearCache();
                        LeaderboardConfigLoader.load();
                        ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.leaderboard.reloaded"), false);
                        return 1;
                    })
                )

                .then(Commands.literal("admin")
                    .requires(adminCheck())
                    .then(Commands.literal("set")
                        .then(Commands.argument("board", StringArgumentType.word()).suggests(BOARD_SUGGESTIONS)
                            .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                                .then(Commands.argument("value", LongArgumentType.longArg())
                                    .executes(ctx -> adminSet(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "board"),
                                        StringArgumentType.getString(ctx, "player"),
                                        LongArgumentType.getLong(ctx, "value")))))))
                    .then(Commands.literal("add")
                        .then(Commands.argument("board", StringArgumentType.word()).suggests(BOARD_SUGGESTIONS)
                            .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                                .then(Commands.argument("delta", LongArgumentType.longArg())
                                    .executes(ctx -> adminAdd(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "board"),
                                        StringArgumentType.getString(ctx, "player"),
                                        LongArgumentType.getLong(ctx, "delta")))))))
                    .then(Commands.literal("reset")
                        .then(Commands.argument("board", StringArgumentType.word()).suggests(BOARD_SUGGESTIONS)
                            .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                                .executes(ctx -> adminSet(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "board"),
                                    StringArgumentType.getString(ctx, "player"), 0L)))))
                    .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                            .then(Commands.argument("displayName", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "id");
                                    if (LeaderboardManager.getInstance().getBoard(id) != null) {
                                        ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.leaderboard.board_exists", id));
                                        return 0;
                                    }
                                    String displayName = StringArgumentType.getString(ctx, "displayName");
                                    LeaderboardConfigLoader.addCustomBoard(id, displayName);
                                    ctx.getSource().sendSuccess(() -> MessageUtil.component("commands.neoessentials.leaderboard.board_created", id), false);
                                    return 1;
                                })))
                    )
                    .then(Commands.literal("delete")
                        .then(Commands.argument("board", StringArgumentType.word()).suggests(BOARD_SUGGESTIONS)
                            .executes(ctx -> {
                                String id = StringArgumentType.getString(ctx, "board");
                                if (!LeaderboardConfigLoader.deleteCustomBoard(id)) {
                                    ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.leaderboard.not_custom_board", id));
                                    return 0;
                                }
                                ctx.getSource().sendSuccess(() -> MessageUtil.component("commands.neoessentials.leaderboard.board_deleted", id), false);
                                return 1;
                            })))
                )

                // ── /leaderboard hologram create <board> <id> [lines] ─────────────
                // Convenience generator: holograms already resolve {placeholder} tokens live
                // (HologramTextProcessor -> PlaceholderManager, refreshed on a per-hologram
                // interval by HologramScheduler), so {leaderboard_<board>:<rank>:name|value}
                // just works — this only saves typing N `/hologram addline` commands by hand.
                .then(Commands.literal("hologram")
                    .requires(adminCheck())
                    .then(Commands.literal("create")
                        .then(Commands.argument("board", StringArgumentType.word()).suggests(BOARD_SUGGESTIONS)
                            .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> createHologram(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "board"),
                                    StringArgumentType.getString(ctx, "id"), 10))
                                .then(Commands.argument("lines", IntegerArgumentType.integer(1, 15))
                                    .executes(ctx -> createHologram(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "board"),
                                        StringArgumentType.getString(ctx, "id"),
                                        IntegerArgumentType.getInteger(ctx, "lines")))))))
                )
            );
        }
    }

    private static int createHologram(CommandSourceStack source, String boardId, String hologramId, int lineCount) {
        if (!ConfigManager.isHologramModuleEnabled()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.leaderboard.holograms_disabled"));
            return 0;
        }
        LeaderboardCache cache = LeaderboardManager.getInstance().getBoard(boardId);
        if (cache == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.leaderboard.board_not_found", boardId));
            return 0;
        }
        if (HologramManager.getInstance().exists(hologramId)) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.leaderboard.hologram_exists", hologramId));
            return 0;
        }
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.hologram.player_only"));
            return 0;
        }

        HologramData data = new HologramData();
        data.id = hologramId.toLowerCase();
        data.x = player.getX();
        data.y = player.getY() + 1.5;
        data.z = player.getZ();
        data.world = HologramRenderer.dimensionKey(LevelCompat.of(player));
        data.refreshInterval = 5;

        data.lines.add(new HologramLine("&6&l" + cache.getDefinition().displayName() + " Leaderboard"));
        for (int rank = 1; rank <= lineCount; rank++) {
            data.lines.add(new HologramLine(
                "&e#" + rank + " &f{leaderboard_" + boardId + ":" + rank + ":name} &7- &a{leaderboard_" + boardId + ":" + rank + ":value}"));
        }

        HologramManager.getInstance().registerHologram(data);
        ServerLevel level = LevelCompat.of(player);
        HologramRenderer.spawn(data, level);

        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.leaderboard.hologram_created", hologramId, boardId, lineCount), true);
        return 1;
    }

    private static java.util.function.Predicate<CommandSourceStack> adminCheck() {
        return src -> {
            var p = src.getPlayer();
            return p == null || PermissionAPI.hasPermission(p.getUUID(), PERM_ADMIN);
        };
    }

    private static int openGui(CommandSourceStack source, String boardId) {
        LeaderboardCache cache = LeaderboardManager.getInstance().getBoard(boardId);
        if (cache == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.leaderboard.board_not_found", boardId));
            return 0;
        }
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only"));
            return 0;
        }
        com.zerog.neoessentials.leaderboard.gui.LeaderboardMenu.open(player, boardId);
        return 1;
    }

    private static int listBoards(CommandSourceStack source) {
        var ids = LeaderboardManager.getInstance().getRegisteredBoardIds();
        String msg = ids.isEmpty() ? "§7(none)" : "§e" + String.join("§7, §e", ids);
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.leaderboard.board_list", msg), false);
        return 1;
    }

    private static int show(CommandSourceStack source, String boardId, int page) {
        LeaderboardCache cache = LeaderboardManager.getInstance().getBoard(boardId);
        if (cache == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.leaderboard.board_not_found", boardId));
            return 0;
        }

        var pageEntries = cache.getPage(source.getServer(), page, PAGE_SIZE);
        if (pageEntries.isEmpty()) {
            // Same behavior as the original /baltop: an empty cache reads as "no entries yet"
            // even on the very first call (where the async rebuild just kicked off and hasn't
            // finished) — a re-run a moment later shows the real data once the build completes.
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.leaderboard.empty"), false);
            return 1;
        }

        int totalPages = cache.getTotalPages(PAGE_SIZE);
        int clampedPage = Math.max(1, Math.min(page, totalPages));
        long ageSeconds = cache.getCacheAgeMs() / 1000L;
        var definition = cache.getDefinition();
        String displayName = definition.displayName();

        String headerFormat = definition.headerFormat();
        if (headerFormat != null) {
            String rendered = headerFormat
                .replace("{displayName}", displayName)
                .replace("{page}", String.valueOf(clampedPage))
                .replace("{totalPages}", String.valueOf(totalPages))
                .replace("{age}", String.valueOf(ageSeconds));
            source.sendSuccess(() -> com.zerog.neoessentials.chat.RichTextFormatter.processTablistText(rendered), false);
        } else {
            source.sendSuccess(() -> MessageUtil.success(
                "commands.neoessentials.leaderboard.header", displayName, clampedPage, totalPages, ageSeconds), false);
        }

        String entryFormat = definition.entryFormat();
        int startRank = (clampedPage - 1) * PAGE_SIZE + 1;
        int rank = startRank;
        for (var entry : pageEntries) {
            final int r = rank++;
            String formatted = cache.getProvider().formatValue(entry.value());
            if (entryFormat != null) {
                String rendered = entryFormat
                    .replace("{rank}", String.valueOf(r))
                    .replace("{name}", entry.name())
                    .replace("{value}", formatted)
                    .replace("{medal}", com.zerog.neoessentials.leaderboard.LeaderboardStyle.medal(r))
                    .replace("{rankColor}", com.zerog.neoessentials.leaderboard.LeaderboardStyle.rankColorTag(r));
                source.sendSuccess(() -> com.zerog.neoessentials.chat.RichTextFormatter.processTablistText(rendered), false);
            } else {
                source.sendSuccess(() -> MessageUtil.info(
                    "commands.neoessentials.leaderboard.entry", r, entry.name(), formatted), false);
            }
        }

        if (cache.isBuilding()) {
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.leaderboard.refreshing"), false);
        }
        return 1;
    }

    // ── Admin: custom-board value editing ────────────────────────────────────
    private static int adminSet(CommandSourceStack source, String boardId, String playerName, long value) {
        return withCustomBoardAndPlayer(source, boardId, playerName, (server, uuid) -> {
            LeaderboardConfigLoader.customStats().set(boardId, uuid, value);
            LeaderboardManager.getInstance().getBoard(boardId).invalidate();
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.leaderboard.value_set", boardId, playerName, value), false);
        });
    }

    private static int adminAdd(CommandSourceStack source, String boardId, String playerName, long delta) {
        return withCustomBoardAndPlayer(source, boardId, playerName, (server, uuid) -> {
            LeaderboardConfigLoader.customStats().add(boardId, uuid, delta);
            LeaderboardManager.getInstance().getBoard(boardId).invalidate();
            long newValue = LeaderboardConfigLoader.customStats().get(boardId, uuid);
            source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.leaderboard.value_set", boardId, playerName, newValue), false);
        });
    }

    private interface PlayerAction {
        void run(MinecraftServer server, UUID uuid);
    }

    private static int withCustomBoardAndPlayer(CommandSourceStack source, String boardId, String playerName, PlayerAction action) {
        if (!LeaderboardConfigLoader.isCustomBoard(boardId)) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.leaderboard.not_custom_board", boardId));
            return 0;
        }
        MinecraftServer server = source.getServer();
        Optional<UUID> uuid = resolveUuid(server, playerName);
        if (uuid.isEmpty()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", playerName));
            return 0;
        }
        action.run(server, uuid.get());
        return 1;
    }

    private static Optional<UUID> resolveUuid(MinecraftServer server, String name) {
        var online = server.getPlayerList().getPlayerByName(name);
        if (online != null) return Optional.of(online.getUUID());
        try {
            return server.services().nameToIdCache().get(name).map(p -> p.id());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
