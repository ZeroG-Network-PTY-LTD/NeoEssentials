package com.zerog.neoessentials.chat;

import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

/**
 * Manages private message toggling for players.
 */
public class MsgToggleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MsgToggleManager.class);
    private static final Set<String> toggledPlayers = ConcurrentHashMap.newKeySet();

    public static boolean toggleMsg(ServerPlayer player) {
        String name = player.getName().getString().toLowerCase();
        if (toggledPlayers.contains(name)) {
            toggledPlayers.remove(name);
            NeoLog.debug(LOGGER, LogCategory.CHAT, "{} un-toggled msg (now receiving private messages)", name);
            return true; // Now receiving messages
        } else {
            toggledPlayers.add(name);
            NeoLog.debug(LOGGER, LogCategory.CHAT, "{} toggled msg (now blocking private messages)", name);
            return false; // Now blocking messages
        }
    }

    public static boolean isMsgToggled(ServerPlayer player) {
        return toggledPlayers.contains(player.getName().getString().toLowerCase());
    }
}
