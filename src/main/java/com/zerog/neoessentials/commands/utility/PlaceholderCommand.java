package com.zerog.neoessentials.commands.utility;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.zerog.neoessentials.api.PlaceholderManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

/**
 * In-game admin command for inspecting and testing the NeoEssentials placeholder system.
 *
 * <h3>Sub-commands</h3>
 * <ul>
 *   <li>{@code /placeholder list}                    – List all registered placeholder identifiers</li>
 *   <li>{@code /placeholder test <text> [player]}    – Resolve placeholders in {@code text} (optional player context)</li>
 *   <li>{@code /placeholder info <identifier>}       – Show whether an identifier is registered</li>
 *   <li>{@code /placeholder stats}                   – Show registry statistics</li>
 * </ul>
 *
 * <p>Permission: {@code neoessentials.admin.placeholders}</p>
 */
public class PlaceholderCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlaceholderCommand.class);

    // ─────────────────────────────────────────────────────────────────────────
    // Registration
    // ─────────────────────────────────────────────────────────────────────────

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("placeholder")
            .requires(src -> PermissionValidator.validateAdminPermission(src, "neoessentials.admin.placeholders").hasPermission())
            .executes(PlaceholderCommand::showHelp)

            // /placeholder list
            .then(Commands.literal("list")
                .executes(PlaceholderCommand::handleList))

            // /placeholder stats
            .then(Commands.literal("stats")
                .executes(PlaceholderCommand::handleStats))

            // /placeholder info <identifier>
            .then(Commands.literal("info")
                .then(Commands.argument("identifier", StringArgumentType.word())
                    .suggests(PlaceholderCommand::suggestIdentifiers)
                    .executes(PlaceholderCommand::handleInfo)))

            // /placeholder test <text>
            .then(Commands.literal("test")
                .then(Commands.argument("text", StringArgumentType.greedyString())
                    .executes(PlaceholderCommand::handleTest)))
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Handlers
    // ─────────────────────────────────────────────────────────────────────────

    private static int showHelp(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.placeholder.help_header"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.placeholder.help_list_line"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.placeholder.help_info_line"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.placeholder.help_test_line"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.placeholder.help_stats_line"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.placeholder.help_footer"), false);
        return 1;
    }

    /** /placeholder list */
    private static int handleList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        PlaceholderManager pm = PlaceholderManager.getInstance();
        Set<String> all = new TreeSet<>(pm.getRegisteredPlaceholders());

        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.placeholder.list_header", all.size()), false);
        if (all.isEmpty()) {
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.placeholder.list_none"), false);
        } else {
            // Group into rows of 4 for readability
            StringBuilder row = new StringBuilder("§7");
            int n = 0;
            for (String id : all) {
                row.append("{").append(id).append("}  ");
                if (++n % 4 == 0) {
                    String line = row.toString().trim();
                    src.sendSuccess(() -> Component.literal(line), false);
                    row = new StringBuilder("§7");
                }
            }
            if (row.length() > 2) {
                String line = row.toString().trim();
                src.sendSuccess(() -> Component.literal(line), false);
            }
        }
        return 1;
    }

    /** /placeholder stats */
    private static int handleStats(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        PlaceholderManager pm = PlaceholderManager.getInstance();
        Map<String, Object> stats = pm.getStatistics();

        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.placeholder.stats_header"), false);
        stats.forEach((k, v) ->
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.placeholder.stats_line", k, v), false));
        return 1;
    }

    /** /placeholder info <identifier> */
    private static int handleInfo(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        String identifier = StringArgumentType.getString(ctx, "identifier");
        PlaceholderManager pm = PlaceholderManager.getInstance();
        boolean registered = pm.isPlaceholderRegistered(identifier);

        if (registered) {
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.placeholder.info_registered", identifier), false);
        } else {
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.placeholder.info_not_registered", identifier), false);
        }
        return 1;
    }

    /** /placeholder test <text>  — uses command issuer as player context */
    private static int handleTest(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        String text = StringArgumentType.getString(ctx, "text");
        PlaceholderManager pm = PlaceholderManager.getInstance();

        ServerPlayer player = null;
        try {
            player = src.getPlayerOrException();
        } catch (Exception ignored) {
            // console — no player context
        }

        final ServerPlayer resolvePlayer = player;
        String resolved;
        try {
            resolved = pm.setPlaceholders(resolvePlayer, text);
        } catch (Exception e) {
            LOGGER.warn("Error resolving test placeholder '{}': {}", text, e.getMessage());
            resolved = MessageUtil.localize("commands.neoessentials.placeholder.test_error", e.getMessage());
        }

        final String result = resolved;
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.placeholder.test_input_line", text), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.placeholder.test_output_line", result), false);
        return 1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tab completion
    // ─────────────────────────────────────────────────────────────────────────

    private static CompletableFuture<Suggestions> suggestIdentifiers(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String input = builder.getRemaining().toLowerCase();
        PlaceholderManager pm = PlaceholderManager.getInstance();
        pm.getRegisteredPlaceholders().stream()
            .filter(id -> id.startsWith(input))
            .sorted()
            .forEach(builder::suggest);
        return builder.buildFuture();
    }
}

