package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * World management commands for NeoEssentials.
 * Provides commands for world teleportation, listing, and basic management.
 */
public class WorldCommands {

    /**
     * Registers all world management commands.
     *
     * @param dispatcher The command dispatcher to register commands with
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /world command group
        dispatcher.register(
            Commands.literal("world")
                .requires(source -> PermissionUtil.hasModeratorPermission(source, "neoessentials.command.world"))
                .then(
                    // /world list - List all worlds
                    Commands.literal("list")
                        .executes(this::listWorlds)
                )
                .then(
                    // /world tp <world> - Teleport to a world
                    Commands.literal("tp")
                        .then(
                            Commands.argument("world", DimensionArgument.dimension())
                                .executes(this::teleportToWorld)
                                .then(
                                    Commands.argument("player", EntityArgument.player())
                                        .executes(this::teleportPlayerToWorld)
                                )
                        )
                )
                .then(
                    // /world info <world> - Get world information
                    Commands.literal("info")
                        .then(
                            Commands.argument("world", DimensionArgument.dimension())
                                .executes(this::getWorldInfo)
                        )
                )
                .then(
                    // /world spawn <world> - Teleport to world spawn
                    Commands.literal("spawn")
                        .then(
                            Commands.argument("world", DimensionArgument.dimension())
                                .executes(this::teleportToWorldSpawn)
                        )
                )
        );
    }

    /**
     * Lists all available worlds.
     */
    private int listWorlds(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = null;
        try {
            player = context.getSource().getPlayerOrException();
        } catch (Exception e) {
            // Command run from console
        }

        MinecraftServer server = context.getSource().getServer();
        Iterable<ServerLevel> levels = server.getAllLevels();

        if (player != null) {
            LanguageUtil.sendMessage(player, "commands.world.list.header");
        } else {
            NeoEssentials.LOGGER.info("Available worlds:");
        }

        for (ServerLevel level : levels) {
            String worldName = level.dimension().location().toString();
            int playerCount = level.players().size();
            
            String message = String.format("§6%s §7- Players: §e%d",
                worldName, playerCount);
            
            if (player != null) {
                LanguageUtil.sendMessage(player, message);
            } else {
                NeoEssentials.LOGGER.info(message);
            }
        }

        return 1;
    }

    /**
     * Teleports the command sender to a specific world.
     */
    private int teleportToWorld(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ServerLevel targetLevel = DimensionArgument.getDimension(context, "world");
            
            if (targetLevel == null) {
                LanguageUtil.sendErrorMessage(player, "commands.world.tp.invalid");
                return 0;
            }

            // Teleport to the world spawn
            BlockPos spawnPos = targetLevel.getSharedSpawnPos();
            double x = spawnPos.getX();
            double y = spawnPos.getY();
            double z = spawnPos.getZ();
            
            player.teleportTo(targetLevel, x, y, z, player.getYRot(), player.getXRot());
            
            LanguageUtil.sendMessage(player, "commands.world.tp.success", 
                targetLevel.dimension().location().toString());
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error teleporting to world: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Teleports another player to a specific world.
     */
    private int teleportPlayerToWorld(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer executor = context.getSource().getPlayerOrException();
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            ServerLevel targetLevel = DimensionArgument.getDimension(context, "world");
            
            if (targetLevel == null) {
                LanguageUtil.sendErrorMessage(executor, "commands.world.tp.invalid");
                return 0;
            }

            // Teleport to the world spawn
            BlockPos spawnPos = targetLevel.getSharedSpawnPos();
            double x = spawnPos.getX();
            double y = spawnPos.getY();
            double z = spawnPos.getZ();
            
            target.teleportTo(targetLevel, x, y, z, target.getYRot(), target.getXRot());
            
            LanguageUtil.sendMessage(executor, "commands.world.tp.success.other", 
                target.getScoreboardName(), targetLevel.dimension().location().toString());
            LanguageUtil.sendMessage(target, "commands.world.tp.success", 
                targetLevel.dimension().location().toString());
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error teleporting player to world: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Gets information about a specific world.
     */
    private int getWorldInfo(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ServerLevel level = DimensionArgument.getDimension(context, "world");
            
            if (level == null) {
                LanguageUtil.sendErrorMessage(player, "commands.world.info.invalid");
                return 0;
            }

            String worldName = level.dimension().location().toString();
            int playerCount = level.players().size();
            long worldTime = level.getDayTime();
            boolean isDay = level.isDay();
            boolean isThundering = level.isThundering();
            boolean isRaining = level.isRaining();
            
            LanguageUtil.sendMessage(player, "commands.world.info.header", worldName);
            LanguageUtil.sendMessage(player, "§7Players: §e" + playerCount);
            LanguageUtil.sendMessage(player, "§7World Time: §e" + worldTime);
            LanguageUtil.sendMessage(player, "§7Day Time: §" + (isDay ? "a" : "c") + (isDay ? "Day" : "Night"));
            LanguageUtil.sendMessage(player, "§7Weather: §e" + (isThundering ? "Thundering" : isRaining ? "Raining" : "Clear"));
            
            // Spawn coordinates
            BlockPos spawnPos = level.getSharedSpawnPos();
            double spawnX = spawnPos.getX();
            double spawnY = spawnPos.getY();
            double spawnZ = spawnPos.getZ();
            LanguageUtil.sendMessage(player, "§7Spawn: §e" + String.format("%.1f, %.1f, %.1f", spawnX, spawnY, spawnZ));
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error getting world info: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Teleports the command sender to a world's spawn point.
     */
    private int teleportToWorldSpawn(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ServerLevel targetLevel = DimensionArgument.getDimension(context, "world");
            
            if (targetLevel == null) {
                LanguageUtil.sendErrorMessage(player, "commands.world.spawn.invalid");
                return 0;
            }

            // Get spawn coordinates
            BlockPos spawnPos = targetLevel.getSharedSpawnPos();
            double x = spawnPos.getX();
            double y = spawnPos.getY();
            double z = spawnPos.getZ();
            
            // Teleport to spawn
            player.teleportTo(targetLevel, x, y, z, player.getYRot(), player.getXRot());
            
            LanguageUtil.sendMessage(player, "commands.world.spawn.success", 
                targetLevel.dimension().location().toString());
            
            return 1;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error teleporting to world spawn: {}", e.getMessage());
            return 0;
        }
    }
}
