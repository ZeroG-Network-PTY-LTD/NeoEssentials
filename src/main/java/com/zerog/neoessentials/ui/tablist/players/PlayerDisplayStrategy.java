package com.zerog.neoessentials.ui.tablist.players;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Interface for strategies to display players in the tablist
 */
public interface PlayerDisplayStrategy {
    /**
     * Gets the display text for a player
     * 
     * @param player The player to get display text for
     * @return The component representing how this player should be displayed
     */
    Component getPlayerDisplay(ServerPlayer player);
}
