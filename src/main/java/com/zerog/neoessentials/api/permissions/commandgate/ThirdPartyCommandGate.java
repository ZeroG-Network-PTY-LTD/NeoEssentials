package com.zerog.neoessentials.api.permissions.commandgate;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lets admins require a NeoEssentials permission node to run a command registered by another
 * mod (or a vanilla command NeoEssentials hasn't otherwise removed/overridden), configured via
 * {@code command_permissions.json}.
 *
 * <p>Installed once per server boot, from a {@code RegisterCommandsEvent} listener at
 * {@code EventPriority.LOWEST} so every other mod's commands already
 * exist in the shared dispatcher. Each targeted node's private {@code command} field
 * (Brigadier's executor) is reflectively wrapped with a live permission check — the same
 * technique {@code HelpCommand.removeRootLiteral} already uses on {@code CommandNode}'s
 * {@code children}/{@code literals} fields, just applied to wrap instead of remove.</p>
 *
 * <p>The wrapper re-reads the current config on every invocation rather than baking in a
 * value at wrap time, so {@code /neoe reload} only needs to reload config data — it never
 * touches Brigadier internals again after the initial install.</p>
 */
public final class ThirdPartyCommandGate {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThirdPartyCommandGate.class);

    private static volatile CommandGateConfig currentConfig = CommandGateConfig.empty();
    private static final Map<CommandNode<?>, Boolean> wrapped = new IdentityHashMap<>();

    private ThirdPartyCommandGate() {}

    /**
     * Walks the dispatcher for every top-level path prefix present in the current config and
     * wraps each resolvable executable node under it. Call once, from a
     * {@code RegisterCommandsEvent} listener registered at {@code EventPriority.LOWEST}.
     */
    public static synchronized void installOnRegisteredCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        reloadConfig();

        List<List<String>> topLevelPaths = new ArrayList<>();
        for (CommandGateConfig.Mapping mapping : currentConfig.getMappings()) {
            List<String> path = mapping.pathSegments();
            if (topLevelPaths.stream().noneMatch(existing -> existing.equals(path.subList(0, 1)))) {
                topLevelPaths.add(List.of(path.get(0)));
            }
        }

        int resolved = 0;
        int skipped = 0;
        int leavesWrapped = 0;
        for (List<String> topLevelPath : topLevelPaths) {
            CommandNode<CommandSourceStack> node = findNode(dispatcher.getRoot(), topLevelPath);
            if (node == null) {
                NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,
                    "command_permissions.json: no registered command matches '{}' (mod not installed, typo, or it registers later) — skipping",
                    String.join(" ", topLevelPath));
                skipped++;
                continue;
            }
            resolved++;
            leavesWrapped += wrapSubtree(node);
        }

        NeoLog.info(LOGGER, LogCategory.PERMISSIONS,
            "Third-party command permission gate installed: {} mapping(s) configured, {} top-level path(s) resolved, {} skipped, {} command node(s) wrapped",
            currentConfig.getMappings().size(), resolved, skipped, leavesWrapped);
    }

    /** Re-parses {@code command_permissions.json} into a fresh config snapshot. Called at install time and by {@code /neoe reload}. */
    public static void reloadConfig() {
        try {
            var json = ConfigManager.getInstance().getConfig(ConfigManager.COMMAND_PERMISSIONS_CONFIG);
            currentConfig = CommandGateConfig.parse(json);
        } catch (Exception e) {
            NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,
                "Failed to load command_permissions.json, keeping previous mappings: {}", e.getMessage());
        }
    }

    private static CommandNode<CommandSourceStack> findNode(CommandNode<CommandSourceStack> root, List<String> pathSegments) {
        CommandNode<CommandSourceStack> current = root;
        for (String segment : pathSegments) {
            current = current.getChild(segment);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    /** Wraps every executable node at or below {@code node}. Returns how many were wrapped. */
    private static int wrapSubtree(CommandNode<CommandSourceStack> node) {
        int count = 0;
        Deque<CommandNode<CommandSourceStack>> queue = new ArrayDeque<>();
        Deque<List<String>> pathQueue = new ArrayDeque<>();
        queue.add(node);
        pathQueue.add(pathOf(node));
        while (!queue.isEmpty()) {
            CommandNode<CommandSourceStack> current = queue.poll();
            List<String> path = pathQueue.poll();
            if (current.getCommand() != null && wrapNode(current, path)) {
                count++;
            }
            for (CommandNode<CommandSourceStack> child : current.getChildren()) {
                List<String> childPath = new ArrayList<>(path);
                childPath.add(child.getName());
                queue.add(child);
                pathQueue.add(childPath);
            }
        }
        return count;
    }

    private static List<String> pathOf(CommandNode<CommandSourceStack> node) {
        List<String> path = new ArrayList<>();
        path.add(node.getName());
        return path;
    }

    @SuppressWarnings("unchecked")
    private static boolean wrapNode(CommandNode<CommandSourceStack> node, List<String> path) {
        if (wrapped.containsKey(node)) {
            return false;
        }
        try {
            Field commandField = CommandNode.class.getDeclaredField("command");
            commandField.setAccessible(true);
            Command<CommandSourceStack> original = (Command<CommandSourceStack>) commandField.get(node);
            if (original == null) {
                return false;
            }
            Command<CommandSourceStack> gated = ctx -> {
                if (!ConfigManager.isCommandPermissionGateEnabled()) {
                    return original.run(ctx);
                }
                String requiredNode = currentConfig.findPermissionNodeFor(path);
                if (requiredNode == null) {
                    return original.run(ctx);
                }
                PermissionValidator.PermissionResult result =
                    PermissionValidator.validatePermission(ctx.getSource(), requiredNode);
                if (!result.hasPermission()) {
                    ctx.getSource().sendFailure(MessageUtil.error(result.getErrorMessage()));
                    return 0;
                }
                return original.run(ctx);
            };
            commandField.set(node, gated);
            wrapped.put(node, Boolean.TRUE);
            return true;
        } catch (Exception e) {
            NeoLog.warn(LOGGER, LogCategory.PERMISSIONS,
                "Failed to wrap command node '{}' for third-party permission gating: {}",
                String.join(" ", path), e.getMessage());
            return false;
        }
    }
}
