package com.zerog.neoessentials;

import com.zerog.neoessentials.economy.shops.ShopManager;
import com.zerog.neoessentials.economy.shops.SignShopHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event listener for shop sign interactions
 * Prevents shop signs from entering edit mode and handles shop transactions
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class ShopSignEventListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ShopSignEventListener.class);
    private static final String SHOP_HEADER = "[SHOP]";
    private static final String ADMIN_SHOP_HEADER = "[Admin Shop]";
    
    private final SignShopHandler shopHandler;
    
    public ShopSignEventListener(ShopManager shopManager) {
        this.shopHandler = new SignShopHandler(shopManager);
    }
    
    /**
     * Handle right-click on blocks (specifically signs)
     * This event is fired with HIGH priority to intercept shop sign interactions
     * before the default sign editing behavior takes over
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // Only handle server-side events
        if (event.getLevel().isClientSide()) {
            return;
        }
        
        BlockPos clickedPos = event.getPos();
        
        // Check if the clicked block is a sign
        if (!(event.getLevel().getBlockState(clickedPos).getBlock() instanceof SignBlock)) {
            return;
        }
        
        // Check if it's a shop sign
        if (!isShopSign(event.getLevel(), clickedPos)) {
            return;
        }
        
        LOGGER.debug("Shop sign interaction detected at {} by player {}", 
                    clickedPos, event.getEntity().getName().getString());
        
        // Use existing SignShopHandler to handle the interaction
        InteractionResult result = shopHandler.handleSignInteraction(
            event.getEntity(),
            event.getLevel(),
            clickedPos,
            event.getHand()
        );
        
        // If the shop interaction was successful or failed (but was a shop), 
        // prevent the default sign editing behavior
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }
    
    /**
     * Check if a sign at the given position is a shop sign
     */
    private boolean isShopSign(net.minecraft.world.level.Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity signEntity)) {
            return false;
        }
        
        // Check if the first line contains the shop header
        Component[] lines = signEntity.getFrontText().getMessages(false);
        if (lines.length == 0) {
            return false;
        }
        
        String firstLine = lines[0].getString();
        return SHOP_HEADER.equals(firstLine) || ADMIN_SHOP_HEADER.equals(firstLine);
    }
}
