 package com.zerog.neoessentials.tablist;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * /tablist — Admin command for the NeoEssentials BungeeTabListPlus-inspired tablist.
 *
 * <h2>Sub-commands</h2>
 * <pre>
 * /tablist reload                          — reload tablist config and push to all players
 * /tablist enable                          — enable the tablist system
 * /tablist disable                         — disable the tablist system (revert to vanilla)
 * /tablist preview                         — show your own current header/footer
 * /tablist info                            — show current tablist config status
 * /tablist set header &lt;text&gt;               — set a single-frame global header in-game
 * /tablist set footer &lt;text&gt;               — set a single-frame global footer in-game
 * /tablist player &lt;player&gt; header &lt;text&gt;   — per-player header override
 * /tablist player &lt;player&gt; footer &lt;text&gt;   — per-player footer override
 * /tablist player &lt;player&gt; reset           — clear per-player overrides
 * /tablist group &lt;group&gt; header &lt;text&gt;     — per-group header override
 * /tablist group &lt;group&gt; footer &lt;text&gt;     — per-group footer override
 * /tablist group &lt;group&gt; reset             — clear per-group overrides
 *
 * BungeeTabListPlus-style commands:
 * /tablist proxy status                    — show proxy integration status
 * /tablist proxy setserver &lt;count&gt;         — manually set a server's player count
 * /tablist fakeplayer list                 — list configured fake-player entries
 * /tablist fakeplayer add &lt;id&gt; &lt;display&gt;  — add a runtime fake player entry
 * /tablist fakeplayer remove &lt;id&gt;         — remove a fake player entry by id
 * /tablist fakeplayer refresh              — re-inject all fake entries for all players
 * /tablist layout info                     — show current layout configuration
 * /tablist layout sort [on|off]            — toggle group-weight sorting
 * /tablist independent [on|off]            — show or toggle independent mode
 * </pre>
 */
public class TablistCommand {

    private static final String PERM_ADMIN = "neoessentials.tablist.admin";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("tablist")) return;

        dispatcher.register(Commands.literal("tablist")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), PERM_ADMIN);
            })
            .executes(ctx -> { showHelp(ctx.getSource()); return 1; })

            // ── /tablist reload ───────────────────────────────────────────────
            .then(Commands.literal("reload")
                .executes(ctx -> {
                    TablistManager.getInstance().loadConfig();
                    var server = ServerLifecycleHooks.getCurrentServer();
                    if (server != null) TablistManager.getInstance().updateAll(server);
                    ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.tablist.reloaded"), false);
                    return 1;
                })
            )

            // ── /tablist enable ───────────────────────────────────────────────
            .then(Commands.literal("enable")
                .executes(ctx -> {
                    TablistManager.getInstance().setEnabled(true);
                    var server = ServerLifecycleHooks.getCurrentServer();
                    if (server != null) TablistManager.getInstance().updateAll(server);
                    ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.tablist.enabled"), false);
                    return 1;
                })
            )

            // ── /tablist disable ──────────────────────────────────────────────
            .then(Commands.literal("disable")
                .executes(ctx -> {
                    TablistManager.getInstance().setEnabled(false);
                    var server = ServerLifecycleHooks.getCurrentServer();
                    if (server != null) {
                        var emptyPacket = new net.minecraft.network.protocol.game.ClientboundTabListPacket(
                            Component.empty(), Component.empty());
                        for (var p : server.getPlayerList().getPlayers()) p.connection.send(emptyPacket);
                    }
                    ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.tablist.disabled"), false);
                    return 1;
                })
            )

            // ── /tablist preview ──────────────────────────────────────────────
            .then(Commands.literal("preview")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayer();
                    if (player == null) {
                        ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.general.player_only"));
                        return 0;
                    }
                    var server = player.getServer();
                    if (server != null) TablistManager.getInstance().updatePlayer(player, server);
                    ctx.getSource().sendSuccess(() -> Component.literal("§aPreviewing your tablist header/footer."), false);
                    return 1;
                })
            )

            // ── /tablist info ─────────────────────────────────────────────────
            .then(Commands.literal("info")
                .executes(ctx -> {
                    TablistManager mgr = TablistManager.getInstance();
                    ProxyIntegration proxy = ProxyIntegration.getInstance();
                    TablistLayout layout = TablistLayout.getInstance();
                    boolean enabled = mgr.isEnabled();
                    String groups = mgr.getGroupsWithOverrides().isEmpty()
                        ? "§7(none)" : "§e" + String.join(", ", mgr.getGroupsWithOverrides());
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        "§6§lNeoEssentials Tablist (BTLP-style) §8—\n" +
                        "§7Status: §" + (enabled ? "aEnabled" : "cDisabled") +
                        "  §7Mode: §e" + (mgr.isIndependentMode() ? "Independent" : "Proxy-managed") + "\n" +
                        "§7Header frames: §e" + mgr.getHeaderFrameCount() +
                        " §7| Footer frames: §e" + mgr.getFooterFrameCount() + "\n" +
                        "§7Refresh: §e" + mgr.getRefreshIntervalTicks() + "§7t " +
                        "§8| §7Sort by group: §e" + layout.isSortByGroupWeight() + "\n" +
                        "§7Columns: §e" + layout.getColumns() +
                        " §8| §7PlayersByServer: §e" + layout.isPlayersByServer() + "\n" +
                        "§7Fake entries: §e" + FakePlayerManager.getInstance().getCount() +
                        "  §7Hide vanished: §e" + mgr.isHideVanished() + "\n" +
                        "§7Proxy: §" + (proxy.isProxyEnabled() ? "a" : "7") +
                        (proxy.isProxyEnabled()
                            ? "enabled (detected=" + proxy.isProxyDetected() + ", network=" + proxy.getNetworkOnline() + ")"
                            : "disabled") + "\n" +
                        "§7Group overrides: " + groups + "\n" +
                        "§7Config: §fconfig/neoessentials/tablist.json"
                    ), false);
                    return 1;
                })
            )

            // ── /tablist set header|footer ────────────────────────────────────
            .then(Commands.literal("set")
                .then(Commands.literal("header")
                    .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String text = StringArgumentType.getString(ctx, "text");
                            TablistManager tablist = TablistManager.getInstance();
                            tablist.setHeaderOverride(text);
                            var server = ServerLifecycleHooks.getCurrentServer();
                            if (server != null) tablist.updateAll(server);
                            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.tablist.header_set"), false);
                            return 1;
                        })
                    )
                )
                .then(Commands.literal("footer")
                    .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String text = StringArgumentType.getString(ctx, "text");
                            TablistManager tablist = TablistManager.getInstance();
                            tablist.setFooterOverride(text);
                            var server = ServerLifecycleHooks.getCurrentServer();
                            if (server != null) tablist.updateAll(server);
                            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.tablist.footer_set"), false);
                            return 1;
                        })
                    )
                )
            )

            // ── /tablist player <player> header|footer|reset ─────────────────
            .then(Commands.literal("player")
                .then(Commands.argument("target", EntityArgument.player())
                    .then(Commands.literal("header")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                String text = StringArgumentType.getString(ctx, "text");
                                TablistManager tablist = TablistManager.getInstance();
                                tablist.setPlayerHeaderOverride(target.getUUID(), text);
                                var server = ServerLifecycleHooks.getCurrentServer();
                                if (server != null) tablist.updatePlayer(target, server);
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                    "§aSet custom tablist header for §e" + target.getName().getString() + "§a."
                                ), false);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("footer")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                String text = StringArgumentType.getString(ctx, "text");
                                TablistManager tablist = TablistManager.getInstance();
                                tablist.setPlayerFooterOverride(target.getUUID(), text);
                                var server = ServerLifecycleHooks.getCurrentServer();
                                if (server != null) tablist.updatePlayer(target, server);
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                    "§aSet custom tablist footer for §e" + target.getName().getString() + "§a."
                                ), false);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("reset")
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                            TablistManager tablist = TablistManager.getInstance();
                            tablist.clearPlayerOverrides(target.getUUID());
                            var server = ServerLifecycleHooks.getCurrentServer();
                            if (server != null) tablist.updatePlayer(target, server);
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                "§aCleared tablist overrides for §e" + target.getName().getString() + "§a."
                            ), false);
                            return 1;
                        })
                    )
                )
            )

            // ── /tablist group <group> header|footer|reset ───────────────────
            .then(Commands.literal("group")
                .then(Commands.argument("group", StringArgumentType.word())
                    .then(Commands.literal("header")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String group = StringArgumentType.getString(ctx, "group");
                                String text  = StringArgumentType.getString(ctx, "text");
                                TablistManager tablist = TablistManager.getInstance();
                                tablist.setGroupHeaderOverride(group, text);
                                var server = ServerLifecycleHooks.getCurrentServer();
                                if (server != null) tablist.updateAll(server);
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                    "§aSet tablist header for group §e" + group + "§a."
                                ), false);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("footer")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String group = StringArgumentType.getString(ctx, "group");
                                String text  = StringArgumentType.getString(ctx, "text");
                                TablistManager tablist = TablistManager.getInstance();
                                tablist.setGroupFooterOverride(group, text);
                                var server = ServerLifecycleHooks.getCurrentServer();
                                if (server != null) tablist.updateAll(server);
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                    "§aSet tablist footer for group §e" + group + "§a."
                                ), false);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("reset")
                        .executes(ctx -> {
                            String group = StringArgumentType.getString(ctx, "group");
                            TablistManager tablist = TablistManager.getInstance();
                            tablist.clearGroupOverrides(group);
                            var server = ServerLifecycleHooks.getCurrentServer();
                            if (server != null) tablist.updateAll(server);
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                "§aCleared tablist overrides for group §e" + group + "§a."
                            ), false);
                            return 1;
                        })
                    )
                )
            )

            // ── /tablist proxy (BTLP-style proxy integration commands) ────────
            .then(Commands.literal("proxy")
                .then(Commands.literal("status")
                    .executes(ctx -> {
                        ProxyIntegration proxy = ProxyIntegration.getInstance();
                        String servers = proxy.getKnownServers().isEmpty()
                            ? "§7(none)" : "§e" + String.join("§7, §e", proxy.getKnownServers());
                        StringBuilder serverCounts = new StringBuilder();
                        proxy.getServerPlayerCounts().forEach((s, c) ->
                            serverCounts.append("  §7").append(s).append("§8: §e").append(c).append("\n")
                        );
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "§6§lProxy Integration Status:\n" +
                            "§7Proxy enabled: §" + (proxy.isProxyEnabled() ? "a" : "c") + proxy.isProxyEnabled() + "\n" +
                            "§7Proxy detected: §" + (proxy.isProxyDetected() ? "a" : "7") + proxy.isProxyDetected() + "\n" +
                            "§7Server label: §e" + proxy.getServerLabel() + "\n" +
                            "§7Network online: §e" + proxy.getNetworkOnline() + "\n" +
                            "§7Known servers: " + servers + "\n" +
                            "§7Per-server counts:\n" + serverCounts
                        ), false);
                        return 1;
                    })
                )
                .then(Commands.literal("setserver")
                    .then(Commands.argument("server", StringArgumentType.word())
                        .then(Commands.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0))
                            .executes(ctx -> {
                                String srvName = StringArgumentType.getString(ctx, "server");
                                int count = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "count");
                                ProxyIntegration.getInstance().setServerOnline(srvName, count);
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                    "§aSet §e" + srvName + " §aplayer count to §e" + count + "§a."
                                ), false);
                                return 1;
                            })
                        )
                    )
                )
            )

            // ── /tablist fakeplayer (BTLP-style fake player commands) ─────────
            .then(Commands.literal("fakeplayer")
                .then(Commands.literal("list")
                    .executes(ctx -> {
                        var entries = FakePlayerManager.getInstance().getEntries();
                        if (entries.isEmpty()) {
                            ctx.getSource().sendSuccess(() -> Component.literal("§7No fake player entries configured."), false);
                            return 1;
                        }
                        StringBuilder sb = new StringBuilder("§6§lFake Player Entries (").append(entries.size()).append("):\n");
                        for (var e : entries) {
                            sb.append("  §e").append(e.slotId())
                              .append(" §8→ §f").append(e.display())
                              .append(" §8(latency=§7").append(e.latency()).append("§8, listed=§7").append(e.listed()).append("§8)\n");
                        }
                        String msg = sb.toString();
                        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                        return 1;
                    })
                )
                .then(Commands.literal("add")
                    .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("display", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String id = StringArgumentType.getString(ctx, "id");
                                String display = StringArgumentType.getString(ctx, "display");
                                FakePlayerManager.getInstance().addEntry(
                                    new FakePlayerManager.FakeEntry(id, display, 0, true)
                                );
                                var server = ServerLifecycleHooks.getCurrentServer();
                                if (server != null) FakePlayerManager.getInstance().refreshAll(server);
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                    "§aAdded fake player §e" + id + " §adisplaying §f" + display + "§a."
                                ), false);
                                return 1;
                            })
                        )
                    )
                )
                .then(Commands.literal("remove")
                    .then(Commands.argument("id", StringArgumentType.word())
                        .executes(ctx -> {
                            String id = StringArgumentType.getString(ctx, "id");
                            boolean removed = FakePlayerManager.getInstance().removeEntry(id);
                            if (removed) {
                                ctx.getSource().sendSuccess(() -> Component.literal("§aRemoved fake player §e" + id + "§a."), false);
                            } else {
                                ctx.getSource().sendFailure(Component.literal("§cNo fake player entry found with id §e" + id + "§c."));
                            }
                            return removed ? 1 : 0;
                        })
                    )
                )
                .then(Commands.literal("refresh")
                    .executes(ctx -> {
                        var server = ServerLifecycleHooks.getCurrentServer();
                        if (server != null) FakePlayerManager.getInstance().refreshAll(server);
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "§aRefreshed §e" + FakePlayerManager.getInstance().getCount() + " §afake player entries for all online players."
                        ), false);
                        return 1;
                    })
                )
            )

            // ── /tablist layout (BTLP-style layout commands) ──────────────────
            .then(Commands.literal("layout")
                .then(Commands.literal("info")
                    .executes(ctx -> {
                        TablistLayout layout = TablistLayout.getInstance();
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "§6§lTablist Layout Configuration:\n" +
                            "§7Columns: §e" + layout.getColumns() + " §8(max " + layout.getTotalSlots() + " slots)\n" +
                            "§7Sort by group weight: §e" + layout.isSortByGroupWeight() + "\n" +
                            "§7Group sections: §e" + layout.isGroupSections() + "\n" +
                            "§7Players by server: §e" + layout.isPlayersByServer() + "\n" +
                            "§7Exclude servers: §e" + layout.getExcludeServers() + "\n" +
                            "§7Hidden servers: §e" + layout.getHiddenServers()
                        ), false);
                        return 1;
                    })
                )
                .then(Commands.literal("sort")
                    .executes(ctx -> {
                        // Toggle sort without reload
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "§7Sort state is config-driven. Use §e/tablist reload §7after editing tablist.json."
                        ), false);
                        return 1;
                    })
                )
            )

            // ── /tablist independent [on|off] ─────────────────────────────────
            .then(Commands.literal("independent")
                .executes(ctx -> {
                    boolean current = TablistManager.getInstance().isIndependentMode();
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        "§7Independent mode is §" + (current ? "a" : "c") + (current ? "ON" : "OFF") +
                        "§7. Use §e/tablist independent on §7or §e/tablist independent off §7to change."
                    ), false);
                    return 1;
                })
                .then(Commands.literal("on")
                    .executes(ctx -> {
                        TablistManager.getInstance().setIndependentMode(true);
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "§aIndependent mode §2enabled§a. NeoEssentials now fully manages the tablist."
                        ), false);
                        return 1;
                    })
                )
                .then(Commands.literal("off")
                    .executes(ctx -> {
                        TablistManager.getInstance().setIndependentMode(false);
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "§cIndependent mode §4disabled§c. Tab formatting may be shared with proxy plugins."
                        ), false);
                        return 1;
                    })
                )
            )
        );
    }

    private static void showHelp(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal(
            "§6§lTablist Commands (BungeeTabListPlus-style):\n" +
            "§e/tablist reload §7— reload tablist.json config\n" +
            "§e/tablist enable/disable §7— toggle tablist system\n" +
            "§e/tablist preview §7— preview your header/footer\n" +
            "§e/tablist info §7— full status, proxy, layout, fake players\n" +
            "§e/tablist set header/footer <text> §7— runtime override\n" +
            "§6Per-player:\n" +
            "§e/tablist player <name> header/footer/reset\n" +
            "§6Per-group:\n" +
            "§e/tablist group <group> header/footer/reset\n" +
            "§6Proxy (BTLP-style):\n" +
            "§e/tablist proxy status §7— proxy integration status\n" +
            "§e/tablist proxy setserver <name> <count> §7— manual count\n" +
            "§6Fake Players (BTLP-style):\n" +
            "§e/tablist fakeplayer list §7— show configured fake entries\n" +
            "§e/tablist fakeplayer add <id> <display> §7— add runtime entry\n" +
            "§e/tablist fakeplayer remove <id> §7— remove entry\n" +
            "§e/tablist fakeplayer refresh §7— re-inject all fake entries\n" +
            "§6Layout (BTLP-style):\n" +
            "§e/tablist layout info §7— show column/sorting config\n" +
            "§6Independent mode:\n" +
            "§e/tablist independent §7— show mode\n" +
            "§e/tablist independent on/off §7— toggle\n" +
            "§7Config: §fconfig/neoessentials/tablist.json\n" +
            "§7Placeholders: §f{network_online} {server_online:NAME} {current_server}\n" +
            "§7             {rank_weight} {session_minutes} {level} {health} {afk}"
        ), false);
    }
}

