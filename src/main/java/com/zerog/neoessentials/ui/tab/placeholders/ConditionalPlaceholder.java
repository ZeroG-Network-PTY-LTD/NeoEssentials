package com.zerog.neoessentials.ui.tab.placeholders;

import com.zerog.neoessentials.ui.tab.TabPlayerData;
import net.minecraft.server.level.ServerPlayer;

/**
 * Interface for placeholder conditions
 */
public interface ConditionalPlaceholder {
    /**
     * Checks if the condition matches
     * 
     * @param player The player
     * @param data The player data
     * @param condition The condition to check
     * @return True if the condition is met
     */
    boolean matches(ServerPlayer player, TabPlayerData data, String condition);
    
    /**
     * Processes the conditional placeholder
     * 
     * @param player The player
     * @param data The player data
     * @param condition The condition string
     * @param trueValue The value to return if true
     * @param falseValue The value to return if false
     * @return The processed result
     */
    String process(ServerPlayer player, TabPlayerData data, String condition, 
                 String trueValue, String falseValue);
}
