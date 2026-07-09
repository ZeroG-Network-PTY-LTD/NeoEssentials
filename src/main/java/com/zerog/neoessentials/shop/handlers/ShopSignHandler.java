package com.zerog.neoessentials.shop.handlers;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.shop.ShopManager;
import com.zerog.neoessentials.shop.ShopParser;
import com.zerog.neoessentials.shop.model.ShopData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.ClickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens for sign placement, text finalization, and sign breakage to manage ChestShop signs.
 *
 * <h3>Registration flow</h3>
 * <ol>
 *   <li>{@link BlockEvent.EntityPlaceEvent} — a sign is placed; we record the position + player as "pending".</li>
 *   <li>{@link ServerTickEvent.Post} — every tick we re-check pending signs.
 *       Once a sign has text on all 4 lines (player finished editing), we attempt to parse it
 *       as a shop. Pending entries time out after 30 seconds if never filled.</li>
 * </ol>
 *
 * <h3>Removal flow</h3>
 * <ul>
 *   <li>{@link BreakBlockEvent} — when a sign block is broken, any shop registered at
 *       that position is removed (including its hologram) via {@link ShopManager#removeShop}.</li>
 * </ul>
 *
 * This deferred approach is necessary because NeoForge 1.21.1 has no sign-text-written event;
 * the {@code ServerboundSignUpdatePacket} is processed server-side before any NeoForge event fires.
 */
@EventBusSubscriber(modid = "neoessentials")
public class ShopSignHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShopSignHandler.class);

    // ── Pending sign queue ────────────────────────────────────────────────────

    private record PendingSign(UUID playerUUID, String dimension, long placedAt) {}

    /** sign position key → pending entry */
    private static final Map<String, PendingSign> pending = new ConcurrentHashMap<>();
    private static final long TIMEOUT_MS = 30_000L; // 30 seconds
    private static final int CHECK_INTERVAL_TICKS = 5; // check every 5 ticks
    private static int tickCounter = 0;

    // ── Sign placed ───────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onSignPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockState state = event.getState();
        if (!(state.getBlock() instanceof SignBlock)) return;

        BlockPos pos = event.getPos();
        String dimension = level.dimension().identifier().toString();
        String key = dimension + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
        pending.put(key, new PendingSign(player.getUUID(), dimension, System.currentTimeMillis()));
    }

    // ── Sign broken ───────────────────────────────────────────────────────────

    /**
     * When a sign block is broken, remove any shop registered at that position.
     * ShopManager.removeShop() also calls ShopHologramManager.deleteShopHologram()
     * so the floating hologram is cleaned up atomically with the shop data.
     *
     * <p>This fires BEFORE the block is removed from the world, so the BlockEntity
     * is still accessible (though we only need the position and dimension here).
     */
    @SubscribeEvent
    public static void onSignBroken(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        // Only care about sign blocks
        if (!(level.getBlockState(pos).getBlock() instanceof SignBlock)) return;
        String dimension = level.dimension().identifier().toString();
        // Also remove from the pending queue in case the sign was placed but never written
        String key = dimension + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
        pending.remove(key);
        // Remove the shop (and its hologram via ShopManager → ShopHologramManager)
        ShopData removed = ShopManager.getInstance().removeShop(dimension, pos);
        if (removed != null) {
            LOGGER.debug("[ChestShop] Removed shop at {} (sign broken by {})",
                key, event.getPlayer() != null ? event.getPlayer().getName().getString() : "unknown");
        }
    }

    // ── Tick check ────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++tickCounter % CHECK_INTERVAL_TICKS != 0) return;

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, PendingSign>> iter = pending.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<String, PendingSign> entry = iter.next();
            PendingSign ps = entry.getValue();

            // Time out stale entries
            if (now - ps.placedAt() > TIMEOUT_MS) {
                iter.remove();
                continue;
            }

            // Parse the key back to pos/dimension
            String key = entry.getKey();
            String[] atSplit = key.split("@", 2);
            if (atSplit.length < 2) { iter.remove(); continue; }
            String[] xyzParts = atSplit[1].split(",");
            if (xyzParts.length < 3) { iter.remove(); continue; }
            try {
                int x = Integer.parseInt(xyzParts[0]);
                int y = Integer.parseInt(xyzParts[1]);
                int z = Integer.parseInt(xyzParts[2]);
                BlockPos pos = new BlockPos(x, y, z);

                // Get the level via the server
                var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                if (server == null) continue;
                ServerLevel level = server.getAllLevels().iterator().next(); // fallback
                for (ServerLevel sl : server.getAllLevels()) {
                    if (sl.dimension().identifier().toString().equals(ps.dimension())) {
                        level = sl; break;
                    }
                }

                BlockEntity be = level.getBlockEntity(pos);
                if (!(be instanceof SignBlockEntity sign)) { iter.remove(); continue; }

                // Read text from the sign
                String[] lines = readSignLines(sign);

                // Price line must have B or S to be a shop sign — skip silently if not
                String priceLine = lines[ShopData.PRICE_LINE].toUpperCase().trim();
                if (!priceLine.contains("B") && !priceLine.contains("S")) {
                    // Not a shop sign — don't process yet, but also don't time out immediately
                    // Wait for the timeout to clean it up
                    continue;
                }

                // Quantity line must be non-empty before we can proceed
                if (lines[ShopData.QUANTITY_LINE].isBlank()) continue;

                iter.remove(); // done processing this pending entry

                // Get the player
                ServerPlayer player = server.getPlayerList().getPlayer(ps.playerUUID());
                if (player == null) continue; // player left

                tryRegisterShop(player, lines, pos, ps.dimension(), level);

            } catch (Exception e) {
                iter.remove();
            }
        }
    }

    // ── Shop registration ─────────────────────────────────────────────────────

    /**
     * Attempt to register the sign at {@code pos} as a ChestShop.
     * Also called externally from /chestshop convert for existing signs.
     *
     * <p>Handles both auto-assign features:
     * <ul>
     *   <li>Line 0 blank → auto-assigned to the placing player's name</li>
     *   <li>Line 3 "?"   → shop registered in pending state; owner must right-click with item</li>
     * </ul>
     */
    public static void tryRegisterShop(ServerPlayer player, String[] lines,
                                       BlockPos pos, String dimension, ServerLevel level) {
        // Strip colour codes from line 0 before permission/admin check
        String ownerLine = lines[ShopData.NAME_LINE].replaceAll("§[0-9a-fA-FkKlLmMnNoOrRiI]", "").trim();

        // [Shop] header → auto-assign to placing player (alternative to blank line)
        if (SHOP_HEADER.equalsIgnoreCase(ownerLine)) {
            ownerLine = player.getName().getString();
            lines[ShopData.NAME_LINE] = ownerLine;
        }

        // Blank line 0 → auto-assign this player
        if (ownerLine.isEmpty()) {
            ownerLine = player.getName().getString();
            lines[ShopData.NAME_LINE] = ownerLine;
        }

        boolean wantsAdmin = ShopData.ADMIN_SHOP_NAME.equalsIgnoreCase(ownerLine);

        if (wantsAdmin && !PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.create.admin")) {
            player.sendSystemMessage(Component.literal("§cYou don't have permission to create admin shops."));
            return;
        }
        if (!wantsAdmin && !PermissionAPI.hasPermission(player.getUUID(), "neoessentials.shop.create")) {
            player.sendSystemMessage(Component.literal("§cYou don't have permission to create shops."));
            return;
        }

        // Per-player shop limit (skip for admin shops)
        if (!wantsAdmin) {
            int used = ShopManager.getInstance().getShopsByOwner(player.getUUID()).size();
            int max  = getMaxShopsPerPlayer();
            if (max > 0 && used >= max) {
                player.sendSystemMessage(Component.literal(
                    "§cShop limit reached! You have §f" + used + "§c/§f" + max + " §cshops. Remove one to create another."));
                return;
            }
        }

        // Check for duplicate sign position before parsing
        ShopData existing = ShopManager.getInstance().getShopBySign(dimension, pos);
        if (existing != null) {
            player.sendSystemMessage(Component.literal("§cA shop already exists at this sign."));
            return;
        }

        Optional<ShopData> parsed = ShopParser.parse(
            lines, pos, dimension, level, player.getUUID(), player.getName().getString());

        if (parsed.isEmpty()) {
            if (!wantsAdmin && ShopParser.findAdjacentChest(pos, level) == null) {
                player.sendSystemMessage(Component.literal(
                    "§cNo chest found next to this sign. Place a chest first."));
            } else {
                player.sendSystemMessage(Component.literal(
                    "§cInvalid shop sign format.  Lines: [name or blank] / [qty] / [B x:S y] / [item or ?]"));
            }
            return;
        }

        ShopData shop = parsed.get();
        ShopManager.getInstance().registerShop(shop);

        // Rewrite sign lines with colour formatting
        writeSignLines(level, pos, ShopParser.formatSignLines(shop));

        if (shop.itemPending) {
            // Shop is registered but non-functional — item still needed
            player.sendSystemMessage(Component.literal(
                "§aShop frame created! §eRight-click this sign while holding the item you want to sell/buy."));
        } else {
            player.sendSystemMessage(Component.literal("§aShop created successfully!"));
            String currency = com.zerog.neoessentials.economy.managers.EconomyManager.getInstance().getCurrencySymbol();
            if (!shop.isAdminShop()) {
                if (shop.buyPrice  != null) player.sendSystemMessage(Component.literal(
                    "§eBuy price:  §f" + currency + shop.buyPrice.toPlainString()));
                if (shop.sellPrice != null) player.sendSystemMessage(Component.literal(
                    "§eSell price: §f" + currency + shop.sellPrice.toPlainString()));
            } else {
                player.sendSystemMessage(Component.literal("§2[Admin Shop] Unlimited stock."));
            }
        }

        // ── Hologram opt-in prompt ────────────────────────────────────────────
        // Only offer a hologram if there isn't one already (e.g. re-conversion).
        if (!shop.hologramEnabled) {
            String enableCmd = "/chestshop hologram enablepos "
                + shop.signX + " " + shop.signY + " " + shop.signZ;
            Component holoPrompt = Component.literal("§eWant a hologram above your shop? ")
                .append(Component.literal("§a§l[Click to Enable]")
                    .withStyle(s -> s.withClickEvent(com.zerog.neoessentials.util.ClickEventCompat.create(
                        ClickEvent.Action.RUN_COMMAND, enableCmd)))
                    .withStyle(s -> s.withHoverEvent(
                        com.zerog.neoessentials.util.HoverEventCompat.create(
                            net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                            Component.literal("§7Enables a floating text display above\nyour sign shop. You can move it later with\n§e/chestshop hologram move <x> <y> <z>")))));
            player.sendSystemMessage(holoPrompt);
        }
    }

    // ── Sign text helpers ─────────────────────────────────────────────────────

    /** Read raw plain text from all 4 front-face lines of a sign. */
    public static String[] readSignLines(SignBlockEntity sign) {
        String[] lines = new String[4];
        var signText = sign.getFrontText();
        for (int i = 0; i < 4; i++) {
            Component msg = signText.getMessage(i, false);
            lines[i] = msg.getString();
        }
        return lines;
    }

    /** Write formatted text back to a sign's 4 lines and sync to clients. */
    public static void writeSignLines(ServerLevel level, BlockPos pos, String[] lines) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SignBlockEntity sign)) return;
        for (int i = 0; i < 4 && i < lines.length; i++) {
            final String text = lines[i] != null ? lines[i] : "";
            Component component = net.minecraft.network.chat.Component.literal(text);
            var currentText = sign.getFrontText();
            var newText = currentText.setMessage(i, component);
            sign.updateText(s -> newText, true);
        }
        sign.setChanged();
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 3);
    }

    // ── Config helpers ────────────────────────────────────────────────────────

    /** Alternative trigger recognised on line 0 (in addition to owner name / blank). */
    public static final String SHOP_HEADER = "[Shop]";

    private static int getMaxShopsPerPlayer() {
        try {
            var cfg = com.zerog.neoessentials.config.ConfigManager.getInstance()
                    .getConfig(com.zerog.neoessentials.config.ConfigManager.MAIN_CONFIG);
            if (cfg != null && cfg.has("shop")) {
                var shopCfg = cfg.getAsJsonObject("shop");
                if (shopCfg.has("maxShopsPerPlayer")) return shopCfg.get("maxShopsPerPlayer").getAsInt();
            }
        } catch (Exception ignored) {}
        return 10; // default
    }
}

