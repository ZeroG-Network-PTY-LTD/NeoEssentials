package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.economy.shops.ShopManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command to create sign shops
 */
public class CreateShopCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateShopCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("createshop")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC)) // Require op level 2
            .then(Commands.argument("buyPrice", DoubleArgumentType.doubleArg(0))
                .then(Commands.argument("sellPrice", DoubleArgumentType.doubleArg(0))
                    .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                        .executes(CreateShopCommand::createShopCommand))))
        );
    }
    
    private static int createShopCommand(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            
            if (!(source.getEntity() instanceof Player player)) {
                source.sendFailure(Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en", "neoessentials.shop.create.player_only")));
                return 0;
            }
            net.minecraft.server.level.ServerPlayer serverPlayer = (player instanceof net.minecraft.server.level.ServerPlayer sp) ? sp : null;
            
            double buyPrice = DoubleArgumentType.getDouble(context, "buyPrice");
            double sellPrice = DoubleArgumentType.getDouble(context, "sellPrice");
            int quantity = IntegerArgumentType.getInteger(context, "quantity");
            
            // Get the item the player is holding
            ItemStack heldItem = player.getMainHandItem();
                if (heldItem.isEmpty()) {
                    player.sendSystemMessage(Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(serverPlayer, "neoessentials.shop.create.hold_item")));
                return 0;
            }
            
            // Get the block the player is looking at
            HitResult hitResult = player.pick(5.0, 0.0F, false);
                if (!(hitResult instanceof BlockHitResult blockHit)) {
                    player.sendSystemMessage(Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(serverPlayer, "neoessentials.shop.create.look_sign")));
                return 0;
            }
            
            BlockPos signPos = blockHit.getBlockPos();
            
            // Check if it's a sign
            if (!(player.level().getBlockState(signPos).getBlock() instanceof SignBlock)) {
                player.sendSystemMessage(Component.literal("§cYou must be looking at a sign to create a shop!"));
                return 0;
            }
            
            // Check if it's already a shop sign
            if (player.level().getBlockEntity(signPos) instanceof SignBlockEntity signEntity) {
                net.minecraft.network.chat.Component[] lines = signEntity.getFrontText().getMessages(false);
                if (lines.length > 0 && "[SHOP]".equals(lines[0].getString())) {
                        player.sendSystemMessage(Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(serverPlayer, "neoessentials.shop.create.already_shop")));
                    return 0;
                }
            }
            
            ShopManager shopManager = ShopManager.getInstance();
            if (shopManager != null) {
                com.zerog.neoessentials.economy.shops.SignShopHandler shopHandler = 
                    new com.zerog.neoessentials.economy.shops.SignShopHandler(shopManager);
                
                boolean success = shopHandler.createSignShop(player, signPos, heldItem, buyPrice, sellPrice, quantity);
                
                if (success) {
                    player.sendSystemMessage(Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(serverPlayer, "neoessentials.shop.create.success", heldItem.getDisplayName().getString(), buyPrice, sellPrice, quantity)));
                    LOGGER.info("Player {} created shop at {} for {} (Buy: ${}, Sell: ${}, Qty: {})",
                               player.getName().getString(), signPos, heldItem.getDisplayName().getString(),
                               buyPrice, sellPrice, quantity);
                    return 1;
                } else {
                    player.sendSystemMessage(Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(serverPlayer, "neoessentials.shop.create.failed_generic")));
                    return 0;
                }
            } else {
                player.sendSystemMessage(Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(serverPlayer, "neoessentials.shop.create.manager_unavailable")));
                return 0;
            }
            
        } catch (Exception e) {
            LOGGER.error("Error executing createshop command", e);
            context.getSource().sendFailure(Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en", "neoessentials.shop.create.error_generic", e.getMessage())));
            return 0;
        }
    }
}
