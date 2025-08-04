package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

/**
 * Heal command implementation - /heal [player]
 * Restores a player's health and hunger to full
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class HealCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /heal - Heal yourself
        dispatcher.register(Commands.literal("heal")
            .requires(source -> source.hasPermission(2))
            .executes(HealCommand::healSelf)
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> source.hasPermission(2))
                .executes(HealCommand::healOther)
            )
        );
    }
    
    /**
     * Heal the command executor
     */
    private static int healSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        healPlayer(player);
        
        context.getSource().sendSuccess(() -> Component.literal("§aYou have been healed!"), false);
        return 1;
    }
    
    /**
     * Heal another player
     */
    private static int healOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        ServerPlayer executor = context.getSource().getPlayerOrException();
        
        healPlayer(target);
        
        // Send confirmation to both players
        context.getSource().sendSuccess(() -> Component.literal("§aYou have healed " + target.getName().getString() + "!"), true);
        target.sendSystemMessage(Component.literal("§aYou have been healed by " + executor.getName().getString() + "!"));
        
        return 1;
    }
    
    /**
     * Perform the healing operation
     */
    private static void healPlayer(ServerPlayer player) {
        // Restore full health
        player.setHealth(player.getMaxHealth());
        
        // Restore full hunger
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0f);
        
        // Remove harmful effects
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WITHER);
        player.removeEffect(MobEffects.HUNGER);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
        player.removeEffect(MobEffects.CONFUSION);
        player.removeEffect(MobEffects.BLINDNESS);
        
        // Extinguish fire
        player.clearFire();
    }
}
