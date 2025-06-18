package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.PowerToolManager;
<<<<<<< HEAD
import com.zerog.neoessentials.utils.VanillaBooleanParser;
=======
import com.zerog.neoessentials.utils.StringToBooleanArgumentType;
>>>>>>> 2b0efb3 (Implement powertool and jail management systems)
import com.zerog.neoessentials.utils.TextUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Implements powertool-related commands.
 */
public class PowerToolCommands {

    /**
     * Registers all powertool-related commands with the dispatcher.
     *
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerPowerToolCommand(dispatcher);
    }

    /**
     * Registers the powertool command.
     *
     * @param dispatcher The command dispatcher
     */
    private void registerPowerToolCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /powertool <command>  - Binds a command to the held item
        // /powertool           - Shows the command bound to the held item
        // /powertool -c        - Clears the powertool on the held item
        // /powertool -a        - Lists all powertools for the player
        // /powertool -r        - Clears all powertools for the player
        // /powertool -e <true/false> - Enables or disables powertools for the player
        // /powertool -t        - Toggles powertools on/off for the player
        LiteralArgumentBuilder<CommandSourceStack> powerToolCommand = Commands.literal("powertool")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.powertool"))
                .executes(this::executePowerToolInfo)
                .then(Commands.literal("-c")
                        .executes(this::executePowerToolClear))
                .then(Commands.literal("-a")
                        .executes(this::executePowerToolList))
                .then(Commands.literal("-r")
                        .executes(this::executePowerToolRemoveAll))
                .then(Commands.literal("-e")
<<<<<<< HEAD
                        .then(Commands.argument("enabled", VanillaBooleanParser.argument())
                                .suggests(VanillaBooleanParser.booleanSuggestions())
=======
                        .then(Commands.argument("enabled", StringToBooleanArgumentType.stringToBoolean())
>>>>>>> 2b0efb3 (Implement powertool and jail management systems)
                                .executes(this::executePowerToolEnable)))
                .then(Commands.literal("-t")
                        .executes(this::executePowerToolToggle))
                .then(Commands.argument("command", StringArgumentType.greedyString())
                        .executes(this::executePowerToolSet));

        // Register aliases
        dispatcher.register(powerToolCommand);
        dispatcher.register(Commands.literal("pt")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.powertool"))
                .executes(this::executePowerToolInfo)
                .then(Commands.literal("-c")
                        .executes(this::executePowerToolClear))
                .then(Commands.literal("-a")
                        .executes(this::executePowerToolList))
                .then(Commands.literal("-r")
                        .executes(this::executePowerToolRemoveAll))
                .then(Commands.literal("-e")
<<<<<<< HEAD
                        .then(Commands.argument("enabled", VanillaBooleanParser.argument())
                                .suggests(VanillaBooleanParser.booleanSuggestions())
=======
                        .then(Commands.argument("enabled", StringToBooleanArgumentType.stringToBoolean())
>>>>>>> 2b0efb3 (Implement powertool and jail management systems)
                                .executes(this::executePowerToolEnable)))
                .then(Commands.literal("-t")
                        .executes(this::executePowerToolToggle))
                .then(Commands.argument("command", StringArgumentType.greedyString())
                        .executes(this::executePowerToolSet)));
    }

    /**
     * Shows information about the powertool bound to the held item.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private int executePowerToolInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (heldItem.isEmpty()) {
            source.sendFailure(Component.literal(TextUtil.formatText("&cYou must be holding an item.")));
            return 0;
        }

        PowerToolManager powerToolManager = NeoEssentials.getInstance().getDataManager().getPowerToolManager();
        String command = powerToolManager.getPowerToolCommand(player, heldItem.getItem());

        if (command == null) {
            source.sendSuccess(() -> Component.literal(TextUtil.formatText(
                    "&6" + heldItem.getDisplayName().getString() + " &ais not a powertool.")), false);
        } else {
            source.sendSuccess(() -> Component.literal(TextUtil.formatText(
                    "&6" + heldItem.getDisplayName().getString() + " &ais bound to: &6/" + command)), false);
        }

        boolean enabled = powerToolManager.isPowerToolEnabled(player);
        source.sendSuccess(() -> Component.literal(TextUtil.formatText(
                "&aPowertools are currently " + (enabled ? "&2enabled" : "&4disabled") + "&a for you.")), false);

        return 1;
    }

    /**
     * Clears the powertool bound to the held item.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private int executePowerToolClear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (heldItem.isEmpty()) {
            source.sendFailure(Component.literal(TextUtil.formatText("&cYou must be holding an item.")));
            return 0;
        }

        PowerToolManager powerToolManager = NeoEssentials.getInstance().getDataManager().getPowerToolManager();
        boolean removed = powerToolManager.clearPowerTool(player, heldItem.getItem());

        if (removed) {
            source.sendSuccess(() -> Component.literal(TextUtil.formatText(
                    "&aRemoved powertool binding from &6" + heldItem.getDisplayName().getString() + "&a.")), true);
        } else {
            source.sendFailure(Component.literal(TextUtil.formatText(
                    "&6" + heldItem.getDisplayName().getString() + " &cis not a powertool.")));
        }

        return removed ? 1 : 0;
    }

    /**
     * Lists all powertools for the player.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private int executePowerToolList(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        PowerToolManager powerToolManager = NeoEssentials.getInstance().getDataManager().getPowerToolManager();
        Map<String, String> powerTools = powerToolManager.getPlayerPowerTools(player);

        if (powerTools.isEmpty()) {
            source.sendSuccess(() -> Component.literal(TextUtil.formatText("&aYou have no powertools.")), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal(TextUtil.formatText("&aYour powertools:")), false);
        for (Map.Entry<String, String> entry : powerTools.entrySet()) {
            String itemName = entry.getKey();
            String command = entry.getValue();
            source.sendSuccess(() -> Component.literal(TextUtil.formatText("&6" + itemName + " &a-> &6/" + command)), false);
        }

        boolean enabled = powerToolManager.isPowerToolEnabled(player);
        source.sendSuccess(() -> Component.literal(TextUtil.formatText(
                "&aPowertools are currently " + (enabled ? "&2enabled" : "&4disabled") + "&a for you.")), false);

        return 1;
    }

    /**
     * Removes all powertools for the player.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private int executePowerToolRemoveAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        PowerToolManager powerToolManager = NeoEssentials.getInstance().getDataManager().getPowerToolManager();
        int count = powerToolManager.clearAllPowerTools(player);

        if (count > 0) {
            source.sendSuccess(() -> Component.literal(TextUtil.formatText(
                    "&aRemoved &6" + count + " &apowertool binding" + (count == 1 ? "" : "s") + ".")), true);
        } else {
            source.sendSuccess(() -> Component.literal(TextUtil.formatText(
                    "&aYou have no powertools to remove.")), false);
        }

        return 1;
    }

    /**
     * Enables or disables powertools for the player.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private int executePowerToolEnable(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
<<<<<<< HEAD
        boolean enabled = VanillaBooleanParser.getBoolean(context, "enabled");
=======
        boolean enabled = StringToBooleanArgumentType.getBoolean(context, "enabled");
>>>>>>> 2b0efb3 (Implement powertool and jail management systems)

        PowerToolManager powerToolManager = NeoEssentials.getInstance().getDataManager().getPowerToolManager();
        powerToolManager.setPowerToolEnabled(player, enabled);

        source.sendSuccess(() -> Component.literal(TextUtil.formatText(
                "&aPowertools are now " + (enabled ? "&2enabled" : "&4disabled") + "&a for you.")), true);

        return 1;
    }

    /**
     * Toggles powertools on/off for the player.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private int executePowerToolToggle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        PowerToolManager powerToolManager = NeoEssentials.getInstance().getDataManager().getPowerToolManager();
        boolean enabled = powerToolManager.togglePowerTool(player);

        source.sendSuccess(() -> Component.literal(TextUtil.formatText(
                "&aPowertools are now " + (enabled ? "&2enabled" : "&4disabled") + "&a for you.")), true);

        return 1;
    }

    /**
     * Sets a powertool binding for the held item.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private int executePowerToolSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        String command = StringArgumentType.getString(context, "command");
        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (heldItem.isEmpty()) {
            source.sendFailure(Component.literal(TextUtil.formatText("&cYou must be holding an item.")));
            return 0;
        }

        // Remove the leading slash if present
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        PowerToolManager powerToolManager = NeoEssentials.getInstance().getDataManager().getPowerToolManager();
        Item item = heldItem.getItem();
        powerToolManager.setPowerTool(player, item, command);
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 0264cbd (fix: Make command and heldItem final for lambda in PowerToolCommands)
        
        final String finalCommand = command; // Make command final for the lambda
        final ItemStack finalItem = heldItem; // Make heldItem final for the lambda
        
<<<<<<< HEAD
        source.sendSuccess(() -> Component.literal(TextUtil.formatText(
                "&aSet powertool on &6" + finalItem.getDisplayName().getString() + " &ato: &6/" + finalCommand)), true);
=======

        source.sendSuccess(() -> Component.literal(TextUtil.formatText(
                "&aSet powertool on &6" + heldItem.getDisplayName().getString() + " &ato: &6/" + command)), true);
>>>>>>> 2b0efb3 (Implement powertool and jail management systems)
=======
        source.sendSuccess(() -> Component.literal(TextUtil.formatText(
                "&aSet powertool on &6" + finalItem.getDisplayName().getString() + " &ato: &6/" + finalCommand)), true);
>>>>>>> 0264cbd (fix: Make command and heldItem final for lambda in PowerToolCommands)

        boolean enabled = powerToolManager.isPowerToolEnabled(player);
        if (!enabled) {
            source.sendSuccess(() -> Component.literal(TextUtil.formatText(
                    "&cNote: Your powertools are currently disabled. Use &6/powertool -t &cto enable them.")), false);
        }

        return 1;
    }
}
