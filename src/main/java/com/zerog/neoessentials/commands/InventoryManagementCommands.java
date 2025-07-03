package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.utils.TextUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implements enhanced inventory management commands for NeoEssentials.
 * <p>
 * This system provides advanced inventory management functionality including:
 * <ul>
 *   <li>Clear player inventories with confirmation system</li>
 *   <li>View and edit other player inventories</li>
 *   <li>Inventory inspection for administrative purposes</li>
 *   <li>Permission-based access control</li>
 * </ul>
 * </p>
 * 
 * @author ZeroG
 * @since 1.0.2.94
 */
public class InventoryManagementCommands {

    // Track players who have confirmation toggle enabled
    private static final Map<UUID, Boolean> clearConfirmationEnabled = new HashMap<>();

    /**
     * Registers all inventory management commands with the dispatcher.
     *
     * @param dispatcher The command dispatcher
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerClearInventoryCommand(dispatcher);
        registerClearInventoryConfirmToggleCommand(dispatcher);
        registerInvSeeCommand(dispatcher);
        registerDisposalCommand(dispatcher);
    }

    /**
     * Registers the clearinventory command.
     * Usage: /clearinventory [player] [confirm]
     *
     * @param dispatcher The command dispatcher
     */
    private static void registerClearInventoryCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> clearInventoryCommand = Commands.literal("clearinventory")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.clearinventory"))
                // Clear own inventory
                .executes(InventoryManagementCommands::executeClearOwnInventory)
                // Clear own inventory with confirmation override
                .then(Commands.argument("confirm", BoolArgumentType.bool())
                        .executes(InventoryManagementCommands::executeClearOwnInventoryWithConfirm))
                // Clear other player's inventory
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.clearinventory.others"))
                        .executes(InventoryManagementCommands::executeClearOtherInventory)
                        .then(Commands.argument("confirm", BoolArgumentType.bool())
                                .executes(InventoryManagementCommands::executeClearOtherInventoryWithConfirm)));

        dispatcher.register(clearInventoryCommand);
        
        // Register alias
        dispatcher.register(Commands.literal("ci")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.clearinventory"))
                .executes(InventoryManagementCommands::executeClearOwnInventory)
                .then(Commands.argument("confirm", BoolArgumentType.bool())
                        .executes(InventoryManagementCommands::executeClearOwnInventoryWithConfirm))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> CommandManager.hasPermission(source, "neoessentials.clearinventory.others"))
                        .executes(InventoryManagementCommands::executeClearOtherInventory)
                        .then(Commands.argument("confirm", BoolArgumentType.bool())
                                .executes(InventoryManagementCommands::executeClearOtherInventoryWithConfirm))));
    }

    /**
     * Registers the clearinventoryconfirmtoggle command.
     * Usage: /clearinventoryconfirmtoggle
     *
     * @param dispatcher The command dispatcher
     */
    private static void registerClearInventoryConfirmToggleCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> toggleCommand = Commands.literal("clearinventoryconfirmtoggle")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.clearinventory"))
                .executes(InventoryManagementCommands::executeToggleConfirmation);

        dispatcher.register(toggleCommand);
    }

    /**
     * Registers the invsee command.
     * Usage: /invsee <player>
     *
     * @param dispatcher The command dispatcher
     */
    private static void registerInvSeeCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> invSeeCommand = Commands.literal("invsee")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.invsee"))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(InventoryManagementCommands::executeInvSee));

        dispatcher.register(invSeeCommand);
    }

    /**
     * Registers the disposal/trash command.
     * Usage: /disposal or /trash
     *
     * @param dispatcher The command dispatcher
     */
    private static void registerDisposalCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> disposalCommand = Commands.literal("disposal")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.disposal"))
                .executes(InventoryManagementCommands::executeDisposal);

        dispatcher.register(disposalCommand);
        
        // Register alias
        dispatcher.register(Commands.literal("trash")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.disposal"))
                .executes(InventoryManagementCommands::executeDisposal));
    }

    /**
     * Executes clear own inventory command.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private static int executeClearOwnInventory(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeClearInventory(context, context.getSource().getPlayerOrException(), false);
    }

    /**
     * Executes clear own inventory command with confirmation override.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private static int executeClearOwnInventoryWithConfirm(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        boolean confirm = BoolArgumentType.getBool(context, "confirm");
        return executeClearInventory(context, context.getSource().getPlayerOrException(), confirm);
    }

    /**
     * Executes clear other player's inventory command.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private static int executeClearOtherInventory(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        return executeClearInventory(context, target, false);
    }

    /**
     * Executes clear other player's inventory command with confirmation override.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private static int executeClearOtherInventoryWithConfirm(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        boolean confirm = BoolArgumentType.getBool(context, "confirm");
        return executeClearInventory(context, target, confirm);
    }

    /**
     * Core method to execute inventory clearing with confirmation logic.
     *
     * @param context The command context
     * @param target The target player
     * @param confirmOverride Whether to override confirmation requirement
     * @return 1 if successful, 0 otherwise
     */
    private static int executeClearInventory(CommandContext<CommandSourceStack> context, ServerPlayer target, boolean confirmOverride) {
        CommandSourceStack source = context.getSource();
        ServerPlayer executor;
        
        try {
            executor = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            // Console execution - always allow without confirmation
            clearPlayerInventory(target);
            source.sendSuccess(() -> Component.literal("§aCleared inventory of " + target.getName().getString()), false);
            return 1;
        }

        boolean isSelf = executor.getUUID().equals(target.getUUID());
        boolean needsConfirmation = !confirmOverride && clearConfirmationEnabled.getOrDefault(executor.getUUID(), true);

        // Check if confirmation is needed
        if (needsConfirmation && !confirmOverride) {
            source.sendFailure(Component.literal("§cThis will permanently delete all items in " + 
                (isSelf ? "your" : target.getName().getString() + "'s") + " inventory!"));
            source.sendFailure(Component.literal("§eUse §6/clearinventory " + 
                (isSelf ? "" : target.getName().getString() + " ") + "true §eto confirm."));
            source.sendFailure(Component.literal("§7Or use §6/clearinventoryconfirmtoggle §7to disable confirmation."));
            return 0;
        }

        // Clear the inventory
        int itemsCleared = clearPlayerInventory(target);
        
        if (isSelf) {
            source.sendSuccess(() -> Component.literal("§aYour inventory has been cleared. §7(" + itemsCleared + " items removed)"), false);
        } else {
            source.sendSuccess(() -> Component.literal("§aCleared inventory of " + target.getName().getString() + 
                ". §7(" + itemsCleared + " items removed)"), false);
            target.sendSystemMessage(Component.literal("§cYour inventory has been cleared by " + executor.getName().getString()));
        }

        return 1;
    }

    /**
     * Executes the confirmation toggle command.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private static int executeToggleConfirmation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        
        boolean currentSetting = clearConfirmationEnabled.getOrDefault(player.getUUID(), true);
        boolean newSetting = !currentSetting;
        
        clearConfirmationEnabled.put(player.getUUID(), newSetting);
        
        if (newSetting) {
            source.sendSuccess(() -> Component.literal("§aClear inventory confirmation is now §eENABLED§a."), false);
            source.sendSuccess(() -> Component.literal("§7You will be asked to confirm before clearing inventory."), false);
        } else {
            source.sendSuccess(() -> Component.literal("§cClear inventory confirmation is now §eDISABLED§c."), false);
            source.sendSuccess(() -> Component.literal("§7Inventory will be cleared immediately without confirmation."), false);
        }
        
        return 1;
    }

    /**
     * Executes the invsee command.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private static int executeInvSee(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer viewer = source.getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        
        if (viewer.getUUID().equals(target.getUUID())) {
            source.sendFailure(Component.literal("§cYou cannot view your own inventory with this command."));
            return 0;
        }
        
        // Open the target player's inventory for viewing
        openPlayerInventory(viewer, target);
        
        source.sendSuccess(() -> Component.literal("§aOpened inventory of " + target.getName().getString()), false);
        return 1;
    }

    /**
     * Executes the disposal command.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private static int executeDisposal(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        
        // Create a disposal container (empty chest-like interface)
        SimpleContainer disposalContainer = new SimpleContainer(54) {
            @Override
            public void setChanged() {
                // Auto-delete items when container changes
                this.clearContent();
            }
        };
        
        // Open the disposal interface
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("§cDisposal - Items will be deleted!");
            }
            
            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                return ChestMenu.sixRows(containerId, playerInventory, disposalContainer);
            }
        });
        
        source.sendSuccess(() -> Component.literal("§aOpened disposal interface. §cItems placed will be permanently deleted!"), false);
        return 1;
    }

    /**
     * Clears a player's inventory and returns the number of items removed.
     *
     * @param player The player whose inventory to clear
     * @return The number of items removed
     */
    private static int clearPlayerInventory(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        int itemsCleared = 0;
        
        // Clear main inventory
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                itemsCleared += stack.getCount();
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
        
        // Clear armor slots
        for (int i = 0; i < inventory.armor.size(); i++) {
            ItemStack stack = inventory.armor.get(i);
            if (!stack.isEmpty()) {
                itemsCleared += stack.getCount();
                inventory.armor.set(i, ItemStack.EMPTY);
            }
        }
        
        // Clear offhand
        ItemStack offhand = inventory.offhand.get(0);
        if (!offhand.isEmpty()) {
            itemsCleared += offhand.getCount();
            inventory.offhand.set(0, ItemStack.EMPTY);
        }
        
        // Update the inventory
        inventory.setChanged();
        player.inventoryMenu.broadcastChanges();
        
        return itemsCleared;
    }

    /**
     * Opens a target player's inventory for viewing by another player.
     *
     * @param viewer The player who will view the inventory
     * @param target The player whose inventory will be viewed
     */
    private static void openPlayerInventory(ServerPlayer viewer, ServerPlayer target) {
        // Create a container that mirrors the target's inventory
        SimpleContainer viewContainer = new SimpleContainer(45); // 36 main + 9 hotbar
        Inventory targetInventory = target.getInventory();
        
        // Copy items from target's inventory to view container
        for (int i = 0; i < Math.min(targetInventory.getContainerSize(), viewContainer.getContainerSize()); i++) {
            viewContainer.setItem(i, targetInventory.getItem(i).copy());
        }
        
        // Open the viewing interface
        viewer.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("§6" + target.getName().getString() + "'s Inventory");
            }
            
            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                return new ChestMenu(MenuType.GENERIC_9x6, containerId, playerInventory, viewContainer, 6) {
                    @Override
                    public void removed(Player player) {
                        super.removed(player);
                        // Sync changes back to target player if viewer has edit permission
                        if (CommandManager.hasPermission(((ServerPlayer) player).createCommandSourceStack(), "neoessentials.invsee.edit")) {
                            syncInventoryChanges(viewContainer, targetInventory);
                        }
                    }
                };
            }
        });
    }

    /**
     * Syncs changes from the view container back to the target inventory.
     *
     * @param viewContainer The container that was being viewed
     * @param targetInventory The target player's actual inventory
     */
    private static void syncInventoryChanges(Container viewContainer, Inventory targetInventory) {
        for (int i = 0; i < Math.min(viewContainer.getContainerSize(), targetInventory.getContainerSize()); i++) {
            targetInventory.setItem(i, viewContainer.getItem(i).copy());
        }
        targetInventory.setChanged();
    }
}
