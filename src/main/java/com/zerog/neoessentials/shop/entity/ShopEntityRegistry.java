package com.zerog.neoessentials.shop.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import java.util.UUID;

/**
 * Intercepts player interactions with ArmorStand-based NPC shops.
 *
 * <p>NeoEssentials avoids registering a custom EntityType so that vanilla clients
 * (and clients without NeoEssentials installed) are never disconnected by an
 * unknown registry key. Instead, shop NPCs are ordinary {@link ArmorStand}
 * entities whose persistent data carries a {@code NeoEssentials_ShopId} UUID tag
 * that the server reads on interact.
 *
 * <p>Previously this class registered a {@code DeferredRegister<EntityType<?>>}
 * which caused: <em>"The server sent registries with unknown keys:
 * ResourceKey[minecraft:entity_type / neoessentials:shop_npc]"</em>.
 */
@EventBusSubscriber(modid = "neoessentials")
public class ShopEntityRegistry {

    private ShopEntityRegistry() {}

    /**
     * Intercept right-click on any ArmorStand that carries our shop NBT tag.
     * Cancel the event to suppress the vanilla equip GUI and open the shop instead.
     */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        // Only handle main-hand interact and server-side
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer sp)) return;
        if (!(event.getTarget() instanceof ArmorStand stand)) return;

        CompoundTag persistentData = stand.getPersistentData();
        if (!persistentData.contains(ShopNpcEntity.NBT_SHOP_ID)) return;

        // It's a NeoEssentials NPC shop — cancel vanilla interaction
        event.setCanceled(true);

        UUID shopId = persistentData.getIntArray(ShopNpcEntity.NBT_SHOP_ID)
            .map(net.minecraft.core.UUIDUtil::uuidFromIntArray).orElse(null);
        ShopEntityData shopData = ShopEntityManager.getInstance().getByShopId(shopId);

        if (shopData == null) {
            sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cThis NPC is not linked to a shop. Ask an admin."));
            return;
        }
        if (shopData.listings.isEmpty()) {
            sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cThis NPC shop has no items configured yet."));
            return;
        }

        sp.openMenu(new NpcShopMenu.NpcShopMenuProvider(shopData),
                buf -> buf.writeUtf(shopId.toString()));
    }
}

