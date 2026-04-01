package com.zerog.neoessentials.permissions;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.handler.IPermissionHandler;
import net.neoforged.neoforge.server.permission.nodes.PermissionDynamicContext;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * NeoForge {@link IPermissionHandler} bridge for NeoEssentials.
 *
 * <p>When this handler is the active NeoForge permission handler
 * (set via {@code permissionHandler = "neoessentials:handler"} in
 * {@code config/neoforge-server.toml}), ALL mods that check permissions
 * through NeoForge's {@code PermissionAPI.getPermission(player, node)} will
 * have their Boolean-typed nodes evaluated against NeoEssentials'
 * {@code permissions.json}.</p>
 *
 * <p>This means that adding {@code worldedit.*} or any other mod's permission
 * node to a group in {@code permissions.json} will grant that permission to
 * every member of that group across ALL installed mods—not only NeoEssentials
 * commands.</p>
 *
 * <p>The handler respects the full NeoEssentials permission chain:
 * <ol>
 *   <li>OP bypass (if {@code opsCanBypassPermissions} is enabled)</li>
 *   <li>External adapter (LuckPerms / FTB Ranks, if configured)</li>
 *   <li>Internal {@code permissions.json} groups + user nodes</li>
 * </ol>
 * </p>
 *
 * <p>Non-Boolean nodes (Integer, String, Component) always fall back to the
 * node's own default resolver.</p>
 */
public class NeoEssentialsPermissionHandler implements IPermissionHandler {

    /** The NeoForge handler identifier: {@code neoessentials:handler} */
    public static final ResourceLocation IDENTIFIER =
            ResourceLocation.fromNamespaceAndPath("neoessentials", "handler");

    private static final Logger LOGGER = LoggerFactory.getLogger(NeoEssentialsPermissionHandler.class);

    private final Set<PermissionNode<?>> registeredNodes;

    /**
     * Called by NeoForge after all mods have registered their
     * {@link PermissionNode}s via {@link net.neoforged.neoforge.server.permission.events.PermissionGatherEvent.Nodes}.
     *
     * @param nodes every permission node registered by every loaded mod
     */
    public NeoEssentialsPermissionHandler(Collection<PermissionNode<?>> nodes) {
        this.registeredNodes = new HashSet<>(nodes);
        LOGGER.info("[Permissions] NeoEssentialsPermissionHandler created with {} registered nodes", nodes.size());
    }

    @Override
    public ResourceLocation getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<PermissionNode<?>> getRegisteredNodes() {
        return Collections.unmodifiableSet(registeredNodes);
    }

    /**
     * Checks permission for an online player.
     *
     * <p>For {@link PermissionTypes#BOOLEAN} nodes the check is delegated to
     * {@link PermissionAPI#hasPermission(UUID, String)} so the full
     * NeoEssentials permission chain is honoured (OP-bypass → external adapter →
     * internal groups/wildcards).</p>
     *
     * <p>All other node types fall through to the node's own default resolver.</p>
     */
    @Override
    public <T> T getPermission(ServerPlayer player, PermissionNode<T> node,
                               PermissionDynamicContext<?>... context) {
        if (node.getType().equals(PermissionTypes.BOOLEAN)) {
            String nodeName = node.getNodeName(); // e.g. "worldedit.edit"
            if (PermissionSystem.isInitialized()) {
                try {
                    boolean result = PermissionAPI.hasPermission(player.getUUID(), nodeName);
                    LOGGER.debug("[NeoForgePermHandler] {} -> {} : {}", player.getScoreboardName(), nodeName, result);
                    @SuppressWarnings("unchecked")
                    T typed = (T) Boolean.valueOf(result);
                    return typed;
                } catch (Exception e) {
                    LOGGER.warn("[NeoForgePermHandler] Error checking '{}' for {}: {}",
                            nodeName, player.getScoreboardName(), e.getMessage());
                }
            }
        }
        // Fall back to the node's built-in resolver (usually OP-level check)
        return node.getDefaultResolver().resolve(player, player.getUUID(), context);
    }

    /**
     * Checks permission for an offline player (UUID only).
     *
     * <p>Same delegation logic as {@link #getPermission(ServerPlayer, PermissionNode, PermissionDynamicContext[])},
     * but without access to a live {@link ServerPlayer}.</p>
     */
    @Override
    public <T> T getOfflinePermission(UUID playerUUID, PermissionNode<T> node,
                                      PermissionDynamicContext<?>... context) {
        if (node.getType().equals(PermissionTypes.BOOLEAN)) {
            String nodeName = node.getNodeName();
            if (PermissionSystem.isInitialized()) {
                try {
                    boolean result = PermissionAPI.hasPermission(playerUUID, nodeName);
                    LOGGER.debug("[NeoForgePermHandler] (offline) {} -> {} : {}", playerUUID, nodeName, result);
                    @SuppressWarnings("unchecked")
                    T typed = (T) Boolean.valueOf(result);
                    return typed;
                } catch (Exception e) {
                    LOGGER.warn("[NeoForgePermHandler] Error checking offline '{}' for {}: {}",
                            nodeName, playerUUID, e.getMessage());
                }
            }
        }
        return node.getDefaultResolver().resolve(null, playerUUID, context);
    }
}

