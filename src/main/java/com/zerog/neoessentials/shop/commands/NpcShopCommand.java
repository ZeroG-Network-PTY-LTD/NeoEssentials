package com.zerog.neoessentials.shop.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.shop.entity.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * {@code /npcshop} — create, manage, and inspect NPC entity shops.
 *
 * <pre>
 *   /npcshop create <name>                       — spawn NPC at current position
 *   /npcshop remove                              — remove the nearest NPC shop
 *   /npcshop additem <shopId> <item> <buy> <sell> <qty>
 *   /npcshop removeitem <shopId> <index>
 *   /npcshop list                                — list all NPC shops
 *   /npcshop info <shopId>                       — info about one shop
 *   /npcshop reload                              — reload npc_shops.json
 * </pre>
 *
 * All sub-commands require {@code neoessentials.shop.npc.manage}.
 */
public class NpcShopCommand {

    private static final String PERM = "neoessentials.shop.npc.manage";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var node = Commands.literal("npcshop")
                .requires(src -> src.hasPermission(3) ||
                        (src.getEntity() != null && PermissionAPI.hasPermission(src.getEntity().getUUID(), PERM)))
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> executeCreate(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("remove")
                        .executes(ctx -> executeRemove(ctx.getSource())))
                .then(Commands.literal("additem")
                        .then(Commands.argument("shopId", StringArgumentType.word())
                                .then(Commands.argument("item", StringArgumentType.word())
                                        .then(Commands.argument("buyPrice", DoubleArgumentType.doubleArg(-1))
                                                .then(Commands.argument("sellPrice", DoubleArgumentType.doubleArg(-1))
                                                        .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> executeAddItem(ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "shopId"),
                                                                        StringArgumentType.getString(ctx, "item"),
                                                                        DoubleArgumentType.getDouble(ctx, "buyPrice"),
                                                                        DoubleArgumentType.getDouble(ctx, "sellPrice"),
                                                                        IntegerArgumentType.getInteger(ctx, "quantity")))))))))
                .then(Commands.literal("removeitem")
                        .then(Commands.argument("shopId", StringArgumentType.word())
                                .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                        .executes(ctx -> executeRemoveItem(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "shopId"),
                                                IntegerArgumentType.getInteger(ctx, "index"))))))
                .then(Commands.literal("list")
                        .executes(ctx -> executeList(ctx.getSource())))
                .then(Commands.literal("info")
                        .then(Commands.argument("shopId", StringArgumentType.word())
                                .executes(ctx -> executeInfo(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "shopId")))))
                .then(Commands.literal("reload")
                        .executes(ctx -> executeReload(ctx.getSource())))
                .executes(ctx -> executeHelp(ctx.getSource()));

        dispatcher.register(node);
    }

    // ── /npcshop create <name> ────────────────────────────────────────────────

    private static int executeCreate(CommandSourceStack src, String name) {
        try {
            ServerPlayer player = src.getPlayerOrException();
            if (ShopNpcEntity.TYPE == null) {
                src.sendFailure(Component.literal("§cNPC entity type not registered yet."));
                return 0;
            }

            ShopEntityData shopData = new ShopEntityData();
            shopData.shopId     = UUID.randomUUID();
            shopData.ownerUUID  = player.getUUID();
            shopData.shopName   = name;
            shopData.dimension  = player.level().dimension().location().toString();
            shopData.spawnX     = player.getX();
            shopData.spawnY     = player.getY();
            shopData.spawnZ     = player.getZ();

            // Spawn the entity
            ShopNpcEntity npc = ShopNpcEntity.TYPE.create(player.serverLevel());
            if (npc == null) {
                src.sendFailure(Component.literal("§cFailed to create entity."));
                return 0;
            }
            npc.setPos(player.getX(), player.getY(), player.getZ());
            npc.setShopId(shopData.shopId);
            shopData.entityUUID = npc.getUUID();

            player.serverLevel().addFreshEntity(npc);
            ShopEntityManager.getInstance().register(shopData);

            src.sendSuccess(() -> Component.literal("§aNPC shop §f\"" + name + "\"§a created. " +
                    "§7ID: " + shopData.shopId + "\n§eUse /npcshop additem " + shopData.shopId.toString().substring(0, 8) + "... to add items."), true);
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    // ── /npcshop remove ───────────────────────────────────────────────────────

    private static int executeRemove(CommandSourceStack src) {
        try {
            ServerPlayer player = src.getPlayerOrException();
            // Find nearest ShopNpcEntity within 5 blocks
            List<ShopNpcEntity> nearby = player.serverLevel().getEntitiesOfClass(
                    ShopNpcEntity.class,
                    new AABB(player.getX() - 5, player.getY() - 5, player.getZ() - 5,
                             player.getX() + 5, player.getY() + 5, player.getZ() + 5));

            if (nearby.isEmpty()) {
                src.sendFailure(Component.literal("§cNo NPC shop within 5 blocks."));
                return 0;
            }

            ShopNpcEntity target = nearby.get(0);
            UUID shopId = target.getShopId();
            target.discard();

            if (shopId != null) {
                ShopEntityManager.getInstance().remove(shopId);
                src.sendSuccess(() -> Component.literal("§aNPC shop removed."), true);
            } else {
                src.sendSuccess(() -> Component.literal("§aEntity removed (no linked shop data)."), true);
            }
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    // ── /npcshop additem ──────────────────────────────────────────────────────

    private static int executeAddItem(CommandSourceStack src, String shopIdStr,
                                       String itemId, double buyPrice, double sellPrice, int qty) {
        ShopEntityData shop = resolve(src, shopIdStr);
        if (shop == null) return 0;

        BigDecimal buy  = buyPrice  >= 0 ? BigDecimal.valueOf(buyPrice)  : null;
        BigDecimal sell = sellPrice >= 0 ? BigDecimal.valueOf(sellPrice) : null;

        if (buy == null && sell == null) {
            src.sendFailure(Component.literal("§cBoth buyPrice and sellPrice are -1 (disabled). Set at least one."));
            return 0;
        }

        shop.addListing(new ShopListing("minecraft:" + itemId.replace("minecraft:", ""), buy, sell, qty));
        ShopEntityManager.getInstance().register(shop);
        src.sendSuccess(() -> Component.literal("§aAdded §f" + itemId + " §ato shop §f\"" +
                shop.shopName + "\"§a. Slot " + (shop.listings.size() - 1) + "."), false);
        return 1;
    }

    // ── /npcshop removeitem ───────────────────────────────────────────────────

    private static int executeRemoveItem(CommandSourceStack src, String shopIdStr, int index) {
        ShopEntityData shop = resolve(src, shopIdStr);
        if (shop == null) return 0;
        if (!shop.removeListing(index)) {
            src.sendFailure(Component.literal("§cInvalid index " + index + ". Shop has " + shop.listings.size() + " item(s)."));
            return 0;
        }
        ShopEntityManager.getInstance().register(shop);
        src.sendSuccess(() -> Component.literal("§aRemoved listing at index " + index + "."), false);
        return 1;
    }

    // ── /npcshop list ─────────────────────────────────────────────────────────

    private static int executeList(CommandSourceStack src) {
        var all = ShopEntityManager.getInstance().getAll();
        src.sendSuccess(() -> Component.literal("§6§l=== NPC Shops (" + all.size() + ") ==="), false);
        if (all.isEmpty()) {
            src.sendSuccess(() -> Component.literal("§7No NPC shops exist."), false);
        } else {
            for (ShopEntityData d : all) {
                src.sendSuccess(() -> Component.literal(String.format(
                        "§e%s §7[%s] §f— %d item(s) — §7%.0f,%.0f,%.0f %s",
                        d.shopName,
                        d.shopId.toString().substring(0, 8),
                        d.listings.size(),
                        d.spawnX, d.spawnY, d.spawnZ,
                        d.dimension.replace("minecraft:", "")
                )), false);
            }
        }
        return all.size();
    }

    // ── /npcshop info <shopId> ────────────────────────────────────────────────

    private static int executeInfo(CommandSourceStack src, String shopIdStr) {
        ShopEntityData shop = resolve(src, shopIdStr);
        if (shop == null) return 0;
        src.sendSuccess(() -> Component.literal("§6§l--- NPC Shop Info ---"), false);
        src.sendSuccess(() -> Component.literal("§eName: §f"  + shop.shopName), false);
        src.sendSuccess(() -> Component.literal("§eID:   §7"  + shop.shopId), false);
        src.sendSuccess(() -> Component.literal("§ePos:  §f"  + (int)shop.spawnX + "," + (int)shop.spawnY + "," + (int)shop.spawnZ), false);
        src.sendSuccess(() -> Component.literal("§eItems: §f" + shop.listings.size()), false);
        for (int i = 0; i < shop.listings.size(); i++) {
            ShopListing l = shop.listings.get(i);
            final int idx = i;
            src.sendSuccess(() -> Component.literal(String.format(
                    "§7  [%d] §f%dx %s §e| Buy:§f%s §e| Sell:§f%s",
                    idx, l.quantity(), l.itemId().replace("minecraft:", ""),
                    l.canBuy()  ? l.buyPrice().toPlainString()  : "§7—",
                    l.canSell() ? l.sellPrice().toPlainString() : "§7—"
            )), false);
        }
        return 1;
    }

    // ── /npcshop reload ───────────────────────────────────────────────────────

    private static int executeReload(CommandSourceStack src) {
        ShopEntityManager.getInstance().reload();
        src.sendSuccess(() -> Component.literal("§aNPC shops reloaded (" +
                ShopEntityManager.getInstance().getShopCount() + " shop(s))."), true);
        return 1;
    }

    // ── /npcshop help ─────────────────────────────────────────────────────────

    private static int executeHelp(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal("§6§l=== NPC Shop Commands ==="), false);
        src.sendSuccess(() -> Component.literal("§e/npcshop create <name>                     §7— Spawn NPC at your position"), false);
        src.sendSuccess(() -> Component.literal("§e/npcshop remove                            §7— Remove closest NPC shop"), false);
        src.sendSuccess(() -> Component.literal("§e/npcshop additem <id> <item> <buy> <sell> <qty>"), false);
        src.sendSuccess(() -> Component.literal("§e/npcshop removeitem <id> <index>"), false);
        src.sendSuccess(() -> Component.literal("§e/npcshop list                              §7— List all NPC shops"), false);
        src.sendSuccess(() -> Component.literal("§e/npcshop info <id>                         §7— Detailed info"), false);
        src.sendSuccess(() -> Component.literal("§e/npcshop reload                            §7— Reload from disk"), false);
        src.sendSuccess(() -> Component.literal("§7Pass -1 for buy/sell price to disable that side."), false);
        return 1;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ShopEntityData resolve(CommandSourceStack src, String shopIdStr) {
        ShopEntityData shop = ShopEntityManager.getInstance().getByShopId(shopIdStr);
        if (shop == null) {
            // Allow prefix matching (first 8 chars)
            for (ShopEntityData d : ShopEntityManager.getInstance().getAll()) {
                if (d.shopId.toString().startsWith(shopIdStr)) { shop = d; break; }
            }
        }
        if (shop == null) {
            src.sendFailure(Component.literal("§cNo NPC shop found with ID starting with '" + shopIdStr + "'."));
        }
        return shop;
    }
}

