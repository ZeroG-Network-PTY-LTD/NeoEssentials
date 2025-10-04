package com.zerog.neoessentials.items.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import net.minecraft.world.SimpleContainer;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class DisposeCommand {
    // Store pending disposals: UUID -> SimpleContainer
    private static final Map<java.util.UUID, SimpleContainer> pendingDisposals = new HashMap<>();

    /**
     * Restore pending items to player (used on disconnect or cancel).
     */
    public static void restorePendingItems(ServerPlayer player) {
        SimpleContainer container = pendingDisposals.remove(player.getUUID());
        if (container != null) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (!container.getItem(i).isEmpty()) {
                    player.getInventory().placeItemBackInInventory(container.getItem(i));
                }
            }
            player.sendSystemMessage(MessageUtil.info("commands.neoessentials.dispose.restored"));
        }
    }

    /**
     * Register the /dispose and /trash commands.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("dispose")) return;
        dispatcher.register(
            Commands.literal("dispose")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                .then(Commands.literal("confirm")
                    .executes(ctx -> {
                        PermissionValidator.PermissionResult permResult = 
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.item.dispose");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        if (pendingDisposals.remove(permResult.getPlayer().getUUID()) != null) {
                            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.dispose.confirmed"), false);
                        } else {
                            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.dispose.nothing_pending"));
                        }
                        return 1;
                    })
                )
                .then(Commands.literal("cancel")
                    .executes(ctx -> {
                        PermissionValidator.PermissionResult permResult = 
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.item.dispose");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        ServerPlayer player = permResult.getPlayer();
                        SimpleContainer container = pendingDisposals.remove(player.getUUID());
                        if (container != null) {
                            // Return items to player
                            for (int i = 0; i < container.getContainerSize(); i++) {
                                if (!container.getItem(i).isEmpty()) {
                                    player.getInventory().placeItemBackInInventory(container.getItem(i));
                                }
                            }
                            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.dispose.restored"), false);
                        } else {
                            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.dispose.nothing_pending"));
                        }
                        return 1;
                    })
                )
                .executes(ctx -> {
                    PermissionValidator.PermissionResult permResult = 
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.item.dispose");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    disposeItem(permResult.getPlayer());
                    ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.dispose.opened"), false);
                    return 1;
                })
        );
        dispatcher.register(
            Commands.literal("trash")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                .executes(ctx -> {
                    PermissionValidator.PermissionResult permResult = 
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.item.dispose");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    disposeItem(permResult.getPlayer());
                    ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.dispose.opened"), false);
                    return 1;
                })
        );
    }

    /**
     * Opens a temporary chest GUI for the player. Items placed in the chest are deleted on close.
     */
    public static void disposeItem(ServerPlayer player) {
        SimpleContainer container = new SimpleContainer(27);
        pendingDisposals.put(player.getUUID(), container);
        MenuProvider provider = new MenuProvider() {
            @Override
            public AbstractContainerMenu createMenu(int windowId, Inventory playerInv, net.minecraft.world.entity.player.Player playerEntity) {
                return ChestMenu.threeRows(windowId, playerInv, container);
            }

            @Override
            public Component getDisplayName() {
                return MessageUtil.component("commands.neoessentials.dispose.title");
            }
        };
        player.openMenu(provider);
    }
}
