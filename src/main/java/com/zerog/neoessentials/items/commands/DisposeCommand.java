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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

/**
 * Provides item disposal functionality through a temporary trash GUI.
 * 
 * <p>Commands:</p>
 * <ul>
 *   <li>/dispose - Open trash GUI</li>
 *   <li>/dispose confirm - Confirm pending disposal</li>
 *   <li>/dispose cancel - Cancel and restore items</li>
 *   <li>/trash - Alias for /dispose</li>
 * </ul>
 * 
 * <p>Permissions:</p>
 * <ul>
 *   <li>neoessentials.item.dispose - Use dispose/trash commands</li>
 * </ul>
 * 
 * <p>Configuration:</p>
 * <ul>
 *   <li>commands.dispose.enabled - Enable/disable command</li>
 * </ul>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>27-slot trash GUI for item disposal</li>
 *   <li>Pending disposal tracking for safety</li>
 *   <li>Automatic restoration on disconnect</li>
 *   <li>Audit logging for administrative oversight</li>
 * </ul>
 */
public class DisposeCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(DisposeCommand.class);
    
    // Store pending disposals: UUID -> SimpleContainer
    private static final Map<java.util.UUID, SimpleContainer> pendingDisposals = new HashMap<>();

    /**
     * Restore pending items to player (used on disconnect or cancel).
     * Logs restoration for audit purposes.
     * 
     * @param player The player whose items to restore
     */
    public static void restorePendingItems(ServerPlayer player) {
        SimpleContainer container = pendingDisposals.remove(player.getUUID());
        if (container != null) {
            int itemCount = 0;
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (!container.getItem(i).isEmpty()) {
                    player.getInventory().placeItemBackInInventory(container.getItem(i));
                    itemCount++;
                }
            }
            player.sendSystemMessage(MessageUtil.info("commands.neoessentials.dispose.restored"));
            
            // Log restoration for audit trail
            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Restored {} pending disposal items for player {} (disconnect/cancel)", 
                itemCount, player.getName().getString());
        }
    }

    /**
     * Register the /dispose and /trash commands.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (ConfigManager.getInstance().isCommandEnabled("dispose")) {
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
                        ServerPlayer player = permResult.getPlayer();
                        SimpleContainer container = pendingDisposals.remove(player.getUUID());
                        if (container != null) {
                            // Count items being disposed
                            int itemCount = 0;
                            for (int i = 0; i < container.getContainerSize(); i++) {
                                if (!container.getItem(i).isEmpty()) {
                                    itemCount++;
                                }
                            }
                            
                            // Log disposal confirmation for audit trail
                            NeoLog.info(LOGGER, LogCategory.GENERAL, "Player {} confirmed disposal of {} items", 
                                player.getName().getString(), itemCount);
                            
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
                            // Count and return items to player
                            int itemCount = 0;
                            for (int i = 0; i < container.getContainerSize(); i++) {
                                if (!container.getItem(i).isEmpty()) {
                                    player.getInventory().placeItemBackInInventory(container.getItem(i));
                                    itemCount++;
                                }
                            }
                            
                            // Log cancellation for audit trail
                            NeoLog.debug(LOGGER, LogCategory.GENERAL, "Player {} cancelled disposal and restored {} items", 
                                player.getName().getString(), itemCount);
                            
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
        }
        if (ConfigManager.getInstance().isCommandEnabled("trash")) {
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
