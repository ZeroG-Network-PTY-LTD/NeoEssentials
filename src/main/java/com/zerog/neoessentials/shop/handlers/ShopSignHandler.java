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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens for sign placement and subsequent text finalization to register new ChestShop signs.
 *
 * <h3>Flow</h3>
 * <ol>
 *   <li>{@link BlockEvent.EntityPlaceEvent} — a sign is placed; we record the position + player as "pending".</li>
 *   <li>{@link ServerTickEvent.Post} — every tick we re-check pending signs.
 *       Once a sign has text on all 4 lines (player finished editing), we attempt to parse it
 *       as a shop. Pending entries time out after 30 seconds if never filled.</li>
 * </ol>
 *
 * This deferred approach is necessary because NeoForge 1.21.1 has no sign-text-written event;
 * the {@code ServerboundSignUpdatePacket} is processed server-side before any NeoForge event fires.
 */
@EventBusSubscriber(modid = "neoessentials")
public class ShopSignHandler {

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
        String dimension = level.dimension().location().toString();
        String key = dimension + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
        pending.put(key, new PendingSign(player.getUUID(), dimension, System.currentTimeMillis()));
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
                    if (sl.dimension().location().toString().equals(ps.dimension())) {
                        level = sl; break;
                    }
                }

                BlockEntity be = level.getBlockEntity(pos);
                if (!(be instanceof SignBlockEntity sign)) { iter.remove(); continue; }

                // Read text from the sign
                String[] lines = readSignLines(sign);
                // Only proceed if line 0 is non-empty (player finished editing)
                if (lines[0].isBlank()) continue; // not yet edited

                // If it doesn't look like a shop (no B or S on line 2), remove from pending silently
                String priceLine = lines[ShopData.PRICE_LINE].toUpperCase().trim();
                if (!priceLine.contains("B") && !priceLine.contains("S")) {
                    iter.remove(); continue;
                }

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
            final int lineIdx = i;
            final String text = lines[i] != null ? lines[i] : "";
            Component component = net.minecraft.network.chat.Component.literal(text);
            var currentText = sign.getFrontText();
            var newText = currentText.setMessage(lineIdx, component);
            sign.updateText(s -> newText, true);
        }
        sign.setChanged();
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 3);
    }
}

