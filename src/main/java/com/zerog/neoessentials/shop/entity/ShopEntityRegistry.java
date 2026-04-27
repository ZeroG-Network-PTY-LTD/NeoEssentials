package com.zerog.neoessentials.shop.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers the {@link ShopNpcEntity} entity type on the NeoForge mod event bus.
 *
 * <p>Call {@link #register(IEventBus)} from the {@code NeoEssentials} mod constructor
 * to wire up the deferred registrations.
 */
public class ShopEntityRegistry {

    /** Deferred register — must be driven on the MOD event bus. */
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, "neoessentials");

    /** The NPC shop entity type holder. */
    public static final DeferredHolder<EntityType<?>, EntityType<ShopNpcEntity>> SHOP_NPC =
            ENTITY_TYPES.register("shop_npc", () -> EntityType.Builder
                    .of(ShopNpcEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f)
                    .noSummon()
                    .build("neoessentials:shop_npc"));

    /**
     * Wire up the deferred registers on the mod event bus.
     * Call this from the {@code NeoEssentials} constructor.
     */
    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        // Set static reference on the entity class for easy access elsewhere
        modEventBus.addListener(ShopEntityRegistry::onAttributeCreation);
    }

    /** Provide attributes for the ShopNpcEntity. */
    private static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(SHOP_NPC.get(), ShopNpcEntity.createAttributes().build());
        // Set the static TYPE reference so ShopNpcEntity.TYPE is usable without DeferredHolder
        ShopNpcEntity.TYPE = SHOP_NPC.get();
    }
}

