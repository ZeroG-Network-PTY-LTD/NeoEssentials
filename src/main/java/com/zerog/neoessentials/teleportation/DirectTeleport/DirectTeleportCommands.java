package com.zerog.neoessentials.teleportation.DirectTeleport;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Random;

public class DirectTeleportCommands {
    private static final Random RANDOM = new Random();
    private static final String PERMISSION_TP = "neoessentials.teleport.tp";
    private static final String PERMISSION_TPHERE = "neoessentials.teleport.tphere";
    private static final String PERMISSION_TPPOS = "neoessentials.teleport.tppos";
    private static final String PERMISSION_TOP = "neoessentials.teleport.top";
    private static final String PERMISSION_TPR = "neoessentials.teleport.tpr";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ConfigManager config = ConfigManager.getInstance();
        
        // /tp command - teleport to player or coordinates
        if (config.isTeleportationEnabled() && config.isCommandEnabled("tp")) {
            registerTpCommand(dispatcher);
        }
        
        // /tphere command - bring player to you
        if (config.isTeleportationEnabled() && config.isCommandEnabled("tphere")) {
            registerTphereCommand(dispatcher);
        }
        
        // /tpall command - teleport all players to you
        if (config.isTeleportationEnabled() && config.isCommandEnabled("tpall")) {
            registerTpallCommand(dispatcher);
        }
        
        // /tppos command - teleport to specific coordinates
        if (config.isTeleportationEnabled() && config.isCommandEnabled("tppos")) {
            registerTpposCommand(dispatcher);
        }
        
        // /top command - teleport to highest block
        if (config.isTeleportationEnabled() && config.isCommandEnabled("top")) {
            registerTopCommand(dispatcher);
        }
        
        // /jumpto command - teleport to block you're looking at
        if (config.isTeleportationEnabled() && config.isCommandEnabled("jumpto")) {
            registerJumptoCommand(dispatcher);
        }
        
        // /jump command - alias for /jumpto
        if (config.isTeleportationEnabled() && config.isCommandEnabled("jump")) {
            registerJumpCommand(dispatcher);
        }
        
        // /tpr command - random teleport
        if (config.isTeleportationEnabled() && config.isCommandEnabled("tpr")) {
            registerTprCommand(dispatcher);
        }
        
        // /tpo command - teleport to offline player's last known location
        if (config.isTeleportationEnabled() && config.isCommandEnabled("tpo")) {
            registerTpoCommand(dispatcher);
        }

        // NOTE: /back command is registered in MiscTeleportCommands.java
    }
    
    private static void registerTpCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tp")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return PermissionAPI.hasPermission(player.getUUID(), PERMISSION_TP);
                }
                return source.hasPermission(2); // Console fallback
            })
            .then(Commands.argument("target", EntityArgument.player())
                .executes(context -> teleportToPlayer(context, context.getSource().getPlayerOrException(), 
                    EntityArgument.getPlayer(context, "target"))))
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(context -> teleportToPlayer(context, 
                        EntityArgument.getPlayer(context, "player"),
                        EntityArgument.getPlayer(context, "target")))))
            .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                    .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                        .executes(context -> teleportToCoordinates(context, context.getSource().getPlayerOrException(),
                            DoubleArgumentType.getDouble(context, "x"),
                            DoubleArgumentType.getDouble(context, "y"),
                            DoubleArgumentType.getDouble(context, "z"))))))
        );
    }
    
    private static void registerTphereCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tphere")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return PermissionAPI.hasPermission(player.getUUID(), PERMISSION_TPHERE);
                }
                return source.hasPermission(2); // Console fallback
            })
            .then(Commands.argument("player", EntityArgument.player())
                .executes(context -> teleportPlayerHere(context, EntityArgument.getPlayer(context, "player"))))
        );
    }
    
    private static void registerTpallCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpall")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return PermissionAPI.hasPermission(player.getUUID(), "neoessentials.teleport.admin.tpall");
                }
                return source.hasPermission(2); // Console/command block fallback
            })
            .executes(DirectTeleportCommands::teleportAllPlayers)
        );
    }
    

    
    private static void registerTpposCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tppos")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return PermissionAPI.hasPermission(player.getUUID(), PERMISSION_TPPOS);
                }
                return source.hasPermission(2); // Console fallback
            })
            .then(Commands.argument("coordinates", Vec3Argument.vec3())
                .executes(context -> {
                    try {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        Coordinates coords = Vec3Argument.getCoordinates(context, "coordinates");
                        Vec3 position = coords.getPosition(context.getSource());
                        return teleportToCoordinates(context, player, position.x, position.y, position.z);
                    } catch (CommandSyntaxException e) {
                        context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.admin.failed_coords", e.getMessage()));
                        return 0;
                    }
                }))
        );
    }
    

    
    private static void registerTopCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("top")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return PermissionAPI.hasPermission(player.getUUID(), PERMISSION_TOP);
                }
                return source.hasPermission(0); // Console fallback
            })
            .executes(DirectTeleportCommands::teleportToTop)
        );
    }
    

    
    private static void registerJumptoCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("jumpto")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return PermissionAPI.hasPermission(player.getUUID(), "neoessentials.teleport.jumpto");
                }
                return source.hasPermission(2); // Console/command block fallback
            })
            .executes(DirectTeleportCommands::jumpToTargetBlock)
        );
    }
    
    private static void registerJumpCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("jump")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return PermissionAPI.hasPermission(player.getUUID(), "neoessentials.teleport.jump");
                }
                return source.hasPermission(2); // Console/command block fallback
            })
            .executes(DirectTeleportCommands::jumpToTargetBlock)
        );
    }
    
    private static void registerTprCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpr")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return PermissionAPI.hasPermission(player.getUUID(), PERMISSION_TPR);
                }
                return source.hasPermission(0); // Console fallback
            })
            .executes(DirectTeleportCommands::randomTeleport)
        );
    }
    
    private static void registerTpoCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpo")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer player) {
                    return PermissionAPI.hasPermission(player.getUUID(), "neoessentials.teleport.admin.tpo");
                }
                return source.hasPermission(2); // Console/command blocks need OP 2
            })
            .then(Commands.argument("player", com.mojang.brigadier.arguments.StringArgumentType.word())
                .executes(context -> teleportToOfflinePlayer(context,
                    com.mojang.brigadier.arguments.StringArgumentType.getString(context, "player"))))
        );
    }

    // Command implementations
    private static int teleportToPlayer(CommandContext<CommandSourceStack> context, ServerPlayer player, ServerPlayer target) {
        try {
            if (player == target) {
                context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.admin.self"));
                return 0;
            }
            
            player.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), 
                target.getYRot(), target.getXRot());
            
            context.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.teleport.admin.teleported_player", 
                player.getName().getString(), target.getName().getString()), true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.admin.failed", e.getMessage()));
            return 0;
        }
    }
    
    private static int teleportToCoordinates(CommandContext<CommandSourceStack> context, ServerPlayer player, double x, double y, double z) {
        try {
            player.teleportTo(player.serverLevel(), x, y, z, player.getYRot(), player.getXRot());
            context.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.teleport.admin.teleported_player_coords", 
                player.getName().getString(), String.valueOf((int)x), String.valueOf((int)y), String.valueOf((int)z)), true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.admin.failed_coords", e.getMessage()));
            return 0;
        }
    }
    
    private static int teleportPlayerHere(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            target.teleportTo(player.serverLevel(), player.getX(), player.getY(), player.getZ(), 
                target.getYRot(), target.getXRot());
            
            context.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.teleport.admin.teleported_to", 
                target.getName().getString()), true);
            target.sendSystemMessage(MessageUtil.info("commands.neoessentials.teleport.admin.player_teleported_to_you", 
                player.getName().getString()));
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.admin.failed", e.getMessage()));
            return 0;
        }
    }
    
    private static int teleportAllPlayers(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            net.minecraft.server.MinecraftServer server = player.getServer();
            if (server == null) {
                context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.admin.failed", "Server not available"));
                return 0;
            }
            Collection<ServerPlayer> players = server.getPlayerList().getPlayers();

            int count = 0;
            for (ServerPlayer target : players) {
                if (target != player) {
                    target.teleportTo(player.serverLevel(), player.getX(), player.getY(), player.getZ(), 
                        target.getYRot(), target.getXRot());
                    count++;
                }
            }
            
            if (count == 0) {
                context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.admin.tpall.no_players"));
                return 0;
            }
            
            final int finalCount = count; // Make effectively final for lambda
            context.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.teleport.admin.tpall.teleported", 
                String.valueOf(finalCount), player.getName().getString()), true);
            return count;
        } catch (Exception e) {
            context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.admin.failed", e.getMessage()));
            return 0;
        }
    }
    
    private static int teleportToTop(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            BlockPos currentPos = player.blockPosition();
            ServerLevel level = player.serverLevel();
            
            // Find highest solid block above player
            BlockPos highestPos = null;
            for (int y = level.getMaxBuildHeight() - 1; y > currentPos.getY(); y--) {
                BlockPos checkPos = new BlockPos(currentPos.getX(), y, currentPos.getZ());
                if (!level.getBlockState(checkPos).isAir() && level.getBlockState(checkPos.above()).isAir()) {
                    highestPos = checkPos.above();
                    break;
                }
            }
            
            if (highestPos == null) {
                context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.misc.no_solid_block"));
                return 0;
            }
            
            player.teleportTo(level, highestPos.getX() + 0.5, highestPos.getY(), highestPos.getZ() + 0.5, 
                player.getYRot(), player.getXRot());
            
            context.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.teleport.misc.top_success"), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.misc.top_failed", e.getMessage()));
            return 0;
        }
    }
    
    private static int jumpToTargetBlock(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ServerLevel level = player.serverLevel();
            
            // Raycast to find target block
            Vec3 start = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            Vec3 end = start.add(look.scale(100)); // 100 block range
            
            BlockHitResult hitResult = level.clip(new ClipContext(start, end, 
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            
            if (hitResult.getType() == HitResult.Type.MISS) {
                context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.misc.no_block_in_sight"));
                return 0;
            }
            
            BlockPos targetPos = hitResult.getBlockPos();
            BlockPos teleportPos = targetPos.above();
            
            // Ensure safe landing spot
            if (!level.getBlockState(teleportPos).isAir() || !level.getBlockState(teleportPos.above()).isAir()) {
                context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.misc.jumpto_failed", "Target location unsafe"));
                return 0;
            }
            
            player.teleportTo(level, teleportPos.getX() + 0.5, teleportPos.getY(), teleportPos.getZ() + 0.5, 
                player.getYRot(), player.getXRot());
            
            context.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.teleport.misc.jumpto_success"), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.misc.jumpto_failed", e.getMessage()));
            return 0;
        }
    }
    
    private static int randomTeleport(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ServerLevel level = player.serverLevel();
            
            // Try to find a safe random location within reasonable bounds
            for (int attempts = 0; attempts < 50; attempts++) {
                int x = RANDOM.nextInt(20000) - 10000; // -10k to +10k
                int z = RANDOM.nextInt(20000) - 10000;
                int y = level.getHeight() - 1;
                
                // Find ground level
                for (; y > level.getMinBuildHeight(); y--) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    if (!level.getBlockState(checkPos).isAir() && 
                        level.getBlockState(checkPos.above()).isAir() && 
                        level.getBlockState(checkPos.above(2)).isAir()) {
                        
                        BlockPos teleportPos = checkPos.above();
                        player.teleportTo(level, teleportPos.getX() + 0.5, teleportPos.getY(), 
                            teleportPos.getZ() + 0.5, player.getYRot(), player.getXRot());
                        
                        context.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.teleport.misc.tpr_success", 
                            String.valueOf(teleportPos.getX()), String.valueOf(teleportPos.getY()), String.valueOf(teleportPos.getZ())), false);
                        return 1;
                    }
                }
            }
            
            context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.misc.tpr_no_safe_location"));
            return 0;
        } catch (Exception e) {
            context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.misc.tpr_failed", e.getMessage()));
            return 0;
        }
    }

    private static int teleportToOfflinePlayer(CommandContext<CommandSourceStack> context, String playerName) {
        try {
            ServerPlayer executor = context.getSource().getPlayerOrException();
            DirectTeleportManager manager = DirectTeleportManager.getInstance();
            boolean success = manager.teleportToOfflinePlayer(executor, playerName);
            return success ? 1 : 0;
        } catch (CommandSyntaxException e) {
            context.getSource().sendFailure(MessageUtil.error("Only players can use this command"));
            return 0;
        } catch (Exception e) {
            context.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.admin.tpo_failed", e.getMessage()));
            return 0;
        }
    }
}
