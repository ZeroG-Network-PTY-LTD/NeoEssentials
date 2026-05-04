package com.zerog.neoessentials.hologram.integration;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.economy.managers.EconomyManager;
import com.zerog.neoessentials.hologram.*;
import com.zerog.neoessentials.shop.ShopManager;
import com.zerog.neoessentials.shop.ShopTransaction;
import com.zerog.neoessentials.shop.ShopTransaction.TransactionResult;
import com.zerog.neoessentials.shop.ShopParser;
import com.zerog.neoessentials.shop.events.ShopTransactionEvent;
import com.zerog.neoessentials.shop.model.ShopData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Locale;
/**
 * Auto-creates holographic displays above sign shops so that long item names
 * and pricing info are visible even when they don't fit on the sign itself.
 *
 * <p>Call {@link #createShopHologram(ShopData, String)} when a shop sign is placed,
 * and {@link #deleteShopHologram(String, String)} when the sign is broken.
 *
 * <p>Also listens to {@link ShopTransactionEvent} to refresh the hologram text
 * after each buy/sell.
 */
@EventBusSubscriber(modid = "neoessentials")
public class ShopHologramManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShopHologramManager.class);
    /** Prefix for all shop-auto-holograms so they can be identified / cleaned up. */
    public static final String SHOP_HOLOGRAM_PREFIX = "shop_";
    // ── Public API ────────────────────────────────────────────────────────────
    /**
     * Create (or update) a hologram above a shop sign.
     *
     * @param shop         the shop to display
     * @param dimensionKey e.g. {@code "minecraft:overworld"}
     */
    public static void createShopHologram(ShopData shop, String dimensionKey) {
        try {
            String id = shopHologramId(shop);
            HologramData data = new HologramData();
            data.id = id;
            data.world = dimensionKey;
            // Position: centred on sign block, ~1.8 blocks above it
            data.x = shop.signX + 0.5;
            data.y = shop.signY + 1.8;
            data.z = shop.signZ + 0.5;
            data.refreshInterval = 10;
            data.visible = true;
            if (data.entityUUIDs == null) data.entityUUIDs = new ArrayList<>();
            data.lines = buildShopLines(shop);
            HologramManager.getInstance().registerHologram(data);
            spawnInLevel(data);
            LOGGER.debug("[ShopHologram] Created hologram '{}' for shop at {}", id, shop.toKey());
        } catch (Exception e) {
            LOGGER.error("[ShopHologram] Failed to create shop hologram: {}", e.getMessage(), e);
        }
    }
    /** Remove the hologram that was auto-created for the given shop sign position. */
    public static void deleteShopHologram(String dimensionKey, String shopKey) {
        try {
            // shopKey is in format "dim@x,y,z" - derive id from it
            // try to find the hologram by searching shop prefix holograms
            for (HologramData d : HologramManager.getInstance().getAllHolograms()) {
                if (d.id.startsWith(SHOP_HOLOGRAM_PREFIX) && d.world.equals(dimensionKey)) {
                    // Check if position matches
                    String key = dimensionKey + "@" + (int)Math.floor(d.x - 0.5) + "," + (int)Math.floor(d.y - 1.8) + "," + (int)Math.floor(d.z - 0.5);
                    if (key.equals(shopKey)) {
                        ServerLevel level = findLevel(dimensionKey);
                        if (level != null) HologramRenderer.despawn(d, level);
                        HologramManager.getInstance().removeHologram(d.id);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("[ShopHologram] Error deleting shop hologram: {}", e.getMessage());
        }
    }
    /** Convenience overload using ShopData directly. */
    public static void deleteShopHologram(ShopData shop) {
        try {
            String id = shopHologramId(shop);
            HologramData data = HologramManager.getInstance().getHologram(id);
            if (data != null) {
                ServerLevel level = findLevel(data.world);
                if (level != null) HologramRenderer.despawn(data, level);
                HologramManager.getInstance().removeHologram(id);
            }
        } catch (Exception e) {
            LOGGER.debug("[ShopHologram] Error deleting shop hologram: {}", e.getMessage());
        }
    }
    /**
     * Refresh the hologram text for a shop (e.g. after a transaction updates stock).
     */
    public static void refreshShopHologram(ShopData shop) {
        try {
            String id = shopHologramId(shop);
            HologramData data = HologramManager.getInstance().getHologram(id);
            if (data == null) return;
            data.lines = buildShopLines(shop);
            HologramManager.getInstance().registerHologram(data);
            ServerLevel level = findLevel(data.world);
            if (level != null) HologramRenderer.spawn(data, level);
        } catch (Exception e) {
            LOGGER.debug("[ShopHologram] Error refreshing shop hologram: {}", e.getMessage());
        }
    }
    // ── Event listeners ───────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onShopTransaction(ShopTransactionEvent event) {
        try {
            refreshShopHologram(event.getShop());
        } catch (Exception ignored) {}
    }

    /**
     * Right-clicking a shop hologram entity is identical to right-clicking the sign → BUY.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getTarget() instanceof Display.TextDisplay display)) return;

        ShopData shop = shopFromHologramEntity(display, player.serverLevel());
        if (shop == null) return;

        event.setCanceled(true);

        // Owner right-clicks → show info (same behaviour as sign)
        if (shop.ownerUUID != null && shop.ownerUUID.equals(player.getUUID())) {
            sendShopInfo(player, shop);
            return;
        }

        // Item autofill: owner of a pending "?" shop right-clicks with item in hand
        if (shop.itemPending) {
            net.minecraft.world.item.ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
            boolean canAssign = shop.isAdminShop()
                    ? PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.create.admin")
                    : (shop.ownerUUID != null && shop.ownerUUID.equals(player.getUUID()));
            if (canAssign && !held.isEmpty()) {
                shop.itemId      = com.zerog.neoessentials.economy.worth.WorthManager.getItemId(held);
                shop.itemPending = false;
                ShopManager.getInstance().registerShop(shop);
                player.sendSystemMessage(Component.literal(
                    "§aItem set to §f" + ShopParser.buildItemDisplayName(shop.itemId) + "§a!"));
            } else {
                player.sendSystemMessage(Component.literal("§eHold the item you want to trade, then right-click the hologram."));
            }
            return;
        }

        if (!PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.use")) {
            player.sendSystemMessage(Component.literal("§cYou don't have permission to use shops."));
            return;
        }
        if (!shop.canBuy()) {
            player.sendSystemMessage(Component.literal("§cThis shop does not sell items."));
            return;
        }
        TransactionResult result = ShopTransaction.executeBuy(player, shop, player.serverLevel());
        sendTransactionResult(player, result, shop, true);
    }

    /**
     * Left-clicking (attacking) a shop hologram entity → SELL.
     * Display entities are invulnerable so no damage is dealt.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof Display.TextDisplay display)) return;

        ShopData shop = shopFromHologramEntity(display, player.serverLevel());
        if (shop == null) return;

        event.setCanceled(true);

        // Owner left-clicks → show info
        if (shop.ownerUUID != null && shop.ownerUUID.equals(player.getUUID())) {
            sendShopInfo(player, shop);
            return;
        }

        if (!PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.use")) {
            player.sendSystemMessage(Component.literal("§cYou don't have permission to use shops."));
            return;
        }
        if (!shop.canSell()) {
            player.sendSystemMessage(Component.literal("§cThis shop does not buy items."));
            return;
        }
        TransactionResult result = ShopTransaction.executeSell(player, shop, player.serverLevel());
        sendTransactionResult(player, result, shop, false);
    }
    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Given a TextDisplay entity, return the ShopData it belongs to, or {@code null}
     * if this entity is not a shop hologram.
     *
     * <p>Shop holograms are always positioned at signX+0.5, signY+1.8, signZ+0.5
     * (see {@link #createShopHologram}), so we reverse-compute the sign block pos.
     */
    private static ShopData shopFromHologramEntity(Display.TextDisplay entity, ServerLevel level) {
        try {
            net.minecraft.nbt.CompoundTag data = entity.getPersistentData();
            if (!data.contains("neoessentials_hologram")) return null;
            String holoId = data.getString("neoessentials_hologram_id");
            if (!holoId.startsWith(SHOP_HOLOGRAM_PREFIX)) return null;

            HologramData holo = HologramManager.getInstance().getHologram(holoId);
            if (holo == null) return null;

            int signX = (int) Math.floor(holo.x - 0.5);
            int signY = (int) Math.floor(holo.y - 1.8);
            int signZ = (int) Math.floor(holo.z - 0.5);
            String dimension = HologramRenderer.dimensionKey(level);
            return ShopManager.getInstance().getShopBySign(dimension, new BlockPos(signX, signY, signZ));
        } catch (Exception e) {
            LOGGER.debug("[ShopHologram] shopFromHologramEntity error: {}", e.getMessage());
            return null;
        }
    }

    private static void sendShopInfo(ServerPlayer player, ShopData shop) {
        String currency = EconomyManager.getInstance().getCurrencySymbol();
        String itemDisplay = ShopParser.buildItemDisplayName(shop.itemId);
        player.sendSystemMessage(Component.literal("§6§l--- Shop Info ---"));
        player.sendSystemMessage(Component.literal("§eOwner: §f" + shop.ownerName));
        player.sendSystemMessage(Component.literal("§eItem:  §f" + shop.quantity + "x " + itemDisplay));
        if (shop.buyPrice  != null) player.sendSystemMessage(Component.literal("§eBuy:   §f" + currency + shop.buyPrice.toPlainString()));
        if (shop.sellPrice != null) player.sendSystemMessage(Component.literal("§eSell:  §f" + currency + shop.sellPrice.toPlainString()));
    }

    private static void sendTransactionResult(ServerPlayer player, TransactionResult result,
                                              ShopData shop, boolean buying) {
        String currency = EconomyManager.getInstance().getCurrencySymbol();
        String itemDisplay = ShopParser.buildItemDisplayName(shop.itemId);
        switch (result.type) {
            case SUCCESS -> {
                if (buying) {
                    player.sendSystemMessage(Component.literal(String.format(
                        "§aYou bought §f%dx %s §afor §f%s%s§a from §f%s§a.",
                        result.quantity, itemDisplay, currency, result.price.toPlainString(), shop.ownerName)));
                } else {
                    player.sendSystemMessage(Component.literal(String.format(
                        "§aYou sold §f%dx %s §afor §f%s%s§a.",
                        result.quantity, itemDisplay, currency, result.price.toPlainString())));
                }
            }
            case NOT_ENOUGH_MONEY -> player.sendSystemMessage(Component.literal(buying
                ? "§cYou don't have enough money to buy that."
                : "§cThe shop owner can't afford to buy that."));
            case NOT_ENOUGH_STOCK -> player.sendSystemMessage(Component.literal(buying
                ? "§cThis shop is out of stock."
                : "§cYou don't have enough of that item."));
            case NO_SPACE -> player.sendSystemMessage(Component.literal(buying
                ? "§cYour inventory is full."
                : "§cThe shop's chest is full."));
            case NO_CHEST -> player.sendSystemMessage(Component.literal("§cShop has no linked chest."));
            case SHOP_DISABLED -> player.sendSystemMessage(Component.literal(buying
                ? "§cThis shop doesn't sell items."
                : "§cThis shop doesn't buy items."));
            default -> player.sendSystemMessage(Component.literal("§cTransaction failed (internal error)."));
        }
    }

    private static String shopHologramId(ShopData shop) {
        return (SHOP_HOLOGRAM_PREFIX + shop.signDimension + "_" + shop.signX + "_" + shop.signY + "_" + shop.signZ)
            .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }
    private static java.util.List<HologramLine> buildShopLines(ShopData shop) {
        java.util.List<HologramLine> lines = new ArrayList<>();
        // Line 1: Item name (solves the "too long for sign" problem)
        String itemName = shop.itemId != null ? shop.itemId.replace("minecraft:", "") : "?";
        String ownerDisplay = shop.isAdminShop() ? "&6[Admin Shop]" : ("&e" + shop.ownerName);
        lines.add(new HologramLine(ownerDisplay + " &7| &f" + itemName + " &8x" + shop.quantity));
        // Line 2: Buy / Sell prices
        String buyStr  = shop.canBuy()  ? "&aBuy: &f$" + shop.buyPrice.toPlainString()   : "";
        String sellStr = shop.canSell() ? "&cSell: &f$" + shop.sellPrice.toPlainString() : "";
        String priceLine;
        if (!buyStr.isEmpty() && !sellStr.isEmpty()) priceLine = buyStr + " &7| " + sellStr;
        else if (!buyStr.isEmpty()) priceLine = buyStr;
        else if (!sellStr.isEmpty()) priceLine = sellStr;
        else priceLine = "&7(no transactions)";
        lines.add(new HologramLine(priceLine));
        return lines;
    }
    private static void spawnInLevel(HologramData data) {
        try {
            ServerLevel level = findLevel(data.world);
            if (level != null) HologramRenderer.spawn(data, level);
        } catch (Exception e) {
            LOGGER.debug("[ShopHologram] Could not spawn in level: {}", e.getMessage());
        }
    }
    private static ServerLevel findLevel(String dimensionKey) {
        try {
            net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return null;
            for (ServerLevel level : server.getAllLevels()) {
                if (HologramRenderer.dimensionKey(level).equals(dimensionKey)) return level;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
