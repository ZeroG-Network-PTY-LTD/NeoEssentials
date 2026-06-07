package com.zerog.neoessentials.auctionhouse.command;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.zerog.neoessentials.auctionhouse.AuctionConfig;
import com.zerog.neoessentials.auctionhouse.AuctionHouseManager;
import com.zerog.neoessentials.auctionhouse.gui.GUIAuctionHouse;
import com.zerog.neoessentials.auctionhouse.gui.GUIExpiredItems;
import com.zerog.neoessentials.auctionhouse.gui.GUIPersonalAuctionHouse;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
/**
 * Registers all {@code /ah} sub-commands for the NeoEssentials Auction House.
 */
public final class AuctionHouseCommand {
    private AuctionHouseCommand() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("ah")
                .executes(ctx -> executeOpen(ctx.getSource()))
                .then(Commands.literal("sell")
                        .requires(CommandSourceStack::isPlayer)
                        .then(Commands.argument("price", DoubleArgumentType.doubleArg(0.01))
                                .executes(ctx -> executeSell(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "price")))))
                .then(Commands.literal("selling")
                        .requires(CommandSourceStack::isPlayer)
                        .executes(ctx -> executeSelling(ctx.getSource())))
                .then(Commands.literal("expired")
                        .requires(CommandSourceStack::isPlayer)
                        .executes(ctx -> executeExpired(ctx.getSource())))
                .then(Commands.literal("reload")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> executeReload(ctx.getSource())))
                .then(Commands.literal("help")
                        .executes(ctx -> executeHelp(ctx.getSource())));
        dispatcher.register(root);
        dispatcher.register(Commands.literal("auctionhouse").redirect(dispatcher.getRoot().getChild("ah")));
    }
    private static int executeOpen(CommandSourceStack source) {
        if (!source.isPlayer()) { source.sendFailure(Component.literal("Players only.")); return 0; }
        GUIAuctionHouse.open(source.getPlayer());
        return Command.SINGLE_SUCCESS;
    }
    private static int executeSell(CommandSourceStack source, double price) {
        if (!source.isPlayer()) { source.sendFailure(Component.literal("Players only.")); return 0; }
        ServerPlayer player = source.getPlayer();
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            player.sendSystemMessage(Component.literal("\u00a7cYou must hold an item to sell!"));
            return 0;
        }
        if (price <= 0) {
            player.sendSystemMessage(Component.literal("\u00a7cPrice must be greater than 0!"));
            return 0;
        }
        ItemStack toSell = held.copy();
        int result = AuctionHouseManager.getInstance().sellItem(player, toSell, price);
        switch (result) {
            case -2 -> player.sendSystemMessage(Component.literal(
                    "\u00a7cYou have reached the maximum listings per player (" + AuctionConfig.get().getMaxItemsPerPlayer() + ")."));
            case -1 -> player.sendSystemMessage(Component.literal("\u00a7cFailed to list item. Please try again."));
            default -> {
                player.getMainHandItem().shrink(toSell.getCount());
                player.sendSystemMessage(Component.literal(
                        "\u00a7aListed \u00a7e" + toSell.getHoverName().getString() +
                        " \u00a7afor \u00a76" + String.format("%.2f", price) + "\u00a7a! (ID: " + result + ")"));
            }
        }
        return Command.SINGLE_SUCCESS;
    }
    private static int executeSelling(CommandSourceStack source) {
        if (!source.isPlayer()) { source.sendFailure(Component.literal("Players only.")); return 0; }
        GUIPersonalAuctionHouse.open(source.getPlayer());
        return Command.SINGLE_SUCCESS;
    }
    private static int executeExpired(CommandSourceStack source) {
        if (!source.isPlayer()) { source.sendFailure(Component.literal("Players only.")); return 0; }
        GUIExpiredItems.open(source.getPlayer());
        return Command.SINGLE_SUCCESS;
    }
    private static int executeReload(CommandSourceStack source) {
        AuctionConfig.invalidate();
        AuctionConfig.load();
        source.sendSuccess(() -> Component.literal("\u00a7aAuction House config reloaded."), true);
        return Command.SINGLE_SUCCESS;
    }
    private static int executeHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(
                "\u00a76\u00a7l-- Auction House Commands --\n" +
                "\u00a7e/ah\u00a77 - Open the Auction House\n" +
                "\u00a7e/ah sell <price>\u00a77 - List held item for sale\n" +
                "\u00a7e/ah selling\u00a77 - View your active listings\n" +
                "\u00a7e/ah expired\u00a77 - Collect expired listings\n" +
                "\u00a7e/ah reload\u00a77 - Reload config (admin)\n" +
                "\u00a7e/auctionhouse\u00a77 - Alias for /ah"), false);
        return Command.SINGLE_SUCCESS;
    }
}