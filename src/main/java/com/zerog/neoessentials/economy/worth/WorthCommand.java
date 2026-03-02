package com.zerog.neoessentials.economy.worth;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;

/**
 * /worth [item] [amount]
 *
 * Port of EssentialsX Commandworth:
 *  - /worth           → show value of item in hand
 *  - /worth <item>    → show value of named item (console / player)
 *  - /worth <item> <amount>  → show value of N of that item
 *  - /worth hand      → alias for held item
 */
public class WorthCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!com.zerog.neoessentials.config.ConfigManager.isEconomyEnabled()) return;

        dispatcher.register(Commands.literal("worth")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.worth");
            })
            // /worth  — hand item
            .executes(ctx -> executeWorthHand(ctx, 0))
            // /worth hand [amount]
            .then(Commands.literal("hand")
                .executes(ctx -> executeWorthHand(ctx, 0))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                    .executes(ctx -> executeWorthHand(ctx, IntegerArgumentType.getInteger(ctx, "amount"))))
            )
            // /worth <item> [amount]
            .then(Commands.argument("item", StringArgumentType.word())
                .executes(ctx -> executeWorthItem(ctx,
                    StringArgumentType.getString(ctx, "item"), 1))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                    .executes(ctx -> executeWorthItem(ctx,
                        StringArgumentType.getString(ctx, "item"),
                        IntegerArgumentType.getInteger(ctx, "amount"))))
            )
        );
    }

    // /worth [hand]
    private static int executeWorthHand(CommandContext<CommandSourceStack> ctx, int amount) {
        var source = ctx.getSource();
        var player = source.getPlayer();
        if (player == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only"));
            return 0;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.worth.no_item_in_hand"));
            return 0;
        }
        int qty = amount > 0 ? amount : held.getCount();
        return showWorth(source, held, qty);
    }

    // /worth <item> [amount]
    private static int executeWorthItem(CommandContext<CommandSourceStack> ctx, String itemId, int amount) {
        var source = ctx.getSource();
        ItemStack stack = WorthManager.resolveItem(itemId);
        if (stack == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.worth.unknown_item", itemId));
            return 0;
        }
        stack.setCount(amount);
        return showWorth(source, stack, amount);
    }

    private static int showWorth(CommandSourceStack source, ItemStack stack, int amount) {
        WorthManager wm = WorthManager.getInstance();
        BigDecimal price = wm.getPrice(stack);
        if (price == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.worth.no_price",
                WorthManager.getItemId(stack)));
            return 0;
        }
        BigDecimal total = price.multiply(BigDecimal.valueOf(amount));
        String itemId = WorthManager.getItemId(stack);
        String symbol = getCurrencySymbol();

        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.worth.result",
            amount, itemId, symbol + format(total), symbol + format(price)), false);
        return 1;
    }

    static String getCurrencySymbol() {
        try {
            var cfg = com.zerog.neoessentials.config.ConfigManager.getInstance()
                .getConfig(com.zerog.neoessentials.config.ConfigManager.MAIN_CONFIG);
            if (cfg.has("economy")) {
                var eco = cfg.getAsJsonObject("economy");
                if (eco.has("currencySymbol")) return eco.get("currencySymbol").getAsString();
            }
        } catch (Exception ignored) {}
        return "$";
    }

    static String format(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}

