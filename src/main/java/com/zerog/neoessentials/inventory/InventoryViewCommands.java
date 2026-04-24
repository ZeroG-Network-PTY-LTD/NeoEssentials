package com.zerog.neoessentials.inventory;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Commands for viewing and editing other players' inventories
 * Commands:
 * - /invsee player - View another player's inventory (read-only)
 * - /invseeedit player - View and edit another player's inventory
 * - /enderchest player - View another player's ender chest (read-only)
 * - /enderchestedit player - View and edit another player's ender chest
 * Permissions:
 * - neoessentials.invsee - View other players' inventories
 * - neoessentials.invsee.edit - Edit other players' inventories
 * - neoessentials.enderchest - View other players' ender chests
 * - neoessentials.enderchest.edit - Edit other players' ender chests
 */
public class InventoryViewCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryViewCommands.class);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /invsee <player> - View inventory (read-only)
        dispatcher.register(
            Commands.literal("invsee")
                .requires(source -> hasPermission(source, "neoessentials.invsee"))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> viewInventory(ctx, false))
                )
        );

        // /invseeedit <player> - View and edit inventory
        dispatcher.register(
            Commands.literal("invseeedit")
                .requires(source -> hasPermission(source, "neoessentials.invsee.edit"))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> viewInventory(ctx, true))
                )
        );

        // /enderchest <player> - View ender chest (read-only)
        dispatcher.register(
            Commands.literal("enderchest")
                .requires(source -> hasPermission(source, "neoessentials.enderchest"))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> viewEnderChest(ctx, false))
                )
        );

        // /enderchestedit <player> - View and edit ender chest
        dispatcher.register(
            Commands.literal("enderchestedit")
                .requires(source -> hasPermission(source, "neoessentials.enderchest.edit"))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> viewEnderChest(ctx, true))
                )
        );

        // Aliases — registered as full commands (NOT as redirects) so that
        // Brigadier applies the requires() check before any dispatch occurs.
        // Using .redirect() alone does NOT re-evaluate the target's requires().
        dispatcher.register(
            Commands.literal("inv")
                .requires(source -> hasPermission(source, "neoessentials.invsee"))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> viewInventory(ctx, false))
                )
        );
        dispatcher.register(
            Commands.literal("ec")
                .requires(source -> hasPermission(source, "neoessentials.enderchest"))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> viewEnderChest(ctx, false))
                )
        );
        dispatcher.register(
            Commands.literal("ecedit")
                .requires(source -> hasPermission(source, "neoessentials.enderchest.edit"))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> viewEnderChest(ctx, true))
                )
        );

        LOGGER.info("Registered inventory view commands: /invsee, /invseeedit, /enderchest, /enderchestedit (/inv, /ec, /ecedit)");
    }

    /**
     * Check if source has permission
     */
    private static boolean hasPermission(CommandSourceStack source, String permission) {
        // Console always has permission
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return true;
        }

        return com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
            player.getUUID(), permission);
    }

    /**
     * View another player's inventory
     */
    private static int viewInventory(CommandContext<CommandSourceStack> ctx, boolean editable) throws CommandSyntaxException {
        ServerPlayer viewer = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");

        // Don't allow viewing own inventory (use regular 'E' key)
        if (viewer.getUUID().equals(target.getUUID())) {
            viewer.sendSystemMessage(MessageUtil.error("commands.neoessentials.invsee.self"));
            return 0;
        }

        // Open the inventory view
        if (editable) {
            openEditableInventory(viewer, target);
            viewer.sendSystemMessage(MessageUtil.success("commands.neoessentials.invsee.edit_success", target.getName().getString()));
            LOGGER.info("{} is viewing and editing {}'s inventory", viewer.getName().getString(), target.getName().getString());
        } else {
            openReadOnlyInventory(viewer, target);
            viewer.sendSystemMessage(MessageUtil.success("commands.neoessentials.invsee.view_success", target.getName().getString()));
            LOGGER.info("{} is viewing {}'s inventory (read-only)", viewer.getName().getString(), target.getName().getString());
        }

        return 1;
    }

    /**
     * View another player's ender chest
     */
    private static int viewEnderChest(CommandContext<CommandSourceStack> ctx, boolean editable) throws CommandSyntaxException {
        ServerPlayer viewer = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");

        // Open the ender chest view
        if (editable) {
            viewer.openMenu(new SimpleMenuProvider(
                (id, playerInventory, player) -> ChestMenu.threeRows(id, playerInventory, target.getEnderChestInventory()),
                Component.literal(target.getName().getString() + "'s Ender Chest (Editable)")
            ));
            viewer.sendSystemMessage(MessageUtil.success("commands.neoessentials.ec.edit_success", target.getName().getString()));
            LOGGER.info("{} is viewing and editing {}'s ender chest", viewer.getName().getString(), target.getName().getString());
        } else {
            // For read-only, we create a copy of the ender chest
            openReadOnlyEnderChest(viewer, target);
            viewer.sendSystemMessage(MessageUtil.success("commands.neoessentials.ec.view_success", target.getName().getString()));
            LOGGER.info("{} is viewing {}'s ender chest (read-only)", viewer.getName().getString(), target.getName().getString());
        }

        return 1;
    }

    /**
     * Open a read-only view of another player's inventory
     */
    private static void openReadOnlyInventory(ServerPlayer viewer, ServerPlayer target) {
        int invSize = target.getInventory().getContainerSize();
        if (invSize <= 27) {
            // 27 or fewer: use threeRows
            net.minecraft.world.Container inventoryCopy = new net.minecraft.world.SimpleContainer(invSize);
            for (int i = 0; i < invSize; i++) {
                inventoryCopy.setItem(i, target.getInventory().getItem(i).copy());
            }
            viewer.openMenu(new SimpleMenuProvider(
                (id, playerInventory, player) -> ChestMenu.threeRows(id, playerInventory, inventoryCopy),
                Component.literal(target.getName().getString() + "'s Inventory (Read-Only)")
            ));
        } else {
            // More than 27: always use 54 slots (sixRows)
            net.minecraft.world.SimpleContainer inventoryCopy = new net.minecraft.world.SimpleContainer(54);
            for (int i = 0; i < invSize; i++) {
                inventoryCopy.setItem(i, target.getInventory().getItem(i).copy());
            }
            for (int i = invSize; i < 54; i++) {
                inventoryCopy.setItem(i, net.minecraft.world.item.ItemStack.EMPTY.copy());
            }
            viewer.openMenu(new SimpleMenuProvider(
                (id, playerInventory, player) -> ChestMenu.sixRows(id, playerInventory, inventoryCopy),
                Component.literal(target.getName().getString() + "'s Inventory (Read-Only)")
            ));
        }
    }

    /**
     * Open an editable view of another player's inventory (changes are persisted and live)
     */
    private static void openEditableInventory(ServerPlayer viewer, ServerPlayer target) {
        int invSize = target.getInventory().getContainerSize();
        // Use custom container menu for any inventory size
        viewer.openMenu(new SimpleMenuProvider(
            (id, playerInventory, player) -> new PlayerInventoryContainerMenu(id, playerInventory, target),
            PlayerInventoryContainerMenu.getTitle(target)
        ));
    }

    /**
     * Open a read-only view of another player's ender chest
     */
    private static void openReadOnlyEnderChest(ServerPlayer viewer, ServerPlayer target) {
        // Create a copy of the ender chest
        net.minecraft.world.Container enderChestCopy = new net.minecraft.world.SimpleContainer(27);

        // Copy all items
        for (int i = 0; i < 27; i++) {
            enderChestCopy.setItem(i, target.getEnderChestInventory().getItem(i).copy());
        }

        // Open as a chest menu (read-only)
        viewer.openMenu(new SimpleMenuProvider(
            (id, playerInventory, player) -> ChestMenu.threeRows(id, playerInventory, enderChestCopy),
            Component.literal(target.getName().getString() + "'s Ender Chest (Read-Only)")
        ));
    }
}

