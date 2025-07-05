package com.zerog.neoessentials.ui.economy;

import com.zerog.neoessentials.util.LanguageUtil;
import net.minecraft.server.level.ServerPlayer;

/**
 * Economy Settings GUI (placeholder)
 * 
 * @author ZeroG
 * @since 1.0.2.131
 */
public class EconomySettingsGUI {
    
    public static void openEconomySettingsGUI(ServerPlayer player) {
        LanguageUtil.sendMessage(player, "neoessentials.economy.economy_settings_coming_soon");
    }
}
