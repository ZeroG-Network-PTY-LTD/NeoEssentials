package com.zerog.neoessentials.shops;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * Unified Shop Registry for NeoEssentials
 */
public final class ShopRegistry {
    private static final Map<BlockPos, Shop> bySignPos = new ConcurrentHashMap<>();

    public static Optional<Shop> getBySign(BlockPos pos) {
        return Optional.ofNullable(bySignPos.get(pos));
    }

    public static void register(Shop shop) {
        bySignPos.put(shop.signPos(), shop);
        persistAll();
    }

    public static void remove(BlockPos pos) {
        bySignPos.remove(pos);
        persistAll();
    }

    // Simple persistence: write all shops to shops.json in workspace
    private static void persistAll() {
        try {
            Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
            for (Shop shop : bySignPos.values()) {
                String owner = shop.owner().toString();
                grouped.computeIfAbsent(owner, k -> new ArrayList<>());
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("signPos", shop.signPos().toShortString());
                entry.put("chestPos", shop.chestPos().toShortString());
                entry.put("type", shop.type().name());
                entry.put("item", shop.itemSpec().getItem().toString());
                entry.put("amount", shop.amount());
                entry.put("price", shop.price());
                entry.put("flags", shop.flags());
                grouped.get(owner).add(entry);
            }
            java.nio.file.Path path = java.nio.file.Paths.get("shops.json");
            String json = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(grouped);
            java.nio.file.Files.write(path, json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Load all shops from shops.json (grouped by owner UUID)
    public static void loadAll() {
        bySignPos.clear();
        java.nio.file.Path path = java.nio.file.Paths.get("shops.json");
        if (!java.nio.file.Files.exists(path)) return;
        try {
            String json = new String(java.nio.file.Files.readAllBytes(path), java.nio.charset.StandardCharsets.UTF_8);
            var gson = new com.google.gson.Gson();
            // Try new format (grouped by owner)
            var type = new com.google.gson.reflect.TypeToken<Map<String, List<Map<String, Object>>>>(){}.getType();
            Map<String, List<Map<String, Object>>> grouped = gson.fromJson(json, type);
            if (grouped != null && !grouped.isEmpty()) {
                for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
                    UUID owner = UUID.fromString(entry.getKey());
                    for (Map<String, Object> shopData : entry.getValue()) {
                        BlockPos signPos = BlockPosArgumentParser.fromString((String)shopData.get("signPos"));
                        BlockPos chestPos = BlockPosArgumentParser.fromString((String)shopData.get("chestPos"));
                        ShopType typeVal = ShopType.valueOf((String)shopData.get("type"));
                        ItemStack item = ItemStackArgumentParser.fromString((String)shopData.get("item"));
                        int amount = ((Number)shopData.get("amount")).intValue();
                        double price = ((Number)shopData.get("price")).doubleValue();
                        Shop shop = new Shop(signPos, chestPos, owner, typeVal, item, amount, price);
                        bySignPos.put(signPos, shop);
                    }
                }
                return;
            }
            // Fallback: try old format (flat list)
            var oldType = new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> shopList = gson.fromJson(json, oldType);
            if (shopList != null) {
                for (Map<String, Object> shopData : shopList) {
                    BlockPos signPos = BlockPosArgumentParser.fromString((String)shopData.get("signPos"));
                    BlockPos chestPos = BlockPosArgumentParser.fromString((String)shopData.get("chestPos"));
                    UUID owner = UUID.fromString((String)shopData.get("owner"));
                    ShopType typeVal = ShopType.valueOf((String)shopData.get("type"));
                    ItemStack item = ItemStackArgumentParser.fromString((String)shopData.get("item"));
                    int amount = ((Number)shopData.get("amount")).intValue();
                    double price = ((Number)shopData.get("price")).doubleValue();
                    Shop shop = new Shop(signPos, chestPos, owner, typeVal, item, amount, price);
                    bySignPos.put(signPos, shop);
                }
                // Save in new format
                persistAll();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Get all shops for a given owner UUID
    public static List<Shop> getShopsByOwner(UUID owner) {
        List<Shop> result = new ArrayList<>();
        for (Shop shop : bySignPos.values()) {
            if (shop.owner().equals(owner)) {
                result.add(shop);
            }
        }
        return result;
    }

    // Returns the first shop with the given chest position, if any
    public static Optional<Shop> getByChest(BlockPos chestPos) {
        for (Shop shop : bySignPos.values()) {
            if (shop.chestPos() != null && shop.chestPos().equals(chestPos)) {
                return Optional.of(shop);
            }
        }
        return Optional.empty();
    }

    public static Collection<Shop> allShops() {
        return bySignPos.values();
    }
}

/**
 * Shop data model
 */
class Shop {
    private final BlockPos signPos;
    private final BlockPos chestPos;
    private final UUID owner;
    private final ShopType type;
    private final ItemStack itemSpec;
    private final int amount;
    private final double price;
    private final Set<String> flags = new HashSet<>();

    public Shop(BlockPos signPos, BlockPos chestPos, UUID owner, ShopType type, ItemStack itemSpec, int amount, double price) {
        this.signPos = signPos;
        this.chestPos = chestPos;
        this.owner = owner;
        this.type = type;
        this.itemSpec = itemSpec;
        this.amount = amount;
        this.price = price;
    }
    public BlockPos signPos() { return signPos; }
    public BlockPos chestPos() { return chestPos; }
    public UUID owner() { return owner; }
    public ShopType type() { return type; }
    public ItemStack itemSpec() { return itemSpec; }
    public int amount() { return amount; }
    public double price() { return price; }
    public Set<String> flags() { return flags; }
    public Object mutex() { return this; }
}

enum ShopType {
    BUY, SELL, ADMIN_BUY, ADMIN_SELL, TRADE;
    public boolean isBuy() { return this == BUY || this == ADMIN_BUY; }
    public boolean isSell() { return this == SELL || this == ADMIN_SELL; }
    public boolean isAdmin() { return this == ADMIN_BUY || this == ADMIN_SELL; }
}