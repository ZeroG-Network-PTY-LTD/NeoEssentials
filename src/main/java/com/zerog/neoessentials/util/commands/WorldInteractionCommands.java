package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.teleportation.Misc.MiscTeleportManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * World interaction & fun commands ported from EssentialsX:
 * /fireball [type] [speed] [ride]  — shoot a projectile (Commandfireball)
 * /tree <type>                     — grow a tree at look target (Commandtree)
 * /bigtree                         — grow a large tree at look target (Commandbigtree)
 * /break                           — break the looked-at block instantly (Commandbreak)
 * /ice [player]                    — freeze a player solid (Commandice)
 * /bottom                          — teleport to bottom of world at your XZ (Commandbottom)
 *  /tpaall                          — send tpa-here request to all online players (Commandtpaall)
 *  /broadcastworld <msg>            — broadcast to players in your world (Commandbroadcastworld)
 */
public class WorldInteractionCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldInteractionCommands.class);

    /** Projectile type names → NeoForge EntityType */
    private static final List<String> PROJECTILE_TYPES = Arrays.asList(
        "fireball", "small", "large", "arrow", "skull", "egg",
        "snowball", "expbottle", "dragon", "trident", "windcharge"
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerFireball(dispatcher);
        registerTree(dispatcher);
        registerBreak(dispatcher);
        registerIce(dispatcher);
        registerBottom(dispatcher);
        registerTpaAll(dispatcher);
        registerBroadcastWorld(dispatcher);
    }

    // ── /fireball [type] [speed] [ride] ───────────────────────────────────────
    // Essentials: Commandfireball — shoots a projectile in the player's look direction.
    private static void registerFireball(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("fireball")
            .requires(src -> src.getPlayer() == null
                || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.fireball"))
            .executes(ctx -> executeFireball(ctx, "fireball", 2.0, false))
            .then(Commands.argument("type", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(PROJECTILE_TYPES, b))
                .executes(ctx -> executeFireball(ctx, StringArgumentType.getString(ctx, "type"), 2.0, false))
                .then(Commands.argument("speed", DoubleArgumentType.doubleArg(0, 10))
                    .executes(ctx -> executeFireball(ctx,
                        StringArgumentType.getString(ctx, "type"),
                        DoubleArgumentType.getDouble(ctx, "speed"), false))
                    .then(Commands.literal("ride")
                        .requires(src -> src.getPlayer() == null
                            || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.fireball.ride"))
                        .executes(ctx -> executeFireball(ctx,
                            StringArgumentType.getString(ctx, "type"),
                            DoubleArgumentType.getDouble(ctx, "speed"), true))
                    )
                )
            )
        );
    }

    private static int executeFireball(CommandContext<CommandSourceStack> ctx, String type, double speed, boolean ride) {
        var src = ctx.getSource();
        var player = src.getPlayer();
        if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }

        // Per-type permission
        if (!PermissionAPI.hasPermission(player.getUUID(), "neoessentials.fireball." + type)
                && !PermissionAPI.hasPermission(player.getUUID(), "neoessentials.fireball.*")) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.fireball.no_perm_type", type));
            return 0;
        }

        var level = player.serverLevel();
        Vec3 eyePos = player.getEyePosition();
        Vec3 dir = player.getLookAngle().scale(speed);
        Vec3 spawnPos = eyePos.add(dir);

        net.minecraft.world.entity.Entity projectile = switch (type.toLowerCase()) {
            case "small"      -> {
                var fb = new SmallFireball(level, player, dir);
                fb.moveTo(spawnPos.x, spawnPos.y, spawnPos.z);
                yield fb;
            }
            case "large"      -> {
                var fb = new LargeFireball(level, player, dir, 1);
                fb.moveTo(spawnPos.x, spawnPos.y, spawnPos.z);
                yield fb;
            }
            case "skull"      -> {
                var sk = new WitherSkull(level, player, dir);
                sk.moveTo(spawnPos.x, spawnPos.y, spawnPos.z);
                yield sk;
            }
            case "arrow"      -> {
                var ar = new Arrow(net.minecraft.world.entity.EntityType.ARROW, level);
                ar.setOwner(player);
                ar.moveTo(spawnPos.x, spawnPos.y, spawnPos.z);
                ar.setDeltaMovement(dir);
                yield ar;
            }
            case "egg"        -> {
                var eg = new ThrownEgg(level, player);
                eg.moveTo(spawnPos.x, spawnPos.y, spawnPos.z);
                eg.setDeltaMovement(dir);
                yield eg;
            }
            case "snowball"   -> {
                var sb = new Snowball(level, player);
                sb.moveTo(spawnPos.x, spawnPos.y, spawnPos.z);
                sb.setDeltaMovement(dir);
                yield sb;
            }
            case "expbottle"  -> {
                var eb = new ThrownExperienceBottle(level, player);
                eb.moveTo(spawnPos.x, spawnPos.y, spawnPos.z);
                eb.setDeltaMovement(dir);
                yield eb;
            }
            case "dragon"     -> {
                var df = new DragonFireball(level, player, dir);
                df.moveTo(spawnPos.x, spawnPos.y, spawnPos.z);
                yield df;
            }
            case "windcharge" -> {
                var wc = net.minecraft.world.entity.EntityType.WIND_CHARGE.create(level);
                if (wc != null) {
                    wc.setOwner(player);
                    wc.moveTo(spawnPos.x, spawnPos.y, spawnPos.z);
                    wc.setDeltaMovement(dir);
                }
                yield wc != null ? wc : new LargeFireball(level, player, dir, 1);
            }
            default           -> {
                // "fireball" and fallback
                var fb = new LargeFireball(level, player, dir, 1);
                fb.moveTo(spawnPos.x, spawnPos.y, spawnPos.z);
                yield fb;
            }
        };

        level.addFreshEntity(projectile);

        if (ride) {
            player.startRiding(projectile, true);
        }

        final String ft = type;
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.fireball.shot", ft), false);
        return 1;
    }

    // ── /tree <type> / /bigtree ───────────────────────────────────────────────
    // Essentials: Commandtree — grow a tree at the looked-at block.
    private static final List<String> TREE_TYPES = Arrays.asList(
        "oak", "birch", "spruce", "jungle", "acacia", "darkoak",
        "mangrove", "cherry", "azalea", "bigoak", "mega_spruce", "mega_jungle"
    );

    private static void registerTree(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("tree")
            .requires(src -> src.getPlayer() == null
                || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.tree"))
            .then(Commands.argument("type", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(TREE_TYPES, b))
                .executes(ctx -> executeTree(ctx, StringArgumentType.getString(ctx, "type")))
            )
        );
        // /bigtree alias → grows a large oak
        d.register(Commands.literal("bigtree")
            .requires(src -> src.getPlayer() == null
                || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.tree"))
            .executes(ctx -> executeTree(ctx, "bigoak"))
        );
    }

    private static int executeTree(CommandContext<CommandSourceStack> ctx, String typeName) {
        var src = ctx.getSource();
        var player = src.getPlayer();
        if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }

        var level = player.serverLevel();
        // Raycast to find target block, then plant one block above
        var hit = player.pick(20, 1.0f, false);
        BlockPos target = BlockPos.containing(hit.getLocation()).above();

        // Map name → ConfiguredFeature placement via world generator
        // NeoForge 1.21.1: use level.registryAccess() to find the tree feature
        var treeFeatureKey = resolveTreeFeatureKey(typeName);
        if (treeFeatureKey == null) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.tree.unknown", typeName));
            return 0;
        }

        // Place tree using WorldgenFeature
        boolean placed = tryPlaceTree(level, target, treeFeatureKey);
        if (!placed) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.tree.failed"));
            return 0;
        }

        final String fn = typeName;
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.tree.spawned", fn), false);
        return 1;
    }

    private static net.minecraft.resources.ResourceLocation resolveTreeFeatureKey(String name) {
        return switch (name.toLowerCase()) {
            case "oak"          -> net.minecraft.resources.ResourceLocation.withDefaultNamespace("oak");
            case "birch"        -> net.minecraft.resources.ResourceLocation.withDefaultNamespace("birch");
            case "spruce"       -> net.minecraft.resources.ResourceLocation.withDefaultNamespace("spruce");
            case "jungle"       -> net.minecraft.resources.ResourceLocation.withDefaultNamespace("jungle_tree");
            case "acacia"       -> net.minecraft.resources.ResourceLocation.withDefaultNamespace("acacia");
            case "darkoak", "dark_oak" -> net.minecraft.resources.ResourceLocation.withDefaultNamespace("dark_oak");
            case "mangrove"     -> net.minecraft.resources.ResourceLocation.withDefaultNamespace("mangrove");
            case "cherry"       -> net.minecraft.resources.ResourceLocation.withDefaultNamespace("cherry");
            case "bigoak", "big_oak" -> net.minecraft.resources.ResourceLocation.withDefaultNamespace("fancy_oak");
            case "mega_spruce"  -> net.minecraft.resources.ResourceLocation.withDefaultNamespace("mega_spruce");
            case "mega_jungle"  -> net.minecraft.resources.ResourceLocation.withDefaultNamespace("mega_jungle_tree");
            case "azalea"       -> net.minecraft.resources.ResourceLocation.withDefaultNamespace("azalea_tree");
            default             -> null;
        };
    }

    private static boolean tryPlaceTree(ServerLevel level, BlockPos pos, net.minecraft.resources.ResourceLocation featureKey) {
        try {
            var registry = level.registryAccess().registry(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE);
            if (registry.isEmpty()) return false;
            var holder = registry.get().get(featureKey);
            if (holder == null) return false;
            return holder.place(level, level.getChunkSource().getGenerator(), level.getRandom(), pos);
        } catch (Exception e) {
            LOGGER.warn("Failed to place tree '{}': {}", featureKey, e.getMessage());
            return false;
        }
    }

    // ── /break ────────────────────────────────────────────────────────────────
    // Essentials: Commandbreak — instantly break the looked-at block.
    private static void registerBreak(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("break")
            .requires(src -> src.getPlayer() == null
                || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.break"))
            .executes(ctx -> {
                var src = ctx.getSource();
                var player = src.getPlayer();
                if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }

                var hit = player.pick(20, 1.0f, false);
                if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
                    src.sendFailure(MessageUtil.error("commands.neoessentials.break.nothing"));
                    return 0;
                }
                BlockPos bpos = BlockPos.containing(hit.getLocation());
                @SuppressWarnings("resource") // ServerLevel is not AutoCloseable
                var level = player.serverLevel();
                BlockState state = level.getBlockState(bpos);

                if (state.isAir()) {
                    src.sendFailure(MessageUtil.error("commands.neoessentials.break.nothing"));
                    return 0;
                }
                // Bedrock protection
                if (state.is(Blocks.BEDROCK)
                        && !PermissionAPI.hasPermission(player.getUUID(), "neoessentials.break.bedrock")) {
                    src.sendFailure(MessageUtil.error("commands.neoessentials.break.bedrock"));
                    return 0;
                }

                // Destroy the block (no drops to keep it clean like Essentials)
                level.destroyBlock(bpos, false, player);
                src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.break.broken"), false);
                return 1;
            })
        );
    }

    // ── /ice [player] ─────────────────────────────────────────────────────────
    // Essentials: Commandice — freeze a player solid (setFreezeTicks to max).
    private static void registerIce(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("ice")
            .requires(src -> src.getPlayer() == null
                || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.ice"))
            // /ice — freeze self
            .executes(ctx -> executeIce(ctx, null))
            // /ice <player>
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                .requires(src -> src.getPlayer() == null
                    || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.ice.others"))
                .executes(ctx -> executeIce(ctx, StringArgumentType.getString(ctx, "target")))
            )
        );
    }

    private static int executeIce(CommandContext<CommandSourceStack> ctx, String targetName) {
        var src = ctx.getSource();
        ServerPlayer target;
        if (targetName != null) {
            target = src.getServer().getPlayerList().getPlayerByName(targetName);
            if (target == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", targetName)); return 0; }
        } else {
            target = src.getPlayer();
            if (target == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }
        }
        target.setTicksFrozen(target.getTicksRequiredToFreeze() + 1);
        if (targetName != null) {
            target.sendSystemMessage(MessageUtil.info("commands.neoessentials.ice.frozen"));
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.ice.frozen_other", target.getName().getString()), true);
        } else {
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.ice.frozen"), false);
        }
        return 1;
    }

    // ── /bottom ───────────────────────────────────────────────────────────────
    // Essentials: Commandbottom — teleport to the bottom of the world at current XZ.
    private static void registerBottom(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("bottom")
            .requires(src -> src.getPlayer() == null
                || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.bottom"))
            .executes(ctx -> {
                var src = ctx.getSource();
                var player = src.getPlayer();
                if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }

                var level = player.serverLevel();
                int x = player.getBlockX();
                int z = player.getBlockZ();
                int minY = level.getMinBuildHeight();

                // Find the lowest safe Y (solid block + air above it)
                BlockPos safePos = null;
                for (int y = minY; y < player.getBlockY(); y++) {
                    BlockPos check = new BlockPos(x, y, z);
                    BlockPos above = check.above();
                    if (!level.getBlockState(check).isAir()
                            && level.getBlockState(above).isAir()
                            && level.getBlockState(above.above()).isAir()) {
                        safePos = above;
                    }
                }

                if (safePos == null) {
                    src.sendFailure(MessageUtil.error("commands.neoessentials.bottom.no_safe"));
                    return 0;
                }

                MiscTeleportManager.getInstance().saveBackLocation(player);
                final BlockPos finalPos = safePos;
                player.teleportTo(level, finalPos.getX() + 0.5, finalPos.getY(), finalPos.getZ() + 0.5,
                    player.getYRot(), player.getXRot());
                src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.bottom.teleported",
                    level.dimension().location().getPath(), finalPos.getX(), finalPos.getY(), finalPos.getZ()), false);
                return 1;
            })
        );
    }

    // ── /tpaall ───────────────────────────────────────────────────────────────
    // Essentials: Commandtpaall — send a tpa-here request to every online player.
    private static void registerTpaAll(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("tpaall")
            .requires(src -> src.getPlayer() == null
                || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.tpaall"))
            .executes(ctx -> executeTpaAll(ctx, null))
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                .requires(src -> src.getPlayer() == null
                    || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.tpaall.others"))
                .executes(ctx -> executeTpaAll(ctx, StringArgumentType.getString(ctx, "target")))
            )
        );
    }

    private static int executeTpaAll(CommandContext<CommandSourceStack> ctx, String targetName) {
        var src = ctx.getSource();
        // The player who everyone should tp to
        ServerPlayer hub = targetName != null
            ? src.getServer().getPlayerList().getPlayerByName(targetName)
            : src.getPlayer();
        if (hub == null) {
            if (targetName != null) src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", targetName));
            else src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only"));
            return 0;
        }

        int sent = 0;
        var manager = com.zerog.neoessentials.teleportation.TeleportRequests.TeleportRequestManager.getInstance();
        for (ServerPlayer online : src.getServer().getPlayerList().getPlayers()) {
            if (online.getUUID().equals(hub.getUUID())) continue;
            // Respect tptoggle
            if (!com.zerog.neoessentials.util.commands.ItemCustomisationCommands.isTpToggleAllowed(online.getUUID())) continue;
            boolean success = manager.sendTeleportRequest(hub, online,
                com.zerog.neoessentials.teleportation.TeleportRequests.TeleportRequestType.TPAHERE);
            if (success) sent++;
        }

        final int fs = sent;
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.tpaall.sent", fs), true);
        LOGGER.info("{} sent tpaall, {} requests sent", src.getTextName(), sent);
        return sent > 0 ? 1 : 0;
    }

    // ── /broadcastworld <message> ─────────────────────────────────────────────
    // Essentials: Commandbroadcastworld — broadcast to all players in the sender's world.
    private static void registerBroadcastWorld(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("broadcastworld")
            .requires(src -> src.getPlayer() == null
                || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.broadcastworld"))
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(ctx -> {
                    var src = ctx.getSource();
                    String msg = StringArgumentType.getString(ctx, "message");
                    ServerLevel targetLevel = src.getLevel();
                    Component broadcast = MessageUtil.coloredText("§6[World] §e" + msg);
                    int count = 0;
                    for (ServerPlayer p : src.getServer().getPlayerList().getPlayers()) {
                        @SuppressWarnings("resource") // ServerLevel is not AutoCloseable
                        boolean sameLevel = p.serverLevel() == targetLevel;
                        if (sameLevel) {
                            p.sendSystemMessage(broadcast);
                            count++;
                        }
                    }
                    final int fc = count;
                    src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.broadcastworld.sent",
                        targetLevel.dimension().location().getPath(), fc), false);
                    LOGGER.info("[BroadcastWorld:{}] {}", targetLevel.dimension().location().getPath(), msg);
                    return 1;
                })
            )
        );
        // aliases
        d.register(Commands.literal("bcastworld")
            .requires(src -> src.getPlayer() == null
                || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.broadcastworld"))
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(ctx -> {
                    var src = ctx.getSource();
                    src.getServer().getCommands().performPrefixedCommand(src,
                        "broadcastworld " + StringArgumentType.getString(ctx, "message"));
                    return 1;
                })
            )
        );
    }
}

