package com.zerog.neoessentials.inventory;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Commands for viewing and editing other players' inventories
 * Commands:
 * - /invsee player    - View another player's inventory (read-only)
 * - /invseeedit player - View and edit another player's inventory
 * - /enderchest player - View another player's ender chest (read-only)
 * - /enderchestedit player - View and edit another player's ender chest
 * Permissions:
 * - neoessentials.invsee        - View other players' inventories
 * - neoessentials.invsee.edit   - Edit other players' inventories
 * - neoessentials.enderchest    - View other players' ender chests
 * - neoessentials.enderchest.edit - Edit other players' ender chests
 *
 * Anti-duplication: only one editor may hold an edit lock per target at a time.
 * Config: commands.invsee / invseeedit / enderchest / enderchestedit control enable/disable.
 * Audit: every view/edit action is written to neoessentials/inventory_audit.log.
 */
@EventBusSubscriber(modid = "neoessentials")
public class InventoryViewCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryViewCommands.class);

    /**
     * targetUUID → viewerUUID for live inventory edit sessions.
     * A target may only be edited by one viewer at a time.
     */
    private static final ConcurrentHashMap<UUID, UUID> activeInvEdits = new ConcurrentHashMap<>();

    /**
     * targetUUID → viewerUUID for live ender-chest edit sessions.
     */
    private static final ConcurrentHashMap<UUID, UUID> activeEcEdits = new ConcurrentHashMap<>();

    // ── Command registration ────────────────────────────────────────────────

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /invsee <player> - View inventory (read-only)
        dispatcher.register(
            Commands.literal("invsee")
                .requires(source -> isEnabled("invsee") && hasPermission(source, "neoessentials.invsee"))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> viewInventory(ctx, false))
                )
        );

        // /invseeedit <player> - View and edit inventory
        dispatcher.register(
            Commands.literal("invseeedit")
                .requires(source -> isEnabled("invseeedit") && hasPermission(source, "neoessentials.invsee.edit"))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> viewInventory(ctx, true))
                )
        );

        // /enderchest <player> - View ender chest (read-only)
        dispatcher.register(
            Commands.literal("enderchest")
                .requires(source -> isEnabled("enderchest") && hasPermission(source, "neoessentials.enderchest"))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> viewEnderChest(ctx, false))
                )
        );

        // /enderchestedit <player> - View and edit ender chest
        dispatcher.register(
            Commands.literal("enderchestedit")
                .requires(source -> isEnabled("enderchestedit") && hasPermission(source, "neoessentials.enderchest.edit"))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> viewEnderChest(ctx, true))
                )
        );

        // Aliases — registered as full commands (NOT as redirects) so that
        // Brigadier applies the requires() check before any dispatch occurs.
        dispatcher.register(
            Commands.literal("inv")
                .requires(source -> isEnabled("invsee") && hasPermission(source, "neoessentials.invsee"))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> viewInventory(ctx, false))
                )
        );
        dispatcher.register(
            Commands.literal("ec")
                .requires(source -> isEnabled("enderchest") && hasPermission(source, "neoessentials.enderchest"))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> viewEnderChest(ctx, false))
                )
        );
        dispatcher.register(
            Commands.literal("ecedit")
                .requires(source -> isEnabled("enderchestedit") && hasPermission(source, "neoessentials.enderchest.edit"))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> viewEnderChest(ctx, true))
                )
        );

        LOGGER.info("Registered inventory view commands: /invsee, /invseeedit, /enderchest, /enderchestedit (/inv, /ec, /ecedit)");
    }

    // ── Edit-lock management ────────────────────────────────────────────────

    /**
     * A player can only have one menu open at a time, so any container close
     * means an edit session (if one was open) has ended — release its lock.
     * Without this, closing the GUI (rather than disconnecting) left the
     * target permanently locked out for every other editor.
     */
    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            releaseEditLocks(player.getUUID());
        }
    }

    /**
     * Release all edit locks held by the given viewer UUID and log closures.
     * Called when a viewer disconnects or closes their menu, so the target is accessible again.
     */
    public static void releaseEditLocks(UUID viewerId) {
        String viewerName = resolveNameFromServer(viewerId);

        activeInvEdits.entrySet().removeIf(entry -> {
            if (entry.getValue().equals(viewerId)) {
                String targetName = resolveNameFromServer(entry.getKey());
                InventoryAuditLogger.log(viewerName, InventoryAuditLogger.INV_EDIT_CLOSED,
                    targetName, "Viewer disconnected — edit lock released");
                LOGGER.info("Inventory edit lock released: {} was editing {}'s inventory", viewerName, targetName);
                return true;
            }
            return false;
        });

        activeEcEdits.entrySet().removeIf(entry -> {
            if (entry.getValue().equals(viewerId)) {
                String targetName = resolveNameFromServer(entry.getKey());
                InventoryAuditLogger.log(viewerName, InventoryAuditLogger.EC_EDIT_CLOSED,
                    targetName, "Viewer disconnected — ender chest edit lock released");
                LOGGER.info("Ender chest edit lock released: {} was editing {}'s ender chest", viewerName, targetName);
                return true;
            }
            return false;
        });
    }

    /**
     * Resolve a player display name from the running server.
     * Falls back to the UUID string if the player is not found (offline).
     */
    private static String resolveNameFromServer(UUID uuid) {
        try {
            net.minecraft.server.MinecraftServer server =
                net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                net.minecraft.server.level.ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                if (p != null) return p.getName().getString();
                // Fall back to profile cache for recently-seen players
                net.minecraft.server.players.UserNameToIdResolver cache = server.services().nameToIdCache();
                if (cache != null) {
                    java.util.Optional<net.minecraft.server.players.NameAndId> profile = cache.get(uuid);
                    if (profile.isPresent()) return profile.get().name();
                }
            }
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Failed to resolve name for {}", uuid, e);
        }
        return uuid.toString();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /** Returns true if the command is enabled in config (defaults to true). */
    private static boolean isEnabled(String command) {
        try {
            return ConfigManager.getInstance().isCommandEnabled(command);
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Failed to check isCommandEnabled for '{}' — defaulting to enabled", command, e);
            return true;
        }
    }

    /** Check if source has permission */
    private static boolean hasPermission(CommandSourceStack source, String permission) {
        // Console always has permission
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return true;
        }
        return com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
            player.getUUID(), permission);
    }

    // ── Command executors ───────────────────────────────────────────────────

    /**
     * View another player's inventory (read-only or editable).
     */
    private static int viewInventory(CommandContext<CommandSourceStack> ctx, boolean editable) throws CommandSyntaxException {
        ServerPlayer viewer = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");

        // Don't allow viewing own inventory (use regular 'E' key)
        if (viewer.getUUID().equals(target.getUUID())) {
            viewer.sendSystemMessage(MessageUtil.error("commands.neoessentials.invsee.self"));
            return 0;
        }

        if (editable) {
            // ── Anti-duplication: enforce single-editor lock ──
            UUID existingViewer = activeInvEdits.get(target.getUUID());
            if (existingViewer != null && !existingViewer.equals(viewer.getUUID())) {
                // Another editor already has this inventory open
                String editorName = getPlayerName(viewer.level().getServer(), existingViewer);
                viewer.sendSystemMessage(MessageUtil.error(
                    "commands.neoessentials.invsee.concurrent_edit",
                    target.getName().getString(), editorName));
                InventoryAuditLogger.log(
                    viewer.getName().getString(), InventoryAuditLogger.EDIT_BLOCKED,
                    target.getName().getString(),
                    "Already being edited by " + editorName);
                LOGGER.warn("{} was blocked from editing {}'s inventory — already being edited by {}",
                    viewer.getName().getString(), target.getName().getString(), editorName);
                return 0;
            }

            // Acquire the edit lock
            activeInvEdits.put(target.getUUID(), viewer.getUUID());

            openEditableInventory(viewer, target);
            viewer.sendSystemMessage(MessageUtil.success(
                "commands.neoessentials.invsee.edit_success", target.getName().getString()));
            InventoryAuditLogger.log(
                viewer.getName().getString(), InventoryAuditLogger.INV_EDIT_OPENED,
                target.getName().getString(),
                "Editable inventory opened");
            LOGGER.info("{} is viewing and editing {}'s inventory", viewer.getName().getString(), target.getName().getString());
        } else {
            openReadOnlyInventory(viewer, target);
            viewer.sendSystemMessage(MessageUtil.success(
                "commands.neoessentials.invsee.view_success", target.getName().getString()));
            InventoryAuditLogger.log(
                viewer.getName().getString(), InventoryAuditLogger.INV_VIEWED,
                target.getName().getString(),
                "Read-only inventory view opened");
            LOGGER.info("{} is viewing {}'s inventory (read-only)", viewer.getName().getString(), target.getName().getString());
        }

        return 1;
    }

    /**
     * View another player's ender chest (read-only or editable).
     */
    private static int viewEnderChest(CommandContext<CommandSourceStack> ctx, boolean editable) throws CommandSyntaxException {
        ServerPlayer viewer = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");

        if (editable) {
            // ── Anti-duplication: enforce single-editor lock ──
            UUID existingViewer = activeEcEdits.get(target.getUUID());
            if (existingViewer != null && !existingViewer.equals(viewer.getUUID())) {
                String editorName = getPlayerName(viewer.level().getServer(), existingViewer);
                viewer.sendSystemMessage(MessageUtil.error(
                    "commands.neoessentials.ec.concurrent_edit",
                    target.getName().getString(), editorName));
                InventoryAuditLogger.log(
                    viewer.getName().getString(), InventoryAuditLogger.EDIT_BLOCKED,
                    target.getName().getString(),
                    "Ender chest already being edited by " + editorName);
                LOGGER.warn("{} was blocked from editing {}'s ender chest — already being edited by {}",
                    viewer.getName().getString(), target.getName().getString(), editorName);
                return 0;
            }

            // Acquire the edit lock
            activeEcEdits.put(target.getUUID(), viewer.getUUID());

            viewer.openMenu(new SimpleMenuProvider(
                (id, playerInventory, player) -> ChestMenu.threeRows(id, playerInventory, target.getEnderChestInventory()),
                Component.literal(target.getName().getString() + "'s Ender Chest (Editable)")
            ));
            viewer.sendSystemMessage(MessageUtil.success(
                "commands.neoessentials.ec.edit_success", target.getName().getString()));
            InventoryAuditLogger.log(
                viewer.getName().getString(), InventoryAuditLogger.EC_EDIT_OPENED,
                target.getName().getString(),
                "Editable ender chest opened");
            LOGGER.info("{} is viewing and editing {}'s ender chest", viewer.getName().getString(), target.getName().getString());
        } else {
            openReadOnlyEnderChest(viewer, target);
            viewer.sendSystemMessage(MessageUtil.success(
                "commands.neoessentials.ec.view_success", target.getName().getString()));
            InventoryAuditLogger.log(
                viewer.getName().getString(), InventoryAuditLogger.EC_VIEWED,
                target.getName().getString(),
                "Read-only ender chest view opened");
            LOGGER.info("{} is viewing {}'s ender chest (read-only)", viewer.getName().getString(), target.getName().getString());
        }

        return 1;
    }

    /**
     * Open a read-only view of another player's inventory (item copies — no changes possible).
     * Uses locked slots that cannot be picked up, preventing item duplication.
     */
    private static void openReadOnlyInventory(ServerPlayer viewer, ServerPlayer target) {
        int invSize = target.getInventory().getContainerSize();
        boolean sixRow = invSize > 27;
        int displaySlots = sixRow ? 54 : 27;
        int chestRows = sixRow ? 6 : 3;

        // Snapshot: copies so the target's actual inventory is never touched
        net.minecraft.world.SimpleContainer snapshot = new net.minecraft.world.SimpleContainer(displaySlots);
        for (int i = 0; i < Math.min(invSize, displaySlots); i++) {
            snapshot.setItem(i, target.getInventory().getItem(i).copy());
        }

        net.minecraft.world.inventory.MenuType<?> menuType = sixRow
            ? net.minecraft.world.inventory.MenuType.GENERIC_9x6
            : net.minecraft.world.inventory.MenuType.GENERIC_9x3;
        final int rows = chestRows;

        viewer.openMenu(new SimpleMenuProvider(
            (id, playerInv, p) -> buildReadOnlyMenu(id, playerInv, snapshot, rows, menuType),
            Component.literal(target.getName().getString() + "'s Inventory (Read-Only)")
        ));
    }

    /**
     * Open an editable view of another player's inventory (changes are live and persisted).
     */
    private static void openEditableInventory(ServerPlayer viewer, ServerPlayer target) {
        viewer.openMenu(new SimpleMenuProvider(
            (id, playerInventory, player) -> new PlayerInventoryContainerMenu(id, playerInventory, target),
            PlayerInventoryContainerMenu.getTitle(target)
        ));
    }

    /**
     * Open a read-only view of another player's ender chest (item copies — no changes possible).
     * Uses locked slots that cannot be picked up, preventing item duplication.
     */
    private static void openReadOnlyEnderChest(ServerPlayer viewer, ServerPlayer target) {
        net.minecraft.world.SimpleContainer snapshot = new net.minecraft.world.SimpleContainer(27);
        for (int i = 0; i < 27; i++) {
            snapshot.setItem(i, target.getEnderChestInventory().getItem(i).copy());
        }
        viewer.openMenu(new SimpleMenuProvider(
            (id, playerInv, p) -> buildReadOnlyMenu(id, playerInv, snapshot, 3,
                net.minecraft.world.inventory.MenuType.GENERIC_9x3),
            Component.literal(target.getName().getString() + "'s Ender Chest (Read-Only)")
        ));
    }

    // ── Read-only menu builder ───────────────────────────────────────────────

    /**
     * Build an {@link net.minecraft.world.inventory.AbstractContainerMenu} whose upper
     * {@code rows*9} slots are <b>locked</b> (cannot be picked up or placed into),
     * backed by {@code snapshot}.  The viewer's own inventory and hotbar are appended
     * as normal interactive slots so the GUI renders correctly.
     *
     * <p>This is the duplication-safe replacement for {@code ChestMenu.threeRows} /
     * {@code ChestMenu.sixRows}: the old approach used a {@code SimpleContainer} filled
     * with item copies but opened it through a standard {@code ChestMenu}, which allows
     * items to be freely moved out of the container — letting a viewer steal copies while
     * the original items remained in the target's inventory.
     */
    private static net.minecraft.world.inventory.AbstractContainerMenu buildReadOnlyMenu(
            int menuId,
            net.minecraft.world.entity.player.Inventory viewerInv,
            net.minecraft.world.Container snapshot,
            int rows,
            net.minecraft.world.inventory.MenuType<?> menuType) {

        return new net.minecraft.world.inventory.AbstractContainerMenu(menuType, menuId) {
            {
                // ── Locked display slots (target's items — not takeable) ──────
                int displaySlots = rows * 9;
                for (int i = 0; i < displaySlots; i++) {
                    int sx = 8 + (i % 9) * 18;
                    int sy = 18 + (i / 9) * 18;
                    // Anonymous Slot override: disallow pickup and placement
                    addSlot(new net.minecraft.world.inventory.Slot(snapshot, i, sx, sy) {
                        @Override
                        public boolean mayPickup(net.minecraft.world.entity.player.Player player) {
                            return false; // prevent item theft / duplication
                        }
                        @Override
                        public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                            return false; // prevent placing into display slots
                        }
                    });
                }
                // ── Viewer's main inventory (rows 1-3) ────────────────────────
                // Standard NeoForge positions: chest rows end at y = 18 + rows*18,
                // then 14 px gap before viewer inventory.
                int invTopY = 18 + rows * 18 + 14;
                for (int i = 0; i < 27; i++) {
                    addSlot(new net.minecraft.world.inventory.Slot(
                        viewerInv, i + 9, 8 + (i % 9) * 18, invTopY + (i / 9) * 18));
                }
                // ── Viewer's hotbar ───────────────────────────────────────────
                int hotbarY = invTopY + 3 * 18 + 4;
                for (int i = 0; i < 9; i++) {
                    addSlot(new net.minecraft.world.inventory.Slot(
                        viewerInv, i, 8 + i * 18, hotbarY));
                }
            }

            @Override
            public boolean stillValid(net.minecraft.world.entity.player.Player player) {
                return true;
            }

            @Override
            public net.minecraft.world.item.ItemStack quickMoveStack(
                    net.minecraft.world.entity.player.Player player, int index) {
                // Shift-click is disabled entirely for read-only views
                return net.minecraft.world.item.ItemStack.EMPTY;
            }
        };
    }

    /**
     * Return the online player's name for the given UUID, or the UUID string if offline.
     */
    private static String getPlayerName(net.minecraft.server.MinecraftServer server, UUID uuid) {
        if (server == null) return resolveNameFromServer(uuid);
        ServerPlayer p = server.getPlayerList().getPlayer(uuid);
        return p != null ? p.getName().getString() : resolveNameFromServer(uuid);
    }
}

