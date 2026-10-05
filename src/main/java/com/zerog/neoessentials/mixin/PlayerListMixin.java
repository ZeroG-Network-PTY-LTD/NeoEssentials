package com.zerog.neoessentials.mixin;

import com.zerog.neoessentials.chat.handlers.PlayerJoinQuitHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops vanilla's own "X joined the game" / "X left the game" broadcasts when a custom
 * join/quit message is configured, so players see only the custom one instead of both.
 * NeoForge has no event for these broadcasts, and both of them (PlayerList.placeNewPlayer for
 * join, ServerGamePacketListenerImpl.removePlayerFromWorld for leave) go through this one
 * method on 1.21.1, 26.1 and 26.2 alike, so filtering here by translation key covers both
 * without depending on either caller's internals.
 */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Inject(method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V", at = @At("HEAD"), cancellable = true)
    private void neoessentials$suppressVanillaJoinQuit(Component message, boolean overlay, CallbackInfo ci) {
        if (PlayerJoinQuitHandler.shouldSuppressVanillaMessage(message)) {
            ci.cancel();
        }
    }
}
