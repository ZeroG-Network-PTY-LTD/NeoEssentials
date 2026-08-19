package com.zerog.neoessentials.shop.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Utility helpers for NeoEssentials NPC shop entities.
 *
 * <p>Shop NPCs are plain vanilla {@link ArmorStand} entities — no custom
 * EntityType is registered. This avoids the registry-sync disconnect that
 * clients without NeoEssentials installed would otherwise receive:
 * <em>"The server sent registries with unknown keys:
 * ResourceKey[minecraft:entity_type / neoessentials:shop_npc]"</em>.
 *
 * <p>The shop identity is stored in {@link net.minecraft.nbt.CompoundTag
 * persistent data} under {@link #NBT_SHOP_ID}. Interactions are intercepted
 * by {@link ShopEntityRegistry}.
 */
public final class ShopNpcEntity {

    /** Persistent-data key used to tag a shop ArmorStand (UUID stored as two longs). */
    public static final String NBT_SHOP_ID = "NeoEssentials_ShopId";

    private ShopNpcEntity() {}

    // ── Factory ───────────────────────────────────────────────────────────────

    /**
     * Create and configure a new ArmorStand as a shop NPC.
     *
     * <p>The returned entity has NOT been added to the level yet — call
     * {@code level.addFreshEntity(stand)} after setting its position.
     *
     * @param level    the server level
     * @param shopId   the shopId to embed in persistent data
     * @param shopName display name shown above the NPC head
     * @return configured ArmorStand, ready to spawn
     */
    public static ArmorStand create(Level level, UUID shopId, String shopName) {
        ArmorStand stand = new ArmorStand(EntityType.ARMOR_STAND, level);
        stand.setInvulnerable(true);
        stand.setSilent(true);
        stand.setNoGravity(true);
        // ArmorStand extends LivingEntity (not Mob) — no setNoAi(); stands are stationary by default
        stand.setCustomName(com.zerog.neoessentials.util.MessageUtil.component(
            "commands.neoessentials.npcshop.entity_display_name", shopName));
        stand.setCustomNameVisible(true);
        stand.getPersistentData().putIntArray(NBT_SHOP_ID, net.minecraft.core.UUIDUtil.uuidToIntArray(shopId));
        return stand;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Returns the shopId embedded in the ArmorStand's persistent data, or {@code null}. */
    public static UUID getShopId(ArmorStand stand) {
        var data = stand.getPersistentData();
        return data.getIntArray(NBT_SHOP_ID).map(net.minecraft.core.UUIDUtil::uuidFromIntArray).orElse(null);
    }

    /** Returns {@code true} if this ArmorStand is a NeoEssentials shop NPC. */
    public static boolean isShopNpc(ArmorStand stand) {
        return stand.getPersistentData().contains(NBT_SHOP_ID);
    }
}

