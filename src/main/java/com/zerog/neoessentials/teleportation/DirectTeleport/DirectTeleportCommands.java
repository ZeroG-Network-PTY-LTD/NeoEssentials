package com.zerog.neoessentials.teleportation.DirectTeleport;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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

public class DirectTeleportCommands {
    private static final String PERMISSION_TP     = "neoessentials.teleport.tp";
    private static final String PERMISSION_TPHERE = "neoessentials.teleport.tphere";
    private static final String PERMISSION_TPPOS  = "neoessentials.teleport.tppos";
    private static final String PERMISSION_TOP    = "neoessentials.teleport.top";
    private static final String PERMISSION_TPR    = "neoessentials.teleport.tpr";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ConfigManager config = ConfigManager.getInstance();

        if (config.isTeleportationEnabled() && config.isCommandEnabled("tp"))      registerTpCommand(dispatcher);
        if (config.isTeleportationEnabled() && config.isCommandEnabled("tphere"))  registerTphereCommand(dispatcher);
        if (config.isTeleportationEnabled() && config.isCommandEnabled("tpall"))   registerTpallCommand(dispatcher);
        if (config.isTeleportationEnabled() && config.isCommandEnabled("tppos"))   registerTpposCommand(dispatcher);
        if (config.isTeleportationEnabled() && config.isCommandEnabled("top"))     registerTopCommand(dispatcher);
        if (config.isTeleportationEnabled() && config.isCommandEnabled("jumpto"))  registerJumptoCommand(dispatcher);
        if (config.isTeleportationEnabled() && config.isCommandEnabled("jump"))    registerJumpCommand(dispatcher);
        if (config.isTeleportationEnabled() && config.isCommandEnabled("tpr"))     registerTprCommand(dispatcher);
        if (config.isTeleportationEnabled() && config.isCommandEnabled("tpo"))     registerTpoCommand(dispatcher);
        // NOTE: /back is in MiscTeleportCommands.java
    }

    // -----------------------------------------------------------------------
    // Registration helpers
    // -----------------------------------------------------------------------

    private static void registerTpCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tp")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer p) return PermissionAPI.hasPermission(p.getUUID(), PERMISSION_TP);
                return com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(source, 2);
            })
            .then(Commands.argument("target", EntityArgument.player())
                .executes(ctx -> teleportToPlayer(ctx, ctx.getSource().getPlayerOrException(),
                    EntityArgument.getPlayer(ctx, "target"))))
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> teleportToPlayer(ctx,
                        EntityArgument.getPlayer(ctx, "player"),
                        EntityArgument.getPlayer(ctx, "target")))))
            .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                    .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                        .executes(ctx -> teleportToCoordinates(ctx, ctx.getSource().getPlayerOrException(),
                            DoubleArgumentType.getDouble(ctx, "x"),
                            DoubleArgumentType.getDouble(ctx, "y"),
                            DoubleArgumentType.getDouble(ctx, "z"))))))
        );
    }

    private static void registerTphereCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tphere")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer p) return PermissionAPI.hasPermission(p.getUUID(), PERMISSION_TPHERE);
                return com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(source, 2);
            })
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> teleportPlayerHere(ctx, EntityArgument.getPlayer(ctx, "player"))))
        );
    }

    private static void registerTpallCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpall")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer p) return PermissionAPI.hasPermission(p.getUUID(), "neoessentials.teleport.admin.tpall");
                return com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(source, 2);
            })
            .executes(DirectTeleportCommands::teleportAllPlayers)
        );
    }

    private static void registerTpposCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tppos")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer p) return PermissionAPI.hasPermission(p.getUUID(), PERMISSION_TPPOS);
                return com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(source, 2);
            })
            .then(Commands.argument("coordinates", Vec3Argument.vec3())
                .executes(ctx -> {
                    try {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        Coordinates coords = Vec3Argument.getCoordinates(ctx, "coordinates");
                        Vec3 pos = coords.getPosition(ctx.getSource());
                        return teleportToCoordinates(ctx, player, pos.x, pos.y, pos.z);
                    } catch (CommandSyntaxException e) {
                        ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.admin.failed_coords", e.getMessage()));
                        return 0;
                    }
                }))
        );
    }

    private static void registerTopCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("top")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer p) return PermissionAPI.hasPermission(p.getUUID(), PERMISSION_TOP);
                return com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(source, 0);
            })
            .executes(DirectTeleportCommands::teleportToTop)
        );
    }

    private static void registerJumptoCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("jumpto")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer p) return PermissionAPI.hasPermission(p.getUUID(), "neoessentials.teleport.jumpto");
                return com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(source, 2);
            })
            .executes(DirectTeleportCommands::jumpToTargetBlock)
        );
    }

    private static void registerJumpCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("jump")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer p) return PermissionAPI.hasPermission(p.getUUID(), "neoessentials.teleport.jump");
                return com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(source, 2);
            })
            .executes(DirectTeleportCommands::jumpToTargetBlock)
        );
    }

    /**
     * Registers /tpr [locationName], its aliases (/rtp, /randomtp, /randomteleport),
     * and the admin /settpr <name> command.
     */
    private static void registerTprCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Helper: build a tpr literal with optional location argument
        for (String alias : new String[]{"tpr", "rtp", "randomtp", "randomteleport"}) {
            dispatcher.register(Commands.literal(alias)
                .requires(source -> {
                    if (source.getEntity() instanceof ServerPlayer p) return PermissionAPI.hasPermission(p.getUUID(), PERMISSION_TPR);
                    return com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(source, 0);
                })
                .executes(ctx -> randomTeleport(ctx, ""))
                .then(Commands.argument("locationName", StringArgumentType.word())
                    .executes(ctx -> randomTeleport(ctx, StringArgumentType.getString(ctx, "locationName"))))
            );
        }

        // /neoe tpr|rtp|randomtp|randomteleport sub-commands
        for (String root : new String[]{"neoe", "neoessentials"}) {
            dispatcher.register(Commands.literal(root)
                .then(Commands.literal("tpr").executes(ctx -> randomTeleport(ctx, "")))
                .then(Commands.literal("rtp").executes(ctx -> randomTeleport(ctx, "")))
                .then(Commands.literal("randomtp").executes(ctx -> randomTeleport(ctx, "")))
                .then(Commands.literal("randomteleport").executes(ctx -> randomTeleport(ctx, "")))
            );
        }

        // /settpr <locationName> — set the RTP centre for a named slot
        dispatcher.register(Commands.literal("settpr")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer p) return PermissionAPI.hasPermission(p.getUUID(), "neoessentials.teleport.settpr");
                return com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(source, 2);
            })
            .then(Commands.argument("locationName", StringArgumentType.word())
                .executes(DirectTeleportCommands::setTprLocation))
        );
    }

    private static void registerTpoCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpo")
            .requires(source -> {
                if (source.getEntity() instanceof ServerPlayer p) return PermissionAPI.hasPermission(p.getUUID(), "neoessentials.teleport.admin.tpo");
                return com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(source, 2);
            })
            .then(Commands.argument("player", StringArgumentType.word())
                .executes(ctx -> teleportToOfflinePlayer(ctx, StringArgumentType.getString(ctx, "player"))))
        );
    }

    // -----------------------------------------------------------------------
    // Command implementations
    // -----------------------------------------------------------------------

    private static int teleportToPlayer(CommandContext<CommandSourceStack> ctx, ServerPlayer player, ServerPlayer target) {
        try {
            if (player == target) {
                ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.admin.self"));
                return 0;
            }
            // Save back location BEFORE teleporting so /back works
            com.zerog.neoessentials.teleportation.Misc.MiscTeleportManager.getInstance().saveBackLocation(player);
            player.teleportTo(
                com.zerog.neoessentials.util.LevelCompat.of(target),
                target.getX(),
                target.getY(),
                target.getZ(),
                java.util.Set.of(),
                target.getYRot(),
                target.getXRot(),
                true
            );
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.teleport.admin.teleported_player",
                player.getName().getString(), target.getName().getString()), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.admin.failed", e.getMessage()));
            return 0;
        }
    }

    private static int teleportToCoordinates(CommandContext<CommandSourceStack> ctx, ServerPlayer player, double x, double y, double z) {
        try {
            // Save back location BEFORE teleporting so /back works
            com.zerog.neoessentials.teleportation.Misc.MiscTeleportManager.getInstance().saveBackLocation(player);
            player.teleportTo(
                com.zerog.neoessentials.util.LevelCompat.of(player),
                x,
                y,
                z,
                java.util.Set.of(),
                player.getYRot(),
                player.getXRot(),
                true
            );
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.teleport.admin.teleported_player_coords",
                player.getName().getString(), String.valueOf((int) x), String.valueOf((int) y), String.valueOf((int) z)), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.admin.failed_coords", e.getMessage()));
            return 0;
        }
    }

    private static int teleportPlayerHere(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            // Save back location BEFORE teleporting so /back works (for the player being moved)
            com.zerog.neoessentials.teleportation.Misc.MiscTeleportManager.getInstance().saveBackLocation(target);
            target.teleportTo(
                com.zerog.neoessentials.util.LevelCompat.of(player),
                player.getX(),
                player.getY(),
                player.getZ(),
                java.util.Set.of(),
                target.getYRot(),
                target.getXRot(),
                true
            );
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.teleport.admin.teleported_to",
                target.getName().getString()), true);
            target.sendSystemMessage(MessageUtil.info("commands.neoessentials.teleport.admin.player_teleported_to_you",
                player.getName().getString()));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.admin.failed", e.getMessage()));
            return 0;
        }
    }

    private static int teleportAllPlayers(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            net.minecraft.server.MinecraftServer server = player.level().getServer();
            if (server == null) {
                ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.admin.failed", "Server not available"));
                return 0;
            }
            Collection<ServerPlayer> players = server.getPlayerList().getPlayers();
            int count = 0;
            for (ServerPlayer target : players) {
                if (target != player) {
                    // Save back location BEFORE teleporting so /back works for each player
                    com.zerog.neoessentials.teleportation.Misc.MiscTeleportManager.getInstance().saveBackLocation(target);
                    target.teleportTo(
                com.zerog.neoessentials.util.LevelCompat.of(player),
                player.getX(),
                player.getY(),
                player.getZ(),
                java.util.Set.of(),
                target.getYRot(),
                target.getXRot(),
                true
            );
                    count++;
                }
            }
            if (count == 0) {
                ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.admin.tpall.no_players"));
                return 0;
            }
            final int finalCount = count;
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.teleport.admin.tpall.teleported",
                String.valueOf(finalCount), player.getName().getString()), true);
            return count;
        } catch (Exception e) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.admin.failed", e.getMessage()));
            return 0;
        }
    }

    private static int teleportToTop(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            BlockPos currentPos = player.blockPosition();
            ServerLevel level = com.zerog.neoessentials.util.LevelCompat.of(player);
            BlockPos highestPos = null;
            for (int y = com.zerog.neoessentials.util.LevelHeightCompat.maxBuildHeight(level) - 1; y > currentPos.getY(); y--) {
                BlockPos checkPos = new BlockPos(currentPos.getX(), y, currentPos.getZ());
                if (!level.getBlockState(checkPos).isAir() && level.getBlockState(checkPos.above()).isAir()) {
                    highestPos = checkPos.above();
                    break;
                }
            }
            if (highestPos == null) {
                ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.misc.no_solid_block"));
                return 0;
            }
            // Save back location BEFORE teleporting so /back works
            com.zerog.neoessentials.teleportation.Misc.MiscTeleportManager.getInstance().saveBackLocation(player);
            player.teleportTo(
                level,
                highestPos.getX() + 0.5,
                highestPos.getY(),
                highestPos.getZ() + 0.5,
                java.util.Set.of(),
                player.getYRot(),
                player.getXRot(),
                true
            );
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.teleport.misc.top_success"), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.misc.top_failed", e.getMessage()));
            return 0;
        }
    }

    private static int jumpToTargetBlock(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerLevel level = com.zerog.neoessentials.util.LevelCompat.of(player);
            Vec3 start = player.getEyePosition();
            Vec3 end = start.add(player.getLookAngle().scale(100));
            BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.MISS) {
                ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.misc.no_block_in_sight"));
                return 0;
            }
            BlockPos teleportPos = hit.getBlockPos().above();
            if (!level.getBlockState(teleportPos).isAir() || !level.getBlockState(teleportPos.above()).isAir()) {
                ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.misc.jumpto_failed", "Target location unsafe"));
                return 0;
            }
            // Save back location BEFORE teleporting so /back works
            com.zerog.neoessentials.teleportation.Misc.MiscTeleportManager.getInstance().saveBackLocation(player);
            player.teleportTo(
                level,
                teleportPos.getX() + 0.5,
                teleportPos.getY(),
                teleportPos.getZ() + 0.5,
                java.util.Set.of(),
                player.getYRot(),
                player.getXRot(),
                true
            );
            ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.teleport.misc.jumpto_success"), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.misc.jumpto_failed", e.getMessage()));
            return 0;
        }
    }

    /**
     * /tpr [locationName] — delegates to RandomTeleportManager (Essentials-style RTP).
     */
    private static int randomTeleport(CommandContext<CommandSourceStack> ctx, String locationName) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            RandomTeleportManager.getInstance().randomTeleport(player, locationName);
            return 1;
        } catch (CommandSyntaxException e) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.misc.tpr_failed", e.getMessage()));
            return 0;
        } catch (Exception e) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.misc.tpr_failed", e.getMessage()));
            return 0;
        }
    }

    /**
     * /settpr <locationName> — saves player's current position as the RTP centre for that slot.
     */
    private static int setTprLocation(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            String name = StringArgumentType.getString(ctx, "locationName");

            com.google.gson.JsonObject config = ConfigManager.getInstance().getConfig(ConfigManager.MAIN_CONFIG);
            com.google.gson.JsonObject teleportation = config.has("teleportation")
                    ? config.getAsJsonObject("teleportation") : new com.google.gson.JsonObject();
            com.google.gson.JsonObject tprSettings = teleportation.has("randomTeleportSettings")
                    ? teleportation.getAsJsonObject("randomTeleportSettings") : new com.google.gson.JsonObject();
            com.google.gson.JsonObject locations = tprSettings.has("locations")
                    ? tprSettings.getAsJsonObject("locations") : new com.google.gson.JsonObject();
            com.google.gson.JsonObject locEntry = locations.has(name)
                    ? locations.getAsJsonObject(name) : new com.google.gson.JsonObject();

            com.google.gson.JsonObject center = new com.google.gson.JsonObject();
            center.addProperty("x", player.getX());
            center.addProperty("y", player.getY());
            center.addProperty("z", player.getZ());
            center.addProperty("world", player.level().dimension().identifier().toString());
            locEntry.add("center", center);
            if (!locEntry.has("minRange")) locEntry.addProperty("minRange", 0);
            if (!locEntry.has("maxRange")) locEntry.addProperty("maxRange", 10000);

            locations.add(name, locEntry);
            tprSettings.add("locations", locations);
            teleportation.add("randomTeleportSettings", tprSettings);
            config.add("teleportation", teleportation);
            ConfigManager.getInstance().saveConfig(ConfigManager.MAIN_CONFIG, config);

            RandomTeleportManager.getInstance().clearCache(name);

            ctx.getSource().sendSuccess(() -> MessageUtil.success(
                    "commands.neoessentials.teleport.misc.settpr_success", name,
                    String.format("%.1f", player.getX()),
                    String.format("%.1f", player.getY()),
                    String.format("%.1f", player.getZ())), true);
            return 1;
        } catch (CommandSyntaxException e) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.player_only"));
            return 0;
        } catch (Exception e) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.misc.settpr_failed", e.getMessage()));
            return 0;
        }
    }

    private static int teleportToOfflinePlayer(CommandContext<CommandSourceStack> ctx, String playerName) {
        try {
            ServerPlayer executor = ctx.getSource().getPlayerOrException();
            boolean success = DirectTeleportManager.getInstance().teleportToOfflinePlayer(executor, playerName);
            return success ? 1 : 0;
        } catch (CommandSyntaxException e) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.player_only"));
            return 0;
        } catch (Exception e) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.teleport.admin.tpo_failed", e.getMessage()));
            return 0;
        }
    }
}
