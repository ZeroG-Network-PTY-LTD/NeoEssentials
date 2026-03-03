package com.zerog.neoessentials.tablist;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * /tablist — Admin command to manage the tablist at runtime.
 *
 * Sub-commands:
 *   /tablist reload          — reload tablist config and push to all players
 *   /tablist enable          — enable the tablist system
 *   /tablist disable         — disable the tablist system (revert to vanilla)
 *   /tablist preview         — show your own current header/footer
 *   /tablist set header <text>  — set a single-frame header in-game
 *   /tablist set footer <text>  — set a single-frame footer in-game
 *   /tablist info            — show current tablist config status
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
            .executes(ctx -> {
                showHelp(ctx.getSource());
                return 1;
            })
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
                    // Send empty header/footer to all players to restore vanilla appearance
                    var server = ServerLifecycleHooks.getCurrentServer();
                    if (server != null) {
                        var emptyPacket = new net.minecraft.network.protocol.game.ClientboundTabListPacket(
                            Component.empty(), Component.empty());
                        for (var p : server.getPlayerList().getPlayers()) {
                            p.connection.send(emptyPacket);
                        }
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
                    boolean enabled = TablistManager.getInstance().isEnabled();
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        "§6Tablist System §8— §" + (enabled ? "aEnabled" : "cDisabled") + "\n" +
                        "§7Use §e/tablist reload §7to reload config, §e/tablist preview §7to preview your tab."
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
                            // Update the in-memory first frame directly (runtime override)
                            // A full reload from disk will reset this
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
        );
    }

    private static void showHelp(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal(
            "§6§lTablist Commands:\n" +
            "§e/tablist reload §7— reload config\n" +
            "§e/tablist enable §7— enable tablist\n" +
            "§e/tablist disable §7— disable tablist\n" +
            "§e/tablist preview §7— preview your header/footer\n" +
            "§e/tablist set header <text> §7— set header override\n" +
            "§e/tablist set footer <text> §7— set footer override\n" +
            "§e/tablist info §7— show status"
        ), false);
    }
}

