package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility commands ported from EssentialsX:
 * /ptime [reset|day|night|<ticks>] [player]  — per-player client-side time override
 * /pweather [reset|sun|storm] [player]         — per-player client-side weather override
 * /effect <player> <effect> [duration] [amp]   — apply potion effects
 * /spawnmob <mob> [amount] [player]             — spawn entities
 * /unlimited [list|clear|<item>] [player]       — infinite item use
 * /condense [item]                              — condense items to storage blocks
 */
public class UtilityCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(UtilityCommands.class);

    // Per-player time: UUID → fixed ticks (-1 = reset)
    private static final Map<UUID, Long> playerTimes = new ConcurrentHashMap<>();
    // Per-player weather: UUID → "sun"|"storm"|null(reset)
    private static final Map<UUID, String> playerWeather = new ConcurrentHashMap<>();
    // Unlimited items: UUID → Set of item registry IDs
    private static final Map<UUID, Set<String>> unlimitedItems = new ConcurrentHashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerPtime(dispatcher);
        registerPweather(dispatcher);
        registerEffectCmd(dispatcher);
        registerSpawnMob(dispatcher);
        registerUnlimited(dispatcher);
        registerCondense(dispatcher);
    }

    // ── /ptime [reset|<value>] [player] ──────────────────────────────────────
    // Essentials: sets a per-player time offset. "reset" removes it.
    // NeoForge: ClientboundSetTimePacket with lockTime=true
    private static void registerPtime(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("ptime")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.ptime"); })
            .executes(UtilityCommands::executePtimeGet)
            // /ptime reset [player]
            .then(Commands.literal("reset")
                .executes(ctx -> executePtimeSet(ctx, -1L, null))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                    .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.ptime.others"))
                    .executes(ctx -> executePtimeSet(ctx, -1L, StringArgumentType.getString(ctx, "target"))))
            )
            // /ptime <value> [player]
            .then(Commands.argument("time", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    List.of("reset","day","noon","night","midnight","sunrise","0","1000","6000","12000","18000"), b))
                .executes(ctx -> executePtimeSet(ctx, parseTimeTicks(StringArgumentType.getString(ctx, "time")), null))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                    .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.ptime.others"))
                    .executes(ctx -> executePtimeSet(ctx,
                        parseTimeTicks(StringArgumentType.getString(ctx, "time")),
                        StringArgumentType.getString(ctx, "target"))))
            )
        );
    }

    private static int executePtimeGet(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        ServerPlayer target = resolveTarget(src, null);
        if (target == null) return 0;
        Long t = playerTimes.get(target.getUUID());
        if (t == null || t < 0) {
            src.sendSuccess(() -> MessageUtil.info("commands.neoessentials.ptime.none", target.getName().getString()), false);
        } else {
            final long ft = t;
            src.sendSuccess(() -> MessageUtil.info("commands.neoessentials.ptime.current", target.getName().getString(), ft), false);
        }
        return 1;
    }

    @SuppressWarnings("resource") // ServerLevel does not implement AutoCloseable — IDE false positive
    private static int executePtimeSet(CommandContext<CommandSourceStack> ctx, long ticks, String targetName) {
        var src = ctx.getSource();
        ServerPlayer target = resolveTarget(src, targetName);
        if (target == null) return 0;

        if (ticks < 0) {
            playerTimes.remove(target.getUUID());
            // Reset to real world time
            sendTimePacket(target, com.zerog.neoessentials.util.WorldClockCompat.getTime(com.zerog.neoessentials.util.LevelCompat.of(target)), false);
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.ptime.reset", target.getName().getString()), false);
        } else {
            playerTimes.put(target.getUUID(), ticks);
            sendTimePacket(target, ticks, true);
            final long ft = ticks;
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.ptime.set", target.getName().getString(), ft), false);
        }
        return 1;
    }

    @SuppressWarnings("resource") // ServerLevel does not implement AutoCloseable — IDE false positive
    private static void sendTimePacket(ServerPlayer player, long ticks, boolean lock) {
        var level = com.zerog.neoessentials.util.LevelCompat.of(player);
        level.dimensionType().defaultClock().ifPresent(clock -> {
            var state = new net.minecraft.world.clock.ClockNetworkState(ticks, 0f, lock ? 0f : 1f);
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTimePacket(
                level.getGameTime(), java.util.Map.of(clock, state)));
        });
    }

    // ── /pweather [reset|sun|storm] [player] ─────────────────────────────────
    private static void registerPweather(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("pweather")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.pweather"); })
            .executes(UtilityCommands::executePweatherGet)
            .then(Commands.literal("reset")
                .executes(ctx -> executePweatherSet(ctx, null, null))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                    .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.pweather.others"))
                    .executes(ctx -> executePweatherSet(ctx, null, StringArgumentType.getString(ctx, "target"))))
            )
            .then(Commands.literal("sun")
                .executes(ctx -> executePweatherSet(ctx, "sun", null))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                    .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.pweather.others"))
                    .executes(ctx -> executePweatherSet(ctx, "sun", StringArgumentType.getString(ctx, "target"))))
            )
            .then(Commands.literal("clear")
                .executes(ctx -> executePweatherSet(ctx, "sun", null))
            )
            .then(Commands.literal("storm")
                .executes(ctx -> executePweatherSet(ctx, "storm", null))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                    .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.pweather.others"))
                    .executes(ctx -> executePweatherSet(ctx, "storm", StringArgumentType.getString(ctx, "target"))))
            )
            .then(Commands.literal("rain")
                .executes(ctx -> executePweatherSet(ctx, "storm", null))
            )
        );
    }

    private static int executePweatherGet(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        ServerPlayer target = resolveTarget(src, null);
        if (target == null) return 0;
        String w = playerWeather.get(target.getUUID());
        String label = w == null ? "server default" : w;
        src.sendSuccess(() -> MessageUtil.info("commands.neoessentials.pweather.current", target.getName().getString(), label), false);
        return 1;
    }

    @SuppressWarnings("resource") // ServerLevel does not implement AutoCloseable — IDE false positive
    private static int executePweatherSet(CommandContext<CommandSourceStack> ctx, String type, String targetName) {
        var src = ctx.getSource();
        ServerPlayer target = resolveTarget(src, targetName);
        if (target == null) return 0;

        if (type == null) {
            playerWeather.remove(target.getUUID());
            sendWeatherPacket(target, com.zerog.neoessentials.util.LevelCompat.of(target).isRaining());
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.pweather.reset", target.getName().getString()), false);
        } else {
            playerWeather.put(target.getUUID(), type);
            sendWeatherPacket(target, "storm".equals(type));
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.pweather.set", target.getName().getString(), type), false);
        }
        return 1;
    }

    private static void sendWeatherPacket(ServerPlayer player, boolean raining) {
        // NeoForge 1.21.1: ClientboundGameEventPacket with RAIN_LEVEL_CHANGE / THUNDER_LEVEL_CHANGE
        float level = raining ? 1.0f : 0.0f;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(
            net.minecraft.network.protocol.game.ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, level));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(
            net.minecraft.network.protocol.game.ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, level));
    }

    // ── Dashboard-facing wrappers ─────────────────────────────────────────────
    // Same state changes as /ptime and /pweather, just driven by a target ServerPlayer
    // directly instead of a CommandSourceStack.

    /** Current ptime override for {@code uuid}, or null if using real world time. */
    public static Long getPtime(UUID uuid) {
        Long t = playerTimes.get(uuid);
        return (t != null && t >= 0) ? t : null;
    }

    /** Sets (or, if {@code ticks} is null, resets) {@code target}'s ptime override. */
    public static void setPtime(ServerPlayer target, Long ticks) {
        if (ticks == null || ticks < 0) {
            playerTimes.remove(target.getUUID());
            sendTimePacket(target, com.zerog.neoessentials.util.WorldClockCompat.getTime(com.zerog.neoessentials.util.LevelCompat.of(target)), false);
        } else {
            playerTimes.put(target.getUUID(), ticks);
            sendTimePacket(target, ticks, true);
        }
    }

    /** Current pweather override for {@code uuid} ("sun"/"storm"), or null if using server weather. */
    public static String getPweather(UUID uuid) {
        return playerWeather.get(uuid);
    }

    /** Sets (or, if {@code type} is null, resets) {@code target}'s pweather override. */
    public static void setPweather(ServerPlayer target, String type) {
        if (type == null) {
            playerWeather.remove(target.getUUID());
            sendWeatherPacket(target, com.zerog.neoessentials.util.LevelCompat.of(target).isRaining());
        } else {
            playerWeather.put(target.getUUID(), type);
            sendWeatherPacket(target, "storm".equals(type));
        }
    }

    /** Called on player join — restore their ptime/pweather. */
    public static void onPlayerJoin(ServerPlayer player) {
        Long t = playerTimes.get(player.getUUID());
        if (t != null && t >= 0) sendTimePacket(player, t, true);
        String w = playerWeather.get(player.getUUID());
        if (w != null) sendWeatherPacket(player, "storm".equals(w));
    }

    /** Called on player quit — clean up in-memory only (we persist nothing for now). */
    public static void onPlayerQuit(UUID uuid) {
        playerTimes.remove(uuid);
        playerWeather.remove(uuid);
        unlimitedItems.remove(uuid);
    }

    // ── /effect <player> <effect|clear> [dur] [amp] ───────────────────────────
    // Essentials: Commandpotion applies effects to held potion. We extend this to
    // applying effects directly to players (more useful in NeoForge context).
    private static void registerEffectCmd(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("effect")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.effect"); })
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                // /effect <player> clear
                .then(Commands.literal("clear")
                    .executes(ctx -> executeEffectClear(ctx, StringArgumentType.getString(ctx, "target")))
                )
                // /effect <player> <effect> [duration] [amplifier]
                .then(Commands.argument("effect", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                        BuiltInRegistries.MOB_EFFECT.keySet().stream()
                            .map(Identifier::getPath).toList(), b))
                    .executes(ctx -> executeEffectApply(ctx,
                        StringArgumentType.getString(ctx, "target"),
                        StringArgumentType.getString(ctx, "effect"), 30, 0))
                    .then(Commands.argument("duration", IntegerArgumentType.integer(1, 1000000))
                        .executes(ctx -> executeEffectApply(ctx,
                            StringArgumentType.getString(ctx, "target"),
                            StringArgumentType.getString(ctx, "effect"),
                            IntegerArgumentType.getInteger(ctx, "duration"), 0))
                        .then(Commands.argument("amplifier", IntegerArgumentType.integer(0, 255))
                            .executes(ctx -> executeEffectApply(ctx,
                                StringArgumentType.getString(ctx, "target"),
                                StringArgumentType.getString(ctx, "effect"),
                                IntegerArgumentType.getInteger(ctx, "duration"),
                                IntegerArgumentType.getInteger(ctx, "amplifier")))
                        )
                    )
                )
            )
        );
    }

    private static int executeEffectClear(CommandContext<CommandSourceStack> ctx, String targetName) {
        var src = ctx.getSource();
        ServerPlayer target = resolveTarget(src, targetName);
        if (target == null) return 0;
        target.removeAllEffects();
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.effect.cleared", targetName), true);
        return 1;
    }

    private static int executeEffectApply(CommandContext<CommandSourceStack> ctx,
            String targetName, String effectId, int durationSeconds, int amplifier) {
        var src = ctx.getSource();
        ServerPlayer target = resolveTarget(src, targetName);
        if (target == null) return 0;

        // Resolve effect — try with and without minecraft: prefix
        String id = effectId.contains(":") ? effectId : "minecraft:" + effectId;
        Identifier loc = Identifier.tryParse(id);
        if (loc == null) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.effect.unknown", effectId));
            return 0;
        }
        net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effectHolder =
            BuiltInRegistries.MOB_EFFECT.get(loc).map(h -> (net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>) h).orElse(null);
        if (effectHolder == null) {
            // Try by path only across all namespaces
            effectHolder = BuiltInRegistries.MOB_EFFECT.entrySet().stream()
                .filter(e -> e.getKey().identifier().getPath().equals(effectId.toLowerCase()))
                .map(e -> BuiltInRegistries.MOB_EFFECT.wrapAsHolder(e.getValue()))
                .findFirst().orElse(null);
        }
        if (effectHolder == null) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.effect.unknown", effectId));
            return 0;
        }

        int durationTicks = durationSeconds * 20;
        target.addEffect(new MobEffectInstance(
            effectHolder, durationTicks, amplifier, false, true));
        final String eid = effectId; final int fa = amplifier; final int fd = durationSeconds;
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.effect.applied",
            targetName, eid, fa, fd), true);
        return 1;
    }

    // ── /spawnmob <mob> [amount] [player] ─────────────────────────────────────
    private static void registerSpawnMob(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("spawnmob")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.spawnmob"); })
            .then(Commands.argument("mob", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                        .map(Identifier::getPath).toList(), b))
                .executes(ctx -> executeSpawnMob(ctx, StringArgumentType.getString(ctx, "mob"), 1, null))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100))
                    .executes(ctx -> executeSpawnMob(ctx, StringArgumentType.getString(ctx, "mob"),
                        IntegerArgumentType.getInteger(ctx, "amount"), null))
                    .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                        .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.spawnmob.others"))
                        .executes(ctx -> executeSpawnMob(ctx, StringArgumentType.getString(ctx, "mob"),
                            IntegerArgumentType.getInteger(ctx, "amount"),
                            StringArgumentType.getString(ctx, "target")))
                    )
                )
            )
        );
        // alias /mob
        d.register(Commands.literal("mob")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.spawnmob"); })
            .then(Commands.argument("mob", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                        .map(Identifier::getPath).toList(), b))
                .executes(ctx -> executeSpawnMob(ctx, StringArgumentType.getString(ctx, "mob"), 1, null))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100))
                    .executes(ctx -> executeSpawnMob(ctx, StringArgumentType.getString(ctx, "mob"),
                        IntegerArgumentType.getInteger(ctx, "amount"), null)))
            )
        );
    }

    @SuppressWarnings("deprecation")
    private static int executeSpawnMob(CommandContext<CommandSourceStack> ctx,
            String mobId, int amount, String targetName) {
        var src = ctx.getSource();
        ServerPlayer spawnAt = targetName != null
            ? src.getServer().getPlayerList().getPlayerByName(targetName)
            : src.getPlayer();
        if (spawnAt == null) {
            if (targetName != null) src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", targetName));
            else src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only"));
            return 0;
        }

        String id = mobId.contains(":") ? mobId : "minecraft:" + mobId;
        Identifier loc = Identifier.tryParse(id);
        if (loc == null) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.spawnmob.unknown", mobId));
            return 0;
        }
        Optional<EntityType<?>> typeOpt = BuiltInRegistries.ENTITY_TYPE.getOptional(loc);
        if (typeOpt.isEmpty()) {
            typeOpt = BuiltInRegistries.ENTITY_TYPE.entrySet().stream()
                .filter(e -> e.getKey().identifier().getPath().equals(mobId.toLowerCase()))
                .<EntityType<?>>map(Map.Entry::getValue)
                .findFirst();
        }
        if (typeOpt.isEmpty()) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.spawnmob.unknown", mobId));
            return 0;
        }
        EntityType<?> entityType = typeOpt.get();
        var level = com.zerog.neoessentials.util.LevelCompat.of(spawnAt);
        int spawned = 0;
        for (int i = 0; i < amount; i++) {
            var entity = com.zerog.neoessentials.util.EntityTypeCompat.create(entityType, level);
            if (entity == null) break;
            entity.snapTo(spawnAt.getX(), spawnAt.getY(), spawnAt.getZ(), spawnAt.getYRot(), 0f);
            if (entity instanceof Mob mob) {
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnAt.blockPosition()),
                    EntitySpawnReason.COMMAND, null);
            }
            level.addFreshEntity(entity);
            spawned++;
        }
        final int fs = spawned;
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.spawnmob.success",
            fs, mobId, spawnAt.getName().getString()), true);
        NeoLog.info(LOGGER, LogCategory.GENERAL, "{} spawned {}x {} at {}", senderName(src), fs, mobId, spawnAt.getName().getString());
        return 1;
    }

    // ── /unlimited [list|clear|<item>] [player] ───────────────────────────────
    // Essentials: when item is in unlimited set, consuming it auto-refills to the stack max.
    private static void registerUnlimited(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("unlimited")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.unlimited"); })
            .executes(ctx -> executeUnlimitedList(ctx, null))
            .then(Commands.literal("list")
                .executes(ctx -> executeUnlimitedList(ctx, null))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                    .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.unlimited.others"))
                    .executes(ctx -> executeUnlimitedList(ctx, StringArgumentType.getString(ctx, "target"))))
            )
            .then(Commands.literal("clear")
                .executes(ctx -> executeUnlimitedClear(ctx, null))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                    .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.unlimited.others"))
                    .executes(ctx -> executeUnlimitedClear(ctx, StringArgumentType.getString(ctx, "target"))))
            )
            .then(Commands.argument("item", StringArgumentType.word())
                .executes(ctx -> executeUnlimitedToggle(ctx, StringArgumentType.getString(ctx, "item"), null))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                    .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.unlimited.others"))
                    .executes(ctx -> executeUnlimitedToggle(ctx,
                        StringArgumentType.getString(ctx, "item"),
                        StringArgumentType.getString(ctx, "target")))
                )
            )
        );
    }

    private static int executeUnlimitedList(CommandContext<CommandSourceStack> ctx, String targetName) {
        var src = ctx.getSource();
        ServerPlayer target = resolveTarget(src, targetName);
        if (target == null) return 0;
        Set<String> items = unlimitedItems.getOrDefault(target.getUUID(), Collections.emptySet());
        String list = items.isEmpty() ? "none" : String.join(", ", items);
        src.sendSuccess(() -> MessageUtil.info("commands.neoessentials.unlimited.list",
            target.getName().getString(), list), false);
        return 1;
    }

    private static int executeUnlimitedClear(CommandContext<CommandSourceStack> ctx, String targetName) {
        var src = ctx.getSource();
        ServerPlayer target = resolveTarget(src, targetName);
        if (target == null) return 0;
        unlimitedItems.remove(target.getUUID());
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.unlimited.cleared",
            target.getName().getString()), true);
        return 1;
    }

    private static int executeUnlimitedToggle(CommandContext<CommandSourceStack> ctx, String itemId, String targetName) {
        var src = ctx.getSource();
        ServerPlayer target = resolveTarget(src, targetName);
        if (target == null) return 0;

        // Resolve item ID
        String id = itemId.contains(":") ? itemId : "minecraft:" + itemId;
        Identifier loc = Identifier.tryParse(id);
        if (loc == null || BuiltInRegistries.ITEM.getValue(loc) == net.minecraft.world.item.Items.AIR) {
            // Try hand
            if (itemId.equalsIgnoreCase("hand") && src.getPlayer() != null) {
                id = com.zerog.neoessentials.economy.worth.WorthManager.getItemId(src.getPlayer().getMainHandItem());
            } else {
                src.sendFailure(MessageUtil.error("commands.neoessentials.worth.unknown_item", itemId));
                return 0;
            }
        }

        Set<String> items = unlimitedItems.computeIfAbsent(target.getUUID(), k -> new HashSet<>());
        final String fid = id;
        if (items.contains(id)) {
            items.remove(id);
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.unlimited.removed", fid,
                target.getName().getString()), false);
        } else {
            items.add(id);
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.unlimited.added", fid,
                target.getName().getString()), false);
        }
        return 1;
    }

    /** Check if an item ID is unlimited for this player. Used by item-use event handler. */
    @SuppressWarnings("unused")
    public static boolean isUnlimited(UUID uuid, String itemId) {
        Set<String> items = unlimitedItems.get(uuid);
        return items != null && items.contains(itemId);
    }

    // ── /condense [item] ──────────────────────────────────────────────────────
    // Essentials: convert loose items to storage block equivalents using crafting recipes.
    // We hard-code the most common condensable mappings (nugget→ingot→block pattern).
    private static void registerCondense(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("condense")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.condense"); })
            .executes(ctx -> executeCondense(ctx, null))
            .then(Commands.argument("item", StringArgumentType.word())
                .executes(ctx -> executeCondense(ctx, StringArgumentType.getString(ctx, "item")))
            )
        );
    }

    // Condense rules: item → (required count, result item, result count)
    private static final List<CondenseRule> CONDENSE_RULES = List.of(
        // Nuggets → Ingots
        new CondenseRule("minecraft:iron_nugget", 9, "minecraft:iron_ingot", 1),
        new CondenseRule("minecraft:gold_nugget", 9, "minecraft:gold_ingot", 1),
        // Ingots → Blocks
        new CondenseRule("minecraft:iron_ingot", 9, "minecraft:iron_block", 1),
        new CondenseRule("minecraft:gold_ingot", 9, "minecraft:gold_block", 1),
        new CondenseRule("minecraft:copper_ingot", 9, "minecraft:copper_block", 1),
        new CondenseRule("minecraft:netherite_ingot", 9, "minecraft:netherite_block", 1),
        new CondenseRule("minecraft:diamond", 9, "minecraft:diamond_block", 1),
        new CondenseRule("minecraft:emerald", 9, "minecraft:emerald_block", 1),
        new CondenseRule("minecraft:lapis_lazuli", 9, "minecraft:lapis_block", 1),
        new CondenseRule("minecraft:redstone", 9, "minecraft:redstone_block", 1),
        new CondenseRule("minecraft:coal", 9, "minecraft:coal_block", 1),
        new CondenseRule("minecraft:quartz", 4, "minecraft:quartz_block", 1),
        new CondenseRule("minecraft:wheat", 9, "minecraft:hay_block", 1),
        new CondenseRule("minecraft:bone_meal", 9, "minecraft:bone_block", 1),
        new CondenseRule("minecraft:snowball", 4, "minecraft:snow_block", 1),
        new CondenseRule("minecraft:clay_ball", 4, "minecraft:clay", 1),
        new CondenseRule("minecraft:glowstone_dust", 4, "minecraft:glowstone", 1),
        new CondenseRule("minecraft:amethyst_shard", 4, "minecraft:amethyst_block", 1),
        new CondenseRule("minecraft:raw_iron", 9, "minecraft:raw_iron_block", 1),
        new CondenseRule("minecraft:raw_gold", 9, "minecraft:raw_gold_block", 1),
        new CondenseRule("minecraft:raw_copper", 9, "minecraft:raw_copper_block", 1)
    );

    private static int executeCondense(CommandContext<CommandSourceStack> ctx, String filterItem) {
        var src = ctx.getSource();
        var player = src.getPlayer();
        if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }

        var inv = player.getInventory();
        int converted = 0;

        for (CondenseRule rule : CONDENSE_RULES) {
            if (filterItem != null) {
                String fid = filterItem.contains(":") ? filterItem : "minecraft:" + filterItem;
                if (!rule.inputId.equals(fid)) continue;
            }
            // Count how many of the input item we have
            int count = 0;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (!s.isEmpty() && com.zerog.neoessentials.economy.worth.WorthManager.getItemId(s).equals(rule.inputId)) {
                    count += s.getCount();
                }
            }
            if (count < rule.inputCount) continue;

            int times = count / rule.inputCount;
            // Remove inputs — inline count directly, no redundant intermediate variable
            int remaining = times * rule.inputCount;
            for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
                ItemStack s = inv.getItem(i);
                if (!s.isEmpty() && com.zerog.neoessentials.economy.worth.WorthManager.getItemId(s).equals(rule.inputId)) {
                    int take = Math.min(s.getCount(), remaining);
                    s.shrink(take);
                    remaining -= take;
                    if (s.isEmpty()) inv.setItem(i, ItemStack.EMPTY);
                }
            }

            // Add outputs
            Identifier outLoc = Identifier.tryParse(rule.outputId);
            if (outLoc != null) {
                net.minecraft.world.item.Item outItem = BuiltInRegistries.ITEM.getValue(outLoc);
                if (outItem != net.minecraft.world.item.Items.AIR) {
                    int totalOut = times * rule.outputCount;
                    int maxStack = new ItemStack(outItem).getMaxStackSize();
                    while (totalOut > 0) {
                        int give = Math.min(totalOut, maxStack);
                        ItemStack out = new ItemStack(outItem, give);
                        if (!inv.add(out)) player.drop(out, false);
                        totalOut -= give;
                    }
                    converted++;
                }
            }
        }

        if (converted == 0) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.condense.nothing"));
            return 0;
        }
        final int fc = converted;
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.condense.success", fc), false);
        return 1;
    }

    private record CondenseRule(String inputId, int inputCount, String outputId, int outputCount) {}

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static ServerPlayer resolveTarget(CommandSourceStack src, String targetName) {
        if (targetName != null) {
            ServerPlayer p = src.getServer().getPlayerList().getPlayerByName(targetName);
            if (p == null) src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", targetName));
            return p;
        }
        ServerPlayer self = src.getPlayer();
        if (self == null) src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only"));
        return self;
    }

    private static long parseTimeTicks(String value) {
        return switch (value.toLowerCase()) {
            case "reset" -> -1L;
            case "sunrise" -> 23000L;
            case "day", "morning" -> 1000L;
            case "noon" -> 6000L;
            case "afternoon" -> 9000L;
            case "sunset" -> 12000L;
            case "night", "dusk" -> 13000L;
            case "midnight" -> 18000L;
            default -> { try { yield Long.parseLong(value); } catch (NumberFormatException e) { yield -2L; } }
        };
    }

    private static String senderName(CommandSourceStack src) {
        return src.getPlayer() != null ? src.getPlayer().getName().getString() : "Console";
    }
}

