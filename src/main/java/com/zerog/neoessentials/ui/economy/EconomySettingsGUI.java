package com.zerog.neoessentials.ui.economy;

import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.EconomyManager;
import net.minecraft.server.level.ServerPlayer;

/**
 * Economy Settings GUI for viewing economy configuration
 * Simplified chat-based interface for reliability
 * 
 * @author ZeroG
 * @since 1.0.2.133
 */
public class EconomySettingsGUI {
    
    /**
     * Opens a simplified economy settings GUI (chat-based)
     */
    public static void openEconomySettingsGUI(ServerPlayer player) {
        try {
            EconomyManager economyManager = NeoEssentials.getInstance().getDataManager().getEconomyManager();
            
            LanguageUtil.sendMessage(player, "neoessentials.economy.settings_header");
            
            // Display current economy settings
            LanguageUtil.sendMessage(player, "neoessentials.economy.settings_currency_info");
            LanguageUtil.sendMessage(player, "neoessentials.economy.settings_currency_symbol", "$");
            LanguageUtil.sendMessage(player, "neoessentials.economy.settings_currency_name", "Coins");
            
            // Display economy statistics
            LanguageUtil.sendMessage(player, "neoessentials.economy.settings_stats_header");
            double totalCurrency = economyManager.getTotalCurrency();
            LanguageUtil.sendMessage(player, "neoessentials.economy.settings_total_currency", String.format("$%.2f", totalCurrency));
            
            // Display player count
            int playerCount = economyManager.getPlayerCount();
            LanguageUtil.sendMessage(player, "neoessentials.economy.settings_active_players", String.valueOf(playerCount));
            
            // Display system status
            LanguageUtil.sendMessage(player, "neoessentials.economy.settings_system_status");
            LanguageUtil.sendMessage(player, "neoessentials.economy.settings_status_operational");
            
            // Display available features
            LanguageUtil.sendMessage(player, "neoessentials.economy.settings_features_header");
            LanguageUtil.sendMessage(player, "neoessentials.economy.settings_feature_transfers");
            LanguageUtil.sendMessage(player, "neoessentials.economy.settings_feature_history");
            LanguageUtil.sendMessage(player, "neoessentials.economy.settings_feature_stats");
            LanguageUtil.sendMessage(player, "neoessentials.economy.settings_feature_banking_planned");
            
            LanguageUtil.sendMessage(player, "neoessentials.economy.settings_footer");
            
        } catch (Exception e) {
            LanguageUtil.sendMessage(player, "neoessentials.economy.settings_error", e.getMessage());
        }
    }
}
