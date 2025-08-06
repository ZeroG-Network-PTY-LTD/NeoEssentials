package com.zerog.neoessentials.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sound Effects Utility for GUI Interactions
 * Handles playing sounds for GUI actions like purchases, errors, etc.
 */
public class SoundUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoundUtil.class);
    
    /**
     * Play a sound effect for a player
     * 
     * @param player The player to play the sound for
     * @param soundId The sound ID (e.g., "minecraft:entity.experience_orb.pickup")
     * @param volume Volume level (0.0 to 1.0)
     * @param pitch Pitch level (0.5 to 2.0, 1.0 is normal)
     */
    public static void playSound(ServerPlayer player, String soundId, float volume, float pitch) {
        try {
            if (soundId == null || soundId.isEmpty()) {
                return;
            }
            
            ResourceLocation soundLocation = ResourceLocation.parse(soundId);
            SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.get(soundLocation);
            
            if (soundEvent != null) {
                player.playNotifySound(soundEvent, SoundSource.MASTER, volume, pitch);
            } else {
                LOGGER.warn("Unknown sound effect: {}", soundId);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to play sound effect: " + soundId, e);
        }
    }
    
    /**
     * Play a sound effect with default volume and pitch
     */
    public static void playSound(ServerPlayer player, String soundId) {
        playSound(player, soundId, 1.0f, 1.0f);
    }
    
    /**
     * Play purchase success sound
     */
    public static void playPurchaseSuccess(ServerPlayer player) {
        playSound(player, "minecraft:entity.experience_orb.pickup", 1.0f, 1.0f);
    }
    
    /**
     * Play purchase failure sound
     */
    public static void playPurchaseFailure(ServerPlayer player) {
        playSound(player, "minecraft:entity.villager.no", 1.0f, 1.0f);
    }
    
    /**
     * Play GUI open sound
     */
    public static void playGuiOpen(ServerPlayer player) {
        playSound(player, "minecraft:ui.button.click", 0.7f, 1.0f);
    }
    
    /**
     * Play page turn sound (for navigation)
     */
    public static void playPageTurn(ServerPlayer player) {
        playSound(player, "minecraft:item.book.page_turn", 0.8f, 1.0f);
    }
    
    /**
     * Play sell success sound
     */
    public static void playSellSuccess(ServerPlayer player) {
        playSound(player, "minecraft:block.note_block.chime", 1.0f, 1.2f);
    }
    
    /**
     * Play error sound
     */
    public static void playError(ServerPlayer player) {
        playSound(player, "minecraft:entity.enderman.teleport", 0.5f, 0.8f);
    }
    
    /**
     * Play positive action sound
     */
    public static void playPositive(ServerPlayer player) {
        playSound(player, "minecraft:entity.player.levelup", 0.3f, 1.5f);
    }
    
    /**
     * Play negative action sound
     */
    public static void playNegative(ServerPlayer player) {
        playSound(player, "minecraft:entity.item.break", 0.7f, 0.8f);
    }
}
