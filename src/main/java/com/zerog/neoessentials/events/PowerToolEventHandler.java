package com.zerog.neoessentials.events;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.PowerToolManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Handles events related to Powertool functionality.
 */
public class PowerToolEventHandler {
    
    @SubscribeEvent
    public void onPlayerRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        PowerToolManager powerToolManager = NeoEssentials.getInstance().getDataManager().getPowerToolManager();
        
        // Check if powertools are enabled for this player
        if (!powerToolManager.isPowerToolEnabled(player)) {
            return;
        }
        
        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (heldItem.isEmpty()) {
            return;
        }
        
        String command = powerToolManager.getPowerToolCommand(player, heldItem.getItem());
        if (command == null) {
            return;
        }
        
        // Execute the command
        MinecraftServer server = player.getServer();
        if (server != null) {
            CommandSourceStack source = player.createCommandSourceStack();
            server.getCommands().performPrefixedCommand(source, command);
            
            // Cancel the event to prevent the normal item use
            event.setCanceled(true);
        }
    }
}
