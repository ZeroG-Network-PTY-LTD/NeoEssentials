package com.zerog.neoessentials.test;

import com.zerog.neoessentials.gui.CustomGuiManager;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class to verify GUI functionality works correctly
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class GuiTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuiTest.class);
    
    /**
     * Test opening a shop GUI for a player
     */
    public static void testShopGui(ServerPlayer player) {
        try {
            LOGGER.info("Testing shop GUI for player: {}", player.getName().getString());
            
            CustomGuiManager guiManager = CustomGuiManager.getInstance();
            guiManager.openGui(player, CustomGuiManager.GuiType.SHOP_MAIN);
            
            LOGGER.info("Successfully opened shop GUI");
            
        } catch (Exception e) {
            LOGGER.error("Failed to test shop GUI", e);
        }
    }
    
    /**
     * Test opening player stats GUI
     */
    public static void testStatsGui(ServerPlayer player) {
        try {
            LOGGER.info("Testing stats GUI for player: {}", player.getName().getString());
            
            CustomGuiManager guiManager = CustomGuiManager.getInstance();
            guiManager.openGui(player, CustomGuiManager.GuiType.PLAYER_STATS);
            
            LOGGER.info("Successfully opened stats GUI");
            
        } catch (Exception e) {
            LOGGER.error("Failed to test stats GUI", e);
        }
    }
    
    /**
     * Test opening kit selector GUI
     */
    public static void testKitGui(ServerPlayer player) {
        try {
            LOGGER.info("Testing kit GUI for player: {}", player.getName().getString());
            
            CustomGuiManager guiManager = CustomGuiManager.getInstance();
            guiManager.openGui(player, CustomGuiManager.GuiType.KIT_SELECTOR);
            
            LOGGER.info("Successfully opened kit GUI");
            
        } catch (Exception e) {
            LOGGER.error("Failed to test kit GUI", e);
        }
    }
    
    /**
     * Test all GUI types
     */
    public static void testAllGuis(ServerPlayer player) {
        LOGGER.info("Testing all GUI types for player: {}", player.getName().getString());
        
        // Test with small delays between each GUI
        testShopGui(player);
        
        // In a real implementation, you'd schedule these with delays
        // For now, just test one at a time
        LOGGER.info("GUI testing complete. Test each GUI individually using respective test methods.");
    }
}
