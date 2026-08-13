package com.zerog.neoessentials.moderation.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.moderation.BanManager;
import com.zerog.neoessentials.moderation.KickManager;
import com.zerog.neoessentials.chat.MuteManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.moderation.WarnEntry;
import com.zerog.neoessentials.moderation.WarnManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * /modhistory <player> — staff command that shows a player's full moderation record
 * (bans, mutes, kicks, warns — active and past) in one place, in-game. Mirrors exactly what
 * the dashboard's public player-lookup page shows via
 * {@link com.zerog.neoessentials.webdashboard.endpoints.PublicModerationEndpoint}, so staff
 * don't need to alt-tab to the dashboard just to see whether someone's been in trouble before.
 */
public class ModHistoryCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModHistoryCommand.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault());
    private static final int ENTRIES_PER_SECTION = 5;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!com.zerog.neoessentials.config.ConfigManager.isModerationEnabled()) {
            LOGGER.debug("Moderation module is disabled, skipping modhistory command registration");
            return;
        }
        var cfg = com.zerog.neoessentials.config.ConfigManager.getInstance();
        if (!cfg.isCommandEnabled("modhistory")) return;

        dispatcher.register(Commands.literal("modhistory")
            .requires(src -> PermissionValidator.validatePermission(src, "neoessentials.moderation.history").hasPermission())
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    ctx.getSource().getServer().getPlayerNames(), b))
                .executes(ctx -> executeHistory(ctx, StringArgumentType.getString(ctx, "player")))
            )
        );
        dispatcher.register(Commands.literal("history").redirect(dispatcher.getRoot().getChild("modhistory")));
    }

    private static int executeHistory(CommandContext<CommandSourceStack> ctx, String playerName) {
        CommandSourceStack source = ctx.getSource();
        UUID playerId = resolvePlayerUUID(ctx, playerName);

        if (playerId == null) {
            source.sendFailure(MessageUtil.error("neoessentials.moderation.player_not_found", playerName));
            return 0;
        }

        List<BanManager.BanEntry> bans = BanManager.getInstance().getBanHistory(playerId);
        List<MuteManager.MuteEntry> mutes = MuteManager.getMuteHistory(playerName);
        List<KickManager.KickEntry> kicks = KickManager.getInstance().getKickHistory(playerId);
        List<WarnEntry> warns = WarnManager.getInstance().getWarnings(playerId);

        int total = bans.size() + mutes.size() + kicks.size() + warns.size();
        source.sendSuccess(() -> Component.literal("§8[§bNE§8] §r§fModeration history for §e" + playerName
            + "§f (§7" + total + " total record" + (total == 1 ? "" : "s") + "§f):"), false);

        if (total == 0) {
            source.sendSuccess(() -> Component.literal("§7  Clean record — nothing on file."), false);
            return 1;
        }

        printSection(source, "Bans", bans, b ->
            String.format("%s §7by §f%s§7 — %s §7(%s%s)",
                b.active ? "§cACTIVE" : "§8expired", b.bannedBy, b.reason,
                TIME_FORMAT.format(Instant.ofEpochMilli(b.banTime)),
                b.expireTime == 0 ? ", permanent" : ""));

        printSection(source, "Mutes", mutes, m ->
            String.format("%s §7by §f%s§7 — %s §7(%s%s)",
                m.active ? "§cACTIVE" : "§8expired", m.mutedBy, m.reason,
                TIME_FORMAT.format(Instant.ofEpochMilli(m.muteTime)),
                m.expireTime == 0 ? ", permanent" : ""));

        printSection(source, "Kicks", kicks, k ->
            String.format("§7by §f%s§7 — %s §7(%s)",
                k.kickedBy, k.reason, TIME_FORMAT.format(Instant.ofEpochMilli(k.kickTime))));

        printSection(source, "Warns", warns, w ->
            String.format("§7by §f%s§7 — %s §7(%s)",
                w.getWarnedBy(), w.getReason(), TIME_FORMAT.format(Instant.ofEpochMilli(w.getTimestamp()))));

        return 1;
    }

    private static <T> void printSection(CommandSourceStack source, String label, List<T> entries, java.util.function.Function<T, String> formatter) {
        if (entries.isEmpty()) return;
        source.sendSuccess(() -> Component.literal("§7  " + label + " (§f" + entries.size() + "§7):"), false);
        int shown = Math.min(entries.size(), ENTRIES_PER_SECTION);
        for (int i = 0; i < shown; i++) {
            String line = formatter.apply(entries.get(i));
            source.sendSuccess(() -> Component.literal("§7    - " + line), false);
        }
        if (entries.size() > shown) {
            int more = entries.size() - shown;
            source.sendSuccess(() -> Component.literal("§7    ... and " + more + " more"), false);
        }
    }

    /** Resolves a player UUID by name — online first, then the server's profile cache
     *  (anyone who has ever joined), matching {@link com.zerog.neoessentials.webdashboard.endpoints.PublicModerationEndpoint}'s
     *  own resolution so in-game and dashboard lookups always agree on the same player. */
    private static UUID resolvePlayerUUID(CommandContext<CommandSourceStack> ctx, String playerName) {
        ServerPlayer online = ctx.getSource().getServer().getPlayerList().getPlayerByName(playerName);
        if (online != null) return online.getUUID();
        try {
            var profile = ctx.getSource().getServer().services().nameToIdCache().get(playerName);
            if (profile.isPresent()) return profile.get().id();
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.MODERATION, "Profile cache lookup failed for player name {}", playerName, e);
        }
        return null;
    }
}
