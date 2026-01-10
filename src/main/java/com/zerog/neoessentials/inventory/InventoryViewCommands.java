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
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * Commands for viewing and editing other players' inventories
 *
 * Commands:
 * - /invsee player - View another player's inventory (read-only)
 * - /invseeedit player - View and edit another player's inventory
 * - /enderchest player - View another player's ender chest (read-only)
 * - /enderchestedit player - View and edit another player's ender chest
 *
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

        // Aliases
        dispatcher.register(Commands.literal("inv").redirect(dispatcher.getRoot().getChild("invsee")));
        dispatcher.register(Commands.literal("ec").redirect(dispatcher.getRoot().getChild("enderchest")));
        dispatcher.register(Commands.literal("ecedit").redirect(dispatcher.getRoot().getChild("enderchestdit")));

        LOGGER.info("Registered inventory view commands: /invsee, /invseeedit, /enderchest, /enderchest");
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
            viewer.sendSystemMessage(MessageUtil.error("You cannot view your own inventory with this command!"));
            return 0;
        }

        // Open the inventory view
        if (editable) {
            openEditableInventory(viewer, target);
            viewer.sendSystemMessage(MessageUtil.success("Opening editable inventory of " + target.getName().getString()));
            LOGGER.info("{} is viewing and editing {}'s inventory", viewer.getName().getString(), target.getName().getString());
        } else {
            openReadOnlyInventory(viewer, target);
            viewer.sendSystemMessage(MessageUtil.success("Viewing inventory of " + target.getName().getString()));
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
            viewer.sendSystemMessage(MessageUtil.success("Opening editable ender chest of " + target.getName().getString()));
            LOGGER.info("{} is viewing and editing {}'s ender chest", viewer.getName().getString(), target.getName().getString());
        } else {
            // For read-only, we create a copy of the ender chest
            openReadOnlyEnderChest(viewer, target);
            viewer.sendSystemMessage(MessageUtil.success("Viewing ender chest of " + target.getName().getString()));
            LOGGER.info("{} is viewing {}'s ender chest (read-only)", viewer.getName().getString(), target.getName().getString());
        }

        return 1;
    }

    /**
     * Open a read-only view of another player's inventory
     */
    private static void openReadOnlyInventory(ServerPlayer viewer, ServerPlayer target) {
        // Create a copy of the target's inventory that won't sync changes back
        net.minecraft.world.Container inventoryCopy = new net.minecraft.world.SimpleContainer(target.getInventory().getContainerSize());

        // Copy all items
        for (int i = 0; i < target.getInventory().getContainerSize(); i++) {
            inventoryCopy.setItem(i, target.getInventory().getItem(i).copy());
        }

        // Open as a chest menu (read-only)
        viewer.openMenu(new SimpleMenuProvider(
            (id, playerInventory, player) -> {
                if (inventoryCopy.getContainerSize() <= 27) {
                    return ChestMenu.threeRows(id, playerInventory, inventoryCopy);
                } else {
                    return ChestMenu.sixRows(id, playerInventory, inventoryCopy);
                }
            },
            Component.literal(target.getName().getString() + "'s Inventory (Read-Only)")
        ));
    }

    /**
     * Open an editable view of another player's inventory
     */
    private static void openEditableInventory(ServerPlayer viewer, ServerPlayer target) {
        // Open the target's actual inventory container (changes will sync)
        viewer.openMenu(new MenuProvider() {
            @Nonnull
            @Override
            public Component getDisplayName() {
                return Component.literal(target.getName().getString() + "'s Inventory (Editable)");
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, @Nonnull Inventory playerInventory, @Nonnull Player player) {
                // Use the target player's inventory directly
                return ChestMenu.sixRows(id, playerInventory, target.getInventory());
            }
        });
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

