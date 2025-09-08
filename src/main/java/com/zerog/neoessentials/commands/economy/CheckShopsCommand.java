package com.zerog.neoessentials.commands.economy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command to check shop integrity and diagnose shop disappearance issues
 */
public class CheckShopsCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckShopsCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("checkshops")
                .requires(source -> source.hasPermission(4)) // OP level 4
                .executes(CheckShopsCommand::checkShops)
        );
    }
    
    private static int checkShops(CommandContext<CommandSourceStack> context) {
        try {
            var shopManager = com.zerog.neoessentials.economy.shops.ShopManager.getInstance();
            if (shopManager == null) {
                context.getSource().sendSystemMessage(Component.literal("§cShop manager not available!"));
                return 0;
            }
            
            var signShops = shopManager.getSignShops();
            context.getSource().sendSystemMessage(
                Component.literal("§eChecking integrity of " + signShops.size() + " registered shops...")
            );
            
            int validShops = 0;
            int invalidShops = 0;
            int missingSignShops = 0;
            int missingChestShops = 0;
            
            for (var shop : signShops) {
                boolean isValid = true;
                StringBuilder issues = new StringBuilder();
                
                // Check if sign still exists
                try {
                    ServerLevel level = context.getSource().getLevel();
                    BlockPos signPos = shop.getSignPos();
                    
                    if (level.getBlockEntity(signPos) instanceof SignBlockEntity) {
                        // Sign exists
                    } else {
                        isValid = false;
                        issues.append("Missing sign; ");
                        missingSignShops++;
                    }
                    
                    // Check if chest still exists (if not admin shop)
                    if (shop.getChestPos() != null && !"SERVER".equals(shop.getOwnerId())) {
                        BlockPos chestPos = shop.getChestPos();
                        if (!(level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity)) {
                            isValid = false;
                            issues.append("Missing chest; ");
                            missingChestShops++;
                        }
                    }
                    
                    // Validate shop data
                    if (shop.getItem() == null || shop.getItem().isEmpty()) {
                        isValid = false;
                        issues.append("Invalid item; ");
                    }
                    
                    if (shop.getBuyPrice() < 0 && shop.getSellPrice() < 0) {
                        isValid = false;
                        issues.append("Invalid prices; ");
                    }
                    
                } catch (Exception e) {
                    isValid = false;
                    issues.append("Exception: ").append(e.getMessage()).append("; ");
                    LOGGER.warn("Error checking shop at {}: {}", shop.getSignPos(), e.getMessage());
                }
                
                if (isValid) {
                    validShops++;
                } else {
                    invalidShops++;
                    context.getSource().sendSystemMessage(
                        Component.literal("§c✗ Shop at " + shop.getSignPos().toShortString() + 
                                        " - Issues: " + issues.toString())
                    );
                }
            }
            
            // Summary report
            context.getSource().sendSystemMessage(Component.literal("§a=== Shop Integrity Report ==="));
            context.getSource().sendSystemMessage(Component.literal("§aTotal shops: " + signShops.size()));
            context.getSource().sendSystemMessage(Component.literal("§aValid shops: " + validShops));
            context.getSource().sendSystemMessage(Component.literal("§cInvalid shops: " + invalidShops));
            context.getSource().sendSystemMessage(Component.literal("§cMissing signs: " + missingSignShops));
            context.getSource().sendSystemMessage(Component.literal("§cMissing chests: " + missingChestShops));
            
            LOGGER.info("Shop integrity check completed - {}/{} shops valid", validShops, signShops.size());
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendSystemMessage(
                Component.literal("§cError checking shops: " + e.getMessage())
            );
            LOGGER.error("Error in shop integrity check command", e);
            return 0;
        }
    }
}
