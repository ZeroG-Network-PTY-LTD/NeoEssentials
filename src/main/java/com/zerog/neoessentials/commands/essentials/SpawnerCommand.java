package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Spawner command implementation for NeoEssentials
 * Allows modification of mob spawner properties
 * 
 * Commands:
 * - /spawner <mob> - Set spawner mob type (looking at spawner)
 * - /spawner <mob> [delay] - Set spawner mob type and delay
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class SpawnerCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /spawner <mob> - Set spawner mob type
        dispatcher.register(Commands.literal("spawner")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .then(Commands.argument("mob", StringArgumentType.word())
                .executes(ctx -> setSpawnerMob(ctx, 20)) // Default delay of 20 ticks (1 second)
                .then(Commands.argument("delay", IntegerArgumentType.integer(1, 200))
                    .executes(ctx -> setSpawnerMob(ctx, IntegerArgumentType.getInteger(ctx, "delay")))
                )
            )
        );
    }

    /**
     * Set spawner mob type and delay
     */
    private static int setSpawnerMob(CommandContext<CommandSourceStack> context, int delay) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String mobName = StringArgumentType.getString(context, "mob");
        
        // Get the block the player is looking at
        BlockPos spawnerPos = getTargetSpawnerPosition(player);
        if (spawnerPos == null) {
            context.getSource().sendFailure(Component.literal("§cYou must be looking at a spawner!"));
            return 0;
        }
        
        ServerLevel level = player.serverLevel();
        BlockEntity blockEntity = level.getBlockEntity(spawnerPos);
        
        if (!(blockEntity instanceof SpawnerBlockEntity spawner)) {
            context.getSource().sendFailure(Component.literal("§cThat block is not a spawner!"));
            return 0;
        }
        
        // Parse entity type from string
        ResourceLocation entityId;
        try {
            if (mobName.contains(":")) {
                entityId = ResourceLocation.parse(mobName);
            } else {
                entityId = ResourceLocation.fromNamespaceAndPath("minecraft", mobName);
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cInvalid mob name: " + mobName));
            return 0;
        }
        
        // Verify the entity type exists
        EntityType<?> entityType = level.registryAccess()
            .registryOrThrow(Registries.ENTITY_TYPE)
            .get(entityId);
            
        if (entityType == null) {
            context.getSource().sendFailure(Component.literal("§cInvalid entity type: " + entityId));
            return 0;
        }
        
        // Set the spawner properties
        try {
            BaseSpawner spawnerData = spawner.getSpawner();
            spawnerData.setEntityId(entityType, level, level.random, spawnerPos);
            
            // Mark for update
            spawner.setChanged();
            level.sendBlockUpdated(spawnerPos, 
                level.getBlockState(spawnerPos), 
                level.getBlockState(spawnerPos), 3);
            
            String entityName = entityType.getDescription().getString();
            String message = delay == 20 ? 
                "§aSpawner set to spawn §e" + entityName :
                "§aSpawner set to spawn §e" + entityName + "§a with delay of §e" + delay + "§a ticks";
                
            context.getSource().sendSuccess(() -> Component.literal(message), true);
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cFailed to modify spawner: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Get the position of the spawner the player is looking at
     */
    private static BlockPos getTargetSpawnerPosition(ServerPlayer player) {
        HitResult hitResult = player.pick(5.0D, 1.0F, false);
        
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult) hitResult;
            BlockPos pos = blockHitResult.getBlockPos();
            
            if (player.level().getBlockState(pos).is(Blocks.SPAWNER)) {
                return pos;
            }
        }
        
        return null;
    }
}
