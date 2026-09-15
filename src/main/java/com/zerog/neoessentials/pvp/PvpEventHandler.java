package com.zerog.neoessentials.pvp;

import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Blocks player-vs-player damage whenever {@link PvpManager#canFight} says no — global switch
 * off, either player opted out, either in their newbie-protection window, or either standing in
 * a configured safe zone.
 */
@EventBusSubscriber(modid = "neoessentials")
public class PvpEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PvpEventHandler.class);

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!ConfigManager.isPvpModuleEnabled()) return;
        if (!(event.getEntity() instanceof ServerPlayer target)) return;

        DamageSource source = event.getContainer().getSource();
        if (!(source.getEntity() instanceof ServerPlayer attacker)) return;
        if (attacker.equals(target)) return;

        if (PvpManager.getInstance().canFight(attacker, target)) return;

        event.setNewDamage(0f);
        NeoLog.debug(LOGGER, LogCategory.GENERAL, "[PvP] Blocked hit: {} -> {}",
            attacker.getName().getString(), target.getName().getString());

        if (ConfigManager.isPvpNotifyOnBlockedHitEnabled() && PvpManager.getInstance().shouldNotify(attacker.getUUID())) {
            attacker.sendSystemMessage(MessageUtil.error("commands.neoessentials.pvp.hit_blocked_attacker",
                target.getName().getString()));
            target.sendSystemMessage(MessageUtil.info("commands.neoessentials.pvp.hit_blocked_target",
                attacker.getName().getString()));
        }
    }

    /** Starts a new player's newbie-protection window the first time they're ever seen. */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PvpManager.getInstance().recordFirstSeenIfAbsent(player.getUUID());
        }
    }
}
