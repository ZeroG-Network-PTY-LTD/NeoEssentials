package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.authlib.GameProfile;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.StreamSupport;

/**
 * Server admin utility commands ported from EssentialsX:
 *
 *  /broadcast <message>              — broadcast to all players (Essentials: broadcastTl)
 *  /time [set|add] <value> [world]   — get/set/add world time
 *  /weather <sun|storm|thunder> [duration] [world] — set world weather
 *  /kill <player>                    — kill a player (respects kill.exempt)
 *  /gamemode <mode> [player]         — full /gamemode command with all modes
 *  /tpo <player>                     — teleport override (bypass tptoggle)
 *  /tpohere <player>                 — bring player here override
 *  /tpoffline <player>               — teleport to offline player's last position
 */
public class ServerAdminCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerAdminCommands.class);

    // Named time values matching Essentials
    private static final List<String> TIME_NAMES = List.of(
        "sunrise", "day", "morning", "noon", "afternoon", "sunset", "night", "midnight"
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerBroadcast(dispatcher);
        registerTime(dispatcher);
        registerWeather(dispatcher);
        registerKill(dispatcher);
        registerGamemode(dispatcher);
        registerTpo(dispatcher);
        registerTpoffline(dispatcher);
    }

    // ── /broadcast <message> ─────────────────────────────────────────────────
    private static void registerBroadcast(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("broadcast")
            .requires(src -> {
                var p = src.getPlayer();
                return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.broadcast");
            })
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(ctx -> {
                    String msg = StringArgumentType.getString(ctx, "message");
                    String senderName = ctx.getSource().getPlayer() != null
                        ? ctx.getSource().getPlayer().getName().getString() : "Console";
                    // Resolve placeholders using sender's player context (may be null for console)
                    net.minecraft.server.level.ServerPlayer senderPlayer = ctx.getSource().getPlayer();
                    String resolved = com.zerog.neoessentials.api.PlaceholderAPI.setPlaceholders(senderPlayer, msg);
                    Component broadcast = MessageUtil.coloredText("§6[Broadcast] §f" + resolved);
                    ctx.getSource().getServer().getPlayerList().getPlayers()
                        .forEach(p -> p.sendSystemMessage(broadcast));
                    ctx.getSource().getServer().sendSystemMessage(broadcast);
                    LOGGER.info("[Broadcast] {} : {}", senderName, resolved);
                    return 1;
                })
            )
        );
        // alias /bc
        d.register(Commands.literal("bc")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.broadcast"); })
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(ctx -> {
                    String msg = StringArgumentType.getString(ctx, "message");
                    net.minecraft.server.level.ServerPlayer senderPlayer = ctx.getSource().getPlayer();
                    String resolved = com.zerog.neoessentials.api.PlaceholderAPI.setPlaceholders(senderPlayer, msg);
                    Component broadcast = MessageUtil.coloredText("§6[Broadcast] §f" + resolved);
                    ctx.getSource().getServer().getPlayerList().getPlayers().forEach(p -> p.sendSystemMessage(broadcast));
                    ctx.getSource().getServer().sendSystemMessage(broadcast);
                    return 1;
                })
            )
        );
        // alias /announce
        d.register(Commands.literal("announce")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.broadcast"); })
            .then(Commands.argument("message", StringArgumentType.greedyString())
                .executes(ctx -> {
                    String msg = StringArgumentType.getString(ctx, "message");
                    net.minecraft.server.level.ServerPlayer senderPlayer = ctx.getSource().getPlayer();
                    String resolved = com.zerog.neoessentials.api.PlaceholderAPI.setPlaceholders(senderPlayer, msg);
                    Component broadcast = MessageUtil.coloredText("§6[Broadcast] §f" + resolved);
                    ctx.getSource().getServer().getPlayerList().getPlayers().forEach(p -> p.sendSystemMessage(broadcast));
                    ctx.getSource().getServer().sendSystemMessage(broadcast);
                    return 1;
                })
            )
        );
    }

    // ── /time [set|add] <value> [world] ──────────────────────────────────────
    // Time names: day=1000, noon=6000, sunset=12000, night=13000, midnight=18000, sunrise=23000
    private static void registerTime(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("time")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.time"); })
            // /time  — show current time
            .executes(ServerAdminCommands::executeTimeGet)
            // /time set <value>
            .then(Commands.literal("set")
                .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.time.set"); })
                .then(Commands.argument("value", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(TIME_NAMES, b))
                    .executes(ctx -> executeTimeSet(ctx, StringArgumentType.getString(ctx, "value"), false))
                )
            )
            // /time add <value>
            .then(Commands.literal("add")
                .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.time.set"); })
                .then(Commands.argument("value", StringArgumentType.word())
                    .executes(ctx -> executeTimeSet(ctx, StringArgumentType.getString(ctx, "value"), true))
                )
            )
            // /time <value>  (shorthand — implies set)
            .then(Commands.argument("value", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(TIME_NAMES, b))
                .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.time.set"); })
                .executes(ctx -> executeTimeSet(ctx, StringArgumentType.getString(ctx, "value"), false))
            )
        );
        // /day and /night aliases
        d.register(Commands.literal("day")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.time.set"); })
            .executes(ctx -> setAllWorldsTime(ctx, 1000L, false)));
        d.register(Commands.literal("night")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.time.set"); })
            .executes(ctx -> setAllWorldsTime(ctx, 13000L, false)));
    }

    private static int executeTimeGet(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        ServerLevel level = src.getLevel();
        long time = com.zerog.neoessentials.util.WorldClockCompat.getTime(level) % 24000;
        src.sendSuccess(() -> MessageUtil.info("commands.neoessentials.time.current",
            level.dimension().identifier().getPath(), time, ticksToName(time)), false);
        return 1;
    }

    private static int executeTimeSet(CommandContext<CommandSourceStack> ctx, String value, boolean add) {
        long ticks = parseTimeTicks(value);
        if (ticks < 0) {
            ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.time.invalid", value));
            return 0;
        }
        return setAllWorldsTime(ctx, ticks, add);
    }

    private static int setAllWorldsTime(CommandContext<CommandSourceStack> ctx, long ticks, boolean add) {
        var src = ctx.getSource();
        for (ServerLevel level : src.getServer().getAllLevels()) {
            if (add) {
                com.zerog.neoessentials.util.WorldClockCompat.setTime(level, com.zerog.neoessentials.util.WorldClockCompat.getTime(level) + ticks);
            } else {
                com.zerog.neoessentials.util.WorldClockCompat.setTime(level, ticks);
            }
        }
        String op = add ? "Added" : "Set";
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.time.set",
            op, ticks, add ? "" : " (" + ticksToName(ticks) + ")"), true);
        return 1;
    }

    private static long parseTimeTicks(String value) {
        return switch (value.toLowerCase()) {
            case "sunrise" -> 23000L;
            case "day", "morning" -> 1000L;
            case "noon" -> 6000L;
            case "afternoon" -> 9000L;
            case "sunset" -> 12000L;
            case "night", "dusk" -> 13000L;
            case "midnight" -> 18000L;
            default -> {
                try { yield Long.parseLong(value); }
                catch (NumberFormatException e) { yield -1L; }
            }
        };
    }

    private static String ticksToName(long ticks) {
        long t = ticks % 24000;
        if (t < 1500) return "sunrise";
        if (t < 6000) return "day";
        if (t < 9000) return "noon";
        if (t < 12000) return "afternoon";
        if (t < 13800) return "sunset";
        if (t < 18000) return "night";
        return "midnight";
    }

    // ── /weather <sun|storm|thunder> [duration] ───────────────────────────────
    private static void registerWeather(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("weather")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.weather"); })
            .then(Commands.literal("sun")
                .executes(ctx -> executeWeather(ctx, "sun", 0))
                .then(Commands.argument("duration", IntegerArgumentType.integer(1, 1000000))
                    .executes(ctx -> executeWeather(ctx, "sun", IntegerArgumentType.getInteger(ctx, "duration"))))
            )
            .then(Commands.literal("clear")
                .executes(ctx -> executeWeather(ctx, "sun", 0))
                .then(Commands.argument("duration", IntegerArgumentType.integer(1, 1000000))
                    .executes(ctx -> executeWeather(ctx, "sun", IntegerArgumentType.getInteger(ctx, "duration"))))
            )
            .then(Commands.literal("rain")
                .executes(ctx -> executeWeather(ctx, "storm", 0))
                .then(Commands.argument("duration", IntegerArgumentType.integer(1, 1000000))
                    .executes(ctx -> executeWeather(ctx, "storm", IntegerArgumentType.getInteger(ctx, "duration"))))
            )
            .then(Commands.literal("storm")
                .executes(ctx -> executeWeather(ctx, "storm", 0))
                .then(Commands.argument("duration", IntegerArgumentType.integer(1, 1000000))
                    .executes(ctx -> executeWeather(ctx, "storm", IntegerArgumentType.getInteger(ctx, "duration"))))
            )
            .then(Commands.literal("thunder")
                .executes(ctx -> executeWeather(ctx, "thunder", 0))
                .then(Commands.argument("duration", IntegerArgumentType.integer(1, 1000000))
                    .executes(ctx -> executeWeather(ctx, "thunder", IntegerArgumentType.getInteger(ctx, "duration"))))
            )
        );
        // aliases
        d.register(Commands.literal("sun")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.weather"); })
            .executes(ctx -> executeWeather(ctx, "sun", 0)));
        d.register(Commands.literal("storm")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.weather"); })
            .executes(ctx -> executeWeather(ctx, "storm", 0)));
        d.register(Commands.literal("thunder")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.weather"); })
            .executes(ctx -> executeWeather(ctx, "thunder", 0)));
    }

    private static int executeWeather(CommandContext<CommandSourceStack> ctx, String type, int durationSeconds) {
        var src = ctx.getSource();
        // Apply to all overworld-type levels
        for (ServerLevel level : src.getServer().getAllLevels()) {
            if (!level.dimensionType().hasSkyLight()) continue; // skip nether/end
            int ticks = durationSeconds > 0 ? durationSeconds * 20 : 6000;
            switch (type) {
                case "sun" -> level.setWeatherParameters(ticks, 0, false, false);
                case "storm" -> level.setWeatherParameters(0, ticks, true, false);
                case "thunder" -> level.setWeatherParameters(0, ticks, true, true);
            }
        }
        String label = durationSeconds > 0
            ? type + " for " + durationSeconds + "s"
            : type;
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.weather.set", label), true);
        LOGGER.info("{} set weather to {}", src.getPlayer() != null ? src.getPlayer().getName().getString() : "Console", label);
        return 1;
    }

    // ── /kill <player> ────────────────────────────────────────────────────────
    private static void registerKill(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("kill")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.kill"); })
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                .executes(ctx -> executeKill(ctx, StringArgumentType.getString(ctx, "target")))
            )
        );
    }

    private static int executeKill(CommandContext<CommandSourceStack> ctx, String targetName) {
        var src = ctx.getSource();
        ServerPlayer target = src.getServer().getPlayerList().getPlayerByName(targetName);
        if (target == null) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", targetName));
            return 0;
        }
        // Essentials: check kill.exempt
        if (PermissionAPI.hasPermission(target.getUUID(), "neoessentials.kill.exempt")
                && src.getPlayer() != null
                && !PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.kill.force")) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.kill.exempt", targetName));
            return 0;
        }
        target.hurt(target.damageSources().genericKill(), Float.MAX_VALUE);
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.kill.success", targetName), true);
        LOGGER.info("{} killed {}", src.getPlayer() != null ? src.getPlayer().getName().getString() : "Console", targetName);
        return 1;
    }

    // ── /gamemode <mode> [player] ─────────────────────────────────────────────
    private static void registerGamemode(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("gamemode")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.gamemode"); })
            .then(Commands.literal("survival")
                .executes(ctx -> executeGamemode(ctx, GameType.SURVIVAL, null))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                    .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.gamemode.others"))
                    .executes(ctx -> executeGamemode(ctx, GameType.SURVIVAL, StringArgumentType.getString(ctx, "target"))))
            )
            .then(Commands.literal("creative")
                .executes(ctx -> executeGamemode(ctx, GameType.CREATIVE, null))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                    .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.gamemode.others"))
                    .executes(ctx -> executeGamemode(ctx, GameType.CREATIVE, StringArgumentType.getString(ctx, "target"))))
            )
            .then(Commands.literal("adventure")
                .executes(ctx -> executeGamemode(ctx, GameType.ADVENTURE, null))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                    .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.gamemode.others"))
                    .executes(ctx -> executeGamemode(ctx, GameType.ADVENTURE, StringArgumentType.getString(ctx, "target"))))
            )
            .then(Commands.literal("spectator")
                .executes(ctx -> executeGamemode(ctx, GameType.SPECTATOR, null))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                    .requires(src -> src.getPlayer() == null || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.gamemode.others"))
                    .executes(ctx -> executeGamemode(ctx, GameType.SPECTATOR, StringArgumentType.getString(ctx, "target"))))
            )
            // numeric shortcuts: 0=survival, 1=creative, 2=adventure, 3=spectator
            .then(Commands.literal("0").executes(ctx -> executeGamemode(ctx, GameType.SURVIVAL, null)))
            .then(Commands.literal("1").executes(ctx -> executeGamemode(ctx, GameType.CREATIVE, null)))
            .then(Commands.literal("2").executes(ctx -> executeGamemode(ctx, GameType.ADVENTURE, null)))
            .then(Commands.literal("3").executes(ctx -> executeGamemode(ctx, GameType.SPECTATOR, null)))
        );
    }

    private static int executeGamemode(CommandContext<CommandSourceStack> ctx, GameType mode, String targetName) {
        var src = ctx.getSource();
        ServerPlayer target = targetName != null
            ? src.getServer().getPlayerList().getPlayerByName(targetName)
            : src.getPlayer();
        if (target == null) {
            if (targetName != null) src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", targetName));
            else src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only"));
            return 0;
        }
        target.setGameMode(mode);
        String modeName = mode.getName();
        boolean isOther = src.getPlayer() == null || !src.getPlayer().getUUID().equals(target.getUUID());
        if (isOther) {
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.gamemode.other", target.getName().getString(), modeName), true);
            target.sendSystemMessage(MessageUtil.info("commands.neoessentials.gamemode.self", modeName));
        } else {
            src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.gamemode.self", modeName), false);
        }
        return 1;
    }

    // ── /tpo <player> and /tpohere <player> (override tptoggle) ──────────────
    private static void registerTpo(CommandDispatcher<CommandSourceStack> d) {
        // /tpo <player> — teleport to player ignoring their tptoggle
        d.register(Commands.literal("tpo")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.teleport.tpo"); })
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                .executes(ctx -> {
                    var src = ctx.getSource();
                    var self = src.getPlayer();
                    if (self == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }
                    String name = StringArgumentType.getString(ctx, "target");
                    ServerPlayer target = src.getServer().getPlayerList().getPlayerByName(name);
                    if (target == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", name)); return 0; }
                    self.teleportTo(
                com.zerog.neoessentials.util.LevelCompat.of(target),
                java.util.Set.of(),
                target.getX(),
                target.getY(),
                target.getZ(),
                target.getYRot(),
                target.getXRot(),
                true
            );
                    src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.teleport.tpo.success", name), false);
                    return 1;
                })
            )
        );
        // /tpohere <player> — bring player ignoring their tptoggle
        d.register(Commands.literal("tpohere")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.teleport.tpohere"); })
            .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), b))
                .executes(ctx -> {
                    var src = ctx.getSource();
                    var self = src.getPlayer();
                    if (self == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }
                    String name = StringArgumentType.getString(ctx, "target");
                    ServerPlayer target = src.getServer().getPlayerList().getPlayerByName(name);
                    if (target == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", name)); return 0; }
                    target.teleportTo(
                com.zerog.neoessentials.util.LevelCompat.of(self),
                java.util.Set.of(),
                self.getX(),
                self.getY(),
                self.getZ(),
                self.getYRot(),
                self.getXRot(),
                true
            );
                    src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.teleport.tpohere.success", name), true);
                    target.sendSystemMessage(MessageUtil.info("commands.neoessentials.teleport.tpohere.notify", self.getName().getString()));
                    return 1;
                })
            )
        );
    }

    // ── /tpoffline <player> ───────────────────────────────────────────────────
    // Teleports to an offline player's last recorded position using NeoForge player data
    private static void registerTpoffline(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("tpoffline")
            .requires(src -> { var p = src.getPlayer(); return p == null || PermissionAPI.hasPermission(p.getUUID(), "neoessentials.teleport.tpoffline"); })
            .then(Commands.argument("player", StringArgumentType.word())
                .executes(ctx -> {
                    var src = ctx.getSource();
                    var self = src.getPlayer();
                    if (self == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }
                    String name = StringArgumentType.getString(ctx, "player");

                    // First check if online
                    ServerPlayer online = src.getServer().getPlayerList().getPlayerByName(name);
                    if (online != null) {
                        // Player is online — just use tpo logic
                        self.teleportTo(
                com.zerog.neoessentials.util.LevelCompat.of(online),
                java.util.Set.of(),
                online.getX(),
                online.getY(),
                online.getZ(),
                online.getYRot(),
                online.getXRot(),
                true
            );
                        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.teleport.tpoffline.online", name), false);
                        return 1;
                    }

                    // Try to find by UUID from usercache
                    var userCache = src.getServer().services().nameToIdCache();
                    Optional<net.minecraft.server.players.NameAndId> profile = userCache != null ? userCache.get(name) : Optional.empty();
                    if (profile.isEmpty()) {
                        src.sendFailure(MessageUtil.error("commands.neoessentials.teleport.tpoffline.not_found", name));
                        return 0;
                    }
                    java.util.UUID uuid = profile.get().id();
                    // Load offline player data from world save
                    net.minecraft.nbt.CompoundTag tag = loadOfflinePlayerData(src.getServer(), uuid);

                    if (tag == null || !tag.contains("Pos")) {
                        src.sendFailure(MessageUtil.error("commands.neoessentials.teleport.tpoffline.no_data", name));
                        return 0;
                    }

                    var pos = tag.getList("Pos", net.minecraft.nbt.Tag.TAG_DOUBLE);
                    double x = pos.getDouble(0), y = pos.getDouble(1), z = pos.getDouble(2);
                    var rot = tag.getList("Rotation", net.minecraft.nbt.Tag.TAG_FLOAT);
                    float yaw = !rot.isEmpty() ? rot.getFloat(0) : 0f;
                    float pitch = rot.size() > 1 ? rot.getFloat(1) : 0f;

                    // Dimension
                    var dimKey = tag.contains("Dimension")
                        ? Identifier.tryParse(com.zerog.neoessentials.util.CompoundTagCompat.getString(tag, "Dimension")) : null;
                    ServerLevel level = dimKey != null
                        ? StreamSupport.stream(src.getServer().getAllLevels().spliterator(), false)
                            .filter(l -> l.dimension().identifier().equals(dimKey))
                            .findFirst().orElse(src.getServer().overworld())
                        : src.getServer().overworld();

                    final double fx = x, fy = y, fz = z;
                    self.teleportTo(
                level,
                java.util.Set.of(),
                fx,
                fy,
                fz,
                yaw,
                pitch,
                true
            );
                    src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.teleport.tpoffline.success",
                        name, String.format("%.1f, %.1f, %.1f", fx, fy, fz)), false);
                    return 1;
                })
            )
        );
    }

    /** Load offline player NBT data from the world saves directory. */
    private static net.minecraft.nbt.CompoundTag loadOfflinePlayerData(
            net.minecraft.server.MinecraftServer server, java.util.UUID uuid) {
        try {
            java.io.File playerDataDir = new java.io.File(
                server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).toFile(), "");
            java.io.File playerFile = new java.io.File(playerDataDir, uuid + ".dat");
            if (!playerFile.exists()) return null;
            return net.minecraft.nbt.NbtIo.readCompressed(playerFile.toPath(),
                net.minecraft.nbt.NbtAccounter.unlimitedHeap());
        } catch (Exception e) {
            return null;
        }
    }

    // ── Called from NeoEssentials to register world/spawner/recipe ────────────
    public static void registerWorldCommands(CommandDispatcher<CommandSourceStack> d) {
        registerWorld(d);
        registerSpawner(d);
        registerRecipe(d);
    }

    // ── /world [name] [player] ────────────────────────────────────────────────
    // Essentials: Commandworld — teleport to a named dimension.
    private static void registerWorld(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("world")
            .requires(src -> src.getPlayer() == null
                || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.world"))
            .executes(ctx -> {
                var src = ctx.getSource();
                List<String> worlds = new ArrayList<>();
                for (ServerLevel level : src.getServer().getAllLevels())
                    worlds.add(level.dimension().identifier().toString());
                src.sendSuccess(() -> MessageUtil.info("commands.neoessentials.world.list",
                    String.join(", ", worlds)), false);
                return 1;
            })
            .then(Commands.argument("dimension", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    StreamSupport.stream(ctx.getSource().getServer().getAllLevels().spliterator(), false)
                        .map(l -> l.dimension().identifier().getPath()).toList(), b))
                .executes(ctx -> executeWorld(ctx, StringArgumentType.getString(ctx, "dimension"), null))
                .then(Commands.argument("target", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                        ctx.getSource().getServer().getPlayerNames(), b))
                    .requires(src -> src.getPlayer() == null
                        || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.world.others"))
                    .executes(ctx -> executeWorld(ctx,
                        StringArgumentType.getString(ctx, "dimension"),
                        StringArgumentType.getString(ctx, "target")))
                )
            )
        );
    }

    private static int executeWorld(CommandContext<CommandSourceStack> ctx, String dimName, String targetName) {
        var src = ctx.getSource();
        ServerPlayer player = targetName != null
            ? src.getServer().getPlayerList().getPlayerByName(targetName)
            : src.getPlayer();
        if (player == null) {
            if (targetName != null) src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_not_found", targetName));
            else src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only"));
            return 0;
        }
        ServerLevel target = null;
        for (ServerLevel level : src.getServer().getAllLevels()) {
            Identifier key = level.dimension().identifier();
            if (key.getPath().equalsIgnoreCase(dimName) || key.toString().equalsIgnoreCase(dimName)) {
                target = level; break;
            }
        }
        if (target == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.world.not_found", dimName)); return 0; }
        BlockPos spawn = target.getSharedSpawnPos();
        final ServerLevel fl = target;
        player.teleportTo(
                fl,
                java.util.Set.of(),
                spawn.getX() + 0.5,
                spawn.getY(),
                spawn.getZ() + 0.5,
                player.getYRot(),
                player.getXRot(),
                true
            );
        final String fn = dimName;
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.world.teleported", player.getName().getString(), fn), false);
        return 1;
    }

    // ── /spawner <mob> ────────────────────────────────────────────────────────
    // Essentials: Commandspawner — change looked-at spawner to a new entity type.
    private static void registerSpawner(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("spawner")
            .requires(src -> src.getPlayer() == null
                || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.spawner"))
            .then(Commands.argument("mob", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(Identifier::getPath).toList(), b))
                .executes(ctx -> executeSpawner(ctx, StringArgumentType.getString(ctx, "mob")))
            )
        );
    }

    private static int executeSpawner(CommandContext<CommandSourceStack> ctx, String mobName) {
        var src = ctx.getSource();
        var player = src.getPlayer();
        if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }
        if (!PermissionAPI.hasPermission(player.getUUID(), "neoessentials.spawner." + mobName.toLowerCase())
                && !PermissionAPI.hasPermission(player.getUUID(), "neoessentials.spawner.*")) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.spawner.no_perm_mob", mobName));
            return 0;
        }
        String id = mobName.contains(":") ? mobName : "minecraft:" + mobName;
        Identifier loc = Identifier.tryParse(id);
        Optional<net.minecraft.world.entity.EntityType<?>> typeOpt = loc != null
            ? BuiltInRegistries.ENTITY_TYPE.getOptional(loc) : Optional.empty();
        if (typeOpt.isEmpty()) {
            typeOpt = BuiltInRegistries.ENTITY_TYPE.entrySet().stream()
                .filter(e -> e.getKey().identifier().getPath().equalsIgnoreCase(mobName))
                .<net.minecraft.world.entity.EntityType<?>>map(Map.Entry::getValue)
                .findFirst();
        }
        if (typeOpt.isEmpty()) { src.sendFailure(MessageUtil.error("commands.neoessentials.spawnmob.unknown", mobName)); return 0; }
        var hit = player.pick(6, 1.0f, false);
        BlockPos bpos = BlockPos.containing(hit.getLocation());
        var level = com.zerog.neoessentials.util.LevelCompat.of(player);
        var state = level.getBlockState(bpos);
        if (!state.is(Blocks.SPAWNER) || !(level.getBlockEntity(bpos) instanceof SpawnerBlockEntity spawnerBE)) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.spawner.not_looking_at_spawner")); return 0;
        }
        spawnerBE.setEntityId(typeOpt.get(), level.getRandom());
        level.sendBlockUpdated(bpos, state, state, 3);
        spawnerBE.setChanged();
        final String fn = mobName;
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.spawner.changed", fn), true);
        LOGGER.info("{} changed spawner at {} to {}", player.getName().getString(), bpos, mobName);
        return 1;
    }

    // ── /recipe [item] ────────────────────────────────────────────────────────
    // Essentials: Commandrecipe — unlock and show crafting recipes for an item.
    private static void registerRecipe(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("recipe")
            .requires(src -> src.getPlayer() == null
                || PermissionAPI.hasPermission(src.getPlayer().getUUID(), "neoessentials.recipe"))
            .executes(ctx -> executeRecipe(ctx, null))
            .then(Commands.argument("item", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                    BuiltInRegistries.ITEM.keySet().stream().map(Identifier::getPath).toList(), b))
                .executes(ctx -> executeRecipe(ctx, StringArgumentType.getString(ctx, "item")))
            )
        );
    }

    private static int executeRecipe(CommandContext<CommandSourceStack> ctx, String itemName) {
        var src = ctx.getSource();
        var player = src.getPlayer();
        if (player == null) { src.sendFailure(MessageUtil.error("commands.neoessentials.general.player_only")); return 0; }
        net.minecraft.world.item.Item item = null;
        if (itemName == null) {
            ItemStack held = player.getMainHandItem();
            if (held.isEmpty()) { src.sendFailure(MessageUtil.error("commands.neoessentials.recipe.no_item")); return 0; }
            item = held.getItem();
        } else {
            String id = itemName.contains(":") ? itemName : "minecraft:" + itemName;
            Identifier loc = Identifier.tryParse(id);
            if (loc != null) item = BuiltInRegistries.ITEM.get(loc);
            if (item == null || item == net.minecraft.world.item.Items.AIR) {
                src.sendFailure(MessageUtil.error("commands.neoessentials.recipe.unknown_item", itemName)); return 0;
            }
        }
        final net.minecraft.world.item.Item finalItem = item;
        List<net.minecraft.world.item.crafting.RecipeHolder<?>> matching = new ArrayList<>();
        for (var holder : src.getServer().getRecipeManager().getRecipes()) {
            try {
                if (holder.value().getResultItem(src.getServer().registryAccess()).getItem() == finalItem)
                    matching.add(holder);
            } catch (Exception ignored) {}
        }
        if (matching.isEmpty()) {
            src.sendFailure(MessageUtil.error("commands.neoessentials.recipe.no_recipe", finalItem.getDescriptionId())); return 0;
        }
        // awardRecipes takes Collection<RecipeHolder<?>> in NeoForge 1.21.1
        player.awardRecipes(matching);
        final int fc = matching.size();
        // Use registry key to get item name string (Item.getId returns int in 1.21)
        final String desc = BuiltInRegistries.ITEM.getKey(finalItem).getPath();
        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.recipe.unlocked", fc, desc), false);
        return 1;
    }
}

