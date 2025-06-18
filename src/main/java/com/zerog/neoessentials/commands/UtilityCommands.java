package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.utils.TextUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Implements utility commands like jump, break, etc.
 */
public class UtilityCommands {

    /**
     * Registers all utility commands with the dispatcher.
     *
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerJumpCommand(dispatcher);
    }

    /**
     * Registers the jump command.
     *
     * @param dispatcher The command dispatcher
     */
    private void registerJumpCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /jump - Jump to where you're looking
        LiteralArgumentBuilder<CommandSourceStack> jumpCommand = Commands.literal("jump")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.jump"))
                .executes(this::executeJump);

        dispatcher.register(jumpCommand);
        
        // Register aliases
        dispatcher.register(Commands.literal("j")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.jump"))
                .executes(this::executeJump));
    }
    
    /**
     * Executes the jump command, teleporting the player to where they're looking.
     *
     * @param context The command context
     * @return 1 if successful, 0 otherwise
     */
    private int executeJump(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        
        // Calculate look position (max distance 100 blocks)
        Vec3 eyePosition = player.getEyePosition();
        Vec3 viewVector = player.getViewVector(1.0F);
        Vec3 targetVector = eyePosition.add(viewVector.x * 100, viewVector.y * 100, viewVector.z * 100);
        
        // Perform ray trace
        ClipContext clipContext = new ClipContext(
            eyePosition, 
            targetVector, 
            ClipContext.Block.OUTLINE, 
            ClipContext.Fluid.NONE, 
            player
        );
        
        BlockHitResult hitResult = level.clip(clipContext);
        
        if (hitResult.getType() == HitResult.Type.MISS) {
            source.sendFailure(Component.translatable("neoessentials.commands.jump.no_target"));
            return 0;
        }
        
        // Get the hit position and adjust for safe teleport
        BlockPos hitPos = hitResult.getBlockPos();
        Direction face = hitResult.getDirection();
        
        // Move to the block adjacent to the one hit
        BlockPos targetPos = hitPos.relative(face);
        
        // Ensure the target block is air/clear to stand in
        BlockState blockState = level.getBlockState(targetPos);
        BlockState blockAbove = level.getBlockState(targetPos.above());
        
        if (!blockState.getCollisionShape(level, targetPos).isEmpty() || 
            !blockAbove.getCollisionShape(level, targetPos.above()).isEmpty()) {
            source.sendFailure(Component.translatable("neoessentials.commands.jump.unsafe_target"));
            return 0;
        }
        
        // Teleport to the center of the block
        double targetX = targetPos.getX() + 0.5;
        double targetY = targetPos.getY();
        double targetZ = targetPos.getZ() + 0.5;
        
        player.teleportTo(targetX, targetY, targetZ);
        source.sendSuccess(() -> Component.literal(TextUtil.formatText("&aJumped to your target block.")), true);
        
        return 1;
    }
}
