package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * Handles inventory GUI related commands like /workbench, /anvil, etc.
 */
public class InventoryCommands {

    /**
     * Registers all inventory commands with the command dispatcher
     * 
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register /workbench command
        dispatcher.register(
            Commands.literal("workbench")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.workbench"))
                .executes(this::executeWorkbench)
        );
        
        // Register /wb alias
        dispatcher.register(
            Commands.literal("wb")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.workbench"))
                .executes(this::executeWorkbench)
        );
        
        // Register /craft alias
        dispatcher.register(
            Commands.literal("craft")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.workbench"))
                .executes(this::executeWorkbench)
        );
        
        // Register /anvil command
        dispatcher.register(
            Commands.literal("anvil")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.anvil"))
                .executes(this::executeAnvil)
        );
        
        // Register /grindstone command
        dispatcher.register(
            Commands.literal("grindstone")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.grindstone"))
                .executes(this::executeGrindstone)
        );
        
        // Register /cartographytable command
        dispatcher.register(
            Commands.literal("cartographytable")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.cartographytable"))
                .executes(this::executeCartographyTable)
        );
        
        // Register /carttable alias
        dispatcher.register(
            Commands.literal("carttable")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.cartographytable"))
                .executes(this::executeCartographyTable)
        );
        
        // Register /loom command
        dispatcher.register(
            Commands.literal("loom")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.loom"))
                .executes(this::executeLoom)
        );
        
        // Register /smithingtable command
        dispatcher.register(
            Commands.literal("smithingtable")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.smithingtable"))
                .executes(this::executeSmithingTable)
        );
        
        // Register /smithtable alias
        dispatcher.register(
            Commands.literal("smithtable")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.smithingtable"))
                .executes(this::executeSmithingTable)
        );
          // Register /stonecutter command
        dispatcher.register(
            Commands.literal("stonecutter")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.stonecutter"))
                .executes(this::executeStonecutter)
        );
        
        // Register /enderchest command
        dispatcher.register(
            Commands.literal("enderchest")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.enderchest"))
                .executes(this::executeEnderchest)
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.enderchest.others"))
                    .executes(this::executeEnderchestOthers)
                )
        );
        
        // Register /echest alias
        dispatcher.register(
            Commands.literal("echest")
                .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.enderchest"))
                .executes(this::executeEnderchest)
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> PermissionUtil.hasPermission(source, "neoessentials.command.enderchest.others"))
                    .executes(this::executeEnderchestOthers)
                )
        );
        
        NeoEssentials.LOGGER.info("Registered inventory commands");
    }
    
    /**
     * Executes the /workbench command
     */
    private int executeWorkbench(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Create workbench container
        player.openMenu(new SimpleMenuProvider(
            (windowId, playerInv, playerEntity) -> new CraftingMenu(windowId, playerInv, ContainerLevelAccess.create(player.level(), player.blockPosition())),
            Component.translatable("container.crafting")
        ));
        
        LanguageUtil.sendMessage(player, "inventory.workbench.opened");
        return 1;
    }
    
    /**
     * Executes the /anvil command
     */
    private int executeAnvil(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Create anvil container
        player.openMenu(new SimpleMenuProvider(
            (windowId, playerInv, playerEntity) -> new AnvilMenu(windowId, playerInv, ContainerLevelAccess.create(player.level(), player.blockPosition())),
            Component.translatable("container.repair")
        ));
        
        LanguageUtil.sendMessage(player, "inventory.anvil.opened");
        return 1;
    }
    
    /**
     * Executes the /grindstone command
     */
    private int executeGrindstone(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Create grindstone container
        player.openMenu(new SimpleMenuProvider(
            (windowId, playerInv, playerEntity) -> new GrindstoneMenu(windowId, playerInv, ContainerLevelAccess.create(player.level(), player.blockPosition())),
            Component.translatable("container.grindstone_title")
        ));
        
        LanguageUtil.sendMessage(player, "inventory.grindstone.opened");
        return 1;
    }
    
    /**
     * Executes the /cartographytable command
     */
    private int executeCartographyTable(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Create cartography table container
        player.openMenu(new SimpleMenuProvider(
            (windowId, playerInv, playerEntity) -> new CartographyTableMenu(windowId, playerInv, ContainerLevelAccess.create(player.level(), player.blockPosition())),
            Component.translatable("container.cartography_table")
        ));
        
        LanguageUtil.sendMessage(player, "inventory.cartography_table.opened");
        return 1;
    }
    
    /**
     * Executes the /loom command
     */
    private int executeLoom(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Create loom container
        player.openMenu(new SimpleMenuProvider(
            (windowId, playerInv, playerEntity) -> new LoomMenu(windowId, playerInv, ContainerLevelAccess.create(player.level(), player.blockPosition())),
            Component.translatable("container.loom")
        ));
        
        LanguageUtil.sendMessage(player, "inventory.loom.opened");
        return 1;
    }
    
    /**
     * Executes the /smithingtable command
     */
    private int executeSmithingTable(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Create smithing table container
        player.openMenu(new SimpleMenuProvider(
            (windowId, playerInv, playerEntity) -> new SmithingMenu(windowId, playerInv, ContainerLevelAccess.create(player.level(), player.blockPosition())),
            Component.translatable("container.upgrade")
        ));
        
        LanguageUtil.sendMessage(player, "inventory.smithing_table.opened");
        return 1;
    }
      /**
     * Executes the /stonecutter command
     */
    private int executeStonecutter(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Create stonecutter container
        player.openMenu(new SimpleMenuProvider(
            (windowId, playerInv, playerEntity) -> new StonecutterMenu(windowId, playerInv, ContainerLevelAccess.create(player.level(), player.blockPosition())),
            Component.translatable("container.stonecutter")
        ));
        
        LanguageUtil.sendMessage(player, "inventory.stonecutter.opened");
        return 1;
    }
    
    /**
     * Executes the /enderchest command, opening the player's own enderchest
     */
    private int executeEnderchest(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Open the player's own enderchest
        player.openMenu(new SimpleMenuProvider(
            (windowId, playerInv, playerEntity) -> ChestMenu.threeRows(windowId, playerInv, player.getEnderChestInventory()),
            Component.translatable("container.enderchest")
        ));
        
        LanguageUtil.sendMessage(player, "inventory.enderchest.opened");
        return 1;
    }
    
    /**
     * Executes the /enderchest <player> command, allowing admins to view other players' enderchests
     */
    private int executeEnderchestOthers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer source = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        
        // Open the target's enderchest
        source.openMenu(new SimpleMenuProvider(
            (windowId, playerInv, playerEntity) -> ChestMenu.threeRows(windowId, playerInv, target.getEnderChestInventory()),
            Component.literal(target.getScoreboardName() + "'s Enderchest")
        ));
        
        LanguageUtil.sendMessage(source, "inventory.enderchest.opened_other", target.getDisplayName().getString());
        return 1;
    }
}
