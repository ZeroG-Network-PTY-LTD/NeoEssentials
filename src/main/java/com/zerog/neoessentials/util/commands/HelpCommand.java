package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.commands.CommandRegistry;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;

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

        // Vanilla's /help node shadows pagination (/help 2) in this Brigadier build.
        // We remove the existing root nodes via reflection before registering ours.
        removeRootLiteral(dispatcher, "help");
        removeRootLiteral(dispatcher, "?");

        // NOTE: Vanilla Minecraft registers /help <command:string> before any mod.
        // Brigadier matches children in insertion order, so a separate int-argument branch
        // would never be reached (the vanilla string branch grabs the number first).
        // Solution: use a single optional string argument and detect page numbers inside.
        dispatcher.register(Commands.literal("help")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), PERMISSION);
            })
            // /help
            .executes(ctx -> executeHelp(ctx, null, 1))
            // /help <page_or_command>  — handles both "/help 2" and "/help warp"
            .then(Commands.argument("page_or_command", StringArgumentType.word())
                .executes(ctx -> {
                    String arg = StringArgumentType.getString(ctx, "page_or_command");
                    try {
                        int pageNum = Integer.parseInt(arg);
                        if (pageNum >= 1) return executeHelp(ctx, null, pageNum);
                    } catch (NumberFormatException ignored) {}
                    return executeHelp(ctx, arg, 1);
                })
                // /help <command> <page>
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(ctx -> executeHelp(ctx,
                        StringArgumentType.getString(ctx, "page_or_command"),
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
            .then(Commands.argument("page_or_command", StringArgumentType.word())
                .executes(ctx -> {
                    String arg = StringArgumentType.getString(ctx, "page_or_command");
                    try {
                        int pageNum = Integer.parseInt(arg);
                        if (pageNum >= 1) return executeHelp(ctx, null, pageNum);
                    } catch (NumberFormatException ignored) {}
                    return executeHelp(ctx, arg, 1);
                })
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
        // Show all registered commands; individual commands handle their own permission checks
        List<CommandRegistry.CommandInfo> accessible = allCommands.stream()
            .filter(cmd -> {
                // Console can see everything; for players check admin or generic perm
                if (uuid == null) return true;
                // Admin can see all
                if (PermissionAPI.hasPermission(uuid, "neoessentials.admin")) return true;
                if (PermissionAPI.hasPermission(uuid, "neoessentials.*")) return true;
                // A command explicitly marked as requiring nothing (registerCommandWithPermission(...,
                // "", ...)) is always visible — matches its actual .requires() (or lack thereof).
                if (cmd.hasPermissionOverride() && cmd.getPermissionNodeOverride().isEmpty()) return true;
                String perm = resolvePermissionNode(cmd);
                // Try the command-specific permission; if not explicitly denied, show it
                return PermissionAPI.hasPermission(uuid, perm);
            })
            .sorted(Comparator.comparing(CommandRegistry.CommandInfo::getName))
            .toList();

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
                .toList();
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
        final int pFinal = p;
        final int totalPagesFinal = totalPages;
        src.sendSuccess(() -> MessageUtil.component(
            "commands.neoessentials.help.header", pFinal, totalPagesFinal), false);

        // List commands
        for (int i = start; i < end; i++) {
            CommandRegistry.CommandInfo cmd = accessible.get(i);
            String desc = getLocalizedDescription(cmd);
            src.sendSuccess(() -> MessageUtil.component(
                "commands.neoessentials.help.entry", cmd.getName(), desc), false);
        }

        // Footer
        if (totalPages > 1) {
            final int nextPage = p < totalPages ? (p + 1) : 1;
            src.sendSuccess(() -> MessageUtil.component(
                "commands.neoessentials.help.footer_next", nextPage), false);
        } else {
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.help.footer"), false);
        }
        return 1;
    }

    private static void showCommandDetail(CommandSourceStack src, CommandRegistry.CommandInfo cmd) {
        src.sendSuccess(() -> MessageUtil.component(
            "commands.neoessentials.help.detail_header", cmd.getName()), false);
        String desc = getLocalizedDescription(cmd);
        src.sendSuccess(() -> Component.literal("§7" + desc), false);
        String permDisplay = cmd.hasPermissionOverride() && cmd.getPermissionNodeOverride().isEmpty()
            ? "none — open to everyone"
            : resolvePermissionNode(cmd);
        src.sendSuccess(() -> MessageUtil.component(
            "commands.neoessentials.help.detail_permission", permDisplay), false);
        List<String> aliases = cmd.getAliases();
        if (aliases != null && !aliases.isEmpty()) {
            src.sendSuccess(() -> MessageUtil.component(
                "commands.neoessentials.help.detail_aliases", String.join("§7, §e", aliases)), false);
        }
    }

    /**
     * The permission node to check/display for a command: its explicit override if one was
     * registered via {@link CommandRegistry#registerCommandWithPermission}, else the legacy
     * {@code "neoessentials." + name} guess (correct for most simple commands, but not all —
     * see that method's own doc for why an explicit override is sometimes needed).
     */
    private static String resolvePermissionNode(CommandRegistry.CommandInfo cmd) {
        if (cmd.hasPermissionOverride()) {
            return cmd.getPermissionNodeOverride();
        }
        return "neoessentials." + cmd.getName().toLowerCase();
    }

    /**
     * Get a localized description for a command.
     * Checks for a translation key "commands.neoessentials.cmd.NAME.description" first;
     * falls back to the registered English description if not found.
     */
    private static String getLocalizedDescription(CommandRegistry.CommandInfo cmd) {
        String name = cmd.getName().toLowerCase();
        String descKey = "commands.neoessentials.cmd." + name + ".description";
        if (MessageUtil.hasTranslation(descKey)) {
            return MessageUtil.localize(descKey);
        }
        String fallback = cmd.getDescription();
        return (fallback != null && !fallback.isEmpty())
            ? fallback
            : MessageUtil.localize("commands.neoessentials.help.no_description");
    }

    @SuppressWarnings("unchecked")
    private static void removeRootLiteral(CommandDispatcher<CommandSourceStack> dispatcher, String literal) {
        try {
            CommandNode<CommandSourceStack> root = dispatcher.getRoot();
            Field childrenField = CommandNode.class.getDeclaredField("children");
            Field literalsField = CommandNode.class.getDeclaredField("literals");
            childrenField.setAccessible(true);
            literalsField.setAccessible(true);

            Map<String, CommandNode<CommandSourceStack>> children =
                (Map<String, CommandNode<CommandSourceStack>>) childrenField.get(root);
            Map<String, CommandNode<CommandSourceStack>> literals =
                (Map<String, CommandNode<CommandSourceStack>>) literalsField.get(root);

            children.remove(literal);
            literals.remove(literal);
        } catch (Exception e) {
            LOGGER.debug("Could not remove existing '{}' command node before registering NeoEssentials help", literal, e);
        }
    }
}
