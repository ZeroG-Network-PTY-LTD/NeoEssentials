package com.zerog.neoessentials.ui.tab.placeholders;

import com.zerog.neoessentials.ui.tab.TabPlayerData;
import net.minecraft.server.level.ServerPlayer;

/**
 * A simple condition matcher implementation
 */
public class SimpleConditionMatcher implements ConditionalPlaceholder {
    private final ConditionalPlaceholderChecker checker;
    
    /**
     * Creates a new simple condition matcher
     * 
     * @param checker The condition checker function
     */
    public SimpleConditionMatcher(ConditionalPlaceholderChecker checker) {
        this.checker = checker;
    }

    @Override
    public boolean matches(ServerPlayer player, TabPlayerData data, String condition) {
        return checker.check(player, data, condition);
    }
    
    @Override
    public String process(ServerPlayer player, TabPlayerData data, String condition, String trueValue, String falseValue) {
        return matches(player, data, condition) ? trueValue : falseValue;
    }
}
