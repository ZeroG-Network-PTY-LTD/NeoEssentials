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
 * /tablist — Admin command to manage the tablist at runtime.
 *
 * Sub-commands:
 *   /tablist reload                          — reload tablist config and push to all players
 *   /tablist enable                          — enable the tablist system
 *   /tablist disable                         — disable the tablist system (revert to vanilla)
 *   /tablist preview                         — show your own current header/footer
 *   /tablist info                            — show current tablist config status
 *   /tablist set header <text>               — set a single-frame global header in-game
 *   /tablist set footer <text>               — set a single-frame global footer in-game
 *   /tablist player <player> header <text>   — per-player header override
 *   /tablist player <player> footer <text>   — per-player footer override
 *   /tablist player <player> reset           — clear per-player overrides
 *   /tablist group <group> header <text>     — per-group header override
 *   /tablist group <group> footer <text>     — per-group footer override
 *   /tablist group <group> reset             — clear per-group overrides
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

            // /tablist reload
            .then(Commands.literal("reload")
                .executes(ctx -> {
                    TablistManager.getInstance().loadConfig();
                    var server = ServerLifecycleHooks.getCurrentServer();
                    if (server != null) TablistManager.getInstance().updateAll(server);
                    ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.tablist.reloaded"), false);
                    return 1;
                })
            )

            // /tablist enable
            .then(Commands.literal("enable")
                .executes(ctx -> {
                    TablistManager.getInstance().setEnabled(true);
                    var server = ServerLifecycleHooks.getCurrentServer();
                    if (server != null) TablistManager.getInstance().updateAll(server);
                    ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.tablist.enabled"), false);
                    return 1;
                })
            )

            // /tablist disable
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

            // /tablist preview
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

            // /tablist info
            .then(Commands.literal("info")
                .executes(ctx -> {
                    TablistManager mgr = TablistManager.getInstance();
                    boolean enabled = mgr.isEnabled();
                    String groups = mgr.getGroupsWithOverrides().isEmpty()
                        ? "§7(none)"
                        : "§e" + String.join(", ", mgr.getGroupsWithOverrides());
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        "§6Tablist System §8— §" + (enabled ? "aEnabled" : "cDisabled") + "\n" +
                        "§7Header frames: §e" + mgr.getHeaderFrameCount() + " §7| Footer frames: §e" + mgr.getFooterFrameCount() + "\n" +
                        "§7Refresh: §e" + mgr.getRefreshIntervalTicks() + " §7ticks §8| §7Hide vanished: §e" + mgr.isHideVanished() + "\n" +
                        "§7Group overrides: " + groups + "\n" +
                        "§7Config: §fconfig/neoessentials/tablist.json"
                    ), false);
                    return 1;
                })
            )

            // /tablist set header|footer <text>
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

            // /tablist player <player> header|footer|reset
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

            // /tablist group <group> header|footer|reset
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
        );
    }

    private static void showHelp(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal(
            "§6§lTablist Commands:\n" +
            "§e/tablist reload §7— reload tablist.json config\n" +
            "§e/tablist enable §7— enable tablist\n" +
            "§e/tablist disable §7— disable tablist\n" +
            "§e/tablist preview §7— preview your header/footer\n" +
            "§e/tablist info §7— show status, frame counts, group overrides\n" +
            "§e/tablist set header <text> §7— global header override (runtime)\n" +
            "§e/tablist set footer <text> §7— global footer override (runtime)\n" +
            "§6Per-player:\n" +
            "§e/tablist player <name> header <text> §7— custom header\n" +
            "§e/tablist player <name> footer <text> §7— custom footer\n" +
            "§e/tablist player <name> reset §7— clear per-player overrides\n" +
            "§6Per-group:\n" +
            "§e/tablist group <group> header <text> §7— custom header for group\n" +
            "§e/tablist group <group> footer <text> §7— custom footer for group\n" +
            "§e/tablist group <group> reset §7— clear group overrides\n" +
            "§7Config: §fconfig/neoessentials/tablist.json\n" +
            "§7Colors: §f&6 &a §7hex: §f&#FF5500 §7gradients: §f<gradient:FF0000-0000FF>text</gradient>"
        ), false);
    }
}

