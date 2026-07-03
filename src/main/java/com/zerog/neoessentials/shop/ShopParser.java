package com.zerog.neoessentials.shop;

import com.zerog.neoessentials.economy.worth.WorthManager;
import com.zerog.neoessentials.shop.model.ShopData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Parses raw sign-line arrays into {@link ShopData} instances.
 *
 * <h3>Sign format:</h3>
 * <pre>
 *  Line 0: owner name    — player name or "Admin Shop"
 *  Line 1: quantity      — positive integer, max 3456
 *  Line 2: price line    — "B &lt;n&gt;", "S &lt;n&gt;", or "B &lt;n&gt;:S &lt;n&gt;" (also "free")
 *  Line 3: item          — item id (e.g. "diamond", "minecraft:diamond")
 * </pre>
 */
public final class ShopParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShopParser.class);
    public static final int MAX_QUANTITY = 3456; // double-chest of 54 stacks
    /** The autofill code players can write on line 3 to assign the item later by right-clicking. */
    public static final String AUTOFILL_CODE = "?";

    private ShopParser() {}

    /**
     * Parse sign lines into a {@link ShopData}.
     *
     * <p>Special values:
     * <ul>
     *   <li>Line 0 blank → auto-assigned to the player placing the sign (ownerUUID must be non-null)</li>
     *   <li>Line 3 "?" → shop is created in {@link ShopData#itemPending} state;
     *       the owner must right-click the sign holding an item to complete it</li>
     * </ul>
     *
     * @param lines      raw sign text (4 elements)
     * @param signPos    world position of the sign
     * @param dimension  dimension key string
     * @param level      server level (used for chest adjacency lookup)
     * @param ownerUUID  UUID of the player placing the sign (null → admin shop resolved from name)
     * @param ownerName  display name of the placing player (used for auto-assign)
     * @return populated ShopData, or empty on validation failure
     */
    public static Optional<ShopData> parse(String[] lines, BlockPos signPos,
                                           String dimension, ServerLevel level,
                                           UUID ownerUUID, String ownerName) {
        if (lines == null || lines.length < 4) return Optional.empty();

        ShopData shop = new ShopData();

        // ── Line 0: owner ─────────────────────────────────────────────────────
        String ownerLine = strip(lines[ShopData.NAME_LINE]);

        // Blank → auto-assign to the placing player
        if (ownerLine.isEmpty()) {
            if (ownerUUID == null || ownerName == null || ownerName.isBlank()) {
                LOGGER.debug("[ChestShop] Blank owner line but no player context at {}", signPos);
                return Optional.empty();
            }
            ownerLine = ownerName;
        }

        if (ShopData.ADMIN_SHOP_NAME.equalsIgnoreCase(ownerLine)) {
            shop.ownerUUID = null; // admin shop
        } else {
            shop.ownerUUID = ownerUUID;
        }
        shop.ownerName = ownerLine;

        // ── Line 1: quantity ──────────────────────────────────────────────────
        String qtyStr = strip(lines[ShopData.QUANTITY_LINE]);
        int quantity;
        try {
            quantity = Integer.parseInt(qtyStr);
        } catch (NumberFormatException e) {
            LOGGER.debug("[ChestShop] Invalid quantity '{}' at {}", qtyStr, signPos);
            return Optional.empty();
        }
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            LOGGER.debug("[ChestShop] Quantity {} out of range at {}", quantity, signPos);
            return Optional.empty();
        }
        shop.quantity = quantity;

        // ── Line 2: price ─────────────────────────────────────────────────────
        String priceLine = strip(lines[ShopData.PRICE_LINE]).toUpperCase();
        if (!parsePriceLine(priceLine, shop)) {
            LOGGER.debug("[ChestShop] Invalid price line '{}' at {}", priceLine, signPos);
            return Optional.empty();
        }
        if (!shop.canBuy() && !shop.canSell()) return Optional.empty();

        // ── Line 3: item ──────────────────────────────────────────────────────
        String itemRaw = strip(lines[ShopData.ITEM_LINE]);
        if (itemRaw.isEmpty()) return Optional.empty();

        // "?" autofill
        if (AUTOFILL_CODE.equals(itemRaw)) {
            shop.itemId      = null;
            shop.itemPending = true;
        } else {
            // Normalise for registry lookup (spaces→underscores, lowercase)
            String itemStr = normalizeItemStr(itemRaw);

            ItemStack resolved = resolveItem(itemStr);
            if (resolved == null || resolved.isEmpty()) {
                LOGGER.debug("[ChestShop] Could not resolve item '{}' (normalised: '{}') at {} — " +
                    "check the item ID is correct (e.g. 'thermal:copper_ingot' or 'diamond')",
                    itemRaw, itemStr, signPos);
                return Optional.empty();
            }
            shop.itemId      = WorthManager.getItemId(resolved);
            shop.itemPending = false;
        }

        // ── Sign position ─────────────────────────────────────────────────────
        shop.signDimension = dimension;
        shop.signX = signPos.getX();
        shop.signY = signPos.getY();
        shop.signZ = signPos.getZ();

        // ── Chest adjacency ───────────────────────────────────────────────────
        if (!shop.isAdminShop()) {
            BlockPos chestPos = findAdjacentChest(signPos, level);
            if (chestPos == null) {
                LOGGER.debug("[ChestShop] No chest found adjacent to sign at {}", signPos);
                return Optional.empty();
            }
            shop.hasChest      = true;
            shop.chestDimension = dimension;
            shop.chestX = chestPos.getX();
            shop.chestY = chestPos.getY();
            shop.chestZ = chestPos.getZ();
        } else {
            shop.hasChest = false;
        }

        return Optional.of(shop);
    }

    /**
     * Backwards-compat overload — derives ownerName from the UUID via the server player list.
     * Prefer the 6-arg version when the name is already known.
     */
    public static Optional<ShopData> parse(String[] lines, BlockPos signPos,
                                           String dimension, ServerLevel level,
                                           UUID ownerUUID) {
        String name = null;
        if (ownerUUID != null) {
            var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                var p = server.getPlayerList().getPlayer(ownerUUID);
                if (p != null) name = p.getName().getString();
            }
        }
        return parse(lines, signPos, dimension, level, ownerUUID, name);
    }

    // ── Price line parser ─────────────────────────────────────────────────────

    /**
     * Fills buyPrice and sellPrice from formats:
     * <ul>
     *   <li>{@code B 10} or {@code B10}           — buy only</li>
     *   <li>{@code S 5}  or {@code S5}             — sell only</li>
     *   <li>{@code B 10:S 5}                       — both</li>
     *   <li>{@code FREE}                           — free (price = 0)</li>
     * </ul>
     */
    private static boolean parsePriceLine(String line, ShopData shop) {
        // Both: "B <n>:S <n>" or "S <n>:B <n>"
        if (line.contains(":")) {
            String[] parts = line.split(":", 2);
            BigDecimal p0 = parsePricePart(parts[0].trim());
            BigDecimal p1 = parsePricePart(parts[1].trim());
            if (p0 == null || p1 == null) return false;
            String t0 = parts[0].trim();
            String t1 = parts[1].trim();
            if (t0.startsWith("B")) { shop.buyPrice  = p0; }
            else if (t0.startsWith("S")) { shop.sellPrice = p0; }
            else return false;
            if (t1.startsWith("B")) { shop.buyPrice  = p1; }
            else if (t1.startsWith("S")) { shop.sellPrice = p1; }
            else return false;
            return true;
        }
        // Single side
        BigDecimal price = parsePricePart(line);
        if (price == null) return false;
        if (line.startsWith("B")) { shop.buyPrice  = price; return true; }
        if (line.startsWith("S")) { shop.sellPrice = price; return true; }
        return false;
    }

    /** Parse "B 10", "B10", "S 5.5", "FREE", "BFREE" etc. → BigDecimal (>=0). */
    private static BigDecimal parsePricePart(String part) {
        part = part.trim();
        // strip leading B or S
        if (part.startsWith("B") || part.startsWith("S")) {
            part = part.substring(1).trim();
        }
        if (part.equalsIgnoreCase("FREE") || part.isEmpty()) return BigDecimal.ZERO;
        try {
            // handle K/M suffixes
            if (part.endsWith("K")) return new BigDecimal(part.substring(0, part.length()-1)).multiply(BigDecimal.valueOf(1_000));
            if (part.endsWith("M")) return new BigDecimal(part.substring(0, part.length()-1)).multiply(BigDecimal.valueOf(1_000_000));
            BigDecimal val = new BigDecimal(part);
            if (val.compareTo(BigDecimal.ZERO) < 0) return null;
            return val;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ── Item resolution ───────────────────────────────────────────────────────

    /**
     * Resolves an item name to an ItemStack.
     * First tries WorthManager.resolveItem (handles aliases), then the vanilla registry.
     */
    public static ItemStack resolveItem(String itemStr) {
        // Try WorthManager first (handles aliases like "diamond", "iron_sword")
        try {
            ItemStack fromWorth = WorthManager.resolveItem(itemStr);
            if (fromWorth != null && !fromWorth.isEmpty()) return fromWorth;
        } catch (Exception ignored) {}

        // Try vanilla registry directly
        String id = itemStr.toLowerCase().trim();
        if (!id.contains(":")) id = "minecraft:" + id;
        try {
            Optional<Item> item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id));
            if (item.isPresent()) return new ItemStack(item.get());
        } catch (Exception ignored) {}

        return ItemStack.EMPTY;
    }

    // ── Chest adjacency ───────────────────────────────────────────────────────

    /**
     * Scans all 6 cardinal neighbours + the block below for a {@link ChestBlockEntity}.
     * This mirrors uBlock.findConnectedContainer() from ChestShop-3.
     */
    public static BlockPos findAdjacentChest(BlockPos signPos, ServerLevel level) {
        // Check 6 cardinal directions + below (most common placement)
        BlockPos[] candidates = {
            signPos.below(),
            signPos.above(),
            signPos.north(),
            signPos.south(),
            signPos.east(),
            signPos.west()
        };
        for (BlockPos candidate : candidates) {
            BlockEntity be = level.getBlockEntity(candidate);
            if (be instanceof ChestBlockEntity) {
                return candidate;
            }
        }
        return null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String strip(String s) {
        if (s == null) return "";
        // Strip Minecraft colour codes (§x)
        return s.replaceAll("§[0-9a-fA-FkKlLmMnNoOrR]", "").trim();
    }

    /**
     * Normalise an item string from a sign line for registry lookup.
     * Handles spaces-as-underscores (display names), ellipsis truncation artefacts,
     * and the unicode ellipsis character (…).
     */
    public static String normalizeItemStr(String raw) {
        if (raw == null) return "";
        // Strip colour codes first
        String s = raw.replaceAll("§[0-9a-fA-FkKlLmMnNoOrR]", "").trim();
        // Remove trailing ellipsis (…) — artefact of display truncation
        if (s.endsWith("…") || s.endsWith("...")) {
            // Truncated display — unusable for lookup; caller should handle
            return s;
        }
        // Replace spaces with underscores (display names use spaces)
        s = s.replace(" ", "_");
        return s.toLowerCase();
    }

    /**
     * Build the 4 display lines to write back to the sign after validation.
     *
     * <p><b>Note:</b> These lines are purely cosmetic — the shop data lives in
     * {@code shops.json} and is never re-parsed from the sign. It is therefore
     * safe to shorten the item ID on the sign without losing data.
     *
     * <ul>
     *   <li>Line 0 = owner  (§2 dark-green for admin, §b aqua for player)</li>
     *   <li>Line 1 = quantity</li>
     *   <li>Line 2 = formatted price (§a buy, §6 sell)</li>
     *   <li>Line 3 = smart-shortened item id, or §e§l? if still pending</li>
     * </ul>
     */
    public static String[] formatSignLines(ShopData shop) {
        String ownerLine = shop.isAdminShop()
            ? "§2" + ShopData.ADMIN_SHOP_NAME  // dark green
            : "§b" + shop.ownerName;           // aqua

        StringBuilder priceBuilder = new StringBuilder();
        if (shop.buyPrice  != null) priceBuilder.append("§aB ").append(shop.buyPrice.toPlainString());
        if (shop.buyPrice  != null && shop.sellPrice != null) priceBuilder.append("§f:");
        if (shop.sellPrice != null) priceBuilder.append("§6S ").append(shop.sellPrice.toPlainString());

        // Line 3: pending = yellow bold ?, assigned = smart display name
        String itemLine;
        if (shop.itemPending || shop.itemId == null) {
            itemLine = "§e§l?";
        } else {
            itemLine = buildSignItemDisplayName(shop);
        }

        return new String[] {
            ownerLine,
            String.valueOf(shop.quantity),
            priceBuilder.toString(),
            itemLine
        };
    }

    /**
     * Build a display-friendly item name for a sign line (max ~16 visible chars).
     *
     * <p>Strategy:
     * <ol>
     *   <li>Strip {@code minecraft:} prefix — vanilla items show just the path</li>
     *   <li>Modded items keep their namespace — {@code thermal:copper_ingot} is shown as-is
     *       if it fits; otherwise abbreviated to {@code thermal:copper…}</li>
     *   <li>Underscores replaced with spaces for readability</li>
     * </ol>
     */
    public static String buildItemDisplayName(String fullId) {
        if (fullId == null || fullId.isBlank()) return "?";
        // Strip minecraft: namespace for vanilla items
        String display = fullId.startsWith("minecraft:") ? fullId.substring(10) : fullId;
        // Replace underscores with spaces for readability
        display = display.replace("_", " ");
        // Minecraft signs render ~16 chars per line — truncate with ellipsis if needed
        if (display.length() > 16) {
            // Try to keep namespace visible: "thermal:copper i…"
            display = display.substring(0, 15) + "…";
        }
        return display;
    }

    /**
     * Display name for the physical sign line (line 3) — prefers the item's REAL name
     * (respecting a custom name from {@link ShopData#itemNbt}, e.g. a modded item's proper
     * name instead of its raw registry id) via {@code getHoverName()}, truncated to the
     * ~16-char sign-line limit. Falls back to the id-derived name ({@link #buildItemDisplayName})
     * if the item can't be resolved.
     */
    public static String buildSignItemDisplayName(ShopData shop) {
        String name = null;
        try {
            ItemStack resolved = com.zerog.neoessentials.shop.ShopTransaction.resolveItem(shop);
            if (!resolved.isEmpty()) name = resolved.getHoverName().getString();
        } catch (Exception ignored) {}
        if (name == null || name.isBlank()) return buildItemDisplayName(shop.itemId);
        return name.length() > 16 ? name.substring(0, 15) + "…" : name;
    }

    /**
     * Full, untruncated display name for chat messages (shop info / transaction feedback) —
     * unlike {@link #buildItemDisplayName}, which exists solely for the ~16-char sign line.
     * Uses the item's real {@code getHoverName()} (respects custom names/lore from the shop's
     * stored {@link ShopData#itemNbt}) when the item resolves; falls back to the id-based
     * name if resolution fails (e.g. a since-removed modded item).
     */
    public static String buildFullItemDisplayName(ShopData shop) {
        try {
            ItemStack resolved = com.zerog.neoessentials.shop.ShopTransaction.resolveItem(shop);
            if (!resolved.isEmpty()) return resolved.getHoverName().getString();
        } catch (Exception ignored) {}
        return buildItemDisplayName(shop.itemId).replace("…", "");
    }

    /**
     * Captures a held item's {@code DataComponentMap} (custom name, enchantments, modded
     * NBT-backed data, etc.) as a JSON string for {@link ShopData#itemNbt}, so a shop trades
     * the actual item the owner held — not a data-less lookalike rebuilt from just its
     * registry id. Returns {@code null} for a plain item with no non-default components
     * (keeps old shops.json entries lean/unchanged) or if serialization fails.
     */
    public static String captureComponents(ItemStack held) {
        try {
            if (held.isEmpty()) return null;
            net.minecraft.core.component.DataComponentMap defaults = new ItemStack(held.getItem()).getComponents();
            if (held.getComponents().equals(defaults)) return null; // nothing custom to preserve
            return com.zerog.neoessentials.auctionhouse.AuctionComponentSerializer
                .serialize(held.getComponents()).toString();
        } catch (Exception e) {
            LOGGER.warn("[ChestShop] Failed to capture item components for '{}': {}",
                held.getItem(), e.getMessage());
            return null;
        }
    }
}



