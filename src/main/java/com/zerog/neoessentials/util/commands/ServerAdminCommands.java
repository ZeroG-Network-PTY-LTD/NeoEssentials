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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

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
    private static final List<String> TIME_NAMES = Arrays.asList(
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
                    // Broadcast to all players with color code support
                    Component broadcast = MessageUtil.coloredText("§6[Broadcast] §f" + msg);
                    ctx.getSource().getServer().getPlayerList().getPlayers()
                        .forEach(p -> p.sendSystemMessage(broadcast));
                    ctx.getSource().getServer().sendSystemMessage(broadcast);
                    LOGGER.info("[Broadcast] {} : {}", senderName, msg);
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
                    Component broadcast = MessageUtil.coloredText("§6[Broadcast] §f" + msg);
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
                    Component broadcast = MessageUtil.coloredText("§6[Broadcast] §f" + msg);
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
            .executes(ctx -> executeTimeGet(ctx))
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
        long time = level.getDayTime() % 24000;
        src.sendSuccess(() -> MessageUtil.info("commands.neoessentials.time.current",
            level.dimension().location().getPath(), time, ticksToName(time)), false);
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
                level.setDayTime(level.getDayTime() + ticks);
            } else {
                level.setDayTime(ticks);
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
                case "sun" -> {
                    level.setWeatherParameters(ticks, 0, false, false);
                }
                case "storm" -> {
                    level.setWeatherParameters(0, ticks, true, false);
                }
                case "thunder" -> {
                    level.setWeatherParameters(0, ticks, true, true);
                }
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
                    self.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
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
                    target.teleportTo(self.serverLevel(), self.getX(), self.getY(), self.getZ(), self.getYRot(), self.getXRot());
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
                        self.teleportTo(online.serverLevel(), online.getX(), online.getY(), online.getZ(), online.getYRot(), online.getXRot());
                        src.sendSuccess(() -> MessageUtil.success("commands.neoessentials.teleport.tpoffline.online", name), false);
                        return 1;
                    }

                    // Try to find by UUID from usercache
                    var userCache = src.getServer().getProfileCache();
                    var profile = userCache != null ? userCache.get(name) : java.util.Optional.empty();
                    if (profile.isEmpty()) {
                        src.sendFailure(MessageUtil.error("commands.neoessentials.teleport.tpoffline.not_found", name));
                        return 0;
                    }
                    java.util.UUID uuid = profile.get().getId();
                    // Load offline player data from world save
                    net.minecraft.nbt.CompoundTag tag = src.getServer().getPlayerList().getSingleplayer() != null
                        ? null : loadOfflinePlayerData(src.getServer(), uuid);

                    if (tag == null || !tag.contains("Pos")) {
                        src.sendFailure(MessageUtil.error("commands.neoessentials.teleport.tpoffline.no_data", name));
                        return 0;
                    }

                    var pos = tag.getList("Pos", net.minecraft.nbt.Tag.TAG_DOUBLE);
                    double x = pos.getDouble(0), y = pos.getDouble(1), z = pos.getDouble(2);
                    var rot = tag.getList("Rotation", net.minecraft.nbt.Tag.TAG_FLOAT);
                    float yaw = rot.size() > 0 ? rot.getFloat(0) : 0f;
                    float pitch = rot.size() > 1 ? rot.getFloat(1) : 0f;

                    // Dimension
                    var dimKey = tag.contains("Dimension")
                        ? net.minecraft.resources.ResourceLocation.tryParse(tag.getString("Dimension")) : null;
                    ServerLevel level = dimKey != null
                        ? src.getServer().getAllLevels().stream()
                            .filter(l -> l.dimension().location().equals(dimKey))
                            .findFirst().orElse(src.getServer().overworld())
                        : src.getServer().overworld();

                    final double fx = x, fy = y, fz = z;
                    final float fyaw = yaw, fpitch = pitch;
                    self.teleportTo(level, fx, fy, fz, fyaw, fpitch);
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
                server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).toFile(),
                ""
            );
            java.io.File playerFile = new java.io.File(playerDataDir, uuid + ".dat");
            if (!playerFile.exists()) return null;
            return net.minecraft.nbt.NbtIo.readCompressed(playerFile.toPath(),
                net.minecraft.nbt.NbtAccounter.unlimitedHeap());
        } catch (Exception e) {
            return null;
        }
    }
}

