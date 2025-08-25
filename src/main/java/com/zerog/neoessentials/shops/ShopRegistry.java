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
            List<Map<String, Object>> shopList = new ArrayList<>();
            for (Shop shop : bySignPos.values()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("signPos", shop.signPos().toShortString());
                entry.put("chestPos", shop.chestPos().toShortString());
                entry.put("owner", shop.owner().toString());
                entry.put("type", shop.type().name());
                entry.put("item", shop.itemSpec().getItem().toString());
                entry.put("amount", shop.amount());
                entry.put("price", shop.price());
                entry.put("flags", shop.flags());
                shopList.add(entry);
            }
            java.nio.file.Path path = java.nio.file.Paths.get("shops.json");
            String json = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(shopList);
            java.nio.file.Files.write(path, json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
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
