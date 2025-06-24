package com.zerog.neoessentials.ui.tab.placeholders;

import com.zerog.neoessentials.ui.tab.TabPlayerData;
import net.minecraft.server.level.ServerPlayer;

/**
 * Functional interface for checking conditional placeholders
 */
@FunctionalInterface
public interface ConditionalPlaceholderChecker {
    /**
     * Checks if the condition is met
     * 
     * @param player The player
     * @param data The player data
     * @param condition The condition to check
     * @return True if the condition is met
     */
    boolean check(ServerPlayer player, TabPlayerData data, String condition);
}
