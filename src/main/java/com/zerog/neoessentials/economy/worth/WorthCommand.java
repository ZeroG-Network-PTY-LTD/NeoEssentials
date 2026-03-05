package com.zerog.neoessentials.economy.worth;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;

/**
 * /worth [item] [amount]
 *
 * - /worth                    → value of held item
 * - /worth hand [amount]      → explicit held-item alias
 * - /worth <item> [amount]    → item by id (vanilla name, mod:item_id, fuzzy name)
 * - /worth <mod:item> [amt]   → full namespaced id supported via greedyString
 */
public class WorthCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!com.zerog.neoessentials.config.ConfigManager.isEconomyEnabled()) return;

        dispatcher.register(Commands.literal("worth")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.worth");
            })
            // /worth  — held item
            .executes(ctx -> executeWorthHand(ctx, 0))
            // /worth hand [amount]
            .then(Commands.literal("hand")
                .executes(ctx -> executeWorthHand(ctx, 0))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                    .executes(ctx -> executeWorthHand(ctx, IntegerArgumentType.getInteger(ctx, "amount"))))
            )
            // /worth <item|mod:item> [amount]
            // greedyString captures "thermal:copper_ingot" or "copper ingot 64" as one token
            .then(Commands.argument("item", StringArgumentType.greedyString())
                .executes(ctx -> {
                    String raw = StringArgumentType.getString(ctx, "item").trim();
                    // Support trailing number as amount: "diamond 64"
                    String[] parts = raw.split("\\s+");
                    if (parts.length >= 2) {
                        try {
                            int amt = Integer.parseInt(parts[parts.length - 1]);
                            String itemPart = String.join(" ", java.util.Arrays.copyOf(parts, parts.length - 1));
                            return executeWorthItem(ctx, itemPart, amt);
                        } catch (NumberFormatException ignored) {}
                    }
                    return executeWorthItem(ctx, raw, 1);
                })
            )
        );
    }

    // ── /worth (hand) ─────────────────────────────────────────────────────────
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

    // ── /worth <item> [amount] ────────────────────────────────────────────────
    private static int executeWorthItem(CommandContext<CommandSourceStack> ctx, String itemId, int amount) {
        var source = ctx.getSource();
        // Normalize: spaces → underscores, lowercase — handles "copper ingot", "Thermal:Copper_Ingot"
        String normalized = itemId.trim().toLowerCase().replace(" ", "_");
        ItemStack stack = WorthManager.resolveItem(normalized);
        if (stack == null || stack.isEmpty()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.worth.unknown_item", itemId));
            return 0;
        }
        int qty = Math.max(1, amount);
        return showWorth(source, stack, qty);
    }

    // ── show result ───────────────────────────────────────────────────────────
    private static int showWorth(CommandSourceStack source, ItemStack stack, int amount) {
        WorthManager wm = WorthManager.getInstance();
        BigDecimal price = wm.getPrice(stack);
        if (price == null) {
            String itemId = WorthManager.getItemId(stack);
            source.sendFailure(MessageUtil.error("commands.neoessentials.worth.no_price", itemId));
            return 0;
        }
        BigDecimal total = price.multiply(BigDecimal.valueOf(amount));
        String itemId = WorthManager.getItemId(stack);
        String symbol  = getCurrencySymbol();
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
