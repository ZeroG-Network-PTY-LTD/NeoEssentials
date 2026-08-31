package com.zerog.neoessentials.votifier.commands;

import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.votifier.PlayerVoteEvent;
import com.zerog.neoessentials.votifier.Vote;
import com.zerog.neoessentials.votifier.VoteBroadcastToggle;
import com.zerog.neoessentials.votifier.VotePartyManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

public class VotifierCommands {
    private static final String PERM_VOTE = "neoessentials.votifier.vote";
    private static final String PERM_ADMIN = "neoessentials.votifier.admin";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.isVotifierModuleEnabled()) return;

        dispatcher.register(Commands.literal("vote")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), PERM_VOTE);
            })
            .executes(VotifierCommands::showVoteLinks)
        );

        dispatcher.register(Commands.literal("votes")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), PERM_VOTE);
            })
            .executes(ctx -> showVotes(ctx.getSource(), null))
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                .executes(ctx -> showVotes(ctx.getSource(), StringArgumentType.getString(ctx, "player"))))
        );

        dispatcher.register(Commands.literal("togglevotebroadcast")
            .requires(src -> src.getPlayer() != null)
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayer();
                boolean nowOptedOut = VoteBroadcastToggle.toggle(player.getUUID());
                ctx.getSource().sendSuccess(() -> MessageUtil.success(nowOptedOut
                    ? "commands.neoessentials.votifier.broadcast_disabled"
                    : "commands.neoessentials.votifier.broadcast_enabled"), false);
                return 1;
            })
        );

        dispatcher.register(Commands.literal("voteparty")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), PERM_VOTE);
            })
            .executes(ctx -> {
                int progress = VotePartyManager.getInstance().getProgress();
                int required = VotePartyManager.getInstance().getRequired();
                ctx.getSource().sendSuccess(() -> MessageUtil.info(
                    "commands.neoessentials.votifier.party_progress", progress, required), false);
                return 1;
            })
        );

        dispatcher.register(Commands.literal("votifier")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), PERM_ADMIN);
            })
            .then(Commands.literal("reload")
                .executes(ctx -> {
                    ConfigManager.getInstance().clearCache();
                    ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.votifier.reloaded"), false);
                    return 1;
                }))
            .then(Commands.literal("genkeys")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> MessageUtil.info(
                        "commands.neoessentials.votifier.genkeys_hint"), false);
                    return 1;
                }))
            .then(Commands.literal("testvote")
                .then(Commands.argument("site", StringArgumentType.word())
                    .executes(ctx -> testVote(ctx.getSource(),
                        StringArgumentType.getString(ctx, "site"),
                        ctx.getSource().getTextName()))
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                        .executes(ctx -> testVote(ctx.getSource(),
                            StringArgumentType.getString(ctx, "site"),
                            StringArgumentType.getString(ctx, "player"))))))
        );
    }

    private static int showVoteLinks(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        return showVoteLinks(ctx.getSource());
    }

    private static int showVoteLinks(CommandSourceStack source) {
        JsonObject votifier = getVotifierConfig();
        if (votifier == null || !votifier.has("voteLinks") || !votifier.getAsJsonObject("voteLinks").entrySet().iterator().hasNext()) {
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.votifier.no_links"), false);
            return 1;
        }
        JsonObject links = votifier.getAsJsonObject("voteLinks");
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.votifier.links_header"), false);
        for (var entry : links.entrySet()) {
            String line = "§e" + entry.getKey() + "§7: §f" + entry.getValue().getAsString();
            source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(line), false);
        }
        return 1;
    }

    private static int showVotes(CommandSourceStack source, String playerName) {
        String target = playerName != null ? playerName : source.getTextName();
        JsonObject record = com.zerog.neoessentials.storage.StorageManager.getInstance()
            .getStore().get("votifier_stats", target.toLowerCase());
        long total = record != null && record.has("total") ? record.get("total").getAsLong() : 0L;
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.votifier.total_votes", target, total), false);
        return 1;
    }

    private static int testVote(CommandSourceStack source, String site, String playerName) {
        Vote vote = new Vote(site, playerName, "127.0.0.1", String.valueOf(System.currentTimeMillis()));
        NeoForge.EVENT_BUS.post(new PlayerVoteEvent(vote));
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.votifier.testvote_sent", site, playerName), false);
        return 1;
    }

    private static JsonObject getVotifierConfig() {
        try {
            JsonObject root = ConfigManager.getInstance().getConfig(ConfigManager.VOTIFIER_CONFIG);
            return root.has("votifier") ? root.getAsJsonObject("votifier") : null;
        } catch (Exception e) {
            return null;
        }
    }
}
