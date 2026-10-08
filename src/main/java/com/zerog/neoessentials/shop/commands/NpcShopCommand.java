package com.zerog.neoessentials.shop.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.shop.entity.*;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
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
 *   /npcshop respawn <shopId>                    — re-summon a lost NPC entity
 *   /npcshop entitytype <shopId> <entityType> <ai> — change entity type / AI mode (respawns it)
 * </pre>
 *
 * <p>{@code <entityType>} accepts any registered entity type id, vanilla or from another
 * installed mod (e.g. {@code minecraft:villager}), not just the default {@code minecraft:armor_stand}
 * — see {@link ShopNpcEntity}'s javadoc for why that's safe. {@code <ai>} only matters when the
 * type is a {@link net.minecraft.world.entity.Mob}: {@code true} leaves its normal AI running
 * (leashed to its spawn point so it can't wander off), {@code false} freezes it in place exactly
 * like the default ArmorStand.
 *
 * All sub-commands require {@code neoessentials.shop.npc.manage}.
 */
public class NpcShopCommand {

    private static final String PERM = "neoessentials.shop.npc.manage";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!com.zerog.neoessentials.config.ConfigManager.isShopModuleEnabled()) {
            return;
        }
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isCommandEnabled("npcshop")) {
            return;
        }

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
                                .suggests(NpcShopCommand::suggestShopIds)
                                // An id argument, not word(): word() stops at ':' so "modid:item" couldn't be typed at all.
                                .then(Commands.argument("item", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                                        .suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggestResource(
                                                net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet(), b))
                                        .then(Commands.argument("buyPrice", DoubleArgumentType.doubleArg(-1))
                                                .then(Commands.argument("sellPrice", DoubleArgumentType.doubleArg(-1))
                                                        .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> executeAddItem(ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "shopId"),
                                                                        net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "item").toString(),
                                                                        DoubleArgumentType.getDouble(ctx, "buyPrice"),
                                                                        DoubleArgumentType.getDouble(ctx, "sellPrice"),
                                                                        IntegerArgumentType.getInteger(ctx, "quantity")))))))))
                .then(Commands.literal("additemhand")
                        .then(Commands.argument("shopId", StringArgumentType.word())
                                .suggests(NpcShopCommand::suggestShopIds)
                                .then(Commands.argument("buyPrice", DoubleArgumentType.doubleArg(-1))
                                        .then(Commands.argument("sellPrice", DoubleArgumentType.doubleArg(-1))
                                                .executes(ctx -> executeAddItemHand(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "shopId"),
                                                        DoubleArgumentType.getDouble(ctx, "buyPrice"),
                                                        DoubleArgumentType.getDouble(ctx, "sellPrice")))))))
                .then(Commands.literal("addcommand")
                        .then(Commands.argument("shopId", StringArgumentType.word())
                                .suggests(NpcShopCommand::suggestShopIds)
                                .then(Commands.argument("icon", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                                        .suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggestResource(
                                                net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet(), b))
                                        .then(Commands.argument("price", DoubleArgumentType.doubleArg(0))
                                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                                        .executes(ctx -> executeAddCommand(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "shopId"),
                                                                net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "icon").toString(),
                                                                DoubleArgumentType.getDouble(ctx, "price"),
                                                                StringArgumentType.getString(ctx, "command"))))))))
                .then(Commands.literal("editlisting")
                        .then(Commands.argument("shopId", StringArgumentType.word())
                                .suggests(NpcShopCommand::suggestShopIds)
                                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                        .then(Commands.literal("price")
                                                .then(Commands.argument("buyPrice", DoubleArgumentType.doubleArg(-1))
                                                        .then(Commands.argument("sellPrice", DoubleArgumentType.doubleArg(-1))
                                                                .executes(ctx -> editListing(ctx.getSource(), ctx, "price")))))
                                        .then(Commands.literal("name")
                                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                                        .executes(ctx -> editListing(ctx.getSource(), ctx, "name"))))
                                        .then(Commands.literal("addcommand")
                                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                                        .executes(ctx -> editListing(ctx.getSource(), ctx, "addcommand"))))
                                        .then(Commands.literal("clearcommands")
                                                .executes(ctx -> editListing(ctx.getSource(), ctx, "clearcommands")))
                                        .then(Commands.literal("runas")
                                                .then(Commands.argument("runas", StringArgumentType.word())
                                                        .suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(List.of("console", "player"), b))
                                                        .executes(ctx -> editListing(ctx.getSource(), ctx, "runas"))))
                                        .then(Commands.literal("permission")
                                                .then(Commands.argument("permission", StringArgumentType.word())
                                                        .executes(ctx -> editListing(ctx.getSource(), ctx, "permission")))))))
                .then(Commands.literal("removeitem")
                        .then(Commands.argument("shopId", StringArgumentType.word())
                                .suggests(NpcShopCommand::suggestShopIds)
                                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                        .executes(ctx -> executeRemoveItem(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "shopId"),
                                                IntegerArgumentType.getInteger(ctx, "index"))))))
                .then(Commands.literal("list")
                        .executes(ctx -> executeList(ctx.getSource())))
                .then(Commands.literal("info")
                        .then(Commands.argument("shopId", StringArgumentType.word())
                                .suggests(NpcShopCommand::suggestShopIds)
                                .executes(ctx -> executeInfo(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "shopId")))))
                .then(Commands.literal("reload")
                        .executes(ctx -> executeReload(ctx.getSource())))
                .then(Commands.literal("respawn")
                        .then(Commands.argument("shopId", StringArgumentType.word())
                                .suggests(NpcShopCommand::suggestShopIds)
                                .executes(ctx -> executeRespawn(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "shopId")))))
                .then(Commands.literal("entitytype")
                        .then(Commands.argument("shopId", StringArgumentType.word())
                                .suggests(NpcShopCommand::suggestShopIds)
                                .then(Commands.argument("entityType", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggestResource(
                                                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.keySet(), builder))
                                        // <ai> is optional — leaving it out keeps the shop's current AI setting
                                        .executes(ctx -> executeEntityType(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "shopId"),
                                                net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "entityType").toString(),
                                                null))
                                        .then(Commands.argument("ai", BoolArgumentType.bool())
                                                .executes(ctx -> executeEntityType(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "shopId"),
                                                        net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "entityType").toString(),
                                                        BoolArgumentType.getBool(ctx, "ai")))))))
                .executes(ctx -> executeHelp(ctx.getSource()));

        dispatcher.register(node);
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestShopIds(
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        java.util.List<String> options = new java.util.ArrayList<>();
        options.add("this");
        for (ShopEntityData d : ShopEntityManager.getInstance().getAll()) {
            if (d.shopName != null && !d.shopName.contains(" ")) options.add(d.shopName);
            options.add(d.shopId.toString().substring(0, 8));
        }
        return net.minecraft.commands.SharedSuggestionProvider.suggest(options, builder);
    }

    // ── /npcshop create <name> ────────────────────────────────────────────────

    private static int executeCreate(CommandSourceStack src, String name) {
        try {
            ServerPlayer player = src.getPlayerOrException();

            ShopEntityData shopData = new ShopEntityData();
            shopData.shopId    = UUID.randomUUID();
            shopData.ownerUUID = player.getUUID();
            shopData.shopName  = name;
            shopData.dimension = player.level().dimension().location().toString();
            shopData.spawnX    = player.getX();
            shopData.spawnY    = player.getY();
            shopData.spawnZ    = player.getZ();

            // Spawn a vanilla ArmorStand by default — no custom EntityType needed client-side.
            // Use /npcshop entitytype afterward to switch to a different (vanilla or modded)
            // entity type and/or enable its AI.
            Entity npc = ShopNpcEntity.create(com.zerog.neoessentials.util.LevelCompat.of(player), shopData.shopId, name,
                    shopData.entityTypeId, shopData.aiEnabled);
            npc.setPos(player.getX(), player.getY(), player.getZ());
            shopData.entityUUID = npc.getUUID();

            com.zerog.neoessentials.util.LevelCompat.of(player).addFreshEntity(npc);
            ShopEntityManager.getInstance().register(shopData);

            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.created",
                    name, shopData.shopId, shopData.shopId.toString().substring(0, 8)), true);
            return 1;
        } catch (Exception e) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.npcshop.error", e.getMessage()));
            return 0;
        }
    }

    // ── /npcshop remove ───────────────────────────────────────────────────────

    private static int executeRemove(CommandSourceStack src) {
        try {
            ServerPlayer player = src.getPlayerOrException();

            // Find nearest NeoEssentials shop entity (any type) within 5 blocks
            List<Entity> nearby = com.zerog.neoessentials.util.LevelCompat.of(player).getEntitiesOfClass(
                    Entity.class,
                    new AABB(player.getX() - 5, player.getY() - 5, player.getZ() - 5,
                             player.getX() + 5, player.getY() + 5, player.getZ() + 5),
                    ShopNpcEntity::isShopNpc);

            if (nearby.isEmpty()) {
                src.sendFailure(MessageUtil.component("commands.neoessentials.npcshop.remove_none_nearby"));
                return 0;
            }

            Entity target = nearby.getFirst();
            UUID shopId = ShopNpcEntity.getShopId(target);
            target.discard();

            if (shopId != null) {
                ShopEntityManager.getInstance().remove(shopId);
                src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.removed"), true);
            } else {
                src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.entity_removed_unlinked"), true);
            }
            return 1;
        } catch (Exception e) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.npcshop.error", e.getMessage()));
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
            src.sendFailure(MessageUtil.component("commands.neoessentials.npcshop.additem_both_disabled"));
            return 0;
        }

        // Keep a modded namespace as-is; only bare names get "minecraft:".
        String fullId = itemId.contains(":") ? itemId : "minecraft:" + itemId;
        if (com.zerog.neoessentials.shop.ShopTransaction.resolveItem(fullId).isEmpty()) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.npcshop.addcommand_bad_icon", itemId));
            return 0;
        }
        if (shop.listings.size() >= 54) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.npcshop.shop_full"));
            return 0;
        }
        shop.addListing(new ShopListing(fullId, null, buy, sell, qty));
        ShopEntityManager.getInstance().register(shop);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.additem_success",
                itemId, shop.shopName, shop.listings.size()), false);
        return 1;
    }

    // ── /npcshop additemhand <shop> <buy> <sell> ─────────────────────────────

    /** Adds the item in the admin's main hand — with its data (name, enchantments, modded NBT) and stack size. */
    private static int executeAddItemHand(CommandSourceStack src, String shopIdStr, double buyPrice, double sellPrice) {
        ServerPlayer player;
        try {
            player = src.getPlayerOrException();
        } catch (Exception e) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.npcshop.error", e.getMessage()));
            return 0;
        }
        ShopEntityData shop = resolve(src, shopIdStr);
        if (shop == null) return 0;

        net.minecraft.world.item.ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.npcshop.additemhand_empty"));
            return 0;
        }
        BigDecimal buy  = buyPrice  >= 0 ? BigDecimal.valueOf(buyPrice)  : null;
        BigDecimal sell = sellPrice >= 0 ? BigDecimal.valueOf(sellPrice) : null;
        if (buy == null && sell == null) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.npcshop.additem_both_disabled"));
            return 0;
        }
        if (shop.listings.size() >= 54) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.npcshop.shop_full"));
            return 0;
        }

        shop.addListing(new ShopListing(
                com.zerog.neoessentials.economy.worth.WorthManager.getItemId(held),
                com.zerog.neoessentials.shop.ShopParser.captureComponents(held),
                buy, sell, held.getCount()));
        ShopEntityManager.getInstance().register(shop);
        String name = held.getHoverName().getString();
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.additem_success",
                held.getCount() + "x " + name, shop.shopName, shop.listings.size()), false);
        return 1;
    }

    // ── /npcshop addcommand <shopId> <icon> <price> <command> ─────────────────

    /** Adds a paid command listing — {@code <icon>} is just the item shown in the GUI. */
    private static int executeAddCommand(CommandSourceStack src, String shopIdStr, String icon, double price, String command) {
        ShopEntityData shop = resolve(src, shopIdStr);
        if (shop == null) return 0;

        String iconId = icon.contains(":") ? icon : "minecraft:" + icon;
        if (com.zerog.neoessentials.shop.ShopTransaction.resolveItem(iconId).isEmpty()) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.npcshop.addcommand_bad_icon", icon));
            return 0;
        }
        if (shop.listings.size() >= 54) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.npcshop.shop_full"));
            return 0;
        }

        shop.addListing(ShopListing.command(iconId, BigDecimal.valueOf(price), command));
        ShopEntityManager.getInstance().register(shop);
        int slot = shop.listings.size();
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.addcommand_success",
                shop.shopName, slot, shop.shopId.toString().substring(0, 8)), false);
        return 1;
    }

    // ── /npcshop editlisting <shopId> <index> <name|addcommand|clearcommands|runas|permission> ──

    private static int editListing(CommandSourceStack src, com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String field) {
        ShopEntityData shop = resolve(src, StringArgumentType.getString(ctx, "shopId"));
        if (shop == null) return 0;
        int slot = IntegerArgumentType.getInteger(ctx, "index"); // 1-based, as shown in /npcshop info
        int index = slot - 1;
        if (index >= shop.listings.size()) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.npcshop.removeitem_invalid_index",
                    slot, shop.listings.size()));
            return 0;
        }
        ShopListing listing = shop.listings.get(index);
        boolean anyListing = field.equals("name") || field.equals("price");
        if (!anyListing && !listing.isCommandListing()) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.npcshop.editlisting_not_command", slot));
            return 0;
        }

        ShopListing updated;
        switch (field) {
            case "name" -> updated = listing.withDisplayName(StringArgumentType.getString(ctx, "name"));
            case "price" -> {
                double b = DoubleArgumentType.getDouble(ctx, "buyPrice");
                double s = DoubleArgumentType.getDouble(ctx, "sellPrice");
                updated = listing.withPrices(b >= 0 ? BigDecimal.valueOf(b) : null, s >= 0 ? BigDecimal.valueOf(s) : null);
            }
            case "addcommand" -> updated = listing.withAddedCommand(StringArgumentType.getString(ctx, "command"));
            case "clearcommands" -> updated = listing.withNoCommands();
            case "runas" -> {
                String mode = StringArgumentType.getString(ctx, "runas").toLowerCase();
                if (!mode.equals("console") && !mode.equals("player")) {
                    src.sendFailure(MessageUtil.component("commands.neoessentials.npcshop.editlisting_bad_runas"));
                    return 0;
                }
                updated = listing.withRunAsPlayer(mode.equals("player"));
            }
            default -> {
                String node = StringArgumentType.getString(ctx, "permission");
                updated = listing.withPermission(node.equalsIgnoreCase("none") ? null : node);
            }
        }
        shop.listings.set(index, updated);
        ShopEntityManager.getInstance().register(shop);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.editlisting_success", slot, field), false);
        return 1;
    }

    // ── /npcshop removeitem ───────────────────────────────────────────────────

    private static int executeRemoveItem(CommandSourceStack src, String shopIdStr, int slot) {
        ShopEntityData shop = resolve(src, shopIdStr);
        if (shop == null) return 0;
        if (!shop.removeListing(slot - 1)) { // 1-based, as shown in /npcshop info
            src.sendFailure(MessageUtil.component("commands.neoessentials.npcshop.removeitem_invalid_index",
                    slot, shop.listings.size()));
            return 0;
        }
        ShopEntityManager.getInstance().register(shop);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.removeitem_success", slot), false);
        return 1;
    }

    // ── /npcshop list ─────────────────────────────────────────────────────────

    private static int executeList(CommandSourceStack src) {
        var all = ShopEntityManager.getInstance().getAll();
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.list_header", all.size()), false);
        if (all.isEmpty()) {
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.list_empty"), false);
        } else {
            for (ShopEntityData d : all) {
                src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.list_entry",
                        d.shopName,
                        d.shopId.toString().substring(0, 8),
                        d.listings.size(),
                        String.format("%.0f", d.spawnX), String.format("%.0f", d.spawnY), String.format("%.0f", d.spawnZ),
                        d.dimension.replace("minecraft:", "")
                ), false);
            }
        }
        return all.size();
    }

    // ── /npcshop info <shopId> ────────────────────────────────────────────────

    private static int executeInfo(CommandSourceStack src, String shopIdStr) {
        ShopEntityData shop = resolve(src, shopIdStr);
        if (shop == null) return 0;
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.info_header"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.info_name", shop.shopName), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.info_id", shop.shopId), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.info_pos",
                (int) shop.spawnX, (int) shop.spawnY, (int) shop.spawnZ), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.info_items", shop.listings.size()), false);
        for (int i = 0; i < shop.listings.size(); i++) {
            ShopListing l = shop.listings.get(i);
            final int idx = i + 1; // 1-based slots, matching editlisting/removeitem
            if (l.isCommandListing()) {
                src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.info_command_listing",
                        idx, NpcShopMenu.listingLabel(l),
                        l.canBuy() ? l.buyPrice().toPlainString() : "§7—",
                        l.runAsPlayer() ? "player" : "console",
                        l.permission() != null ? l.permission() : "none"), false);
                for (String cmd : l.commands()) {
                    src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.info_command_line", cmd), false);
                }
                continue;
            }
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.info_listing",
                    idx, l.quantity(), NpcShopMenu.listingLabel(l),
                    l.canBuy()  ? l.buyPrice().toPlainString()  : "§7—",
                    l.canSell() ? l.sellPrice().toPlainString() : "§7—"
            ), false);
        }
        return 1;
    }

    // ── /npcshop reload ───────────────────────────────────────────────────────

    private static int executeReload(CommandSourceStack src) {
        ShopEntityManager.getInstance().reload();
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.reload_success",
                ShopEntityManager.getInstance().getShopCount()), true);
        return 1;
    }

    // ── /npcshop respawn <shopId> ─────────────────────────────────────────────

    /**
     * Re-summon the NPC entity for an existing shop whose entity was lost
     * (e.g. killed by void damage, which bypasses {@code setInvulnerable}, or removed
     * by an unrelated admin/anticheat command) without losing its listings — the
     * listings live in {@link ShopEntityData}, keyed by {@code shopId}, independent
     * of the in-world entity. Re-summons using the shop's current
     * {@link ShopEntityData#entityTypeId}/{@link ShopEntityData#aiEnabled}.
     */
    private static int executeRespawn(CommandSourceStack src, String shopIdStr) {
        ShopEntityData shop = resolve(src, shopIdStr);
        if (shop == null) return 0;

        var server = src.getServer();
        ServerLevel level = null;
        for (ServerLevel l : server.getAllLevels()) {
            if (l.dimension().location().toString().equals(shop.dimension)) { level = l; break; }
        }
        if (level == null) {
            src.sendFailure(MessageUtil.component(
                    "commands.neoessentials.npcshop.respawn_dimension_missing", shop.dimension));
            return 0;
        }

        if (shop.entityUUID != null && level.getEntity(shop.entityUUID) != null) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.npcshop.respawn_already_exists"));
            return 0;
        }

        Entity npc = ShopNpcEntity.create(level, shop.shopId, shop.shopName, shop.entityTypeId, shop.aiEnabled);
        npc.setPos(shop.spawnX, shop.spawnY, shop.spawnZ);
        level.addFreshEntity(npc);
        ShopEntityManager.getInstance().updateEntityUUID(shop.shopId, npc.getUUID());

        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.respawn_success", shop.shopName), true);
        return 1;
    }

    // ── /npcshop entitytype <shopId> <entityType> <ai> ───────────────────────────

    /**
     * Changes an existing shop's entity type and/or AI mode — vanilla or from any other
     * installed mod, see {@link ShopNpcEntity}'s javadoc for why that's safe. Despawns the
     * shop's current in-world entity (if present) and immediately respawns it with the new
     * settings at the same position, so the change is visible without a separate
     * {@code /npcshop respawn}.
     */
    private static int executeEntityType(CommandSourceStack src, String shopIdStr, String entityTypeId, Boolean aiArg) {
        ShopEntityData shop = resolve(src, shopIdStr);
        if (shop == null) return 0;
        boolean aiEnabled = aiArg != null ? aiArg : shop.aiEnabled;

        if (!ShopNpcEntity.isValidEntityTypeId(entityTypeId)) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.npcshop.entitytype_unknown", entityTypeId));
            return 0;
        }

        shop.entityTypeId = entityTypeId;
        shop.aiEnabled = aiEnabled;
        ShopEntityManager.getInstance().register(shop);

        var server = src.getServer();
        ServerLevel level = null;
        for (ServerLevel l : server.getAllLevels()) {
            if (l.dimension().location().toString().equals(shop.dimension)) { level = l; break; }
        }
        if (level == null) {
            // Data saved either way — the new type/AI will apply next time the shop is
            // respawned in a loaded dimension.
            src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.entitytype_success_no_respawn",
                    shop.shopName, entityTypeId), true);
            return 1;
        }

        if (shop.entityUUID != null) {
            Entity old = level.getEntity(shop.entityUUID);
            if (old != null) old.discard();
        }

        Entity npc = ShopNpcEntity.create(level, shop.shopId, shop.shopName, shop.entityTypeId, shop.aiEnabled);
        npc.setPos(shop.spawnX, shop.spawnY, shop.spawnZ);
        level.addFreshEntity(npc);
        ShopEntityManager.getInstance().updateEntityUUID(shop.shopId, npc.getUUID());

        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.entitytype_success",
                shop.shopName, entityTypeId, aiEnabled), true);
        return 1;
    }

    // ── /npcshop help ─────────────────────────────────────────────────────────

    private static int executeHelp(CommandSourceStack src) {
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.help_header"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.help_create"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.help_remove"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.help_additem"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.help_additemhand"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.help_addcommand"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.help_editlisting"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.help_removeitem"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.help_list"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.help_info"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.help_reload"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.help_respawn"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.help_entitytype"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.help_price_hint"), false);
        src.sendSuccess(() -> MessageUtil.component("commands.neoessentials.npcshop.help_shop_arg"), false);
        return 1;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Finds a shop by {@code this} (the shop NPC nearest the player, within 5 blocks), its full
     * id, an id prefix (the 8 characters shown in /npcshop list), or its name (case-insensitive).
     */
    private static ShopEntityData resolve(CommandSourceStack src, String shopIdStr) {
        ShopEntityData shop = null;
        if (shopIdStr.equalsIgnoreCase("this")) {
            shop = nearestShop(src);
            if (shop == null) {
                src.sendFailure(MessageUtil.component("commands.neoessentials.npcshop.remove_none_nearby"));
                return null;
            }
            return shop;
        }
        shop = ShopEntityManager.getInstance().getByShopId(shopIdStr);
        if (shop == null) {
            // Allow prefix matching (first 8 chars), then the shop's name
            for (ShopEntityData d : ShopEntityManager.getInstance().getAll()) {
                if (d.shopId.toString().startsWith(shopIdStr)) { shop = d; break; }
            }
        }
        if (shop == null) {
            for (ShopEntityData d : ShopEntityManager.getInstance().getAll()) {
                if (shopIdStr.equalsIgnoreCase(d.shopName)) { shop = d; break; }
            }
        }
        if (shop == null) {
            src.sendFailure(MessageUtil.component("commands.neoessentials.npcshop.not_found", shopIdStr));
        }
        return shop;
    }

    /** The registered shop whose NPC is closest to the command's player, within 5 blocks. */
    private static ShopEntityData nearestShop(CommandSourceStack src) {
        ServerPlayer player = src.getPlayer();
        if (player == null) return null;
        List<Entity> nearby = com.zerog.neoessentials.util.LevelCompat.of(player).getEntitiesOfClass(
                Entity.class, player.getBoundingBox().inflate(5), ShopNpcEntity::isShopNpc);
        return nearby.stream()
                .sorted(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .map(ShopNpcEntity::getShopId)
                .filter(java.util.Objects::nonNull)
                .map((UUID id) -> ShopEntityManager.getInstance().getByShopId(id))
                .filter(java.util.Objects::nonNull)
                .findFirst().orElse(null);
    }
}

