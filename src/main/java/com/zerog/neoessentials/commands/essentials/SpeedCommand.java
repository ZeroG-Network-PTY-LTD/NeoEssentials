package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;

/**
 * Speed command implementation - /speed <type> <value> [player]
 * Controls player movement speed for walking and flying
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class SpeedCommand {
    
    // Speed limits
    private static final float MIN_SPEED = 0.0f;
    private static final float MAX_SPEED = 1.0f;
    private static final float DEFAULT_WALK_SPEED = 0.1f;
    private static final float DEFAULT_FLY_SPEED = 0.05f;
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /speed <walking/flying> <speed> - Set your own speed
        dispatcher.register(Commands.literal("speed")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .then(Commands.argument("type", StringArgumentType.word())
                .then(Commands.argument("speed", FloatArgumentType.floatArg(MIN_SPEED, MAX_SPEED))
                    .executes(SpeedCommand::setSpeedSelf)
                    .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
                        .executes(SpeedCommand::setSpeedOther)
                    )
                )
            )
        );
        // Alias: /sp
        dispatcher.register(Commands.literal("sp")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .then(Commands.argument("type", StringArgumentType.word())
                .then(Commands.argument("speed", FloatArgumentType.floatArg(MIN_SPEED, MAX_SPEED))
                    .executes(SpeedCommand::setSpeedSelf)
                    .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
                        .executes(SpeedCommand::setSpeedOther)
                    )
                )
            )
        );
    }
    
    /**
     * Set speed for the command executor
     */
    private static int setSpeedSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String type = StringArgumentType.getString(context, "type");
        float speed = FloatArgumentType.getFloat(context, "speed");
        boolean success = setPlayerSpeed(player, type, speed);
        if (success) {
            context.getSource().sendSuccess(() -> com.zerog.neoessentials.util.MessageUtil.translatable(player, "neoessentials.speed.self_success", type, String.format("%.2f", speed)), false);
        } else {
            context.getSource().sendFailure(com.zerog.neoessentials.util.MessageUtil.translatable("neoessentials.speed.invalid_type"));
        }
        return success ? 1 : 0;
    }
    
    /**
     * Set speed for another player
     */
    private static int setSpeedOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        ServerPlayer executor = context.getSource().getPlayerOrException();
        String type = StringArgumentType.getString(context, "type");
        float speed = FloatArgumentType.getFloat(context, "speed");
        boolean success = setPlayerSpeed(target, type, speed);
        if (success) {
            context.getSource().sendSuccess(() -> com.zerog.neoessentials.util.MessageUtil.translatable(executor, "neoessentials.speed.other_success", target.getName().getString(), type, String.format("%.2f", speed)), true);
            target.sendSystemMessage(com.zerog.neoessentials.util.MessageUtil.translatable(target, "neoessentials.speed.success", type, String.format("%.2f", speed), executor.getName().getString()));
        } else {
            context.getSource().sendFailure(com.zerog.neoessentials.util.MessageUtil.translatable("neoessentials.speed.invalid_type"));
        }
        return success ? 1 : 0;
    }
    
    /**
     * Set speed for a specific player
     */
    private static boolean setPlayerSpeed(ServerPlayer player, String type, float speed) {
        Abilities abilities = player.getAbilities();
        
        switch (type.toLowerCase()) {
            case "walking", "walk", "w" -> {
                // Convert from 0.0-1.0 range to Minecraft's walking speed range
                // Minecraft default walking speed is 0.1f, max reasonable is ~1.0f
                float walkSpeed = speed * 10.0f; // Scale up for walking
                abilities.setWalkingSpeed(Math.min(walkSpeed, 1.0f));
                player.onUpdateAbilities();
                return true;
            }
            case "flying", "fly", "f" -> {
                // Convert from 0.0-1.0 range to Minecraft's flying speed range  
                // Minecraft default flying speed is 0.05f, max reasonable is ~1.0f
                float flySpeed = speed * 2.0f; // Scale up for flying
                abilities.setFlyingSpeed(Math.min(flySpeed, 1.0f));
                player.onUpdateAbilities();
                return true;
            }
            default -> {
                return false;
            }
        }
    }
    
    /**
     * Reset player speed to defaults
     */
    public static void resetSpeed(ServerPlayer player) {
        Abilities abilities = player.getAbilities();
        abilities.setWalkingSpeed(DEFAULT_WALK_SPEED);
        abilities.setFlyingSpeed(DEFAULT_FLY_SPEED);
        player.onUpdateAbilities();
    }
    
    /**
     * Get current walking speed
     */
    public static float getWalkingSpeed(ServerPlayer player) {
        return player.getAbilities().getWalkingSpeed();
    }
    
    /**
     * Get current flying speed
     */
    public static float getFlyingSpeed(ServerPlayer player) {
        return player.getAbilities().getFlyingSpeed();
    }
}