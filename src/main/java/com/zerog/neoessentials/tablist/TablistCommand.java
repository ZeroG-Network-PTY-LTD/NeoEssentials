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
                    var server = player.level().getServer();
                    if (server != null) TablistManager.getInstance().updatePlayer(player, server);
                    ctx.getSource().sendSuccess(() -> MessageUtil.component("commands.neoessentials.tablist.preview_active"), false);
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
                    final String fStatus = (enabled ? "aEnabled" : "cDisabled");
                    final String fMode = (mgr.isIndependentMode() ? "Independent" : "Proxy-managed");
                    final String fProxyColor = (proxy.isProxyEnabled() ? "a" : "7");
                    final String fProxyState = proxy.isProxyEnabled()
                        ? "enabled (detected=" + proxy.isProxyDetected() + ", network=" + proxy.getNetworkOnline() + ")"
                        : "disabled";
                    ctx.getSource().sendSuccess(() -> MessageUtil.component("commands.neoessentials.tablist.info",
                        fStatus, fMode, mgr.getHeaderFrameCount(), mgr.getFooterFrameCount(),
                        mgr.getRefreshIntervalTicks(), layout.isSortByGroupWeight(), layout.getColumns(),
                        layout.isPlayersByServer(), FakePlayerManager.getInstance().getCount(), mgr.isHideVanished(),
                        fProxyColor, fProxyState, groups
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
                                ctx.getSource().sendSuccess(() -> MessageUtil.component(
                                    "commands.neoessentials.tablist.player_header_set", target.getName().getString()
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
                                ctx.getSource().sendSuccess(() -> MessageUtil.component(
                                    "commands.neoessentials.tablist.player_footer_set", target.getName().getString()
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
                            ctx.getSource().sendSuccess(() -> MessageUtil.component(
                                "commands.neoessentials.tablist.player_reset", target.getName().getString()
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
                                ctx.getSource().sendSuccess(() -> MessageUtil.component(
                                    "commands.neoessentials.tablist.group_header_set", group
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
                                ctx.getSource().sendSuccess(() -> MessageUtil.component(
                                    "commands.neoessentials.tablist.group_footer_set", group
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
                            ctx.getSource().sendSuccess(() -> MessageUtil.component(
                                "commands.neoessentials.tablist.group_reset", group
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
                        final String fEnabledLine = (proxy.isProxyEnabled() ? "a" : "c") + proxy.isProxyEnabled();
                        final String fDetectedLine = (proxy.isProxyDetected() ? "a" : "7") + proxy.isProxyDetected();
                        ctx.getSource().sendSuccess(() -> MessageUtil.component("commands.neoessentials.tablist.proxy_status",
                            fEnabledLine, fDetectedLine, proxy.getServerLabel(), proxy.getNetworkOnline(),
                            servers, serverCounts.toString()
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
                                ctx.getSource().sendSuccess(() -> MessageUtil.component(
                                    "commands.neoessentials.tablist.proxy_setserver", srvName, count
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
                            ctx.getSource().sendSuccess(() -> MessageUtil.component("commands.neoessentials.tablist.fakeplayer_list_empty"), false);
                            return 1;
                        }
                        StringBuilder sb = new StringBuilder(MessageUtil.localize("commands.neoessentials.tablist.fakeplayer_list_header", entries.size())).append("\n");
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
                                ctx.getSource().sendSuccess(() -> MessageUtil.component(
                                    "commands.neoessentials.tablist.fakeplayer_added", id, display
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
                                ctx.getSource().sendSuccess(() -> MessageUtil.component("commands.neoessentials.tablist.fakeplayer_removed", id), false);
                            } else {
                                ctx.getSource().sendFailure(MessageUtil.component("commands.neoessentials.tablist.fakeplayer_not_found", id));
                            }
                            return removed ? 1 : 0;
                        })
                    )
                )
                .then(Commands.literal("refresh")
                    .executes(ctx -> {
                        var server = ServerLifecycleHooks.getCurrentServer();
                        if (server != null) FakePlayerManager.getInstance().refreshAll(server);
                        ctx.getSource().sendSuccess(() -> MessageUtil.component(
                            "commands.neoessentials.tablist.fakeplayer_refreshed", FakePlayerManager.getInstance().getCount()
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
                        ctx.getSource().sendSuccess(() -> MessageUtil.component("commands.neoessentials.tablist.layout_info",
                            layout.getColumns(), layout.getTotalSlots(), layout.isSortByGroupWeight(),
                            layout.isGroupSections(), layout.isPlayersByServer(), layout.getExcludeServers(),
                            layout.getHiddenServers()
                        ), false);
                        return 1;
                    })
                )
                .then(Commands.literal("sort")
                    .executes(ctx -> {
                        // Toggle sort without reload
                        ctx.getSource().sendSuccess(() -> MessageUtil.component(
                            "commands.neoessentials.tablist.layout_sort_info"
                        ), false);
                        return 1;
                    })
                )
            )

            // ── /tablist independent [on|off] ─────────────────────────────────
            .then(Commands.literal("independent")
                .executes(ctx -> {
                    boolean current = TablistManager.getInstance().isIndependentMode();
                    final String fState = (current ? "a" : "c") + (current ? "ON" : "OFF");
                    ctx.getSource().sendSuccess(() -> MessageUtil.component(
                        "commands.neoessentials.tablist.independent_status", fState
                    ), false);
                    return 1;
                })
                .then(Commands.literal("on")
                    .executes(ctx -> {
                        TablistManager.getInstance().setIndependentMode(true);
                        ctx.getSource().sendSuccess(() -> MessageUtil.component(
                            "commands.neoessentials.tablist.independent_on"
                        ), false);
                        return 1;
                    })
                )
                .then(Commands.literal("off")
                    .executes(ctx -> {
                        TablistManager.getInstance().setIndependentMode(false);
                        ctx.getSource().sendSuccess(() -> MessageUtil.component(
                            "commands.neoessentials.tablist.independent_off"
                        ), false);
                        return 1;
                    })
                )
            )

            // ── /tablist animations list ──────────────────────────────────────
            .then(Commands.literal("animations")
                .then(Commands.literal("list")
                    .executes(ctx -> {
                        AnimationManager am = AnimationManager.getInstance();
                        ctx.getSource().sendSuccess(() -> MessageUtil.component(
                            "commands.neoessentials.tablist.animations_list_header", am.getAnimationCount()
                        ), false);
                        for (String line : am.getSummaryLines()) {
                            String colored = line.replace("&", "§");
                            ctx.getSource().sendSuccess(() -> Component.literal(colored), false);
                        }
                        ctx.getSource().sendSuccess(() -> MessageUtil.component(
                            "commands.neoessentials.tablist.animations_list_footer"
                        ), false);
                        return 1;
                    })
                )
            )
        );
    }

    private static void showHelp(CommandSourceStack src) {
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.tablist.help"), false);
    }
}

