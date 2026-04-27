package com.zerog.neoessentials.shop.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Shop NPC entity.
 *
 * <p>A peaceful, stationary mob that opens a virtual shop GUI when right-clicked.
 * The {@code shopId} NBT tag links this entity to its {@link ShopEntityData}.
 *
 * <p>Rendered as a default entity (invisible white box on clients that have no
 * client-side renderer for {@code neoessentials:shop_npc}).  Admins manage these
 * entities through the {@code /npcshop} command.
 */
public class ShopNpcEntity extends PathfinderMob {

    private static final String NBT_SHOP_ID = "NeoEssentials_ShopId";

    @Nullable
    private UUID shopId;

    public ShopNpcEntity(EntityType<? extends ShopNpcEntity> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        this.setSilent(true);
        this.setNoAi(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    // ── Interaction ───────────────────────────────────────────────────────────

    @Override
    @Nonnull
    @SuppressWarnings("resource") // Level is not AutoCloseable; IntelliJ false positive
    protected InteractionResult mobInteract(@Nonnull Player player, @Nonnull InteractionHand hand) {
        if (!level().isClientSide() && player instanceof ServerPlayer sp && hand == InteractionHand.MAIN_HAND) {
            if (shopId != null) {
                ShopEntityData shopData = ShopEntityManager.getInstance().getByShopId(shopId);
                if (shopData != null && !shopData.listings.isEmpty()) {
                    sp.openMenu(new NpcShopMenu.NpcShopMenuProvider(shopData), buf -> buf.writeUtf(shopId.toString()));
                } else {
                    sp.sendSystemMessage(Component.literal("§cThis NPC shop has no items configured yet."));
                }
            } else {
                sp.sendSystemMessage(Component.literal("§cThis NPC is not linked to a shop. Ask an admin."));
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    // ── No combat ─────────────────────────────────────────────────────────────

    @Override
    public boolean canBeLeashed() { return false; }

    @Override
    public boolean isAggressive() { return false; }


    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(@Nonnull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if (shopId != null) nbt.putUUID(NBT_SHOP_ID, shopId);
    }

    @Override
    public void readAdditionalSaveData(@Nonnull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.hasUUID(NBT_SHOP_ID)) {
            this.shopId = nbt.getUUID(NBT_SHOP_ID);
            // Sync entity UUID back to ShopEntityManager
            ShopEntityManager.getInstance().updateEntityUUID(shopId, this.getUUID());
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    @Nullable
    public UUID getShopId() { return shopId; }

    public void setShopId(UUID shopId) {
        this.shopId = shopId;
        if (shopId != null) {
            ShopEntityManager.getInstance().updateEntityUUID(shopId, this.getUUID());
        }
    }

    /** Entity type constant — set by {@link ShopEntityRegistry} before use. */
    public static EntityType<ShopNpcEntity> TYPE;
}


