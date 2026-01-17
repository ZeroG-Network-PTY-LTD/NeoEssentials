package com.zerog.neoessentials.teleportation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Combat event handler: marks players in combat ONLY when actually fighting.
 * This tracks PvP combat and combat with mobs, but NOT environmental damage.
 * Environmental damage includes: fall damage, drowning, fire, lava, hunger, etc.
 */
@EventBusSubscriber(modid = "neoessentials")
public class CombatEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CombatEventHandler.class);

    /**
     * Mark player as in combat when they attack an entity
     */
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CombatTracker.markInCombat(player);
            LOGGER.debug("Player {} entered combat by attacking {}",
                player.getName().getString(),
                event.getTarget().getName().getString());
        }
    }

    /**
     * Mark player as in combat when they take damage from another entity.
     * Environmental damage (fall, drowning, fire, etc.) does NOT trigger combat.
     */
    @SubscribeEvent
    public static void onPlayerDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DamageSource source = event.getContainer().getSource();

        // Only mark in combat if damage is from an entity (not environmental)
        // Environmental damage (fall, fire, drowning, etc.) won't have a source entity
        if (source.getEntity() != null && source.getEntity() instanceof LivingEntity) {
            // Only mark if not already in combat to avoid spam
            if (!CombatTracker.isInCombat(player)) {
                CombatTracker.markInCombat(player);
                LOGGER.debug("Player {} entered combat by receiving damage from {}",
                    player.getName().getString(),
                    source.getEntity().getName().getString());
            }
        }
    }

    /**
     * Clear combat status when player logs out
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CombatTracker.clearCombat(player);
            LOGGER.debug("Cleared combat status for logging out player {}", player.getName().getString());
        }
    }
}
