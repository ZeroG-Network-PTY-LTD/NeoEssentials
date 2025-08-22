package com.zerog.neoessentials.events;
import java.util.UUID;

import com.zerog.neoessentials.managers.SocialSpyManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.network.chat.Component;

@net.neoforged.fml.common.EventBusSubscriber(modid = "neoessentials")
public class SocialSpyEventHandler {
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer sender = event.getPlayer();
        String message = event.getRawText();
        if (sender.getServer() == null) return;
        for (UUID spyUUID : SocialSpyManager.getEnabledPlayers()) {
            if (sender.getUUID().equals(spyUUID)) continue;
            if (sender.getServer() == null) continue;
            var server = sender.getServer();
            if (server == null) continue;
            ServerPlayer spy = server.getPlayerList().getPlayer(spyUUID);
            if (spy != null) {
                spy.sendSystemMessage(Component.literal("[SocialSpy] " + sender.getName().getString() + ": " + message));
            }
        }
    }
}
