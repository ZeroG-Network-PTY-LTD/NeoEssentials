package com.zerog.neoessentials.teleportation;

import com.zerog.neoessentials.config.ConfigManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles teleport cancel on damage during warmup, if enabled in config.
 */
@EventBusSubscriber(modid = "neoessentials")
public class TeleportDamageCancelHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeleportDamageCancelHandler.class);

    // Track players with pending teleports (UUID)
    private static final ConcurrentHashMap<UUID, Runnable> pendingTeleports = new ConcurrentHashMap<>();

    /**
     * Register a pending teleport for a player (call from teleport warmup start)
     */
    public static void registerPendingTeleport(ServerPlayer player, Runnable cancelAction) {
        pendingTeleports.put(player.getUUID(), cancelAction);
    }

    /**
     * Unregister a pending teleport for a player (call from teleport complete/cancel)
     */
    public static void unregisterPendingTeleport(ServerPlayer player) {
        pendingTeleports.remove(player.getUUID());
    }

    /**
     * Whether {@code player} already has a warmup-teleport pending. This map holds only one
     * cancel-action per player — registering a second pending teleport (e.g. starting a new
     * /warp while a /home warmup is still counting down) silently overwrites the first one's
     * entry, so the first teleport's damage-cancel stops working and the second's `unregister`
     * call (firing when the FIRST teleport's timer elapses) removes an entry that now belongs
     * to the second. Callers should check this before starting a new warmup and reject/queue
     * instead of starting a second one.
     */
    public static boolean isPending(ServerPlayer player) {
        return pendingTeleports.containsKey(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!ConfigManager.getInstance().isCancelOnDamageEnabled()) return;
        Runnable cancelAction = pendingTeleports.get(player.getUUID());
        if (cancelAction != null) {
            LOGGER.debug("Cancelling teleport for {} due to damage taken.", player.getName().getString());
            cancelAction.run();
            pendingTeleports.remove(player.getUUID());
        }
    }
}
