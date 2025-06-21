package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.utils.TextUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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

/**
 * Implements commands to open various user interfaces like workbench, anvil, etc.
 */
public class UICommands {

    /**
     * Registers all UI-related commands with the dispatcher.
     *
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerWorkbenchCommand(dispatcher);
        registerAnvilCommand(dispatcher);
        registerCartographyTableCommand(dispatcher);
        registerGrindstoneCommand(dispatcher);
        registerLoomCommand(dispatcher);
        registerSmithingTableCommand(dispatcher);
        registerStonecutterCommand(dispatcher);
    }

    /**
     * Registers the workbench command.
     *
     * @param dispatcher The command dispatcher
     */
    private void registerWorkbenchCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /workbench - Opens a crafting table UI
        LiteralArgumentBuilder<CommandSourceStack> workbenchCommand = Commands.literal("workbench")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.workbench"))
                .executes(this::executeWorkbench);

        // Register aliases
        dispatcher.register(workbenchCommand);
        dispatcher.register(Commands.literal("wb")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.workbench"))
                .executes(this::executeWorkbench));
        dispatcher.register(Commands.literal("craft")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.workbench"))
                .executes(this::executeWorkbench));
    }

    /**
     * Registers the anvil command.
     *
     * @param dispatcher The command dispatcher
     */
    private void registerAnvilCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /anvil - Opens an anvil UI
        LiteralArgumentBuilder<CommandSourceStack> anvilCommand = Commands.literal("anvil")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.anvil"))
                .executes(this::executeAnvil);

        dispatcher.register(anvilCommand);
    }

    /**
     * Registers the cartography table command.
     *
     * @param dispatcher The command dispatcher
     */
    private void registerCartographyTableCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /cartographytable - Opens a cartography table UI
        LiteralArgumentBuilder<CommandSourceStack> cartographyTableCommand = Commands.literal("cartographytable")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.cartographytable"))
                .executes(this::executeCartographyTable);

        // Register aliases
        dispatcher.register(cartographyTableCommand);
        dispatcher.register(Commands.literal("carttable")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.cartographytable"))
                .executes(this::executeCartographyTable));
    }

    /**
     * Registers the grindstone command.
     *
     * @param dispatcher The command dispatcher
     */
    private void registerGrindstoneCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /grindstone - Opens a grindstone UI
        LiteralArgumentBuilder<CommandSourceStack> grindstoneCommand = Commands.literal("grindstone")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.grindstone"))
                .executes(this::executeGrindstone);

        dispatcher.register(grindstoneCommand);
    }

    /**
     * Registers the loom command.
     *
     * @param dispatcher The command dispatcher
     */
    private void registerLoomCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /loom - Opens a loom UI
        LiteralArgumentBuilder<CommandSourceStack> loomCommand = Commands.literal("loom")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.loom"))
                .executes(this::executeLoom);

        dispatcher.register(loomCommand);
    }

    /**
     * Registers the smithing table command.
     *
     * @param dispatcher The command dispatcher
     */
    private void registerSmithingTableCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /smithingtable - Opens a smithing table UI
        LiteralArgumentBuilder<CommandSourceStack> smithingTableCommand = Commands.literal("smithingtable")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.smithingtable"))
                .executes(this::executeSmithingTable);

        // Register aliases
        dispatcher.register(smithingTableCommand);
        dispatcher.register(Commands.literal("smithtable")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.smithingtable"))
                .executes(this::executeSmithingTable));
    }

    /**
     * Registers the stonecutter command.
     *
     * @param dispatcher The command dispatcher
     */
    private void registerStonecutterCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /stonecutter - Opens a stonecutter UI
        LiteralArgumentBuilder<CommandSourceStack> stonecutterCommand = Commands.literal("stonecutter")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.stonecutter"))
                .executes(this::executeStonecutter);

        dispatcher.register(stonecutterCommand);
    }

    /**
     * Executes the workbench command.
     *
     * @param context The command context
     * @return 1 if successful
     */
    private int executeWorkbench(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.openMenu(new SimpleMenuProvider(
            (containerId, playerInventory, playerEntity) -> new CraftingMenu(
                containerId, playerInventory, ContainerLevelAccess.create(player.level(), player.blockPosition())
            ),
            Component.translatable("container.crafting")
        ));
        
        context.getSource().sendSuccess(() -> Component.literal(TextUtil.formatText("&aOpened crafting table.")), true);
        return 1;
    }

    /**
     * Executes the anvil command.
     *
     * @param context The command context
     * @return 1 if successful
     */
    private int executeAnvil(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.openMenu(new SimpleMenuProvider(
            (containerId, playerInventory, playerEntity) -> new AnvilMenu(
                containerId, playerInventory, ContainerLevelAccess.create(player.level(), player.blockPosition())
            ),
            Component.translatable("container.repair")
        ));
        
        context.getSource().sendSuccess(() -> Component.literal(TextUtil.formatText("&aOpened anvil.")), true);
        return 1;
    }

    /**
     * Executes the cartography table command.
     *
     * @param context The command context
     * @return 1 if successful
     */
    private int executeCartographyTable(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.openMenu(new SimpleMenuProvider(
            (containerId, playerInventory, playerEntity) -> new CartographyTableMenu(
                containerId, playerInventory, ContainerLevelAccess.create(player.level(), player.blockPosition())
            ),
            Component.translatable("container.cartography_table")
        ));
        
        context.getSource().sendSuccess(() -> Component.literal(TextUtil.formatText("&aOpened cartography table.")), true);
        return 1;
    }

    /**
     * Executes the grindstone command.
     *
     * @param context The command context
     * @return 1 if successful
     */
    private int executeGrindstone(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.openMenu(new SimpleMenuProvider(
            (containerId, playerInventory, playerEntity) -> new GrindstoneMenu(
                containerId, playerInventory, ContainerLevelAccess.create(player.level(), player.blockPosition())
            ),
            Component.translatable("container.grindstone_title")
        ));
        
        context.getSource().sendSuccess(() -> Component.literal(TextUtil.formatText("&aOpened grindstone.")), true);
        return 1;
    }

    /**
     * Executes the loom command.
     *
     * @param context The command context
     * @return 1 if successful
     */
    private int executeLoom(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.openMenu(new SimpleMenuProvider(
            (containerId, playerInventory, playerEntity) -> new LoomMenu(
                containerId, playerInventory, ContainerLevelAccess.create(player.level(), player.blockPosition())
            ),
            Component.translatable("container.loom")
        ));
        
        context.getSource().sendSuccess(() -> Component.literal(TextUtil.formatText("&aOpened loom.")), true);
        return 1;
    }

    /**
     * Executes the smithing table command.
     *
     * @param context The command context
     * @return 1 if successful
     */
    private int executeSmithingTable(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.openMenu(new SimpleMenuProvider(
            (containerId, playerInventory, playerEntity) -> new SmithingMenu(
                containerId, playerInventory, ContainerLevelAccess.create(player.level(), player.blockPosition())
            ),
            Component.translatable("container.upgrade")
        ));
        
        context.getSource().sendSuccess(() -> Component.literal(TextUtil.formatText("&aOpened smithing table.")), true);
        return 1;
    }

    /**
     * Executes the stonecutter command.
     *
     * @param context The command context
     * @return 1 if successful
     */
    private int executeStonecutter(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        player.openMenu(new SimpleMenuProvider(
            (containerId, playerInventory, playerEntity) -> new StonecutterMenu(
                containerId, playerInventory, ContainerLevelAccess.create(player.level(), player.blockPosition())
            ),
            Component.translatable("container.stonecutter")
        ));
        
        context.getSource().sendSuccess(() -> Component.literal(TextUtil.formatText("&aOpened stonecutter.")), true);
        return 1;
    }
}
