package com.zerog.neoessentials.moderation.handlers;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.moderation.FreezeManager;
import com.zerog.neoessentials.moderation.JailManager;
import com.zerog.neoessentials.moderation.VanishManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Event handler for moderation system integration.
 * Handles freeze interaction blocking and jail restrictions.
 *
 * Jail enforcement ported from EssentialsX Jails.java (JailListener):
 *  - Block break / place cancellation
 *  - Interact cancellation
 *  - Attack cancellation
 *  - Gamemode change prevention
 *  - Respawn location redirect → jail
 *  - Teleport interception → redirect back to jail
 *  - Movement via PlayerMoveEvent (replaces tick-based scan)
 *  - Player login: checkJailTimeout + teleport to jail
 */
@EventBusSubscriber(modid = "neoessentials")
public class ModerationEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModerationEventHandler.class);

    // ── Player Login ─────────────────────────────────────────────────────────
    /**
     * On login: check if timed jail expired while offline, then teleport to jail.
     * Essentials: onJailPlayerJoin / user.checkJailTimeout(currentTime)
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Vanish on-join: restore tab-list hiding and show vanish reminder if applicable
        try {
            if (ConfigManager.getInstance().isVanishSystemEnabled()) {
                VanishManager.getInstance().onPlayerJoin(player);
            }
        } catch (Exception e) {
            LOGGER.error("Error handling vanish on player login", e);
        }

        // Freeze on-join: remind the player they are frozen and lock their position
        try {
            if (ConfigManager.isFreezeSystemEnabled()) {
                FreezeManager.getInstance().onPlayerJoin(player);
            }
        } catch (Exception e) {
            LOGGER.error("Error handling freeze on player login", e);
        }

        // Jail on-join: check expiry and teleport to jail if still jailed
        try {
            if (!JailManager.isJailSystemEnabled()) return;
            JailManager jailManager = JailManager.getInstance();
            UUID playerId = player.getUUID();

            // Check timeout first — may auto-release the player (Essentials: checkJailTimeout)
            if (jailManager.checkJailTimeout(playerId)) {
                player.sendSystemMessage(MessageUtil.success("commands.neoessentials.jail.released_expired"));
                return;
            }

            // Still jailed — delegate to existing onPlayerJoin logic
            jailManager.onPlayerJoin(player);
        } catch (Exception e) {
            LOGGER.error("Error handling jail on player login", e);
        }
    }

    // ── Movement ─────────────────────────────────────────────────────────────
    /**
     * Intercept player movement. If the player leaves the jail radius, teleport them back.
     * Uses PlayerMoveEvent instead of a per-tick server scan — far less expensive.
     * Essentials: onJailPlayerTeleport (handles movement + teleport events both).
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerMove(PlayerEvent.PlayerChangedDimensionEvent event) {
        // Dimension change is an unconditional teleport — redirect back if jailed
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        redirectJailedPlayer(player, "dimension change");
    }


    // ── Respawn ───────────────────────────────────────────────────────────────
    /**
     * Redirect jailed players' respawn location back to jail.
     * Essentials: onJailPlayerRespawn (EventPriority.HIGHEST) sets event.setRespawnLocation.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        try {
            if (!JailManager.isJailSystemEnabled()) return;
            JailManager jailManager = JailManager.getInstance();
            UUID playerId = player.getUUID();
            if (!jailManager.isPlayerJailed(playerId)) return;

            JailManager.JailEntry jailEntry = jailManager.getJailEntry(playerId);
            JailManager.JailLocation jailLoc = jailManager.getJailLocation(jailEntry.jailName);
            if (jailLoc == null) return;

            // NeoForge respawn: teleport after spawn tick
            net.minecraft.server.MinecraftServer server = player.level().getServer();
            if (server == null) return;
            ServerLevel jailLevel = server.getLevel(
                net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    net.minecraft.resources.Identifier.tryParse(
                        jailLoc.dimension != null ? jailLoc.dimension : "minecraft:overworld")));
            if (jailLevel == null) return;

            // Schedule 1-tick delayed teleport so respawn completes first
            com.zerog.neoessentials.scheduler.DelayedTaskScheduler.schedule(1, () -> {
                if (player.isAlive()) {
                    player.teleportTo(
                jailLevel,
                jailLoc.position.getX() + 0.5,
                jailLoc.position.getY() + 1,
                jailLoc.position.getZ() + 0.5,
                java.util.Set.of(),
                player.getYRot(),
                player.getXRot(),
                true
            );
                    player.sendSystemMessage(MessageUtil.warning("commands.neoessentials.jail.message"));
                }
            });
        } catch (Exception e) {
            LOGGER.error("Error redirecting jailed player respawn", e);
        }
    }

    // ── Interaction ───────────────────────────────────────────────────────────
    /**
     * Cancel all right-click interactions for jailed players.
     * Essentials: onJailPlayerInteract cancels unless essentials.jail.allow-interact.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerRightClick(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        try {
            UUID playerId = player.getUUID();

            // Freeze check
            if (ConfigManager.isFreezeSystemEnabled()) {
                if (FreezeManager.getInstance().isPlayerFrozen(playerId)) {
                    event.setCanceled(true);
                    return;
                }
            }

            // Jail check
            if (JailManager.isJailSystemEnabled()
                    && JailManager.getInstance().isPlayerJailed(playerId)
                    && !PermissionAPI.hasPermission(playerId, "neoessentials.jail.allow-interact")) {
                event.setCanceled(true);
                return;
            }

            // Vanish interact check
            if (ConfigManager.getInstance().isVanishSystemEnabled()
                    && ConfigManager.isVanishPreventInteractionEnabled()) {
                VanishManager vanishManager = VanishManager.getInstance();
                if (vanishManager.isPlayerVanished(playerId)) {
                    String seePerm = ConfigManager.getInstance().getSeeVanishedPermission();
                    if (!PermissionAPI.hasPermission(playerId, seePerm)) {
                        event.setCanceled(true);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error handling player interaction", e);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        try {
            UUID playerId = player.getUUID();
            if (ConfigManager.isFreezeSystemEnabled()
                    && FreezeManager.getInstance().isPlayerFrozen(playerId)) {
                event.setCanceled(true);
                return;
            }
            if (JailManager.isJailSystemEnabled()
                    && JailManager.getInstance().isPlayerJailed(playerId)
                    && !PermissionAPI.hasPermission(playerId, "neoessentials.jail.allow-interact")) {
                event.setCanceled(true);
            }
        } catch (Exception e) {
            LOGGER.error("Error handling right-click block for jail/freeze", e);
        }
    }

    // ── Attack ────────────────────────────────────────────────────────────────
    /**
     * Cancel attacks by jailed players against other players.
     * Essentials: onJailEntityDamageByEntity cancels EntityDamageByEntityEvent for jailed attackers.
     * NeoForge 1.21.1: LivingAttackEvent does not exist — use LivingDamageEvent.Pre instead.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingAttack(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        try {
            if (JailManager.isJailSystemEnabled()
                    && JailManager.getInstance().isPlayerJailed(attacker.getUUID())
                    && !PermissionAPI.hasPermission(attacker.getUUID(), "neoessentials.jail.allow-attack")) {
                event.setNewDamage(0f);
                return;
            }
            // FreezeManager.canPlayerAttack() existed but was never actually called anywhere —
            // a frozen player could still attack and damage other players.
            if (!FreezeManager.getInstance().canPlayerAttack(attacker)) {
                event.setNewDamage(0f);
            }
        } catch (Exception e) {
            LOGGER.error("Error handling attack for jailed/frozen player", e);
        }
    }

    // ── Item Pickup / Drop ────────────────────────────────────────────────────
    /**
     * FreezeManager.canPlayerPickupItems()/canPlayerDropItems() existed but were never
     * actually called anywhere — a frozen player could still pick up and drop items freely.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemPickup(net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        try {
            if (!FreezeManager.getInstance().canPlayerPickupItems(player)) {
                event.setCanPickup(net.minecraft.util.TriState.FALSE);
            }
        } catch (Exception e) {
            LOGGER.error("Error handling item pickup for frozen player", e);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemToss(net.neoforged.neoforge.event.entity.item.ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        try {
            if (!FreezeManager.getInstance().canPlayerDropItems(player)) {
                event.setCanceled(true);
            }
        } catch (Exception e) {
            LOGGER.error("Error handling item drop for frozen player", e);
        }
    }

    // ── Block Break / Place ───────────────────────────────────────────────────
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockBreak(BreakBlockEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        try {
            UUID playerId = player.getUUID();

            if (ConfigManager.isFreezeSystemEnabled()
                    && FreezeManager.getInstance().isPlayerFrozen(playerId)) {
                event.setCanceled(true);
                return;
            }

            if (JailManager.isJailSystemEnabled()
                    && JailManager.getInstance().isPlayerJailed(playerId)
                    && !PermissionAPI.hasPermission(playerId, "neoessentials.jail.allow-break")) {
                event.setCanceled(true);
                return;
            }

            // Region-wide protection: a jail cell's blocks can't be broken by ANYONE, jailed or
            // not — the check above only stops the JAILED occupant, so someone standing outside
            // the cell (or a jailed player targeting the cell from a neighboring one) could
            // still dig into it. neoessentials.jail.bypass exempts staff doing maintenance.
            if (JailManager.isJailSystemEnabled()
                    && !PermissionAPI.hasPermission(playerId, "neoessentials.jail.bypass")
                    && JailManager.getInstance().isInsideAnyJail(event.getPos(),
                        player.level().dimension().location().toString())) {
                event.setCanceled(true);
                return;
            }

            if (ConfigManager.getInstance().isVanishSystemEnabled()
                    && ConfigManager.isVanishPreventInteractionEnabled()) {
                VanishManager vanishManager = VanishManager.getInstance();
                if (vanishManager.isPlayerVanished(playerId)) {
                    String seePerm = ConfigManager.getInstance().getSeeVanishedPermission();
                    if (!PermissionAPI.hasPermission(playerId, seePerm)) {
                        event.setCanceled(true);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error handling block break for moderation", e);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        try {
            UUID playerId = player.getUUID();

            if (ConfigManager.isFreezeSystemEnabled()
                    && FreezeManager.getInstance().isPlayerFrozen(playerId)) {
                event.setCanceled(true);
                return;
            }

            if (JailManager.isJailSystemEnabled()
                    && JailManager.getInstance().isPlayerJailed(playerId)
                    && !PermissionAPI.hasPermission(playerId, "neoessentials.jail.allow-place")) {
                event.setCanceled(true);
                return;
            }

            // Region-wide protection — see matching comment in onBlockBreak above.
            if (JailManager.isJailSystemEnabled()
                    && !PermissionAPI.hasPermission(playerId, "neoessentials.jail.bypass")
                    && JailManager.getInstance().isInsideAnyJail(event.getPos(),
                        player.level().dimension().location().toString())) {
                event.setCanceled(true);
                return;
            }

            if (ConfigManager.getInstance().isVanishSystemEnabled()
                    && ConfigManager.isVanishPreventInteractionEnabled()) {
                VanishManager vanishManager = VanishManager.getInstance();
                if (vanishManager.isPlayerVanished(playerId)) {
                    String seePerm = ConfigManager.getInstance().getSeeVanishedPermission();
                    if (!PermissionAPI.hasPermission(playerId, seePerm)) {
                        event.setCanceled(true);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error handling block place for moderation", e);
        }
    }

    // ── Position enforcement (periodic, lightweight) ──────────────────────────
    /**
     * Periodic tick check — only runs every 20 ticks (1 second), only for jailed players.
     * Far cheaper than per-tick all-player scan. Replaces old tick-based implementation.
     * Also handles timed-jail expiry on tick.
     */
    private static int tickCounter = 0;

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        if (++tickCounter < 20) return;
        tickCounter = 0;

        // ── Freeze enforcement ────────────────────────────────────────────────
        // Must run independently of jail so it works even when jail is disabled.
        if (ConfigManager.isFreezeSystemEnabled()) {
            FreezeManager freezeManager = FreezeManager.getInstance();
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                try {
                    if (freezeManager.isPlayerFrozen(player.getUUID())) {
                        freezeManager.enforceFreezePosition(player);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error enforcing freeze for player {}", player.getName().getString(), e);
                }
            }
        }

        // ── Jail enforcement ──────────────────────────────────────────────────
        if (!JailManager.isJailSystemEnabled()) return;
        JailManager jailManager = JailManager.getInstance();

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            try {
                UUID playerId = player.getUUID();
                if (!jailManager.isPlayerJailed(playerId)) continue;

                // Expired timed jail → auto-release
                if (jailManager.checkJailTimeout(playerId)) {
                    player.sendSystemMessage(MessageUtil.success("commands.neoessentials.jail.released_expired"));
                    continue;
                }

                // Movement enforcement: push back if outside radius
                BlockPos currentPos = player.blockPosition();
                if (!jailManager.canPlayerMove(player, currentPos)) {
                    redirectJailedPlayer(player, "movement");
                }
            } catch (Exception e) {
                LOGGER.error("Error enforcing jail for player {}", player.getName().getString(), e);
            }
        }
    }

    // ── Helper ─────────────────────────────────────────────────────────────────
    private static void redirectJailedPlayer(ServerPlayer player, String reason) {
        try {
            if (!JailManager.isJailSystemEnabled()) return;
            JailManager jailManager = JailManager.getInstance();
            UUID playerId = player.getUUID();
            if (!jailManager.isPlayerJailed(playerId)) return;

            JailManager.JailEntry jailEntry = jailManager.getJailEntry(playerId);
            if (jailEntry == null) return;
            JailManager.JailLocation jailLoc = jailManager.getJailLocation(jailEntry.jailName);
            if (jailLoc == null) return;

            net.minecraft.server.MinecraftServer server = player.level().getServer();
            if (server == null) return;

            net.minecraft.resources.Identifier dimId =
                net.minecraft.resources.Identifier.tryParse(
                    jailLoc.dimension != null ? jailLoc.dimension : "minecraft:overworld");
            ServerLevel level = server.getLevel(
                net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION, dimId));
            if (level == null) level = server.overworld();

            player.teleportTo(
                level,
                jailLoc.position.getX() + 0.5,
                jailLoc.position.getY() + 1,
                jailLoc.position.getZ() + 0.5,
                java.util.Set.of(),
                player.getYRot(),
                player.getXRot(),
                true
            );

            player.sendSystemMessage(MessageUtil.warning("commands.neoessentials.jail.escape_prevented"));
            LOGGER.debug("Jailed player {} redirected back to jail ({}).", player.getName().getString(), reason);
        } catch (Exception e) {
            LOGGER.error("Error redirecting jailed player", e);
        }
    }
}
