package com.zerog.neoessentials.auctionhouse.command;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.zerog.neoessentials.auctionhouse.AuctionConfig;
import com.zerog.neoessentials.auctionhouse.AuctionHouseManager;
import com.zerog.neoessentials.auctionhouse.gui.GUIAuctionHouse;
import com.zerog.neoessentials.auctionhouse.gui.GUIExpiredItems;
import com.zerog.neoessentials.auctionhouse.gui.GUIPersonalAuctionHouse;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
/**
 * Registers all {@code /ah} sub-commands for the NeoEssentials Auction House.
 */
public final class AuctionHouseCommand {
    private AuctionHouseCommand() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!com.zerog.neoessentials.config.ConfigManager.isAuctionHouseModuleEnabled()) {
            return;
        }

        boolean ahEnabled = com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("ah");
        boolean auctionhouseEnabled = com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("auctionhouse");

        if (!ahEnabled && !auctionhouseEnabled) {
            return;
        }

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
                        .requires(src -> com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(src, 2))
                        .executes(ctx -> executeReload(ctx.getSource())))
                .then(Commands.literal("help")
                        .executes(ctx -> executeHelp(ctx.getSource())));
        if (ahEnabled) {
            dispatcher.register(root);
        }
        if (auctionhouseEnabled && ahEnabled) {
            dispatcher.register(Commands.literal("auctionhouse").redirect(dispatcher.getRoot().getChild("ah")));
        }
    }
    private static int executeOpen(CommandSourceStack source) {
        if (!source.isPlayer()) { source.sendFailure(MessageUtil.error("commands.neoessentials.ah.players_only")); return 0; }
        GUIAuctionHouse.open(source.getPlayer());
        return Command.SINGLE_SUCCESS;
    }
    private static int executeSell(CommandSourceStack source, double price) {
        if (!source.isPlayer()) { source.sendFailure(MessageUtil.error("commands.neoessentials.ah.players_only")); return 0; }
        ServerPlayer player = source.getPlayer();
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.ah.sell.need_item"));
            return 0;
        }
        if (price <= 0) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.ah.sell.invalid_price"));
            return 0;
        }
        ItemStack toSell = held.copy();
        int result = AuctionHouseManager.getInstance().sellItem(player, toSell, price);
        switch (result) {
            case -2 -> player.sendSystemMessage(MessageUtil.error("commands.neoessentials.ah.sell.max_listings",
                    AuctionConfig.get().getMaxItemsPerPlayer()));
            case -1 -> player.sendSystemMessage(MessageUtil.error("commands.neoessentials.ah.sell.failed"));
            default -> {
                player.getMainHandItem().shrink(toSell.getCount());
                player.sendSystemMessage(MessageUtil.success("commands.neoessentials.ah.sell.success",
                        toSell.getHoverName().getString(), String.format("%.2f", price), result));
            }
        }
        return Command.SINGLE_SUCCESS;
    }
    private static int executeSelling(CommandSourceStack source) {
        if (!source.isPlayer()) { source.sendFailure(MessageUtil.error("commands.neoessentials.ah.players_only")); return 0; }
        GUIPersonalAuctionHouse.open(source.getPlayer());
        return Command.SINGLE_SUCCESS;
    }
    private static int executeExpired(CommandSourceStack source) {
        if (!source.isPlayer()) { source.sendFailure(MessageUtil.error("commands.neoessentials.ah.players_only")); return 0; }
        GUIExpiredItems.open(source.getPlayer());
        return Command.SINGLE_SUCCESS;
    }
    private static int executeReload(CommandSourceStack source) {
        AuctionConfig.invalidate();
        AuctionConfig.load();
        source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.ah.reload.success"), true);
        return Command.SINGLE_SUCCESS;
    }
    private static int executeHelp(CommandSourceStack source) {
        source.sendSuccess(() -> MessageUtil.component("commands.neoessentials.ah.help"), false);
        return Command.SINGLE_SUCCESS;
    }
}