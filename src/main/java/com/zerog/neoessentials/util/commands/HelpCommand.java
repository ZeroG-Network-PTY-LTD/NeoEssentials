package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.api.permissions.PermissionRegistry;
import com.zerog.neoessentials.commands.CommandRegistry;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * /help [page|command] — Paginated help system ported from EssentialsX Commandhelp.
 *
 * Displays all commands the player has permission to use, paginated.
 * /help <command> shows detailed info about a specific command.
 */
public class HelpCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(HelpCommand.class);
    private static final int CMDS_PER_PAGE = 10;
    private static final String PERMISSION = "neoessentials.help";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("help")) return;

        dispatcher.register(Commands.literal("help")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), PERMISSION);
            })
            // /help
            .executes(ctx -> executeHelp(ctx, null, 1))
            // /help <page>
            .then(Commands.argument("page", IntegerArgumentType.integer(1))
                .executes(ctx -> executeHelp(ctx, null, IntegerArgumentType.getInteger(ctx, "page")))
            )
            // /help <command>
            .then(Commands.argument("command", StringArgumentType.word())
                .executes(ctx -> executeHelp(ctx, StringArgumentType.getString(ctx, "command"), 1))
                // /help <command> <page>
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> executeHelp(ctx,
                        StringArgumentType.getString(ctx, "command"),
                        IntegerArgumentType.getInteger(ctx, "page")))
                )
            )
        );
        // /? alias
        dispatcher.register(Commands.literal("?")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), PERMISSION);
            })
            .executes(ctx -> executeHelp(ctx, null, 1))
            .then(Commands.argument("page", IntegerArgumentType.integer(1))
                .executes(ctx -> executeHelp(ctx, null, IntegerArgumentType.getInteger(ctx, "page")))
            )
        );
    }

    private static int executeHelp(CommandContext<CommandSourceStack> ctx, String search, int page) {
        var src = ctx.getSource();
        ServerPlayer player = src.getPlayer();
        UUID uuid = player != null ? player.getUUID() : null;

        // Get all registered commands
        CommandRegistry registry = CommandRegistry.getInstance();
        List<CommandRegistry.CommandInfo> allCommands = registry.getAllCommandsSorted();

        // Build list of commands accessible to this player
        List<CommandRegistry.CommandInfo> accessible = allCommands.stream()
            .filter(cmd -> {
                // Console can see everything
                if (uuid == null) return true;
                // Check for a permission node neoessentials.<command>
                String perm = "neoessentials." + cmd.getName();
                // If no specific permission registered, show it (default accessible)
                return !PermissionRegistry.getInstance().isRegistered(perm)
                    || PermissionAPI.hasPermission(uuid, perm)
                    || PermissionAPI.hasPermission(uuid, "neoessentials.admin");
            })
            .sorted(Comparator.comparing(CommandRegistry.CommandInfo::getName))
            .collect(Collectors.toList());

        // If searching for a specific command
        if (search != null && !search.isEmpty()) {
            final String query = search.toLowerCase();
            // Try exact match first
            Optional<CommandRegistry.CommandInfo> exact = accessible.stream()
                .filter(c -> c.getName().equalsIgnoreCase(query))
                .findFirst();
            if (exact.isPresent()) {
                showCommandDetail(src, exact.get());
                return 1;
            }
            // Filter by search term
            accessible = accessible.stream()
                .filter(c -> c.getName().toLowerCase().contains(query)
                    || (c.getDescription() != null && c.getDescription().toLowerCase().contains(query)))
                .collect(Collectors.toList());
            if (accessible.isEmpty()) {
                src.sendFailure(MessageUtil.error("commands.neoessentials.help.not_found", search));
                return 0;
            }
        }

        int totalPages = Math.max(1, (int) Math.ceil(accessible.size() / (double) CMDS_PER_PAGE));
        int p = Math.max(1, Math.min(page, totalPages));
        int start = (p - 1) * CMDS_PER_PAGE;
        int end = Math.min(start + CMDS_PER_PAGE, accessible.size());

        // Header
        src.sendSuccess(() -> Component.literal(
            "§6════ §eNeoEssentials Help §7(Page " + p + "/" + totalPages + ") §6════"), false);

        // List commands
        for (int i = start; i < end; i++) {
            CommandRegistry.CommandInfo cmd = accessible.get(i);
            String desc = cmd.getDescription() != null ? cmd.getDescription() : "No description";
            src.sendSuccess(() -> Component.literal("  §e/" + cmd.getName() + " §7- " + desc), false);
        }

        // Footer
        if (totalPages > 1) {
            src.sendSuccess(() -> Component.literal(
                "§7Use §e/help " + (p < totalPages ? (p + 1) : 1) + "§7 for the next page, or §e/help <command>§7 for details."), false);
        } else {
            src.sendSuccess(() -> Component.literal("§7Use §e/help <command>§7 for details on a specific command."), false);
        }
        return 1;
    }

    private static void showCommandDetail(CommandSourceStack src, CommandRegistry.CommandInfo cmd) {
        src.sendSuccess(() -> Component.literal("§6════ §e/" + cmd.getName() + " §6════"), false);
        String desc = cmd.getDescription() != null ? cmd.getDescription() : "No description available.";
        src.sendSuccess(() -> Component.literal("§7" + desc), false);
        src.sendSuccess(() -> Component.literal("§7Permission: §e" + "neoessentials." + cmd.getName()), false);
        List<String> aliases = cmd.getAliases();
        if (aliases != null && !aliases.isEmpty()) {
            src.sendSuccess(() -> Component.literal("§7Aliases: §e" + String.join("§7, §e", aliases)), false);
        }
    }
}



