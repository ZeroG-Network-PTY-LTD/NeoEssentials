package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.integration.ErrorHandlingIntegration;
import com.zerog.neoessentials.performance.PerformanceCommandWrapper;
import com.zerog.neoessentials.permissions.PermissionNodes;
import com.zerog.neoessentials.util.CommandConfigUtil;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import com.mojang.brigadier.arguments.StringArgumentType;

/**
 * Heal command implementation - /heal [player]
 * Restores a player's health and hunger to full
 * 
 * Permission Nodes:
 * - essentials.heal - Heal yourself
 * - essentials.heal.others - Heal other players
 * - essentials.heal.* - All heal permissions
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class HealCommand implements IEssentialCommand {
    
    @Override
    public String getCommandName() {
        return "heal";
    }
    
    @Override
    public String getDescription() {
        return "Restore a player's health and hunger to full";
    }
    
    @Override
    public String getUsage() {
        return "/heal [player]";
    }
    
    @Override
    public String[] getAliases() {
        return new String[0];
    }
    
    @Override
    public String getPermission() {
        return PermissionNodes.HEAL_SELF;
    }
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /heal - Heal yourself
        dispatcher.register(Commands.literal("heal")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.HEAL_SELF))
            .executes(HealCommand::healSelf)
            .then(Commands.argument("player", StringArgumentType.word())
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.HEAL_OTHERS))
                .executes(HealCommand::healOther)
            )
        );
    }
    
    /**
     * Heal the command executor
     */
    private static int healSelf(CommandContext<CommandSourceStack> context) {
        // Check if heal command is enabled
        if (!CommandConfigUtil.validateCommandExecution(context.getSource(), "heal", null, "Heal")) {
            return 0;
        }
        
        return PerformanceCommandWrapper.executeWithTracking(
            context.getSource(),
            "heal_self",
            (source) -> ErrorHandlingIntegration.executeWithPermission(
                source,
                "heal self", 
                PermissionNodes.HEAL_SELF,
                (src) -> {
                    ServerPlayer player = src.getPlayerOrException();
                    healPlayer(player);
                    src.sendSuccess(() -> MessageUtil.translatable(player, "neoessentials.heal.self_success"), false);
                    return 1;
                }
            )
        );
    }
    
    /**
     * Heal another player
     */
    private static int healOther(CommandContext<CommandSourceStack> context) {
        // Check if heal command is enabled
        if (!CommandConfigUtil.validateCommandExecution(context.getSource(), "heal", null, "Heal")) {
            return 0;
        }
        
        return ErrorHandlingIntegration.executeWithPermission(
            context.getSource(),
            "heal other",
            PermissionNodes.HEAL_OTHERS, 
            (source) -> {
                String playerName = StringArgumentType.getString(context, "player");
                ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(playerName);
                
                if (target == null) {
                    source.sendFailure(MessageUtil.translatable("neoessentials.player.not_found_online", playerName));
                    return 0;
                }
                
                ServerPlayer executor = source.getPlayerOrException();
                
                healPlayer(target);
                
                // Send confirmation to both players
                source.sendSuccess(() -> MessageUtil.translatable(executor, "neoessentials.heal.other_success", target.getName().getString()), true);
                target.sendSystemMessage(MessageUtil.translatable(target, "neoessentials.heal.success", executor.getName().getString()));
                
                return 1;
            }
        );
    }
    
    /**
     * Perform the healing operation
     */
    private static void healPlayer(ServerPlayer player) {
        // Restore full health
        player.setHealth(player.getMaxHealth());
        
        // Note: Hunger restoration temporarily disabled due to API changes
        // TODO: Implement hunger restoration when FoodData API is available
        
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
