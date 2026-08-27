package com.zerog.neoessentials.leaderboard.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.leaderboard.LeaderboardCache;
import com.zerog.neoessentials.leaderboard.LeaderboardManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;

/**
 * /leaderboard [board] [page] — generalized ranked-stat command; /baltop keeps working
 * unchanged and independently (it isn't rewired through this — see the leaderboard plan's
 * note on keeping the existing, already-trusted /baltop path untouched).
 */
public class LeaderboardCommand {
    private static final String PERM_VIEW = "neoessentials.leaderboard.view";
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
                            IntegerArgumentType.getInteger(ctx, "page")))))
            );
        }
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
        String displayName = cache.getDefinition().displayName();

        source.sendSuccess(() -> MessageUtil.success(
            "commands.neoessentials.leaderboard.header", displayName, clampedPage, totalPages, ageSeconds), false);

        int startRank = (clampedPage - 1) * PAGE_SIZE + 1;
        int rank = startRank;
        for (var entry : pageEntries) {
            final int r = rank++;
            String formatted = cache.getProvider().formatValue(entry.value());
            source.sendSuccess(() -> MessageUtil.info(
                "commands.neoessentials.leaderboard.entry", r, entry.name(), formatted), false);
        }

        if (cache.isBuilding()) {
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.leaderboard.refreshing"), false);
        }
        return 1;
    }
}
