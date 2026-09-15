package com.zerog.neoessentials.teleportation;

import com.zerog.neoessentials.config.ConfigManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Combat event handler: marks players in combat ONLY when actually fighting.
 * This tracks PvP combat and combat with mobs, but NOT environmental damage.
 * Environmental damage includes: fall damage, drowning, fire, lava, hunger, etc.
 *
 * <p>Whether PvE combat counts at all is config-driven — see {@link #countsAsCombat(Entity)}
 * and {@link ConfigManager#isCombatLockPvpOnly()}: some automation mods (mob farms/grinders,
 * e.g. Mob Grinding Utils' Mob Masher) attribute their kills to the owning player for
 * loot/XP/looting-enchant purposes, which is indistinguishable here from that player manually
 * attacking a mob — permanently locking their teleport with no real combat happening. Setting
 * {@code combatLockPvpOnly: true} restricts this tracker to real player-vs-player hits only.
 */
@EventBusSubscriber(modid = "neoessentials")
public class CombatEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CombatEventHandler.class);

    /** Returns true if a hit involving {@code other} should count towards combat lock, per current config. */
    private static boolean countsAsCombat(Entity other) {
        return !ConfigManager.getInstance().isCombatLockPvpOnly() || other instanceof ServerPlayer;
    }

    /**
     * Mark player as in combat when they attack an entity
     */
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && countsAsCombat(event.getTarget())) {
            CombatTracker.markInCombat(player);
            NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "Player {} entered combat by attacking {}",
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
        if (source.getEntity() != null && source.getEntity() instanceof LivingEntity && countsAsCombat(source.getEntity())) {
            // Only mark if not already in combat to avoid spam
            if (!CombatTracker.isInCombat(player)) {
                CombatTracker.markInCombat(player);
                NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "Player {} entered combat by receiving damage from {}",
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
            NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "Cleared combat status for logging out player {}", player.getName().getString());
        }
    }
}
